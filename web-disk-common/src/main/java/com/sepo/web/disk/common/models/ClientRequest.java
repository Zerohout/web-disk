package com.sepo.web.disk.common.models;

import java.io.Serializable;

public class ClientRequest extends SystemInfo implements Serializable {
    public enum Requests {
        AUTH, REG, UPDATE, SEND, GET, RENAME, MOVE, CREATE, DELETE, CANCEL, STATE
    }

    private Requests currRequest;


    public ClientRequest(Requests currRequest) {
        this.currRequest = currRequest;
    }

    public Requests getCurrRequest() {
        return currRequest;
    }
}
