package com.sepo.web.disk.client.controls;

import com.sepo.web.disk.client.controllers.ClientFilesController;
import com.sepo.web.disk.client.controllers.FilesController;
import com.sepo.web.disk.common.models.FileInfo;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeCell;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EditableTreeCell extends TreeCell<FileInfo> {
    private static final Logger logger = LogManager.getLogger(EditableTreeCell.class);
    private TextField textField;

    private FilesController filesController;

    public EditableTreeCell(FilesController filesController) {
        this.filesController = filesController;
    }

    @Override
    public void updateSelected(boolean b) {
        super.updateSelected(b);
        var selectedItemsCount = getTreeView().getSelectionModel().getSelectedItems().size();
        filesController.getDeleteBtn().setDisable(selectedItemsCount == 0);
        filesController.getCancelBtn().setDisable(selectedItemsCount == 0);
        filesController.getAddBtn().setDisable(selectedItemsCount > 1);
        filesController.getDownloadBtn().setDisable(selectedItemsCount == 0);
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
                editItem();
            } else if (keyEvent.getCode() == KeyCode.ESCAPE) {
                cancelEdit();
            }
        });
    }

    private void editItem() {
        if(filesController.isServerFilesController()) {
            cancelEdit();
            return;
        }
        var editedFileInfo = getFileInfo();
        editedFileInfo.setName(textField.getText());
        var newFile = editedFileInfo.getPath().getParent().resolve(editedFileInfo.getName()).toFile();
        if (getFileInfo().getPath().toFile().renameTo(newFile)) {
            editedFileInfo = new FileInfo(newFile.toPath());
            commitEdit(editedFileInfo);
            filesController.getRefreshBtn().fire();
        } else {
            cancelEdit();
        }
    }

    private String getFileName() {
        return getItem() == null ? "" : getItem().getName();
    }

    private ImageView getGraphics() {
        return getItem() == null ? new ImageView() : new ImageView(getItem().getIcon());
    }

    private FileInfo getFileInfo() {
        return getItem() == null ? new FileInfo() : getItem();
    }
}

