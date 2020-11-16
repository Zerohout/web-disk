package com.sepo.web.disk.server;

import com.sepo.web.disk.server.connection.Network;
import com.sepo.web.disk.server.database.Database;
import com.sepo.web.disk.server.helpers.JavaConfig;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.nio.file.Paths;
public class ServerApp {
    public static String dbPath;

    public static void main(String[] args) {
        ApplicationContext context = new AnnotationConfigApplicationContext(JavaConfig.class);
        dbPath = Paths.get("database").normalize().toAbsolutePath().resolve("users.db").toString();
        Database.createUsersTable();
        context.getBean("network",Network.class).start();
    }
}
