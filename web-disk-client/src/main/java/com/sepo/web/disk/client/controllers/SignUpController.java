package com.sepo.web.disk.client.controllers;

import com.sepo.web.disk.client.ClientApp;
import com.sepo.web.disk.client.Helpers.ControlPropertiesHelper;
import com.sepo.web.disk.client.Helpers.MainBridge;
import com.sepo.web.disk.common.models.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
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

public class SignUpController implements Initializable {
    private static final Logger logger = LogManager.getLogger(SignUpController.class);
    @FXML
    private Button signUpBtn;
    @FXML
    private Button signUpShowPassBtn;
    @FXML
    private Button signUpShowRepPassBtn;
    @FXML
    private TextField signUpPassTField;
    @FXML
    private TextField signUpRepPassTField;
    @FXML
    private Label successLbl;
    @FXML
    private Label errorLbl;
    @FXML
    private TextField signUpEmailTField;
    @FXML
    private TextField signUpRepEmailTField;
    @FXML
    private PasswordField signUpPassPField;
    @FXML
    private PasswordField signUpRepPassPField;

    @FXML
    private void backAction(ActionEvent actionEvent) throws IOException {
        ClientApp.setScene("signIn");
        MainBridge.setSignUpController(null);
    }

    @FXML
    private void signUpAction(ActionEvent actionEvent) {
        if (!signUpEmailTField.getText().equals(signUpRepEmailTField.getText())) {
            errorLbl.setVisible(true);
            errorLbl.setText("Emails doesn't matches");
            return;
        }

        if (!signUpPassPField.getText().equals(signUpRepPassPField.getText())) {
            errorLbl.setVisible(true);
            errorLbl.setText("Passwords doesn't matches");
            return;
        }

        MainBridge.setState(ClientEnum.State.REG, ClientEnum.StateWaiting.RESULT);
        ByteBuf bb = ByteBufAllocator.DEFAULT.directBuffer(1);
        MainBridge.sendAuthHandlerByteBuf(bb.writeByte(ClientEnum.Request.REG.getValue()), true);
        MainBridge.authPackAndSendObj(new User(signUpEmailTField.getText(), signUpPassPField.getText()));
    }

    public void respondToAuthResult(ServerEnum.Respond respond) {
        if (respond == ServerEnum.Respond.SUCCESS) {
            if (errorLbl.isVisible()) errorLbl.setVisible(false);
            successLbl.setVisible(true);
            successLbl.setText("Registration is done. Please sign in.");
        } else {
            if (successLbl.isVisible()) successLbl.setVisible(false);
            errorLbl.setVisible(true);
            errorLbl.setText("Registration error. Try another email.");
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        MainBridge.setSignUpController(this);
    }

    @FXML
    public void showSignUpPFieldAction(ActionEvent actionEvent) {
        if (signUpPassPField.getText().isEmpty()) {
            return;
        }
        ControlPropertiesHelper.setPassControlsProp(signUpPassTField, signUpPassPField, signUpShowPassBtn);
    }

    @FXML
    public void showSignUpRepPassPFieldAction(ActionEvent actionEvent) {
        if (signUpRepPassPField.getText().isEmpty()) {
            return;
        }
        ControlPropertiesHelper.setPassControlsProp(signUpRepPassTField, signUpRepPassPField, signUpShowRepPassBtn);
    }

    @FXML
    public void passPFieldAction(KeyEvent keyEvent) {
        ControlPropertiesHelper.passPFieldControlProp(signUpPassPField, signUpShowPassBtn);
        validateTextFields();
    }

    @FXML
    public void textFieldAction(KeyEvent keyEvent) {
        validateTextFields();
    }

    @FXML
    public void passRepPFieldAction(KeyEvent keyEvent) {
        ControlPropertiesHelper.passPFieldControlProp(signUpRepPassPField, signUpShowRepPassBtn);
        validateTextFields();
    }

    private void validateTextFields(){
        ControlPropertiesHelper.signInBtnControlProp(signUpBtn, signUpEmailTField, signUpPassPField, signUpPassTField,
                signUpRepEmailTField, signUpRepPassPField, signUpRepPassTField);
    }
}
