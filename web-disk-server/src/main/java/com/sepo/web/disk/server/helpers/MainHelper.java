package com.sepo.web.disk.server.helpers;

import com.sepo.web.disk.common.models.FileInfo;
import com.sepo.web.disk.common.models.Folder;
import com.sepo.web.disk.server.handlers.MainHandler;
import io.netty.channel.socket.SocketChannel;
import javafx.scene.image.ImageView;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class MainHelper {
    private static final Logger logger = LogManager.getLogger(MainHelper.class);
    public static SocketChannel socketChannel;

    public static void createFileTree(Path path, Folder mainFolder)  {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
            for (var p : stream) {
                var fileInfo = new FileInfo(p);
                if (Files.isDirectory(p)) {
                    var folder = new Folder(fileInfo);
                    mainFolder.addFolders(folder);
                    createFileTree(p, folder);
                }else{
                    mainFolder.addFiles(fileInfo);
                }
            }
        }catch (IOException ex){
            ex.printStackTrace();
        }
    }
}
