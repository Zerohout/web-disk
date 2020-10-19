package com.sepo.web.disk.client.handlers;

import com.sepo.web.disk.client.Helpers.OnActionCallback;
import com.sepo.web.disk.client.controllers.FileManagerController;
import com.sepo.web.disk.client.network.Network;
import com.sepo.web.disk.common.models.*;
import com.sepo.web.disk.common.service.ObjectEncoderDecoder;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.handler.timeout.IdleState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;

@ChannelHandler.Sharable
public class NetworkHandler extends ChannelInboundHandlerAdapter implements OnActionCallback {
    private static final Logger logger = LogManager.getLogger(FileManagerController.class);
    private FileInfo currFileInfo;

    public ClientState currentState;
    //= new ClientState(ClientState.State.IDLE, ClientState.Wait.RESPOND);
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


    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
        logger.info("Channel Registered");
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        super.channelUnregistered(ctx);
        logger.info("Channel Unregistered");
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        //super.channelActive(ctx);
        logger.info("Channel Active");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        //super.channelInactive(ctx);
        logger.info("Channel Inactive");
    }

    // главная логика обмена сообщениями с клиентом
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if(currentState != null) {
            logger.info("Запуск метода channelRead, текущее состояние - " +
                    currentState.getCurrState().toString() +
                    ", текущее ожидание - " + currentState.getCurrWait().toString());
        }else{
            logger.info("Текущее состояние - null");
        }
        ByteBuf bb = (ByteBuf) msg;
        ServerRespond respond;
        ServerState serverState;

        if(currentState == null){
            currentState = new ClientState(ClientState.State.IDLE, ClientState.Wait.RESPOND);
        }

        switch (currentState.getCurrState()) {
            case IDLE:

                break;
            case AUTH:
                respond = (ServerRespond) ObjectEncoderDecoder.DecodeByteBufToObject(ctx, bb);
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
                respond = (ServerRespond) ObjectEncoderDecoder.DecodeByteBufToObject(ctx, bb);
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
                serverState = (ServerState) ObjectEncoderDecoder.DecodeByteBufToObject(ctx, bb);
                logger.info("get ServerState " + serverState.getCurrState());
                if (serverState.getCurrState() != ServerState.State.IDLE) {
                    logger.info("send CANCEL request");
                    send(new ClientRequest(ClientRequest.Requests.CANCEL));
                }
                currentState.setCurrState(ClientState.State.IDLE).setCurrWait(ClientState.Wait.RESPOND);
                break;
            case UPDATE:
                if (ObjectEncoderDecoder.DecodeByteBufToObject(ctx, bb.copy()) instanceof FileInfo) {
                    logger.info("get fileInfo");
                    otherCallback.callback(null, ObjectEncoderDecoder.DecodeByteBufToObject(ctx, bb));
                    send(new ClientRequest(ClientRequest.Requests.UPDATE));
                    break;
                }

                if (ObjectEncoderDecoder.DecodeByteBufToObject(ctx, bb.copy()) == null) logger.info("object null");

                if (ObjectEncoderDecoder.DecodeByteBufToObject(ctx, bb.copy()) instanceof Sendable) {
                    respond = (ServerRespond) ObjectEncoderDecoder.DecodeByteBufToObject(ctx, bb);
                    if (respond.getCurrResult() == ServerRespond.Results.SUCCESS) {
                        logger.info("Get UPDATE SUCCESS result");
                        currentState.setCurrState(ClientState.State.IDLE).setCurrWait(ClientState.Wait.RESPOND);
                        bb.release();
                        callback(respond);
                        return;
                    }
                }
                break;
            case SEND:
                if (ObjectEncoderDecoder.DecodeByteBufToObject(ctx, bb.copy()) instanceof ServerRespond) {
                    respond = (ServerRespond) ObjectEncoderDecoder.DecodeByteBufToObject(ctx, bb);
                    switch (respond.getCurrRespond()) {
                        case GET_FILE_INFO:
                            switch (respond.getCurrResult()) {
                                case PROCESSING:
                                    logger.info("get GET_FILE_INFO respond. Send FileInfo");
                                    send(currFileInfo);
                                    break;
                                case SUCCESS:
                                    logger.info("get GET_FILE_INFO respond, SUCCESS result. Send file.");
                                   sendFile(currFileInfo, Network.getInstance().getCurrentChannel(), f -> {
                                       if (!f.isSuccess()) {
                                           logger.info("Ошибка при передаче файла");

                                           f.cause().printStackTrace();

                                       }
                                       if (f.isSuccess()) {
                                           logger.info("Файл успешно передан");
                                       }
                                   });

                                    break;
                            }
                            break;
                        case GET_FILE:
                            switch (respond.getCurrResult()) {
                                case SUCCESS:
                                    logger.info("server get file.");
                                    //currentState.setCurrState(ClientState.State.IDLE).setCurrWait(ClientState.Wait.RESPOND);
                                    break;
                                case FAILURE:
                                    logger.info("server not get file");
                                    currentState.setCurrState(ClientState.State.IDLE).setCurrWait(ClientState.Wait.RESPOND);
                                    break;
                            }
                            break;
                    }

                }
                break;
        }
        bb.release();
    }

    private void sendFile(FileInfo fileInfo, Channel ctx, ChannelFutureListener finishListener){
        var fileReg = new DefaultFileRegion(fileInfo.getPath().toFile(), 0, currFileInfo.getFileSize());
        var transferOperationFuture = ctx.writeAndFlush(fileReg).syncUninterruptibly();
        if (finishListener != null) {
            transferOperationFuture.addListener(finishListener);
        }
    }

    public void send(Sendable obj) {
        Network.getInstance().getCurrentChannel().writeAndFlush(ObjectEncoderDecoder.EncodeObjToByteBuf(obj));
    }

    @Override
    public void callback(Object... args) {
        if (args.length == 1) {
            if (args[0] instanceof Sendable) {
                if (args[0] instanceof FileInfo) {
                    currentState.setCurrState(ClientState.State.SEND).setCurrWait(ClientState.Wait.RESPOND);
                    send(new ClientRequest(ClientRequest.Requests.SEND));
                    currFileInfo = (FileInfo) args[0];
                    return;
                }
                send((Sendable) args[0]);
            }
        } else if (args.length == 2) {
            if (args[0] instanceof ClientState.State) {
                if(currentState == null){
                    currentState = new ClientState((ClientState.State) args[0],(ClientState.Wait) args[1]);
                }else{

                currentState.setCurrState((ClientState.State) args[0]).setCurrWait((ClientState.Wait) args[1]);
                }
            }
        }
    }


    @Override
    public void setOtherCallback(OnActionCallback otherCallback) {
        this.otherCallback = otherCallback;
        otherCallback.setOtherCallback(this);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
