package com.sepo.web.disk.client.handlers;

import com.sepo.web.disk.client.Helpers.OnActionCallback;
import com.sepo.web.disk.client.controllers.FileManagerController;
import com.sepo.web.disk.client.network.Network;
import com.sepo.web.disk.common.models.*;
import com.sepo.web.disk.common.service.ObjectEncoderDecoder;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class NetworkHandler extends ChannelInboundHandlerAdapter implements OnActionCallback {
    private static final Logger logger = LogManager.getLogger(FileManagerController.class);

    public ClientState currentState = new ClientState(ClientState.State.IDLE, ClientState.Wait.RESPOND);


    private OnActionCallback currCallback;

    public void setOtherCallback(OnActionCallback otherCallback) {
        this.otherCallback = otherCallback;
        otherCallback.setCallback(this);
    }

    private OnActionCallback otherCallback;

    public NetworkHandler() {

    }

    public void onErrorConnectionAction() {
        logger.info("Error connection");
        if (otherCallback != null) {
            otherCallback.callback("Can't connected to server. Please try again.", true, true);
        }
        Network.getInstance().stop();
    }

    public void onSuccessfulConnection() {
        logger.info("Successful connection");
        if (otherCallback != null) {
            otherCallback.callback("", false, false);
        }
    }

    // главная логика обмена сообщениями с клиентом
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf bb = (ByteBuf) msg;
        ServerRespond respond;
        ServerState serverState;

        switch (currentState.getCurrState()) {
            case IDLE:

                break;
            case AUTH:
                respond = (ServerRespond) ObjectEncoderDecoder.DecodeByteBufToObject(bb);
                switch (currentState.getCurrWait()) {
                    case RESPOND:
                        logger.info("getting AUTH respond");
                        otherCallback.callback(respond);
                        currentState.setCurrWait(ClientState.Wait.RESULT);
                        break;
                    case RESULT:
                        switch (respond.getCurrResult()) {
                            case SUCCESS:
                                logger.info("Success auth");
                                otherCallback.callback(respond);
                                break;
                            case FAILURE:
                                logger.info("Failure auth");
                                otherCallback.callback(respond);
                                break;
                        }
                        break;
                }

                break;
            case REG:
                respond = (ServerRespond) ObjectEncoderDecoder.DecodeByteBufToObject(bb);
                switch (currentState.getCurrWait()) {
                    case RESPOND:
                        logger.info("getting REG respond");
                        otherCallback.callback(respond);
                        break;
                    case RESULT:
                        switch (respond.getCurrResult()) {
                            case SUCCESS:
                                logger.info("Success registration");
                                otherCallback.callback(respond);
                                break;
                            case FAILURE:
                                logger.info("Failure registration");
                                otherCallback.callback(respond);
                                break;
                        }
                        break;
                }
                break;
            case STATE:
                serverState = (ServerState) ObjectEncoderDecoder.DecodeByteBufToObject(bb);
                logger.info("get ServerState " + serverState.getCurrState());
                if (serverState.getCurrState() != ServerState.State.IDLE) {
                    logger.info("send CANCEL request");
                    send(new ClientRequest(ClientRequest.Requests.CANCEL));
                }
                currentState.setCurrState(ClientState.State.IDLE).setCurrWait(ClientState.Wait.RESPOND);
                break;
        }
        bb.release();
    }

    public void send(Sendable obj) {
        Network.getInstance().getCurrentChannel().writeAndFlush(ObjectEncoderDecoder.EncodeObjToByteBuf(obj));
    }


    @Override
    public void callback(Object... args) {
        if (args.length == 1) {
            if (args[0] instanceof Sendable) {
                send((Sendable) args[0]);
            }
        } else if (args.length == 2) {
            if (args[0] instanceof ClientState.State) {
                currentState.setCurrState((ClientState.State) args[0]).setCurrWait((ClientState.Wait) args[1]);
            }
        }
    }

    @Override
    public void setCallback(OnActionCallback callback) {
        this.currCallback = callback;
    }

    @Override
    public OnActionCallback getCallback() {
        return currCallback;
    }
}
