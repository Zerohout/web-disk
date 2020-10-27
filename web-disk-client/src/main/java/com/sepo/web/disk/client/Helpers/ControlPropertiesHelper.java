package com.sepo.web.disk.client.Helpers;

import com.sepo.web.disk.client.ClientApp;
import com.sepo.web.disk.client.controllers.FilesController;
import com.sepo.web.disk.client.controls.EditableTreeCell;
import com.sepo.web.disk.common.helpers.MainHelper;
import com.sepo.web.disk.common.models.FileInfo;
import com.sepo.web.disk.common.models.Folder;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Random;
import java.util.stream.Collectors;

public class ControlPropertiesHelper {
    public static final String CLIENT_FOLDER_PATH_NAME = "downloaded";
    public static final String CLIENT_FOLDER_NAME = "client_folder";

    public static void setPassControlsProp(TextField passTField, PasswordField passPField, Button passShowBtn) {
        var passTFieldIsVisible = passTField.isVisible();
        passTField.setVisible(!passTFieldIsVisible);
        passPField.setVisible(passTFieldIsVisible);
        passShowBtn.setText(passTFieldIsVisible ? "show" : "hide");
        if (passTFieldIsVisible) {
            var pass = passTField.getText();
            passPField.setText(pass);
            if (pass.isEmpty()) passShowBtn.setDisable(true);
            passTField.clear();

        } else {
            passTField.setText(passPField.getText());
        }
    }

    public static void passPFieldControlProp(PasswordField passPField, Button showPassBtn) {
        var pass = passPField.getText();
        var passIsEmptyOrBlank = pass.isEmpty() || pass.isBlank();
        if (showPassBtn.isDisable() == passIsEmptyOrBlank) return;
        showPassBtn.setDisable(passIsEmptyOrBlank);
    }

    public static void signInBtnControlProp(Button button, TextField... textFields) {
        for (var tField: textFields) {
            if(tField.getText().isBlank() || tField.getText().isEmpty()){
                if(!tField.isVisible()) continue;
                button.setDisable(true);
                return;
            }
        }
        button.setDisable(false);
    }

    public static void initTreeViews(TreeView<FileInfo> treeView, FilesController filesController) {
        treeView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        treeView.setCellFactory(fileInfoTreeView -> new EditableTreeCell(filesController));

    }

    public static Folder refreshTView(TreeView<FileInfo> treeView, Folder folder) {
        if(folder.getName().equals(CLIENT_FOLDER_NAME)){
            MainHelper.createFileTree(Path.of(CLIENT_FOLDER_PATH_NAME), folder, CLIENT_FOLDER_NAME);
        }
        treeView.getSelectionModel().clearSelection();
        treeView.refresh();
        var root = new TreeItem<FileInfo>();
        root.setExpanded(true);
        treeView.setRoot(root);
        setTViewFiles(folder, root);
        if(folder.getName().equals(CLIENT_FOLDER_NAME)){
            return new Folder(new FileInfo(Path.of(CLIENT_FOLDER_PATH_NAME)), CLIENT_FOLDER_NAME);
        }else{
            return null;
        }
    }

    public static void setTViewFiles(Folder rootFolder, TreeItem<FileInfo> root) {
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

    public static void setBtnIcon(String iconName, Button btn) {
        var icon = new ImageView(new Image(ClientApp.class.getResourceAsStream("/com/sepo/web/disk/icons/" + iconName + ".png"), 25, 25, true, true));
        btn.setGraphic(icon);
    }

    public static ArrayList<FileInfo> getSelectedFilesInfo(TreeView<FileInfo> filesTView){
        return filesTView.getSelectionModel().getSelectedItems()
                .parallelStream()
                .map(TreeItem::getValue)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public static String getDestinationPath(TreeView<FileInfo> filesTView, String defaultDestination) {
        String destinationPath;
        if (filesTView.getSelectionModel().getSelectedItems().size() == 1) {
            var selectedFileInfo = ControlPropertiesHelper.getSelectedFilesInfo(filesTView).get(0);
            if (selectedFileInfo.isFolder()) {
                destinationPath = selectedFileInfo.getAbsolutePath();
            } else {
                destinationPath = selectedFileInfo.getAbsolutePath().replace(selectedFileInfo.getName(), "");
            }
        } else {
            destinationPath = defaultDestination;
        }
        return destinationPath;
    }

    public static int getRandomFolderNumber(){
        var rnd = new Random();
        return rnd.nextInt(Integer.MAX_VALUE-1);
    }

}
