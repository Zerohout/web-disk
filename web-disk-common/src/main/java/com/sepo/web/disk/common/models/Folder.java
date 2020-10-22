package com.sepo.web.disk.common.models;

import java.util.ArrayList;
import java.util.Arrays;

public class Folder implements Sendable {

    private FileInfo fileInfo;
    private ArrayList<Folder> folders;
    private ArrayList<FileInfo> files;

    public Folder(FileInfo fileInfo) {
        this.fileInfo = fileInfo;
        folders = new ArrayList<>();
        files = new ArrayList<>();
    }

    public FileInfo getFileInfo() {
        return this.fileInfo;
    }

    public void addFolders(Folder... folders){
        this.folders.addAll(Arrays.asList(folders));
    }

    public void addFiles(FileInfo... files){
        this.files.addAll(Arrays.asList(files));
    }

    public ArrayList<Folder> getFolders() {
        return folders;
    }

    public ArrayList<FileInfo> getFiles() {
        return files;
    }
}
