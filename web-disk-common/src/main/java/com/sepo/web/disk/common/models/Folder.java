package com.sepo.web.disk.common.models;

import java.util.ArrayList;
import java.util.Arrays;

public class Folder implements Sendable {
    private String name;
    private FileInfo fileInfo;
    private ArrayList<Folder> folders;
    private ArrayList<FileInfo> files;

    public Folder(FileInfo fileInfo, String name) {
        this.fileInfo = fileInfo;
        folders = new ArrayList<>();
        files = new ArrayList<>();
        this.name = name;
    }

    public FileInfo getFileInfo() {
        return this.fileInfo;
    }

    public void addFolders(Folder... folders) {
        this.folders.addAll(Arrays.asList(folders));
    }

    public void addFiles(FileInfo... files) {
        this.files.addAll(Arrays.asList(files));
    }

    public ArrayList<Folder> getFolders() {
        return folders;
    }

    public ArrayList<FileInfo> getFiles() {
        return files;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
