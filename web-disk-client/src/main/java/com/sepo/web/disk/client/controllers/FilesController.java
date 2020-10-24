package com.sepo.web.disk.client.controllers;

import com.sepo.web.disk.common.models.FileInfo;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TreeView;

import static com.sepo.web.disk.client.Helpers.ControlPropertiesHelper.refreshTView;

public abstract class FilesController {
    @FXML
    protected Button refreshBtn;
    @FXML
    protected Button addBtn;
    @FXML
    protected Button downloadBtn;
    @FXML
    protected Button deleteBtn;
    @FXML
    protected Button moveBtn;
    @FXML
    protected Button acceptBtn;
    @FXML
    protected Button cancelBtn;
    @FXML
    protected TreeView<FileInfo> filesTView;
    @FXML
    public abstract void refreshBtnAction(ActionEvent actionEvent);

    private boolean isServerFilesController;

    public FilesController(boolean isServerFilesController) {
        this.isServerFilesController = isServerFilesController;
    }

    public Button getRefreshBtn() {
        return refreshBtn;
    }

    public Button getAddBtn() {
        return addBtn;
    }

    public Button getDownloadBtn() {
        return downloadBtn;
    }

    public Button getDeleteBtn() {
        return deleteBtn;
    }

    public Button getMoveBtn() {
        return moveBtn;
    }

    public Button getAcceptBtn() {
        return acceptBtn;
    }

    public Button getCancelBtn() {
        return cancelBtn;
    }

    public TreeView<FileInfo> getFilesTView() {
        return filesTView;
    }

    public boolean isServerFilesController() {
        return isServerFilesController;
    }
}