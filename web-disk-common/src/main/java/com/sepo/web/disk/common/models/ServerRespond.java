package com.sepo.web.disk.common.models;

import java.io.Serializable;

public class ServerRespond extends SystemInfo implements Serializable {
    public enum Responds {
        AUTH, REG, UPDATE, SEND, GET, RENAME, RESULT
    }

    public enum Results {
        SUCCESS, FAILURE, PROCESSING
    }

    private Responds currRespond;
    private Results currResult;

    public ServerRespond(Responds currRespond, Results currResult) {
        this.currRespond = currRespond;
        this.currResult = currResult;
    }

    public Responds getCurrRespond() {
        return currRespond;
    }

    public Results getCurrResult() {
        return currResult;
    }
}
