package com.sepo.web.disk.common.models;

import java.util.HashMap;
import java.util.Map;

public class ClientEnum implements Sendable {
    private static final long serialVersionUID = -1419756740433855727L;

    public enum State {
        IDLE((byte) 1),
        AUTH((byte) 2),
        REG((byte) 3),
        SENDING((byte) 4),
        GETTING((byte) 5),
        REFRESHING((byte) 6);

        private final byte value;

        State(byte value) {
            this.value = value;
        }

        public byte getValue() {
            return this.value;
        }
    }

    public enum StateWaiting {
        NOTHING((byte) 21),
        RESULT((byte) 22),
        RESPOND((byte) 23),
        TRANSFER((byte) 24),
        COMPLETING((byte) 25),
        OBJECT_SIZE((byte) 26),
        OBJECT((byte) 27),
        FILE((byte) 28);

        private final byte value;

        StateWaiting(byte value) {
            this.value = value;
        }

        public byte getValue() {
            return value;
        }
    }

    public enum Request {
        AUTH((byte) 41),
        REG((byte) 42),
        SEND((byte) 43),
        GET((byte) 44),
        REFRESH((byte) 45),
        OPERATION((byte) 46),
        CANCEL((byte) 47);

        private final byte value;

        Request(byte value) {
            this.value = value;
        }

        public byte getValue() {
            return this.value;
        }
    }

    public enum RequestType {
        FILE((byte) 61),
        STATE((byte) 62),
        RENAME((byte) 63),
        MOVE((byte) 64),
        CREATE((byte) 65),
        DELETE((byte) 66),
        COPY((byte) 67),
        CUT((byte) 68);

        private final byte value;

        RequestType(byte value) {
            this.value = value;
        }

        public byte getValue() {
            return this.value;
        }
    }

    private static final Map<State, Byte> stateMap = new HashMap<>();
    private static final Map<StateWaiting, Byte> stateWaitingMap = new HashMap<>();
    private static final Map<Request, Byte> requestMap = new HashMap<>();
    private static final Map<RequestType, Byte> requestTypeMap = new HashMap<>();

    static {
        for (var val : State.values()) {
            stateMap.put(val, val.value);
        }
        for (var val : StateWaiting.values()) {
            stateWaitingMap.put(val, val.value);
        }
        for (var val : Request.values()) {
            requestMap.put(val, val.value);
        }
        for (var val : RequestType.values()) {
            requestTypeMap.put(val, val.value);
        }
    }

    public static State getStateByValue(byte value) {
        return stateMap
                .entrySet()
                .stream()
                .filter(m -> m.getValue().equals(value))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    public static StateWaiting getStateWaitingByValue(byte value) {
        return stateWaitingMap
                .entrySet()
                .stream()
                .filter(m -> m.getValue().equals(value))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    public static Request getRequestByValue(byte value) {
        return requestMap
                .entrySet()
                .stream()
                .filter(m -> m.getValue().equals(value))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    public static RequestType getRequestTypeByValue(byte value) {
        return requestTypeMap
                .entrySet()
                .stream()
                .filter(m -> m.getValue().equals(value))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }
}
