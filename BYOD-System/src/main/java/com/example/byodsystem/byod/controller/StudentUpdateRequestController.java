package com.example.byodsystem.byod.controller;

import com.example.byodsystem.byod.database.DBConnection;
import com.example.byodsystem.byod.service.UserSession;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

import java.sql.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

public class StudentUpdateRequestController {

    @FXML private Label lblHeaderDate;
    @FXML private Label lblHeaderInitial;
    @FXML private Label lblHeaderName;
    @FXML private ComboBox<String> cbField;
    @FXML private TextField tfCurrentValue;
    @FXML private TextField tfNewValue;
    @FXML private ComboBox<String> cbYearLevelOptions;
    @FXML private TextArea taReason;
    @FXML private Label lblFormError;
    @FXML private Button btnSubmit;
    @FXML private VBox pastRequestsContainer;

    private int studentRefId;
    private Map<String, String> fieldToColumn;
    private Map<String, String> currentValues;

    @FXML
    public void initialize() {
        UserSession session = UserSession.getInstance();
        if (!"STUDENT".equalsIgnoreCase(session.getRole())) return;

        studentRefId = session.getStudentRefId();
        String name = session.getUsername() != null ? session.getUsername() : "Student";
        lblHeaderDate.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")));
        lblHeaderName.setText(name);
        lblHeaderInitial.setText(name.isEmpty() ? "S" : String.valueOf(name.charAt(0)).toUpperCase());

        fieldToColumn = new LinkedHashMap<>();
        fieldToColumn.put("Full Name", "full_name");
        fieldToColumn.put("Course", "course");
        fieldToColumn.put("Year Level", "year_level");
        fieldToColumn.put("Contact Number", "contact_number");

        cbField.setItems(FXCollections.observableArrayList(fieldToColumn.keySet()));

        cbYearLevelOptions.setItems(FXCollections.observableArrayList(
                "1st Year", "2nd Year", "3rd Year", "4th Year", "Irregular"
        ));

        currentValues = new LinkedHashMap<>();
        loadCurrentValues();
        loadPastRequests();
    }

