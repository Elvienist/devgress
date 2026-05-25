package com.example.byodsystem.byod.controller;

import com.example.byodsystem.byod.database.DBConnection;
import com.example.byodsystem.byod.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class MonitoringController {
    @FXML private Button btnDashboard, btnStudents, btnDevices, btnLogs, btnActivity, btnReports, btnSettings, btnLogout;
    @FXML private Label lblDate, lblAdminName, lblAdminRole;
    @FXML private Label lblInsideCount, lblOutsideCount, lblEntriesCount, lblExitsCount;
    @FXML private TextField txtSearchInput;
    @FXML private Button btnCheckIn, btnCheckOut;

    @FXML
    public void initialize() {
        lblAdminName.setText(SessionManager.currentOperatorName);
        lblAdminRole.setText(SessionManager.currentOperatorRole);
        lblDate.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, MMM dd, yyyy")));
        setupSidebarActions();
        loadRealtimeStats();

        btnCheckIn.setOnAction(e -> handleLogAction(true));
        btnCheckOut.setOnAction(e -> handleLogAction(false));
    }

    private void loadRealtimeStats() {
        String query = "SELECT " +
                "(SELECT COUNT(*) FROM device_logs WHERE logout_time IS NULL) AS inside, " +
                "(SELECT COUNT(*) FROM devices WHERE device_id NOT IN (SELECT device_id FROM device_logs WHERE logout_time IS NULL)) AS outside, " +
                "(SELECT COUNT(*) FROM device_logs WHERE CAST(login_time AS DATE) = CURRENT_DATE) AS entries, " +
                "(SELECT COUNT(*) FROM device_logs WHERE CAST(logout_time AS DATE) = CURRENT_DATE) AS exits";

        try (Connection conn = DBConnection.connect(); PreparedStatement ps = conn.prepareStatement(query); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                lblInsideCount.setText(String.valueOf(rs.getInt("inside")));
                lblOutsideCount.setText(String.valueOf(rs.getInt("outside")));
                lblEntriesCount.setText(String.valueOf(rs.getInt("entries")));
                lblExitsCount.setText(String.valueOf(rs.getInt("exits")));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleLogAction(boolean isCheckIn) {
        String target = txtSearchInput.getText().trim();
        String deviceLookup = "SELECT device_id FROM devices WHERE serial_number = ? OR owner_id = (SELECT student_record_id FROM students WHERE student_id = ? LIMIT 1) LIMIT 1";

        try (Connection conn = DBConnection.connect(); PreparedStatement psLook = conn.prepareStatement(deviceLookup)) {
            psLook.setString(1, target);
            psLook.setString(2, target);

            try (ResultSet rs = psLook.executeQuery()) {
                if (rs.next()) {
                    int deviceId = rs.getInt("device_id");
                    if (isCheckIn) {
                        String checkInSQL = "INSERT INTO device_logs (device_id, operator_id, login_time) VALUES (?, ?, CURRENT_TIMESTAMP)";
                        try (PreparedStatement psIn = conn.prepareStatement(checkInSQL)) {
                            psIn.setInt(1, deviceId);
                            psIn.setInt(2, SessionManager.currentOperatorId);
                            psIn.executeUpdate();
                        }
                    } else {
                        String checkOutSQL = "UPDATE device_logs SET logout_time = CURRENT_TIMESTAMP WHERE device_id = ? AND logout_time IS NULL";
                        try (PreparedStatement psOut = conn.prepareStatement(checkOutSQL)) {
                            psOut.setInt(1, deviceId);
                            psOut.executeUpdate();
                        }
                    }
                    txtSearchInput.clear();
                    loadRealtimeStats();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupSidebarActions() {
        btnDashboard.setOnAction(e -> SessionManager.navigate(btnDashboard, "dashboard.fxml"));
        btnStudents.setOnAction(e -> SessionManager.navigate(btnStudents, "students.fxml"));
        btnDevices.setOnAction(e -> SessionManager.navigate(btnDevices, "devices.fxml"));
        btnLogs.setOnAction(e -> SessionManager.navigate(btnLogs, "monitoring.fxml"));
        btnActivity.setOnAction(e -> SessionManager.navigate(btnActivity, "reports.fxml"));
        btnReports.setOnAction(e -> SessionManager.navigate(btnReports, "reports.fxml"));
        btnLogout.setOnAction(e -> SessionManager.navigate(btnLogout, "login.fxml"));
    }
}