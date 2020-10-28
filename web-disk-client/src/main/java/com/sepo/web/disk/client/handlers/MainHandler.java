package com.sepo.web.disk.client.handlers;

import com.sepo.web.disk.client.Helpers.MainBridge;
import com.sepo.web.disk.client.network.Network;
import com.sepo.web.disk.common.models.ClientEnum;
import com.sepo.web.disk.common.models.FileInfo;
import com.sepo.web.disk.common.models.Folder;
import com.sepo.web.disk.common.models.ServerEnum;
import com.sepo.web.disk.common.service.ObjectEncoderDecoder;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCounted;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainHandler extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LogManager.getLogger(MainHandler.class);
    private ClientEnum.State currentState = ClientEnum.State.IDLE;
    private ClientEnum.StateWaiting currentStateWaiting = ClientEnum.StateWaiting.NOTHING;
    private ByteBuf accumulator;
    private ChannelHandlerContext ctx;
    private long receivedBytes;
    private long expectedBytes;
    private FileInfo receivedFileInfo;
    private BufferedOutputStream bos;
    private int gettingFilesCount;


//    private OnActionCallback callback;

    public MainHandler(ChannelHandlerContext ctx) {
        logger.info("mainHandler created");
        Network.mainHandler = this;
        this.ctx = ctx;
        accumulator = ctx.alloc().buffer(1024 * 1024, 1024 * 1024 * 25);
    }

    private void refreshing(ByteBuf bb) {
        if (currentStateWaiting == ClientEnum.StateWaiting.TRANSFER) {
            logger.info("заливаем в аккум");
            if (bb.readableBytes() > 0) accumulator.writeBytes(bb);
            bb.release();
        }
        if (currentStateWaiting == ClientEnum.StateWaiting.COMPLETING) {
            logger.info("завершаем операцию");
            var folder = (Folder) ObjectEncoderDecoder.DecodeByteBufToObject(accumulator);
            accumulator.retain().release();
            currentState = ClientEnum.State.IDLE;
            currentStateWaiting = ClientEnum.StateWaiting.NOTHING;
            MainBridge.refreshServerFiles(folder);
        }
    }

    public void setState(ClientEnum.State state, ClientEnum.StateWaiting stateWaiting) {
        currentState = state;
        currentStateWaiting = stateWaiting;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        logger.info("got message");
        logger.info(currentState+", "+currentStateWaiting);
        var bb = (ByteBuf) msg;
        while (bb.readableBytes() > 0) {
            if (currentState == ClientEnum.State.REFRESHING) {
                refreshing(bb);
            }
            if (currentState == ClientEnum.State.IDLE && currentStateWaiting == ClientEnum.StateWaiting.RESULT) {
                logger.info("got result");
                var respond = ServerEnum.getRespondByValue(bb.readByte());
                MainBridge.refreshClientFiles();
                bb.release();
                currentStateWaiting = ClientEnum.StateWaiting.NOTHING;
            }
            if (currentState == ClientEnum.State.GETTING) {

                logger.info("GETTING");
                if (currentStateWaiting == ClientEnum.StateWaiting.OBJECT_SIZE) {
                    getFileInfoSize(bb);
                }
                if (currentStateWaiting == ClientEnum.StateWaiting.OBJECT) {
                    getFileInfo(bb);
                }
                if (currentStateWaiting == ClientEnum.StateWaiting.FILE) {
                    getServerFile(bb);
                }
            }
            logger.info(bb.readableBytes());
        }
        if(bb.readableBytes() == 0) bb.release();
    }

    private void getFileInfoSize(ByteBuf bb) {
        logger.info("getting obj size, expectedBytes - "+expectedBytes);
        if (expectedBytes == 0L) expectedBytes = 4L;
        while (bb.readableBytes() > 0 && receivedBytes != expectedBytes) {
            accumulator.writeByte(bb.readByte());
            receivedBytes++;
        }
        if (receivedBytes == expectedBytes) {
            expectedBytes = accumulator.readInt();
            logger.info("expected bytes - "+expectedBytes);
            accumulator.retain().release();
            receivedBytes = 0L;
            currentStateWaiting = ClientEnum.StateWaiting.OBJECT;
        }
    }

    private void getFileInfo(ByteBuf bb) {
        logger.info("getting obj");
        while (bb.readableBytes() > 0 && receivedBytes != expectedBytes) {
            accumulator.writeByte(bb.readByte());
            receivedBytes++;
        }
        if (receivedBytes == expectedBytes) {
            receivedFileInfo = (FileInfo) ObjectEncoderDecoder.DecodeByteBufToObject(accumulator);
            expectedBytes = receivedFileInfo.getSize();
            receivedBytes = 0L;
            accumulator.retain().release();
            currentStateWaiting = ClientEnum.StateWaiting.FILE;
            try {
                bos = new BufferedOutputStream(new FileOutputStream(receivedFileInfo.getNewValue().getAbsolutePath()));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private void getServerFile(ByteBuf bb) throws IOException {
        while (bb.readableBytes() > 0 && receivedBytes != expectedBytes) {
            bos.write(bb.readByte());
            receivedBytes++;
        }
        if (receivedBytes == expectedBytes) {
            gettingFilesCount--;
            bos.close();
            if (gettingFilesCount == 0) {
                logger.info("all files received");
                currentState = ClientEnum.State.IDLE;
                currentStateWaiting = ClientEnum.StateWaiting.NOTHING;
                MainBridge.refreshClientFiles();
            } else {
                logger.info("file received, remained - "+gettingFilesCount);
                currentStateWaiting = ClientEnum.StateWaiting.OBJECT_SIZE;
            }
            receivedBytes = 0L;
            expectedBytes = 0L;
        }
    }


    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        logger.info("got message complete");
        if (currentState == ClientEnum.State.REFRESHING) {
            currentStateWaiting = ClientEnum.StateWaiting.COMPLETING;
            refreshing(null);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {

    }

    public void sendRequest(ClientEnum.Request request, ClientEnum.RequestType requestType) {
        if (requestType == null) {
            byte[] reqArr = new byte[1];
            reqArr[0] = request.getValue();
            ctx.writeAndFlush(ObjectEncoderDecoder.EncodeByteArraysToByteBuf(reqArr));
        }
    }

    public void send(ReferenceCounted bb, boolean isFlush) {
        if (isFlush) {
            ctx.writeAndFlush(bb);
        } else {
            ctx.write(bb);
        }
    }

    public void setGettingFilesCount(int gettingFilesCount) {
        this.gettingFilesCount = gettingFilesCount;
    }
}
