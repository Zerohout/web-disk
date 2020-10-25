package com.sepo.web.disk.client.controls;

import com.sepo.web.disk.client.controllers.ServerFilesController;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class ServerFilesControl extends VBox {

    public ServerFilesControl() {
        var fxmlLoader = new FXMLLoader(getClass().getResource("/com/sepo/web/disk/views/filesViewer.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(new ServerFilesController());

        try{
            fxmlLoader.load();
        }catch (IOException ex){
            ex.printStackTrace();
        }
    }
}
