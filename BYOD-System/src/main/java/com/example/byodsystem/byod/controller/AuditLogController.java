package com.example.byodsystem.byod.controller;

import com.example.byodsystem.byod.database.DBConnection;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import com.example.byodsystem.byod.utils.AlertHelper;
import javafx.stage.Window;

import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class AuditLogController implements Initializable {

    @FXML private Label            lblDate;
    @FXML private TextField        txtSearch;
    @FXML private ComboBox<String> cmbActionFilter;
    @FXML private DatePicker       dpStartDate;
    @FXML private DatePicker       dpEndDate;
    @FXML private Button           btnClearDates;

    @FXML private TableView<AuditRow>           tblAuditLog;
    @FXML private TableColumn<AuditRow, String> colTimestamp;
    @FXML private TableColumn<AuditRow, String> colOperator;
    @FXML private TableColumn<AuditRow, String> colRole;
    @FXML private TableColumn<AuditRow, String> colAction;
    @FXML private TableColumn<AuditRow, String> colRecordType;
    @FXML private TableColumn<AuditRow, String> colIdentifier;
    private final ObservableList<AuditRow> allRows = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setDateLabel();
        setupActionFilter();
        setupDatePickers();
        setupTable();
        loadAuditLog();
    }

    private void setDateLabel() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");
        lblDate.setText(LocalDate.now(ZoneId.of("Asia/Manila")).format(fmt));
    }

    private void setupActionFilter() {
        cmbActionFilter.setItems(FXCollections.observableArrayList(
                "All Actions",
                "LOGIN", "LOGOUT",
                "RECORD_CREATED", "RECORD_EDITED", "RECORD_DELETED",
                "RECORD_DEACTIVATED", "RECORD_REACTIVATED",
                "REPORT_GENERATED", "CORRECTION_MADE", "VOID_MADE",
                "PASSWORD_CHANGED", "PASSWORD_RESET",
                "REQUEST_APPROVED", "REQUEST_REJECTED",
                "SETTINGS_CHANGED"
        ));
        cmbActionFilter.getSelectionModel().selectFirst();
    }

    private void setupDatePickers() {
        disableTyping(dpStartDate);
        disableTyping(dpEndDate);
    }

    private void disableTyping(DatePicker dp) {
        dp.getEditor().addEventFilter(KeyEvent.ANY, KeyEvent::consume);
        dp.getEditor().setStyle("-fx-cursor: default;");
        dp.setEditable(false);
    }

    private void setupTable() {
        colTimestamp.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTimestamp()));
        colTimestamp.setCellFactory(col -> centeredCell(
                "-fx-font-family: 'Courier New', monospace; -fx-font-size: 12px; -fx-text-fill: #4a4a4a;"));

        colOperator.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getOperatorName()));
        colOperator.setCellFactory(col -> centeredCell(
                "-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1a1a1a;"));

        colRole.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getRole()));
        colRole.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(Pos.CENTER);
                if (empty || item == null) { setText(null); setGraphic(null); return; }

                Label badge = new Label(item);
                String bgColor, textColor;
                switch (item) {
                    case "ADMIN":
                        bgColor   = "#fde8e8";
                        textColor = "#8B0000";
                        break;
                    case "OFFICER":
                        bgColor   = "#fef3e2";
                        textColor = "#b85c00";
                        break;
                    default:
                        bgColor   = "#e6f4ec";
                        textColor = "#1e6b3a";
                        break;
                }

                badge.setStyle(
                        "-fx-background-color: " + bgColor + ";" +
                                "-fx-text-fill: " + textColor + ";" +
                                "-fx-font-size: 11px;" +
                                "-fx-font-weight: bold;" +
                                "-fx-padding: 3 10;" +
                                "-fx-background-radius: 4;"
                );
                setText(null);
                setGraphic(badge);
            }
        });

        colAction.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getActionType()));
        colAction.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(Pos.CENTER);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(formatAction(item));
                setStyle("-fx-font-size: 13px; -fx-text-fill: #2d2d2d;");
            }
            private String formatAction(String raw) {
                String[] words = raw.replace("_", " ").toLowerCase().split(" ");
                StringBuilder sb = new StringBuilder();
                for (String word : words) {
                    if (!word.isEmpty()) {
                        if (sb.length() > 0) sb.append(" ");
                        sb.append(Character.toUpperCase(word.charAt(0)));
                        sb.append(word.substring(1));
                    }
                }
                return sb.toString();
            }
        });

        colRecordType.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTargetType()));
        colRecordType.setCellFactory(col -> centeredCell(
                "-fx-font-size: 13px; -fx-text-fill: #2d2d2d;"));

        colIdentifier.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getIdentifier()));
        colIdentifier.setCellFactory(col -> centeredCell(
                "-fx-font-family: 'Courier New', monospace; -fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #1a1a1a;"));

        tblAuditLog.setRowFactory(tv -> {
            TableRow<AuditRow> row = new TableRow<>();
            row.setPrefHeight(48);
            row.setStyle("-fx-border-color: transparent transparent #f0f0f0 transparent; -fx-padding: 0;");
            return row;
        });
    }

    private TableCell<AuditRow, String> centeredCell(String inlineStyle) {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(Pos.CENTER);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle(inlineStyle);
                }
            }
        };
    }

    private void loadAuditLog() {
        applyFilter();
    }

    private String buildIdentifier(Connection conn, String targetType, Integer targetId) {
        if (targetType == null || targetId == null) return "";
        try {
            String sql;
            switch (targetType) {
                case "Student":   sql = "SELECT student_code  FROM students    WHERE student_id = ?"; break;
                case "Device":    sql = "SELECT serial_number FROM devices     WHERE device_id  = ?"; break;
                case "User":      sql = "SELECT username       FROM users       WHERE user_id    = ?"; break;
                case "Log Entry": sql = "SELECT CAST(log_id AS VARCHAR) FROM device_logs WHERE log_id = ?"; break;
                case "Report":    sql = "SELECT report_title   FROM reports     WHERE report_id  = ?"; break;
                default:          return String.valueOf(targetId);
            }
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, targetId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) return rs.getString(1);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return String.valueOf(targetId);
    }

    private String extractDetails(String jsonDetails) {
        if (jsonDetails == null || jsonDetails.isBlank()) return "";
        jsonDetails = jsonDetails.replaceAll("[{}\"]", "").trim();
        if (jsonDetails.contains("note")) {
            int idx = jsonDetails.indexOf("note:");
            if (idx >= 0) return jsonDetails.substring(idx + 5).split(",")[0].trim();
        }
        String[] parts = jsonDetails.split(",");
        if (parts.length > 0) {
            String first = parts[0].trim();
            if (first.contains(":")) return first.substring(first.indexOf(":") + 1).trim();
            return first;
        }
        return jsonDetails;
    }

    @FXML private void onSearchKeyReleased() { applyFilter(); }
    @FXML private void onFilterChanged()     { applyFilter(); }

    @FXML
    private void onClearDates() {
        dpStartDate.setValue(null);
        dpEndDate.setValue(null);
        txtSearch.clear();
        cmbActionFilter.getSelectionModel().selectFirst();
        applyFilter();
    }

    private void applyFilter() {
        allRows.clear();

        String search = txtSearch.getText().toLowerCase().trim();
        String action = cmbActionFilter.getValue();
        LocalDate start = dpStartDate.getValue();
        LocalDate end = dpEndDate.getValue();

        boolean hasFilter = !search.isBlank()
                || (action != null && !action.equals("All Actions"))
                || start != null
                || end != null;

        StringBuilder sql = new StringBuilder(
                "SELECT a.audit_id, " +
                        "TO_CHAR(a.performed_at AT TIME ZONE 'Asia/Manila', 'YYYY-MM-DD HH24:MI:SS') AS ts, " +
                        "u.full_name, u.role, a.action_type, a.target_type, a.target_id, a.details " +
                        "FROM audit_log a " +
                        "JOIN users u ON a.operator_id = u.user_id WHERE 1=1 "
        );

        if (action != null && !action.equals("All Actions")) {
            sql.append("AND UPPER(a.action_type) = ? ");
        }
        if (start != null) {
            sql.append("AND a.performed_at AT TIME ZONE 'Asia/Manila' >= ? ");
        }
        if (end != null) {
            sql.append("AND a.performed_at AT TIME ZONE 'Asia/Manila' <= ? ");
        }

        sql.append("ORDER BY a.performed_at DESC ");

        if (!hasFilter) {
            sql.append("LIMIT 75 ");
        }

        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            int paramIdx = 1;
            if (action != null && !action.equals("All Actions")) {
                ps.setString(paramIdx++, action.toUpperCase());
            }
            if (start != null) {
                ps.setTimestamp(paramIdx++, Timestamp.valueOf(start.atStartOfDay()));
            }
            if (end != null) {
                ps.setTimestamp(paramIdx++, Timestamp.valueOf(end.atTime(23, 59, 59)));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String targetType = rs.getString("target_type");
                    Integer targetId = rs.getObject("target_id") != null ? rs.getInt("target_id") : null;
                    String identifier = buildIdentifier(conn, targetType, targetId);
                    String details = extractDetails(rs.getString("details"));

                    AuditRow row = new AuditRow(
                            rs.getInt("audit_id"),
                            rs.getString("ts"),
                            rs.getString("full_name"),
                            rs.getString("role"),
                            rs.getString("action_type"),
                            targetType != null ? targetType : "",
                            identifier,
                            details
                    );

                    if (!search.isBlank()) {
                        boolean matchSearch = row.getTimestamp().toLowerCase().contains(search)
                                || row.getOperatorName().toLowerCase().contains(search)
                                || row.getRole().toLowerCase().contains(search)
                                || row.getActionType().toLowerCase().contains(search)
                                || row.getTargetType().toLowerCase().contains(search)
                                || row.getIdentifier().toLowerCase().contains(search)
                                || row.getDetails().toLowerCase().contains(search);
                        if (matchSearch) {
                            allRows.add(row);
                        }
                    } else {
                        allRows.add(row);
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            AlertHelper.showNegative(btnClearDates.getScene().getWindow(), "Database Error", "Failed to load audit log: " + e.getMessage());
        }

        tblAuditLog.setItems(allRows);
    }

    public static class AuditRow {
        private final int    auditId;
        private final String timestamp;
        private final String operatorName;
        private final String role;
        private final String actionType;
        private final String targetType;
        private final String identifier;
        private final String details;

        public AuditRow(int auditId, String timestamp, String operatorName, String role,
                        String actionType, String targetType, String identifier, String details) {
            this.auditId      = auditId;
            this.timestamp    = timestamp;
            this.operatorName = operatorName;
            this.role         = role;
            this.actionType   = actionType;
            this.targetType   = targetType;
            this.identifier   = identifier;
            this.details      = details;
        }

        public int    getAuditId()      { return auditId;      }
        public String getTimestamp()    { return timestamp;     }
        public String getOperatorName() { return operatorName;  }
        public String getRole()         { return role;          }
        public String getActionType()   { return actionType;    }
        public String getTargetType()   { return targetType;    }
        public String getIdentifier()   { return identifier;    }
        public String getDetails()      { return details;       }
    }
}