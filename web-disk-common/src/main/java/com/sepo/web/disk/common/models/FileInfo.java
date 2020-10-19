package com.sepo.web.disk.common.models;

import com.sepo.web.disk.common.helpers.FileInfoHelper;
import javafx.scene.image.Image;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileInfo implements Sendable {
    private static final long serialVersionUID = 2L;
    private String fileFullName;
    private String filePath;
    private long fileSize;
    private boolean isFolder;
    private transient Image icon;
    private transient Path path;

    public FileInfo(Path path) {
        try {
            var file = new File(path.toUri());
            this.fileFullName = file.getName();
            this.filePath = file.getAbsolutePath();
            this.fileSize = Files.size(path);
            isFolder = Files.isDirectory(path);
            setIcon(path);
            this.path = path;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public FileInfo() {

    }

    public FileInfo setFileFullName(String fileFullName) {
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

    public FileInfo isFolder(boolean isFolder) {
        this.isFolder = isFolder;
        return this;
    }

    public FileInfo setIcon(Image icon) {
        this.icon = icon;
        return this;
    }

    public FileInfo setIcon(Path path) {
        this.icon = FileInfoHelper.setFileInfoIcon(path);
        return this;
    }

    public String getFilePath() {
        return this.filePath;
    }

    public long getFileSize() {
        return this.fileSize;
    }

    public Path getPath() {
        return this.path;
    }

    public String getFileFullName() {
        return this.fileFullName;
    }

    public boolean isFolder() {
        return this.isFolder;
    }


    public Image getIcon() {
        if (icon == null) {
            setIcon(this.path);
        }
        return this.icon;
    }

    @Override
    public String toString() {
        return this.fileFullName;
    }


}
