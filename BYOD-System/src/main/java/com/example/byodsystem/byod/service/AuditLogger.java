package com.example.byodsystem.byod.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class AuditLogger {

    public static void log(Connection conn, int operatorId, String actionType,
                           String targetType, Integer targetId, String detailsJson) {
        String sql = "INSERT INTO audit_log (operator_id, action_type, target_type, target_id, details, performed_at) " +
                "VALUES (?, ?, ?, ?, ?::jsonb, NOW() AT TIME ZONE 'Asia/Manila')";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, operatorId);
            ps.setString(2, actionType);
            ps.setString(3, targetType);
            if (targetId != null) ps.setInt(4, targetId);
            else ps.setNull(4, java.sql.Types.INTEGER);
            ps.setString(5, detailsJson != null ? detailsJson : "{}");
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}