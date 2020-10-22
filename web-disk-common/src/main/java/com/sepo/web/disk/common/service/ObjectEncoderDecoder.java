package com.sepo.web.disk.common.service;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;

public class ObjectEncoderDecoder {
    private static final Logger logger = LogManager.getLogger(ObjectEncoderDecoder.class);

    public static ByteBuf EncodeObjToByteBuf(Object obj) {
        return Unpooled.wrappedBuffer(convertObjectToByteArray(obj));
    }

    public static ByteBuf EncodeByteArraysToByteBuf(byte[]... arrs) {
        var arrSize = 0;

        for (var arr : arrs) arrSize += arr.length;

        var out = new byte[arrSize];
        for (int i = 0, k = 0; i < arrs.length; i++) {
            for (var j = 0; j < arrs[i].length; j++, k++) {
                out[k] = arrs[i][j];
            }
        }
        return Unpooled.wrappedBuffer(out);
    }

    public static byte[] convertObjectToByteArray(Object obj) {
        try (var baos = new ByteArrayOutputStream();
             var oos = new ObjectOutputStream(baos)) {
            oos.writeObject(obj);
            return baos.toByteArray();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Не удалось конвертировать объект \"" + obj.getClass() + "\" в массив байт");
        }
    }

    public static Object convertByteArrayToObject(byte[] arr) {
        try (var bais = new ByteArrayInputStream(arr);
             var ois = new ObjectInputStream(bais)) {
            return ois.readObject();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Не удалось преобразовать массив байт в объект");
        }
    }

    public static Object DecodeByteBufToObject(ByteBuf bb) {
        try (var bbis = new ByteBufInputStream(bb, bb.readableBytes());
             var ois = new ObjectInputStream(bbis)) {
            return ois.readObject();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Не удалось декодировать ByteBuf в объект");
        }
    }
}
