package com.sepo.web.disk.client.handlers;

import com.sepo.web.disk.client.Helpers.MainBridge;
import com.sepo.web.disk.client.network.Network;
import com.sepo.web.disk.common.models.ClientEnum;
import com.sepo.web.disk.common.models.ServerEnum;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AuthHandler extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LogManager.getLogger(AuthHandler.class);
    private ChannelHandlerContext ctx;
    private ClientEnum.State currentState = ClientEnum.State.IDLE;
    private ClientEnum.StateWaiting currentStateWaiting = ClientEnum.StateWaiting.NOTHING;
    private ServerEnum.Respond operationResult;
    private ByteBuf accumulator;

    public AuthHandler() {
        logger.info("AuthHandler constructor");
        Network.authHandler = this;
    }

    public void onErrorConnectionAction() {
        logger.info("Error connection");
        MainBridge.setSignInErrorControls("Can't connected to server. Please try again.", true, true);
        ctx.close();
    }

    public void onSuccessfulConnection() {
        logger.info("Successful connection");
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        logger.info("Channel Registered");
        this.ctx = ctx;
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        logger.info("Channel Unregistered");
        ctx.close();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.info("Channel Active");
        accumulator = ctx.alloc().buffer(1024 * 1024, 1024 * 1024 * 25);
        MainBridge.setSignInErrorControls("", false, false);
    }

    private void auth(ByteBuf bb) {
        if (currentStateWaiting == ClientEnum.StateWaiting.RESULT) {
            logger.info("auth result state");
            operationResult = ServerEnum.getRespondByValue(bb.readByte());
            currentStateWaiting = ClientEnum.StateWaiting.TRANSFER;
        }
        if (currentStateWaiting == ClientEnum.StateWaiting.TRANSFER) {
            logger.info("auth transfer state");
            if (bb.readableBytes() > 0) accumulator.writeBytes(bb);
        }

        if (currentStateWaiting == ClientEnum.StateWaiting.COMPLETING) {
            logger.info("auth completing state");
            if (operationResult == ServerEnum.Respond.SUCCESS) {
                ctx.pipeline().addLast(new MainHandler(ctx));
                ctx.pipeline().remove(this);
                Network.authHandler = null;

            }
            MainBridge.giveAuthResult(operationResult);
            resetStateToIDLE();
            return;
        }
        bb.release();
    }

    private void reg(ByteBuf bb) {
        if (currentStateWaiting == ClientEnum.StateWaiting.RESULT) {
            operationResult = ServerEnum.getRespondByValue(bb.readByte());
            currentStateWaiting = ClientEnum.StateWaiting.COMPLETING;
            bb.release();
        }
        if (currentStateWaiting == ClientEnum.StateWaiting.COMPLETING) {
            MainBridge.giveRegResult(operationResult);
            resetStateToIDLE();
        }
    }

    private void resetStateToIDLE() {
        currentState = ClientEnum.State.IDLE;
        currentStateWaiting = ClientEnum.StateWaiting.NOTHING;
        accumulator.retain().release();
        operationResult = null;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        logger.info("channel read");
        if (currentState == ClientEnum.State.AUTH) {
            auth((ByteBuf) msg);
        }
        if (currentState == ClientEnum.State.REG) {
            reg((ByteBuf) msg);
        }
    }

    public void send(ByteBuf bb, boolean isFlush) {
        if (isFlush) {
            ctx.writeAndFlush(bb);
        } else {
            ctx.write(bb);
        }
    }

    public void setState(ClientEnum.State state, ClientEnum.StateWaiting stateWaiting) {
        currentState = state;
        currentStateWaiting = stateWaiting;
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        logger.info("channel read complete");
        if (currentState == ClientEnum.State.AUTH) {
            currentStateWaiting = ClientEnum.StateWaiting.COMPLETING;
            auth(null);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
