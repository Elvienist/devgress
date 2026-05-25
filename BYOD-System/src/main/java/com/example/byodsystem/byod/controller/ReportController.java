package com.example.byodsystem.byod.controller;

import com.example.byodsystem.byod.database.DBConnection;
import com.example.byodsystem.byod.model.DeviceLog;
import com.example.byodsystem.byod.model.ReportModel;
import com.example.byodsystem.byod.utils.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ReportController {
    @FXML private Button btnDashboard, btnStudents, btnDevices, btnLogs, btnActivity, btnReports, btnSettings, btnLogout;
    @FXML private Label lblDate, lblAdminName, lblAdminRole;
    @FXML private Button btnGenerateReport;
    @FXML private ComboBox<String> cmbReportType;

    @FXML private TableView<DeviceLog> tblActivityLogs;
    @FXML private TableColumn<DeviceLog, String> colLogDetails, colLogTime, colLogDirection;

    @FXML private TableView<ReportModel> tblReports;
    @FXML private TableColumn<ReportModel, String> colRepTitle, colRepType, colRepBy, colRepRange, colRepDate;
    @FXML private TableColumn<ReportModel, Integer> colRepRecords;

    private ObservableList<DeviceLog> logList = FXCollections.observableArrayList();
    private ObservableList<ReportModel> reportList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        lblAdminName.setText(SessionManager.currentOperatorName);
        lblAdminRole.setText(SessionManager.currentOperatorRole);
        lblDate.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, MMM dd, yyyy")));
        cmbReportType.setItems(FXCollections.observableArrayList("Daily Ingress/Egress", "Device Statistics", "Security Audit", "System Activity"));

        setupSidebarActions();
        configureTables();
        loadActivityLogs();
        loadReportsArchived();

        btnGenerateReport.setOnAction(e -> generateNewReportExecution());
    }

    private void configureTables() {
        colLogDetails.setCellValueFactory(new PropertyValueFactory<>("metaRow"));
        colLogTime.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        colLogDirection.setCellValueFactory(new PropertyValueFactory<>("direction"));

        colRepTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colRepType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colRepBy.setCellValueFactory(new PropertyValueFactory<>("generatedBy"));
        colRepRange.setCellValueFactory(new PropertyValueFactory<>("dateRange"));
        colRepRecords.setCellValueFactory(new PropertyValueFactory<>("totalRecords"));
        colRepDate.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
    }

    private void loadActivityLogs() {
        logList.clear();
        String query = "SELECT dl.*, d.serial_number, d.brand, d.model, s.full_name, s.student_id " +
                "FROM device_logs dl JOIN devices d ON dl.device_id = d.device_id " +
                "JOIN students s ON d.owner_id = s.student_record_id ORDER BY dl.login_time DESC LIMIT 50";

        try (Connection conn = DBConnection.connect(); PreparedStatement ps = conn.prepareStatement(query); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String direction = "Entered Campus";
                String timeVal = rs.getString("login_time");
                if (rs.getTimestamp("logout_time") != null) {
                    direction = "Exited Campus";
                    timeVal = rs.getString("logout_time");
                }

                logList.add(new DeviceLog(
                        rs.getInt("log_id"),
                        rs.getString("full_name"),
                        rs.getString("student_id"),
                        rs.getString("brand") + " " + rs.getString("model"),
                        rs.getString("serial_number"),
                        timeVal,
                        direction
                ));
            }
            tblActivityLogs.setItems(logList);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadReportsArchived() {
        reportList.clear();
        String query = "SELECT r.*, s.full_name FROM reports r JOIN students s ON r.generated_by = s.student_record_id ORDER BY r.created_at DESC";

        try (Connection conn = DBConnection.connect(); PreparedStatement ps = conn.prepareStatement(query); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                reportList.add(new ReportModel(
                        rs.getInt("report_id"),
                        rs.getString("report_title"),
                        rs.getString("report_type"),
                        rs.getString("full_name"),
                        rs.getDate("filter_start_date") + " to " + rs.getDate("filter_end_date"),
                        rs.getInt("total_records_processed"),
                        rs.getString("created_at")
                ));
            }
            tblReports.setItems(reportList);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void generateNewReportExecution() {
        String typeSelected = cmbReportType.getValue();
        if (typeSelected == null) return;

        String insertReport = "INSERT INTO reports (report_title, report_type, generated_by, filter_start_date, filter_end_date, total_records_processed) " +
                "VALUES (?, ?, ?, CURRENT_DATE, CURRENT_DATE, (SELECT COUNT(*) FROM device_logs))";

        try (Connection conn = DBConnection.connect(); PreparedStatement ps = conn.prepareStatement(insertReport)) {
            ps.setString(1, typeSelected + " Ledger Summary");
            ps.setString(2, typeSelected);
            ps.setInt(3, SessionManager.currentOperatorId);
            ps.executeUpdate();
            loadReportsArchived();
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