package com.sepo.web.disk.server.handlers;

import com.sepo.web.disk.common.models.*;
import com.sepo.web.disk.server.database.Database;
import com.sepo.web.disk.server.helpers.MainHelper;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.sepo.web.disk.server.connection.Network.SERVER_STORAGE_NAME;

public class AuthHandler extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LogManager.getLogger(AuthHandler.class);
    private MainHelper mh;
    private ClientEnum.Request request;

    public AuthHandler() {
        mh = new MainHelper();
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        logger.info("Channel registered");
        mh.setCtx(ctx)
                .setAccumulator(ctx.alloc().buffer(1024 * 1024, 1024 * 1024 * 25))
                .setCurrentState(ServerEnum.State.IDLE)
                .setCurrentStateWaiting(ServerEnum.StateWaiting.REQUEST);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        logger.info("Channel unregistered");
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.info("Channel active");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.info("Channel inactive");
        ctx.close();
    }

    private void auth(ByteBuf bb) {
        if (mh.getCurrentStateWaiting() == ServerEnum.StateWaiting.OBJECT_SIZE) {
            mh.setObjectSize(bb);
        }
        if (mh.getCurrentStateWaiting() == ServerEnum.StateWaiting.OBJECT) {
            mh.setObject(bb);
        }
        if (mh.getCurrentStateWaiting() == ServerEnum.StateWaiting.COMPLETING) {
            User user = (User) mh.getReceivedObj();

            ServerEnum.Respond result;
            if (Database.getUser(user.getEmail(), user.getPassword()) == null) {
                result = ServerEnum.Respond.FAILURE;
            } else {
                result = ServerEnum.Respond.SUCCESS;
                mh.setUserFilesPath(Path.of(SERVER_STORAGE_NAME).resolve(user.getEmail()));
                createUserDir();
                mh.getCtx().pipeline().remove(this);
                mh.getCtx().pipeline().addLast(new MainHandler(mh));
            }
            mh.sendRespond(result);
        }

    }

    private void reg(ByteBuf bb) {
        if (mh.getCurrentStateWaiting() == ServerEnum.StateWaiting.OBJECT_SIZE) {
            mh.setObjectSize(bb);
        }
        if (mh.getCurrentStateWaiting() == ServerEnum.StateWaiting.OBJECT) {
            mh.setObject(bb);
        }
        if (mh.getCurrentStateWaiting() == ServerEnum.StateWaiting.COMPLETING) {
            ServerEnum.Respond result = Database.insertUser((User) mh.getReceivedObj())
                    ? ServerEnum.Respond.SUCCESS
                    : ServerEnum.Respond.FAILURE;

            mh.sendRespond(result);
        }
    }

    private void createUserDir() {
        if (Files.notExists(mh.getUserFilesPath())) {
            try {
                Files.createDirectory(mh.getUserFilesPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        logger.info("channel read");

        ByteBuf bb = (ByteBuf) msg;
        while (bb.readableBytes() > 0) {
            if (mh.getCurrentState() == ServerEnum.State.IDLE) {
                logger.info("get request");
                idleDistributionByMethods(bb);
            }
            if (mh.getCurrentState() == ServerEnum.State.AUTH) {
                auth(bb);
            }
            if (mh.getCurrentState() == ServerEnum.State.REG) {
                reg(bb);
            }
        }
    }

    private void idleDistributionByMethods(ByteBuf bb) {
        request = ClientEnum.getRequestByValue(bb.readByte());
        switch (request) {
            case AUTH:
                mh.setCurrentState(ServerEnum.State.AUTH);
                break;
            case REG:
                mh.setCurrentState(ServerEnum.State.REG);
                break;
        }
        mh.setCurrentStateWaiting(ServerEnum.StateWaiting.OBJECT_SIZE);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
