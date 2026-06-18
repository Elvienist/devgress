package com.example.byodsystem.byod.controller;
import com.example.byodsystem.byod.service.AuditLogger;
import com.example.byodsystem.byod.service.UserSession;
import com.example.byodsystem.byod.database.DBConnection;

import com.example.byodsystem.byod.utils.AlertHelper;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class SettingsController implements Initializable {

    @FXML private Label lblDate;
    @FXML private TextField txtInstitutionName;
    @FXML private TextField txtAcademicYear;
    @FXML private TextField txtCorrectionWindow;
    @FXML private TextField txtEndOfDayTime;
    @FXML private Button btnSave;

    private int currentUserId = UserSession.getInstance().getUserId();
    private int settingId = -1;
    private Window owner = txtInstitutionName.getScene().getWindow();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setDateLabel();
        loadSettings();
    }

    private void setDateLabel() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");
        lblDate.setText(java.time.LocalDate.now(ZoneId.of("Asia/Manila")).format(fmt));
    }

    private void loadSettings() {
        String sql = "SELECT setting_id, institution_name, academic_year, correction_window_min, end_of_day_time FROM settings LIMIT 1";
        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                settingId = rs.getInt("setting_id");
                txtInstitutionName.setText(rs.getString("institution_name"));
                txtAcademicYear.setText(rs.getString("academic_year"));
                txtCorrectionWindow.setText(String.valueOf(rs.getInt("correction_window_min")));
                Time eod = rs.getTime("end_of_day_time");
                if (eod != null) txtEndOfDayTime.setText(eod.toString().substring(0, 5));
            }
        } catch (SQLException e) {
            e.printStackTrace();

            AlertHelper.showNegative(owner, "Database Error", "Failed to load settings: " + e.getMessage());
        }
    }

    @FXML
    private void saveSettings() {
        String institution = txtInstitutionName.getText().trim();
        String academicYear = txtAcademicYear.getText().trim();
        String correctionStr = txtCorrectionWindow.getText().trim();
        String endOfDay = txtEndOfDayTime.getText().trim();

        if (institution.isBlank()) {
            AlertHelper.showNegative(owner, "Validation", "Institution name cannot be empty.");
            return;
        }
        if (academicYear.isBlank()) {
            AlertHelper.showNegative(owner, "Validation", "Academic year cannot be empty.");
            return;
        }
        int correctionMinutes;
        try {
            correctionMinutes = Integer.parseInt(correctionStr);
            if (correctionMinutes < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            AlertHelper.showNegative(owner, "Validation", "Gate correction time must be a valid positive number.");
            return;
        }
        if (!endOfDay.isBlank() && !endOfDay.matches("^([01]\\d|2[0-3]):[0-5]\\d$")) {
            AlertHelper.showNegative(owner, "Validation", "End of day time must be in HH:MM format (24-hour).");
            return;
        }
        Time eodTime = endOfDay.isBlank() ? null : Time.valueOf(endOfDay + ":00");

        try (Connection conn = DBConnection.connect()) {
            if (settingId < 0) {
                String insertSql = "INSERT INTO settings (institution_name, academic_year, correction_window_min, end_of_day_time, updated_by, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, NOW() AT TIME ZONE 'Asia/Manila') RETURNING setting_id";
                try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                    ps.setString(1, institution);
                    ps.setString(2, academicYear);
                    ps.setInt(3, correctionMinutes);
                    if (eodTime != null) ps.setTime(4, eodTime); else ps.setNull(4, Types.TIME);
                    ps.setInt(5, currentUserId);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) settingId = rs.getInt(1);
                }
            } else {
                String updateSql = "UPDATE settings SET institution_name = ?, academic_year = ?, " +
                        "correction_window_min = ?, end_of_day_time = ?, updated_by = ?, " +
                        "updated_at = NOW() AT TIME ZONE 'Asia/Manila' WHERE setting_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setString(1, institution);
                    ps.setString(2, academicYear);
                    ps.setInt(3, correctionMinutes);
                    if (eodTime != null) ps.setTime(4, eodTime); else ps.setNull(4, Types.TIME);
                    ps.setInt(5, currentUserId);
                    ps.setInt(6, settingId);
                    ps.executeUpdate();
                }
            }
            AuditLogger.log(conn, currentUserId, "SETTINGS_CHANGED", "System", settingId,
                    "{\"institution\":\"" + institution + "\",\"academic_year\":\"" + academicYear +
                            "\",\"correction_window\":" + correctionMinutes + "}");
            AlertHelper.showPositive(owner, "Success", "Settings saved successfully.");
        } catch (SQLException e) {
            e.printStackTrace();
            AlertHelper.showNegative(owner, "Database Error", "Failed to save settings: " + e.getMessage());
        }
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
    @FXML private void changePassword() { navigate("/com/example/byodsystem/byod/changepassword.fxml"); }
    @FXML private void logout() { navigate("/com/example/byodsystem/byod/Login.fxml"); }

    private void navigate(String fxml) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            Stage stage = (Stage) btnSave.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}