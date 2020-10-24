package com.sepo.web.disk.client.Helpers;

import com.sepo.web.disk.client.controllers.ClientFilesController;
import com.sepo.web.disk.client.controllers.ServerFilesController;
import com.sepo.web.disk.client.controllers.SignInController;
import com.sepo.web.disk.client.controllers.SignUpController;
import com.sepo.web.disk.client.handlers.MainHandler;
import com.sepo.web.disk.client.network.Network;
import com.sepo.web.disk.common.models.ClientEnum;
import com.sepo.web.disk.common.models.Folder;
import com.sepo.web.disk.common.models.ServerEnum;
import com.sepo.web.disk.common.models.User;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCounted;
import javafx.application.Platform;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.CountDownLatch;

import static com.sepo.web.disk.client.Helpers.ControlPropertiesHelper.refreshTView;

public class MainHelper {
    private static final Logger logger = LogManager.getLogger(MainHelper.class);
    private static ClientFilesController clientFilesController;
    private static ServerFilesController serverFilesController;
    private static SignInController signInController;
    private static SignUpController signUpController;

    public static void refreshServerFiles(Folder folder) {
        Platform.runLater(() -> refreshTView(serverFilesController.getFilesTView(), folder));

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

    public static void sendUserToServer(User user) {
        Network.authHandler.sendUserData(user);

    }

    public static void setSignInErrorControls(String errorText, boolean isErrorVisible, boolean isRefreshConnBtnVisible) {
        if(signInController == null) return;
        Platform.runLater(() -> signInController.setErrorControls(errorText, isErrorVisible, isRefreshConnBtnVisible));
    }

    public static void giveAuthResult(ServerEnum.Respond respond) {
        if(signInController == null) return;
        Platform.runLater(() -> signInController.respondToAuthResult(respond));
        logger.info("regController - " + signUpController);

    }
    public static void giveRegResult(ServerEnum.Respond respond){
        Platform.runLater(() -> signUpController.respondToAuthResult(respond));
    }

    public static void setClientFilesController(ClientFilesController clientFilesController) {
        MainHelper.clientFilesController = clientFilesController;
    }

    public static void setServerFilesController(ServerFilesController serverFilesController) {
        MainHelper.serverFilesController = serverFilesController;
    }

    public static void setSignInController(SignInController signInController) {
        MainHelper.signInController = signInController;
    }

    public static void setSignUpController(SignUpController signUpController) {
        MainHelper.signUpController = signUpController;
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

    public static void sendByteBuf(ReferenceCounted bb, boolean isFlush){
        Network.mainHandler.send(bb,isFlush);
    }
}
