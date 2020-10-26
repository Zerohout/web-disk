package com.sepo.web.disk.server.handlers;

import com.sepo.web.disk.common.models.*;
import com.sepo.web.disk.common.service.ObjectEncoderDecoder;
import com.sepo.web.disk.server.helpers.MainHelper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.DefaultFileRegion;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import static com.sepo.web.disk.common.helpers.MainHelper.SERVER_FOLDER_NAME;
import static com.sepo.web.disk.common.helpers.MainHelper.createFileTree;

public class MainHandler extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LogManager.getLogger(MainHandler.class);

    private BufferedOutputStream bos;
    private MainHelper mh;

    public MainHandler(MainHelper mh) {
        this.mh = mh;
        this.mh.clearStage();
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
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        logger.info("channelRead()");

        var bb = (ByteBuf) msg;
        while (bb.readableBytes() > 0) {
            if (mh.getCurrentState() == ServerEnum.State.IDLE) {
                idleDistributionByMethods(bb);
            }
            if (mh.getCurrentState() == ServerEnum.State.GETTING) {
                getFiles(bb);
            }
            if(mh.getCurrentState() == ServerEnum.State.SENDING){
                sendFiles(bb);
            }
            if (mh.getCurrentState() == ServerEnum.State.DELETING) {
                deleteFiles(bb);
            }
            if (mh.getCurrentState() == ServerEnum.State.RENAMING) {
                renameFile(bb);
            }
        }
    }

    // распределение ByteBuff по методом в состоянии IDLE
    private void idleDistributionByMethods(ByteBuf bb) {
        if (bb.readableBytes() == 0) return;
        switch (ClientEnum.getRequestByValue(bb.readByte())) {
            case REFRESH:
                logger.info("REFRESH request");
                bb.retain().release();
                refresh();
                break;
            case GET:
                logger.info("GET request");
                mh.setCurrentState(ServerEnum.State.GETTING)
                        .setCurrentStateWaiting(ServerEnum.StateWaiting.OBJECT_SIZE);
                break;
            case SEND:
                logger.info("SEND request");
                mh.setCurrentState(ServerEnum.State.SENDING)
                        .setCurrentStateWaiting(ServerEnum.StateWaiting.OBJECT_SIZE);
                break;
            case OPERATION:
                logger.info("OPERATION request");
                switch (ClientEnum.getRequestTypeByValue(bb.readByte())) {
                    case DELETE:
                        logger.info("\tDELETE requestType");
                        mh.setCurrentState(ServerEnum.State.DELETING)
                                .setCurrentStateWaiting(ServerEnum.StateWaiting.OBJECT_SIZE);
                        break;
                    case RENAME:
                        logger.info("\tRENAME requestType");
                        mh.setCurrentState(ServerEnum.State.RENAMING)
                                .setCurrentStateWaiting(ServerEnum.StateWaiting.OBJECT_SIZE);
                        break;
                }
        }
    }

    //region Sending files
    private void sendFiles(ByteBuf bb){
        logger.info("sendFiles");
        if(mh.getCurrentStateWaiting() == ServerEnum.StateWaiting.OBJECT_SIZE){
            mh.setObjectSize(bb);
        }
        if(mh.getCurrentStateWaiting() == ServerEnum.StateWaiting.OBJECT){
            mh.setObject(bb);
        }

        if(mh.getCurrentStateWaiting() == ServerEnum.StateWaiting.COMPLETING){
            var fileInfoList = (ArrayList<FileInfo>) mh.getReceivedObj();
            logger.info("send files - "+fileInfoList.size());
            for(var fileInfo : fileInfoList){
                sendFile(fileInfo);
            }
        }
    }

    private void sendFile(FileInfo fileInfo){
        //mh.sendRespond(ServerEnum.Respond.GET);
        var newFI = new FileInfo(Path.of(fileInfo.getAbsolutePath()));
        newFI.setNewValue(fileInfo.getNewValue());
        var msg = ObjectEncoderDecoder.EncodeObjToByteBuf(newFI);
        var msgSize = msg.readableBytes();
        var msgSizeBB = ByteBufAllocator.DEFAULT.directBuffer(4);
        msgSizeBB.writeInt(msgSize);
        mh.getCtx().write(msgSizeBB);
        mh.getCtx().writeAndFlush(msg);
        var region = new DefaultFileRegion(newFI.getPath().toFile(), 0, newFI.getSize());
        mh.getCtx().writeAndFlush(region);
    }

    //endregion

    //region Getting file
    private void getFiles(ByteBuf bb) {
        logger.info("getFiles");
        if (mh.getCurrentStateWaiting() == ServerEnum.StateWaiting.OBJECT_SIZE) {
            mh.setObjectSize(bb);
        }
        if (mh.getCurrentStateWaiting() == ServerEnum.StateWaiting.OBJECT) {
            mh.setObject(bb);
        }
        if (mh.getCurrentStateWaiting() == ServerEnum.StateWaiting.COMPLETING) {
            var fileInfo = (FileInfo) mh.getReceivedObj();
            mh.setCurrentState(ServerEnum.State.GETTING)
                    .setCurrentStateWaiting(ServerEnum.StateWaiting.FILE)
                    .setExpectedBytes(fileInfo.getSize());

            var path = fileInfo.getNewValue().getAbsolutePath();
            if (path.equals(SERVER_FOLDER_NAME)) {
                path = mh.getUserFilesPath().resolve(fileInfo.getName()).toString();
            }
            try {
                bos = new BufferedOutputStream(new FileOutputStream(path));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        if (mh.getCurrentStateWaiting() == ServerEnum.StateWaiting.FILE) {
            try {
                gettingFile(bb);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (bb.readableBytes() == 0) bb.release();
    }

    private void gettingFile(ByteBuf bb) throws IOException {
        logger.info("gettingFile");
        while (bb.readableBytes() > 0 && mh.getReceivedBytes() != mh.getExpectedBytes()) {
            bos.write(bb.readByte());
            mh.incrementReceivedBytes();
        }
        if (mh.getReceivedBytes() == mh.getExpectedBytes()) {
            bos.close();
            mh.clearStage();
            idleDistributionByMethods(bb);
        }
    }
    //endregion

    //region Deleting files
    private void deleteFiles(ByteBuf bb) {
        if (mh.getCurrentStateWaiting() == ServerEnum.StateWaiting.OBJECT_SIZE) {
            mh.setObjectSize(bb);
        }
        if (mh.getCurrentStateWaiting() == ServerEnum.StateWaiting.OBJECT) {
            mh.setObject(bb);
        }

        if (mh.getCurrentStateWaiting() == ServerEnum.StateWaiting.COMPLETING) {
            var deletingList = new ArrayList<>((ArrayList<FileInfo>) mh.getReceivedObj());
            for (var fileInfo : deletingList) {
                try {
                    Files.delete(Path.of(fileInfo.getAbsolutePath()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            idleDistributionByMethods(bb);
        }

        if (bb.readableBytes() == 0) bb.release();
    }
    //endregion

    //region Renaming file
    private void renameFile(ByteBuf bb) {
        if (mh.getCurrentStateWaiting() == ServerEnum.StateWaiting.OBJECT_SIZE) {
            mh.setObjectSize(bb);
        }
        if (mh.getCurrentStateWaiting() == ServerEnum.StateWaiting.OBJECT) {
            mh.setObject(bb);
        }
        if (mh.getCurrentStateWaiting() == ServerEnum.StateWaiting.COMPLETING) {
            var fileInfo = (FileInfo) mh.getReceivedObj();
            var oldFile = new File(fileInfo.getAbsolutePath());

            var result = oldFile.renameTo(new File(fileInfo.getNewValue().getAbsolutePath()))
                    ? ServerEnum.Respond.SUCCESS
                    : ServerEnum.Respond.FAILURE;
            mh.sendRespond(result);
            idleDistributionByMethods(bb);
        }

        if (bb.readableBytes() == 0) bb.release();
    }
    //endregion

    //region Refreshing
    private void refresh() {
        var dir = new Folder(new FileInfo(mh.getUserFilesPath()), SERVER_FOLDER_NAME);
        createFileTree(mh.getUserFilesPath(), dir, SERVER_FOLDER_NAME);
        var dirBytes = ObjectEncoderDecoder.convertObjectToByteArray(dir);
        var bb = ObjectEncoderDecoder.EncodeByteArraysToByteBuf(dirBytes);
        mh.getCtx().writeAndFlush(bb);
        mh.clearStage();
    }
    //endregion

    //region Service methods

    //endregion

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}

