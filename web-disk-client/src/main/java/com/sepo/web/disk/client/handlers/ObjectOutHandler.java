package com.sepo.web.disk.client.handlers;

import com.sepo.web.disk.client.Helpers.OnActionCallback;
import com.sepo.web.disk.client.controllers.FileManagerController;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ObjectOutHandler extends ChannelOutboundHandlerAdapter implements OnActionCallback {
    private static final Logger logger = LogManager.getLogger(ObjectOutHandler.class);
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {

    }

    @Override
    public void flush(ChannelHandlerContext ctx) throws Exception {
       logger.info("flush");
    }

    @Override
    public void setOtherCallback(OnActionCallback callback) {
        callback.setOtherCallback(this);
    }

    @Override
    public void callback(Object... args) {

    }
}
