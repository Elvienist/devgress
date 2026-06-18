package com.example.byodsystem.byod.controller;

import com.example.byodsystem.byod.database.DBConnection;
import com.example.byodsystem.byod.service.UserSession;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import com.example.byodsystem.byod.utils.AlertHelper;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ActivityLogController {

    @FXML private Label lblCurrentDate;
    @FXML private Label lblRoleBadge;
    @FXML private Label lblUserInitial;

    @FXML private Label lblTotalEntries;
    @FXML private Label lblTotalExits;
    @FXML private Label lblUniqueStudents;

    @FXML private TextField txtSearch;
    @FXML private TextField txtDateFilter;
    @FXML private Button    btnFilter;

    @FXML private VBox paneTableRows;

    @FXML private Label lblInspectorLogId;
    @FXML private Label lblInspLoggedTime;
    @FXML private Label lblInspDirection;
    @FXML private Label lblInspStatus;
    @FXML private Label lblInspBatchId;
    @FXML private Label lblInspStudentName;
    @FXML private Label lblInspStudentCode;
    @FXML private Label lblInspCourse;
    @FXML private Label lblInspBrand;
    @FXML private Label lblInspModel;
    @FXML private Label lblInspSerial;
    @FXML private Label lblInspOperator;

    @FXML private ComboBox<String> cmbAmendStatus;
    @FXML private TextArea         txtAmendReason;
    @FXML private Button           btnSubmitAmend;
    @FXML private Label            lblAmendError;

    private int    selectedLogId   = -1;
    private String selectedBatchId = null;

    @FXML
    public void initialize() {
        UserSession session = UserSession.getInstance();
        String role = session.getRole() != null ? session.getRole() : "Admin";
        lblRoleBadge.setText(role.substring(0, 1).toUpperCase() + role.substring(1).toLowerCase());
        String username = session.getUsername();
        lblUserInitial.setText(username != null && !username.isEmpty()
                ? String.valueOf(username.charAt(0)).toUpperCase() : "A");
        lblCurrentDate.setText(LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")));

        cmbAmendStatus.setItems(FXCollections.observableArrayList(
                "NORMAL", "AMENDED", "VOIDED"
        ));
        cmbAmendStatus.getSelectionModel().selectFirst();

        btnSubmitAmend.setDisable(true);
        txtAmendReason.setDisable(true);
        cmbAmendStatus.setDisable(true);

        txtSearch.textProperty().addListener((obs, o, n) -> loadLogs());
        txtDateFilter.textProperty().addListener((obs, o, n) -> loadLogs());

        txtAmendReason.textProperty().addListener((obs, o, n) -> {
            if (n != null && !n.trim().isEmpty()) {
                hideInlineError();
            }
        });

        loadStats();
        loadLogs();
    }

    private void loadStats() {
        String sql = "SELECT " +
                "  COUNT(*) FILTER (WHERE direction = 'IN')  AS total_in, " +
                "  COUNT(*) FILTER (WHERE direction = 'OUT') AS total_out, " +
                "  COUNT(DISTINCT d.owner_id)                AS unique_students " +
                "FROM device_logs dl " +
                "JOIN devices d ON d.device_id = dl.device_id";
        try (Connection conn = DBConnection.connect();
             PreparedStatement pst = conn.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {
            if (rs.next()) {
                lblTotalEntries.setText(String.valueOf(rs.getInt("total_in")));
                lblTotalExits.setText(String.valueOf(rs.getInt("total_out")));
                lblUniqueStudents.setText(String.valueOf(rs.getInt("unique_students")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadLogs() {
        paneTableRows.getChildren().clear();

        String search = txtSearch.getText() == null ? "" : txtSearch.getText().trim();
        String date   = txtDateFilter.getText() == null ? "" : txtDateFilter.getText().trim();

        java.sql.Date sqlDate = null;
        if (!date.isEmpty()) {
            try {
                sqlDate = java.sql.Date.valueOf(date);
            } catch (IllegalArgumentException ex) {
            }
        }

        StringBuilder sql = new StringBuilder(
                "SELECT dl.log_id, dl.log_time, dl.direction, dl.status, dl.batch_id, " +
                        "       s.full_name, s.student_code, s.course, s.year_level, " +
                        "       d.brand, d.model, d.serial_number, " +
                        "       u.username AS operator_name " +
                        "FROM device_logs dl " +
                        "JOIN devices  d ON d.device_id  = dl.device_id " +
                        "JOIN students s ON s.student_id  = d.owner_id " +
                        "JOIN users    u ON u.user_id     = dl.operator_id " +
                        "WHERE 1=1 "
        );

        if (!search.isEmpty()) {
            sql.append("AND (LOWER(s.full_name) LIKE LOWER(?) " +
                    "  OR LOWER(s.student_code) LIKE LOWER(?) " +
                    "  OR LOWER(d.serial_number) LIKE LOWER(?)) ");
        }
        if (sqlDate != null) {
            sql.append("AND CAST(dl.log_time AS DATE) = ? ");
        }
        sql.append("ORDER BY dl.log_time DESC LIMIT 100");

        try (Connection conn = DBConnection.connect();
             PreparedStatement pst = conn.prepareStatement(sql.toString())) {

            int idx = 1;
            if (!search.isEmpty()) {
                String like = "%" + search + "%";
                pst.setString(idx++, like);
                pst.setString(idx++, like);
                pst.setString(idx++, like);
            }
            if (sqlDate != null) {
                pst.setDate(idx, sqlDate);
            }

            try (ResultSet rs = pst.executeQuery()) {
                boolean odd = true;
                while (rs.next()) {
                    int    logId    = rs.getInt("log_id");
                    String ts       = rs.getTimestamp("log_time") != null
                            ? rs.getTimestamp("log_time").toInstant()
                              .atZone(java.time.ZoneId.of("Asia/Manila"))
                              .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "—";
                    String fullName = rs.getString("full_name");
                    String stuCode  = rs.getString("student_code");
                    String course   = rs.getString("course") + " " + rs.getString("year_level");
                    String brand    = rs.getString("brand");
                    String model    = rs.getString("model");
                    String serial   = rs.getString("serial_number");
                    String dir      = rs.getString("direction");
                    String operator = rs.getString("operator_name");
                    String status   = rs.getString("status");
                    String batchId  = rs.getString("batch_id");

                    paneTableRows.getChildren().add(
                            buildRow(logId, ts, fullName, stuCode, course,
                                    brand + " " + model, serial, dir,
                                    operator, status, batchId, odd));
                    odd = !odd;
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private HBox buildRow(int logId, String ts, String fullName, String stuCode,
                          String course, String device, String serial,
                          String dir, String operator, String status,
                          String batchId, boolean odd) {

        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new javafx.geometry.Insets(10, 16, 10, 16));

        String baseStyle = "-fx-background-color: " + (odd ? "#FFFFFF" : "#F3F4F6") + ";" +
                "-fx-border-color: #E5E7EB; -fx-border-width: 0 0 1 0;" +
                "-fx-cursor: hand; -fx-focus-color: transparent;" +
                "-fx-faint-focus-color: transparent;";
        String hoverStyle = "-fx-background-color: #EEF2FF;" +
                "-fx-border-color: #D1D5DB; -fx-border-width: 0 0 1 0;" +
                "-fx-cursor: hand; -fx-focus-color: transparent;" +
                "-fx-faint-focus-color: transparent;";

        row.setStyle(baseStyle);

        Label lblTs = new Label(ts);
        lblTs.setPrefWidth(140);
        lblTs.setMinWidth(140);
        lblTs.setMaxWidth(140);
        lblTs.setWrapText(true);
        lblTs.setStyle("-fx-text-fill: #374151; -fx-font-size: 12px; -fx-font-family: 'System';");

        VBox student = new VBox(2);
        student.setPrefWidth(160);
        student.setMinWidth(160);
        student.setMaxWidth(160);
        Label lblName = new Label(fullName);
        lblName.setWrapText(true);
        lblName.setStyle("-fx-text-fill: #111111; -fx-font-weight: bold; -fx-font-size: 13px; -fx-font-family: 'System';");
        Label lblCode = new Label(stuCode);
        lblCode.setWrapText(true);
        lblCode.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 11px; -fx-font-family: 'System';");
        student.getChildren().addAll(lblName, lblCode);

        Label lblDevice = new Label(device);
        lblDevice.setPrefWidth(160);
        lblDevice.setMinWidth(160);
        lblDevice.setMaxWidth(160);
        lblDevice.setWrapText(true);
        lblDevice.setStyle("-fx-text-fill: #374151; -fx-font-size: 12px; -fx-font-family: 'System';");

        Label lblSerial = new Label(serial);
        lblSerial.setPrefWidth(120);
        lblSerial.setMinWidth(120);
        lblSerial.setMaxWidth(120);
        lblSerial.setWrapText(true);
        lblSerial.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 12px; -fx-font-family: 'System';");

        boolean isIn = "IN".equals(dir);
        Label lblDir = new Label(isIn ? "IN" : "OUT");
        lblDir.setPrefWidth(60);
        lblDir.setMinWidth(60);
        lblDir.setMaxWidth(60);
        lblDir.setAlignment(Pos.CENTER);
        lblDir.setStyle(isIn
                ? "-fx-background-color: #D1FAE5; -fx-text-fill: #065F46; -fx-background-radius: 4;" +
                  "-fx-padding: 2 4; -fx-font-weight: bold; -fx-font-size: 11px; -fx-font-family: 'System';"
                : "-fx-background-color: #CFFAFE; -fx-text-fill: #0E7490; -fx-background-radius: 4;" +
                  "-fx-padding: 2 4; -fx-font-weight: bold; -fx-font-size: 11px; -fx-font-family: 'System';");

        Label lblOp = new Label(operator);
        lblOp.setPrefWidth(130);
        lblOp.setMinWidth(130);
        lblOp.setMaxWidth(130);
        lblOp.setWrapText(true);
        lblOp.setStyle("-fx-text-fill: #374151; -fx-font-size: 12px; -fx-font-family: 'System';");

        Label lblStatus = buildStatusBadge(status);
        lblStatus.setPrefWidth(90);
        lblStatus.setMinWidth(90);
        lblStatus.setMaxWidth(90);

        row.getChildren().addAll(lblTs, student, lblDevice, lblSerial, lblDir, lblOp, lblStatus);

        row.setOnMouseClicked(e -> {
            selectLog(logId, ts, dir, status, batchId,
                    fullName, stuCode, course, device, serial, operator);
            row.setStyle(baseStyle);
        });

        row.setOnMouseEntered(e -> row.setStyle(hoverStyle));
        row.setOnMouseExited(e -> row.setStyle(baseStyle));
        row.setFocusTraversable(false);

        return row;
    }

    private Label buildStatusBadge(String status) {
        Label lbl = new Label(status != null ? status : "—");
        lbl.setAlignment(Pos.CENTER);
        switch (status == null ? "" : status.toUpperCase()) {
            case "NORMAL"  -> lbl.setStyle("-fx-background-color: #F3F4F6; -fx-text-fill: #374151; -fx-background-radius: 4; -fx-padding: 2 8; -fx-font-size: 11px; -fx-border-color: #D1D5DB; -fx-border-radius: 4;");
            case "AMENDED" -> lbl.setStyle("-fx-background-color: #FEF3C7; -fx-text-fill: #92400E; -fx-background-radius: 4; -fx-padding: 2 8; -fx-font-size: 11px; -fx-font-weight: bold;");
            case "VOIDED"  -> lbl.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #991B1B; -fx-background-radius: 4; -fx-padding: 2 8; -fx-font-size: 11px; -fx-font-weight: bold;");
            default        -> lbl.setStyle("-fx-background-color: #F3F4F6; -fx-text-fill: #6B7280; -fx-background-radius: 4; -fx-padding: 2 8; -fx-font-size: 11px;");
        }
        return lbl;
    }

    private void selectLog(int logId, String ts, String dir, String status,
                           String batchId, String fullName, String stuCode,
                           String course, String device, String serial,
                           String operator) {

        selectedLogId   = logId;
        selectedBatchId = batchId;

        lblInspectorLogId.setText("#" + String.format("%05d", logId));
        lblInspLoggedTime.setText(ts);
        lblInspDirection.setText(dir);
        lblInspStatus.setText(status != null ? status : "—");
        lblInspBatchId.setText(batchId != null ? batchId : "—");
        lblInspStudentName.setText(fullName);
        lblInspStudentCode.setText(stuCode);
        lblInspCourse.setText(course);

        String[] parts = device.split(" ", 2);
        lblInspBrand.setText(parts.length > 0 ? parts[0] : "—");
        lblInspModel.setText(parts.length > 1 ? parts[1] : "—");
        lblInspSerial.setText(serial);
        lblInspOperator.setText(operator);

        cmbAmendStatus.getSelectionModel().select(status != null ? status : "NORMAL");
        txtAmendReason.clear();
        hideInlineError();

        btnSubmitAmend.setDisable(false);
        txtAmendReason.setDisable(false);
        cmbAmendStatus.setDisable(false);
    }

    @FXML
    public void handleFilter() {
        loadLogs();
    }

    @FXML
    public void handleClearSelection() {
        selectedLogId = -1;
        selectedBatchId = null;
        resetInspector();
    }

    private void showInlineError() {
        if (lblAmendError != null) {
            lblAmendError.setVisible(true);
            lblAmendError.setManaged(true);
        }
    }

    private void hideInlineError() {
        if (lblAmendError != null) {
            lblAmendError.setVisible(false);
            lblAmendError.setManaged(false);
        }
    }

    @FXML
    public void handleSubmitAmend() {
        if (selectedLogId < 0) {
            AlertHelper.showNegative(lblAmendError.getScene().getWindow(), "No Selection", "Please select a log entry first. ");
            return;
        }

        String newStatus = cmbAmendStatus.getValue();
        String reason    = txtAmendReason.getText() == null ? "" : txtAmendReason.getText().trim();

        if (reason.isEmpty()) {
            showInlineError();
            return;
        } else {
            hideInlineError();
        }

        String previousStatus = lblInspStatus.getText();

        String snapshotSql =
                "SELECT dl.log_id, dl.log_time, dl.direction, dl.status, dl.batch_id, " +
                        "       d.serial_number, d.brand, d.model, " +
                        "       s.full_name, s.student_code " +
                        "FROM device_logs dl " +
                        "JOIN devices  d ON d.device_id  = dl.device_id " +
                        "JOIN students s ON s.student_id  = d.owner_id " +
                        "WHERE dl.log_id = ?";

        String previousDataJson = null;
        try (Connection conn = DBConnection.connect();
             PreparedStatement pst = conn.prepareStatement(snapshotSql)) {
            pst.setInt(1, selectedLogId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    previousDataJson = String.format(
                            "{\"log_id\":%d,\"log_time\":\"%s\",\"direction\":\"%s\"," +
                                    "\"status\":\"%s\",\"batch_id\":\"%s\"," +
                                    "\"serial_number\":\"%s\",\"brand\":\"%s\",\"model\":\"%s\"," +
                                    "\"full_name\":\"%s\",\"student_code\":\"%s\"}",
                            rs.getInt("log_id"),
                            rs.getTimestamp("log_time"),
                            rs.getString("direction"),
                            rs.getString("status"),
                            rs.getString("batch_id"),
                            rs.getString("serial_number"),
                            rs.getString("brand"),
                            rs.getString("model"),
                            rs.getString("full_name"),
                            rs.getString("student_code")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            AlertHelper.showNegative(lblAmendError.getScene().getWindow(), "Error", "Failed to read original log data. ");
            return;
        }

        String amendType = "VOIDED".equals(newStatus) ? "VOID" : "EDIT";

        try (Connection conn = DBConnection.connect()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement pst = conn.prepareStatement(
                        "UPDATE device_logs SET status = ? WHERE log_id = ?")) {
                    pst.setString(1, newStatus);
                    pst.setInt(2, selectedLogId);
                    pst.executeUpdate();
                }

                try (PreparedStatement pst = conn.prepareStatement(
                        "INSERT INTO log_amendments " +
                                "(original_log_id, amendment_type, changed_by, reason, previous_data) " +
                                "VALUES (?, ?, ?, ?, ?::jsonb)")) {
                    pst.setInt(1, selectedLogId);
                    pst.setString(2, amendType);
                    pst.setInt(3, UserSession.getInstance().getUserId());
                    pst.setString(4, reason);
                    pst.setString(5, previousDataJson);
                    pst.executeUpdate();
                }

                try (PreparedStatement pst = conn.prepareStatement(
                        "INSERT INTO audit_log " +
                                "(operator_id, action_type, target_type, target_id, details, performed_at) " +
                                "VALUES (?, ?, 'device_logs', ?, ?::jsonb, NOW() AT TIME ZONE 'Asia/Manila')")) {
                    pst.setInt(1, UserSession.getInstance().getUserId());
                    pst.setString(2, "VOIDED".equals(newStatus) ? "VOID_MADE" : "CORRECTION_MADE");
                    pst.setInt(3, selectedLogId);
                    pst.setString(4, String.format(
                            "{\"log_id\":%d,\"previous_status\":\"%s\"," +
                                    "\"new_status\":\"%s\",\"reason\":\"%s\",\"batch_id\":\"%s\"}",
                            selectedLogId,
                            previousStatus.replace("\"", "'"),
                            newStatus,
                            reason.replace("\"", "'"),
                            selectedBatchId != null ? selectedBatchId : ""
                    ));
                    pst.executeUpdate();
                }

                conn.commit();

            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
                AlertHelper.showNegative(lblAmendError.getScene().getWindow(), "Error", "Amendment failed. Transaction rolled back. ");
                return;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            AlertHelper.showNegative(lblAmendError.getScene().getWindow(), "Error", "Database connnection failed. ");
            return;
        }

        AlertHelper.showPositive(lblAmendError.getScene().getWindow(), "Success", "Log #" + selectedLogId + " status updated to " + newStatus + ".");

        selectedLogId   = -1;
        selectedBatchId = null;
        resetInspector();
        loadStats();
        loadLogs();
    }

    private void resetInspector() {
        lblInspectorLogId.setText("#00000");
        lblInspLoggedTime.setText("---");
        lblInspDirection.setText("---");
        lblInspStatus.setText("---");
        lblInspBatchId.setText("---");
        lblInspStudentName.setText("---");
        lblInspStudentCode.setText("---");
        lblInspCourse.setText("---");
        lblInspBrand.setText("---");
        lblInspModel.setText("---");
        lblInspSerial.setText("---");
        lblInspOperator.setText("---");
        txtAmendReason.clear();
        hideInlineError();
        cmbAmendStatus.getSelectionModel().selectFirst();
        btnSubmitAmend.setDisable(true);
        txtAmendReason.setDisable(true);
        cmbAmendStatus.setDisable(true);
    }
}