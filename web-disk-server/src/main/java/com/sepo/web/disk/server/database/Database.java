package com.sepo.web.disk.server.database;

import com.sepo.web.disk.common.models.User;
import com.sepo.web.disk.server.ServerApp;

import java.sql.*;

import static com.sepo.web.disk.server.database.DBRequestBuilder.getCreateUsersTableRequest;
import static com.sepo.web.disk.server.database.DBRequestBuilder.getDropUsersTableRequest;

public class Database {

    static {
        try {
            Class.forName("org.sqlite.JDBC");

        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
        }
        dbr = new DBRequestBuilder();
    }

    private static DBRequestBuilder dbr;
    private static Connection connection;
    private static Statement statement;

    private static void openConnection() {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:users.db");
            statement = connection.createStatement();
        } catch (SQLException e) {
            e.printStackTrace();
            closeConnection();
        }
    }

    public static void createUsersTable() {
        executeUpdate(getCreateUsersTableRequest());
    }

    public static void reCreateUsersTable() {
        dropUsersTable();
        createUsersTable();
    }

    public static void dropUsersTable() {
        executeUpdate(getDropUsersTableRequest());
    }

    public static void clearUsersTable() {
        executeUpdate(dbr.reset().delete().build());
    }

    public static void closeConnection() {
        try {
            statement.close();
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static ResultSet executeQuery(String sqlRequest) {
        try {
            return statement.executeQuery(sqlRequest);
        } catch (SQLException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private static boolean executeUpdate(String sqlCommand) {
        try {
            if (connection == null || connection.isClosed()) openConnection();
            statement.executeUpdate(sqlCommand);
            closeConnection();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            closeConnection();
        }
        return false;
    }

    // [0] - всегда email [1] - опционально password
    public static User getUser(String... args) {
        try {
            if (connection == null || connection.isClosed()) openConnection();
            ResultSet result;
            if (args.length == 1) {
                result = executeQuery(dbr.reset().select().where("email", args[0]).build());
            } else {
                result = executeQuery(dbr.reset()
                        .select()
                        .where("email", args[0])
                        .where("password", args[1]).build());
            }
            if (result == null) return null;
            var out = new User(result.getString("email"), result.getString("password"));
            if (connection != null || !connection.isClosed()) closeConnection();
            return out;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static boolean insertUser(User user) {
        return executeUpdate(dbr.reset()
                .insert("email", user.getEmail())
                .insert("password", user.getPassword())
                .buildInsert()
                .build());
    }

    public static boolean deleteUser(String userNickname) {
        return executeUpdate(dbr.reset().delete().where("nickname", userNickname).build());
    }

}
