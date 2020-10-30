package com.sepo.web.disk.client.controls;

import com.sepo.web.disk.client.controllers.ClientFilesController;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class ClientFilesControl extends VBox {
    public ClientFilesControl() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/sepo/web/disk/views/filesViewer.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(new ClientFilesController());

        try{
            fxmlLoader.load();
        }catch (IOException ex){
            ex.printStackTrace();
        }
    }
}
