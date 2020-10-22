package com.sepo.web.disk.common.models;

import java.util.HashMap;
import java.util.Map;

public class ServerEnum {
    public enum State {
        IDLE((byte) -1), // REQUEST
        AUTH((byte) -2),
        REG((byte) -3),
        PROCESSING((byte)-4),
        SENDING((byte) -5), // file_info, file
        GETTING((byte) -6); // file_info, file, file_tree, state

        private final byte value;

        State(byte value) {
            this.value = value;
        }

        public byte getValue() {
            return this.value;
        }
    }

    public enum StateWaiting {
        REQUEST((byte) -11), // IDLE
        FILE((byte) -12), // IDLE, AUTH, REG
        TRANSFER((byte) -13),
        COMPLETING((byte) -14),
        FILE_TREE((byte) -15);

        private byte value;

        StateWaiting(byte value) {
            this.value = value;
        }

        public byte getValue() {
            return value;
        }
    }

    public enum Respond {
        SUCCESS((byte) -21), // auth "user"
        FAILURE((byte) -22), // reg "user"
        SEND((byte) -23), // send "RequestType" (file, file_tree, state)
        GET((byte) -24), // get "RequestType" (file, )
        OPERATION((byte) -25), // operation "RequestType" (rename, move, delete, rename)
        CANCEL((byte) -26); // set server state to IDLE

        private final byte value;

        Respond(byte value) {
            this.value = value;
        }

        public byte getValue() {
            return this.value;
        }
    }

    public enum RespondType {
        FILE((byte) -31), // get, send
        FILE_TREE((byte) -32), // send
        STATE((byte) -33), // send
        RENAME((byte) -34), // operation
        MOVE((byte) -35), // operation
        CREATE((byte) -36), // operation
        DELETE((byte) -37); // operation

        private final byte value;

        RespondType(byte value) {
            this.value = value;
        }

        public byte getValue() {
            return this.value;
        }
    }

    private static final Map<State,Byte> stateMap = new HashMap<>();
    private static final Map<StateWaiting,Byte> stateWaitingMap = new HashMap<>();
    private static final Map<Respond,Byte> respondMap = new HashMap<>();
    private static final Map<RespondType,Byte> respondTypeMap = new HashMap<>();


    static{
        for(var val : State.values()){
            stateMap.put(val,val.value);
        }
        for(var val : StateWaiting.values()){
            stateWaitingMap.put(val,val.value);
        }
        for(var val : Respond.values()){
            respondMap.put(val,val.value);
        }
        for(var val : RespondType.values()){
            respondTypeMap.put(val,val.value);
        }
    }
    public static State getStateByValue(byte value){
        return stateMap
                .entrySet()
                .stream()
                .filter(m -> m.getValue().equals(value))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    public static StateWaiting getStateWaitingByValue(byte value){
        return stateWaitingMap
                .entrySet()
                .stream()
                .filter(m -> m.getValue().equals(value))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }
    public static Respond getRespondByValue(byte value){
        return respondMap
                .entrySet()
                .stream()
                .filter(m -> m.getValue().equals(value))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }
    public static RespondType getRespondTypeByValue(byte value){
        return respondTypeMap
                .entrySet()
                .stream()
                .filter(m -> m.getValue().equals(value))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }
}
