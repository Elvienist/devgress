package com.example.byodsystem.byod.controller;

import com.example.byodsystem.byod.service.AuditLogger;
import com.example.byodsystem.byod.service.UserSession;
import com.example.byodsystem.byod.database.DBConnection;
import com.example.byodsystem.byod.utils.AlertHelper;
import javafx.collections.FXCollections;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class SettingsController implements Initializable {


    private static final int ACADEMIC_YEAR_START_MONTH = 8; // August

    @FXML private Label lblDate;
    @FXML private TextField txtInstitutionName;
    @FXML private ComboBox<String> cmbAcademicYear;
    @FXML private TextField txtCorrectionWindow;
    @FXML private TextField txtEndOfDayTime;
    @FXML private Button btnSave;

    private int currentUserId = UserSession.getInstance().getUserId();
    private int settingId = -1;

    private String originalAcademicYear = null;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setDateLabel();
        loadSettings();
    }

    private void setDateLabel() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");
        lblDate.setText(java.time.LocalDate.now(ZoneId.of("Asia/Manila")).format(fmt));
    }

    private void populateAcademicYearOptions(String anchorValue) {
        int baseStart;
        if (anchorValue != null && anchorValue.matches("\\d{4}-\\d{4}")) {
            baseStart = Integer.parseInt(anchorValue.substring(0, 4));
        } else {
            LocalDate now = LocalDate.now(ZoneId.of("Asia/Manila"));
            baseStart = now.getMonthValue() >= ACADEMIC_YEAR_START_MONTH
                    ? now.getYear()
                    : now.getYear() - 1;
        }

        List<String> options = new ArrayList<>();
        options.add((baseStart - 1) + "-" + baseStart);
        options.add(baseStart + "-" + (baseStart + 1));
        options.add((baseStart + 1) + "-" + (baseStart + 2));
        cmbAcademicYear.setItems(FXCollections.observableArrayList(options));
    }

    private void loadSettings() {
        String sql = "SELECT setting_id, institution_name, academic_year, correction_window_min, end_of_day_time FROM settings LIMIT 1";
        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                settingId = rs.getInt("setting_id");
                txtInstitutionName.setText(rs.getString("institution_name"));

                String storedAcademicYear = rs.getString("academic_year");
                originalAcademicYear = storedAcademicYear;
                populateAcademicYearOptions(storedAcademicYear);
                cmbAcademicYear.setValue(storedAcademicYear);

                txtCorrectionWindow.setText(String.valueOf(rs.getInt("correction_window_min")));
                Time eod = rs.getTime("end_of_day_time");
                if (eod != null) txtEndOfDayTime.setText(eod.toString().substring(0, 5));
            } else {
                populateAcademicYearOptions(null);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            Window owner = txtInstitutionName.getScene() != null ? txtInstitutionName.getScene().getWindow() : null;
            AlertHelper.showNegative(owner, "Database Error", "Failed to load settings: " + e.getMessage());
        }
    }

    private Integer parseStartYear(String academicYear) {
        if (academicYear == null || !academicYear.matches("\\d{4}-\\d{4}")) return null;
        return Integer.parseInt(academicYear.substring(0, 4));
    }


    private String nextYearLevel(String level) {
        if (level == null) return null;
        String trimmed = level.trim();
        String upper = trimmed.toUpperCase();
        if (upper.contains("IRREGULAR")) return trimmed;
        if (upper.startsWith("1")) return "2nd Year";
        if (upper.startsWith("2")) return "3rd Year";
        if (upper.startsWith("3")) return "4th Year";
        if (upper.startsWith("4")) return "4th Year";
        return trimmed;
    }

    private String previousYearLevel(String level) {
        if (level == null) return null;
        String trimmed = level.trim();
        String upper = trimmed.toUpperCase();
        if (upper.contains("IRREGULAR")) return trimmed;
        if (upper.startsWith("4")) return "3rd Year";
        if (upper.startsWith("3")) return "2nd Year";
        if (upper.startsWith("2")) return "1st Year";
        if (upper.startsWith("1")) return "1st Year";
        return trimmed;
    }

    private int adjustStudentYearLevels(Connection conn, boolean advance) throws SQLException {
        int updated = 0;
        String selectSql = "SELECT student_id, year_level FROM students WHERE status = 'ACTIVE'";
        String updateSql = "UPDATE students SET year_level = ?, updated_at = NOW() AT TIME ZONE 'Asia/Manila' WHERE student_id = ?";

        try (PreparedStatement selectPs = conn.prepareStatement(selectSql);
             ResultSet rs = selectPs.executeQuery();
             PreparedStatement updatePs = conn.prepareStatement(updateSql)) {

            while (rs.next()) {
                int studentId = rs.getInt("student_id");
                String currentLevel = rs.getString("year_level");
                String newLevel = advance ? nextYearLevel(currentLevel) : previousYearLevel(currentLevel);
                if (newLevel != null && !newLevel.equalsIgnoreCase(currentLevel)) {
                    updatePs.setString(1, newLevel);
                    updatePs.setInt(2, studentId);
                    updatePs.addBatch();
                    updated++;
                }
            }
            updatePs.executeBatch();
        }
        return updated;
    }

    @FXML
    private void saveSettings() {
        Window owner = txtInstitutionName.getScene().getWindow();
        String institution = txtInstitutionName.getText().trim();
        String academicYear = cmbAcademicYear.getValue();
        String correctionStr = txtCorrectionWindow.getText().trim();
        String endOfDay = txtEndOfDayTime.getText().trim();

        if (institution.isBlank()) {
            AlertHelper.showNegative(owner, "Validation", "Institution name cannot be empty.");
            return;
        }
        if (academicYear == null || !academicYear.matches("\\d{4}-\\d{4}")) {
            AlertHelper.showNegative(owner, "Validation", "Please select a valid academic year.");
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

        Integer originalStart = parseStartYear(originalAcademicYear);
        Integer newStart = parseStartYear(academicYear);
        boolean isYearAdvance = originalStart != null && newStart != null && newStart > originalStart;
        boolean isYearRegress = originalStart != null && newStart != null && newStart < originalStart;


        Runnable performSave = () -> {
            try (Connection conn = DBConnection.connect()) {
                conn.setAutoCommit(false);
                try {
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

                    int affectedCount = 0;
                    if (isYearAdvance) {
                        affectedCount = adjustStudentYearLevels(conn, true);
                    } else if (isYearRegress) {
                        affectedCount = adjustStudentYearLevels(conn, false);
                    }

                    String yearChangeJson = "";
                    if (isYearAdvance) {
                        yearChangeJson = ",\"year_advanced\":true,\"previous_academic_year\":\"" + originalAcademicYear +
                                "\",\"students_promoted\":" + affectedCount;
                    } else if (isYearRegress) {
                        yearChangeJson = ",\"year_reverted\":true,\"previous_academic_year\":\"" + originalAcademicYear +
                                "\",\"students_demoted\":" + affectedCount;
                    }
                    String details = "{\"institution\":\"" + institution + "\",\"academic_year\":\"" + academicYear +
                            "\",\"correction_window\":" + correctionMinutes + yearChangeJson + "}";
                    AuditLogger.log(conn, currentUserId, "SETTINGS_CHANGED", "System", settingId, details);

                    conn.commit();

                    originalAcademicYear = academicYear;
                    populateAcademicYearOptions(academicYear);
                    cmbAcademicYear.setValue(academicYear);

                    String successMsg;
                    if (isYearAdvance) {
                        successMsg = "New settings have been saved.\n\nAcademic year advanced to " + academicYear + " \u2014 "
                                + affectedCount + " student record(s) promoted to their next year level.";
                    } else if (isYearRegress) {
                        successMsg = "New settings have been saved.\n\nAcademic year reverted to " + academicYear + " \u2014 "
                                + affectedCount + " student record(s) demoted to their previous year level.";
                    } else {
                        successMsg = "New settings have been saved.";
                    }
                    AlertHelper.showPositive(owner, "Settings Saved", successMsg);
                } catch (SQLException ex) {
                    conn.rollback();
                    throw ex;
                } finally {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException e) {
                e.printStackTrace();
                AlertHelper.showNegative(owner, "Database Error", "Failed to save settings: " + e.getMessage());
            }
        };

        if (isYearAdvance) {
            showOverlayConfirm("\u26A0", "#FEF3C7", "#D97706",
                    "Advance Academic Year",
                    "Advance academic year to " + academicYear + "?\n\nThis will automatically promote every " +
                            "active student to their next year level (e.g. 1st Year \u2192 2nd Year, 4th Year " +
                            "stays 4th Year). This action applies system-wide and cannot be undone automatically.",
                    "Yes, Continue", "#7A0000",
                    performSave);
        } else if (isYearRegress) {
            showOverlayConfirm("\u26A0", "#FEF3C7", "#D97706",
                    "Revert Academic Year",
                    "Change academic year back to " + academicYear + "?\n\nThis will automatically demote every " +
                            "active student to their previous year level (e.g. 4th Year \u2192 3rd Year, 1st Year " +
                            "stays 1st Year). This action applies system-wide and cannot be undone automatically.",
                    "Yes, Continue", "#7A0000",
                    performSave);
        } else {
            showOverlayConfirm("\uD83D\uDCBE", "#DBEAFE", "#1D4ED8",
                    "Save Settings",
                    "Are you sure you want to save the changes to the system settings?",
                    "Save", "#8B0000",
                    performSave);
        }
    }


    private void showOverlayConfirm(String iconGlyph, String iconBg, String iconColor,
                                    String title, String message,
                                    String confirmText, String confirmColor,
                                    Runnable onConfirm) {
        Scene scene = btnSave.getScene();
        if (scene == null) return;

        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.55);");
        overlay.setAlignment(Pos.CENTER);
        overlay.setOnMouseClicked(Event::consume);
        overlay.prefWidthProperty().bind(scene.widthProperty());
        overlay.prefHeightProperty().bind(scene.heightProperty());

        StackPane iconCircle = new StackPane();
        iconCircle.setMinSize(64, 64);
        iconCircle.setMaxSize(64, 64);
        iconCircle.setStyle("-fx-background-color: " + iconBg + "; -fx-background-radius: 32;");
        Label iconLabel = new Label(iconGlyph);
        iconLabel.setStyle("-fx-text-fill: " + iconColor + "; -fx-font-size: 28px;");
        iconCircle.getChildren().add(iconLabel);

        Label lblTitle = new Label(title);
        lblTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #111111; -fx-font-family: 'Segoe UI', Inter, sans-serif;");
        lblTitle.setWrapText(true);
        lblTitle.setTextAlignment(TextAlignment.CENTER);

        Label lblSub = new Label(message);
        lblSub.setStyle("-fx-font-size: 13px; -fx-text-fill: #6B7280; -fx-font-family: 'Segoe UI', Inter, sans-serif;");
        lblSub.setWrapText(true);
        lblSub.setTextAlignment(TextAlignment.CENTER);
        lblSub.setMaxWidth(340);

        Button btnCancel = new Button("Cancel");
        btnCancel.setPrefWidth(120);
        btnCancel.setPrefHeight(40);
        btnCancel.setStyle(
                "-fx-background-color: #FFFFFF; " +
                        "-fx-text-fill: #374151; " +
                        "-fx-border-color: #D1D5DB; " +
                        "-fx-border-radius: 8; " +
                        "-fx-background-radius: 8; " +
                        "-fx-font-size: 13px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-cursor: hand; " +
                        "-fx-font-family: 'Segoe UI', Inter, sans-serif;"
        );

        Button btnConfirm = new Button(confirmText);
        btnConfirm.setPrefWidth(140);
        btnConfirm.setPrefHeight(40);
        btnConfirm.setStyle(
                "-fx-background-color: " + confirmColor + "; " +
                        "-fx-text-fill: #FFFFFF; " +
                        "-fx-background-radius: 8; " +
                        "-fx-border-radius: 8; " +
                        "-fx-font-size: 13px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-cursor: hand; " +
                        "-fx-font-family: 'Segoe UI', Inter, sans-serif;"
        );

        HBox btnRow = new HBox(12, btnCancel, btnConfirm);
        btnRow.setAlignment(Pos.CENTER);

        VBox dialog = new VBox(16, iconCircle, lblTitle, lblSub, btnRow);
        dialog.setAlignment(Pos.CENTER);
        dialog.setStyle(
                "-fx-background-color: #FFFFFF; " +
                        "-fx-background-radius: 20; " +
                        "-fx-padding: 36 40 36 40; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.25), 15, 0, 0, 4);"
        );
        dialog.setMaxWidth(400);
        dialog.setMinWidth(380);
        dialog.setPrefHeight(javafx.scene.layout.Region.USE_COMPUTED_SIZE);
        dialog.setMaxHeight(javafx.scene.layout.Region.USE_PREF_SIZE);

        overlay.getChildren().add(dialog);
        StackPane.setAlignment(dialog, Pos.CENTER);

        GaussianBlur blur = new GaussianBlur(6);
        Parent currentRoot = scene.getRoot();

        if (currentRoot instanceof StackPane && "windowRootWrapper".equals(currentRoot.getId())) {
            StackPane wrapper = (StackPane) currentRoot;
            if (!wrapper.getChildren().isEmpty()) {
                wrapper.getChildren().get(0).setEffect(blur);
            }
            wrapper.getChildren().add(overlay);
        } else {
            currentRoot.setEffect(blur);
            StackPane windowRootWrapper = new StackPane();
            windowRootWrapper.setId("windowRootWrapper");
            scene.setRoot(windowRootWrapper);
            windowRootWrapper.getChildren().addAll(currentRoot, overlay);
        }

        btnCancel.setOnAction(e -> closeOverlay(scene, overlay));
        btnConfirm.setOnAction(e -> {
            closeOverlay(scene, overlay);
            if (onConfirm != null) onConfirm.run();
        });
    }

    private void closeOverlay(Scene scene, StackPane overlay) {
        Parent activeRoot = scene.getRoot();
        if (activeRoot instanceof StackPane) {
            StackPane wrapper = (StackPane) activeRoot;
            wrapper.getChildren().remove(overlay);
            if (!wrapper.getChildren().isEmpty()) {
                wrapper.getChildren().get(0).setEffect(null);
            }
        }
    }

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
