package com.sepo.web.disk.client.controllers;

import com.sepo.web.disk.client.ClientApp;
import com.sepo.web.disk.client.Helpers.ControlPropertiesHelper;
import com.sepo.web.disk.client.Helpers.MainBridge;
import com.sepo.web.disk.common.models.ClientEnum;
import com.sepo.web.disk.common.models.FileInfo;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.DefaultFileRegion;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeView;
import javafx.stage.FileChooser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import static com.sepo.web.disk.client.Helpers.ControlPropertiesHelper.*;
import static com.sepo.web.disk.common.helpers.MainHelper.SERVER_FOLDER_NAME;

public class ServerFilesController extends FilesController implements Initializable {
    private static final Logger logger = LogManager.getLogger(ServerFilesController.class);

    public ServerFilesController() {
        super(true);
        MainBridge.setServerFilesController(this);
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        titleLbl.setText("Server files");
        initButtons();
        initTreeViews(filesTView, this);
        sendRefreshRequest();
        Tooltip downTT = new Tooltip();
        downTT.setText("Download selected files from server");
        downloadBtn.setTooltip(downTT);
    }

    private void initButtons() {
        setBtnIcon("refresh", refreshBtn);
        setBtnIcon("add", addBtn);
        setBtnIcon("downFromServer", downloadBtn);
        setBtnIcon("addFolder", addFolderBtn);
        setBtnIcon("copy", copyBtn);
        setBtnIcon("cut", cutBtn);
        setBtnIcon("paste", pasteBtn);
        setBtnIcon("delete", deleteBtn);
        setBtnIcon("cancel", cancelBtn);
    }

    @FXML
    public void addBtnAction(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Выберите файл для копирования в папку загрузок");
        List<File> chosenFiles = fileChooser.showOpenMultipleDialog(ClientApp.getStage());
        if (chosenFiles == null) return;
        ArrayList<File> filesList = new ArrayList<>(chosenFiles);
        ArrayList<FileInfo> fileInfoList = new ArrayList<FileInfo>();
        filesList.forEach(file -> fileInfoList.add(new FileInfo(file.toPath())));
        uploadFiles(fileInfoList);
        refreshBtn.fire();
    }

    @FXML
    public void refreshBtnAction(ActionEvent actionEvent) {
        if (refreshBtn.isDisable()) return;
        sendRefreshRequest();
    }

    @FXML
    public void downloadBtnAction(ActionEvent actionEvent) {
        MainBridge.downloadFiles(ControlPropertiesHelper.getSelectedFilesInfo(filesTView));
    }

    @FXML
    public void deleteBtnAction(ActionEvent actionEvent) {
        if (deleteBtn.isDisable()) return;
        MainBridge.setState(ClientEnum.State.IDLE, ClientEnum.StateWaiting.RESULT);
        ByteBuf req = ByteBufAllocator.DEFAULT.directBuffer(2);
        req.writeByte(ClientEnum.Request.OPERATION.getValue());
        req.writeByte(ClientEnum.RequestType.DELETE.getValue());
        MainBridge.sendMainHandlerByteBuf(req, false);
        MainBridge.mainPackAndSendObj(ControlPropertiesHelper.getSelectedFilesInfo(filesTView));
        refreshBtn.fire();
    }

    @FXML
    public void addFolderBtnAction(ActionEvent actionEvent) {
        if (addFolderBtn.isDisable()) return;
        ByteBuf req = ByteBufAllocator.DEFAULT.directBuffer(2);
        req.writeByte(ClientEnum.Request.OPERATION.getValue());
        req.writeByte(ClientEnum.RequestType.CREATE.getValue());
        MainBridge.sendMainHandlerByteBuf(req, false);

        FileInfo fileInfo = new FileInfo().setName("New_Folder_" + getRandomFolderNumber());
        String destinationPath = getDestinationPath(filesTView, SERVER_FOLDER_NAME) + "\\" + fileInfo.getName();
        fileInfo.setNewValue(new FileInfo().setAbsolutePath(destinationPath));
        MainBridge.mainPackAndSendObj(fileInfo);
        refreshBtn.fire();
    }

