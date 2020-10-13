package com.sepo.web.disk.controllers;

import com.sepo.web.disk.ClientApp;
import com.sepo.web.disk.Helpers.ControlPropertiesHelper;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class SignInController implements Initializable {
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
    }
    public SignInController() {

    }

    @FXML
    private void signUp(ActionEvent actionEvent) throws IOException {
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
        if (signInEmailTField.getText().isEmpty() || signInEmailTField.getText().isBlank()) {
            signInErrorLbl.setVisible(true);
            signInErrorLbl.setText("email is empty");
        }
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


}


