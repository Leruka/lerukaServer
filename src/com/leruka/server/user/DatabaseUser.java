package com.leruka.server.user;

/**
 * Created by leifb on 17.05.16.
 */
class DatabaseUser {
    private int userID;
    private String salt, dbPass;

    DatabaseUser(int userID, String salt, String dbPass) {
        this.userID = userID;
        this.salt = salt;
        this.dbPass = dbPass;
    }

    int getUserID() {
        return userID;
    }

    String getSalt() {
        return salt;
    }

    String getDbPass() {
        return dbPass;
    }
}
