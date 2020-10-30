package com.sepo.web.disk.common.models;

import java.util.HashMap;
import java.util.Map;

public class ServerEnum {
    public enum State {
        IDLE((byte) -1),
        AUTH((byte) -2),
        REG((byte) -3),
        PROCESSING((byte) -4),
        SENDING((byte) -5),
        GETTING((byte) -6),
        DELETING((byte) -7),
        RENAMING((byte) -8),
        CREATING((byte) -9),
        COPYING((byte) -10),
        CUTTING((byte) -11);

        private final byte value;

        State(byte value) {
            this.value = value;
        }

        public byte getValue() {
            return this.value;
        }
    }

    public enum StateWaiting {
        REQUEST((byte) -21),
        FILE((byte) -22),
        OBJECT((byte) -23),
        COMPLETING((byte) -24),
        FILE_TREE((byte) -25),
        OBJECT_SIZE((byte) -26);

        private final byte value;

        StateWaiting(byte value) {
            this.value = value;
        }

        public byte getValue() {
            return value;
        }
    }

    public enum Respond {
        SUCCESS((byte) -41),
        FAILURE((byte) -42),
        SEND((byte) -43),
        GET((byte) -44),
        OPERATION((byte) -45),
        CANCEL((byte) -46);

        private final byte value;

        Respond(byte value) {
            this.value = value;
        }

        public byte getValue() {
            return this.value;
        }
    }

    public enum RespondType {
        FILE((byte) -61),
        FILE_TREE((byte) -62),
        STATE((byte) -63),
        RENAME((byte) -64),
        MOVE((byte) -65),
        CREATE((byte) -66),
        DELETE((byte) -67);

        private final byte value;

        RespondType(byte value) {
            this.value = value;
        }

        public byte getValue() {
            return this.value;
        }
    }

    private static final Map<State, Byte> stateMap = new HashMap<>();
    private static final Map<StateWaiting, Byte> stateWaitingMap = new HashMap<>();
    private static final Map<Respond, Byte> respondMap = new HashMap<>();
    private static final Map<RespondType, Byte> respondTypeMap = new HashMap<>();


    static {
        for (State val : State.values()) {
            stateMap.put(val, val.value);
        }
        for (StateWaiting val : StateWaiting.values()) {
            stateWaitingMap.put(val, val.value);
        }
        for (Respond val : Respond.values()) {
            respondMap.put(val, val.value);
        }
        for (RespondType val : RespondType.values()) {
            respondTypeMap.put(val, val.value);
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

    public static Respond getRespondByValue(byte value) {
        return respondMap
                .entrySet()
                .stream()
                .filter(m -> m.getValue().equals(value))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    public static RespondType getRespondTypeByValue(byte value) {
        return respondTypeMap
                .entrySet()
                .stream()
                .filter(m -> m.getValue().equals(value))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }
}
