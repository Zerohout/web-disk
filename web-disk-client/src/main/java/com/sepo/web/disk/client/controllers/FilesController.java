package com.sepo.web.disk.client.controllers;

import com.sepo.web.disk.common.models.FileInfo;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class FilesController {
    private static final Logger logger = LogManager.getLogger(FilesController.class);
    protected Operation currentOperation;


    public enum Operation {
        COPYING,
        CUTTING,
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
    protected Button addFolderBtn;
    @FXML
    protected Button cutBtn;
    @FXML
    protected Button copyBtn;
    @FXML
    protected Button pasteBtn;
    @FXML
    protected Button cancelBtn;
    @FXML
    protected TreeView<FileInfo> filesTView;
    @FXML
    protected Label titleLbl;

    private final boolean isServerFilesController;

    public FilesController(boolean isServerFilesController) {
        this.isServerFilesController = isServerFilesController;
        currentOperation = Operation.IDLE;
    }

    @FXML
    public void TViewKeyReleasedAction(KeyEvent keyEvent) {
        if (keyEvent.getCode() == KeyCode.F5) {
            refreshBtn.fire();
        }
        if (keyEvent.getCode() == KeyCode.DELETE) {
            deleteBtn.fire();
        }
        if (keyEvent.getCode() == KeyCode.ESCAPE) {
            cancelBtn.fire();
        }
        if(keyEvent.isControlDown() && keyEvent.getCode() == KeyCode.C){
            copyBtn.fire();
        }
        if(keyEvent.isControlDown() && keyEvent.getCode() == KeyCode.X){
            cutBtn.fire();
        }
        if(keyEvent.isControlDown() && keyEvent.getCode() == KeyCode.V){
            pasteBtn.fire();
        }
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

    public Button getAddFolderBtn() {
        return addFolderBtn;
    }

    public Button getCopyBtn() {
        return copyBtn;
    }

    public Button getCutBtn() {
        return cutBtn;
    }

    public Button getPasteBtn() {
        return pasteBtn;
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

    public abstract void renameFile(FileInfo oldValue, FileInfo newValue);


}
