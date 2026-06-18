package com.example.byodsystem.byod.controller;

import com.example.byodsystem.byod.database.DBConnection;
import com.example.byodsystem.byod.service.UserSession;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class StudentDeviceLogController {

    @FXML private Label lblHeaderDate;
    @FXML private Label lblHeaderInitial;
    @FXML private Label lblHeaderName;

    @FXML private DatePicker dpFrom;
    @FXML private DatePicker dpTo;
    @FXML private ComboBox<String> cbDirection;
    @FXML private ComboBox<String> cbDeviceFilter;

    @FXML private Label lblSumEntries;
    @FXML private Label lblSumExits;
    @FXML private Label lblSumTotal;

    @FXML private TableView<ObservableList<String>> tblDeviceLog;
    @FXML private TableColumn<ObservableList<String>, String> colLogTime;
    @FXML private TableColumn<ObservableList<String>, String> colDevice;
    @FXML private TableColumn<ObservableList<String>, String> colSerial;
    @FXML private TableColumn<ObservableList<String>, String> colType;
    @FXML private TableColumn<ObservableList<String>, String> colDirection;
    @FXML private TableColumn<ObservableList<String>, String> colStatus;

    // studentRefId is now the student_code (VARCHAR) stored in session
    private String studentRefId;
    // resolved integer student_id used in all DB queries
    private int studentId = -1;

    @FXML
    public void initialize() {
        UserSession session = UserSession.getInstance();
        if (!"STUDENT".equalsIgnoreCase(session.getRole())) return;

        studentRefId = session.getStudentRefId();  // e.g. "2024-00599-SR-0"
        studentId = resolveStudentId(studentRefId); // look up the integer PK

        String name = session.getFullName() != null ? session.getFullName() : "Student";
        lblHeaderDate.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")));
        lblHeaderName.setText(name);
        lblHeaderInitial.setText(name.isEmpty() ? "S" : String.valueOf(name.charAt(0)).toUpperCase());

        cbDirection.setItems(FXCollections.observableArrayList("All", "IN", "OUT"));
        cbDirection.setValue("All");

        YearMonth current = YearMonth.now();
        dpFrom.setValue(current.atDay(1));
        dpTo.setValue(current.atEndOfMonth());

        setupColumns();
        loadStudentDevices();
        loadData();
    }

    /**
     * Resolves the integer student_id from the student_code (VARCHAR).
     * student_ref_id in users now stores student_code, not student_id.
     */
    private int resolveStudentId(String studentCode) {
        if (studentCode == null || studentCode.isBlank()) return -1;
        String sql = "SELECT student_id FROM students WHERE student_code = ?";
        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, studentCode);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("student_id");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    private void loadStudentDevices() {
        List<String> devicesList = new ArrayList<>();
        devicesList.add("All Devices");

        String sql = "SELECT DISTINCT brand, model FROM devices WHERE owner_id = ? AND status = 'ACTIVE'";
        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);   // FIX: use int studentId, not String studentRefId
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                devicesList.add(rs.getString("brand") + " " + rs.getString("model"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        cbDeviceFilter.setItems(FXCollections.observableArrayList(devicesList));
        cbDeviceFilter.setValue("All Devices");
    }

    private void setupColumns() {
        colLogTime.setCellValueFactory(r -> new SimpleStringProperty(r.getValue().get(0)));
        colDevice.setCellValueFactory(r -> new SimpleStringProperty(r.getValue().get(1)));
        colSerial.setCellValueFactory(r -> new SimpleStringProperty(r.getValue().get(2)));
        colType.setCellValueFactory(r -> new SimpleStringProperty(r.getValue().get(3)));
        colDirection.setCellValueFactory(r -> new SimpleStringProperty(r.getValue().get(4)));
        colStatus.setCellValueFactory(r -> new SimpleStringProperty(r.getValue().get(5)));

        colLogTime.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle("-fx-alignment: CENTER;");
            }
        });

        colDevice.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle("-fx-alignment: CENTER;");
            }
        });

        colSerial.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle("-fx-alignment: CENTER;");
            }
        });

        colType.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle("-fx-alignment: CENTER;");
            }
        });

        colDirection.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                if ("IN".equals(item)) {
                    setStyle("-fx-text-fill: #15803D; -fx-font-weight: bold; -fx-alignment: CENTER;");
                } else {
                    setStyle("-fx-text-fill: #0EA5E9; -fx-font-weight: bold; -fx-alignment: CENTER;");
                }
            }
        });

        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                if ("NORMAL".equals(item)) {
                    setStyle("-fx-text-fill: #15803D; -fx-alignment: CENTER;");
                } else if ("AMENDED".equals(item)) {
                    setStyle("-fx-text-fill: #B45309; -fx-alignment: CENTER;");
                } else {
                    setStyle("-fx-text-fill: #B91C1C; -fx-alignment: CENTER;");
                }
            }
        });
    }

    @FXML
    public void handleApplyFilter() {
        loadData();
    }

    @FXML
    public void handleResetFilter() {
        YearMonth current = YearMonth.now();
        dpFrom.setValue(current.atDay(1));
        dpTo.setValue(current.atEndOfMonth());
        cbDirection.setValue("All");
        cbDeviceFilter.setValue("All Devices");
        loadData();
    }

    private void loadData() {
        LocalDate from = dpFrom.getValue();
        LocalDate to = dpTo.getValue();
        String direction = cbDirection.getValue();
        String selectedDevice = cbDeviceFilter.getValue();

        StringBuilder sql = new StringBuilder("""
                SELECT dl.log_time, d.brand, d.model, d.serial_number, d.device_type,
                       dl.direction, dl.status
                FROM device_logs dl
                JOIN devices d ON dl.device_id = d.device_id
                WHERE d.owner_id = ?
                  AND dl.log_time >= ?
                  AND dl.log_time < ?
                """);

        if (direction != null && !"All".equals(direction)) {
            sql.append(" AND dl.direction = ?");
        }
        if (selectedDevice != null && !"All Devices".equals(selectedDevice)) {
            sql.append(" AND (CONCAT(d.brand, ' ', d.model) = ?)");
        }
        sql.append(" ORDER BY dl.log_time DESC");

        ObservableList<ObservableList<String>> data = FXCollections.observableArrayList();
        int entries = 0, exits = 0;

        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            ps.setInt(idx++, studentId);   // FIX: use int studentId
            ps.setTimestamp(idx++, Timestamp.valueOf(from.atStartOfDay()));
            ps.setTimestamp(idx++, Timestamp.valueOf(to.plusDays(1).atStartOfDay()));

            if (direction != null && !"All".equals(direction)) {
                ps.setString(idx++, direction);
            }
            if (selectedDevice != null && !"All Devices".equals(selectedDevice)) {
                ps.setString(idx, selectedDevice);
            }

            ResultSet rs = ps.executeQuery();
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            while (rs.next()) {
                String device = rs.getString("brand") + " " + rs.getString("model");
                String serial = rs.getString("serial_number");
                String type = capitalize(rs.getString("device_type"));
                String dir = rs.getString("direction");
                String status = rs.getString("status");
                String time = rs.getTimestamp("log_time").toLocalDateTime().format(fmt);

                ObservableList<String> row = FXCollections.observableArrayList(
                        time, device, serial, type, dir, status);
                data.add(row);
                if ("IN".equals(dir)) entries++; else exits++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        tblDeviceLog.setItems(data);
        lblSumEntries.setText(String.valueOf(entries));
        lblSumExits.setText(String.valueOf(exits));
        lblSumTotal.setText(String.valueOf(entries + exits));
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.charAt(0) + s.substring(1).toLowerCase();
    }
}