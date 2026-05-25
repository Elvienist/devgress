package com.example.byodsystem.byod.controller;

import com.example.byodsystem.byod.database.DBConnection;
import com.example.byodsystem.byod.model.Device;
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

public class DeviceController {
    @FXML private Button btnDashboard, btnStudents, btnDevices, btnLogs, btnActivity, btnReports, btnSettings, btnLogout;
    @FXML private Label lblDate, lblAdminName, lblAdminRole;
    @FXML private ComboBox<String> cmbDeviceType;
    @FXML private TextField txtBrand, txtModel, txtSerialNumber, txtOwnerID, txtSearch;
    @FXML private Button btnRegister, btnClear;

    @FXML private TableView<Device> tblDevices;
    @FXML private TableColumn<Device, String> colSerial, colInfo, colOwner, colStatus;

    private ObservableList<Device> deviceList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        lblAdminName.setText(SessionManager.currentOperatorName);
        lblAdminRole.setText(SessionManager.currentOperatorRole);
        lblDate.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, MMM dd, yyyy")));
        cmbDeviceType.setItems(FXCollections.observableArrayList("Laptop", "Tablet", "Other"));
        setupSidebarActions();
        configureTableColumns();
        loadDevices("");

        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> loadDevices(newVal.trim()));
        btnRegister.setOnAction(e -> registerDevice());
        btnClear.setOnAction(e -> clearForm());
    }

    private void configureTableColumns() {
        colSerial.setCellValueFactory(new PropertyValueFactory<>("serialNumber"));
        colInfo.setCellValueFactory(new PropertyValueFactory<>("deviceInfo"));
        colOwner.setCellValueFactory(new PropertyValueFactory<>("ownerDetails"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
    }

    private void loadDevices(String searchKeyword) {
        deviceList.clear();
        String query = "SELECT d.*, s.full_name, s.student_id FROM devices d JOIN students s ON d.owner_id = s.student_record_id " +
                "WHERE d.serial_number ILIKE ? OR d.brand ILIKE ? OR d.model ILIKE ? OR s.full_name ILIKE ? OR s.student_id ILIKE ?";

        try (Connection conn = DBConnection.connect(); PreparedStatement ps = conn.prepareStatement(query)) {
            String wildcard = "%" + searchKeyword + "%";
            ps.setString(1, wildcard);
            ps.setString(2, wildcard);
            ps.setString(3, wildcard);
            ps.setString(4, wildcard);
            ps.setString(5, wildcard);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    deviceList.add(new Device(
                            rs.getInt("device_id"),
                            rs.getString("serial_number"),
                            rs.getString("brand"),
                            rs.getString("model"),
                            rs.getString("device_type"),
                            rs.getString("full_name"),
                            rs.getString("student_id"),
                            rs.getString("status")
                    ));
                }
            }
            tblDevices.setItems(deviceList);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void registerDevice() {
        String studentLookup = "SELECT student_record_id FROM students WHERE student_id = ?";
        String insertQuery = "INSERT INTO devices (serial_number, brand, model, device_type, owner_id, status) VALUES (?, ?, ?, ?, ?, 'Active')";

        try (Connection conn = DBConnection.connect(); PreparedStatement psLook = conn.prepareStatement(studentLookup)) {
            psLook.setString(1, txtOwnerID.getText().trim());
            try (ResultSet rs = psLook.executeQuery()) {
                if (rs.next()) {
                    int recordId = rs.getInt("student_record_id");
                    try (PreparedStatement psIns = conn.prepareStatement(insertQuery)) {
                        psIns.setString(1, txtSerialNumber.getText().trim());
                        psIns.setString(2, txtBrand.getText().trim());
                        psIns.setString(3, txtModel.getText().trim());
                        psIns.setString(4, cmbDeviceType.getValue());
                        psIns.setInt(5, recordId);
                        psIns.executeUpdate();
                    }
                    clearForm();
                    loadDevices("");
                } else {
                    System.out.println("Error: Student ID not found.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void clearForm() {
        txtBrand.clear();
        txtModel.clear();
        txtSerialNumber.clear();
        txtOwnerID.clear();
        cmbDeviceType.getSelectionModel().clearSelection();
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