package com.sepo.web.disk.client.controllers;

import com.sepo.web.disk.client.ClientApp;
import com.sepo.web.disk.client.Helpers.CallbackValue;
import com.sepo.web.disk.client.Helpers.ControlPropertiesHelper;
import com.sepo.web.disk.client.Helpers.OnActionCallback;
import com.sepo.web.disk.client.handlers.NetworkHandler;
import com.sepo.web.disk.client.network.Network;
import com.sepo.web.disk.common.models.ClientRequest;
import com.sepo.web.disk.common.models.ClientState;
import com.sepo.web.disk.common.models.ServerRespond;
import com.sepo.web.disk.common.models.User;
import com.sepo.web.disk.common.service.ObjectEncoderDecoder;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.CountDownLatch;

public class SignInController implements Initializable, OnActionCallback {
    @FXML
    private Button signInRefreshConnectionBtn;
    @FXML
    private Label signInErrorLbl;
    @FXML
    private Button signInShowPassBtn;
    @FXML
    private TextField signInPassTField;
    @FXML
    private Button signInBtn;
    @FXML
    private TextField signInEmailTField;
    @FXML
    private PasswordField signInPassPField;

    public static User getCurrUser() {
        return currUser;
    }

    private static User currUser;

    private static final Logger logger = LogManager.getLogger(SignInController.class);

    private OnActionCallback currCallback;
    private OnActionCallback netHandlerCallback;

    //TODO: переместить соединение с сервером
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        var connection = new Thread(this::connectToServer);
        connection.setDaemon(true);
        Platform.runLater(connection::start);

    }

    @FXML
    private void signUp(ActionEvent actionEvent) throws IOException {
        netHandlerCallback.callback(ClientState.State.STATE, ClientState.Wait.RESULT);
        netHandlerCallback.callback(new ClientRequest(ClientRequest.Requests.STATE));
        ClientApp.setScene("signUp");
    }

    @FXML
    private void showPass(ActionEvent actionEvent) {
        if (signInPassPField.getText().isEmpty()) {
            return;
        }

        ControlPropertiesHelper.setPassControlsProp(signInPassTField, signInPassPField, signInShowPassBtn);
    }

    public void signIn(ActionEvent actionEvent) {
        netHandlerCallback.callback(ClientState.State.AUTH, ClientState.Wait.RESPOND);
        currUser = new User(signInEmailTField.getText(), signInPassPField.getText());
        logger.info("Send AUTH request");
        netHandlerCallback.callback(new ClientRequest(ClientRequest.Requests.AUTH));
    }

    public void passPFieldAction(KeyEvent keyEvent) {
        ControlPropertiesHelper.passPFieldControlProp(signInPassPField, signInShowPassBtn);
        ControlPropertiesHelper.signInBtnControlProp(signInBtn, signInEmailTField, signInPassPField, signInPassTField);
    }


    public void passTFieldAction(KeyEvent keyEvent) {
        ControlPropertiesHelper.signInBtnControlProp(signInBtn, signInEmailTField, signInPassPField, signInPassTField);
    }

    public void emailTFieldAction(KeyEvent keyEvent) {
        ControlPropertiesHelper.signInBtnControlProp(signInBtn, signInEmailTField, signInPassPField, signInPassTField);
    }


    public void refreshConnectionAction(ActionEvent actionEvent) {
        var connection = new Thread(this::connectToServer);
        connection.setDaemon(true);
        Platform.runLater(connection::start);
        Network.getInstance().getNetworkHandler().setOtherCallback(this);
    }

    private void connectToServer() {
        try {
            CountDownLatch networkStarter = new CountDownLatch(1);
            Network.getInstance().setNetworkHandler();
            Network.getInstance().getNetworkHandler().setOtherCallback(this);
            new Thread(() -> Network.getInstance().start(networkStarter)).start();
            networkStarter.await();

        } catch (InterruptedException ex) {
            ex.printStackTrace();
            Network.getInstance().stop();
        }
    }

    // для обмена сообщениями между этим классом и NetworkHandler
    @Override
    public void callback(Object... args) {
        if (args.length == 1) {
            if (args[0] instanceof ServerRespond) {
                var resp = (ServerRespond) args[0];
                switch (resp.getCurrRespond()) {
                    case AUTH:
                        switch (resp.getCurrResult()) {
                            case PROCESSING:
                                logger.info("send User");
                                netHandlerCallback.callback(currUser);
                                break;
                            case SUCCESS:
                                Platform.runLater(() -> {
                                    try {
                                        ClientApp.setScene("fileManager");
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                });
                                break;
                            case FAILURE:
                                Platform.runLater(() -> {
                                    signInErrorLbl.setText("Error login or password.");
                                    signInErrorLbl.setVisible(true);
                                });
                                break;
                        }
                        break;
                }
            }
        } else if (args.length == 3) {
            Platform.runLater(() -> {
                signInErrorLbl.setText((String) args[0]);
                signInErrorLbl.setVisible((Boolean) args[1]);
                signInRefreshConnectionBtn.setVisible((Boolean) args[2]);
            });
        }
    }

    public void setCallback(OnActionCallback callback) {
        this.netHandlerCallback = callback;
    }

    @Override
    public OnActionCallback getCallback() {
        return this.currCallback;
    }
}


