package com.sepo.web.disk.client.controllers;

import com.sepo.web.disk.client.ClientApp;
import com.sepo.web.disk.client.Helpers.MainHelper;
import com.sepo.web.disk.common.models.ClientEnum;
import com.sepo.web.disk.common.models.FileInfo;
import com.sepo.web.disk.common.models.Folder;
import com.sepo.web.disk.common.models.ServerEnum;
import com.sepo.web.disk.common.service.ObjectEncoderDecoder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.DefaultFileRegion;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.stage.FileChooser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import static com.sepo.web.disk.client.Helpers.ControlPropertiesHelper.*;

public class ServerFilesController extends FilesController implements Initializable {
    private static final Logger logger = LogManager.getLogger(ServerFilesController.class);

    private Folder serverFolder;

    public ServerFilesController() {
        super(true);
        MainHelper.setServerFilesController(this);
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        titleLbl.setText("Server files");
        initButtons();
        initTreeViews(filesTView, this);
        sendRefreshRequest();
    }

    private void initButtons() {
        setBtnIcon("refresh", refreshBtn);
        setBtnIcon("add", addBtn);
        setBtnIcon("downFromServer", downloadBtn);
        setBtnIcon("delete", deleteBtn);
        setBtnIcon("move", moveBtn);
        setBtnIcon("accept", acceptBtn);
        setBtnIcon("cancel", cancelBtn);
    }

    private void sendRefreshRequest() {
        logger.info("send REFRESH request");
        MainHelper.setState(ClientEnum.State.REFRESHING, ClientEnum.StateWaiting.TRANSFER);
        MainHelper.sendMainHandlerRequest(ClientEnum.Request.REFRESH, null);
    }

//    @FXML
//    public void tViewDragOverAction(DragEvent dragEvent) {
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
        if (chosenFiles == null) return;
        var filesList = new ArrayList<>(chosenFiles);
        for (var file : filesList) {
            sendFile(file);
        }
        refreshBtn.fire();
    }

    private void sendFile(File file) {
        ByteBuf bb = ByteBufAllocator.DEFAULT.directBuffer(1);
        MainHelper.sendByteBuf(bb.writeByte(ClientEnum.Request.GET.getValue()), false);
        packAndSendObj(new FileInfo(file.toPath()));
        try {
            var region = new DefaultFileRegion(file, 0, Files.size(file.toPath()));
            MainHelper.sendByteBuf(region, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void packAndSendObj(Object object) {
        var fileInfoBB = ObjectEncoderDecoder.EncodeObjToByteBuf(object);
        var fileInfoSize = fileInfoBB.readableBytes();
        var sizeBB = ByteBufAllocator.DEFAULT.directBuffer(4);
        sizeBB.writeInt(fileInfoSize);
        MainHelper.sendByteBuf(sizeBB, false);
        MainHelper.sendByteBuf(fileInfoBB, true);
    }

    @FXML
    public void deleteBtnAction(ActionEvent actionEvent) {
        MainHelper.setState(ClientEnum.State.IDLE, ClientEnum.StateWaiting.RESULT);
        var req = ByteBufAllocator.DEFAULT.directBuffer(2);
        req.writeByte(ClientEnum.Request.OPERATION.getValue());
        req.writeByte(ClientEnum.RequestType.DELETE.getValue());
        MainHelper.sendByteBuf(req, false);
        var selectedFilesInfo = filesTView.getSelectionModel().getSelectedItems()
                .parallelStream()
                .map(TreeItem::getValue)
                .collect(Collectors.toCollection(ArrayList::new));
        packAndSendObj(selectedFilesInfo);
        refreshBtn.fire();
    }

    @FXML
    public void cancelBtnAction(ActionEvent actionEvent) {
        filesTView.getSelectionModel().clearSelection();
    }

    @FXML
    @Override
    public void refreshBtnAction(ActionEvent actionEvent) {
        sendRefreshRequest();
    }


    public TreeView<FileInfo> getFilesTView() {
        return filesTView;
    }

    @Override
    public void renameFile(FileInfo oldValue, FileInfo newValue) {
        logger.info("renaming. oldValue - "+oldValue.getAbsolutePath()+", new value - "+newValue.getAbsolutePath());

        oldValue.setNewValue(newValue);
        var req = ByteBufAllocator.DEFAULT.directBuffer(2);
        req.writeByte(ClientEnum.Request.OPERATION.getValue());
        req.writeByte(ClientEnum.RequestType.RENAME.getValue());
        MainHelper.setState(ClientEnum.State.IDLE, ClientEnum.StateWaiting.RESULT);
        MainHelper.sendByteBuf(req, false);
        packAndSendObj(oldValue);
    }
}
