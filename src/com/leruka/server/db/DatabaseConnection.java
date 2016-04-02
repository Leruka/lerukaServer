package com.leruka.server.db;

import com.leruka.server.Log;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Created by leif on 04.11.15.
 */
public class DatabaseConnection {

    private static Connection connection;
    private static String databaseName;
    private static String databaseUser;
    private static String databasePass;

    static {
        readCredentials("/home/leruka/serverHandler/credentials");
        createConnection();
    }

    public static Connection getCurrentConnection() {
        // Test if the database connection is valid, and recreate if not. The test usually takes less than 1ms
        try {
            if (!connection.isValid(1)) {
                createConnection();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return connection;
    }

    private static void createConnection() {
        // Create connection
        try {
            connection = getConnection();
        } catch (ClassNotFoundException e) {
            // jdbc Driver not found
            System.out.println("#[ERR] Database ERROR! Driver not found");
        } catch (SQLException | InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private static Connection getConnection() throws ClassNotFoundException, IllegalAccessException,
            InstantiationException, SQLException {
        Class.forName("com.mysql.jdbc.Driver").newInstance();
        return DriverManager.getConnection(
                String.format(
                        "jdbc:mysql://localhost/%s?user=%s&password=%s&noAccessToProcedureBodies=true",
                        databaseName,
                        databaseUser,
                        databasePass
                )
        );
    }

    private static void readCredentials(String fileName) {
        Properties properties = new Properties();
        File file = new File(fileName);
        try {
            FileInputStream inputStream = new FileInputStream(file);
            properties.load(inputStream);
            databaseName = properties.getProperty("db");
            databasePass = properties.getProperty("pass");
            databaseUser = properties.getProperty("user");
        } catch (FileNotFoundException e) {
            Log.err("Could not find the credentials file! Cannot to connect to database without it.");
        } catch (IOException e) {
            Log.err("Could not read the credentials file! Cannot to connect to database without it.");
        }
    }

}
