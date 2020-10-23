package com.sepo.web.disk.client.controllers;


import com.sepo.web.disk.client.ClientApp;
import com.sepo.web.disk.client.Helpers.OnActionCallback;
import com.sepo.web.disk.client.network.Network;
import com.sepo.web.disk.common.helpers.MainHelper;
import com.sepo.web.disk.common.models.ClientEnum;
import com.sepo.web.disk.common.models.Folder;
import com.sepo.web.disk.common.models.FileInfo;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.stage.FileChooser;
import javafx.util.Callback;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;


public class FileManagerController implements Initializable, OnActionCallback {
    private static final Logger logger = LogManager.getLogger(FileManagerController.class);
    @FXML
    private Button clientRefreshBtn;
    @FXML
    private Button clientAddBtn;
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
    private Button serverAddBtn;
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
    @FXML
    private TreeView<FileInfo> serverFilesTView;
    @FXML
    private TreeView<FileInfo> clientFilesTView;
    @FXML
    private ListView<FileInfo> clientFileInfoLView;

    private final String clientFolderName = "downloaded";
    private OnActionCallback mainCallback;
    private Folder serverFolder;
    private Folder clientFolder;
    private ObservableList<FileInfo> clientFiles = FXCollections.observableArrayList();


    public FileManagerController() throws IOException {
        if (Files.notExists(Path.of(clientFolderName))) {
            Files.createDirectory(Path.of(clientFolderName));
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        Network.mainHandler.setOtherCallback(this);
        clientFolder = new Folder(new FileInfo(Path.of(clientFolderName)));
        initButtons();
        initTreeViews();
        initClientTransferFilesLView();
        refreshTView(clientFilesTView, clientFolder);
        logger.info("send initialize request");
        mainCallback.callback(ClientEnum.State.REFRESHING, ClientEnum.StateWaiting.TRANSFER);
        mainCallback.callback(ClientEnum.Request.REFRESH);
    }

    private void initButtons() {
        setBtnIcon("refresh", clientRefreshBtn);
        setBtnIcon("add", clientAddBtn);
        setBtnIcon("downToServer", clientDownToServerBtn);
        setBtnIcon("delete", clientDeleteBtn);
        setBtnIcon("move", clientMoveBtn);
        setBtnIcon("accept", clientAcceptBtn);
        setBtnIcon("cancel", clientCancelBtn);
        setBtnIcon("refresh", serverRefreshBtn);
        setBtnIcon("add", serverAddBtn);
        setBtnIcon("downFromServer", serverDownFromServerBtn);
        setBtnIcon("delete", serverDeleteBtn);
        setBtnIcon("move", serverMoveBtn);
        setBtnIcon("accept", serverAcceptBtn);
        setBtnIcon("cancel", serverCancelBtn);
    }

    private void initClientTransferFilesLView() {
        clientFileInfoLView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        clientFileInfoLView.setItems(clientFiles);
    }

    private void initTreeViews() {
        clientFilesTView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        setTViewCellFactory(clientFilesTView);
        serverFilesTView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        setTViewCellFactory(serverFilesTView);
    }

    private static final class EditableTreeCell extends TreeCell<FileInfo> {
        private TextField textField;

        @Override
        public void updateSelected(boolean b) {
            super.updateSelected(b);
            if (b) {
                for (var item : getTreeView().getSelectionModel().getSelectedItems()) {
                    logger.info(item.getValue().getName() + " is selected!");
                }
            } else {
                if (getTreeView().getSelectionModel().getSelectedItems().size() == 0) {
                    logger.info("Empty!");
                } else {
                    for (var item : getTreeView().getSelectionModel().getSelectedItems()) {
                        logger.info(item.getValue().getName() + " still selected!");
                    }
                }

            }
        }

        @Override
        protected void updateItem(FileInfo fileInfo, boolean empty) {
            super.updateItem(fileInfo, empty);
            if (empty) {
                setText(null);
                setGraphic(null);
            } else {
                if (isEditing()) {
                    if (textField != null) {
                        textField.setText(getFileName());
                    }
                    setText(null);
                    setGraphic(textField);
                } else {
                    setText(getFileName());
                    setGraphic(getGraphics());
                }
            }
        }

        @Override
        public void startEdit() {
            super.startEdit();
            if (textField == null) {
                createTextField();
            }
            setText(null);
            setGraphic(textField);
            var extSize = FilenameUtils.getExtension(textField.getText()).length() + 1;
            textField.requestFocus();
            textField.selectRange(0, textField.getText().length() - extSize);
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();
            setText(getFileName());
            setGraphic(getGraphics());
            textField = null;
        }

        private void createTextField() {
            textField = new TextField(getItem() == null ? "" : getItem().getName());
            textField.setOnKeyReleased(keyEvent -> {
                if (keyEvent.getCode() == KeyCode.ENTER) {
                    var editedFileInfo = (getItem() == null ? new FileInfo() : getItem());
                    editedFileInfo.setName(textField.getText());
                    commitEdit(editedFileInfo);
                } else if (keyEvent.getCode() == KeyCode.ESCAPE) {
                    cancelEdit();
                }
            });
        }

        private String getFileName() {
            return getItem() == null ? "" : getItem().getName();
        }

        private ImageView getGraphics() {
            return getItem() == null ? new ImageView() : new ImageView(getItem().getIcon());
        }

    }

    private void setTViewCellFactory(TreeView<FileInfo> treeView) {
        treeView.setCellFactory(fileInfoTreeView -> new EditableTreeCell());
    }

    private void setBtnIcon(String iconName, Button btn) {
        var icon = new ImageView(new Image(getClass().getResourceAsStream("/com/sepo/web/disk/icons/" + iconName + ".png"), 25, 25, true, true));
        btn.setGraphic(icon);
    }

    private void refreshTView(TreeView<FileInfo> treeView, Folder folder) {
        if (folder == clientFolder) {
            MainHelper.createFileTree(Path.of(clientFolderName), clientFolder);
        }
        treeView.refresh();
        var root = new TreeItem<FileInfo>();
        root.setExpanded(true);
        treeView.setRoot(root);
        setTViewFiles(folder, root);
        if (folder == clientFolder) {
            clientFolder = new Folder(new FileInfo(Path.of(clientFolderName)));
        }
    }

    private void setTViewFiles(Folder rootFolder, TreeItem<FileInfo> root) {
        for (var folder : rootFolder.getFolders()) {
            var rootItem = new TreeItem<>(folder.getFileInfo());
            root.getChildren().add(rootItem);
            setTViewFiles(folder, rootItem);
        }
        for (var file : rootFolder.getFiles()) {
            var treeItem = new TreeItem<>(file);
            root.getChildren().add(treeItem);
        }
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
                Platform.runLater(() -> refreshTView(serverFilesTView, serverFolder));

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
    public void serverTViewDragOverAction(DragEvent dragEvent) {
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
    public void serverTViewDragDroppedAction(DragEvent dragEvent) {
        File file = dragEvent.getDragboard().getFiles().get(0);
        var fileInfo = new FileInfo(file.toPath());
        mainCallback.callback(fileInfo);
    }

    public void serverTViewMouseDragOverAction(MouseDragEvent mouseDragEvent) {
        mouseDragEvent.copyFor(mouseDragEvent.getGestureSource(), mouseDragEvent.getTarget());
    }

    public void clientFilesTViewClickAction(MouseEvent mouseEvent) {
        var selectedItemsCount = clientFilesTView.getSelectionModel().getSelectedItems().size();
        clientAddBtn.setDisable(selectedItemsCount > 1);

        if (selectedItemsCount > 0) {
            if (mouseEvent.getClickCount() == 1) {
                clientCancelBtn.setDisable(false);
                clientDeleteBtn.setDisable(false);
            }
        } else {
            clientCancelBtn.setDisable(true);
            clientDeleteBtn.setDisable(true);
        }
    }

    public void clientCancelBtnAction(ActionEvent actionEvent) {
        ((Button) actionEvent.getSource()).setDisable(true);
        clientFilesTView.getSelectionModel().clearSelection();
    }

    public void clientRefreshBtnAction(ActionEvent actionEvent) {
        refreshTView(clientFilesTView, clientFolder);
    }

    public void serverFilesTViewClickAction(MouseEvent mouseEvent) {
        var selectedItemsCount = serverFilesTView.getSelectionModel().getSelectedItems().size();
        serverAddBtn.setDisable(selectedItemsCount < 2);

        if (selectedItemsCount > 0) {
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

    public void serverAddBtnAction(ActionEvent actionEvent) {
        var filesInfo = new ArrayList<FileInfo>();
        var fileChooser = new FileChooser();
        fileChooser.setTitle("Выберите файл для отправки на сервер");
        var files = new ArrayList<>(fileChooser.showOpenMultipleDialog(ClientApp.getStage()));
        for (var file : files) {
            filesInfo.add(new FileInfo(file.toPath()));
        }
        mainCallback.callback(filesInfo, files);
    }

    public void clientAddBtnAction(ActionEvent actionEvent) {
        var fileChooser = new FileChooser();
        fileChooser.setTitle("Выберите файл для копирования в папку загрузок");
        var files = new ArrayList<>(fileChooser.showOpenMultipleDialog(ClientApp.getStage()));
        Path destinationPath;
        if (clientFilesTView.getSelectionModel().getSelectedItems().size() == 0) {
            destinationPath = Path.of(clientFolderName);
        } else {
            var selectedFileInfo = clientFilesTView.getSelectionModel().getSelectedItems().get(0).getValue();
            if (selectedFileInfo.isFolder()) {
                destinationPath = selectedFileInfo.getPath();
            } else {
                destinationPath = Path.of(new File(selectedFileInfo.getAbsolutePath()).getParent());
            }
        }
        try {
            for (var file : files) {
                Files.copy(file.toPath(), destinationPath.resolve(file.getName()), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            refreshTView(clientFilesTView, clientFolder);
        }
    }

    public void clientDeleteBtnAction(ActionEvent actionEvent) {
        var selectedFilesInfo = clientFilesTView.getSelectionModel().getSelectedItems()
                .parallelStream()
                .map(TreeItem::getValue)
                .collect(Collectors.toCollection(ArrayList::new));
        try {
            for (var fileInfo : selectedFilesInfo) {
                Files.delete(fileInfo.getPath());
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            refreshTView(clientFilesTView, clientFolder);
        }
    }


//    private void printAllFiles(ArrayList<File> files){
//        for(var file : files){
//            if(Files.isDirectory(file.toPath())){
//                try {
//                    Files.walkFileTree(file.toPath(), new FileVisitor<>() {
//                        @Override
//                        public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
//                            logger.info("preVisitDirectory " + path.toAbsolutePath());
//                            return FileVisitResult.CONTINUE;
//                        }
//
//                        @Override
//                        public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
//                            logger.info("visitFile " + path.toAbsolutePath());
//                            return FileVisitResult.CONTINUE;
//                        }
//
//                        @Override
//                        public FileVisitResult visitFileFailed(Path path, IOException e) throws IOException {
//                            logger.info("visitFileFailed " + path.toAbsolutePath());
//                            return FileVisitResult.CONTINUE;
//                        }
//
//                        @Override
//                        public FileVisitResult postVisitDirectory(Path path, IOException e) throws IOException {
//                            logger.info("postVisitDirectory " + path.toAbsolutePath());
//                            return FileVisitResult.CONTINUE;
//                        }
//                    });
//                } catch (IOException e) {
//                    e.printStackTrace();
//                } {
//
//                }
//            }
//        }
//    }
}
