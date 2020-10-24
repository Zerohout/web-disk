package com.sepo.web.disk.client.controllers;

import com.sepo.web.disk.client.ClientApp;
import com.sepo.web.disk.client.Helpers.MainHelper;
import com.sepo.web.disk.common.models.ClientEnum;
import com.sepo.web.disk.common.models.FileInfo;
import com.sepo.web.disk.common.models.FilesTransferInfo;
import com.sepo.web.disk.common.models.Folder;
import com.sepo.web.disk.common.service.ObjectEncoderDecoder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.DefaultFileRegion;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
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
            ByteBuf bb = ByteBufAllocator.DEFAULT.directBuffer(1);
            MainHelper.sendByteBuf(bb.writeByte(ClientEnum.Request.GET.getValue()), false);
            bb = ByteBufAllocator.DEFAULT.directBuffer(4);
            var fileInfoBB = ObjectEncoderDecoder.EncodeObjToByteBuf(new FileInfo(file.toPath()));
            var fileInfoSize = fileInfoBB.readableBytes();
            MainHelper.sendByteBuf(bb.writeInt(fileInfoSize), false);
            MainHelper.sendByteBuf(fileInfoBB, false);
            try {
                var region = new DefaultFileRegion(file, 0, Files.size(file.toPath()));
                MainHelper.sendByteBuf(region, true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        refreshBtn.fire();
    }

    private ArrayList<FileInfo> prepareFilesInfo(ArrayList<File> files) {
        var out = new ArrayList<FileInfo>();
        for (var file : files) {
            out.add(new FileInfo(file.toPath()));
        }
        return out;
    }

    private ArrayList<Integer> prepareBytesCountFilesInfoList(ArrayList<FileInfo> filesInfo) {
        var out = new ArrayList<Integer>();
        for (var fileInfo : filesInfo) {
            out.add(ObjectEncoderDecoder.getObjectBytesCount(fileInfo));
        }
        return out;
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

    @FXML
    public void deleteBtnAction(ActionEvent actionEvent) {
    }

    public TreeView<FileInfo> getFilesTView() {
        return filesTView;
    }
}
