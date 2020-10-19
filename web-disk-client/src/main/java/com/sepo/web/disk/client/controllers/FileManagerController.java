package com.sepo.web.disk.client.controllers;


import com.sepo.web.disk.client.ClientApp;
import com.sepo.web.disk.client.Helpers.OnActionCallback;
import com.sepo.web.disk.client.network.Network;
import com.sepo.web.disk.common.models.ClientRequest;
import com.sepo.web.disk.common.models.ClientState;
import com.sepo.web.disk.common.models.FileInfo;
import com.sepo.web.disk.common.models.ServerRespond;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;


public class FileManagerController implements Initializable, OnActionCallback {
    private String clientFolderName = "downloaded";
    private String userEmail = "";
    private TreeItem<FileInfo> serverRoot;
    private ArrayList<FileInfo> serverFiles = new ArrayList<>();

    private String userServerFolderName;
    private OnActionCallback networkCallback;

    private static final Logger logger = LogManager.getLogger(FileManagerController.class);
    @FXML
    private TreeView<FileInfo> serverStorageTView;

    @FXML
    private TreeView<FileInfo> clientDownloadedFilesTView;
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
        clientFiles.add(new FileInfo().setFileFullName(file.getName()).setFilePath(file.getAbsolutePath()).setFileSize(Files.size(file.toPath())));
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        Network.getInstance().getNetworkHandler().setOtherCallback(this);
        userServerFolderName = SignInController.currUser.getEmail();
        initClientTransferFilesLView();
        try {
            var root = new TreeItem<FileInfo>();
            root.setExpanded(true);
            clientDownloadedFilesTView.setRoot(root);
            serverStorageTView.setRoot(serverRoot);
            initClientDownloadedFilesTView(Path.of(clientFolderName), root);
            initTreeViews();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //TODO: Разобраться с java.io.EOFException
//        networkCallback.callback(ClientState.State.UPDATE, ClientState.Wait.RESPOND);
//        networkCallback.callback(new ClientRequest(ClientRequest.Requests.UPDATE));


    }

    // получение и составление дерева директорий в папке downloaded
    private void initClientDownloadedFilesTView(Path path, TreeItem<FileInfo> root) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
            for (var p : stream) {
                var isDirectory = Files.isDirectory(p);
                var fileInfo = new FileInfo(p);
                var treeItem = new TreeItem<>(fileInfo);
                var icon = new ImageView(fileInfo.getIcon());
                icon.setFitWidth(17);
                icon.setFitHeight(17);
                treeItem.setGraphic(icon);
                root.getChildren().add(treeItem);
                if (isDirectory) {
                    initClientDownloadedFilesTView(p, treeItem);
                }
            }
        }
    }

    private void updateServerFiles(ArrayList<FileInfo> files, TreeItem<FileInfo> root) {
        var _root = root;

        for (var file : files) {
            var treeItem = new TreeItem<>(file);
            var icon = new ImageView(file.getIcon());
            icon.setFitWidth(17);
            icon.setFitHeight(17);
            treeItem.setGraphic(icon);
            _root.getChildren().add(treeItem);
            if (file.isFolder()) _root = treeItem;
        }
    }

    private void initClientTransferFilesLView() {
        clientTransferFilesLView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        clientTransferFilesLView.setItems(clientFiles);
    }

    private void initTreeViews() {
        clientDownloadedFilesTView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        serverStorageTView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }


    @Override
    public void setOtherCallback(OnActionCallback callback) {
        this.networkCallback = callback;
    }

    @Override
    public void callback(Object... args) {
        if (args.length == 1) {
            if (args[0] instanceof ServerRespond) {
                if (((ServerRespond) args[0]).getCurrResult() == ServerRespond.Results.SUCCESS) {
                    updateServerFiles(serverFiles, serverRoot);
                }
            }
        }
        if (args.length == 2) {
            if (args[0] == null && args[1] instanceof FileInfo) {
                var file = (FileInfo) args[1];
                serverFiles.add(file);
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
        clientDownloadedFilesTView.getRoot().getChildren().add(new TreeItem<>(fileInfo));
        try {
            Files.copy(file.toPath(),Path.of(clientFolderName).resolve(file.getName()));
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
        networkCallback.callback(fileInfo);
    }
}
