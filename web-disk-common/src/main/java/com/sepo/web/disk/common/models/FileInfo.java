package com.sepo.web.disk.common.models;

import com.sepo.web.disk.common.helpers.FileInfoHelper;
import javafx.scene.image.Image;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileInfo implements Sendable {
    private static final long serialVersionUID = 1451611233309709951L;
    private String name;
    private String absolutePath;
    private long size;
    private boolean isFolder;
    private transient Image icon;
    private transient Path path;

    private FileInfo newValue;

    public FileInfo(Path path) {
        try {
            var file = new File(path.toUri());
            this.name = file.getName();
            this.absolutePath = file.getAbsolutePath();
            this.size = Files.size(path);
            this.isFolder = Files.isDirectory(path);
            this.path = path;
            setIcon(this.absolutePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public FileInfo() {
    }

    public FileInfo setName(String name) {
        this.name = name;
        return this;
    }

    private void setIcon(String absolutePath) {
        this.icon = FileInfoHelper.setFileInfoIcon(absolutePath);
    }

    public String getAbsolutePath() {
        return this.absolutePath;
    }

    public FileInfo setAbsolutePath(String absolutePath){
        this.absolutePath = absolutePath;
        return this;
    }

    public long getSize() {
        return this.size;
    }

    public Path getPath() {
        return this.path;
    }

    public String getName() {
        return this.name;
    }

    public boolean isFolder() {
        return this.isFolder;
    }

    public Image getIcon() {
        if (icon == null) {
            setIcon(this.absolutePath);
        }
        return this.icon;
    }

    public FileInfo getNewValue() {
        return newValue;
    }

    public void setNewValue(FileInfo newValue) {
        this.newValue = newValue;
    }
}

