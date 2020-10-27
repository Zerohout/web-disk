package com.sepo.web.disk.client.controllers;

import com.sepo.web.disk.client.ClientApp;
import com.sepo.web.disk.client.Helpers.ControlPropertiesHelper;
import com.sepo.web.disk.client.Helpers.MainBridge;
import com.sepo.web.disk.common.models.ClientEnum;
import com.sepo.web.disk.common.models.FileInfo;
import com.sepo.web.disk.common.service.ObjectEncoderDecoder;
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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
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
        var downTT = new Tooltip();
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
        var fileChooser = new FileChooser();
        fileChooser.setTitle("Выберите файл для копирования в папку загрузок");
        var chosenFiles = fileChooser.showOpenMultipleDialog(ClientApp.getStage());
        if (chosenFiles == null) return;
        var filesList = new ArrayList<>(chosenFiles);
        var fileInfoList = new ArrayList<FileInfo>();
        filesList.forEach(file -> fileInfoList.add(new FileInfo(file.toPath())));
        uploadFiles(fileInfoList);
        refreshBtn.fire();
    }

    @FXML
    public void refreshBtnAction(ActionEvent actionEvent) {
        if(refreshBtn.isDisable()) return;
        sendRefreshRequest();
    }

    @FXML
    public void downloadBtnAction(ActionEvent actionEvent) {
        MainBridge.downloadFiles(ControlPropertiesHelper.getSelectedFilesInfo(filesTView));
    }

    @FXML
    public void deleteBtnAction(ActionEvent actionEvent) {
        if(deleteBtn.isDisable()) return;
        MainBridge.setState(ClientEnum.State.IDLE, ClientEnum.StateWaiting.RESULT);
        var req = ByteBufAllocator.DEFAULT.directBuffer(2);
        req.writeByte(ClientEnum.Request.OPERATION.getValue());
        req.writeByte(ClientEnum.RequestType.DELETE.getValue());
        MainBridge.sendMainHandlerByteBuf(req, false);
        MainBridge.mainPackAndSendObj(ControlPropertiesHelper.getSelectedFilesInfo(filesTView));
        refreshBtn.fire();
    }

    @FXML
    public void addFolderBtnAction(ActionEvent actionEvent){
        if(addFolderBtn.isDisable()) return;
        var req = ByteBufAllocator.DEFAULT.directBuffer(2);
        req.writeByte(ClientEnum.Request.OPERATION.getValue());
        req.writeByte(ClientEnum.RequestType.CREATE.getValue());
        MainBridge.sendMainHandlerByteBuf(req, false);

        var fileInfo = new FileInfo();
        fileInfo.setName("New_Folder_"+getRandomFolderNumber());
        var destinationPath = getDestinationPath(filesTView, SERVER_FOLDER_NAME) + "\\" + fileInfo.getName();
        fileInfo.setNewValue(new FileInfo().setAbsolutePath(destinationPath));
        MainBridge.mainPackAndSendObj(fileInfo);
        refreshBtn.fire();
    }

    @FXML
    public void copyBtnAction(ActionEvent actionEvent){

    }

    @FXML
    public void cutBtnAction(ActionEvent actionEvent){

    }

    @FXML
    public void pasteBtnAction(ActionEvent actionEvent){

    }

    @FXML
    public void cancelBtnAction(ActionEvent actionEvent) {
        if(cancelBtn.isDisable()) return;
        filesTView.getSelectionModel().clearSelection();
    }

    @Override
    public void renameFile(FileInfo oldValue, FileInfo newValue) {
        oldValue.setNewValue(newValue);
        var req = ByteBufAllocator.DEFAULT.directBuffer(2);
        req.writeByte(ClientEnum.Request.OPERATION.getValue());
        req.writeByte(ClientEnum.RequestType.RENAME.getValue());
        MainBridge.setState(ClientEnum.State.IDLE, ClientEnum.StateWaiting.RESULT);
        MainBridge.sendMainHandlerByteBuf(req, false);
        MainBridge.mainPackAndSendObj(oldValue);
    }

    private void sendRefreshRequest() {
        logger.info("send REFRESH request");
        MainBridge.setState(ClientEnum.State.REFRESHING, ClientEnum.StateWaiting.TRANSFER);
        MainBridge.sendMainHandlerRequest(ClientEnum.Request.REFRESH, null);
    }

    public void uploadFiles(ArrayList<FileInfo> fileInfoList) {
        for (var fileInfo : fileInfoList) {
            ByteBuf bb = ByteBufAllocator.DEFAULT.directBuffer(1);
            MainBridge.sendMainHandlerByteBuf(bb.writeByte(ClientEnum.Request.GET.getValue()), false);
            var destinationPath = getDestinationPath(filesTView, SERVER_FOLDER_NAME) + "\\" + fileInfo.getName();
            fileInfo.setNewValue(new FileInfo().setAbsolutePath(destinationPath));
            MainBridge.mainPackAndSendObj(fileInfo);
            var file = fileInfo.getPath().toFile();
            try {
                var region = new DefaultFileRegion(file, 0, Files.size(file.toPath()));
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
