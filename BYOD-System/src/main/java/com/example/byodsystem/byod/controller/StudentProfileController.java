package com.example.byodsystem.byod.controller;

import com.example.byodsystem.byod.database.DBConnection;
import com.example.byodsystem.byod.service.UserSession;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class StudentProfileController {

    @FXML private Label lblHeaderDate;
    @FXML private Label lblHeaderAvatarInitial;
    @FXML private Label lblHeaderName;
    @FXML private Label lblAvatarInitial;
    @FXML private Label lblFullName;
    @FXML private Label lblStudentCode;
    @FXML private Label lblCourse;
    @FXML private Label lblYearLevel;
    @FXML private Label lblContactNumber;
    @FXML private Label lblStatus;
    @FXML private Label lblTotalEntries;
    @FXML private Label lblTotalExits;
    @FXML private Label lblLastMovement;
    @FXML private VBox devicesContainer;
    @FXML private Button btnRequestUpdate;

    private int studentRefId;

    @FXML
    public void initialize() {
        UserSession session = UserSession.getInstance();
        if (!"STUDENT".equalsIgnoreCase(session.getRole())) {
            return;
        }
        studentRefId = session.getStudentRefId();
        lblHeaderDate.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")));
        loadStudentProfile();
        loadDeviceStats();
        loadRegisteredDevices();
    }

    private void loadStudentProfile() {
        String sql = """
                SELECT s.full_name, s.student_code, s.course, s.year_level,
                       s.contact_number, s.status
                FROM students s
                WHERE s.student_id = ?
                """;
        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentRefId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String fullName = rs.getString("full_name");
                String code = rs.getString("student_code");
                String course = rs.getString("course");
                String yearLevel = rs.getString("year_level");
                String contact = rs.getString("contact_number");
                String status = rs.getString("status");

                String initial = fullName != null && !fullName.isEmpty()
                        ? String.valueOf(fullName.charAt(0)).toUpperCase() : "?";

                lblFullName.setText(fullName != null ? fullName : "—");
                lblStudentCode.setText(code != null ? code : "—");
                lblCourse.setText(course != null ? course : "—");
                lblYearLevel.setText(yearLevel != null ? yearLevel : "—");
                lblContactNumber.setText(contact != null ? contact : "—");
                lblAvatarInitial.setText(initial);
                lblHeaderAvatarInitial.setText(initial);
                lblHeaderName.setText(fullName != null ? fullName : "—");

                if ("ACTIVE".equalsIgnoreCase(status)) {
                    lblStatus.setText("Active");
                    lblStatus.setStyle("-fx-text-fill: #15803D; -fx-background-color: #DCFCE7; -fx-background-radius: 6; -fx-padding: 2 10 2 10; -fx-font-weight: bold;");
                } else {
                    lblStatus.setText("Inactive");
                    lblStatus.setStyle("-fx-text-fill: #B91C1C; -fx-background-color: #FEE2E2; -fx-background-radius: 6; -fx-padding: 2 10 2 10; -fx-font-weight: bold;");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadDeviceStats() {
        String entrySql = """
                SELECT COUNT(*) AS total_entries
                FROM device_logs dl
                JOIN devices d ON dl.device_id = d.device_id
                WHERE d.owner_id = ? AND dl.direction = 'IN' AND dl.status != 'VOIDED'
                """;
        String exitSql = """
                SELECT COUNT(*) AS total_exits
                FROM device_logs dl
                JOIN devices d ON dl.device_id = d.device_id
                WHERE d.owner_id = ? AND dl.direction = 'OUT' AND dl.status != 'VOIDED'
                """;
        String lastMoveSql = """
                SELECT dl.log_time, dl.direction
                FROM device_logs dl
                JOIN devices d ON dl.device_id = d.device_id
                WHERE d.owner_id = ? AND dl.status != 'VOIDED'
                ORDER BY dl.log_time DESC
                LIMIT 1
                """;
        try (Connection conn = DBConnection.connect()) {
            try (PreparedStatement ps = conn.prepareStatement(entrySql)) {
                ps.setInt(1, studentRefId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) lblTotalEntries.setText(String.valueOf(rs.getInt("total_entries")));
            }
            try (PreparedStatement ps = conn.prepareStatement(exitSql)) {
                ps.setInt(1, studentRefId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) lblTotalExits.setText(String.valueOf(rs.getInt("total_exits")));
            }
            try (PreparedStatement ps = conn.prepareStatement(lastMoveSql)) {
                ps.setInt(1, studentRefId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    String time = rs.getTimestamp("log_time").toLocalDateTime()
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    String dir = "IN".equals(rs.getString("direction")) ? "Entry" : "Exit";
                    lblLastMovement.setText(time + " · " + dir);
                } else {
                    lblLastMovement.setText("No movement recorded");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadRegisteredDevices() {
        String sql = """
                SELECT d.device_id, d.brand, d.model, d.device_type, d.serial_number,
                       (SELECT dl.direction FROM device_logs dl
                        WHERE dl.device_id = d.device_id AND dl.status != 'VOIDED'
                        ORDER BY dl.log_time DESC LIMIT 1) AS last_direction
                FROM devices d
                WHERE d.owner_id = ? AND d.status = 'ACTIVE'
                ORDER BY d.created_at
                """;
        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentRefId);
            ResultSet rs = ps.executeQuery();
            devicesContainer.getChildren().clear();
            while (rs.next()) {
                String brand = rs.getString("brand");
                String model = rs.getString("model");
                String type = rs.getString("device_type");
                String serial = rs.getString("serial_number");
                String lastDir = rs.getString("last_direction");

                String displayName = brand + " " + model;
                String typeLabel = capitalize(type) + " • Serial: " + serial;
                boolean isInside = "IN".equals(lastDir);
                String locationText = lastDir == null ? "Unknown" : (isInside ? "Inside" : "Outside");
                String locationStyle = isInside
                        ? "-fx-text-fill: #15803D; -fx-background-color: #DCFCE7; -fx-background-radius: 6; -fx-padding: 2 10 2 10;"
                        : "-fx-text-fill: #6C757D; -fx-background-color: #F3F4F6; -fx-background-radius: 6; -fx-padding: 2 10 2 10;";

                HBox row = new HBox(12);
                row.setStyle("-fx-background-color: #F8F9FA; -fx-background-radius: 8; -fx-padding: 14 16 14 16;");
                row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                StackPane iconBox = new StackPane();
                iconBox.setMinSize(36, 36);
                iconBox.setMaxSize(36, 36);
                iconBox.setStyle("-fx-background-color: #FEE2E2; -fx-background-radius: 8;");
                Label iconLabel = new Label("💻");
                iconLabel.setFont(new Font(16));
                iconBox.getChildren().add(iconLabel);

                VBox info = new VBox(2);
                HBox.setHgrow(info, javafx.scene.layout.Priority.ALWAYS);
                Label nameLabel = new Label(displayName);
                nameLabel.setStyle("-fx-text-fill: #111111; -fx-font-weight: bold;");
                nameLabel.setFont(new Font(13));
                Label typeInfo = new Label(typeLabel);
                typeInfo.setStyle("-fx-text-fill: #6C757D;");
                typeInfo.setFont(new Font(12));
                info.getChildren().addAll(nameLabel, typeInfo);

                Label locationBadge = new Label(locationText);
                locationBadge.setStyle(locationStyle);
                locationBadge.setFont(new Font(12));

                row.getChildren().addAll(iconBox, info, locationBadge);
                devicesContainer.getChildren().add(row);
            }
            if (devicesContainer.getChildren().isEmpty()) {
                Label empty = new Label("No registered devices found.");
                empty.setStyle("-fx-text-fill: #9CA3AF;");
                empty.setFont(new Font(13));
                devicesContainer.getChildren().add(empty);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleRequestUpdate() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/com/example/byodsystem/byod/fxml/studentupdaterequest.fxml"));
            javafx.scene.Parent view = loader.load();
            javafx.scene.layout.StackPane contentArea = (javafx.scene.layout.StackPane)
                    btnRequestUpdate.getScene().lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().clear();
                contentArea.getChildren().add(view);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.charAt(0) + s.substring(1).toLowerCase();
    }
}