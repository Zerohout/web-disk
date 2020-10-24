package com.sepo.web.disk.client.controllers;


import javafx.fxml.Initializable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;


public class FileManagerController implements Initializable{
    private static final Logger logger = LogManager.getLogger(FileManagerController.class);
//    private ObservableList<FileInfo> clientFiles = FXCollections.observableArrayList();


    public FileManagerController() throws IOException {
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
    }
//    public void clientFilesTViewClickAction(MouseEvent mouseEvent) {
//        var selectedItemsCount = clientFilesTView.getSelectionModel().getSelectedItems().size();
//        clientAddBtn.setDisable(selectedItemsCount > 1);
//
//        if (selectedItemsCount > 0) {
//            if (mouseEvent.getClickCount() == 1) {
//                clientCancelBtn.setDisable(false);
//                clientDeleteBtn.setDisable(false);
//            }
//        } else {
//            clientCancelBtn.setDisable(true);
//            clientDeleteBtn.setDisable(true);
//        }
//    }
//
//    public void clientCancelBtnAction(ActionEvent actionEvent) {
//        ((Button) actionEvent.getSource()).setDisable(true);
//        clientFilesTView.getSelectionModel().clearSelection();
//    }
//
//    public void clientRefreshBtnAction(ActionEvent actionEvent) {
//        refreshTView(clientFilesTView, clientFolder);
//    }
//
//    public void serverFilesTViewClickAction(MouseEvent mouseEvent) {
//        var selectedItemsCount = serverFilesTView.getSelectionModel().getSelectedItems().size();
//        serverAddBtn.setDisable(selectedItemsCount < 2);
//
//        if (selectedItemsCount > 0) {
//            if (mouseEvent.getClickCount() == 1) {
//                serverCancelBtn.setDisable(false);
//            }
//
//        }
//    }
//
//    public void serverCancelBtnAction(ActionEvent actionEvent) {
//        ((Button) actionEvent.getSource()).setDisable(true);
//        serverFilesTView.getSelectionModel().clearSelection();
//    }
//
//    public void serverRefreshBtnAction(ActionEvent actionEvent) {
//        mainCallback.callback(ClientEnum.State.REFRESHING, ClientEnum.StateWaiting.TRANSFER);
//        mainCallback.callback(ClientEnum.Request.REFRESH);
//    }
//
//    public void serverAddBtnAction(ActionEvent actionEvent) {
//        var filesInfo = new ArrayList<FileInfo>();
//        var fileChooser = new FileChooser();
//        fileChooser.setTitle("Выберите файл для отправки на сервер");
//        var files = new ArrayList<>(fileChooser.showOpenMultipleDialog(ClientApp.getStage()));
//        for (var file : files) {
//            filesInfo.add(new FileInfo(file.toPath()));
//        }
//        mainCallback.callback(filesInfo, files);
//    }

//    private void printAllFiles(ArrayList<File> files){
//        for(var file : files){
//            if(Files.isDirectory(file.toPath())){
//                try {
//                    Files.walkFileTree(file.toPath(), new FileVisitor<>() {
//                        @Override
//                        public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
//                            logger.info("preVisitDirectory " + path.toAbsolutePath());
//                            return FileVisitResult.CONTINUE;
//                        }
//
//                        @Override
//                        public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
//                            logger.info("visitFile " + path.toAbsolutePath());
//                            return FileVisitResult.CONTINUE;
//                        }
//
//                        @Override
//                        public FileVisitResult visitFileFailed(Path path, IOException e) throws IOException {
//                            logger.info("visitFileFailed " + path.toAbsolutePath());
//                            return FileVisitResult.CONTINUE;
//                        }
//
//                        @Override
//                        public FileVisitResult postVisitDirectory(Path path, IOException e) throws IOException {
//                            logger.info("postVisitDirectory " + path.toAbsolutePath());
//                            return FileVisitResult.CONTINUE;
//                        }
//                    });
//                } catch (IOException e) {
//                    e.printStackTrace();
//                } {
//
//                }
//            }
//        }
//    }
}
