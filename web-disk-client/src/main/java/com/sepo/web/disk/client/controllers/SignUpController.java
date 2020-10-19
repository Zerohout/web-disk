package com.sepo.web.disk.client.controllers;

import com.sepo.web.disk.client.ClientApp;
import com.sepo.web.disk.client.Helpers.OnActionCallback;
import com.sepo.web.disk.client.network.Network;
import com.sepo.web.disk.common.models.ClientRequest;
import com.sepo.web.disk.common.models.ClientState;
import com.sepo.web.disk.common.models.ServerRespond;
import com.sepo.web.disk.common.models.User;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;


import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class SignUpController implements Initializable, OnActionCallback {

    private OnActionCallback networkCallback;
    private User newUser;

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

        networkCallback.callback(ClientState.State.REG, ClientState.Wait.RESPOND);
        newUser = new User(sighUpEmailTField.getText(), signUpPassPField.getText());
        networkCallback.callback(new ClientRequest(ClientRequest.Requests.REG));
    }

    @Override
    public void setOtherCallback(OnActionCallback callback) {
        networkCallback = callback;
    }

    @Override
    public void callback(Object... args) {
        if (args.length == 1) {
            if (args[0] instanceof ServerRespond) {
                var resp = (ServerRespond) args[0];
                switch (resp.getCurrRespond()) {
                    case REG:
                        switch (resp.getCurrResult()) {
                            case PROCESSING:
                                networkCallback.callback(newUser);
                                break;
                            case SUCCESS:
                                Platform.runLater(() -> {
                                    if (errorLbl.isVisible()) {
                                        errorLbl.setVisible(false);
                                    }
                                    successLbl.setVisible(true);
                                    successLbl.setText("Registration is done. Please sign in.");
                                });
                                break;
                            case FAILURE:
                                Platform.runLater(() -> {
                                    if (successLbl.isVisible()) {
                                        successLbl.setVisible(false);
                                    }
                                    errorLbl.setVisible(true);
                                    errorLbl.setText("Registration error. Try another email.");
                                });
                                break;
                        }
                        break;
                }
            }
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        Network.getInstance().getNetworkHandler().setOtherCallback(this);
    }


}
