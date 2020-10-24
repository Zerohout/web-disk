package com.sepo.web.disk.client.controllers;

import com.sepo.web.disk.client.ClientApp;
import com.sepo.web.disk.client.Helpers.MainHelper;
import com.sepo.web.disk.common.models.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.stage.FileChooser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

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
        MainHelper.setClientFilesController(this);

    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
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
        if(chosenFiles == null) return;
        var files = new ArrayList<>(chosenFiles);
        Path destinationPath;
        if (filesTView.getSelectionModel().getSelectedItems().size() == 0) {
            destinationPath = Path.of(CLIENT_FOLDER_PATH_NAME).toAbsolutePath();
        } else {
            var selectedFileInfo = filesTView.getSelectionModel().getSelectedItems().get(0).getValue();
            if (selectedFileInfo.isFolder()) {
                destinationPath = selectedFileInfo.getPath();
            } else {
                destinationPath = selectedFileInfo.getPath().getParent();
            }
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
        var selectedFilesInfo = filesTView.getSelectionModel().getSelectedItems()
                .parallelStream()
                .map(TreeItem::getValue)
                .collect(Collectors.toCollection(ArrayList::new));
        try {
            for (var fileInfo : selectedFilesInfo) {
                logger.info("deleting "+fileInfo.getAbsolutePath());
                Files.delete(fileInfo.getPath());
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            clientFolder = refreshTView(filesTView, clientFolder);
        }
    }
}
