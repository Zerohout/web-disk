package com.sepo.web.disk.client.handlers;

import com.sepo.web.disk.client.Helpers.OnActionCallback;
import com.sepo.web.disk.client.network.Network;
import com.sepo.web.disk.common.models.ClientEnum;
import com.sepo.web.disk.common.models.Folder;
import com.sepo.web.disk.common.service.ObjectEncoderDecoder;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MainHandler extends ChannelInboundHandlerAdapter implements OnActionCallback {
    private static final Logger logger = LogManager.getLogger(MainHandler.class);
    private ClientEnum.State currentState = ClientEnum.State.IDLE;
    private ClientEnum.StateWaiting currentStateWaiting = ClientEnum.StateWaiting.NOTHING;
    private ByteBuf accumulator;
    private ChannelHandlerContext ctx;
    private OnActionCallback callback;

    public MainHandler(ChannelHandlerContext ctx) {
        logger.info("mainHandler created");
        Network.mainHandler = this;
        this.ctx = ctx;
        accumulator = ctx.alloc().buffer(1024 * 1024, 1024 * 1024 * 25);
    }

    private void refreshing(ByteBuf bb) {
        if (currentStateWaiting == ClientEnum.StateWaiting.TRANSFER){
            logger.info("заливаем в аккум");
            if(bb.readableBytes() > 0) accumulator.writeBytes(bb);
            bb.release();
        }
        if(currentStateWaiting == ClientEnum.StateWaiting.COMPLETING){
            logger.info("завершаем операцию");
            var folder = (Folder)ObjectEncoderDecoder.DecodeByteBufToObject(accumulator);
            accumulator.retain().release();
            currentState = ClientEnum.State.IDLE;
            currentStateWaiting = ClientEnum.StateWaiting.NOTHING;
            callback.callback(folder);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        logger.info("got message");
        if (currentState == ClientEnum.State.REFRESHING) {
            refreshing((ByteBuf) msg);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        logger.info("got message complete");
        if(currentState == ClientEnum.State.REFRESHING){
            currentStateWaiting = ClientEnum.StateWaiting.COMPLETING;
            refreshing(null);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {

    }

    @Override
    public void setOtherCallback(OnActionCallback callback) {
        this.callback = callback;
        logger.info("get and set callbacks");
        callback.setOtherCallback(this);
    }

    @Override
    public void callback(Object... args) {
        if(args.length == 1){
            if(args[0] instanceof ClientEnum.Request){
                byte[] reqArr = new byte[1];
                reqArr[0] = ((ClientEnum.Request)args[0]).getValue();
                ctx.writeAndFlush(ObjectEncoderDecoder.EncodeByteArraysToByteBuf(reqArr));
            }
        }
        if (args.length == 2) {
            if (args[0] instanceof ClientEnum.State) {
                logger.info("getting change state callback");
                currentState = (ClientEnum.State) args[0];
                currentStateWaiting = (ClientEnum.StateWaiting) args[1];
            }
        }
    }
}
