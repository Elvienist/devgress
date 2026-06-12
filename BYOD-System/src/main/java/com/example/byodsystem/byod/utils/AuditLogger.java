package com.example.byodsystem.byod.utils;

import com.example.byodsystem.byod.database.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class AuditLogger {

    public void log(int operatorId, String actionType, String targetType, String targetId, String details) {
        String query = "INSERT INTO audit_log (operator_id, action_type, target_type, target_id, details) VALUES (?, ?, ?, ?, ?::jsonb)";

        try (Connection conn = DBConnection.connect()) {

            // Check if connection was successful before proceeding
            if (conn == null) {
                System.err.println("Audit log failed: Could not establish database connection.");
                return; // Exit the method early
            }

            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setInt(1, operatorId);
                pstmt.setString(2, actionType);
                pstmt.setString(3, targetType);
                pstmt.setString(4, targetId);
                pstmt.setString(5, details);

                pstmt.executeUpdate();
            }

        } catch (Exception e) {
            System.err.println("Audit log failed to write: " + e.getMessage());
        }
    }
}