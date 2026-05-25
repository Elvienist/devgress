package com.example.byodsystem.byod.controller;

import com.example.byodsystem.byod.database.DBConnection;
import com.example.byodsystem.byod.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DashboardController {
    @FXML private Button btnDashboard, btnStudents, btnDevices, btnLogs, btnActivity, btnReports, btnSettings, btnLogout;
    @FXML private Label lblDate, lblAdminName, lblAdminRole;
    @FXML private Label lblRegisteredDevices, lblCurrentlyInside, lblTodayEntries, lblTodayExits;
    @FXML private Label lblLaptopsCount, lblTabletsCount, lblOthersCount;
    @FXML private ProgressBar progressLaptops, progressTablets, progressOthers;
    @FXML private LineChart<String, Number> entryPatternChart;

    @FXML
    public void initialize() {
        lblAdminName.setText(SessionManager.currentOperatorName);
        lblAdminRole.setText(SessionManager.currentOperatorRole);
        lblDate.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, MMM dd, yyyy")));
        setupSidebarActions();
        loadMetrics();
    }

    private void loadMetrics() {
        String query = "SELECT " +
                "(SELECT COUNT(*) FROM devices) AS registered, " +
                "(SELECT COUNT(*) FROM device_logs WHERE logout_time IS NULL) AS inside, " +
                "(SELECT COUNT(*) FROM device_logs WHERE CAST(login_time AS DATE) = CURRENT_DATE) AS entries, " +
                "(SELECT COUNT(*) FROM device_logs WHERE CAST(logout_time AS DATE) = CURRENT_DATE) AS exits, " +
                "(SELECT COUNT(*) FROM devices d JOIN device_logs dl ON d.device_id = dl.device_id WHERE dl.logout_time IS NULL AND d.device_type = 'Laptop') AS laps, " +
                "(SELECT COUNT(*) FROM devices d JOIN device_logs dl ON d.device_id = dl.device_id WHERE dl.logout_time IS NULL AND d.device_type = 'Tablet') AS tabs, " +
                "(SELECT COUNT(*) FROM devices d JOIN device_logs dl ON d.device_id = dl.device_id WHERE dl.logout_time IS NULL AND d.device_type = 'Other') AS oth";

        try (Connection conn = DBConnection.connect(); PreparedStatement ps = conn.prepareStatement(query); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                int totalInside = rs.getInt("inside");
                lblRegisteredDevices.setText(String.valueOf(rs.getInt("registered")));
                lblCurrentlyInside.setText(String.valueOf(totalInside));
                lblTodayEntries.setText(String.valueOf(rs.getInt("entries")));
                lblTodayExits.setText(String.valueOf(rs.getInt("exits")));

                int laps = rs.getInt("laps");
                int tabs = rs.getInt("tabs");
                int oth = rs.getInt("oth");

                lblLaptopsCount.setText(String.valueOf(laps));
                lblTabletsCount.setText(String.valueOf(tabs));
                lblOthersCount.setText(String.valueOf(oth));

                if (totalInside > 0) {
                    progressLaptops.setProgress((double) laps / totalInside);
                    progressTablets.setProgress((double) tabs / totalInside);
                    progressOthers.setProgress((double) oth / totalInside);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        XYChart.Series<String, Number> entrySeries = new XYChart.Series<>();
        entrySeries.setName("Entries");
        String chartQuery = "SELECT EXTRACT(HOUR FROM login_time) AS hr, COUNT(*) AS cnt FROM device_logs WHERE CAST(login_time AS DATE) = CURRENT_DATE GROUP BY hr ORDER BY hr ASC";
        int[] hourlyBuckets = new int[24];

        try (Connection conn = DBConnection.connect(); PreparedStatement ps = conn.prepareStatement(chartQuery); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                hourlyBuckets[rs.getInt("hr")] = rs.getInt("cnt");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (int h = 6; h <= 18; h++) {
            String label = (h == 12) ? "12PM" : (h > 12) ? (h - 12) + "PM" : h + "AM";
            entrySeries.getData().add(new XYChart.Data<>(label, hourlyBuckets[h]));
        }
        entryPatternChart.getData().clear();
        entryPatternChart.getData().add(entrySeries);
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