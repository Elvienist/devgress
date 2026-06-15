package com.example.byodsystem.byod.controller;

import com.example.byodsystem.byod.database.DBConnection;
import com.example.byodsystem.byod.service.UserSession;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class StudentProfileRequestController {

    @FXML private Label lblHeaderDate;
    @FXML private Label lblHeaderName;

    @FXML private Button btnFilterAll;
    @FXML private Button btnFilterPending;
    @FXML private Button btnFilterApproved;
    @FXML private Button btnFilterRejected;

    @FXML private TableView<ObservableList<String>> tblRequests;
    @FXML private TableColumn<ObservableList<String>, String> colStudent;
    @FXML private TableColumn<ObservableList<String>, String> colField;
    @FXML private TableColumn<ObservableList<String>, String> colCurrentValue;
    @FXML private TableColumn<ObservableList<String>, String> colRequestedValue;
    @FXML private TableColumn<ObservableList<String>, String> colReason;
    @FXML private TableColumn<ObservableList<String>, String> colStatus;
    @FXML private TableColumn<ObservableList<String>, String> colSubmitted;
    @FXML private TableColumn<ObservableList<String>, String> colActions;

    @FXML private VBox detailPanel;
    @FXML private VBox detailContent;
    @FXML private VBox rejectResponseArea;
    @FXML private TextArea taRejectResponse;
    @FXML private Button btnConfirmAction;
    @FXML private Label lblLedgerTitle;
    @FXML private Label lblReasonTitle;
    @FXML private Label lblActionError;

    private int adminUserId;
    private String currentFilter = null;
    private int selectedRequestId = -1;
    private boolean isPendingApprovalMode = true;
    private ObservableList<String> activeRowData = null;

    private final Map<String, String> columnToLabel = Map.of(
            "full_name", "Full Name",
            "course", "Course",
            "year_level", "Year Level",
            "contact_number", "Contact Number"
    );

    @FXML
    public void initialize() {
        UserSession session = UserSession.getInstance();
        adminUserId = session.getUserId();
        String name = session.getFullName() != null ? session.getFullName() : "Admin";
        lblHeaderDate.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")));
        lblHeaderName.setText(name);

        setTextLimit(taRejectResponse, 255);

        setupColumns();
        loadRequests(null);
        closeLedger();
    }

    public void initializeSession(int userId, boolean isFirstLogin, int studentRefId) {
        this.adminUserId = userId;
    }

    private void setupColumns() {
        colStudent.setCellValueFactory(r -> new SimpleStringProperty(r.getValue().get(0)));
        colField.setCellValueFactory(r -> new SimpleStringProperty(r.getValue().get(1)));
        colCurrentValue.setCellValueFactory(r -> new SimpleStringProperty(r.getValue().get(5)));
        colRequestedValue.setCellValueFactory(r -> new SimpleStringProperty(r.getValue().get(6)));
        colReason.setCellValueFactory(r -> new SimpleStringProperty(r.getValue().get(7)));
        colSubmitted.setCellValueFactory(r -> r.getValue() != null && r.getValue().size() > 3 ? new SimpleStringProperty(r.getValue().get(3)) : new SimpleStringProperty(""));
        colStatus.setCellValueFactory(r -> new SimpleStringProperty(r.getValue().get(2)));

        configureCompactColumn(colStudent, "#111111", true);
        configureCompactColumn(colField, "#374151", false);
        configureCompactColumn(colCurrentValue, "#374151", false);
        configureCompactColumn(colRequestedValue, "#374151", false);
        configureCompactColumn(colReason, "#374151", false);
        configureCompactColumn(colSubmitted, "#6B7280", false);

        colStatus.setCellFactory(col -> new TableCell<>() {
            private final Label badgeLabel = new Label();
            private final HBox container = new HBox(badgeLabel);

            {
                container.setAlignment(Pos.CENTER);
                badgeLabel.setFont(Font.font("Segoe UI", javafx.scene.text.FontWeight.BOLD, 12));
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    badgeLabel.setText(item);
                    switch (item) {
                        case "Pending" -> badgeLabel.setStyle("-fx-text-fill: #92400E; -fx-background-color: #FEF3C7; -fx-background-radius: 20; -fx-padding: 4 12 4 12;");
                        case "Approved" -> badgeLabel.setStyle("-fx-text-fill: #15803D; -fx-background-color: #DCFCE7; -fx-background-radius: 20; -fx-padding: 4 12 4 12;");
                        case "Rejected" -> badgeLabel.setStyle("-fx-text-fill: #B91C1C; -fx-background-color: #FEE2E2; -fx-background-radius: 20; -fx-padding: 4 12 4 12;");
                        default -> badgeLabel.setStyle("-fx-text-fill: #4B5563; -fx-background-color: #E5E7EB; -fx-background-radius: 20; -fx-padding: 4 12 4 12;");
                    }
                    setGraphic(container);
                }
            }
        });

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnCheck = new Button("✓");
            private final Button btnCross = new Button("✕");
            private final HBox container = new HBox(8, btnCheck, btnCross);

            {
                container.setAlignment(Pos.CENTER);
                btnCheck.setStyle("-fx-background-color: #DCFCE7; -fx-text-fill: #15803D; -fx-background-radius: 50; -fx-cursor: hand; -fx-font-weight: bold; -fx-min-width: 26; -fx-min-height: 26; -fx-max-width: 26; -fx-max-height: 26; -fx-font-size: 11;");
                btnCross.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #B91C1C; -fx-background-radius: 50; -fx-cursor: hand; -fx-font-weight: bold; -fx-min-width: 26; -fx-min-height: 26; -fx-max-width: 26; -fx-max-height: 26; -fx-font-size: 11;");

                btnCheck.setOnAction(e -> {
                    ObservableList<String> rowData = getTableView().getItems().get(getIndex());
                    openConfirmationLedger(rowData, true);
                });

                btnCross.setOnAction(e -> {
                    ObservableList<String> rowData = getTableView().getItems().get(getIndex());
                    openConfirmationLedger(rowData, false);
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    ObservableList<String> rowData = getTableView().getItems().get(getIndex());
                    String status = rowData.get(9);
                    if ("PENDING".equals(status)) {
                        setGraphic(container);
                    } else {
                        setGraphic(null);
                    }
                }
            }
        });
    }

    private void configureCompactColumn(TableColumn<ObservableList<String>, String> column, String hexColor, boolean isBold) {
        column.setCellFactory(tc -> new TableCell<>() {
            private final Label label = new Label();
            private final HBox container = new HBox(label);
            private final Tooltip tooltip = new Tooltip();

            {
                container.setAlignment(Pos.CENTER);
                label.setFont(Font.font("Segoe UI", isBold ? javafx.scene.text.FontWeight.BOLD : javafx.scene.text.FontWeight.NORMAL, 13));
                label.setStyle("-fx-text-fill: " + hexColor + "; -fx-alignment: CENTER; -fx-text-alignment: center;");
                container.setStyle("-fx-padding: 8 4 8 4;");
                tooltip.setStyle("-fx-font-size: 12px; -fx-background-color: #1F2937; -fx-text-fill: #FFFFFF; -fx-padding: 6 10 6 10; -fx-background-radius: 6;");
                tooltip.setWrapText(true);
                tooltip.setMaxWidth(300);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setTooltip(null);
                } else {
                    if (item.length() > 35) {
                        label.setText(item.substring(0, 32) + "...");
                        tooltip.setText(item);
                        setTooltip(tooltip);
                    } else {
                        label.setText(item);
                        setTooltip(null);
                    }
                    setGraphic(container);
                }
            }
        });
    }

    private void setTextLimit(TextArea textArea, int limit) {
        textArea.setTextFormatter(new TextFormatter<String>(change -> {
            if (change.isAdded() || change.isReplaced()) {
                if (change.getControlNewText().length() > limit) {
                    return null;
                }
            }
            return change;
        }));
    }

    private void openConfirmationLedger(ObservableList<String> row, boolean approveMode) {
        activeRowData = row;
        selectedRequestId = Integer.parseInt(row.get(4));
        isPendingApprovalMode = approveMode;

        lblActionError.setText("");
        taRejectResponse.clear();

        detailPanel.setVisible(true);
        detailPanel.setManaged(true);

        detailContent.getChildren().clear();
        addDetailRow("Student Profile", row.get(0));
        addDetailRow("Changing Field", row.get(1));
        addDetailRow("Current Value", row.get(5));
        addDetailRow("New Value to Commit", row.get(6));
        addDetailRow("Reason for Request", row.get(7));

        if (approveMode) {
            lblLedgerTitle.setText("Confirm Request Approval");
            lblReasonTitle.setText("Approval Remarks (Optional)");
            btnConfirmAction.setText("Confirm Approve");
            btnConfirmAction.setStyle("-fx-background-color: #15803D; -fx-text-fill: #FFFFFF; -fx-background-radius: 8; -fx-font-weight: bold; -fx-cursor: hand;");
        } else {
            lblLedgerTitle.setText("Confirm Request Rejection");
            lblReasonTitle.setText("Reason for Rejection (Required)");
            btnConfirmAction.setText("Confirm Reject");
            btnConfirmAction.setStyle("-fx-background-color: #B91C1C; -fx-text-fill: #FFFFFF; -fx-background-radius: 8; -fx-font-weight: bold; -fx-cursor: hand;");
        }
    }

    private void addDetailRow(String label, String value) {
        VBox block = new VBox(3);
        Label lbl = new Label(label);
        lbl.setStyle("-fx-text-fill: #9CA3AF;");
        lbl.setFont(new Font(11));
        Label val = new Label(value != null ? value : "—");
        val.setStyle("-fx-text-fill: #111111; -fx-font-weight: bold;");
        val.setFont(new Font(13));
        val.setWrapText(true);
        block.getChildren().addAll(lbl, val);
        detailContent.getChildren().add(block);
    }

    @FXML
    public void handleConfirmAction() {
        if (selectedRequestId == -1 || activeRowData == null) return;
        String remarks = taRejectResponse.getText() != null ? taRejectResponse.getText().trim() : "";

        if (!isPendingApprovalMode && remarks.isEmpty()) {
            lblActionError.setText("You must provide a rejection reason.");
            return;
        }

        if (isPendingApprovalMode) {
            executeApproval(remarks);
        } else {
            executeRejection(remarks);
        }
    }

    private void executeApproval(String remarks) {
        String fieldColumn = columnToLabel.entrySet().stream()
                .filter(e -> e.getValue().equals(activeRowData.get(1)))
                .map(Map.Entry::getKey)
                .findFirst().orElse(null);
        String requestedValue = activeRowData.get(6);

        String updateStudentSql = fieldColumn != null
                ? "UPDATE students SET " + fieldColumn + " = ?, updated_at = NOW() WHERE student_id = (SELECT student_id FROM profile_update_requests WHERE request_id = ?)"
                : null;

        String updateRequestSql = """
                UPDATE profile_update_requests SET status = 'APPROVED', admin_response = ?, resolved_by = ?, resolved_at = NOW()
                WHERE request_id = ?
                """;

        try (Connection conn = DBConnection.connect()) {
            conn.setAutoCommit(false);
            if (updateStudentSql != null) {
                try (PreparedStatement ps = conn.prepareStatement(updateStudentSql)) {
                    ps.setString(1, requestedValue);
                    ps.setInt(2, selectedRequestId);
                    ps.executeUpdate();
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(updateRequestSql)) {
                ps.setString(1, remarks.isEmpty() ? "Approved by Admin" : remarks);
                ps.setInt(2, adminUserId);
                ps.setInt(3, selectedRequestId);
                ps.executeUpdate();
            }
            conn.commit();
            closeLedger();
            loadRequests(currentFilter);
        } catch (Exception e) {
            lblActionError.setText("Failed to process approval.");
            e.printStackTrace();
        }
    }

    private void executeRejection(String remarks) {
        String sql = """
                UPDATE profile_update_requests SET status = 'REJECTED', admin_response = ?,
                resolved_by = ?, resolved_at = NOW()
                WHERE request_id = ?
                """;
        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, remarks);
            ps.setInt(2, adminUserId);
            ps.setInt(3, selectedRequestId);
            ps.executeUpdate();
            closeLedger();
            loadRequests(currentFilter);
        } catch (Exception e) {
            lblActionError.setText("Failed to process rejection.");
            e.printStackTrace();
        }
    }

    @FXML public void handleCancelAction() { closeLedger(); }

    private void closeLedger() {
        selectedRequestId = -1;
        activeRowData = null;
        detailPanel.setVisible(false);
        detailPanel.setManaged(false);
        tblRequests.getSelectionModel().clearSelection();
    }

    private void loadRequests(String statusFilter) {
        String sql = """
                SELECT pur.request_id, s.full_name, pur.field_name,
                       pur.current_value, pur.requested_value, pur.reason,
                       pur.status, pur.admin_response, pur.submitted_at
                FROM profile_update_requests pur
                JOIN students s ON pur.student_id = s.student_id
                """ + (statusFilter != null ? " WHERE pur.status = ?" : "") +
                " ORDER BY pur.submitted_at DESC";

        ObservableList<ObservableList<String>> data = FXCollections.observableArrayList();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (statusFilter != null) ps.setString(1, statusFilter);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String studentName = rs.getString("full_name");
                String field = columnToLabel.getOrDefault(rs.getString("field_name"), rs.getString("field_name"));
                String status = capitalize(rs.getString("status"));
                Timestamp submitted = rs.getTimestamp("submitted_at");
                String submittedStr = submitted != null ? submitted.toLocalDateTime().format(fmt) : "—";

                ObservableList<String> row = FXCollections.observableArrayList(
                        studentName,
                        field, status, submittedStr,
                        String.valueOf(rs.getInt("request_id")),
                        rs.getString("current_value") != null ? rs.getString("current_value") : "—",
                        rs.getString("requested_value") != null ? rs.getString("requested_value") : "—",
                        rs.getString("reason") != null ? rs.getString("reason") : "—",
                        rs.getString("admin_response") != null ? rs.getString("admin_response") : "",
                        rs.getString("status")
                );
                data.add(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        tblRequests.setItems(data);
    }

    @FXML public void handleFilterAll() { setActiveFilter(null); loadRequests(null); }
    @FXML public void handleFilterPending() { setActiveFilter("PENDING"); loadRequests("PENDING"); }
    @FXML public void handleFilterApproved() { setActiveFilter("APPROVED"); loadRequests("APPROVED"); }
    @FXML public void handleFilterRejected() { setActiveFilter("REJECTED"); loadRequests("REJECTED"); }

    private void setActiveFilter(String filter) {
        currentFilter = filter;
        String active = "-fx-background-color: #7A0000; -fx-text-fill: #FFFFFF; -fx-background-radius: 20; -fx-cursor: hand; -fx-font-weight: bold;";
        String inactiveAll = "-fx-background-color: #F3F4F6; -fx-text-fill: #4B5563; -fx-background-radius: 20; -fx-cursor: hand;";
        btnFilterAll.setStyle(filter == null ? active : inactiveAll);
        btnFilterPending.setStyle("PENDING".equals(filter) ? active : "-fx-background-color: #FEF3C7; -fx-text-fill: #92400E; -fx-background-radius: 20; -fx-cursor: hand;");
        btnFilterApproved.setStyle("APPROVED".equals(filter) ? active : "-fx-background-color: #DCFCE7; -fx-text-fill: #15803D; -fx-background-radius: 20; -fx-cursor: hand;");
        btnFilterRejected.setStyle("REJECTED".equals(filter) ? active : "-fx-background-color: #FEE2E2; -fx-text-fill: #B91C1C; -fx-background-radius: 20; -fx-cursor: hand;");
        closeLedger();
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.charAt(0) + s.substring(1).toLowerCase();
    }
}