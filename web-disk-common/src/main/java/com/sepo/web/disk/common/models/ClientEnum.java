package com.sepo.web.disk.common.models;

import java.util.HashMap;
import java.util.Map;

public class ClientEnum implements Sendable {
    private static final long serialVersionUID = -1419756740433855727L;

    public enum State {
        IDLE((byte) 1), // NOTHING, RESULT
        AUTH((byte) 2), // RESULT
        REG((byte) 3), // RESULT
        SENDING((byte) 4), // file_info, file
        GETTING((byte) 5), // file_info, file, file_tree, state
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
        NOTHING((byte) 11), // IDLE
        RESULT((byte) 12), // IDLE, AUTH, REG
        RESPOND((byte) 13),
        TRANSFER((byte) 14),
        COMPLETING((byte) 15);

        private byte value;

        StateWaiting(byte value) {
            this.value = value;
        }

        public byte getValue() {
            return value;
        }
    }

    public enum Request {
        AUTH((byte) 21), // auth "user"
        REG((byte) 22), // reg "user"
        SEND((byte) 23), // send "RequestType" (file, file_tree, state)
        GET((byte) 24), // get "RequestType" (file, )
        REFRESH((byte)25),
        OPERATION((byte) 26), // operation "RequestType" (rename, move, delete, rename)
        CANCEL((byte) 27); // set server state to IDLE

        private final byte value;

        Request(byte value) {
            this.value = value;
        }

        public byte getValue() {
            return this.value;
        }
    }

    public enum RequestType {
        FILE((byte) 31), // get, send
        STATE((byte) 33), // send
        RENAME((byte) 34), // operation
        MOVE((byte) 35), // operation
        CREATE((byte) 36), // operation
        DELETE((byte) 37); // operation

        private final byte value;

        RequestType(byte value) {
            this.value = value;
        }

        public byte getValue() {
            return this.value;
        }
    }

    private static final Map<State,Byte> stateMap = new HashMap<>();
    private static final Map<StateWaiting,Byte> stateWaitingMap = new HashMap<>();
    private static final Map<Request,Byte> requestMap = new HashMap<>();
    private static final Map<RequestType,Byte> requestTypeMap = new HashMap<>();


    static{
        for(var val : State.values()){
            stateMap.put(val,val.value);
        }
        for(var val : StateWaiting.values()){
            stateWaitingMap.put(val,val.value);
        }
        for(var val : Request.values()){
            requestMap.put(val,val.value);
        }
        for(var val : RequestType.values()){
            requestTypeMap.put(val,val.value);
        }
    }

//    private State state;
//    private StateWaiting stateWaiting;
//    private Request request;
//    private RequestType requestType;
//
//    public State getState() {
//        return state;
//    }
//
//    public ClientEnum setState(State state) {
//        this.state = state;
//        return this;
//    }
//
//    public StateWaiting getStateWaiting() {
//        return stateWaiting;
//    }
//
//    public ClientEnum setStateWaiting(StateWaiting stateWaiting) {
//        this.stateWaiting = stateWaiting;
//        return this;
//    }
//
//    public Request getRequest() {
//        return request;
//    }
//
//    public ClientEnum setRequest(Request request) {
//        this.request = request;
//        return this;
//    }
//
//    public RequestType getRequestType() {
//        return requestType;
//    }
//
//    public ClientEnum setRequestType(RequestType requestType) {
//        this.requestType = requestType;
//        return this;
//    }

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
    public static Request getRequestByValue(byte value){
        return requestMap
                .entrySet()
                .stream()
                .filter(m -> m.getValue().equals(value))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }
    public static RequestType getRequestTypeByValue(byte value){
        return requestTypeMap
                .entrySet()
                .stream()
                .filter(m -> m.getValue().equals(value))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }
}
