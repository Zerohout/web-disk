module com.sepo.web.disk {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.sepo.web.disk.controllers to javafx.fxml;
    exports com.sepo.web.disk;
}