    private void loadCurrentValues() {
        String sql = "SELECT full_name, course, year_level, contact_number FROM students WHERE student_id = ?";
        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentRefId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                currentValues.put("Full Name", rs.getString("full_name"));
                currentValues.put("Course", rs.getString("course"));
                currentValues.put("Year Level", rs.getString("year_level"));
                currentValues.put("Contact Number", rs.getString("contact_number"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleFieldSelected() {
        String selected = cbField.getValue();
        lblFormError.setText("");
        tfNewValue.clear();
        cbYearLevelOptions.setValue(null);

        if (selected != null && currentValues.containsKey(selected)) {
            String val = currentValues.get(selected);
            tfCurrentValue.setText(val != null ? val : "");

            if ("Year Level".equals(selected)) {
                tfNewValue.setVisible(false);
                cbYearLevelOptions.setVisible(true);
            } else {
                tfNewValue.setVisible(true);
                cbYearLevelOptions.setVisible(false);
            }
        } else {
            tfCurrentValue.clear();
            tfNewValue.setVisible(true);
            cbYearLevelOptions.setVisible(false);
        }
    }

    private boolean hasPendingRequestForField(String columnName) {
        String sql = "SELECT COUNT(*) FROM profile_update_requests WHERE student_id = ? AND field_name = ? AND status = 'PENDING'";
        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentRefId);
            ps.setString(2, columnName);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @FXML
    public void handleSubmit() {
        lblFormError.setText("");
        lblFormError.setStyle("-fx-text-fill: #DC2626;");

        String field = cbField.getValue();
        String reason = taReason.getText() != null ? taReason.getText().trim() : "";
        String currentVal = tfCurrentValue.getText() != null ? tfCurrentValue.getText().trim() : "";

        String newValue;
        if ("Year Level".equals(field)) {
            newValue = cbYearLevelOptions.getValue() != null ? cbYearLevelOptions.getValue() : "";
        } else {
            newValue = tfNewValue.getText() != null ? tfNewValue.getText().trim() : "";
        }

        if (field == null) { lblFormError.setText("Please select a field."); return; }
        if (newValue.isEmpty()) { lblFormError.setText("New value cannot be empty."); return; }
        if (reason.isEmpty()) { lblFormError.setText("Reason cannot be empty."); return; }
        if (newValue.equalsIgnoreCase(currentVal)) {
            lblFormError.setText("New value is the same as the current value.");
            return;
        }

        String columnName = fieldToColumn.get(field);

        if (hasPendingRequestForField(columnName)) {
            lblFormError.setText("You already have a pending update request for " + field + ".");
            return;
        }

        String sql = """
                INSERT INTO profile_update_requests (student_id, field_name, current_value, requested_value, reason, status, submitted_at)
                VALUES (?, ?, ?, ?, ?, 'PENDING', ?)
                """;
        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentRefId);
            ps.setString(2, columnName);
            ps.setString(3, currentVal.isEmpty() ? null : currentVal);
            ps.setString(4, newValue);
            ps.setString(5, reason);

            ZoneId phZone = ZoneId.of("Asia/Manila");
            ZonedDateTime phNow = ZonedDateTime.now(phZone);
            Timestamp phTimestamp = Timestamp.from(phNow.toInstant());
            ps.setTimestamp(6, phTimestamp);

            ps.executeUpdate();

            cbField.setValue(null);
            tfCurrentValue.clear();
            tfNewValue.clear();
            cbYearLevelOptions.setValue(null);
            cbYearLevelOptions.setVisible(false);
            tfNewValue.setVisible(true);
            taReason.clear();

            lblFormError.setStyle("-fx-text-fill: #15803D;");
            lblFormError.setText("Request submitted successfully.");
            loadPastRequests();
        } catch (Exception e) {
            lblFormError.setStyle("-fx-text-fill: #B91C1C;");
            lblFormError.setText("Failed to submit request. Please try again.");
            e.printStackTrace();
        }
    }

    private void loadPastRequests() {
        if (pastRequestsContainer == null) return;
        pastRequestsContainer.getChildren().clear();
        String sql = """
                SELECT field_name, current_value, requested_value, reason, status, admin_response, submitted_at
                FROM profile_update_requests
                WHERE student_id = ?
                ORDER BY submitted_at DESC
                """;
        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentRefId);
            ResultSet rs = ps.executeQuery();
            boolean hasRows = false;

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm");

            while (rs.next()) {
                hasRows = true;
                String fieldName = rs.getString("field_name");
                String current = rs.getString("current_value");
                String requested = rs.getString("requested_value");
                String reason = rs.getString("reason");
                String status = rs.getString("status");
                String adminResponse = rs.getString("admin_response");
                Timestamp submitted = rs.getTimestamp("submitted_at");
                String submittedStr = submitted != null ? submitted.toLocalDateTime().format(fmt) : "—";

                String displayField = fieldToColumn.entrySet().stream()
                        .filter(e -> e.getValue().equals(fieldName))
                        .map(Map.Entry::getKey)
                        .findFirst().orElse(fieldName);

                VBox card = buildRequestCard(displayField, current, requested, reason, status, adminResponse, submittedStr);
                pastRequestsContainer.getChildren().add(card);
            }
            if (!hasRows) {
                Label empty = new Label("No past requests found.");
                empty.setStyle("-fx-text-fill: #9CA3AF;");
                empty.setFont(new Font("Segoe UI", 13));
                pastRequestsContainer.getChildren().add(empty);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private VBox buildRequestCard(String field, String current, String requested,
                                  String reason, String status, String adminResponse, String submitted) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: #F8F9FA; -fx-background-radius: 8; -fx-border-color: #EBF0F5; -fx-border-radius: 8; -fx-border-width: 1; -fx-padding: 14 16 14 16;");

        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label fieldLabel = new Label(field);
        fieldLabel.setStyle("-fx-text-fill: #111111; -fx-font-weight: bold;");
        fieldLabel.setFont(new Font("Segoe UI", 13));

        Label statusChip = new Label(capitalize(status));
        String chipStyle = switch (status) {
            case "PENDING" -> "-fx-text-fill: #B45309; -fx-background-color: #FEF3C7; -fx-background-radius: 4; -fx-padding: 2 8 2 8; -fx-font-weight: bold;";
            case "APPROVED" -> "-fx-text-fill: #15803D; -fx-background-color: #DCFCE7; -fx-background-radius: 4; -fx-padding: 2 8 2 8; -fx-font-weight: bold;";
            case "REJECTED" -> "-fx-text-fill: #B91C1C; -fx-background-color: #FEE2E2; -fx-background-radius: 4; -fx-padding: 2 8 2 8; -fx-font-weight: bold;";
            default -> "-fx-text-fill: #6C757D; -fx-background-color: #F3F4F6; -fx-background-radius: 4; -fx-padding: 2 8 2 8;";
        };
        statusChip.setStyle(chipStyle);
        statusChip.setFont(new Font("Segoe UI", 11));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label dateLabel = new Label(submitted);
        dateLabel.setStyle("-fx-text-fill: #9CA3AF;");
        dateLabel.setFont(new Font("Segoe UI", 11));

        topRow.getChildren().addAll(fieldLabel, statusChip, spacer, dateLabel);

        Label changeLabel = new Label((current != null ? current : "—") + "  →  " + (requested != null ? requested : "—"));
        changeLabel.setStyle("-fx-text-fill: #374151; -fx-font-weight: 500;");
        changeLabel.setFont(new Font("Segoe UI", 12));

        VBox detail = new VBox(4);
        detail.setVisible(false);
        detail.setManaged(false);

        Label reasonLabel = new Label("Reason: " + (reason != null ? reason : "—"));
        reasonLabel.setStyle("-fx-text-fill: #6C757D;");
        reasonLabel.setFont(new Font("Segoe UI", 12));
        reasonLabel.setWrapText(true);
        detail.getChildren().add(reasonLabel);

        if (adminResponse != null && !adminResponse.isEmpty()) {
            Label respLabel = new Label("Admin Response: " + adminResponse);
            respLabel.setStyle("-fx-text-fill: #374151; -fx-background-color: #EFF6FF; -fx-background-radius: 6; -fx-padding: 6 10 6 10;");
            respLabel.setFont(new Font("Segoe UI", 12));
            respLabel.setWrapText(true);
            detail.getChildren().add(respLabel);
        }

        Button toggle = new Button("Show details ▾");
        toggle.setStyle("-fx-background-color: transparent; -fx-text-fill: #7A0000; -fx-cursor: hand; -fx-padding: 0;");
        toggle.setFont(new Font("Segoe UI", 12));
        toggle.setOnAction(e -> {
            boolean showing = detail.isVisible();
            detail.setVisible(!showing);
            detail.setManaged(!showing);
            toggle.setText(showing ? "Show details ▾" : "Hide details ▴");
        });

        card.getChildren().addAll(topRow, changeLabel, toggle, detail);
        return card;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.charAt(0) + s.substring(1).toLowerCase();
    }
}