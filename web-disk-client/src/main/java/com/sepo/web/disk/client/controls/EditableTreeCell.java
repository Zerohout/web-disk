package com.sepo.web.disk.client.controls;

import com.sepo.web.disk.client.controllers.FilesController;
import com.sepo.web.disk.common.models.FileInfo;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeCell;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;

public class EditableTreeCell extends TreeCell<FileInfo> {
    private static final Logger logger = LogManager.getLogger(EditableTreeCell.class);
    private TextField textField;
    EditableTreeCell onMouseItem;
    Path destinationPath;


    private FilesController filesController;

    public EditableTreeCell(FilesController filesController) {
        this.filesController = filesController;

        setOnMouseEntered(event -> {
            onMouseItem = (EditableTreeCell) event.getTarget();
            onMouseItem.getStyleClass().add("selected-tree-cell");
            if (onMouseItem.getFileName().equals("")) return;
            var fileInfo = onMouseItem.getFileInfo();
            if (fileInfo.isFolder()) {
                destinationPath = Path.of(fileInfo.getAbsolutePath());
            } else {
                destinationPath = Path.of(fileInfo.getAbsolutePath()).getParent();
            }

        });

        setOnMouseExited(event -> {
            onMouseItem = (EditableTreeCell) event.getTarget();
            onMouseItem.getStyleClass().remove("selected-tree-cell");
        });

    }





    @Override
    public void updateSelected(boolean b) {
        super.updateSelected(b);
        if (filesController.getCurrentOperation() != FilesController.Operation.IDLE) return;

        var selectedItemsCount = getTreeView().getSelectionModel().getSelectedItems().size();
        filesController.getDeleteBtn().setDisable(selectedItemsCount == 0);
        filesController.getCancelBtn().setDisable(selectedItemsCount == 0);
        filesController.getDownloadBtn().setDisable(selectedItemsCount == 0);
        if(getItem() != null) {
            this.setContextMenu(createContextMenu(this));
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
                editItem();
            } else if (keyEvent.getCode() == KeyCode.ESCAPE) {
                cancelEdit();
            }
        });
    }

    private void editItem() {
        var editedFileInfo = new FileInfo();
        editedFileInfo.setName(textField.getText());
        var oldPath = getFileInfo().getAbsolutePath();
        oldPath = oldPath.replace(getFileInfo().getName(), "") + textField.getText();
        editedFileInfo.setAbsolutePath(oldPath);

        filesController.renameFile(getFileInfo(), editedFileInfo);
        commitEdit(editedFileInfo);
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

    private ContextMenu createContextMenu(TreeCell<FileInfo> treeCell){
        ContextMenu cm = new ContextMenu();
        var refresh = new MenuItem("Refresh");
        refresh.setOnAction(actionEvent -> filesController.getRefreshBtn().fire());
        var download = new MenuItem(filesController.isServerFilesController()
                ? "Download" : "Upload");
        download.setOnAction(actionEvent -> filesController.getDownloadBtn().fire());
        var delete = new MenuItem("Delete");
        delete.setOnAction(actionEvent -> filesController.getDeleteBtn().fire());
        cm.getItems().addAll(refresh, download, delete);
        return cm;
    }
}

