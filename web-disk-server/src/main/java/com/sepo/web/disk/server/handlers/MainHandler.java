package com.sepo.web.disk.server.handlers;

import com.sepo.web.disk.common.helpers.MainHelper;
import com.sepo.web.disk.common.models.*;
import com.sepo.web.disk.common.service.ObjectEncoderDecoder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

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
        setStateToIdle();
    }

    //region Getting file

    private void getFiles(ByteBuf bb) {
        logger.info("getFiles");
        if (currentStateWaiting == ServerEnum.StateWaiting.TRANSFER) {
            if (bytesExpected == 0L) {
                gettingFileInfoSize(bb);
            }
            if (bytesExpected > 0L) {
                gettingFileInfo(bb);
            }
        }
        if (currentStateWaiting == ServerEnum.StateWaiting.FILE) {
            try {
                gettingFile(bb);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (bb.readableBytes() == 0) bb.release();
    }

    private void gettingFileInfoSize(ByteBuf bb) {
        logger.info("gettingFileInfoSize");
        // предохранитель в случае, если int не полностью пришел
        while (bb.readableBytes() > 0 && bytesReceived < 4L) {
            accumulator.writeByte(bb.readByte());
            bytesReceived++;
        }
        if (bytesReceived == 4L) {
            bytesExpected = accumulator.readInt();
            accumulator.retain().release();
            bytesReceived = 0L;
        }
    }

    private void gettingFileInfo(ByteBuf bb) {
        logger.info("gettingFileInfo");
        while (bb.readableBytes() > 0 && bytesReceived != bytesExpected) {
            accumulator.writeByte(bb.readByte());
            bytesReceived++;
        }
        if (bytesReceived == bytesExpected) {
            var fileInfo = (FileInfo) ObjectEncoderDecoder.DecodeByteBufToObject(accumulator);
            currentStateWaiting = ServerEnum.StateWaiting.FILE;
            bytesExpected = fileInfo.getSize();
            accumulator.retain().release();
            bytesReceived = 0L;
            try {
                bos = new BufferedOutputStream(new FileOutputStream(userFilesPath.resolve(fileInfo.getName()).toString()));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private void gettingFile(ByteBuf bb) throws IOException {
        logger.info("gettingFile");
        while (bb.readableBytes() > 0 && bytesReceived < bytesExpected) {
            bos.write(bb.readByte());
            bytesReceived++;
        }
        if (bytesReceived == bytesExpected) {
            bos.close();
            bytesReceived = 0L;
            bytesExpected = 0L;
            setStateToIdle();
            if (bb.readableBytes() > 0) {
                idleDistributionByMethods(bb);
            }

                setStateToIdle();

        }
    }
    //endregion

    private void deleteFiles(ByteBuf bb) {
        if (currentStateWaiting == ServerEnum.StateWaiting.TRANSFER) {
            if (bytesExpected == 0L) {
                while (bytesReceived < 4L && bb.readableBytes() > 0) {
                    accumulator.writeByte(bb.readByte());
                    bytesReceived++;
                }
                if (bytesReceived == 4L) {
                    bytesExpected = accumulator.readInt();
                    accumulator.retain().release();
                    bytesReceived = 0L;
                }
            }
            if (bytesExpected > 0) {
                while (bytesReceived != bytesExpected && bb.readableBytes() > 0) {
                    accumulator.writeByte(bb.readByte());
                    bytesReceived++;
                }
                if (bytesReceived == bytesExpected) {
                    var deletingList = new ArrayList<>((ArrayList<FileInfo>) ObjectEncoderDecoder.DecodeByteBufToObject(accumulator));
                    accumulator.retain().release();
                    bytesReceived = 0L;
                    bytesExpected = 0L;
                    for (var fileInfo : deletingList) {
                        try {
                            Files.delete(Path.of(fileInfo.getAbsolutePath()));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (bb.readableBytes() > 0) {
                        idleDistributionByMethods(bb);
                    }
                    setStateToIdle();


                }
            }
        }
        if (bb.readableBytes() == 0) bb.release();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (this.ctx == null) this.ctx = ctx;
        if (accumulator == null) accumulator = ctx.alloc().buffer(1024 * 1024, 1024 * 1024 * 25);
        if (msg == null) return;

        var bb = (ByteBuf) msg;
        if (bb.readableBytes() == 0) return;
        if (currentState == ServerEnum.State.IDLE) {
            idleDistributionByMethods(bb);
        }
        if (bb.readableBytes() == 0) return;
        if (currentState == ServerEnum.State.GETTING) {
            getFiles(bb);
        }
        if (currentState == ServerEnum.State.DELETING) {
            deleteFiles(bb);
        }
        if (currentState == ServerEnum.State.RENAMING) {
            renameFile(bb);
        }

    }

    private void renameFile(ByteBuf bb) {
        if (currentStateWaiting == ServerEnum.StateWaiting.TRANSFER) {
            if (bytesExpected == 0L) {
                while (bytesReceived < 4L && bb.readableBytes() > 0) {
                    accumulator.writeByte(bb.readByte());
                    bytesReceived++;
                }
                if (bytesReceived == 4L) {
                    bytesExpected = accumulator.readInt();
                    logger.info("expected - "+ bytesExpected);
                    accumulator.retain().release();
                    bytesReceived = 0L;
                }
            }
            if (bytesExpected > 0L) {
                while (bb.readableBytes() > 0 && bytesReceived != bytesExpected) {
                    accumulator.writeByte(bb.readByte());
                    bytesReceived++;
                }
                if (bytesReceived == bytesExpected) {
                    var fileInfo = (FileInfo) ObjectEncoderDecoder.DecodeByteBufToObject(accumulator);
                    logger.info("fileInfo - "+fileInfo.getAbsolutePath() + ", new value - "+fileInfo.getNewValue().getAbsolutePath());
                    accumulator.retain().release();
                    bytesReceived = 0L;
                    bytesExpected = 0L;
                    setStateToIdle();
                    var oldFile = new File(fileInfo.getAbsolutePath());
                    var newFile = new File(fileInfo.getNewValue().getAbsolutePath());
                    ByteBuf result = ByteBufAllocator.DEFAULT.directBuffer(1);
                    if(oldFile.renameTo(newFile)){
                        logger.info("success renaming");
                        result.writeByte(ServerEnum.Respond.SUCCESS.getValue());
                    }else{
                        logger.info("failure renaming");
                        result.writeByte(ServerEnum.Respond.FAILURE.getValue());
                    }
                    ctx.writeAndFlush(result);
                    if(bb.readableBytes() == 0) bb.release();
                }
            }
        }
    }

    private void setStateToIdle() {
        if (currentState != ServerEnum.State.IDLE && currentStateWaiting != ServerEnum.StateWaiting.REQUEST) {
            currentState = ServerEnum.State.IDLE;
            currentStateWaiting = ServerEnum.StateWaiting.REQUEST;
        }
    }

    // распределение ByteBuff по методом в состоянии IDLE
    private void idleDistributionByMethods(ByteBuf bb) {
        if(bb == null || bb.readableBytes() == 0)return;
        switch (ClientEnum.getRequestByValue(bb.readByte())) {
            case REFRESH:
                logger.info("REFRESH request");
                bb.retain().release();
                refresh();
                break;
            case GET:
                logger.info("GET request");
                currentState = ServerEnum.State.GETTING;
                currentStateWaiting = ServerEnum.StateWaiting.TRANSFER;
                break;
            case OPERATION:
                logger.info("OPERATION request");
                switch (ClientEnum.getRequestTypeByValue(bb.readByte())) {
                    case DELETE:
                        logger.info("\tDELETE requestType");
                        currentState = ServerEnum.State.DELETING;
                        currentStateWaiting = ServerEnum.StateWaiting.TRANSFER;
                        break;
                    case RENAME:
                        logger.info("\tRENAME requestType");
                        currentState = ServerEnum.State.RENAMING;
                        currentStateWaiting = ServerEnum.StateWaiting.TRANSFER;
                        break;
                }
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

}

