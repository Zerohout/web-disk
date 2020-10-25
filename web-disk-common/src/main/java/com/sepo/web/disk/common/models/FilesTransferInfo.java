package com.sepo.web.disk.common.models;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;

public class FilesTransferInfo implements Sendable{
    private static final long serialVersionUID = 462304765383208161L;

    ArrayList<FileInfo> filesInfoList = new ArrayList<>();
    ArrayList<File> filesList = new ArrayList<>();

    private Path destinationPath;
    private FileInfo newValue;

    public ArrayList<FileInfo> getFilesInfoList() {
        return filesInfoList;
    }

    public FilesTransferInfo setFilesInfoList(ArrayList<FileInfo> filesInfo) {
        this.filesInfoList = filesInfo;
        return this;
    }

    public ArrayList<File> getFilesList() {
        return filesList;
    }

    public FilesTransferInfo setFilesList(ArrayList<File> files) {
        this.filesList = files;
        return this;
    }

    public Path getDestinationPath() {
        return destinationPath;
    }

    public FilesTransferInfo setDestinationPath(Path destinationPath) {
        this.destinationPath = destinationPath;
        return this;
    }

    public FileInfo getNewValue() {
        return newValue;
    }

    public FilesTransferInfo setNewValue(FileInfo newValue) {
        this.newValue = newValue;
        return this;
    }
}

