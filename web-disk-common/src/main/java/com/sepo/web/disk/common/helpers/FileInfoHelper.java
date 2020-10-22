package com.sepo.web.disk.common.helpers;

import com.sepo.web.disk.common.models.FileInfo;
import javafx.scene.image.Image;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.nio.file.Path;

public class FileInfoHelper {
    public static Image setFileInfoIcon(String absolutePath) {
        var file = new File(absolutePath);
        var extension = FilenameUtils.getExtension(file.getName());
        String iconName;
        if (file.isDirectory()) {
            iconName = "folder";
        } else {
            switch (extension) {
                case "bmp":
                case "cr2":
                case "crw":
                case "dds":
                case "dng":
                case "gif":
                case "ico":
                case "j2k":
                case "jpg":
                case "jpeg":
                case "mrw":
                case "mng":
                case "msp":
                case "nef":
                case "orf":
                case "pcx":
                case "pef":
                case "png":
                case "psd":
                case "pspimage":
                case "raf":
                case "raw":
                case "tga":
                case "tiff":
                case "wmf":
                    iconName = "image";
                    break;
                case "3g2":
                case "3gp":
                case "3gp2":
                case "3gpp":
                case "3gpp2":
                case "asf":
                case "asx":
                case "avi":
                case "f4v":
                case "flv":
                case "h264":
                case "m4v":
                case "mkv":
                case "mod":
                case "mov":
                case "mp4":
                case "mpeg":
                case "mpg":
                case "mts":
                case "rm":
                case "rmvb":
                case "srt":
                case "swf":
                case "ts":
                case "vcd":
                case "vid":
                case "vob":
                case "webm":
                case "wmv":
                    iconName = "video";
                    break;
                case "aac":
                case "ac3":
                case "aif":
                case "amr":
                case "aob":
                case "ape":
                case "aud":
                case "cdr":
                case "flac":
                case "gpx":
                case "ics":
                case "iff":
                case "m3u":
                case "m3u8":
                case "m4a":
                case "m4b":
                case "m4p":
                case "m4r":
                case "mid":
                case "midi":
                case "mp3":
                case "mpa":
                case "ogg":
                case "ra":
                case "ram":
                case "sdf":
                case "sib":
                case "spl":
                case "wav":
                case "wave":
                case "wma":
                    iconName = "audio";
                    break;
                case "apt":
                case "err":
                case "log":
                case "pwi":
                case "sub":
                case "ttf":
                case "tex":
                case "text":
                case "txt":
                    iconName = "text";
                    break;
                case "doc":
                case "docm":
                case "docx":
                case "dot":
                case "dotm":
                case "dotx":
                case "epub":
                case "fb2":
                case "ibooks":
                case "indd":
                case "key":
                case "kml":
                case "mobi":
                case "mso":
                case "ods":
                case "odt":
                case "pages":
                case "pdf":
                case "pot":
                case "potm":
                case "potx":
                case "pps":
                case "ppsm":
                case "ppsx":
                case "ppt":
                case "pptm":
                case "pptx":
                case "pub":
                case "rtf":
                case "sldm":
                case "wpd":
                case "wps":
                case "xlr":
                case "xls":
                case "xlsb":
                case "xlsm":
                case "xlsx":
                case "xlt":
                case "xltm":
                case "xltx":
                case "xps":
                    iconName = "document";
                    break;
                case "7z":
                case "ace":
                case "arj":
                case "cab":
                case "cbr":
                case "deb":
                case "gz":
                case "gzip":
                case "pkg":
                case "rar":
                case "rpm":
                case "sit":
                case "sitx":
                case "tar":
                case "tar-gz":
                case "tgz":
                case "xar":
                case "zip":
                case "zipx":
                    iconName = "archive";
                    break;
                default:
                    iconName = "file";
                    break;
            }
        }
        return new Image(FileInfo.class.getResourceAsStream("/icons/" + iconName + ".png"));
    }
}
