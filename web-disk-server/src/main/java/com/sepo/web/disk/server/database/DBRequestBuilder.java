package com.sepo.web.disk.server.database;

import java.util.ArrayList;

/**
    LinqToSql analog
 **/

public class DBRequestBuilder {
    private final String SELECT = "SELECT";
    private final String FROM_DB = "FROM users";
    private final String WHERE = "WHERE";
    private final String INSERT = "INSERT INTO users";
    private final String VALUES = "VALUES";
    private final String UPDATE = "UPDATE users SET";
    private final String DELETE = "DELETE FROM users";
    private final String AND = "AND";

    private ArrayList<String> requestParts = new ArrayList<>();
    private ArrayList<String> insertParts = new ArrayList<>();

    public DBRequestBuilder select(String... args) {
        requestParts.add(SELECT);
        if (args == null || args.length == 0 || args[0] == null
                || args[0].equals("") || args[0].equals("*")) {
            requestParts.add("*");
        } else {
            int size = args.length;
            for (int i = 0; i < size; i++) {
                String arg = args[i];
                if (i != size - 1) {
                    arg += ",";
                }
                requestParts.add(arg);
            }
        }
        requestParts.add(FROM_DB);
        return this;
    }

    public DBRequestBuilder delete() {
        requestParts.add(DELETE);
        return this;
    }

    //region WHERE
    public <T> DBRequestBuilder where(String key, T value) {
        checkWhere();
        addKeyValue(key, value);
        return this;
    }

    private void checkWhere() {
        if (!requestParts.contains(WHERE)) {
            requestParts.add(WHERE);
            return;
        }
        if (requestParts.indexOf(WHERE) != requestParts.size() - 1) {
            requestParts.add(AND);
        }
    }
    //endregion

    //region UPDATE
    public <T> DBRequestBuilder update(String key, T value) {
        checkUpdate();
        if (requestParts.size() >= 2) {
            int index = requestParts.size() - 1;
            String temp = requestParts.get(index);
            temp += ",";
            requestParts.remove(index);
            requestParts.add(temp);
        }

        addKeyValue(key, value);
        return this;
    }

    private void checkUpdate() {
        if (!requestParts.contains(UPDATE)) {
            requestParts.add(UPDATE);
        }
    }
    //endregion

    //region INSERT
    public <T> DBRequestBuilder insert(String key, T value) {
        checkInsert();
        int valuesIndex = insertParts.indexOf(VALUES);
        if (valuesIndex == insertParts.size() - 1) {
            insertParts.add(valuesIndex, String.format("(%s)", key));
            if (value instanceof String) {
                insertParts.add(String.format("('%s')", value));
            } else if (value instanceof Integer) {
                insertParts.add(String.format("(%d)", value));
            } else if (value instanceof Boolean) {
                insertParts.add(String.format("(%d)", (Boolean) value ? 1 : 0));
            }
        } else {
            String lastKey = insertParts.get(valuesIndex - 1).replace(")", ",");
            insertParts.remove(valuesIndex - 1);
            insertParts.add(valuesIndex - 1, lastKey);
            insertParts.add(valuesIndex, key + ")");

            String lastValue = insertParts.get(insertParts.size() - 1).replace(")", ",");
            insertParts.remove(insertParts.size() - 1);
            insertParts.add(lastValue);
            if (value instanceof String) {
                insertParts.add(String.format("'%s')", value));
            } else if (value instanceof Integer) {
                insertParts.add(String.format("%d)", value));
            } else if (value instanceof Boolean) {
                insertParts.add(String.format("%d)", (Boolean) value ? 1 : 0));
            }
        }
        return this;
    }

    public DBRequestBuilder buildInsert() {
        requestParts.addAll(insertParts);
        return this;
    }

    private void checkInsert() {
        if (!insertParts.contains(INSERT)) {
            insertParts.add(INSERT);
            insertParts.add(VALUES);
        }
    }
    //endregion

    public DBRequestBuilder reset() {
        return new DBRequestBuilder();
    }

    public String build() {
        StringBuilder out = new StringBuilder();
        int size = requestParts.size();
        for (int i = 0; i < size; i++) {
            out.append(requestParts.get(i));
            out.append(i != size - 1 ? " " : ";");
        }
        return out.toString();
    }

    private <T> void addKeyValue(String key, T value) {
        if (value instanceof String) {
            requestParts.add(String.format("%s = '%s'", key, value));
        } else if (value instanceof Integer) {
            requestParts.add(String.format("%s = %d", key, value));
        } else if (value instanceof Boolean) {
            requestParts.add(String.format("%s = %d", key, (Boolean) value ? 1 : 0));
        }
    }

    public static String getDropUsersTableRequest() {
        return "DROP TABLE IF EXISTS users;";
    }

    public static String getCreateUsersTableRequest() {
        return
            "CREATE TABLE IF NOT EXISTS \"users\" ("+
                "\"id\" INTEGER NOT NULL UNIQUE,"+
                "\"email\" TEXT NOT NULL UNIQUE,"+
                "\"password\" TEXT NOT NULL,"+
                "PRIMARY KEY(\"id\" AUTOINCREMENT))";
    }
}

