package com.sepo.web.disk.server.handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ObjectOutHandler extends ChannelOutboundHandlerAdapter {
    private static final Logger logger = LogManager.getLogger(ObjectOutHandler.class);
    @Override
    public void read(ChannelHandlerContext ctx) throws Exception {
        logger.info("ObjectOutHandler read");
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        logger.info("ObjectOutHandler write");
    }

    @Override
    public void flush(ChannelHandlerContext ctx) throws Exception {
        logger.info("ObjectOutHandler flush");
    }
}
