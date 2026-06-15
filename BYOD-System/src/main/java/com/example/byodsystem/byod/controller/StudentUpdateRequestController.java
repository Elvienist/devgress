package com.example.byodsystem.byod.controller;

import com.example.byodsystem.byod.database.DBConnection;
import com.example.byodsystem.byod.service.UserSession;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
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
    @FXML private Button btnReturnToProfile;
    @FXML private VBox pastRequestsContainer;

    @FXML private Button btnFilterAll;
    @FXML private Button btnFilterPending;
    @FXML private Button btnFilterApproved;
    @FXML private Button btnFilterRejected;

    private int studentRefId;
    private String currentFilter = null;
    private Map<String, String> fieldToColumn;
    private Map<String, String> columnToLabel;

    @FXML
    public void initialize() {
        lblHeaderDate.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")));

        UserSession session = UserSession.getInstance();
        studentRefId = session.getStudentRefId() != null ? session.getStudentRefId() : 0;
        String name = session.getFullName() != null ? session.getFullName() : "Student";
        lblHeaderName.setText(name);
        if (!name.isEmpty()) {
            lblHeaderInitial.setText(String.valueOf(name.charAt(0)).toUpperCase());
        }

        fieldToColumn = new LinkedHashMap<>();
        fieldToColumn.put("Full Name", "full_name");
        fieldToColumn.put("Course", "course");
        fieldToColumn.put("Year Level", "year_level");
        fieldToColumn.put("Contact Number", "contact_number");

        columnToLabel = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : fieldToColumn.entrySet()) {
            columnToLabel.put(entry.getValue(), entry.getKey());
        }

        cbField.setItems(FXCollections.observableArrayList(fieldToColumn.keySet()));
        cbYearLevelOptions.setItems(FXCollections.observableArrayList("1st Year", "2nd Year", "3rd Year", "4th Year"));

        if (taReason != null) {
            taReason.setTextFormatter(new TextFormatter<String>(change -> {
                if (change.isAdded() || change.isReplaced()) {
                    if (change.getControlNewText().length() > 255) {
                        return null;
                    }
                }
                return change;
            }));
        }

        if (pastRequestsContainer != null) {
            pastRequestsContainer.setAlignment(Pos.TOP_LEFT);
        }

        loadCurrentStudentData();
        loadPastRequests();
    }

    private void loadCurrentStudentData() {
        if (studentRefId == 0) return;
        String sql = "SELECT full_name, course, year_level, contact_number FROM students WHERE student_id = ?";
        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentRefId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                cbField.getSelectionModel().select("Full Name");
                tfCurrentValue.setText(rs.getString("full_name"));
                toggleValueInputMode("Full Name");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleFieldChange() {
        String selectedField = cbField.getValue();
        if (selectedField == null || studentRefId == 0) return;

        toggleValueInputMode(selectedField);
        String columnName = fieldToColumn.get(selectedField);
        if (columnName == null) return;

        String sql = "SELECT " + columnName + " FROM students WHERE student_id = ?";
        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentRefId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String val = rs.getString(columnName);
                tfCurrentValue.setText(val != null ? val : "");
            } else {
                tfCurrentValue.setText("");
            }
        } catch (Exception e) {
            e.printStackTrace();
            tfCurrentValue.setText("");
        }
    }

    private void toggleValueInputMode(String selectedField) {
        if ("Year Level".equals(selectedField)) {
            tfNewValue.setVisible(false);
            cbYearLevelOptions.setVisible(true);
            cbYearLevelOptions.getSelectionModel().clearSelection();
        } else {
            cbYearLevelOptions.setVisible(false);
            tfNewValue.setVisible(true);
            tfNewValue.clear();
        }
    }

    @FXML
    public void handleSubmit() {
        lblFormError.setText("");
        String selectedField = cbField.getValue();
        String reason = taReason.getText() != null ? taReason.getText().trim() : "";

        if (selectedField == null) {
            lblFormError.setText("Please select a field to update.");
            return;
        }

        String newValue;
        if ("Year Level".equals(selectedField)) {
            newValue = cbYearLevelOptions.getValue();
        } else {
            newValue = tfNewValue.getText() != null ? tfNewValue.getText().trim() : "";
        }

        if (newValue == null || newValue.isEmpty()) {
            lblFormError.setText("New value cannot be empty.");
            return;
        }
        if (reason.isEmpty()) {
            lblFormError.setText("Please provide a reason for the update request.");
            return;
        }

        String dbColumnName = fieldToColumn.get(selectedField);
        String currentValue = tfCurrentValue.getText();

        String sql = """
            INSERT INTO profile_update_requests (student_id, field_name, current_value, requested_value, reason, status, submitted_at)
            VALUES (?, ?, ?, ?, ?, 'PENDING', NOW())
        """;

        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentRefId);
            ps.setString(2, dbColumnName);
            ps.setString(3, currentValue);
            ps.setString(4, newValue);
            ps.setString(5, reason);

            ps.executeUpdate();

            taReason.clear();
            if ("Year Level".equals(selectedField)) {
                cbYearLevelOptions.getSelectionModel().clearSelection();
            } else {
                tfNewValue.clear();
            }

            loadPastRequests();
            lblFormError.setStyle("-fx-text-fill: #15803D; -fx-font-weight: bold;");
            lblFormError.setText("Request submitted successfully!");

            navigateToProfile();
        } catch (Exception e) {
            e.printStackTrace();
            lblFormError.setStyle("-fx-text-fill: #B91C1C; -fx-font-weight: bold;");
            lblFormError.setText("Database error occurred while submitting.");
        }
    }

    @FXML
    public void handleReturnToProfile() {
        navigateToProfile();
    }

    private void navigateToProfile() {
        try {
            StackPane contentArea = (StackPane) btnReturnToProfile.getScene().lookup("#contentArea");
            if (contentArea != null) {
                Parent view = FXMLLoader.load(getClass().getResource("/com/example/byodsystem/byod/fxml/studentprofile.fxml"));
                contentArea.getChildren().clear();
                contentArea.getChildren().add(view);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML public void handleFilterAll() { setActiveFilter(null); }
    @FXML public void handleFilterPending() { setActiveFilter("PENDING"); }
    @FXML public void handleFilterApproved() { setActiveFilter("APPROVED"); }
    @FXML public void handleFilterRejected() { setActiveFilter("REJECTED"); }

    private void setActiveFilter(String filter) {
        currentFilter = filter;

        String active = "-fx-background-color: #7A0000; -fx-text-fill: #FFFFFF; -fx-background-radius: 20; -fx-padding: 6 14; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 12px;";
        String inactiveAll = "-fx-background-color: #F3F4F6; -fx-text-fill: #4B5563; -fx-background-radius: 20; -fx-padding: 6 14; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 12px;";

        btnFilterAll.setStyle(filter == null ? active : inactiveAll);
        btnFilterPending.setStyle("PENDING".equals(filter) ? active : "-fx-background-color: #FEF3C7; -fx-text-fill: #92400E; -fx-background-radius: 20; -fx-padding: 6 14; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 12px;");
        btnFilterApproved.setStyle("APPROVED".equals(filter) ? active : "-fx-background-color: #DCFCE7; -fx-text-fill: #15803D; -fx-background-radius: 20; -fx-padding: 6 14; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 12px;");
        btnFilterRejected.setStyle("REJECTED".equals(filter) ? active : "-fx-background-color: #FEE2E2; -fx-text-fill: #B91C1C; -fx-background-radius: 20; -fx-padding: 6 14; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 12px;");

        loadPastRequests();
    }

    private void loadPastRequests() {
        pastRequestsContainer.getChildren().clear();
        pastRequestsContainer.setAlignment(Pos.TOP_LEFT);

        if (studentRefId == 0) return;

        String sql = """
            SELECT field_name, current_value, requested_value, reason, status, admin_response, submitted_at
            FROM profile_update_requests
            WHERE student_id = ?
        """ + (currentFilter != null ? " AND status = ?" : "") + """
            ORDER BY submitted_at DESC
        """;

        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentRefId);
            if (currentFilter != null) {
                ps.setString(2, currentFilter);
            }

            ResultSet rs = ps.executeQuery();
            boolean hasItems = false;

            while (rs.next()) {
                hasItems = true;
                String dbField = rs.getString("field_name");
                String displayField = columnToLabel.getOrDefault(dbField, dbField);
                String currentVal = rs.getString("current_value");
                String requestedVal = rs.getString("requested_value");
                String reason = rs.getString("reason");
                String status = rs.getString("status");
                String adminResponse = rs.getString("admin_response");
                Timestamp ts = rs.getTimestamp("submitted_at");

                String formattedDate = "—";
                if (ts != null) {
                    ZonedDateTime zdt = ts.toInstant().atZone(ZoneId.systemDefault());
                    formattedDate = zdt.format(DateTimeFormatter.ofPattern("MMM dd, yyyy • hh:mm a"));
                }

                VBox card = new VBox(12);
                card.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #E5E7EB; -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 16;");

                HBox topRow = new HBox(8);
                topRow.setAlignment(Pos.CENTER_LEFT);

                VBox titleBox = new VBox(2);
                Label fieldLabel = new Label("Update Request: " + displayField);
                fieldLabel.setStyle("-fx-text-fill: #111111; -fx-font-weight: bold;");
                fieldLabel.setFont(new Font("Segoe UI", 14));

                Label dateLabel = new Label("Submitted on " + formattedDate);
                dateLabel.setStyle("-fx-text-fill: #6B7280;");
                dateLabel.setFont(new Font("Segoe UI", 11));
                titleBox.getChildren().addAll(fieldLabel, dateLabel);

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                Label badge = new Label(status != null ? status.toUpperCase() : "PENDING");
                badge.setFont(Font.font("Segoe UI", javafx.scene.text.FontWeight.BOLD, 11));
                if ("APPROVED".equalsIgnoreCase(status)) {
                    badge.setStyle("-fx-text-fill: #15803D; -fx-background-color: #DCFCE7; -fx-background-radius: 20; -fx-padding: 4 10 4 10;");
                } else if ("REJECTED".equalsIgnoreCase(status)) {
                    badge.setStyle("-fx-text-fill: #B91C1C; -fx-background-color: #FEE2E2; -fx-background-radius: 20; -fx-padding: 4 10 4 10;");
                } else {
                    badge.setStyle("-fx-text-fill: #92400E; -fx-background-color: #FEF3C7; -fx-background-radius: 20; -fx-padding: 4 10 4 10;");
                }

                topRow.getChildren().addAll(titleBox, spacer, badge);
                card.getChildren().add(topRow);

                VBox detail = new VBox(8);
                detail.setStyle("-fx-background-color: #F9FAFB; -fx-background-radius: 6; -fx-padding: 12; -fx-border-color: #F3F4F6; -fx-border-width: 1; -fx-border-radius: 6;");
                detail.setVisible(false);
                detail.setManaged(false);

                HBox changeRow = new HBox(16);
                VBox curBox = new VBox(2);
                Label cl = new Label("From (Current)"); cl.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 11px;");
                Label cv = new Label(currentVal != null && !currentVal.isEmpty() ? currentVal : "—"); cv.setStyle("-fx-text-fill: #374151; -fx-font-weight: bold; -fx-font-size: 13px;");
                curBox.getChildren().addAll(cl, cv);

                VBox newBox = new VBox(2);
                Label nl = new Label("To (Requested)"); nl.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 11px;");
                Label nv = new Label(requestedVal != null && !requestedVal.isEmpty() ? requestedVal : "—"); nv.setStyle("-fx-text-fill: #7A0000; -fx-font-weight: bold; -fx-font-size: 13px;");
                newBox.getChildren().addAll(nl, nv);

                changeRow.getChildren().addAll(curBox, newBox);
                detail.getChildren().add(changeRow);

                Label reasonLabel = new Label("Reason: " + (reason != null ? reason : "—"));
                reasonLabel.setStyle("-fx-text-fill: #4B5563;");
                reasonLabel.setFont(new Font("Segoe UI", 12));
                reasonLabel.setWrapText(true);
                detail.getChildren().add(reasonLabel);

                if (adminResponse != null && !adminResponse.isEmpty()) {
                    VBox adminBox = new VBox(4);
                    Label arl = new Label("Administrator Remarks:");
                    arl.setStyle("-fx-text-fill: #374151; -fx-font-weight: bold; -fx-font-size: 11px;");
                    Label respLabel = new Label(adminResponse);
                    respLabel.setStyle("-fx-text-fill: #374151; -fx-background-color: #EFF6FF; -fx-background-radius: 6; -fx-padding: 8 12; -fx-border-color: #DBEAFE; -fx-border-width: 1; -fx-border-radius: 6;");
                    respLabel.setFont(new Font("Segoe UI", 12));
                    respLabel.setWrapText(true);
                    adminBox.getChildren().addAll(arl, respLabel);
                    detail.getChildren().add(adminBox);
                }

                Button toggle = new Button("Show details ▾");
                toggle.setStyle("-fx-background-color: transparent; -fx-text-fill: #7A0000; -fx-cursor: hand; -fx-padding: 0; -fx-font-weight: bold;");
                toggle.setFont(new Font("Segoe UI", 12));
                toggle.setOnAction(e -> {
                    boolean showing = detail.isVisible();
                    detail.setVisible(!showing);
                    detail.setManaged(!showing);
                    toggle.setText(showing ? "Show details ▾" : "Hide details ▴");
                });

                card.getChildren().addAll(detail, toggle);
                pastRequestsContainer.getChildren().add(card);
            }

            if (!hasItems) {
                Label noRequestsLabel = new Label(currentFilter == null ? "No submission history found." : "No historic logs found for state: " + currentFilter.toLowerCase());
                noRequestsLabel.setStyle("-fx-text-fill: #9CA3AF; -fx-font-style: italic; -fx-padding: 16 0 16 4;");
                noRequestsLabel.setFont(new Font("Segoe UI", 13));
                pastRequestsContainer.getChildren().add(noRequestsLabel);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}