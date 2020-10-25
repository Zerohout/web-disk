package com.sepo.web.disk.client.controllers;

import com.sepo.web.disk.common.models.FileInfo;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TreeView;

import java.nio.file.Files;

import static com.sepo.web.disk.client.Helpers.ControlPropertiesHelper.refreshTView;

public abstract class FilesController {
    protected Operation currentOperation;



    public enum Operation{
        MOVING,
        IDLE
    }

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
    @FXML
    protected Label titleLbl;

    private final boolean isServerFilesController;

    public FilesController(boolean isServerFilesController) {
        this.isServerFilesController = isServerFilesController;
        currentOperation = Operation.IDLE;
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

    public Operation getCurrentOperation() {
        return currentOperation;
    }

    public void setCurrentOperation(Operation currentOperation) {
        this.currentOperation = currentOperation;
    }

    public abstract boolean renameFile(FileInfo oldValue, FileInfo newValue);
}
