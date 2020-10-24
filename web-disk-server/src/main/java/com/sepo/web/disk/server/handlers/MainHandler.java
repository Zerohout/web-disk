package com.sepo.web.disk.server.handlers;

import com.sepo.web.disk.common.helpers.MainHelper;
import com.sepo.web.disk.common.models.*;
import com.sepo.web.disk.common.service.ObjectEncoderDecoder;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;

public class MainHandler extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LogManager.getLogger(MainHandler.class);
    private ServerEnum.State currentState = ServerEnum.State.IDLE;
    private ServerEnum.StateWaiting currentStateWaiting = ServerEnum.StateWaiting.REQUEST;
    private ClientEnum.Request request;
    private ClientEnum.RequestType requestType;
    private long bytesReceived;
    private long bytesExpected;
    ByteBuf accumulator;
    BufferedOutputStream bos;

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
        var dir = new Folder(new FileInfo(userFilesPath), MainHelper.SERVER_FOLDER_NAME);
        MainHelper.createFileTree(userFilesPath, dir, MainHelper.SERVER_FOLDER_NAME);
        var dirBytes = ObjectEncoderDecoder.convertObjectToByteArray(dir);
        var bb = ObjectEncoderDecoder.EncodeByteArraysToByteBuf(dirBytes);
        ctx.writeAndFlush(bb);
        logger.info("refreshing");
    }

    private void getFiles(ByteBuf bb) {
        if (currentStateWaiting == ServerEnum.StateWaiting.TRANSFER) {
            if (bytesReceived == 0L && bytesExpected == 0L) {
                logger.info("reading size of fileInfo. byteBuff size - " + bb.readableBytes());
                bytesExpected = bb.readInt();
                logger.info("size of fileInfo is got - " + bytesExpected + ". byteBuf size - " + bb.readableBytes());
            }
            if (bytesExpected > 0L) {
                logger.info("getting fileInfo. bytes received - " + bytesReceived + ", accum size - " + accumulator.readableBytes());
                while (bb.readableBytes() > 0 && bytesReceived != bytesExpected) {
                    accumulator.writeByte(bb.readByte());
                    bytesReceived++;
                }
                if (bytesReceived == bytesExpected) {
                    var fileInfo = (FileInfo) ObjectEncoderDecoder.DecodeByteBufToObject(accumulator);
                    logger.info("fileInfo is got. fileinfo name - " + fileInfo.getName());
                    currentStateWaiting = ServerEnum.StateWaiting.FILE;
                    bytesReceived = 0L;
                    bytesExpected = fileInfo.getSize();
                    logger.info("file size - " + bytesExpected);
                    accumulator.retain().release();
                    try {
                        bos = new BufferedOutputStream(new FileOutputStream(userFilesPath.resolve(fileInfo.getName()).toString()));
                        logger.info(userFilesPath.resolve(fileInfo.getName()).toString());
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }

        }
        if (currentStateWaiting == ServerEnum.StateWaiting.FILE) {
            try {
                logger.info("bb - " + bb.readableBytes() + ", received - " + bytesReceived + ", expected - " + bytesExpected);
                while (bb.readableBytes() > 0 && bytesReceived < bytesExpected) {
                    bos.write(bb.readByte());
                    bytesReceived++;
                }
                logger.info("bb - " + bb.readableBytes() + ", received - " + bytesReceived + ", expected - " + bytesExpected);
                if (bytesReceived == bytesExpected) {
                    bos.close();
                    bytesReceived = 0L;
                    bytesExpected = 0L;
                    if(bb.readableBytes() > 0){
                        request = ClientEnum.getRequestByValue(bb.readByte());
                        if(request == ClientEnum.Request.GET) {
                            currentState = ServerEnum.State.GETTING;
                            currentStateWaiting = ServerEnum.StateWaiting.TRANSFER;
                            getFiles(bb);
                        }else{
                            refresh();
                        }
                        return;
                    }
                    currentState = ServerEnum.State.IDLE;
                    currentStateWaiting = ServerEnum.StateWaiting.REQUEST;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (bb.readableBytes() == 0) bb.release();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (this.ctx == null) this.ctx = ctx;
        if (accumulator == null) accumulator = ctx.alloc().buffer(1024 * 1024, 1024 * 1024 * 25);

        var bb = (ByteBuf) msg;
        if (currentState == ServerEnum.State.IDLE) {
            logger.info("get request");
            request = ClientEnum.getRequestByValue(bb.readByte());

            if (request == ClientEnum.Request.REFRESH) {
                bb.release();
                refresh();
                currentState = ServerEnum.State.IDLE;
                currentStateWaiting = ServerEnum.StateWaiting.REQUEST;
            }
            if (request == ClientEnum.Request.GET) {
                currentState = ServerEnum.State.GETTING;
                currentStateWaiting = ServerEnum.StateWaiting.TRANSFER;
            }
        }
        if (currentState == ServerEnum.State.GETTING) {
            getFiles(bb);
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
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        logger.info("Channel read complete");
        if (currentState == ServerEnum.State.IDLE || currentState == ServerEnum.State.GETTING) return;
         currentStateWaiting = ServerEnum.StateWaiting.COMPLETING;
        //if (currentState == ServerEnum.State.) getFiles(null);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

}

