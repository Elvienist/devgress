package com.example.byodsystem.byod.controller;

import com.example.byodsystem.byod.database.DBConnection;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class DashboardController {

    @FXML private Label dateLabel;
    @FXML private Label adminNameLabel;
    @FXML private Label adminRoleLabel;
    @FXML private Label adminInitialLabel;
    @FXML private Label welcomeLabel;

    @FXML private HBox    pendingBanner;
    @FXML private Label   pendingCountLabel;

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

    @FXML private StackPane reviewOverlay;
    @FXML private Label     overlayTitleLabel;
    @FXML private Label     overlayCounterLabel;
    @FXML private Label     overlayStudentNameLabel;
    @FXML private TextField overlayFieldLabel;
    @FXML private TextField overlayCurrentLabel;
    @FXML private TextField overlayRequestedLabel;
    @FXML private TextArea  overlayReasonArea;
    @FXML private VBox      rejectReasonBox;
    @FXML private TextField rejectReasonField;
    @FXML private Button    overlayRejectBtn;

    private int    currentUserId       = 1;
    private String currentUserFullName = "Administrator";
    private String currentUserRole     = "Admin";

    private final List<PendingRequest> pendingQueue = new ArrayList<>();
    private int queueIndex = 0;

    private record PendingRequest(int requestId, int studentId,
                                  String fieldName, String currentValue,
                                  String requestedValue, String reason,
                                  String studentName) {}

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
                readPendingRequests(conn);
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

    private void readPendingRequests(Connection conn) throws SQLException {
        String sql = "SELECT COUNT(*) AS cnt FROM profile_update_requests WHERE status = 'PENDING'";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                int cnt = rs.getInt("cnt");
                Platform.runLater(() -> {
                    if (cnt == 0) {
                        pendingBanner.setVisible(false);
                        pendingBanner.setManaged(false);
                    } else {
                        pendingBanner.setVisible(true);
                        pendingBanner.setManaged(true);
                        pendingCountLabel.setText(
                                "You have " + cnt + " pending student profile update request"
                                        + (cnt == 1 ? "" : "s"));
                    }
                });
            }
        }
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

    @FXML
    public void handleDismissBanner() {
        pendingBanner.setVisible(false);
        pendingBanner.setManaged(false);
    }

    @FXML
    public void handleReviewRequests() {
        Thread t = new Thread(() -> {
            try (Connection conn = DBConnection.connect()) {
                if (conn == null) return;

                List<PendingRequest> rows = new ArrayList<>();
                String sql = """
                    SELECT pur.request_id, pur.field_name, pur.current_value,
                           pur.requested_value, pur.reason,
                           s.full_name AS student_name, s.student_id
                    FROM   profile_update_requests pur
                    JOIN   students s ON s.student_id = pur.student_id
                    WHERE  pur.status = 'PENDING'
                    ORDER  BY pur.submitted_at ASC
                    """;

                try (PreparedStatement ps = conn.prepareStatement(sql);
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        rows.add(new PendingRequest(
                                rs.getInt("request_id"),
                                rs.getInt("student_id"),
                                rs.getString("field_name"),
                                rs.getString("current_value"),
                                rs.getString("requested_value"),
                                rs.getString("reason"),
                                rs.getString("student_name")
                        ));
                    }
                }

                Platform.runLater(() -> {
                    if (rows.isEmpty()) {
                        pendingBanner.setVisible(false);
                        pendingBanner.setManaged(false);
                        return;
                    }
                    pendingQueue.clear();
                    pendingQueue.addAll(rows);
                    queueIndex = 0;
                    showOverlayItem();
                });

            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private void showOverlayItem() {
        if (queueIndex >= pendingQueue.size()) {
            closeOverlay();
            refreshPendingBanner();
            return;
        }

        PendingRequest r = pendingQueue.get(queueIndex);
        int total = pendingQueue.size();

        overlayCounterLabel.setText((queueIndex + 1) + " of " + total);
        overlayStudentNameLabel.setText(r.studentName());
        overlayFieldLabel.setText(prettyFieldName(r.fieldName()));
        overlayCurrentLabel.setText(r.currentValue() != null ? r.currentValue() : "—");
        overlayRequestedLabel.setText(r.requestedValue());
        overlayReasonArea.setText(r.reason());

        rejectReasonBox.setVisible(false);
        rejectReasonBox.setManaged(false);
        rejectReasonField.clear();
        overlayRejectBtn.setText("Reject");

        reviewOverlay.setVisible(true);
        reviewOverlay.setManaged(true);
    }

    private void closeOverlay() {
        reviewOverlay.setVisible(false);
        reviewOverlay.setManaged(false);
    }

    @FXML
    public void handleOverlaySkip() {
        queueIndex++;
        showOverlayItem();
    }

    @FXML
    public void handleOverlayReject() {
        if (!rejectReasonBox.isVisible()) {
            rejectReasonBox.setVisible(true);
            rejectReasonBox.setManaged(true);
            overlayRejectBtn.setText("Confirm Reject");
        } else {
            String reason = rejectReasonField.getText().trim();
            if (reason.isBlank()) reason = "No reason provided.";
            submitDecision(false, reason);
        }
    }

    @FXML
    public void handleOverlayApprove() {
        submitDecision(true, null);
    }

    @FXML
    public void handleOverlayClose() {
        closeOverlay();
    }

    private void submitDecision(boolean approved, String adminResponse) {
        PendingRequest r = pendingQueue.get(queueIndex);

        Thread t = new Thread(() -> {
            try (Connection conn = DBConnection.connect()) {
                if (conn == null) return;
                updateRequest(conn, r.requestId(), r.studentId(),
                        r.fieldName(), r.requestedValue(), approved, adminResponse);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            Platform.runLater(() -> {
                queueIndex++;
                showOverlayItem();
            });
        });
        t.setDaemon(true);
        t.start();
    }

    private void updateRequest(Connection conn, int requestId, int studentId,
                               String fieldName, String requestedValue,
                               boolean approved, String adminResponse) throws SQLException {
        conn.setAutoCommit(false);
        try {
            try (PreparedStatement ps = conn.prepareStatement("""
                    UPDATE profile_update_requests
                    SET status = ?, admin_response = ?,
                        resolved_at = CURRENT_TIMESTAMP, resolved_by = ?
                    WHERE request_id = ?
                    """)) {
                ps.setString(1, approved ? "APPROVED" : "REJECTED");
                ps.setString(2, adminResponse);
                ps.setInt(3, currentUserId);
                ps.setInt(4, requestId);
                ps.executeUpdate();
            }

            if (approved) {
                String col = sanitizeColumnName(fieldName);
                if (col != null) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE students SET " + col + " = ?, updated_at = CURRENT_TIMESTAMP WHERE student_id = ?")) {
                        ps.setString(1, requestedValue);
                        ps.setInt(2, studentId);
                        ps.executeUpdate();
                    }
                }
            }

            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO audit_log (operator_id, action_type, target_type, target_id, details, performed_at)
                    VALUES (?, ?, 'profile_update_requests', ?, ?::jsonb, NOW() AT TIME ZONE 'Asia/Manila')
                    """)) {
                ps.setInt(1, currentUserId);
                ps.setString(2, approved ? "REQUEST_APPROVED" : "REQUEST_REJECTED");
                ps.setInt(3, requestId);
                ps.setString(4, String.format(
                        "{\"field\":\"%s\",\"request_id\":%d,\"student_id\":%d}",
                        fieldName, requestId, studentId));
                ps.executeUpdate();
            }

            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    private static String sanitizeColumnName(String fieldName) {
        return switch (fieldName.toLowerCase()) {
            case "full_name"      -> "full_name";
            case "course"         -> "course";
            case "year_level"     -> "year_level";
            case "contact_number" -> "contact_number";
            default               -> null;
        };
    }

    private static String prettyFieldName(String fieldName) {
        return switch (fieldName.toLowerCase()) {
            case "full_name"      -> "Full Name";
            case "course"         -> "Course / Program";
            case "year_level"     -> "Year Level";
            case "contact_number" -> "Contact Number";
            default               -> fieldName;
        };
    }

    private void refreshPendingBanner() {
        Thread t = new Thread(() -> {
            try (Connection conn = DBConnection.connect()) {
                if (conn == null) return;
                readPendingRequests(conn);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
        t.setDaemon(true);
        t.start();
    }

}