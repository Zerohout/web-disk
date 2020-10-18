package com.sepo.web.disk.server.handlers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.util.ArrayList;

import com.sepo.web.disk.common.models.*;
import com.sepo.web.disk.common.service.*;
import com.sepo.web.disk.server.database.Database;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MainHandler extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LogManager.getLogger(MainHandler.class);
    private ServerState currentState;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        currentState = new ServerState(ServerState.State.IDLE, ServerState.Wait.REQUEST);
        logger.info("Client connected...");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.info("Client disconnected...");
        ctx.close();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf bb = (ByteBuf) msg;

        // Отлов команд Cancel и Get_State
        // использую bb.copy так как после декодирования bb буфер каким-то образом очищается сам
        if (ObjectEncoderDecoder.DecodeByteBufToObject(bb.copy()) instanceof Sendable) {
            var temp = (Sendable) ObjectEncoderDecoder.DecodeByteBufToObject(bb.copy());
            if (temp instanceof ClientRequest) {
                var req = (ClientRequest) temp;
                switch ((req).getCurrRequest()) {
                    case STATE:
                        logger.info("get STATE request");
                        send(ctx, currentState);
                        bb.release();
                        return;
                    case CANCEL:
                        logger.info("get CANCEL request");
                        currentState.setCurrState(ServerState.State.IDLE);
                        bb.release();
                        return;
                }

            }
        }

        // Главная логика обмена сообщениями c сервером
        switch (currentState.getCurrState()) {
            case IDLE:
                var request = (ClientRequest) ObjectEncoderDecoder.DecodeByteBufToObject(bb);
                switch (request.getCurrRequest()) {
                    case AUTH:
                        logger.info("getting AUTH request");
                        currentState.setCurrState(ServerState.State.AUTH)
                                .setCurrWait(ServerState.Wait.DATA);

                        send(ctx, new ServerRespond(ServerRespond.Responds.AUTH, ServerRespond.Results.PROCESSING));
                        break;
                    case REG:
                        logger.info("getting REG request");
                        currentState.setCurrState(ServerState.State.REG).setCurrWait(ServerState.Wait.DATA);
                        send(ctx, new ServerRespond(ServerRespond.Responds.REG, ServerRespond.Results.PROCESSING));
                        break;

                }
                break;
            case AUTH:
                switch (currentState.getCurrWait()) {
                    case REQUEST:
                        break;
                    case DATA:
                        var user = (User) ObjectEncoderDecoder.DecodeByteBufToObject(bb);
                        logger.info("Get user " + user.getEmail() + " | " + user.getPassword());

                        var us = Database.getUser(user.getEmail(), user.getPassword());
                        if (us == null) {
                            logger.info("User not found. Send FAILURE result to client");
                            send(ctx, new ServerRespond(ServerRespond.Responds.AUTH, ServerRespond.Results.FAILURE));
                            currentState.setCurrState(ServerState.State.AUTH).setCurrWait(ServerState.Wait.DATA);
                        } else {
                            logger.info("User found. Send SUCCESS result to client");
                            send(ctx, new ServerRespond(ServerRespond.Responds.AUTH, ServerRespond.Results.SUCCESS));
                            currentState.setCurrState(ServerState.State.IDLE).setCurrWait(ServerState.Wait.REQUEST);
                        }
                        break;
                }
                break;
            case REG:
                switch (currentState.getCurrWait()){
                    case REQUEST:
                        break;
                    case DATA:
                        var user = (User) ObjectEncoderDecoder.DecodeByteBufToObject(bb);
                        logger.info("Get new User " + user.getEmail() + " | " + user.getPassword());

                        if(Database.insertUser(user)){
                            logger.info("User was inserted to db. Send SUCCESS result.");
                            send(ctx, new ServerRespond(ServerRespond.Responds.REG, ServerRespond.Results.SUCCESS));
                            currentState.setCurrState(ServerState.State.IDLE).setCurrWait(ServerState.Wait.REQUEST);
                        }else{
                            logger.info("Can't insert user to db. Send FAILURE result.");
                            send(ctx, new ServerRespond(ServerRespond.Responds.REG, ServerRespond.Results.FAILURE));
                            currentState.setCurrState(ServerState.State.REG).setCurrWait(ServerState.Wait.DATA);
                        }
                        break;
                }
                break;
        }
        bb.release();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    private void send(ChannelHandlerContext ctx, Sendable obj) {
        ctx.writeAndFlush(ObjectEncoderDecoder.EncodeObjToByteBuf(obj));
    }


}

