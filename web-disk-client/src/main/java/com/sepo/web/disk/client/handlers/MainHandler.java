package com.sepo.web.disk.client.handlers;

import com.sepo.web.disk.client.Helpers.MainBridge;
import com.sepo.web.disk.client.network.Network;
import com.sepo.web.disk.common.models.ClientEnum;
import com.sepo.web.disk.common.models.Folder;
import com.sepo.web.disk.common.models.ServerEnum;
import com.sepo.web.disk.common.service.ObjectEncoderDecoder;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCounted;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MainHandler extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LogManager.getLogger(MainHandler.class);
    private ClientEnum.State currentState = ClientEnum.State.IDLE;
    private ClientEnum.StateWaiting currentStateWaiting = ClientEnum.StateWaiting.NOTHING;
    private ByteBuf accumulator;
    private ChannelHandlerContext ctx;
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
        if (currentState == ClientEnum.State.REFRESHING) {
            refreshing((ByteBuf) msg);
        }
        if(currentState == ClientEnum.State.IDLE && currentStateWaiting == ClientEnum.StateWaiting.RESULT){
        logger.info("got result");
            var respond = ServerEnum.getRespondByValue(((ByteBuf)msg).readByte());
            MainBridge.giveRenameResult(respond);
            ((ByteBuf)msg).release();
            currentStateWaiting = ClientEnum.StateWaiting.NOTHING;
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

//    @Override
//    public void callback(Object... args) {
//        if (args.length == 2) {
//            logger.info(args[0].getClass());
//            logger.info(args[1].getClass());
//
//            if(args[0] instanceof ArrayList){
//                var filesInfo = new ArrayList<>((ArrayList<FileInfo>)args[0]);
//                var files = new ArrayList<>((ArrayList<File>)args[1]);
//            }
//        }
//    }


}
