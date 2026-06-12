package com.example.byodsystem.byod.database;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

public class DBConnection {

    public static Connection connect() {
        Connection conn = null;
        try {
            Properties props = new Properties();
            InputStream input = DBConnection.class
                    .getClassLoader()
                    .getResourceAsStream("config.properties");

            if (input == null) {
                System.out.println("config.properties not found!");
                return null;
            }

            props.load(input);
            String url = props.getProperty("db.url");
            String user = props.getProperty("db.user");
            String password = props.getProperty("db.password");

            Class.forName("org.postgresql.Driver");
            conn = DriverManager.getConnection(url, user, password);

        } catch (Exception e) {
            System.out.println("Database Connection Failed");
            e.printStackTrace();
        }
        return conn;
    }
}
