package com.sepo.web.disk.client.Helpers;

public interface OnActionCallback {
    void setCallback(OnActionCallback callback);

    OnActionCallback getCallback();

    void callback(Object... args);

}
