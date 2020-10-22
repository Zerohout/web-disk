package com.sepo.web.disk.client.controllers;


import com.sepo.web.disk.client.ClientApp;
import com.sepo.web.disk.client.Helpers.OnActionCallback;
import com.sepo.web.disk.client.network.Network;
import com.sepo.web.disk.common.models.ClientEnum;
import com.sepo.web.disk.common.models.Folder;
import com.sepo.web.disk.common.models.FileInfo;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.MouseDragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ResourceBundle;


public class FileManagerController implements Initializable, OnActionCallback {
    @FXML
    private Button clientRefreshBtn;
    @FXML
    private Button clientDownToServerBtn;
    @FXML
    private Button clientDeleteBtn;
    @FXML
    private Button clientMoveBtn;
    @FXML
    private Button clientAcceptBtn;
    @FXML
    private Button clientCancelBtn;
    @FXML
    private Button serverRefreshBtn;
    @FXML
    private Button serverDownFromServerBtn;
    @FXML
    private Button serverDeleteBtn;
    @FXML
    private Button serverMoveBtn;
    @FXML
    private Button serverAcceptBtn;
    @FXML
    private Button serverCancelBtn;

    private final String clientFolderName = "downloaded";
    private OnActionCallback mainCallback;
    private Folder serverFolder;

    private static final Logger logger = LogManager.getLogger(FileManagerController.class);
    @FXML
    private TreeView<FileInfo> serverFilesTView;

    @FXML
    private TreeView<FileInfo> clientFilesTView;
    @FXML
    private ListView<FileInfo> clientTransferFilesLView;

    private ObservableList<FileInfo> clientFiles = FXCollections.observableArrayList();

    public FileManagerController() throws IOException {
        if (Files.notExists(Path.of(clientFolderName))) {
            Files.createDirectory(Path.of(clientFolderName));
        }

    }


    public void clientDragOverAction(DragEvent dragEvent) {
        if (dragEvent.getDragboard().hasFiles()) {
            dragEvent.acceptTransferModes(TransferMode.LINK);
        }

    }

    public void clientDragDroppedAction(DragEvent dragEvent) throws IOException {
        File file = dragEvent.getDragboard().getFiles().get(0);
        clientFiles.add(new FileInfo(file.toPath()));
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        Network.mainHandler.setOtherCallback(this);
        initButtons();
        initClientTransferFilesLView();
        refreshClientFilesTView();
        initTreeViews();
        logger.info("send initialize request");
        mainCallback.callback(ClientEnum.State.REFRESHING, ClientEnum.StateWaiting.TRANSFER);
        mainCallback.callback(ClientEnum.Request.REFRESH);
    }

    private void initButtons() {
        setBtnIcon("refresh", clientRefreshBtn);
        setBtnIcon("downToServer", clientDownToServerBtn);
        setBtnIcon("delete", clientDeleteBtn);
        setBtnIcon("move", clientMoveBtn);
        setBtnIcon("accept", clientAcceptBtn);
        setBtnIcon("cancel", clientCancelBtn);
        setBtnIcon("refresh", serverRefreshBtn);
        setBtnIcon("downFromServer", serverDownFromServerBtn);
        setBtnIcon("delete", serverDeleteBtn);
        setBtnIcon("move", serverMoveBtn);
        setBtnIcon("accept", serverAcceptBtn);
        setBtnIcon("cancel", serverCancelBtn);
    }


    private void setBtnIcon(String iconName, Button btn) {
        var icon = new ImageView(new Image(ClientApp.class.getResourceAsStream("/com/sepo/web/disk/icons/" + iconName + ".png")));
        icon.setFitWidth(20);
        icon.setFitHeight(20);
        btn.setGraphic(icon);
    }


