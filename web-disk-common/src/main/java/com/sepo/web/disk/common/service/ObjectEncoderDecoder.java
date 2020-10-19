package com.sepo.web.disk.common.service;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;

public class ObjectEncoderDecoder {
    private static final Logger logger = LogManager.getLogger(ObjectEncoderDecoder.class);

    public static ByteBuf EncodeObjToByteBuf(Object obj) {
        try (var baos = new ByteArrayOutputStream();
             var oos = new ObjectOutputStream(baos)) {
            oos.writeObject(obj);
            var bytes = baos.toByteArray();
            return Unpooled.wrappedBuffer(bytes);
        } catch (Exception ex) {
            //throw new RuntimeException(ex.getMessage());
            ex.printStackTrace();
            return null;
        }

    }

    //    public static Object DecodeByteBufToObject(ChannelHandlerContext ctx,ByteBuf bb) {
//        var bytes = new byte[bb.readableBytes()];
//        bb.readBytes(bytes);
//        try (var bais = new ByteArrayInputStream(bytes);
//             var ois = new ObjectInputStream(bais)) {//
//            return ois.readObject();
//        } catch (Exception ex) {
//            ex.printStackTrace();
//            throw new RuntimeException("Фигня какая-то");
//        }
//    }

    public static Object DecodeByteBufToObject(ChannelHandlerContext ctx, ByteBuf bb) {
        byte[] arr;
        if (bb.hasArray()) {
            arr = bb.array();
        } else {
            var length = bb.readableBytes();
            arr = new byte[length];
            bb.getBytes(bb.readerIndex(), arr);
        }
        try (var bais = new ByteArrayInputStream(arr);
             var ois = new ObjectInputStream(bais)) {
            return ois.readObject();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Фигня какая-то");
        }
    }
//    public static Object DecodeByteBufToObject(ChannelHandlerContext ctx, ByteBuf in) {
//        CustomDecoder decoder = new CustomDecoder(ClassResolvers.cacheDisabled(null));
//        try {
//            return decoder.decode(ctx, in);
//        } catch (Exception e) {
//            e.printStackTrace();
//            throw new RuntimeException(e.getMessage());
//        }
//
//    }

}
