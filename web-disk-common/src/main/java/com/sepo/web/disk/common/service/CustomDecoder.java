package com.sepo.web.disk.common.service;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.serialization.ClassResolver;

public class CustomDecoder extends LengthFieldBasedFrameDecoder {
    private final ClassResolver classResolver;

    public CustomDecoder(ClassResolver classResolver) {
        this(1024 * 1024, classResolver);
    }

    public CustomDecoder(int maxObjectSize, ClassResolver classResolver) {
        super(maxObjectSize, 0, 4, 0, 4);
        this.classResolver = classResolver;
    }

    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        ByteBuf frame = (ByteBuf) super.decode(ctx, in);
        if (frame == null) {
            return null;
        } else {
            try (CustomCompactObjectInputStream ois = new CustomCompactObjectInputStream(
                    new ByteBufInputStream(frame, true), this.classResolver)) {

                return ois.readObject();
            }
        }
    }
}