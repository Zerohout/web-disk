package com.sepo.web.disk.common.helpers;

import com.sepo.web.disk.common.models.FileInfo;
import com.sepo.web.disk.common.models.Folder;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class MainHelper {
    public static final String SERVER_FOLDER_NAME = "server_folder";

    public static void createFileTree(Path path, Folder mainFolder, String folderName)  {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
            for (var p : stream) {
                var fileInfo = new FileInfo(p);
                if (Files.isDirectory(p)) {
                    var folder = new Folder(fileInfo, folderName);
                    mainFolder.addFolders(folder);
                    createFileTree(p, folder, folderName);
                }else{
                    mainFolder.addFiles(fileInfo);
                }
            }
        }catch (IOException ex){
            ex.printStackTrace();
        }
    }
}
