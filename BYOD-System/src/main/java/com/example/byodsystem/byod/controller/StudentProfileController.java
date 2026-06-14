package com.example.byodsystem.byod.controller;

import com.example.byodsystem.byod.database.DBConnection;
import com.example.byodsystem.byod.service.UserSession;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import java.io.IOException;
import java.sql.*;

public class StudentProfileController {
    @FXML private Label lblAvatarInitials, lblFullName, lblStudentCode, lblCourse, lblYearLevel, lblContactNumber, lblStatus;
    @FXML private Label lblTotalEntries, lblTotalExits, lblLastMovement;
    @FXML private VBox devicesContainer;

    private int loggedInUserId;
    private Integer activeStudentId = null;

    @FXML
    public void initialize() {
        this.loggedInUserId = UserSession.getInstance().getUserId();

        loadStudentProfileData();

        if (activeStudentId != null) {
            loadMovementStats();
            loadRegisteredDevices();
        } else {
            redirectToRegistration();
        }
    }

    private void loadStudentProfileData() {
        String sql = "SELECT s.student_id, s.student_code, s.full_name, s.course, s.year_level, s.contact_number, s.status " +
                "FROM users u " +
                "LEFT JOIN students s ON u.student_ref_id = s.student_id " +
                "WHERE u.user_id = ?";

        try (Connection conn = DBConnection.connect();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setInt(1, loggedInUserId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next() && rs.getObject("student_id") != null) {
                    activeStudentId = rs.getInt("student_id");

                    String fullName = rs.getString("full_name");
                    lblFullName.setText(fullName != null ? fullName : "N/A");
                    lblStudentCode.setText(rs.getString("student_code"));
                    lblCourse.setText(rs.getString("course"));
                    lblYearLevel.setText(rs.getString("year_level"));

                    String contact = rs.getString("contact_number");
                    lblContactNumber.setText(contact != null ? contact : "No Contact Info");
                    lblStatus.setText(rs.getString("status"));

                    if (fullName != null && !fullName.trim().isEmpty()) {
                        String[] nameTokens = fullName.trim().split("\\s+");
                        if (nameTokens.length >= 2) {
                            lblAvatarInitials.setText(("" + nameTokens[0].charAt(0) + nameTokens[nameTokens.length - 1].charAt(0)).toUpperCase());
                        } else {
                            lblAvatarInitials.setText(("" + nameTokens[0].charAt(0)).toUpperCase());
                        }
                    } else {
                        lblAvatarInitials.setText("??");
                    }
                } else {
                    activeStudentId = null;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showNotification(Alert.AlertType.ERROR, "Database Error", "Failed to load profile records.");
        }
    }

    private void redirectToRegistration() {
        Platform.runLater(() -> {
            try {
                showNotification(Alert.AlertType.INFORMATION, "Profile Missing",
                        "No profile is linked to this account. Redirecting to registration view.");

                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/byodsystem/byod/fxml/studentprofilerequest.fxml"));
                Parent root = loader.load();

                StudentProfileRequestController requestController = loader.getController();
                requestController.initializeSession(loggedInUserId, true, null);

                StackPane contentArea = (StackPane) devicesContainer.getScene().lookup("#contentArea");
                if (contentArea != null) {
                    contentArea.getChildren().setAll(root);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void loadMovementStats() {
        String sql = "SELECT log_type, count(*) as total FROM gate_logs WHERE student_id = ? GROUP BY log_type";
        try (Connection conn = DBConnection.connect();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setInt(1, activeStudentId);
            try (ResultSet rs = pst.executeQuery()) {
                lblTotalEntries.setText("0");
                lblTotalExits.setText("0");
                while (rs.next()) {
                    String logType = rs.getString("log_type");
                    String totalCount = String.valueOf(rs.getInt("total"));

                    if ("ENTRY".equalsIgnoreCase(logType)) {
                        lblTotalEntries.setText(totalCount);
                    } else if ("EXIT".equalsIgnoreCase(logType)) {
                        lblTotalExits.setText(totalCount);
                    }
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void loadRegisteredDevices() {
        devicesContainer.getChildren().clear();
        String sql = "SELECT device_name, device_type, serial_number, status FROM devices WHERE student_id = ?";
        try (Connection conn = DBConnection.connect();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setInt(1, activeStudentId);
            try (ResultSet rs = pst.executeQuery()) {
                boolean hasDevices = false;
                while (rs.next()) {
                    hasDevices = true;
                    devicesContainer.getChildren().add(createDeviceCardUI(
                            rs.getString("device_name"),
                            rs.getString("device_type"),
                            rs.getString("serial_number"),
                            rs.getString("status")
                    ));
                }
                if (!hasDevices) {
                    devicesContainer.getChildren().add(new Label("No BYOD hardware equipment linked to this profile yet."));
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private HBox createDeviceCardUI(String name, String type, String serial, String status) {
        HBox card = new HBox(16);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color: #F8F9FA; -fx-border-color: #E5E7EB; -fx-border-radius: 8; -fx-padding: 16;");

        Label icon = new Label("LAPTOP".equalsIgnoreCase(type) ? "💻" : "📱");
        icon.setStyle("-fx-text-fill: #991B1B; -fx-font-size: 20;");

        VBox details = new VBox(2, new Label(name), new Label(type + " • " + serial));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label statusLbl = new Label(status != null ? status : "UNKNOWN");
        if ("ACTIVE".equalsIgnoreCase(status)) {
            statusLbl.setStyle("-fx-background-color: #D1FAE5; -fx-text-fill: #065F46; -fx-padding: 4 12; -fx-background-radius: 4; -fx-font-weight: bold;");
        } else {
            statusLbl.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #991B1B; -fx-padding: 4 12; -fx-background-radius: 4; -fx-font-weight: bold;");
        }

        card.getChildren().addAll(icon, details, spacer, statusLbl);
        return card;
    }

    @FXML
    public void handleNavigateToRequest() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/byodsystem/byod/fxml/studentprofilerequest.fxml"));
        Parent root = loader.load();

        StudentProfileRequestController requestController = loader.getController();
        requestController.initializeSession(loggedInUserId, (activeStudentId == null), activeStudentId);

        StackPane contentArea = (StackPane) devicesContainer.getScene().lookup("#contentArea");
        if (contentArea != null) {
            contentArea.getChildren().setAll(root);
        }
    }

    private void showNotification(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}