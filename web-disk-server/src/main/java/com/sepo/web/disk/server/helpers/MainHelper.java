package com.sepo.web.disk.server.helpers;

import com.sepo.web.disk.common.models.ServerEnum;
import com.sepo.web.disk.common.service.ObjectEncoderDecoder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;

public class MainHelper {
    private static final Logger logger = LogManager.getLogger(MainHelper.class);
    private ServerEnum.State currentState = ServerEnum.State.IDLE;
    private ServerEnum.StateWaiting currentStateWaiting = ServerEnum.StateWaiting.REQUEST;
    private long receivedBytes;
    private long expectedBytes;
    private ByteBuf accumulator;
    private Object receivedObj;
    private ChannelHandlerContext ctx;
    private Path userFilesPath;

    public void setObjectSize(ByteBuf bb) {
        if (expectedBytes == 0L) expectedBytes = 4L;
        fillAccumulator(bb);
        if (receivedBytes == expectedBytes) {
            expectedBytes = accumulator.readInt();
            accumulator.retain().release();
            receivedBytes = 0L;
            currentStateWaiting = ServerEnum.StateWaiting.OBJECT;
        }
    }

    public void setObject(ByteBuf bb) {
        logger.info("gettingFileInfo");
        fillAccumulator(bb);
        if (receivedBytes == expectedBytes) {
            receivedObj = ObjectEncoderDecoder.DecodeByteBufToObject(accumulator);
            clearStage();
            currentStateWaiting = ServerEnum.StateWaiting.COMPLETING;
        }
    }

    public void fillAccumulator(ByteBuf bb) {
        while (bb.readableBytes() > 0 && receivedBytes != expectedBytes) {
            accumulator.writeByte(bb.readByte());
            receivedBytes++;
        }
    }

    public void clearStage() {
        if (accumulator.readableBytes() == 0) accumulator.retain().release();
        if (receivedBytes != 0L) receivedBytes = 0L;
        if (expectedBytes != 0L) expectedBytes = 0L;
        setStateToIdle();
    }

    public void setStateToIdle() {
        if (currentState != ServerEnum.State.IDLE && currentStateWaiting != ServerEnum.StateWaiting.REQUEST) {
            currentState = ServerEnum.State.IDLE;
            currentStateWaiting = ServerEnum.StateWaiting.REQUEST;
        }
    }

    public void sendResult(ServerEnum.Respond result){
        var msg = ByteBufAllocator.DEFAULT.directBuffer(1);
        msg.writeByte(result.getValue());
        ctx.writeAndFlush(msg);
    }

    public void incrementReceivedBytes(){
        receivedBytes++;
    }

    public ServerEnum.State getCurrentState() {
        return currentState;
    }

    public MainHelper setCurrentState(ServerEnum.State currentState) {
        this.currentState = currentState;
        return this;
    }

    public ServerEnum.StateWaiting getCurrentStateWaiting() {
        return currentStateWaiting;
    }

    public MainHelper setCurrentStateWaiting(ServerEnum.StateWaiting currentStateWaiting) {
        this.currentStateWaiting = currentStateWaiting;
        return this;
    }

    public long getReceivedBytes() {
        return receivedBytes;
    }

    public MainHelper setReceivedBytes(long receivedBytes) {
        this.receivedBytes = receivedBytes;
        return this;
    }

    public long getExpectedBytes() {
        return expectedBytes;
    }

    public MainHelper setExpectedBytes(long expectedBytes) {
        this.expectedBytes = expectedBytes;
        return this;
    }

    public ByteBuf getAccumulator() {
        return accumulator;
    }

    public MainHelper setAccumulator(ByteBuf accumulator) {
        this.accumulator = accumulator;
        return this;
    }

    public Object getReceivedObj() {
        return receivedObj;
    }

    public MainHelper setReceivedObj(Object receivedObj) {
        this.receivedObj = receivedObj;
        return this;
    }

    public ChannelHandlerContext getCtx() {
        return ctx;
    }

    public MainHelper setCtx(ChannelHandlerContext ctx) {
        this.ctx = ctx;
        return this;
    }

    public Path getUserFilesPath() {
        return userFilesPath;
    }

    public MainHelper setUserFilesPath(Path userFilesPath) {
        this.userFilesPath = userFilesPath;
        return this;
    }
}
