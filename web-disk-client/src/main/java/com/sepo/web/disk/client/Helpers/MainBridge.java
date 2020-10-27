package com.sepo.web.disk.client.Helpers;

import com.sepo.web.disk.client.controllers.*;
import com.sepo.web.disk.client.network.Network;
import com.sepo.web.disk.common.models.*;
import com.sepo.web.disk.common.service.ObjectEncoderDecoder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.ReferenceCounted;
import javafx.application.Platform;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

public class MainBridge {
    private static final Logger logger = LogManager.getLogger(MainBridge.class);
    private static ClientFilesController clientFilesController;
    private static ServerFilesController serverFilesController;
    private static SignInController signInController;
    private static SignUpController signUpController;

    public static void refreshServerFiles(Folder folder) {
        Platform.runLater(() -> ControlPropertiesHelper.refreshTView(serverFilesController.getFilesTView(), folder));
    }

    public static void refreshServerFiles() {
        serverFilesController.getRefreshBtn().fire();
    }
    public static void refreshClientFiles(){
        Platform.runLater(() -> clientFilesController.getRefreshBtn().fire());
    }

    public static void setState(ClientEnum.State state, ClientEnum.StateWaiting stateWaiting) {
        if (Network.mainHandler != null) {
            Network.mainHandler.setState(state, stateWaiting);
        } else {
            Network.authHandler.setState(state, stateWaiting);
        }
    }

    public static void sendMainHandlerRequest(ClientEnum.Request request, ClientEnum.RequestType requestType) {
        if (Network.mainHandler != null) {
            Network.mainHandler.sendRequest(request, requestType);
        }
    }

    public static void setSignInErrorControls(String errorText, boolean isErrorVisible, boolean isRefreshConnBtnVisible) {
        if (signInController == null) return;
        Platform.runLater(() -> signInController.setErrorControls(errorText, isErrorVisible, isRefreshConnBtnVisible));
    }

    public static void giveAuthResult(ServerEnum.Respond respond) {
        if (signInController == null) return;
        Platform.runLater(() -> signInController.respondToAuthResult(respond));
        logger.info("regController - " + signUpController);
    }

    public static void giveRegResult(ServerEnum.Respond respond) {
        Platform.runLater(() -> signUpController.respondToAuthResult(respond));
    }

    public static void setClientFilesController(ClientFilesController clientFilesController) {
        MainBridge.clientFilesController = clientFilesController;
    }

    public static void setServerFilesController(ServerFilesController serverFilesController) {
        MainBridge.serverFilesController = serverFilesController;
    }

    public static void setSignInController(SignInController signInController) {
        MainBridge.signInController = signInController;
    }

    public static void setSignUpController(SignUpController signUpController) {
        MainBridge.signUpController = signUpController;
    }

    public static void connectToServer() {
        try {
            CountDownLatch networkStarter = new CountDownLatch(1);
            CountDownLatch handlerStarter = new CountDownLatch(1);
            new Thread(() -> Network.getInstance().start(networkStarter, handlerStarter)).start();
            handlerStarter.await();
            networkStarter.await();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    public static void sendMainHandlerByteBuf(ReferenceCounted bb, boolean isFlush) {
        Network.mainHandler.send(bb, isFlush);
    }

    public static void sendAuthHandlerByteBuf(ByteBuf bb, boolean isFLush) {
        Network.authHandler.send(bb, isFLush);
    }

    public static void uploadFiles(ArrayList<FileInfo> fileInfoList) {
        serverFilesController.uploadFiles(fileInfoList);
    }

    public static void downloadFiles(ArrayList<FileInfo> fileInfoList) {
        clientFilesController.downloadFiles(fileInfoList);
    }

    public static void mainPackAndSendObj(Object object) {
        var msg = ObjectEncoderDecoder.EncodeObjToByteBuf(object);
        var msgSize = msg.readableBytes();
        var msgSizeBB = ByteBufAllocator.DEFAULT.directBuffer(4);
        msgSizeBB.writeInt(msgSize);
        sendMainHandlerByteBuf(msgSizeBB, false);
        sendMainHandlerByteBuf(msg, true);
    }

    public static void authPackAndSendObj(Object object) {
        var msg = ObjectEncoderDecoder.EncodeObjToByteBuf(object);
        var msgSize = msg.readableBytes();
        var msgSizeBB = ByteBufAllocator.DEFAULT.directBuffer(4);
        msgSizeBB.writeInt(msgSize);
        sendAuthHandlerByteBuf(msgSizeBB, false);
        sendAuthHandlerByteBuf(msg, true);
    }

    public static void setGettingFilesCount(int count){
        Network.mainHandler.setGettingFilesCount(count);
    }
}
