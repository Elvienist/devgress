package com.example.byodsystem.byod.database;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {

    private static final String URL = "jdbc:postgresql://dpg-d82knujtqb8s73ehb140-a.singapore-postgres.render.com:5432/byod_system_database";
    private static final String USER = "byod_system_database_user";
    private static final String PASSWORD = "8WqB9gzpV743jgjcBZlWmETneYCUPh0P";

    public static Connection connect() {
        Connection conn = null;
        try {
            Class.forName("org.postgresql.Driver");
            conn = DriverManager.getConnection(URL, USER, PASSWORD);
            //System.out.println("Database Connected Successfully");
        } catch (Exception e) {
            System.out.println("Database Connection Failed");
            e.printStackTrace();
        }
        return conn;
    }
}