package com.sepo.web.disk.common.models;

import java.io.Serializable;

public class ServerState extends SystemInfo implements Serializable {
    public enum State {
        IDLE, AUTH, REG, SEND, GET;
    }
    public enum Wait{
        DATA, REQUEST
    }

    private State currState;
    private Wait currWait;

    public ServerState(State currState, Wait currWait) {
        this.currState = currState;
        this.currWait = currWait;
    }

    public State getCurrState() {
        return currState;
    }

    public ServerState setCurrState(State currState) {
        this.currState = currState;
        return this;
    }

    public Wait getCurrWait() {
        return currWait;
    }

    public ServerState setCurrWait(Wait currWait) {
        this.currWait = currWait;
        return this;
    }
}
