package com.sepo.web.disk.client.Helpers;

public interface OnActionCallback {
    void setOtherCallback(OnActionCallback callback);

    void callback(Object... args);

}
