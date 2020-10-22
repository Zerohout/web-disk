package com.sepo.web.disk.server.handlers;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import com.sepo.web.disk.common.models.*;
import com.sepo.web.disk.common.service.*;
import com.sepo.web.disk.server.helpers.MainHelper;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@ChannelHandler.Sharable
public class MainHandler extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LogManager.getLogger(MainHandler.class);
    private ServerEnum.State currentState = ServerEnum.State.IDLE;
    private ServerEnum.StateWaiting currentStateWaiting = ServerEnum.StateWaiting.REQUEST;
    private ClientEnum.Request request;
    private ClientEnum.RequestType requestType;
    ChannelHandlerContext ctx;
    Path userFilesPath;

    public MainHandler(Path userFilesPath) {
        this.userFilesPath = userFilesPath;
    }

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
        logger.info("Channel Registered");
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        logger.info("Channel Unregistered");
    }

    private void refresh() {
        var dir = new Folder(new FileInfo(userFilesPath));
        MainHelper.createFileTree(userFilesPath, dir);
        var dirBytes = ObjectEncoderDecoder.convertObjectToByteArray(dir);
        var bb = ObjectEncoderDecoder.EncodeByteArraysToByteBuf(dirBytes);
        ctx.writeAndFlush(bb);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (this.ctx == null) this.ctx = ctx;
        if (currentState == ServerEnum.State.IDLE) {
            var bb = (ByteBuf) msg;
            logger.info("get request");
            request = ClientEnum.getRequestByValue(bb.readByte());

            if (request == ClientEnum.Request.REFRESH) {
                bb.release();
                refresh();
                currentState = ServerEnum.State.IDLE;
                currentStateWaiting = ServerEnum.StateWaiting.REQUEST;
            }

        }


//        // Главная логика обмена сообщениями c сервером
//        switch (currentState.getCurrState()) {
//            case IDLE:
//                var request = (ClientRequest) ObjectEncoderDecoder.DecodeByteBufToObject(ctx, bb);
//
//                switch (request.getRequest()) {
//                    case UPDATE:
//                        logger.info("getting UPDATE request");
//                        if (currFileInfoIndex < fileInfoList.size()) {
//                            logger.info(String.format("Send fileInfo %d/%d", (currFileInfoIndex + 1), fileInfoList.size()));
//                            ctx.writeAndFlush(ObjectEncoderDecoder.EncodeObjToByteBuf(fileInfoList.get(currFileInfoIndex)));
//                            currFileInfoIndex++;
//
//                        } else {
//                            logger.info("send respond");
//                            send(ctx, new ServerRespond(ServerRespond.Responds.UPDATE, ServerRespond.Results.SUCCESS));
//                            currentState.setCurrState(ServerState.State.IDLE).setCurrWait(ServerState.Wait.REQUEST);
//                        }
//                        break;
//                    case SEND:
//                        logger.info("getting SEND request");
//                        currentState.setCurrState(ServerState.State.GET).setCurrWait(ServerState.Wait.DATA);
//                        send(ctx, new ServerRespond(ServerRespond.Responds.GET_FILE_INFO, ServerRespond.Results.PROCESSING));
//                        break;
//                }
//                break;
//            case GET:
//                switch (currentState.getCurrWait()) {
//                    case DATA:
//                        currFileInfo = (FileInfo) ObjectEncoderDecoder.DecodeByteBufToObject(ctx, bb);
//                        logger.info(String.format("get fileInfo. File - %s, size %,d.", currFileInfo.getFileFullName(), currFileInfo.getFileSize()));
//                        currentState.setCurrWait(ServerState.Wait.FILE);
//                        send(ctx, new ServerRespond(ServerRespond.Responds.GET_FILE_INFO, ServerRespond.Results.SUCCESS));
//                        break;
//                    case FILE:
//                        logger.info("get file");
//
////                        var out = new BufferedOutputStream(new FileOutputStream(userStoragePath.resolve(currFileInfo.getFileFullName()).toString()));
////                        var receivedBytes = 0L;
////                        while (bb.readableBytes() > 0) {
////                            out.write(bb.readByte());
////                            receivedBytes++;
////                            if(receivedBytes == currFileInfo.getFileSize()){
////                                break;
////                            }
////                        }
////                        out.close();
//                        try (var out = new BufferedOutputStream(new FileOutputStream(userStoragePath.resolve(currFileInfo.getFileFullName()).toString()))) {
//
//                            logger.info("байтов к прочтению - "+ bb.readableBytes());
//                            while (bb.readableBytes() > 0) {
//                                out.write(bb.readByte());
//                            }
//                        } catch (IOException ex) {
//                            ex.printStackTrace();
//                            logger.info("file not got.");
//                            send(ctx, new ServerRespond(ServerRespond.Responds.GET_FILE, ServerRespond.Results.FAILURE));
//                            if (Files.size(new File(userStoragePath.resolve(currFileInfo.getFileFullName()).toString()).toPath()) ==
//                                    currFileInfo.getFileSize()) {
//                                currentState.setCurrState(ServerState.State.IDLE).setCurrWait(ServerState.Wait.REQUEST);
//                                bb.release();
//
//                            }
//                            return;
//                        }
//                        logger.info("file successful got");
//                        //send(ctx, new ServerRespond(ServerRespond.Responds.GET_FILE, ServerRespond.Results.SUCCESS));
//                        // currentState.setCurrState(ServerState.State.IDLE).setCurrWait(ServerState.Wait.REQUEST);
//                        break;
//                }
//        }
        // bb.release();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

}

