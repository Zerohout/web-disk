package com.sepo.web.disk.client.handlers;

import com.sepo.web.disk.client.Helpers.OnActionCallback;
import com.sepo.web.disk.client.controllers.FileManagerController;
import com.sepo.web.disk.client.network.Network;
import com.sepo.web.disk.common.models.*;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.sepo.web.disk.common.service.ObjectEncoderDecoder.*;

@ChannelHandler.Sharable
public class AuthHandler extends ChannelInboundHandlerAdapter implements OnActionCallback {
    private static final Logger logger = LogManager.getLogger(AuthHandler.class);
    private ChannelHandlerContext ctx;
    private OnActionCallback otherCallback;
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
        if (otherCallback != null) {
            otherCallback.callback("Can't connected to server. Please try again.", true, true);
        }
        ctx.close();
    }

    public void onSuccessfulConnection() {
        logger.info("Successful connection");

    }

    //region ChannelInboundHandlerAdapter methods

    //endregion

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
        if (otherCallback != null) {
            otherCallback.callback("", false, false);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        //super.channelInactive(ctx);
        logger.info("Channel Inactive");
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

            }
            otherCallback.callback(operationResult);
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
            otherCallback.callback(operationResult);
            resetStateToIDLE();
        }
    }

    private void resetStateToIDLE() {
        currentState = ClientEnum.State.IDLE;
        currentStateWaiting = ClientEnum.StateWaiting.NOTHING;
        accumulator.release();
        operationResult = null;
    }

    // главная логика обмена сообщениями с клиентом
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        logger.info("channel read");
        if (currentState == ClientEnum.State.AUTH) {
            auth((ByteBuf) msg);
        }
        if (currentState == ClientEnum.State.REG) {
            reg((ByteBuf) msg);
        }


//
//        ByteBuf bb = (ByteBuf) msg;
//        ServerRespond respond;
//        ServerState serverState;
//
//        switch (currentState.getCurrState()) {
//            case REG:
//                respond = (ServerRespond) ObjectEncoderDecoder.DecodeByteBufToObject(ctx, bb);
//                switch (currentState.getCurrWait()) {
//                    case RESPOND:
//                        logger.info("getting REG respond");
//                        otherCallback.callback(respond);
//                        break;
//                }
//                break;
//            case STATE:
//                serverState = (ServerState) ObjectEncoderDecoder.DecodeByteBufToObject(ctx, bb);
//                logger.info("get ServerState " + serverState.getCurrState());
//                if (serverState.getCurrState() != ServerState.State.IDLE) {
//                    logger.info("send CANCEL request");
//                    send(new ClientRequest(ClientRequest.Request.CANCEL));
//                }
//                currentState.setCurrState(ClientState.State.IDLE).setCurrWait(ClientState.Wait.RESPOND);
//                break;
//            case UPDATE:
//                if (ObjectEncoderDecoder.DecodeByteBufToObject(ctx, bb.copy()) instanceof FileInfo) {
//                    logger.info("get fileInfo");
//                    otherCallback.callback(null, ObjectEncoderDecoder.DecodeByteBufToObject(ctx, bb));
//                    send(new ClientRequest(ClientRequest.Request.UPDATE));
//                    break;
//                }
//
//                if (ObjectEncoderDecoder.DecodeByteBufToObject(ctx, bb.copy()) == null) logger.info("object null");
//
//                if (ObjectEncoderDecoder.DecodeByteBufToObject(ctx, bb.copy()) instanceof Sendable) {
//                    respond = (ServerRespond) ObjectEncoderDecoder.DecodeByteBufToObject(ctx, bb);
//                    if (respond.getCurrResult() == ServerRespond.Results.SUCCESS) {
//                        logger.info("Get UPDATE SUCCESS result");
//                        currentState.setCurrState(ClientState.State.IDLE).setCurrWait(ClientState.Wait.RESPOND);
//                        bb.release();
//                        callback(respond);
//                        return;
//                    }
//                }
//                break;
//            case SEND:
//                if (ObjectEncoderDecoder.DecodeByteBufToObject(ctx, bb.copy()) instanceof ServerRespond) {
//                    respond = (ServerRespond) ObjectEncoderDecoder.DecodeByteBufToObject(ctx, bb);
//                    switch (respond.getCurrRespond()) {
//                        case GET_FILE_INFO:
//                            switch (respond.getCurrResult()) {
//                                case PROCESSING:
//                                    logger.info("get GET_FILE_INFO respond. Send FileInfo");
//                                    send(currFileInfo);
//                                    break;
//                                case SUCCESS:
//                                    logger.info("get GET_FILE_INFO respond, SUCCESS result. Send file.");
//                                    sendFile(currFileInfo, Network.getInstance().getCurrentChannel(), f -> {
//                                        if (!f.isSuccess()) {
//                                            logger.info("Ошибка при передаче файла");
//
//                                            f.cause().printStackTrace();
//
//                                        }
//                                        if (f.isSuccess()) {
//                                            logger.info("Файл успешно передан");
//                                        }
//                                    });
//
//                                    break;
//                            }
//                            break;
//                        case GET_FILE:
//                            switch (respond.getCurrResult()) {
//                                case SUCCESS:
//                                    logger.info("server get file.");
//                                    //currentState.setCurrState(ClientState.State.IDLE).setCurrWait(ClientState.Wait.RESPOND);
//                                    break;
//                                case FAILURE:
//                                    logger.info("server not get file");
//                                    currentState.setCurrState(ClientState.State.IDLE).setCurrWait(ClientState.Wait.RESPOND);
//                                    break;
//                            }
//                            break;
//                    }
//
//                }
//                break;
//        }
//        bb.release();
    }

