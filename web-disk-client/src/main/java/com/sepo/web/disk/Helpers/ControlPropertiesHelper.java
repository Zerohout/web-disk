package com.sepo.web.disk.Helpers;

import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class ControlPropertiesHelper {
    public static void setPassControlsProp(TextField passTField, PasswordField passPField, Button passShowBtn) {
        var passTFieldIsVisible = passTField.isVisible();
        passTField.setVisible(!passTFieldIsVisible);
        passPField.setVisible(passTFieldIsVisible);
        passShowBtn.setText(passTFieldIsVisible ? "show" : "hide");
        if (passTFieldIsVisible) {
            var pass = passTField.getText();
            passPField.setText(pass);
            if (pass.isEmpty()) passShowBtn.setDisable(true);
            passTField.clear();

        } else {
            passTField.setText(passPField.getText());
        }
    }

    public static void passPFieldControlProp(PasswordField passPField, Button showPassBtn) {
        var pass = passPField.getText();
        var passIsEmptyOrBlank = pass.isEmpty() || pass.isBlank();
        if (showPassBtn.isDisable() == passIsEmptyOrBlank) return;
        showPassBtn.setDisable(passIsEmptyOrBlank);
    }

    public static void signInBtnControlProp(Button button, TextField... textFields) {
        for (var tField: textFields) {
            if(tField.getText().isBlank() || tField.getText().isEmpty()){
                if(!tField.isVisible()) continue;
                button.setDisable(true);
                return;
            }
        }
        button.setDisable(false);
    }
}
