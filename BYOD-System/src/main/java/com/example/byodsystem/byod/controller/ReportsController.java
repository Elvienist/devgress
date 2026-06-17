package com.example.byodsystem.byod.controller;

import com.example.byodsystem.byod.service.AuditLogger;
import com.example.byodsystem.byod.service.UserSession;
import com.example.byodsystem.byod.database.DBConnection;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class ReportsController implements Initializable {

    @FXML private Label lblDate;
    @FXML private Label lblTypeError;
    @FXML private Label lblDateError;
    @FXML private Label lblGlobalError;
    @FXML private ComboBox<String> cmbReportType;
    @FXML private DatePicker dpDateStart;
    @FXML private DatePicker dpDateEnd;
    @FXML private Button btnGenerate;
    @FXML private TableView<ReportRow> tblReports;

    private final int currentUserId = UserSession.getInstance().getUserId();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setDateLabel();
        setupComboBox();
        setupTable();
        loadReports();

        dpDateStart.setEditable(false);
        dpDateEnd.setEditable(false);
    }

    private void setDateLabel() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");
        lblDate.setText(LocalDate.now(ZoneId.of("Asia/Manila")).format(fmt));
    }

    private void setupComboBox() {
        cmbReportType.setItems(FXCollections.observableArrayList(
                "Daily Summary",
                "Device Stats",
                "Activity Report",
                "User Activity",
                "Student Report"
        ));
    }

    @SuppressWarnings("unchecked")
    private void setupTable() {
        TableColumn<ReportRow, String> titleCol = (TableColumn<ReportRow, String>) tblReports.getColumns().get(0);
        TableColumn<ReportRow, String> typeCol = (TableColumn<ReportRow, String>) tblReports.getColumns().get(1);
        TableColumn<ReportRow, String> dateRangeCol = (TableColumn<ReportRow, String>) tblReports.getColumns().get(2);
        TableColumn<ReportRow, Integer> recordsCol = (TableColumn<ReportRow, Integer>) tblReports.getColumns().get(3);
        TableColumn<ReportRow, String> generatedCol = (TableColumn<ReportRow, String>) tblReports.getColumns().get(4);
        TableColumn<ReportRow, Void> exportCol = (TableColumn<ReportRow, Void>) tblReports.getColumns().get(5);

        titleCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getReportTitle()));
        titleCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #212529; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 12 10; -fx-alignment: CENTER;");
                }
            }
        });

        typeCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getReportType()));
        typeCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label label = new Label(item.toUpperCase());
                    label.setStyle("-fx-background-color: #E9ECEF; -fx-text-fill: #495057; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 4;");
                    HBox box = new HBox(label);
                    box.setAlignment(Pos.CENTER);
                    setGraphic(box);
                    setStyle("-fx-padding: 12 10; -fx-alignment: CENTER;");
                }
            }
        });

        dateRangeCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDateRange()));
        dateRangeCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setStyle(empty ? "" : "-fx-text-fill: #495057; -fx-font-size: 13px; -fx-padding: 12 10; -fx-alignment: CENTER;");
            }
        });

        recordsCol.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getTotalRecords()).asObject());
        recordsCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%,d", item));
                    setStyle("-fx-text-fill: #212529; -fx-font-size: 13px; -fx-padding: 12 10; -fx-alignment: CENTER;");
                }
            }
        });

        generatedCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCreatedAt()));
        generatedCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setStyle(empty ? "" : "-fx-text-fill: #495057; -fx-font-size: 13px; -fx-padding: 12 10; -fx-alignment: CENTER;");
            }
        });

        exportCol.setCellFactory(col -> new TableCell<>() {
            private final Button btnDownload = new Button("📥");
            {
                btnDownload.setStyle(
                        "-fx-background-color: transparent; " +
                                "-fx-background-radius: 0; " +
                                "-fx-padding: 0; " +
                                "-fx-text-fill: #7A0000; " +
                                "-fx-font-size: 16px; " +
                                "-fx-cursor: hand;"
                );

                btnDownload.setOnMouseEntered(e -> btnDownload.setStyle(
                        "-fx-background-color: transparent; -fx-background-radius: 0; -fx-padding: 0; -fx-text-fill: #A30000; -fx-font-size: 16px; -fx-cursor: hand;"
                ));
                btnDownload.setOnMouseExited(e -> btnDownload.setStyle(
                        "-fx-background-color: transparent; -fx-background-radius: 0; -fx-padding: 0; -fx-text-fill: #7A0000; -fx-font-size: 16px; -fx-cursor: hand;"
                ));

                btnDownload.setOnAction(e -> {
                    ReportRow row = getTableView().getItems().get(getIndex());
                    exportReport(row);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox box = new HBox(btnDownload);
                    box.setAlignment(Pos.CENTER);
                    setGraphic(box);
                    setStyle("-fx-padding: 12 10; -fx-alignment: CENTER;");
                }
            }
        });

        tblReports.setRowFactory(tv -> {
            TableRow<ReportRow> row = new TableRow<>();
            row.setStyle("-fx-background-color: white; -fx-border-color: transparent transparent #E9ECEF transparent; -fx-border-width: 0 0 1 0;");
            return row;
        });
    }

    private void loadReports() {
        ObservableList<ReportRow> list = FXCollections.observableArrayList();
        String sql = "SELECT report_id, report_title, report_type, date_start, date_end, total_records, " +
                "TO_CHAR(created_at, 'YYYY-MM-DD HH24:MI') AS ts " +
                "FROM reports ORDER BY created_at DESC";
        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String dateRange = rs.getDate("date_start") + " to " + rs.getDate("date_end");
                String createdAt = rs.getString("ts");
                if (createdAt == null) createdAt = "";

                list.add(new ReportRow(rs.getInt("report_id"), rs.getString("report_title"),
                        rs.getString("report_type"), dateRange, rs.getInt("total_records"), createdAt));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showGlobalError("Database Connection Fault: Failed to load report history archives.");
        }
        tblReports.setItems(list);
    }

    private void clearInlineErrors() {
        toggleErrorLabel(lblTypeError, "", false);
        toggleErrorLabel(lblDateError, "", false);
        toggleErrorLabel(lblGlobalError, "", false);
    }

    private void toggleErrorLabel(Label label, String msg, boolean show) {
        label.setText(msg);
        label.setVisible(show);
        label.setManaged(show);
    }

    private void showGlobalError(String msg) { toggleErrorLabel(lblGlobalError, msg, true); }

    @FXML
    private void generateReport() {
        clearInlineErrors();
        String type = cmbReportType.getValue();
        LocalDate start = dpDateStart.getValue();
        LocalDate end = dpDateEnd.getValue();
        boolean failed = false;

        if (type == null || type.isBlank()) {
            toggleErrorLabel(lblTypeError, "⚠ Field Required: Please specify a report type category selection.", true);
            failed = true;
        }
        if (start == null || end == null) {
            toggleErrorLabel(lblDateError, "⚠ Field Required: Complete start and end parameters matching date bounds must be set.", true);
            failed = true;
        } else if (end.isBefore(start)) {
            toggleErrorLabel(lblDateError, "⚠ Constraint Violation: Selected end boundary coordinate cannot precede initial start window.", true);
            failed = true;
        }

        if (failed) return;

        int totalRecords = countRecords(type, start, end);
        String title = type + " - " + start.format(DateTimeFormatter.ofPattern("MMM yyyy"));
        String insertSql = "INSERT INTO reports (report_title, report_type, generated_by, date_start, date_end, total_records, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, NOW()) RETURNING report_id";

        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(insertSql)) {
            ps.setString(1, title);
            ps.setString(2, type);
            ps.setInt(3, currentUserId);
            ps.setDate(4, Date.valueOf(start));
            ps.setDate(5, Date.valueOf(end));
            ps.setInt(6, totalRecords);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                AuditLogger.log(conn, currentUserId, "REPORT_GENERATED", "Report", rs.getInt(1),
                        "{\"report_type\":\"" + type + "\",\"date_start\":\"" + start + "\"}");
            }
            loadReports();
        } catch (SQLException e) {
            e.printStackTrace();
            showGlobalError("Write Failure: Unable to save generated calculations to database storage.");
        }
    }

    private int countRecords(String type, LocalDate start, LocalDate end) {
        String sql = switch (type) {
            case "Daily Summary", "Activity Report" -> "SELECT COUNT(*) FROM device_logs WHERE DATE(log_time AT TIME ZONE 'Asia/Manila') BETWEEN ? AND ?";
            case "Device Stats" -> "SELECT COUNT(*) FROM devices WHERE DATE(created_at AT TIME ZONE 'Asia/Manila') BETWEEN ? AND ?";
            case "User Activity" -> "SELECT COUNT(*) FROM audit_log WHERE DATE(performed_at AT TIME ZONE 'Asia/Manila') BETWEEN ? AND ?";
            case "Student Report" -> "SELECT COUNT(*) FROM students WHERE DATE(created_at AT TIME ZONE 'Asia/Manila') BETWEEN ? AND ?";
            default -> null;
        };

        if (sql == null) return 0;
        try (Connection conn = DBConnection.connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(start));
            ps.setDate(2, Date.valueOf(end));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    private void exportReport(ReportRow row) {
        clearInlineErrors();
        FileChooser fc = new FileChooser();
        fc.setTitle("Export System Sheet Document");
        fc.setInitialFileName(row.getReportTitle().replaceAll("[^a-zA-Z0-9\\-_ ]", "_") + ".csv");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Sheet Document Document (*.csv)", "*.csv"));
        File file = fc.showSaveDialog(tblReports.getScene().getWindow());
        if (file == null) return;

        String[] dateParts = row.getDateRange().split(" to ");
        if (dateParts.length != 2) {
            showGlobalError("Extraction Fault: Target rows parsed display corrupt timestamp metadata boundaries.");
            return;
        }

        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"))) {
            pw.println("==========================================================================================");
            pw.println("                           CAMPUS BYOD MANAGEMENT SYSTEM ARCHIVE LOG                      ");
            pw.println("==========================================================================================");
            pw.println("DOCUMENT DETAILS & METADATA OVERVIEW");
            pw.println("Report Title  : " + row.getReportTitle());
            pw.println("Report Type   : " + row.getReportType().toUpperCase());
            pw.println("Date Boundary : " + row.getDateRange());
            pw.println("Generated At  : \t" + row.getCreatedAt());
            pw.println("Total Rows    : " + row.getTotalRecords());
            pw.println("------------------------------------------------------------------------------------------");
            pw.println();

            writeReportData(pw, row.getReportType(), dateParts[0].trim(), dateParts[1].trim());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            showGlobalError("Access Denied: The file is currently open in Excel or another program. Close it and try again.");
        } catch (IOException e) {
            e.printStackTrace();
            showGlobalError("System OS File Stream Error: Failed to write data records to local disk.");
        }
    }

    private void writeReportData(PrintWriter pw, String type, String startDate, String endDate) {
        String sql = switch (type) {
            case "Daily Summary", "Activity Report" ->
                    "SELECT dl.log_id, d.serial_number, d.brand, d.model, s.student_code, s.full_name, dl.direction, " +
                            "TO_CHAR(dl.log_time AT TIME ZONE 'Asia/Manila', 'YYYY-MM-DD HH24:MI:SS') AS formatted_log_time, dl.status " +
                            "FROM device_logs dl JOIN devices d ON dl.device_id = d.device_id JOIN students s ON d.owner_id = s.student_id " +
                            "WHERE DATE(dl.log_time AT TIME ZONE 'Asia/Manila') BETWEEN ? AND ? ORDER BY dl.log_time DESC";
            case "Device Stats" ->
                    "SELECT d.device_id, d.serial_number, d.brand, d.model, d.device_type, s.student_code, s.full_name, d.status, " +
                            "TO_CHAR(d.created_at AT TIME ZONE 'Asia/Manila', 'YYYY-MM-DD HH24:MI:SS') AS formatted_log_time " +
                            "FROM devices d JOIN students s ON d.owner_id = s.student_id WHERE DATE(d.created_at AT TIME ZONE 'Asia/Manila') BETWEEN ? AND ? ORDER BY d.created_at DESC";
            case "User Activity" ->
                    "SELECT a.audit_id, u.username, u.role, a.action_type, a.target_type, a.target_id, " +
                            "TO_CHAR(a.performed_at AT TIME ZONE 'Asia/Manila', 'YYYY-MM-DD HH24:MI:SS') AS formatted_log_time " +
                            "FROM audit_log a JOIN users u ON a.operator_id = u.user_id WHERE DATE(a.performed_at AT TIME ZONE 'Asia/Manila') BETWEEN ? AND ? ORDER BY a.performed_at DESC";
            case "Student Report" ->
                    "SELECT s.student_id, s.student_code, s.full_name, s.course, s.year_level, s.contact_number, s.status, " +
                            "TO_CHAR(s.created_at AT TIME ZONE 'Asia/Manila', 'YYYY-MM-DD HH24:MI:SS') AS formatted_log_time " +
                            "FROM students s WHERE DATE(s.created_at AT TIME ZONE 'Asia/Manila') BETWEEN ? AND ? ORDER BY s.created_at DESC";
            default -> null;
        };

        if (sql == null) return;

        try (Connection conn = DBConnection.connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(startDate));
            ps.setDate(2, Date.valueOf(endDate));
            ResultSet rs = ps.executeQuery();

            if (type.equals("Daily Summary") || type.equals("Activity Report")) {
                pw.println("Log ID,Serial Number,Brand,Model,Student Code,Student Name,Direction,Log Time,Status");
                while (rs.next()) {
                    pw.printf("%d,%s,%s,%s,%s,%s,%s,\t%s,%s%n", rs.getInt("log_id"), sanitizeCsv(rs.getString("serial_number")),
                            sanitizeCsv(rs.getString("brand")), sanitizeCsv(rs.getString("model")), sanitizeCsv(rs.getString("student_code")),
                            sanitizeCsv(rs.getString("full_name")), rs.getString("direction"), rs.getString("formatted_log_time"), rs.getString("status"));
                }
            } else if (type.equals("Device Stats")) {
                pw.println("Device ID,Serial Number,Brand,Model,Type,Student Code,Owner,Status,Registered At");
                while (rs.next()) {
                    pw.printf("%d,%s,%s,%s,%s,%s,%s,%s,\t%s%n", rs.getInt("device_id"), sanitizeCsv(rs.getString("serial_number")),
                            sanitizeCsv(rs.getString("brand")), sanitizeCsv(rs.getString("model")), rs.getString("device_type"),
                            sanitizeCsv(rs.getString("student_code")), sanitizeCsv(rs.getString("full_name")), rs.getString("status"), rs.getString("formatted_log_time"));
                }
            } else if (type.equals("User Activity")) {
                pw.println("Audit ID,Username,Role,Action,Target Type,Target ID,Performed At");
                while (rs.next()) {
                    pw.printf("%d,%s,%s,%s,%s,%s,\t%s%n", rs.getInt("audit_id"), sanitizeCsv(rs.getString("username")),
                            rs.getString("role"), rs.getString("action_type"), rs.getString("target_type"), rs.getString("target_id"), rs.getString("formatted_log_time"));
                }
            } else if (type.equals("Student Report")) {
                pw.println("Student ID,Student Code,Full Name,Course,Year Level,Contact,Status,Registered At");
                while (rs.next()) {
                    pw.printf("%d,%s,%s,%s,%s,%s,%s,\t%s%n", rs.getInt("student_id"), sanitizeCsv(rs.getString("student_code")),
                            sanitizeCsv(rs.getString("full_name")), sanitizeCsv(rs.getString("course")), sanitizeCsv(rs.getString("year_level")),
                            sanitizeCsv(rs.getString("contact_number")), rs.getString("status"), rs.getString("formatted_log_time"));
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private String sanitizeCsv(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n") || val.contains("\r")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }

    private void navigate(String fxml) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            Stage stage = (Stage) tblReports.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML private void navigateToDashboard() { navigate("/com/example/byodsystem/byod/dashboard.fxml"); }
    @FXML private void navigateToStudents() { navigate("/com/example/byodsystem/byod/StudentsDevices.fxml"); }
    @FXML private void navigateToGate() { navigate("/com/example/byodsystem/byod/GateScreen.fxml"); }
    @FXML private void navigateToActivity() { navigate("/com/example/byodsystem/byod/ActivityLog.fxml"); }
    @FXML private void navigateToUserManagement() { navigate("/com/example/byodsystem/byod/UserManagement.fxml"); }
    @FXML private void navigateToProfileRequests() { navigate("/com/example/byodsystem/byod/ProfileRequests.fxml"); }
    @FXML private void navigateToReports() { navigate("/com/example/byodsystem/byod/Reports.fxml"); }
    @FXML private void navigateToAuditLog() { navigate("/com/example/byodsystem/byod/AuditLog.fxml"); }
    @FXML private void navigateToSettings() { navigate("/com/example/byodsystem/byod/Settings.fxml"); }
    @FXML private void changePassword() { navigate("/com/example/byodsystem/byod/ChangePassword.fxml"); }
    @FXML private void logout() { navigate("/com/example/byodsystem/byod/Login.fxml"); }

    public static class ReportRow {
        private final int reportId;
        private final String reportTitle;
        private final String reportType;
        private final String dateRange;
        private final int totalRecords;
        private final String createdAt;

        public ReportRow(int reportId, String reportTitle, String reportType, String dateRange, int totalRecords, String createdAt) {
            this.reportId = reportId;
            this.reportTitle = reportTitle;
            this.reportType = reportType;
            this.dateRange = dateRange;
            this.totalRecords = totalRecords;
            this.createdAt = createdAt;
        }

        public int getReportId() { return reportId; }
        public String getReportTitle() { return reportTitle; }
        public String getReportType() { return reportType; }
        public String getDateRange() { return dateRange; }
        public int getTotalRecords() { return totalRecords; }
        public String getCreatedAt() { return createdAt; }
    }
}