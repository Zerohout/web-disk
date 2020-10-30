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

    private final FilesController filesController;

    public EditableTreeCell(FilesController filesController) {
        this.filesController = filesController;

        setOnMouseEntered(event -> {
            onMouseItem = (EditableTreeCell) event.getTarget();
            onMouseItem.getStyleClass().add("selected-tree-cell");
            if (onMouseItem.getFileName().equals("")) return;
            FileInfo fileInfo = onMouseItem.getFileInfo();
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
        int selectedItemsCount = getTreeView().getSelectionModel().getSelectedItems().size();
        filesController.getDeleteBtn().setDisable(selectedItemsCount == 0);
        filesController.getCancelBtn().setDisable(selectedItemsCount == 0);
        filesController.getDownloadBtn().setDisable(selectedItemsCount == 0);
        filesController.getCopyBtn().setDisable(filesController.getCurrentOperation() != FilesController.Operation.IDLE ||
                selectedItemsCount == 0);
        filesController.getCutBtn().setDisable(filesController.getCurrentOperation() != FilesController.Operation.IDLE ||
                selectedItemsCount == 0);
        filesController.getPasteBtn().setDisable(filesController.getCurrentOperation() != FilesController.Operation.COPYING &&
                filesController.getCurrentOperation() != FilesController.Operation.CUTTING);
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
        if (getItem() != null) {
            this.setContextMenu(createContextMenu());
        } else {
            this.setContextMenu(createNullItemContextMenu());
        }
    }

    @Override
    public void startEdit() {
        super.startEdit();
        filesController.setCurrentOperation(FilesController.Operation.EDITING);
        if (textField == null) {
            createTextField();
        }
        setText(null);
        setGraphic(textField);
        int extSize = FilenameUtils.getExtension(textField.getText()).length() + 1;
        textField.requestFocus();
        textField.selectRange(0, textField.getText().length() - extSize);
    }

    @Override
    public void cancelEdit() {
        super.cancelEdit();
        setText(getFileName());
        setGraphic(getGraphics());
        textField = null;
        filesController.setCurrentOperation(FilesController.Operation.IDLE);
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
        FileInfo editedFileInfo = new FileInfo();
        editedFileInfo.setName(textField.getText());
        String oldPath = getFileInfo().getAbsolutePath();
        oldPath = oldPath.replace(getFileInfo().getName(), "") + textField.getText();
        editedFileInfo.setAbsolutePath(oldPath);

        filesController.renameFile(getFileInfo(), editedFileInfo);
        commitEdit(editedFileInfo);
        filesController.setCurrentOperation(FilesController.Operation.IDLE);

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

    private ContextMenu createContextMenu() {
        ContextMenu cm = new ContextMenu();

        MenuItem refresh = new MenuItem("Refresh");
        refresh.setOnAction(actionEvent -> filesController.getRefreshBtn().fire());
        cm.getItems().add(refresh);
        MenuItem add = new MenuItem("Add");
        add.setOnAction(actionEvent -> filesController.getAddBtn().fire());
        if (!filesController.getAddBtn().isDisable()) {
            cm.getItems().add(add);
        }
        MenuItem addFolder = new MenuItem("Add folder");
        addFolder.setOnAction(actionEvent -> filesController.getAddFolderBtn().fire());
        if (!filesController.getAddFolderBtn().isDisable()) {
            cm.getItems().add(addFolder);
        }
        MenuItem download = new MenuItem(filesController.isServerFilesController()
                ? "Download" : "Upload");
        download.setOnAction(actionEvent -> filesController.getDownloadBtn().fire());
        if (!filesController.getDownloadBtn().isDisable()) {
            cm.getItems().add(download);
        }
        MenuItem copy = new MenuItem("Copy");
        copy.setOnAction(actionEvent -> filesController.getCopyBtn().fire());
        if (!filesController.getCopyBtn().isDisable()) {
            cm.getItems().add(copy);
        }
        MenuItem cut = new MenuItem("Cut");
        cut.setOnAction(actionEvent -> filesController.getCutBtn().fire());
        if (!filesController.getCutBtn().isDisable()) {
            cm.getItems().add(cut);
        }
        MenuItem paste = new MenuItem("Paste");
        paste.setOnAction(actionEvent -> filesController.getPasteBtn().fire());
        if (!filesController.getPasteBtn().isDisable()) {
            cm.getItems().add(paste);
        }
        MenuItem delete = new MenuItem("Delete");
        delete.setOnAction(actionEvent -> filesController.getDeleteBtn().fire());
        if (!filesController.getDeleteBtn().isDisable()) {
            cm.getItems().add(delete);
        }
        MenuItem cancel = new MenuItem("Cancel");
        cancel.setOnAction(actionEvent -> filesController.getCancelBtn().fire());
        if (!filesController.getCancelBtn().isDisable()) {
            cm.getItems().add(cancel);
        }
        return cm;
    }

    private ContextMenu createNullItemContextMenu() {
        ContextMenu cm = new ContextMenu();
        MenuItem refresh = new MenuItem("Refresh");
        refresh.setOnAction(actionEvent -> filesController.getRefreshBtn().fire());
        cm.getItems().add(refresh);
        MenuItem add = new MenuItem("Add");
        add.setOnAction(actionEvent -> filesController.getAddBtn().fire());
        if (!filesController.getAddBtn().isDisable()) {
            cm.getItems().add(add);
        }
        MenuItem addFolder = new MenuItem("Add folder");
        addFolder.setOnAction(actionEvent -> filesController.getAddFolderBtn().fire());
        if (!filesController.getAddFolderBtn().isDisable()) {
            cm.getItems().add(addFolder);
        }
        return cm;
    }
}

