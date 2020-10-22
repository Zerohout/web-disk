package com.sepo.web.disk.client.controllers;

import com.sepo.web.disk.client.ClientApp;
import com.sepo.web.disk.client.Helpers.*;
import com.sepo.web.disk.client.network.Network;
import com.sepo.web.disk.common.models.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
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

    //TODO: передавать User иным способом (убрать статику)
    public static User currUser;

    private static final Logger logger = LogManager.getLogger(SignInController.class);


    private OnActionCallback otherCallback;

    //TODO: переместить соединение с сервером, т.к. идет reconnect при переходе на данную сцену
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        var connection = new Thread(this::connectToServer);
        connection.setDaemon(true);
        Platform.runLater(connection::start);

    }

    @FXML
    private void signUpAction(ActionEvent actionEvent) throws IOException {
        ClientApp.setScene("signUp");
    }

    @FXML
    private void showPassAction(ActionEvent actionEvent) {
        if (signInPassPField.getText().isEmpty()) {
            return;
        }


        ControlPropertiesHelper.setPassControlsProp(signInPassTField, signInPassPField, signInShowPassBtn);
    }

    @FXML
    public void signInAction(ActionEvent actionEvent) {
        otherCallback.callback(ClientEnum.State.AUTH, ClientEnum.StateWaiting.RESULT);
        otherCallback.callback(new User(signInEmailTField.getText(), signInPassPField.getText()));
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
        Network.authHandler.setOtherCallback(this);
    }

    private void connectToServer() {
        try {
            CountDownLatch networkStarter = new CountDownLatch(1);
            CountDownLatch handlerStarter = new CountDownLatch(1);
            new Thread(() -> Network.getInstance().start(networkStarter, handlerStarter)).start();
            handlerStarter.await();
            Network.authHandler.setOtherCallback(this);
            networkStarter.await();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    // для обмена сообщениями между этим классом и NetworkHandler
    @Override
    public void callback(Object... args) {
        if (args.length == 1) {
            if (args[0] instanceof ServerEnum.Respond) {
                var resp = (ServerEnum.Respond) args[0];
                if (resp == ServerEnum.Respond.SUCCESS) {
                    Platform.runLater(() -> {
                        try {
                            logger.info("set scene to fileManager");
                            ClientApp.setScene("fileManager");

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                } else {
                    logger.info("set error msg to UI");
                    Platform.runLater(() -> {
                        signInErrorLbl.setText("Error login or password.");
                        signInErrorLbl.setVisible(true);
                    });
                }
            }
        }if(args.length == 3){
            Platform.runLater(() -> {
                signInErrorLbl.setText((String) args[0]);
                signInErrorLbl.setVisible((Boolean) args[1]);
                signInRefreshConnectionBtn.setVisible((Boolean) args[2]);
            });
        }
    }


    @Override
    public void setOtherCallback(OnActionCallback callback) {
        otherCallback = callback;
    }

}


