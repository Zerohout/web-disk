package com.sepo.web.disk.server;

import com.sepo.web.disk.server.connection.Network;
import com.sepo.web.disk.server.database.Database;

import java.nio.file.Paths;

public class ServerApp {
    public static String dbPath;

    public static void main(String[] args) {
        dbPath = Paths.get("database").normalize().toAbsolutePath().resolve("users.db").toString();
        Database.createUsersTable();
        Network.getInstance().start();
    }



}
