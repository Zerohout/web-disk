package com.sepo.web.disk.client.controllers;


import com.sepo.web.disk.common.models.FileInfo;
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
import java.util.ResourceBundle;




public class FileManagerController implements Initializable {
    private final String CLIENT_FOLDER_NAME = "downloaded";
    private final String SERVER_STORAGE_NAME = "serverStorage";

    private static final Logger logger = LogManager.getLogger(FileManagerController.class);
    @FXML
    private TreeView<FileInfo> serverStorageTView;

    @FXML
    private TreeView<FileInfo> clientDownloadedFilesTView;
    @FXML
    private ListView<FileInfo> clientTransferFilesLView;

    private ObservableList<FileInfo> clientFiles = FXCollections.observableArrayList();

    public FileManagerController() throws IOException {
        if (Files.notExists(Path.of(CLIENT_FOLDER_NAME))) {
            Files.createDirectory(Path.of(CLIENT_FOLDER_NAME));
        }
        if (Files.notExists(Path.of(SERVER_STORAGE_NAME))) {
            Files.createDirectory(Path.of(SERVER_STORAGE_NAME));
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
        initClientTransferFilesLView();
        try {
            var root = new TreeItem<FileInfo>();
            root.setExpanded(true);
            clientDownloadedFilesTView.setRoot(root);
            initClientDownloadedFilesTView(Path.of(CLIENT_FOLDER_NAME), root);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void initClientDownloadedFilesTView(Path test, TreeItem<FileInfo> root) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(test)) {
            for (var path : stream) {
                var isDirectory = Files.isDirectory(path);
                var file = new File(path.toUri());
                var fileInfo = new FileInfo()
                        .setFileFullName(file.getName())
                        .setFilePath(file.getAbsolutePath())
                        .setFileSize(Files.size(path))
                        .isFolder(isDirectory)
                        .setIcon(path);
                var treeItem = new TreeItem<>(fileInfo);

                var icon = new ImageView(fileInfo.getIcon());
                icon.setFitWidth(15);
                icon.setFitHeight(15);
                treeItem.setGraphic(icon);
                root.getChildren().add(treeItem);
                if (isDirectory) {
                    initClientDownloadedFilesTView(path, treeItem);
                }
            }
        }
    }

    private void initClientTransferFilesLView() {
        clientTransferFilesLView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        clientTransferFilesLView.setItems(clientFiles);
    }


}
