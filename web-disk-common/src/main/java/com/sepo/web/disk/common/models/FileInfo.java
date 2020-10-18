package com.sepo.web.disk.common.models;

import com.sepo.web.disk.common.helpers.FileInfoHelper;
import javafx.scene.image.Image;

import java.nio.file.Path;

public class FileInfo implements Sendable {
    private String fileFullName;
    private String filePath;
    private long fileSize;
    private boolean isFolder;
    private Image icon;

    public FileInfo setFileFullName(String fileFullName){
        this.fileFullName = fileFullName;
        return this;
    }

    public FileInfo setFilePath(String filePath) {
        this.filePath = filePath;
        return this;
    }

    public FileInfo setFileSize(long fileSize) {
        this.fileSize = fileSize;
        return this;
    }

    public FileInfo isFolder(boolean isFolder){
        this.isFolder = isFolder;
        return this;
    }

    public FileInfo setIcon(Image icon){
        this.icon = icon;
        return this;
    }

    public FileInfo setIcon(Path path){
        this.icon = FileInfoHelper.setFileInfoIcon(path);
        return this;
    }

    public String getFilePath() {
        return this.filePath;
    }

    public long getFileSize() {
        return this.fileSize;
    }

    public String getFileFullName(){
        return this.fileFullName;
    }

    public boolean isFolder(){
        return this.isFolder;
    }

    public Image getIcon(){
        return this.icon;
    }

    @Override
    public String toString(){
        return this.fileFullName;
    }


}
