package com.sepo.web.disk.client.controllers;

import com.sepo.web.disk.client.ClientApp;
import com.sepo.web.disk.client.Helpers.MainHelper;
import com.sepo.web.disk.common.models.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class SignUpController implements Initializable {
    @FXML
    private Label successLbl;
    @FXML
    private Label errorLbl;
    @FXML
    private TextField sighUpEmailTField;
    @FXML
    private TextField signUpRepEmailTField;
    @FXML
    private PasswordField signUpPassPField;
    @FXML
    private PasswordField signUpRepPassPField;

    @FXML
    private void backAction(ActionEvent actionEvent) throws IOException {
        ClientApp.setScene("signIn");
        MainHelper.setSignUpController(null);
    }

    @FXML
    private void signUpAction(ActionEvent actionEvent) {
        if (!sighUpEmailTField.getText().equals(signUpRepEmailTField.getText())) {
            errorLbl.setVisible(true);
            errorLbl.setText("Emails doesn't matches");
            return;
        }
        if (!signUpPassPField.getText().equals(signUpRepPassPField.getText())) {
            errorLbl.setVisible(true);
            errorLbl.setText("Passwords doesn't matches");
            return;
        }

        MainHelper.setState(ClientEnum.State.REG, ClientEnum.StateWaiting.RESULT);
        MainHelper.sendUserToServer(new User(sighUpEmailTField.getText(), signUpPassPField.getText()));
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
        MainHelper.setSignUpController(this);
    }


    public void showSignUpPFieldAction(ActionEvent actionEvent) {

    }

    public void showSignUpRepPassPFieldAction(ActionEvent actionEvent) {

    }
}
