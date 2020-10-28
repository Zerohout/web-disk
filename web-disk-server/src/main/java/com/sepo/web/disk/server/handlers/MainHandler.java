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
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;

import static com.sepo.web.disk.common.helpers.MainHelper.SERVER_FOLDER_NAME;
import static com.sepo.web.disk.common.helpers.MainHelper.createFileTree;

public class MainHandler extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LogManager.getLogger(MainHandler.class);

    private BufferedOutputStream bos;
    private final MainHelper mh;

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
            if (mh.getCurrentState() == ServerEnum.State.SENDING) {
                sendFiles(bb);
            }
            if (mh.getCurrentState() == ServerEnum.State.DELETING) {
                deleteFiles(bb);
            }
            if (mh.getCurrentState() == ServerEnum.State.RENAMING) {
                renameFile(bb);
            }
            if (mh.getCurrentState() == ServerEnum.State.CREATING) {
                createFolder(bb);
            }
            if (mh.getCurrentState() == ServerEnum.State.COPYING) {
                copyFiles(bb);
            }
            if (mh.getCurrentState() == ServerEnum.State.CUTTING) {
                cutFiles(bb);
            }
        }
    }


    // распределение ByteBuff по методом в состоянии IDLE
    private void idleDistributionByMethods(ByteBuf bb) {
        if (bb.readableBytes() == 0) return;
        var req = ClientEnum.getRequestByValue(bb.readByte());
        if (req == ClientEnum.Request.REFRESH) {
            logger.info("REFRESH request");
            bb.retain().release();
            refresh();
            return;
        }
        if (req == ClientEnum.Request.GET) {
            logger.info("GET request");
            mh.setCurrentState(ServerEnum.State.GETTING);
        }
        if (req == ClientEnum.Request.SEND) {
            logger.info("SEND request");
            mh.setCurrentState(ServerEnum.State.SENDING);
        }
        if (req == ClientEnum.Request.OPERATION) {
            logger.info("OPERATION request");
            var reqType = ClientEnum.getRequestTypeByValue(bb.readByte());
            if (reqType == ClientEnum.RequestType.DELETE) {
                logger.info("\tDELETE requestType");
                mh.setCurrentState(ServerEnum.State.DELETING);
            }
            if (reqType == ClientEnum.RequestType.RENAME) {
                logger.info("\tRENAME requestType");
                mh.setCurrentState(ServerEnum.State.RENAMING);
            }
            if (reqType == ClientEnum.RequestType.CREATE) {
                logger.info("\t CREATE requestType");
                mh.setCurrentState(ServerEnum.State.CREATING);
            }
            if (reqType == ClientEnum.RequestType.COPY) {
                logger.info("\t COPY requestType");
                mh.setCurrentState(ServerEnum.State.COPYING);
            }
            if (reqType == ClientEnum.RequestType.CUT) {
                logger.info("\t CUT requestType");
                mh.setCurrentState(ServerEnum.State.CUTTING);
            }
        }
        mh.setCurrentStateWaiting(ServerEnum.StateWaiting.OBJECT_SIZE);

//        switch (ClientEnum.getRequestByValue(bb.readByte())) {
//            case REFRESH:
//                logger.info("REFRESH request");
//                bb.retain().release();
//                refresh();
//                break;
//            case GET:
//                logger.info("GET request");
//                mh.setCurrentState(ServerEnum.State.GETTING)
//                        .setCurrentStateWaiting(ServerEnum.StateWaiting.OBJECT_SIZE);
//                break;
//            case SEND:
//                logger.info("SEND request");
//                mh.setCurrentState(ServerEnum.State.SENDING)
//                        .setCurrentStateWaiting(ServerEnum.StateWaiting.OBJECT_SIZE);
//                break;
//            case OPERATION:
//                logger.info("OPERATION request");
//                switch (ClientEnum.getRequestTypeByValue(bb.readByte())) {
//                    case DELETE:
//                        logger.info("\tDELETE requestType");
//                        mh.setCurrentState(ServerEnum.State.DELETING)
//                                .setCurrentStateWaiting(ServerEnum.StateWaiting.OBJECT_SIZE);
//                        break;
//                    case RENAME:
//                        logger.info("\tRENAME requestType");
//                        mh.setCurrentState(ServerEnum.State.RENAMING)
//                                .setCurrentStateWaiting(ServerEnum.StateWaiting.OBJECT_SIZE);
//                        break;
//                    case CREATE:
//                        logger.info("\t CREATE requestType");
//                        mh.setCurrentState(ServerEnum.State.CREATING)
//                                .setCurrentStateWaiting(ServerEnum.StateWaiting.OBJECT_SIZE);
//                        break;
//                    case COPY:
//                        logger.info("\t COPY requestType");
//                        mh.setCurrentState(ServerEnum.State.COPYING)
//                                .setCurrentStateWaiting(ServerEnum.StateWaiting.OBJECT_SIZE);
//                }
//        }
    }


    //region Sending files
    private void sendFiles(ByteBuf bb) {
        logger.info("sendFiles");
        getObject(bb);

        if (mh.getCurrentStateWaiting() == ServerEnum.StateWaiting.COMPLETING) {
            var fileInfoList = (ArrayList<FileInfo>) mh.getReceivedObj();
            logger.info("send files - " + fileInfoList.size());
            for (var fileInfo : fileInfoList) {
                sendFile(fileInfo);
            }
            mh.clearStage();
        }
    }

    private void sendFile(FileInfo fileInfo) {
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
        logger.info("file is sent");
    }
    //endregion

    //region Getting file
    private void getFiles(ByteBuf bb) {
        logger.info("getFiles");
        getObject(bb);
        if (mh.getCurrentStateWaiting() == ServerEnum.StateWaiting.COMPLETING) {
            var fileInfo = (FileInfo) mh.getReceivedObj();
            mh.setCurrentState(ServerEnum.State.GETTING)
                    .setCurrentStateWaiting(ServerEnum.StateWaiting.FILE)
                    .setExpectedBytes(fileInfo.getSize());

            var path = fileInfo.getNewValue().getAbsolutePath();
            if (path.equals(SERVER_FOLDER_NAME + "\\" + fileInfo.getName())) {
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
        getObject(bb);
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
        getObject(bb);
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

    //region Creating folder
    private void createFolder(ByteBuf bb) {
        getObject(bb);
        if (mh.getCurrentStateWaiting() == ServerEnum.StateWaiting.COMPLETING) {
            var fileInfo = (FileInfo) mh.getReceivedObj();
            var path = fileInfo.getNewValue().getAbsolutePath();
            if (path.equals(SERVER_FOLDER_NAME + "\\" + fileInfo.getName())) {
                path = mh.getUserFilesPath().resolve(fileInfo.getName()).toString();
            }
            try {
                Files.createDirectory(new File(path).toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
            mh.setCurrentStateWaiting(ServerEnum.StateWaiting.REQUEST);
        }
    }
    //endregion

    //region Copying files
    private void copyFiles(ByteBuf bb){
        getObject(bb);
        if(mh.getCurrentStateWaiting() == ServerEnum.StateWaiting.COMPLETING){
            var copyingFiles = new ArrayList<>((ArrayList<FileInfo>) mh.getReceivedObj());
            for(var fileInfo : copyingFiles){
                var destination = fileInfo.getNewValue().getAbsolutePath();
                if (destination.equals(SERVER_FOLDER_NAME + "\\")) {
                    destination = mh.getUserFilesPath().toString();
                }
                try {
                    Files.copy(Path.of(fileInfo.getAbsolutePath()),
                            Path.of(destination + "\\"+ fileInfo.getName()),
                            StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            refresh();
            mh.setCurrentStateWaiting(ServerEnum.StateWaiting.REQUEST);
        }
    }
    //endregion

    //region Cutting files
    private void cutFiles(ByteBuf bb){
        getObject(bb);
        if(mh.getCurrentStateWaiting() == ServerEnum.StateWaiting.COMPLETING){
            var copyingFiles = new ArrayList<>((ArrayList<FileInfo>) mh.getReceivedObj());
            for(var fileInfo : copyingFiles){
                var destination = fileInfo.getNewValue().getAbsolutePath();
                if (destination.equals(SERVER_FOLDER_NAME + "\\")) {
                    destination = mh.getUserFilesPath().toString();
                }
                try {
                    Files.move(Path.of(fileInfo.getAbsolutePath()),
                            Path.of(destination + "\\"+ fileInfo.getName()),
                            StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            refresh();
            mh.setCurrentStateWaiting(ServerEnum.StateWaiting.REQUEST);
        }
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
    private void getObject(ByteBuf bb) {
        if (mh.getCurrentStateWaiting() == ServerEnum.StateWaiting.OBJECT_SIZE) {
            mh.setObjectSize(bb);
        }
        if (mh.getCurrentStateWaiting() == ServerEnum.StateWaiting.OBJECT) {
            mh.setObject(bb);
        }
    }
    //endregion

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}

