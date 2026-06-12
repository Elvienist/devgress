package com.example.byodsystem.byod.dao;

import com.example.byodsystem.byod.database.DBConnection;
import com.example.byodsystem.byod.model.UserModel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class AuthDAO {

    public static UserModel getByUsername(String username) {
        String sql = "SELECT user_id, username, full_name, role, password_hash, first_login, status " +
                "FROM users WHERE username = ? AND status = 'ACTIVE'";

        try (Connection conn = DBConnection.connect();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setString(1, username);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                return new UserModel(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getString("full_name"),
                        rs.getString("role"),
                        rs.getString("password_hash"),
                        rs.getBoolean("first_login"),
                        rs.getString("status")
                );
            }
            return null;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void updatePassword(int userId, String newHashedPassword) {
        String sql = "UPDATE users SET password_hash = ? WHERE user_id = ?";

        try (Connection conn = DBConnection.connect();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setString(1, newHashedPassword);
            pst.setInt(2, userId);
            pst.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void clearFirstLogin(int userId) {
        String sql = "UPDATE users SET first_login = FALSE WHERE user_id = ?";

        try (Connection conn = DBConnection.connect();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setInt(1, userId);
            pst.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void updateLastLogin(int userId) {
        String sql = "UPDATE users SET last_login = CURRENT_TIMESTAMP WHERE user_id = ?";

        try (Connection conn = DBConnection.connect();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setInt(1, userId);
            pst.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static UserModel getById(int userId) {
        String sql = "SELECT user_id, username, full_name, role, password_hash, first_login, status " +
                "FROM users WHERE user_id = ?";

        try (Connection conn = DBConnection.connect();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setInt(1, userId);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                return new UserModel(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getString("full_name"),
                        rs.getString("role"),
                        rs.getString("password_hash"),
                        rs.getBoolean("first_login"),
                        rs.getString("status")
                );
            }
            return null;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}