module com.sepo.web.disk.web.disk.client {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.apache.logging.log4j;
    requires org.apache.commons.io;
    requires com.sepo.web.disk.web.disk.common;

    opens com.sepo.web.disk.controllers to javafx.fxml;
    exports com.sepo.web.disk;

}

