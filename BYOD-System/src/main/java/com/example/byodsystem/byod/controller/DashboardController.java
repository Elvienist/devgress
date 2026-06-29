package com.example.byodsystem.byod.controller;

import com.example.byodsystem.byod.database.DBConnection;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DashboardController {

    @FXML private Label dateLabel;
    @FXML private Label adminNameLabel;
    @FXML private Label adminRoleLabel;
    @FXML private Label adminInitialLabel;
    @FXML private Label welcomeLabel;

    @FXML private Label totalStudentsLabel;
    @FXML private Label totalDevicesLabel;
    @FXML private Label devicesInsideLabel;
    @FXML private Label todayEntriesLabel;
    @FXML private Label todayExitsLabel;

    @FXML private Label studentsTrendLabel;
    @FXML private Label devicesTrendLabel;
    @FXML private Label insideTrendLabel;
    @FXML private Label entriesTodayTrendLabel;
    @FXML private Label exitsTodayTrendLabel;

    @FXML private BarChart<String, Number> entryPatternChart;

    @FXML private ProgressBar laptopsBar;
    @FXML private ProgressBar tabletsBar;
    @FXML private ProgressBar phonesBar;
    @FXML private ProgressBar othersBar;

    @FXML private Label laptopsCountLabel;
    @FXML private Label tabletsCountLabel;
    @FXML private Label phonesCountLabel;
    @FXML private Label othersCountLabel;

    @FXML private Label laptopsPctLabel;
    @FXML private Label tabletsPctLabel;
    @FXML private Label phonesPctLabel;
    @FXML private Label othersPctLabel;

    private int    currentUserId       = 1;
    private String currentUserFullName = "Administrator";
    private String currentUserRole     = "Admin";

    @FXML
    public void initialize() {
        dateLabel.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")));
        adminNameLabel.setText(currentUserFullName);
        adminRoleLabel.setText(currentUserRole);
        adminInitialLabel.setText(currentUserFullName.substring(0, 1).toUpperCase());
        welcomeLabel.setText("Welcome back, " + currentUserFullName);
        loadDashboardAsync();
    }

    public void setCurrentUser(int userId, String fullName, String role) {
        this.currentUserId       = userId;
        this.currentUserFullName = fullName;
        this.currentUserRole     = role;
    }

    private void loadDashboardAsync() {
        Thread t = new Thread(() -> {
            try (Connection conn = DBConnection.connect()) {
                if (conn == null) return;
                readStats(conn);
                readEntryPattern(conn);
                readDeviceBreakdown(conn);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private void readStats(Connection conn) throws SQLException {
        String sql = """
            SELECT
                (SELECT COUNT(*) FROM students WHERE status = 'ACTIVE') AS total_students,
                (SELECT COUNT(*) FROM devices  WHERE status = 'ACTIVE') AS total_devices,
                (SELECT COUNT(DISTINCT dl.device_id)
                   FROM device_logs dl
                   WHERE dl.direction = 'IN' AND dl.status = 'NORMAL'
                     AND NOT EXISTS (
                         SELECT 1 FROM device_logs dl2
                         WHERE dl2.device_id = dl.device_id
                           AND dl2.direction  = 'OUT'
                           AND dl2.log_time   > dl.log_time
                           AND dl2.status     = 'NORMAL'
                     )
                ) AS devices_inside,
                (SELECT COUNT(*) FROM device_logs
                   WHERE direction = 'IN'  AND status = 'NORMAL'
                     AND log_time::date = CURRENT_DATE) AS today_entries,
                (SELECT COUNT(*) FROM device_logs
                   WHERE direction = 'OUT' AND status = 'NORMAL'
                     AND log_time::date = CURRENT_DATE) AS today_exits,
                (SELECT COUNT(*) FROM device_logs
                   WHERE direction = 'IN'  AND status = 'NORMAL'
                     AND log_time::date = CURRENT_DATE - 1) AS yesterday_entries,
                (SELECT COUNT(*) FROM device_logs
                   WHERE direction = 'OUT' AND status = 'NORMAL'
                     AND log_time::date = CURRENT_DATE - 1) AS yesterday_exits,
                (SELECT COUNT(*) FROM students WHERE status = 'ACTIVE'
                     AND created_at::date >= date_trunc('month', CURRENT_DATE) - INTERVAL '1 month'
                     AND created_at::date <  date_trunc('month', CURRENT_DATE)) AS students_last_month,
                (SELECT COUNT(*) FROM devices WHERE status = 'ACTIVE'
                     AND created_at::date >= date_trunc('month', CURRENT_DATE) - INTERVAL '1 month'
                     AND created_at::date <  date_trunc('month', CURRENT_DATE)) AS devices_last_month
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                long ts  = rs.getLong("total_students");
                long td  = rs.getLong("total_devices");
                long di  = rs.getLong("devices_inside");
                long te  = rs.getLong("today_entries");
                long tex = rs.getLong("today_exits");
                long ye  = rs.getLong("yesterday_entries");
                long yex = rs.getLong("yesterday_exits");
                long slm = rs.getLong("students_last_month");
                long dlm = rs.getLong("devices_last_month");

                String sTrend  = buildTrend(ts, slm == 0 ? ts : slm);
                String dTrend  = buildTrend(td, dlm == 0 ? td : dlm);
                String iTrend  = di + " now";
                String eTrend  = buildTrend(te, ye);
                String exTrend = buildTrend(tex, yex);

                Platform.runLater(() -> {
                    totalStudentsLabel.setText(String.format("%,d", ts));
                    totalDevicesLabel.setText(String.format("%,d", td));
                    devicesInsideLabel.setText(String.format("%,d", di));
                    todayEntriesLabel.setText(String.format("%,d", te));
                    todayExitsLabel.setText(String.format("%,d", tex));
                    studentsTrendLabel.setText(sTrend);
                    devicesTrendLabel.setText(dTrend);
                    insideTrendLabel.setText(iTrend);
                    entriesTodayTrendLabel.setText(eTrend);
                    exitsTodayTrendLabel.setText(exTrend);
                });
            }
        }
    }

    private String buildTrend(long current, long previous) {
        if (previous == 0) return "—";
        double pct = ((current - previous) / (double) previous) * 100.0;
        return String.format("%+.0f%%", pct);
    }

    private void readEntryPattern(Connection conn) throws SQLException {
        String sql = """
            SELECT EXTRACT(HOUR FROM log_time)::INT AS hour, COUNT(*) AS cnt
            FROM   device_logs
            WHERE  direction = 'IN' AND status = 'NORMAL'
              AND  log_time::date = CURRENT_DATE
            GROUP  BY 1 ORDER BY 1
            """;

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                series.getData().add(
                        new XYChart.Data<>(formatHour(rs.getInt("hour")), rs.getLong("cnt")));
            }
        }

        Platform.runLater(() -> {
            entryPatternChart.setAnimated(false);
            entryPatternChart.getData().clear();
            entryPatternChart.getData().add(series);
            series.getData().forEach(d -> d.getNode().setStyle("-fx-bar-fill: #7A0000;"));
        });
    }

    private static String formatHour(int h) {
        if (h == 0)  return "12 AM";
        if (h < 12)  return h + " AM";
        if (h == 12) return "12 PM";
        return (h - 12) + " PM";
    }

    private void readDeviceBreakdown(Connection conn) throws SQLException {
        String sql = "SELECT device_type, COUNT(*) AS cnt FROM devices WHERE status = 'ACTIVE' GROUP BY device_type";
        long laptops = 0, tablets = 0, phones = 0, others = 0;
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                long cnt = rs.getLong("cnt");
                switch (rs.getString("device_type").toUpperCase()) {
                    case "LAPTOP" -> laptops += cnt;
                    case "TABLET" -> tablets += cnt;
                    case "PHONE"  -> phones  += cnt;
                    default       -> others  += cnt;
                }
            }
        }

        long total = laptops + tablets + phones + others;
        if (total == 0) return;

        final long fL = laptops, fT = tablets, fP = phones, fO = others;
        final double pL = (double) fL / total;
        final double pT = (double) fT / total;
        final double pP = (double) fP / total;
        final double pO = (double) fO / total;

        Platform.runLater(() -> {
            laptopsBar.setProgress(pL);
            tabletsBar.setProgress(pT);
            phonesBar.setProgress(pP);
            othersBar.setProgress(pO);

            laptopsCountLabel.setText(String.valueOf(fL));
            tabletsCountLabel.setText(String.valueOf(fT));
            phonesCountLabel.setText(String.valueOf(fP));
            othersCountLabel.setText(String.valueOf(fO));

            laptopsPctLabel.setText(Math.round(pL * 100) + "% of total");
            tabletsPctLabel.setText(Math.round(pT * 100) + "% of total");
            phonesPctLabel.setText(Math.round(pP * 100) + "% of total");
            othersPctLabel.setText(Math.round(pO * 100) + "% of total");
        });
    }

    private static String sanitizeColumnName(String fieldName) {
        return switch (fieldName.toLowerCase()) {
            case "full_name" -> "full_name";
            case "section"   -> "section";
            case "email"     -> "email";
            default          -> null;
        };
    }

    private static String prettyFieldName(String fieldName) {
        return switch (fieldName.toLowerCase()) {
            case "full_name" -> "Full Name";
            case "section"   -> "Section";
            case "email"     -> "Email";
            default          -> fieldName;
        };
    }

}