//    private void sendFile(FileInfo fileInfo, Channel ctx, ChannelFutureListener finishListener) {
//        var fileReg = new DefaultFileRegion(new File(fileInfo.getAbsolutePath()), 0, currFileInfo.getSize());
//        var transferOperationFuture = ctx.writeAndFlush(fileReg).syncUninterruptibly();
//        if (finishListener != null) {
//            transferOperationFuture.addListener(finishListener);
//        }
//    }

    public void send(Sendable obj) {
        ctx.writeAndFlush(EncodeObjToByteBuf(obj));
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
    public void callback(Object... args) {
        if (args.length == 2) {
            if (args[0] instanceof ClientEnum.State) {
                logger.info("getting change state callback");
                currentState = (ClientEnum.State) args[0];
                currentStateWaiting = (ClientEnum.StateWaiting) args[1];
            }
            if (args[0] instanceof ClientEnum.Request) {
                var req = (ClientEnum.Request) args[0];
                if (args[1] instanceof ClientEnum.RequestType) {
                    var reqType = (ClientEnum.RequestType) args[1];
//                    if (req == ClientEnum.Request.GET && reqType == ClientEnum.RequestType.FILE_TREE) {
//                        if (folder != null) {
//                            logger.info("send folder callback");
//                            otherCallback.callback(folder);
//                            if (currentState != ClientEnum.State.IDLE) {
//                                currentState = ClientEnum.State.IDLE;
//                                currentStateWaiting = ClientEnum.StateWaiting.NOTHING;
//                            }
//                            folder = null;
//                            return;
//                        }
//                    }
                }
            }
        }
        if (args.length == 1) {
            if (args[0] instanceof User) {
                var reqArr = new byte[1];
                reqArr[0] = currentState == ClientEnum.State.AUTH
                        ? ClientEnum.Request.AUTH.getValue()
                        : ClientEnum.Request.REG.getValue();
                ctx.writeAndFlush(EncodeByteArraysToByteBuf(reqArr, convertObjectToByteArray(args[0])));
            }

        }
//        if (args.length == 1) {
//            if (args[0] instanceof Sendable) {
//                if (args[0] instanceof FileInfo) {
//                    currentState.setCurrState(ClientState.State.SEND).setCurrWait(ClientState.Wait.RESPOND);
//                    send(new ClientRequest(ClientRequest.Request.SEND));
//                    currFileInfo = (FileInfo) args[0];
//                    return;
//                }
//                send((Sendable) args[0]);
//            }
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
