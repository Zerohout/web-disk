package com.sepo.web.disk.common.service;

import com.sepo.web.disk.common.models.ClientState;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.*;

public class ObjectEncoderDecoder {

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

    public static Object DecodeByteBufToObject(ByteBuf bb) {
        var bytes = new byte[bb.readableBytes()];
        bb.readBytes(bytes);
        try (var bais = new ByteArrayInputStream(bytes);
             var ois = new ObjectInputStream(bais)) {
            return ois.readObject();
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage());
        }
    }

}
