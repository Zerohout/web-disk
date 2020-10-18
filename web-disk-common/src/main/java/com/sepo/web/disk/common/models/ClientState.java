package com.sepo.web.disk.common.models;

import java.io.Serializable;

    public class ClientState extends SystemInfo implements Serializable {
        public enum State {
            IDLE, AUTH, REG, UPDATE, SEND, GET, RENAME, STATE
        }
        public enum Wait{
            RESPOND, RESULT
        }
        private ClientState.State currState;
        private ClientState.Wait currWait;

        public ClientState(ClientState.State currState, ClientState.Wait currWait) {
            this.currState = currState;
            this.currWait = currWait;
        }

        public ClientState.State getCurrState() {
            return currState;
        }

        public ClientState setCurrState(ClientState.State currState) {
            this.currState = currState;
            return this;
        }

        public ClientState.Wait getCurrWait() {
            return currWait;
        }

        public ClientState setCurrWait(ClientState.Wait currWait) {
            this.currWait = currWait;
            return this;
        }
    }

