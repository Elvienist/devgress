package com.example.byodsystem.byod.controller;

import com.example.byodsystem.byod.database.DBConnection;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import java.io.IOException;
import java.sql.*;

public class StudentProfileRequestController {

    @FXML private VBox registrationView;
    @FXML private VBox updateRequestView;

    @FXML private TextField regStudentCode;
    @FXML private TextField regFullName;
    @FXML private TextField regCourse;
    @FXML private ComboBox<String> regYearLevel;
    @FXML private TextField regContactNumber;

    @FXML private TextField currentFullName;
    @FXML private TextField currentCourse;
    @FXML private TextField currentYearLevel;
    @FXML private TextField currentContactNumber;

    @FXML private TextField reqFullName;
    @FXML private TextField reqCourse;
    @FXML private ComboBox<String> reqYearLevel;
    @FXML private TextField reqContactNumber;
    @FXML private TextArea txtReason;

    private int activeUserId;
    private int currentStudentId;

    public void initializeSession(int userId, boolean isFirstLogin, Integer studentRefId) {
        this.activeUserId = userId;
        if (isFirstLogin || studentRefId == null || studentRefId == 0) {
            registrationView.setVisible(true);
            updateRequestView.setVisible(false);
        } else {
            this.currentStudentId = studentRefId;
            registrationView.setVisible(false);
            updateRequestView.setVisible(true);
            loadAllCurrentProfileValues();
        }
    }

    @FXML
    public void handleSaveRegistration() {
        String code = regStudentCode.getText() == null ? "" : regStudentCode.getText().trim();
        String name = regFullName.getText() == null ? "" : regFullName.getText().trim();
        String crs = regCourse.getText() == null ? "" : regCourse.getText().trim();
        String yr = regYearLevel.getValue();
        String contact = regContactNumber.getText() == null ? "" : regContactNumber.getText().trim();

        if (code.isEmpty() || name.isEmpty() || crs.isEmpty() || yr == null) {
            showNotification(Alert.AlertType.WARNING, "Form Errors", "Please fill out required student profile items.");
            return;
        }

        String insertStudent = "INSERT INTO students (student_code, full_name, course, year_level, contact_number, status) VALUES (?, ?, ?, ?, ?, 'ACTIVE') RETURNING student_id";
        String updateUser = "UPDATE users SET student_ref_id = ? WHERE user_id = ?";

        Connection conn = null;
        try {
            conn = DBConnection.connect();
            conn.setAutoCommit(false);

            int generatedStudentId = 0;
            try (PreparedStatement pst1 = conn.prepareStatement(insertStudent)) {
                pst1.setString(1, code);
                pst1.setString(2, name);
                pst1.setString(3, crs);
                pst1.setString(4, yr);
                pst1.setString(5, contact.isEmpty() ? null : contact);

                try (ResultSet rs = pst1.executeQuery()) {
                    if (rs.next()) generatedStudentId = rs.getInt(1);
                }
            }

            if (generatedStudentId > 0) {
                try (PreparedStatement pst2 = conn.prepareStatement(updateUser)) {
                    pst2.setInt(1, generatedStudentId);
                    pst2.setInt(2, activeUserId);
                    pst2.executeUpdate();
                }
            }

            conn.commit();
            showNotification(Alert.AlertType.INFORMATION, "Success", "Profile completed successfully.");
            navigateToProfileView();

        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            e.printStackTrace();
            showNotification(Alert.AlertType.ERROR, "System Error", "Database failure saving profile registration components.");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }

    @FXML
    public void handleSubmitUpdateRequest() {
        String reason = txtReason.getText() == null ? "" : txtReason.getText().trim();
        if (reason.isEmpty()) {
            showNotification(Alert.AlertType.WARNING, "Input Needed", "A clear explanation reason text must be submitted.");
            return;
        }

        String sql = "INSERT INTO profile_update_requests (student_id, field_name, current_value, requested_value, reason) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.connect()) {
            conn.setAutoCommit(false);

            try (PreparedStatement pst = conn.prepareStatement(sql)) {
                if (!reqFullName.getText().trim().isEmpty()) {
                    executeBatchInsertion(pst, "full_name", currentFullName.getText(), reqFullName.getText().trim(), reason);
                }
                if (!reqCourse.getText().trim().isEmpty()) {
                    executeBatchInsertion(pst, "course", currentCourse.getText(), reqCourse.getText().trim(), reason);
                }
                if (reqYearLevel.getValue() != null) {
                    executeBatchInsertion(pst, "year_level", currentYearLevel.getText(), reqYearLevel.getValue(), reason);
                }
                if (!reqContactNumber.getText().trim().isEmpty()) {
                    executeBatchInsertion(pst, "contact_number", currentContactNumber.getText(), reqContactNumber.getText().trim(), reason);
                }
            }

            conn.commit();
            showNotification(Alert.AlertType.INFORMATION, "Request Logged", "Your updates are pending verification clearance.");
            navigateToProfileView();
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
    }

    private void executeBatchInsertion(PreparedStatement pst, String field, String oldVal, String newVal, String reason) throws SQLException {
        pst.setInt(1, currentStudentId);
        pst.setString(2, field);
        pst.setString(3, oldVal);
        pst.setString(4, newVal);
        pst.setString(5, reason);
        pst.executeUpdate();
    }

    private void loadAllCurrentProfileValues() {
        String sql = "SELECT full_name, course, year_level, contact_number FROM students WHERE student_id = ?";
        try (Connection conn = DBConnection.connect();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, this.currentStudentId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    currentFullName.setText(rs.getString("full_name"));
                    currentCourse.setText(rs.getString("course"));
                    currentYearLevel.setText(rs.getString("year_level"));
                    String contact = rs.getString("contact_number");
                    currentContactNumber.setText(contact != null ? contact : "");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void navigateToProfileView() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/byodsystem/byod/fxml/studentprofile.fxml"));
        Parent root = loader.load();
        StackPane contentArea = (StackPane) registrationView.getScene().lookup("#contentArea");
        if (contentArea != null) {
            contentArea.getChildren().setAll(root);
        }
    }

    private void showNotification(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}