    @FXML
    public void copyBtnAction(ActionEvent actionEvent) {
        currentOperation = Operation.COPYING;
        copyingOrCuttingFileInfoList.addAll(ControlPropertiesHelper.getSelectedFilesInfo(filesTView));
        filesTView.getSelectionModel().clearSelection();
    }

    @FXML
    public void cutBtnAction(ActionEvent actionEvent) {
        currentOperation = Operation.CUTTING;
        copyingOrCuttingFileInfoList.addAll(ControlPropertiesHelper.getSelectedFilesInfo(filesTView));
        filesTView.getSelectionModel().clearSelection();
    }

    @FXML
    public void pasteBtnAction(ActionEvent actionEvent) {
        ByteBuf req;
        req = ByteBufAllocator.DEFAULT.directBuffer(2);
        req.writeByte(ClientEnum.Request.OPERATION.getValue());
        if (currentOperation == Operation.COPYING) {
            req.writeByte(ClientEnum.RequestType.COPY.getValue());
        }
        if(currentOperation == Operation.CUTTING){
            req.writeByte(ClientEnum.RequestType.CUT.getValue());
        }
        for (FileInfo fileInfo : copyingOrCuttingFileInfoList) {
            String destinationPath = getDestinationPath(filesTView, SERVER_FOLDER_NAME) + "\\";
            fileInfo.setNewValue(new FileInfo().setAbsolutePath(destinationPath));
        }
        MainBridge.sendMainHandlerByteBuf(req, false);
        MainBridge.mainPackAndSendObj(copyingOrCuttingFileInfoList);
        MainBridge.setState(ClientEnum.State.REFRESHING, ClientEnum.StateWaiting.TRANSFER);
        copyingOrCuttingFileInfoList.clear();
        currentOperation = Operation.IDLE;
        if (!pasteBtn.isDisable()) pasteBtn.setDisable(true);
    }

    @FXML
    public void cancelBtnAction(ActionEvent actionEvent) {
        if (cancelBtn.isDisable()) return;
        filesTView.getSelectionModel().clearSelection();
        copyingOrCuttingFileInfoList.clear();
        if(currentOperation != Operation.IDLE) currentOperation = Operation.IDLE;
        if(!pasteBtn.isDisable()) pasteBtn.setDisable(true);
    }

    @Override
    public void renameFile(FileInfo oldValue, FileInfo newValue) {
        oldValue.setNewValue(newValue);
        ByteBuf req = ByteBufAllocator.DEFAULT.directBuffer(2);
        req.writeByte(ClientEnum.Request.OPERATION.getValue());
        req.writeByte(ClientEnum.RequestType.RENAME.getValue());
        MainBridge.setState(ClientEnum.State.IDLE, ClientEnum.StateWaiting.RESULT);
        MainBridge.sendMainHandlerByteBuf(req, false);
        MainBridge.mainPackAndSendObj(oldValue);
    }

    private void sendRefreshRequest() {
        MainBridge.setState(ClientEnum.State.REFRESHING, ClientEnum.StateWaiting.TRANSFER);
        MainBridge.sendMainHandlerRequest(ClientEnum.Request.REFRESH, null);
    }

    public void uploadFiles(ArrayList<FileInfo> fileInfoList) {
        for (FileInfo fileInfo : fileInfoList) {
            ByteBuf bb = ByteBufAllocator.DEFAULT.directBuffer(1);
            MainBridge.sendMainHandlerByteBuf(bb.writeByte(ClientEnum.Request.GET.getValue()), false);
            String destinationPath = getDestinationPath(filesTView, SERVER_FOLDER_NAME) + "\\" + fileInfo.getName();
            fileInfo.setNewValue(new FileInfo().setAbsolutePath(destinationPath));
            MainBridge.mainPackAndSendObj(fileInfo);
            File file = fileInfo.getPath().toFile();
            try {
                DefaultFileRegion region = new DefaultFileRegion(file, 0, Files.size(file.toPath()));
                MainBridge.sendMainHandlerByteBuf(region, true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public TreeView<FileInfo> getFilesTView() {
        return filesTView;
    }
}
