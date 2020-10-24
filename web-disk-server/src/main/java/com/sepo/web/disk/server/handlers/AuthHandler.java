package com.sepo.web.disk.server.handlers;

import com.sepo.web.disk.common.models.*;
import com.sepo.web.disk.common.service.ObjectEncoderDecoder;
import com.sepo.web.disk.server.database.Database;
import com.sepo.web.disk.server.helpers.MainHelper;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.SocketChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.sepo.web.disk.server.connection.Network.SERVER_STORAGE_NAME;

public class AuthHandler extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LogManager.getLogger(AuthHandler.class);
    ServerEnum.State currentState;
    ServerEnum.StateWaiting currentStateWaiting;
    ByteBuf accumulator;
    ClientEnum.Request request;
    ChannelHandlerContext ctx;
    Path userFilesPath;

    public AuthHandler() {
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        logger.info("Channel registered");
        accumulator = ctx.alloc().buffer(1024 * 1024, 1024 * 1024 * 25);
        this.ctx = ctx;
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        logger.info("Channel unregistered");
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.info("Channel active");
        currentState = ServerEnum.State.IDLE;
        currentStateWaiting = ServerEnum.StateWaiting.REQUEST;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.info("Channel inactive");
        ctx.close();
    }

    private void auth(ByteBuf bb) {
        if (currentStateWaiting == ServerEnum.StateWaiting.TRANSFER) {
            logger.info("auth transfer, buffer size - " + bb.readableBytes());
            if (bb.readableBytes() > 0) accumulator.writeBytes(bb);
            logger.info("auth transfer, accum size - " + accumulator.readableBytes());
            bb.release();
        }
        if (currentStateWaiting == ServerEnum.StateWaiting.COMPLETING) {
            logger.info("auth completing, accum size - " + accumulator.readableBytes());
            var user = (User) ObjectEncoderDecoder.DecodeByteBufToObject(accumulator);
            accumulator.retain().release();
            var respArr = new byte[1];

            if (Database.getUser(user.getEmail(), user.getPassword()) == null) {
                respArr[0] = ServerEnum.Respond.FAILURE.getValue();
                ctx.writeAndFlush(ObjectEncoderDecoder.EncodeByteArraysToByteBuf(respArr));
            } else {
                respArr[0] = ServerEnum.Respond.SUCCESS.getValue();
                userFilesPath = Path.of(SERVER_STORAGE_NAME).resolve(user.getEmail());
                createUserDir();

                ctx.writeAndFlush(ObjectEncoderDecoder.EncodeByteArraysToByteBuf(respArr));
                ctx.pipeline().remove(this);
                ctx.pipeline().addLast(new MainHandler(userFilesPath));
            }
            currentState = ServerEnum.State.IDLE;
            currentStateWaiting = ServerEnum.StateWaiting.REQUEST;
        }

    }

    private void reg(ByteBuf bb) {
        if (currentStateWaiting == ServerEnum.StateWaiting.TRANSFER) {
            logger.info("reg transfer, buffer size - " + bb.readableBytes());
            if (bb.readableBytes() > 0) accumulator.writeBytes(bb);
            logger.info("reg transfer, accum size - " + accumulator.readableBytes());
            bb.release();
        }
        if (currentStateWaiting == ServerEnum.StateWaiting.COMPLETING) {
            logger.info("reg completing, accum size - " + accumulator.readableBytes());
            var user = (User) ObjectEncoderDecoder.DecodeByteBufToObject(accumulator);
            accumulator.retain().release();

            var respArr = new byte[1];
            respArr[0] = Database.insertUser(user)
                    ? ServerEnum.Respond.SUCCESS.getValue()
                    : ServerEnum.Respond.FAILURE.getValue();

            ctx.writeAndFlush(ObjectEncoderDecoder.EncodeByteArraysToByteBuf(respArr));
            currentState = ServerEnum.State.IDLE;
            currentStateWaiting = ServerEnum.StateWaiting.REQUEST;
        }
    }

    private void createUserDir() {
        if (Files.notExists(userFilesPath)) {
            try {
                Files.createDirectory(userFilesPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        logger.info("channel read");

        if (currentState == ServerEnum.State.IDLE) {
            var bb = (ByteBuf) msg;
            logger.info("get request");
            request = ClientEnum.getRequestByValue(bb.readByte());
            if (request == ClientEnum.Request.AUTH) {
                currentState = ServerEnum.State.AUTH;
                currentStateWaiting = ServerEnum.StateWaiting.TRANSFER;
                auth(bb);
            }
            if (request == ClientEnum.Request.REG) {
                currentState = ServerEnum.State.REG;
                currentStateWaiting = ServerEnum.StateWaiting.TRANSFER;
                reg(bb);
            }
        }
    }


    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        logger.info("Channel read complete, accum size - " + accumulator.readableBytes());
        currentStateWaiting = ServerEnum.StateWaiting.COMPLETING;
        if (currentState == ServerEnum.State.AUTH) auth(null);
        if (currentState == ServerEnum.State.REG) reg(null);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
