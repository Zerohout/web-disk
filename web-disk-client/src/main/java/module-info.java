module com.sepo.web.disk.web.disk.client {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.apache.logging.log4j;
    requires org.apache.commons.io;
    requires io.netty.all;
    requires com.sepo.web.disk.web.disk.common;

    opens com.sepo.web.disk.client.controllers to javafx.fxml;
    exports com.sepo.web.disk.client;
    exports com.sepo.web.disk.client.controls;
}

