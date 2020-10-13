package com.sepo.web.disk.controllers;

import com.sepo.web.disk.App;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;


import java.io.IOException;

public class SignUpController {
    @FXML
    private void cancel(ActionEvent actionEvent) throws IOException {
        App.setScene("main");
    }
}
