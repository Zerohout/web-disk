package com.sepo.web.disk.client.controllers;

import com.sepo.web.disk.client.ClientApp;
import com.sepo.web.disk.client.Helpers.ControlPropertiesHelper;
import com.sepo.web.disk.client.Helpers.MainBridge;
import com.sepo.web.disk.common.models.*;
import io.netty.buffer.ByteBufAllocator;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.stage.FileChooser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.ResourceBundle;

import static com.sepo.web.disk.client.Helpers.ControlPropertiesHelper.*;

public class ClientFilesController extends FilesController implements Initializable {
    private static final Logger logger = LogManager.getLogger(ClientFilesController.class);

    private Folder clientFolder;


    public ClientFilesController() {
        super(false);
        if (Files.notExists(Path.of(CLIENT_FOLDER_PATH_NAME))) {
            try {
                Files.createDirectory(Path.of(CLIENT_FOLDER_PATH_NAME));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        MainBridge.setClientFilesController(this);

    }

    @Override
    public void renameFile(FileInfo oldValue, FileInfo newValue) {
        var oldFile = new File(oldValue.getAbsolutePath());
        oldFile.renameTo(new File(newValue.getAbsolutePath()));
        refreshBtn.fire();
    }

    public void getFilesFromServer(ArrayList<FileInfo> fileInfoList) {
        String destinationPath;
        MainBridge.setGettingFilesCount(fileInfoList.size());
        for (var fileInfo : fileInfoList) {
            if (filesTView.getSelectionModel().getSelectedItems().size() == 1) {
                var selectedFileInfo = ControlPropertiesHelper.getSelectedFilesInfo(filesTView).get(0);
                if (selectedFileInfo.isFolder()) {
                    destinationPath = selectedFileInfo.getPath().resolve(fileInfo.getName()).toString();
                } else {
                    destinationPath = selectedFileInfo.getPath().getParent().resolve(fileInfo.getName()).toString();
                }
            } else {
                destinationPath = Path.of(CLIENT_FOLDER_PATH_NAME).toAbsolutePath().resolve(fileInfo.getName()).toString();
            }
            fileInfo.setNewValue(new FileInfo().setAbsolutePath(destinationPath));
        }

        var bb = ByteBufAllocator.DEFAULT.directBuffer(1);
        bb.writeByte(ClientEnum.Request.SEND.getValue());

        MainBridge.setState(ClientEnum.State.GETTING, ClientEnum.StateWaiting.OBJECT_SIZE);
        MainBridge.sendMainHandlerByteBuf(bb, false);
        MainBridge.packAndSendObj(fileInfoList);
    }

    public void respondToRenameResult(ServerEnum.Respond result) {
        refreshBtn.fire();
    }

    @FXML
    public void downloadBtnAction(ActionEvent actionEvent) {
        var selectedItems = ControlPropertiesHelper.getSelectedFilesInfo(filesTView);
        for (var fileInfo : selectedItems) {
            MainBridge.sendFilesToServer(fileInfo);
        }
        MainBridge.refreshServerFiles();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        titleLbl.setText("Downloaded files");
        clientFolder = new Folder(new FileInfo(Path.of(CLIENT_FOLDER_PATH_NAME)), CLIENT_FOLDER_NAME);
        initButtons();
        initTreeViews(filesTView, this);
        clientFolder = refreshTView(filesTView, clientFolder);
    }

    private void initButtons() {
        setBtnIcon("refresh", refreshBtn);
        setBtnIcon("add", addBtn);
        setBtnIcon("downToServer", downloadBtn);
        setBtnIcon("delete", deleteBtn);
        setBtnIcon("move", moveBtn);
        setBtnIcon("accept", acceptBtn);
        setBtnIcon("cancel", cancelBtn);
    }

//    @FXML
//    public void tViewDragOverAction(DragEvent dragEvent) {
//
//    }
//
//    @FXML
//    public void tViewDragDroppedAction(DragEvent dragEvent) {
//
//    }
//
//    @FXML
//    public void tViewClickAction(MouseEvent mouseEvent) {
//    }

    @FXML
    public void addBtnAction(ActionEvent actionEvent) {
        var fileChooser = new FileChooser();
        fileChooser.setTitle("Выберите файл для копирования в папку загрузок");
        var chosenFiles = fileChooser.showOpenMultipleDialog(ClientApp.getStage());
        if (chosenFiles == null) return;
        var files = new ArrayList<>(chosenFiles);
        Path destinationPath;
        if (filesTView.getSelectionModel().getSelectedItems().size() == 1) {
            var selectedFileInfo = ControlPropertiesHelper.getSelectedFilesInfo(filesTView).get(0);
            if (selectedFileInfo.isFolder()) {
                destinationPath = selectedFileInfo.getPath();
            } else {
                destinationPath = selectedFileInfo.getPath().getParent();
            }
        } else {
            destinationPath = Path.of(CLIENT_FOLDER_PATH_NAME).toAbsolutePath();
        }
        try {
            for (var file : files) {
                Files.copy(file.toPath(), destinationPath.resolve(file.getName()), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            clientFolder = refreshTView(filesTView, clientFolder);
        }
    }

    @FXML
    public void cancelBtnAction(ActionEvent actionEvent) {
        filesTView.getSelectionModel().clearSelection();
    }

    @FXML
    @Override
    public void refreshBtnAction(ActionEvent actionEvent) {
        clientFolder = refreshTView(filesTView, clientFolder);
    }

    @FXML
    public void deleteBtnAction(ActionEvent actionEvent) {
        var selectedFilesInfo = ControlPropertiesHelper.getSelectedFilesInfo(filesTView);
        try {
            for (var fileInfo : selectedFilesInfo) {
                Files.delete(fileInfo.getPath());
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            clientFolder = refreshTView(filesTView, clientFolder);
        }
    }
}
