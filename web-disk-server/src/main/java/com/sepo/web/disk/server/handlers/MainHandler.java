package com.sepo.web.disk.server.handlers;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import com.sepo.web.disk.common.models.*;
import com.sepo.web.disk.common.service.*;
import com.sepo.web.disk.server.database.Database;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.sepo.web.disk.server.connection.Network.serverStorageName;

@ChannelHandler.Sharable
public class MainHandler extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LogManager.getLogger(MainHandler.class);
    private ServerState currentState = new ServerState(ServerState.State.IDLE, ServerState.Wait.REQUEST);
    private Path userStoragePath;
    private ArrayList<FileInfo> fileInfoList = new ArrayList<>();
    private int currFileInfoIndex = 0;
    private FileInfo currFileInfo;


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

        logger.info("Client connected...");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.info("Client disconnected...");
        ctx.close();
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
        logger.info("Channel Registered");
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        super.channelUnregistered(ctx);
        logger.info("Channel Unregistered");
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (currentState != null) {
            logger.info("Запуск метода channelRead, текущее состояние - " +
                    currentState.getCurrState().toString() +
                    ", текущее ожидание - " + currentState.getCurrWait().toString());
        } else {
            logger.info("Текущее состояние - null");
        }
        ByteBuf bb = (ByteBuf) msg;
        User user;

        // Отлов команд Cancel и Get_State
        // использую bb.copy так как после декодирования bb буфер каким-то образом очищается сам
        if (currentState.getCurrState() != ServerState.State.GET) {
            if (ObjectEncoderDecoder.DecodeByteBufToObject(ctx, bb.copy()) instanceof Sendable) {
                var temp = (Sendable) ObjectEncoderDecoder.DecodeByteBufToObject(ctx, bb.copy());
                if (temp instanceof ClientRequest) {
                    var req = (ClientRequest) temp;
                    switch ((req).getCurrRequest()) {
                        case STATE:
                            logger.info("get STATE request");
                            send(ctx, currentState);
                            bb.release();
                            return;
                        case CANCEL:
                            logger.info("get CANCEL request");
                            currentState.setCurrState(ServerState.State.IDLE);
                            bb.release();
                            currFileInfoIndex = 0;

                            return;
                    }

                }
            }
        }


        // Главная логика обмена сообщениями c сервером
        switch (currentState.getCurrState()) {
            case IDLE:
//                if (ObjectEncoderDecoder.DecodeByteBufToObject(bb.copy()) instanceof ServerRespond) {
//                    return;
//                }
                var request = (ClientRequest) ObjectEncoderDecoder.DecodeByteBufToObject(ctx, bb);

                switch (request.getCurrRequest()) {
                    case AUTH:
                        logger.info("getting AUTH request");
                        currentState.setCurrState(ServerState.State.AUTH)
                                .setCurrWait(ServerState.Wait.DATA);

                        send(ctx, new ServerRespond(ServerRespond.Responds.AUTH, ServerRespond.Results.PROCESSING));
                        break;
                    case REG:
                        logger.info("getting REG request");
                        currentState.setCurrState(ServerState.State.REG).setCurrWait(ServerState.Wait.DATA);
                        send(ctx, new ServerRespond(ServerRespond.Responds.REG, ServerRespond.Results.PROCESSING));
                        break;
                    case UPDATE:
                        logger.info("getting UPDATE request");
                        if (currFileInfoIndex < fileInfoList.size()) {
                            logger.info(String.format("Send fileInfo %d/%d", (currFileInfoIndex + 1), fileInfoList.size()));
                            ctx.writeAndFlush(ObjectEncoderDecoder.EncodeObjToByteBuf(fileInfoList.get(currFileInfoIndex)));
                            currFileInfoIndex++;

                        } else {
                            logger.info("send respond");
                            send(ctx, new ServerRespond(ServerRespond.Responds.UPDATE, ServerRespond.Results.SUCCESS));
                            currentState.setCurrState(ServerState.State.IDLE).setCurrWait(ServerState.Wait.REQUEST);
                        }
                        break;
                    case SEND:
                        logger.info("getting SEND request");
                        currentState.setCurrState(ServerState.State.GET).setCurrWait(ServerState.Wait.DATA);
                        send(ctx, new ServerRespond(ServerRespond.Responds.GET_FILE_INFO, ServerRespond.Results.PROCESSING));
                        break;
                }
                break;
            case AUTH:
                switch (currentState.getCurrWait()) {
                    case REQUEST:
                        break;
                    case DATA:
                        user = (User) ObjectEncoderDecoder.DecodeByteBufToObject(ctx, bb);
                        logger.info("Get user " + user.getEmail() + " | " + user.getPassword());

                        if (Database.getUser(user.getEmail(), user.getPassword()) == null) {
                            logger.info("User not found. Send FAILURE result to client");
                            send(ctx, new ServerRespond(ServerRespond.Responds.AUTH, ServerRespond.Results.FAILURE));
                            currentState.setCurrState(ServerState.State.AUTH).setCurrWait(ServerState.Wait.DATA);
                        } else {
                            logger.info("User found. Send SUCCESS result to client");
                            userStoragePath = Path.of(serverStorageName).resolve(user.getEmail());
                            if (Files.notExists(userStoragePath)) {
                                Files.createDirectory(userStoragePath);
                            }
                            getFileList(userStoragePath);
                            send(ctx, new ServerRespond(ServerRespond.Responds.AUTH, ServerRespond.Results.SUCCESS));
                            currentState.setCurrState(ServerState.State.IDLE).setCurrWait(ServerState.Wait.REQUEST);
                        }
                        break;
                }
                break;
            case REG:
                switch (currentState.getCurrWait()) {
                    case REQUEST:
                        break;
                    case DATA:
                        user = (User) ObjectEncoderDecoder.DecodeByteBufToObject(ctx, bb);
                        logger.info("Get new User " + user.getEmail() + " | " + user.getPassword());

                        if (Database.insertUser(user)) {
                            logger.info("User was inserted to db. Send SUCCESS result.");
                            send(ctx, new ServerRespond(ServerRespond.Responds.REG, ServerRespond.Results.SUCCESS));
                            currentState.setCurrState(ServerState.State.IDLE).setCurrWait(ServerState.Wait.REQUEST);
                        } else {
                            logger.info("Can't insert user to db. Send FAILURE result.");
                            send(ctx, new ServerRespond(ServerRespond.Responds.REG, ServerRespond.Results.FAILURE));
                            currentState.setCurrState(ServerState.State.REG).setCurrWait(ServerState.Wait.DATA);
                        }
                        break;
                }
                break;
            case GET:
                switch (currentState.getCurrWait()) {
                    case DATA:
                        currFileInfo = (FileInfo) ObjectEncoderDecoder.DecodeByteBufToObject(ctx, bb);
                        logger.info(String.format("get fileInfo. File - %s, size %,d.", currFileInfo.getFileFullName(), currFileInfo.getFileSize()));
                        currentState.setCurrWait(ServerState.Wait.FILE);
                        send(ctx, new ServerRespond(ServerRespond.Responds.GET_FILE_INFO, ServerRespond.Results.SUCCESS));
                        break;
                    case FILE:
                        logger.info("get file");

//                        var out = new BufferedOutputStream(new FileOutputStream(userStoragePath.resolve(currFileInfo.getFileFullName()).toString()));
//                        var receivedBytes = 0L;
//                        while (bb.readableBytes() > 0) {
//                            out.write(bb.readByte());
//                            receivedBytes++;
//                            if(receivedBytes == currFileInfo.getFileSize()){
//                                break;
//                            }
//                        }
//                        out.close();
                        try (var out = new BufferedOutputStream(new FileOutputStream(userStoragePath.resolve(currFileInfo.getFileFullName()).toString()))) {

                            logger.info("байтов к прочтению - "+ bb.readableBytes());
                            while (bb.readableBytes() > 0) {
                                out.write(bb.readByte());
                            }
                        } catch (IOException ex) {
                            ex.printStackTrace();
                            logger.info("file not got.");
                            send(ctx, new ServerRespond(ServerRespond.Responds.GET_FILE, ServerRespond.Results.FAILURE));
                            if (Files.size(new File(userStoragePath.resolve(currFileInfo.getFileFullName()).toString()).toPath()) ==
                                    currFileInfo.getFileSize()) {
                                currentState.setCurrState(ServerState.State.IDLE).setCurrWait(ServerState.Wait.REQUEST);
                                bb.release();

                            }
                            return;
                        }
                        logger.info("file successful got");
                        //send(ctx, new ServerRespond(ServerRespond.Responds.GET_FILE, ServerRespond.Results.SUCCESS));
                        // currentState.setCurrState(ServerState.State.IDLE).setCurrWait(ServerState.Wait.REQUEST);
                        break;
                }
        }
       // bb.release();
    }

    // получение и составление дерева директорий в папке downloaded
    private void getFileList(Path path) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
            stream.forEach(p -> {
                fileInfoList.add(new FileInfo(p));
                try {
                    if (Files.isDirectory(p)) getFileList(p);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    private void send(ChannelHandlerContext ctx, Sendable obj) {
        ctx.writeAndFlush(ObjectEncoderDecoder.EncodeObjToByteBuf(obj));
    }


}

