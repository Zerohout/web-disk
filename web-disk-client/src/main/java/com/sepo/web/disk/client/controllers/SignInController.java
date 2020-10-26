package com.sepo.web.disk.client.controllers;

import com.sepo.web.disk.client.ClientApp;
import com.sepo.web.disk.client.Helpers.*;
import com.sepo.web.disk.common.models.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
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

public class SignInController implements Initializable {
    private static final Logger logger = LogManager.getLogger(SignInController.class);
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

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        MainBridge.setSignInController(this);
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
        MainBridge.setState(ClientEnum.State.AUTH, ClientEnum.StateWaiting.RESULT);
        ByteBuf bb = ByteBufAllocator.DEFAULT.directBuffer(1);
        MainBridge.sendAuthHandlerByteBuf(bb.writeByte(ClientEnum.Request.AUTH.getValue()), true);
        MainBridge.authPackAndSendObj(new User(signInEmailTField.getText(), signInPassPField.getText()));
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
        var connection = new Thread(MainBridge::connectToServer);
        connection.setDaemon(true);
        Platform.runLater(connection::start);
    }

    // среагировать на результат аутентификации
    public void respondToAuthResult(ServerEnum.Respond respond) {
        if (respond == ServerEnum.Respond.SUCCESS) {
            try {
                logger.info("set scene to fileManager");
                ClientApp.setScene("fileManager");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            logger.info("set error msg to UI");
            signInErrorLbl.setText("Error login or password.");
            signInErrorLbl.setVisible(true);
        }
    }

    public void setErrorControls(String errorText, boolean isErrorVisible, boolean isRefreshConnBtnVisible) {
        signInErrorLbl.setText(errorText);
        signInErrorLbl.setVisible(isErrorVisible);
        signInRefreshConnectionBtn.setVisible(isRefreshConnBtnVisible);
    }
}


