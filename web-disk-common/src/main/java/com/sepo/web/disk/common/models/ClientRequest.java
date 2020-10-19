package com.sepo.web.disk.common.models;

import java.io.Serializable;

public class ClientRequest extends SystemInfo implements Serializable {

    private boolean isMainFolder;
    private Requests currRequest;

    public enum Requests {
        AUTH, REG, UPDATE, SEND, GET, RENAME, MOVE, CREATE, DELETE, CANCEL, STATE
    }

    public ClientRequest(Requests currRequest) {
        this.currRequest = currRequest;
    }

    public boolean isMainFolder() {
        return isMainFolder;
    }

    public void isMainFolder(boolean mainFolder) {
        isMainFolder = mainFolder;
    }
    public Requests getCurrRequest() {
        return currRequest;
    }
}
