package com.sepo.web.disk.client;

import com.sepo.web.disk.client.Helpers.MainBridge;
import com.sepo.web.disk.client.network.Network;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class ClientApp extends Application {
    private static final Logger logger = LogManager.getLogger(ClientApp.class);
    private static Scene scene;
    private static Stage stage;

    @Override
    public void start(Stage stage) throws IOException, InterruptedException {
        ClientApp.stage = stage;
        scene = new Scene(loadFXML("signIn"));
        stage.setScene(scene);
        stage.show();
        var connection = new Thread(MainBridge::connectToServer);
        connection.setDaemon(true);
        Platform.runLater(connection::start);
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        Network.getInstance().stop();
    }

    public static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    public static void setScene(String fxml) throws IOException {
        scene = new Scene(loadFXML(fxml));
        stage.setScene(scene);
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(ClientApp.class.getResource("/com/sepo/web/disk/views/" + fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static void main(String[] args) {
        launch();
    }

    public static Stage getStage(){
        return stage;
    }
}