    private void refreshClientFilesTView() {
        clientFilesTView.refresh();
        var root = new TreeItem<FileInfo>();
        root.setExpanded(true);
        clientFilesTView.setRoot(root);
        try {
            setClientFiles(Path.of(clientFolderName), root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setClientFiles(Path path, TreeItem<FileInfo> root) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
            for (var p : stream) {
                var isDirectory = Files.isDirectory(p);
                var fileInfo = new FileInfo(p);
                var treeItem = new TreeItem<>(fileInfo);
                treeItem.setGraphic(getIcon(fileInfo));
                root.getChildren().add(treeItem);
                if (isDirectory) {
                    setClientFiles(p, treeItem);
                }
            }
        }
    }


    private void refreshServerFilesTView() {
        serverFilesTView.refresh();
        var serverRoot = new TreeItem<FileInfo>();
        serverRoot.setExpanded(true);
        serverFilesTView.setRoot(serverRoot);
        setServerFiles(serverFolder, serverRoot);
    }

    private void setServerFiles(Folder rootFolder, TreeItem<FileInfo> root) {
        for (var folder : rootFolder.getFolders()) {
            var rootItem = new TreeItem<>(folder.getFileInfo());
            rootItem.setGraphic(getIcon(folder.getFileInfo()));
            root.getChildren().add(rootItem);
            setServerFiles(folder, rootItem);
        }
        for (var file : rootFolder.getFiles()) {
            var treeItem = new TreeItem<>(file);
            treeItem.setGraphic(getIcon(file));
            root.getChildren().add(treeItem);
        }
    }

    private ImageView getIcon(FileInfo fileInfo) {
        var icon = new ImageView(fileInfo.getIcon());
        icon.setFitWidth(17);
        icon.setFitHeight(17);
        return icon;
    }

    private void initClientTransferFilesLView() {
        clientTransferFilesLView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        clientTransferFilesLView.setItems(clientFiles);
    }

    private void initTreeViews() {
        clientFilesTView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        serverFilesTView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }


    @Override
    public void setOtherCallback(OnActionCallback callback) {
        logger.info("get callback");
        this.mainCallback = callback;
    }

    @Override
    public void callback(Object... args) {
        if (args.length == 1) {
            if (args[0] instanceof Folder) {
                serverFolder = (Folder) args[0];
                Platform.runLater(this::refreshServerFilesTView);

            }
        }
    }

    @FXML
    public void clientTViewDragOverAction(DragEvent dragEvent) {
        if (dragEvent.getDragboard().hasFiles()) {
            dragEvent.acceptTransferModes(TransferMode.ANY);
        }
    }

    @FXML
    public void clientTViewDragDroppedAction(DragEvent dragEvent) {
        File file = dragEvent.getDragboard().getFiles().get(0);
        var fileInfo = new FileInfo(file.toPath());
        clientFilesTView.getRoot().getChildren().add(new TreeItem<>(fileInfo));
        try {
            Files.copy(file.toPath(), Path.of(clientFolderName).resolve(file.getName()));
            ClientApp.setScene("fileManager");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @FXML
    public void serverTViewDragOverAction(DragEvent dragEvent) {
        if (dragEvent.getDragboard().hasFiles()) {
            dragEvent.acceptTransferModes(TransferMode.ANY);
        }
    }

    @FXML
    public void serverTViewDragDroppedAction(DragEvent dragEvent) {
        File file = dragEvent.getDragboard().getFiles().get(0);
        var fileInfo = new FileInfo(file.toPath());
        mainCallback.callback(fileInfo);
    }

    public void serverTViewMouseDragOverAction(MouseDragEvent mouseDragEvent) {
        mouseDragEvent.copyFor(mouseDragEvent.getGestureSource(), mouseDragEvent.getTarget());
    }

    public void clientFilesTViewClickAction(MouseEvent mouseEvent) {
        if (clientFilesTView.getSelectionModel().getSelectedItems().size() > 0) {
            if (mouseEvent.getClickCount() == 1) {
                clientCancelBtn.setDisable(false);
            }

        }
    }

    public void clientCancelBtnAction(ActionEvent actionEvent) {
        ((Button) actionEvent.getSource()).setDisable(true);
        clientFilesTView.getSelectionModel().clearSelection();
    }

    public void clientRefreshBtnAction(ActionEvent actionEvent) {
        refreshClientFilesTView();
    }

    public void serverFilesTViewClickAction(MouseEvent mouseEvent) {
        if (serverFilesTView.getSelectionModel().getSelectedItems().size() > 0) {
            if (mouseEvent.getClickCount() == 1) {
                serverCancelBtn.setDisable(false);
            }

        }
    }

    public void serverCancelBtnAction(ActionEvent actionEvent) {
        ((Button) actionEvent.getSource()).setDisable(true);
        serverFilesTView.getSelectionModel().clearSelection();
    }

    public void serverRefreshBtnAction(ActionEvent actionEvent) {
        mainCallback.callback(ClientEnum.State.REFRESHING, ClientEnum.StateWaiting.TRANSFER);
        mainCallback.callback(ClientEnum.Request.REFRESH);
    }
}
