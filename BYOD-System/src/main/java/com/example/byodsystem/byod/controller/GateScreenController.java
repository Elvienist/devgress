package com.example.byodsystem.byod.controller;

import com.example.byodsystem.byod.database.DBConnection;
import com.example.byodsystem.byod.service.UserSession;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GateScreenController {

    @FXML private Label lblCurrentDate;
    @FXML private Label lblRoleBadge;
    @FXML private Label lblUserInitial;

    @FXML private Button btnModeIngress;
    @FXML private Button btnModeEgress;

    @FXML private TextField txtSearchStudent;
    @FXML private VBox paneSearchResults;

    @FXML private VBox paneNoResultsDefault;
    @FXML private SVGPath svgModeIconNode;
    @FXML private Label lblModeStatus;
    @FXML private Label lblModeHint;

    @FXML private VBox paneNoBatches;
    @FXML private VBox paneBatchList;
    @FXML private StackPane paneBatchCount;
    @FXML private Label lblBatchCount;

    @FXML private StackPane paneDeviceSelectModal;
    @FXML private VBox modalSelectContainer;
    @FXML private Label lblModalBadge;
    @FXML private Label lblModalStudentName;
    @FXML private Label lblModalStudentMeta;
    @FXML private Label lblModalSectionTitle;
    @FXML private VBox paneDeviceCheckList;
    @FXML private Label lblSelectedCount;
    @FXML private Button btnReviewSelection;
    @FXML private Region profileDividerLine1;

    @FXML private StackPane paneConfirmModal;
    @FXML private VBox modalConfirmContainer;
    @FXML private Label lblConfirmBadge;
    @FXML private Label lblConfirmStudentName;
    @FXML private Label lblConfirmStudentMeta;
    @FXML private Label lblConfirmInnerName;
    @FXML private Label lblConfirmInnerCode;
    @FXML private Label lblConfirmFor;
    @FXML private VBox paneConfirmDeviceList;
    @FXML private Label lblConfirmWarning;
    @FXML private Button btnConfirmAction;
    @FXML private Region profileDividerLine2;

    @FXML private StackPane paneAckModal;

    private boolean isIngressMode = true;

    private int currentStudentId = -1;
    private String currentStudentCode = "";
    private String currentStudentName = "";
    private String currentStudentMeta = "";

    private final Map<Integer, String> selectedDevices = new LinkedHashMap<>();

    @FXML
    public void initialize() {
        UserSession session = UserSession.getInstance();
        String role = session.getRole() != null ? session.getRole().toUpperCase() : "ADMIN";

        lblRoleBadge.setText(role.charAt(0) + role.substring(1).toLowerCase());
        String username = session.getUsername();
        lblUserInitial.setText(username != null && !username.isEmpty() ? username.substring(0, 1).toUpperCase() : "G");

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");
        lblCurrentDate.setText(LocalDateTime.now().format(fmt));

        txtSearchStudent.textProperty().addListener((obs, oldVal, newVal) -> handleSearch(newVal));

        updateModeUI();
        loadRecentBatches();
    }

    @FXML
    public void handleSwitchToIngress() {
        isIngressMode = true;
        clearSearch();
        updateModeUI();
    }

    @FXML
    public void handleSwitchToEgress() {
        isIngressMode = false;
        clearSearch();
        updateModeUI();
    }

    private void clearSearch() {
        txtSearchStudent.clear();
        paneSearchResults.getChildren().clear();
    }

    private void updateModeUI() {
        if (isIngressMode) {
            btnModeIngress.setStyle("-fx-background-color: #7A0000; -fx-background-radius: 20; -fx-border-radius: 20; -fx-cursor: hand; -fx-text-fill: #FFFFFF; -fx-font-family: 'Inter', 'Segoe UI'; -fx-font-weight: bold; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 4, 0, 0, 2);");
            SVGPath ingIcon = (SVGPath) btnModeIngress.getGraphic();
            if (ingIcon != null) ingIcon.setStyle("-fx-stroke: #FFFFFF;");

            btnModeEgress.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 20; -fx-border-radius: 20; -fx-border-color: #E2E8F0; -fx-border-width: 1.5; -fx-cursor: hand; -fx-text-fill: #64748B; -fx-font-family: 'Inter', 'Segoe UI'; -fx-font-weight: bold;");
            SVGPath egIcon = (SVGPath) btnModeEgress.getGraphic();
            if (egIcon != null) egIcon.setStyle("-fx-stroke: #64748B;");

            svgModeIconNode.setContent("M14 5l7 7m0 0l-7 7m7-7H3");
            svgModeIconNode.setStyle("-fx-stroke: #7A0000;");
            svgModeIconNode.getParent().setStyle("-fx-background-color: #FEE2E2; -fx-background-radius: 12; -fx-alignment: center;");

            lblModeStatus.setText("Ingress Mode Active");
            lblModeHint.setText("Search for a student above to open the device selection window and process check-in.");
        } else {
            btnModeEgress.setStyle("-fx-background-color: #E6A100; -fx-background-radius: 20; -fx-border-radius: 20; -fx-cursor: hand; -fx-text-fill: #FFFFFF; -fx-font-family: 'Inter', 'Segoe UI'; -fx-font-weight: bold; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 4, 0, 0, 2);");
            SVGPath egIcon = (SVGPath) btnModeEgress.getGraphic();
            if (egIcon != null) egIcon.setStyle("-fx-stroke: #FFFFFF;");

            btnModeIngress.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 20; -fx-border-radius: 20; -fx-border-color: #E2E8F0; -fx-border-width: 1.5; -fx-cursor: hand; -fx-text-fill: #64748B; -fx-font-family: 'Inter', 'Segoe UI'; -fx-font-weight: bold;");
            SVGPath ingIcon = (SVGPath) btnModeIngress.getGraphic();
            if (ingIcon != null) ingIcon.setStyle("-fx-stroke: #64748B;");

            svgModeIconNode.setContent("M10 19l-7-7m0 0l7-7m-7 7h18");
            svgModeIconNode.setStyle("-fx-stroke: #E6A100;");
            svgModeIconNode.getParent().setStyle("-fx-background-color: #FEF3C7; -fx-background-radius: 12; -fx-alignment: center;");

            lblModeStatus.setText("Egress Mode Active");
            lblModeHint.setText("Search for a student above to open the device selection window and process check-out.");
        }
    }

    private void handleSearch(String query) {
        paneSearchResults.getChildren().clear();
        if (query == null || query.trim().isEmpty()) {
            paneNoResultsDefault.setVisible(true);
            paneNoResultsDefault.setManaged(true);
            return;
        }

        paneNoResultsDefault.setVisible(false);
        paneNoResultsDefault.setManaged(false);

        String lower = "%" + query.trim().toLowerCase() + "%";
        String sql = "SELECT student_id, student_code, full_name, course, year_level FROM students " +
                "WHERE status = 'ACTIVE' AND (LOWER(student_code) LIKE ? OR LOWER(full_name) LIKE ?) " +
                "ORDER BY full_name ASC LIMIT 5";

        try (Connection conn = DBConnection.connect();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, lower);
            pst.setString(2, lower);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    int studentId = rs.getInt("student_id");
                    String code = rs.getString("student_code");
                    String name = rs.getString("full_name");
                    String course = rs.getString("course");
                    String year = rs.getString("year_level");
                    paneSearchResults.getChildren().add(buildResultRow(studentId, code, name, course, year));
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private HBox buildResultRow(int studentId, String code, String name, String course, String year) {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 10; -fx-border-color: #E2E8F0; -fx-border-radius: 10; -fx-border-width: 1; -fx-cursor: hand; -fx-padding: 14 20;");

        StackPane iconCircle = new StackPane();
        iconCircle.setMaxSize(36, 36);
        iconCircle.setMinSize(36, 36);
        iconCircle.setStyle("-fx-background-color: #F1F5F9; -fx-background-radius: 50;");
        SVGPath userSvg = new SVGPath();
        userSvg.setContent("M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z");
        userSvg.setStyle("-fx-stroke: #64748B; -fx-fill: transparent; -fx-stroke-width: 1.8;");
        iconCircle.getChildren().add(userSvg);

        VBox info = new VBox(2);
        Label lblName = new Label(name);
        lblName.setStyle("-fx-font-family: 'Inter', 'Segoe UI'; -fx-font-weight: bold; -fx-text-fill: #1E293B; -fx-font-size: 14px;");
        Label lblMeta = new Label(code + "  •  " + course + " " + year);
        lblMeta.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12px;");
        info.getChildren().addAll(lblName, lblMeta);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        SVGPath chevron = new SVGPath();
        chevron.setContent("M9 5l7 7-7 7");
        chevron.setStyle("-fx-stroke: #94A3B8; -fx-stroke-width: 2; -fx-fill: transparent; -fx-stroke-linecap: round;");

        row.getChildren().addAll(iconCircle, info, spacer, chevron);

        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color: #F8FAFC; -fx-background-radius: 10; -fx-border-color: #CBD5E1; -fx-border-radius: 10; -fx-border-width: 1; -fx-cursor: hand; -fx-padding: 14 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.02), 8, 0, 0, 2);"));
        row.setOnMouseExited(e -> row.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 10; -fx-border-color: #E2E8F0; -fx-border-radius: 10; -fx-border-width: 1; -fx-cursor: hand; -fx-padding: 14 20;"));

        row.setOnMouseClicked(e -> openDeviceSelection(studentId, code, name, course + " " + year));
        return row;
    }

    private void openDeviceSelection(int studentId, String code, String name, String meta) {
        currentStudentId = studentId;
        currentStudentCode = code;
        currentStudentName = name;
        currentStudentMeta = code + " · " + meta;
        selectedDevices.clear();

        lblModalStudentName.setText(currentStudentName);
        lblModalStudentMeta.setText(currentStudentMeta);

        if (profileDividerLine1 != null) {
            profileDividerLine1.setStyle("-fx-background-color: #1E293B; -fx-min-height: 2; -fx-max-height: 2;");
        }

        if (isIngressMode) {
            modalSelectContainer.setStyle("-fx-background-color: #7A0000, #FFFFFF; -fx-background-insets: 0, 4 0 0 0; -fx-background-radius: 12; -fx-padding: 0;");
            lblModalBadge.setText("INGRESS");
            lblModalBadge.setStyle("-fx-background-color: #7A0000; -fx-text-fill: #FFFFFF; -fx-background-radius: 12; -fx-padding: 4 14; -fx-font-family: 'Inter', 'Segoe UI'; -fx-font-weight: bold; -fx-font-size: 11px;");
            btnReviewSelection.setStyle("-fx-background-color: #7A0000; -fx-background-radius: 8; -fx-cursor: hand; -fx-text-fill: white; -fx-padding: 0 24; -fx-font-weight: bold;");
        } else {
            modalSelectContainer.setStyle("-fx-background-color: #E6A100, #FFFFFF; -fx-background-insets: 0, 4 0 0 0; -fx-background-radius: 12; -fx-padding: 0;");
            lblModalBadge.setText("EGRESS");
            lblModalBadge.setStyle("-fx-background-color: #E6A100; -fx-text-fill: #FFFFFF; -fx-background-radius: 12; -fx-padding: 4 14; -fx-font-family: 'Inter', 'Segoe UI'; -fx-font-weight: bold; -fx-font-size: 11px;");
            btnReviewSelection.setStyle("-fx-background-color: #E6A100; -fx-background-radius: 8; -fx-cursor: hand; -fx-text-fill: white; -fx-padding: 0 24; -fx-font-weight: bold;");
        }

        loadEligibleDevices(studentId);

        paneDeviceSelectModal.setVisible(true);
        paneDeviceSelectModal.setManaged(true);
    }

    private void loadEligibleDevices(int studentId) {
        paneDeviceCheckList.getChildren().clear();
        selectedDevices.clear();

        String sql;
        if (isIngressMode) {
            // Eligible for ingress: device isn't currently marked as already inside.
            // current_location is kept accurate by ActivityLogController whenever a
            // log is voided/un-voided, so we trust it directly instead of re-deriving
            // in/out state from raw device_logs (which would have to remember to
            // exclude VOIDED rows every time).
            sql = "SELECT d.device_id, d.serial_number, d.brand, d.model, d.device_type, " +
                    "COALESCE(d.current_location, 'UNKNOWN') AS current_location " +
                    "FROM devices d " +
                    "WHERE d.owner_id = ? AND d.status = 'ACTIVE' " +
                    "AND COALESCE(d.current_location, 'UNKNOWN') != 'IN' " +
                    "ORDER BY d.device_id ASC";
        } else {
            // Eligible for egress: device must currently be marked inside.
            sql = "SELECT d.device_id, d.serial_number, d.brand, d.model, d.device_type, " +
                    "COALESCE(d.current_location, 'UNKNOWN') AS current_location " +
                    "FROM devices d " +
                    "WHERE d.owner_id = ? AND d.status = 'ACTIVE' " +
                    "AND COALESCE(d.current_location, 'UNKNOWN') = 'IN' " +
                    "ORDER BY d.device_id ASC";
        }

        int eligibleCount = 0;
        try (Connection conn = DBConnection.connect();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, studentId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    int deviceId = rs.getInt("device_id");
                    String serial = rs.getString("serial_number");
                    String brand = rs.getString("brand");
                    String model = rs.getString("model");
                    String type = rs.getString("device_type");
                    String location = rs.getString("current_location");
                    paneDeviceCheckList.getChildren().add(buildDeviceRow(deviceId, serial, brand, model, type, location));
                    eligibleCount++;
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }

        if (eligibleCount == 0) {
            Label lblEmpty = new Label(isIngressMode
                    ? "No registered devices are currently outside the campus."
                    : "No devices are currently inside the campus to clear.");
            lblEmpty.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 13px; -fx-alignment: center; -fx-padding: 30 10; -fx-font-style: italic;");
            lblEmpty.setWrapText(true);
            paneDeviceCheckList.getChildren().add(lblEmpty);
        }

        lblModalSectionTitle.setText((isIngressMode ? "SELECT DEVICES TO BRING IN — " : "SELECT DEVICES TO BRING OUT — ") + eligibleCount + " ELIGIBLE");
        updateSelectionCount();
    }

    private HBox buildDeviceRow(int deviceId, String serial, String brand, String model, String type, String location) {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 10; -fx-border-color: #E2E8F0; -fx-border-radius: 10; -fx-border-width: 1; -fx-cursor: hand; -fx-padding: 14;");

        StackPane iconBox = new StackPane();
        iconBox.setMaxSize(40, 40);
        iconBox.setMinSize(40, 40);

        String brandingColor = isIngressMode ? "#7A0000" : "#E6A100";
        iconBox.setStyle("-fx-background-color: " + brandingColor + "; -fx-background-radius: 8;");

        SVGPath devSvg = new SVGPath();
        if ("LAPTOP".equalsIgnoreCase(type)) {
            devSvg.setContent("M4 6h16M4 10h16M4 14h16M2 18h20a1 1 0 011 1v1H1v-1a1 1 0 011-1z");
        } else {
            devSvg.setContent("M12 18h.01M8 21h8a2 2 0 002-2V5a2 2 0 00-2-2H8a2 2 0 00-2 2v14a2 2 0 002 2z");
        }
        devSvg.setStyle("-fx-stroke: #FFFFFF; -fx-fill: transparent; -fx-stroke-width: 1.5; -fx-stroke-linecap: round;");
        iconBox.getChildren().add(devSvg);

        VBox info = new VBox(2);
        Label lblTitle = new Label(brand + " " + model);
        lblTitle.setStyle("-fx-font-family: 'Inter', 'Segoe UI'; -fx-font-weight: 600; -fx-text-fill: #1E293B; -fx-font-size: 14px;");
        Label lblType = new Label(type.substring(0, 1).toUpperCase() + type.substring(1).toLowerCase());
        lblType.setStyle("-fx-font-family: 'Inter'; -fx-text-fill: #64748B; -fx-font-size: 12px;");
        Label lblSerialNumberString = new Label("S/N: " + serial);
        lblSerialNumberString.setStyle("-fx-font-family: 'JetBrains Mono', 'Courier New'; -fx-font-size: 11px; -fx-text-fill: #94A3B8;");
        info.getChildren().addAll(lblTitle, lblType, lblSerialNumberString);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        StackPane customCheck = new StackPane();
        customCheck.setMaxSize(22, 22);
        customCheck.setMinSize(22, 22);
        customCheck.setStyle("-fx-background-color: transparent; -fx-border-color: #CBD5E1; -fx-border-radius: 50; -fx-background-radius: 50; -fx-border-width: 2;");

        String deviceLabel = brand + " " + model;

        HBox badgeContainer = new HBox(10);
        badgeContainer.setAlignment(Pos.CENTER_LEFT);

        String locationText;
        String locationStyle;
        if ("IN".equals(location)) {
            locationText = "📍 Inside Campus";
            locationStyle = "-fx-background-color: #DBEAFE; -fx-text-fill: #1E40AF; -fx-background-radius: 12; -fx-padding: 4 12; -fx-font-weight: bold; -fx-font-size: 11px;";
        } else if ("OUT".equals(location)) {
            locationText = "📍 Outside Campus";
            locationStyle = "-fx-background-color: #DCFCE7; -fx-text-fill: #15803D; -fx-background-radius: 12; -fx-padding: 4 12; -fx-font-weight: bold; -fx-font-size: 11px;";
        } else {
            locationText = "📍 Location Unknown";
            locationStyle = "-fx-background-color: #F3F4F6; -fx-text-fill: #6B7280; -fx-background-radius: 12; -fx-padding: 4 12; -fx-font-weight: bold; -fx-font-size: 11px;";
        }

        Label lblLocationBadge = new Label(locationText);
        lblLocationBadge.setStyle(locationStyle);

        badgeContainer.getChildren().addAll(lblLocationBadge, customCheck);

        row.setOnMouseClicked(e -> {
            if (selectedDevices.containsKey(deviceId)) {
                selectedDevices.remove(deviceId);
                customCheck.setStyle("-fx-background-color: transparent; -fx-border-color: #CBD5E1; -fx-border-radius: 50; -fx-background-radius: 50; -fx-border-width: 2;");
                customCheck.getChildren().clear();
                row.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 10; -fx-border-color: #E2E8F0; -fx-border-radius: 10; -fx-border-width: 1; -fx-cursor: hand; -fx-padding: 14;");
            } else {
                selectedDevices.put(deviceId, deviceLabel + " (S/N: " + serial + ")");
                customCheck.setStyle("-fx-background-color: " + brandingColor + "; -fx-border-color: " + brandingColor + "; -fx-border-radius: 50; -fx-background-radius: 50;");

                SVGPath whiteCheck = new SVGPath();
                whiteCheck.setContent("M5 13l4 4L19 7");
                whiteCheck.setStyle("-fx-stroke: #FFFFFF; -fx-stroke-width: 2; -fx-fill: transparent; -fx-stroke-linecap: round; -fx-stroke-linejoin: round; -fx-scale-x: 0.5; -fx-scale-y: 0.5;");
                customCheck.getChildren().add(whiteCheck);

                row.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 10; -fx-border-color: " + brandingColor + "; -fx-border-radius: 10; -fx-border-width: 1.5; -fx-cursor: hand; -fx-padding: 14;");
            }
            updateSelectionCount();
        });

        row.getChildren().addAll(iconBox, info, spacer, badgeContainer);
        return row;
    }

    private void updateSelectionCount() {
        int count = selectedDevices.size();
        lblSelectedCount.setText(count + (count == 1 ? " device selected" : " devices selected"));
        btnReviewSelection.setDisable(count == 0);
    }

    @FXML
    public void handleReviewSelection() {
        if (selectedDevices.isEmpty()) return;

        lblConfirmStudentName.setText(currentStudentName);
        lblConfirmInnerName.setText(currentStudentName);
        lblConfirmInnerCode.setText("# " + currentStudentCode);
        lblConfirmStudentMeta.setText(currentStudentMeta);

        if (profileDividerLine2 != null) {
            profileDividerLine2.setStyle("-fx-background-color: #1E293B; -fx-min-height: 2; -fx-max-height: 2;");
        }

        if (isIngressMode) {
            modalConfirmContainer.setStyle("-fx-background-color: #7A0000, #FFFFFF; -fx-background-insets: 0, 4 0 0 0; -fx-background-radius: 12; -fx-padding: 0;");
            lblConfirmBadge.setText("INGRESS");
            lblConfirmBadge.setStyle("-fx-background-color: #7A0000; -fx-text-fill: #FFFFFF; -fx-background-radius: 12; -fx-padding: 4 14; -fx-font-family: 'Inter', 'Segoe UI'; -fx-font-weight: bold; -fx-font-size: 11px;");
            lblConfirmFor.setText("CONFIRM INGRESS FOR:");
            btnConfirmAction.setText("CONFIRM INGRESS");
            btnConfirmAction.setStyle("-fx-background-color: #7A0000; -fx-background-radius: 8; -fx-cursor: hand; -fx-text-fill: white; -fx-padding: 0 24; -fx-font-family: 'Inter', 'Segoe UI'; -fx-font-weight: bold;");
        } else {
            modalConfirmContainer.setStyle("-fx-background-color: #E6A100, #FFFFFF; -fx-background-insets: 0, 4 0 0 0; -fx-background-radius: 12; -fx-padding: 0;");
            lblConfirmBadge.setText("EGRESS");
            lblConfirmBadge.setStyle("-fx-background-color: #E6A100; -fx-text-fill: #FFFFFF; -fx-background-radius: 12; -fx-padding: 4 14; -fx-font-family: 'Inter', 'Segoe UI'; -fx-font-weight: bold; -fx-font-size: 11px;");
            lblConfirmFor.setText("CONFIRM EGRESS FOR:");
            btnConfirmAction.setText("CONFIRM EGRESS");
            btnConfirmAction.setStyle("-fx-background-color: #E6A100; -fx-background-radius: 8; -fx-cursor: hand; -fx-text-fill: white; -fx-padding: 0 24; -fx-font-family: 'Inter', 'Segoe UI'; -fx-font-weight: bold;");
        }

        paneConfirmDeviceList.getChildren().clear();
        String activeBranding = isIngressMode ? "#7A0000" : "#E6A100";

        for (Map.Entry<Integer, String> entry : selectedDevices.entrySet()) {
            String label = entry.getValue();
            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 8; -fx-border-color: #E2E8F0; -fx-border-width: 1; -fx-padding: 12 16;");

            StackPane iconBox = new StackPane();
            iconBox.setMaxSize(32, 32);
            iconBox.setMinSize(32, 32);
            iconBox.setStyle("-fx-background-color: " + activeBranding + "; -fx-background-radius: 6;");

            SVGPath devSvg = new SVGPath();
            devSvg.setContent("M4 6h16M4 10h16M4 14h16M4 18h16");
            devSvg.setStyle("-fx-stroke: #FFFFFF; -fx-stroke-width: 1.5; -fx-stroke-linecap: round; -fx-fill: transparent;");
            iconBox.getChildren().add(devSvg);

            VBox nameContainer = new VBox(2);
            int openParenIdx = label.indexOf(" (");
            String mainTitle = openParenIdx != -1 ? label.substring(0, openParenIdx) : label;
            String subSerial = openParenIdx != -1 ? label.substring(openParenIdx + 2, label.length() - 1) : "";

            Label lblDev = new Label(mainTitle);
            lblDev.setStyle("-fx-font-family: 'Inter', 'Segoe UI'; -fx-font-weight: 600; -fx-text-fill: #1E293B; -fx-font-size: 13px;");
            nameContainer.getChildren().add(lblDev);
            if (!subSerial.isEmpty()) {
                Label lblSerStr = new Label(subSerial);
                lblSerStr.setStyle("-fx-font-family: 'JetBrains Mono', 'Courier New'; -fx-font-size: 11px; -fx-text-fill: #64748B;");
                nameContainer.getChildren().add(lblSerStr);
            }

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            String newLocationText = isIngressMode ? "→ Inside Campus" : "← Outside Campus";
            String newLocationStyle = isIngressMode
                    ? "-fx-background-color: #DBEAFE; -fx-text-fill: #1E40AF; -fx-padding: 4 10; -fx-background-radius: 6; -fx-font-size: 11px; -fx-font-weight: bold;"
                    : "-fx-background-color: #DCFCE7; -fx-text-fill: #15803D; -fx-padding: 4 10; -fx-background-radius: 6; -fx-font-size: 11px; -fx-font-weight: bold;";

            Label lblNewLoc = new Label(newLocationText);
            lblNewLoc.setStyle(newLocationStyle);

            row.getChildren().addAll(iconBox, nameContainer, spacer, lblNewLoc);
            paneConfirmDeviceList.getChildren().add(row);
        }

        lblConfirmWarning.setText("Confirm " + (isIngressMode ? "ingress" : "egress") + " of " + selectedDevices.size() + (selectedDevices.size() == 1 ? " device " : " devices ") + "for " + currentStudentName + "? This action will be logged.");

        paneDeviceSelectModal.setVisible(false);
        paneDeviceSelectModal.setManaged(false);
        paneConfirmModal.setVisible(true);
        paneConfirmModal.setManaged(true);
    }

    @FXML
    public void handleBackToSelection() {
        paneConfirmModal.setVisible(false);
        paneConfirmModal.setManaged(false);
        paneDeviceSelectModal.setVisible(true);
        paneDeviceSelectModal.setManaged(true);
    }

    @FXML
    public void handleConfirmAction() {
        if (selectedDevices.isEmpty()) return;

        UserSession session = UserSession.getInstance();
        int operatorId = session.getUserId();
        String direction = isIngressMode ? "IN" : "OUT";
        String newLocation = isIngressMode ? "IN" : "OUT";
        UUID batchId = UUID.randomUUID();

        List<Integer> confirmedDeviceIds = new ArrayList<>(selectedDevices.keySet());
        int deviceCount = confirmedDeviceIds.size();
        String actionLabel = isIngressMode ? "Ingress" : "Egress";
        String studentSnapshot = currentStudentName;

        String insertLogSql = "INSERT INTO device_logs (device_id, operator_id, direction, log_time, status, batch_id) " +
                "VALUES (?, ?, ?, NOW() AT TIME ZONE 'Asia/Manila', 'NORMAL', ?)";
        String updateLocationSql = "UPDATE devices SET current_location = ? WHERE device_id = ?";

        try (Connection conn = DBConnection.connect()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement pstLog = conn.prepareStatement(insertLogSql);
                     PreparedStatement pstLoc = conn.prepareStatement(updateLocationSql)) {
                    for (Integer deviceId : confirmedDeviceIds) {
                        pstLog.setInt(1, deviceId);
                        pstLog.setInt(2, operatorId);
                        pstLog.setString(3, direction);
                        pstLog.setObject(4, batchId);
                        pstLog.addBatch();

                        pstLoc.setString(1, newLocation);
                        pstLoc.setInt(2, deviceId);
                        pstLoc.addBatch();
                    }
                    pstLog.executeBatch();
                    pstLoc.executeBatch();
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
                showAckPopup(false, "Transaction Failed",
                        "Could not save the gate action. Please try again.");
                return;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAckPopup(false, "Connection Error",
                    "Database connection failed. Please check your connection.");
            return;
        }

        insertAuditLog(operatorId, batchId, direction);

        handleCloseModal();
        clearSearch();
        loadRecentBatches();

        showAckPopup(true,
                actionLabel + " Logged",
                deviceCount + (deviceCount == 1 ? " device" : " devices") + " for " + studentSnapshot + " successfully recorded.");
    }

    private void insertAuditLog(int operatorId, UUID batchId, String direction) {
        String sql = "INSERT INTO audit_log (operator_id, action_type, target_type, target_id, details, performed_at) " +
                "VALUES (?, 'RECORD_CREATED', 'device_logs', ?, ?::jsonb, NOW() AT TIME ZONE 'Asia/Manila')";
        String details = "{\"batch_id\":\"" + batchId + "\",\"direction\":\"" + direction + "\",\"student_id\":" + currentStudentId
                + ",\"device_count\":" + selectedDevices.size() + "}";
        try (Connection conn = DBConnection.connect();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, operatorId);
            pst.setInt(2, currentStudentId);
            pst.setString(3, details);
            pst.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @FXML
    public void handleCloseModal() {
        paneDeviceSelectModal.setVisible(false);
        paneDeviceSelectModal.setManaged(false);
        paneConfirmModal.setVisible(false);
        paneConfirmModal.setManaged(false);
        selectedDevices.clear();
    }

    private void showAckPopup(boolean success, String title, String message) {
        if (paneAckModal == null) {
            System.err.println("Error: paneAckModal is null. Ensure fx:id='paneAckModal' is configured correctly in gatescreen.fxml");
            return;
        }

        paneAckModal.getChildren().clear();

        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.35);");
        overlay.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        VBox card = new VBox(20);
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(340);
        card.setMaxHeight(Region.USE_PREF_SIZE);
        card.setMinHeight(Region.USE_PREF_SIZE);
        card.setPadding(new Insets(36, 32, 32, 32));
        card.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 16; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.18), 24, 0, 0, 6);");

        StackPane iconCircle = new StackPane();
        iconCircle.setMaxSize(64, 64);
        iconCircle.setMinSize(64, 64);

        if (success) {
            iconCircle.setStyle("-fx-background-color: #DCFCE7; -fx-background-radius: 50; " +
                    "-fx-border-color: #BBF7D0; -fx-border-radius: 50; -fx-border-width: 3;");
            SVGPath checkIcon = new SVGPath();
            checkIcon.setContent("M5 13l4 4L19 7");
            checkIcon.setStyle("-fx-stroke: #16A34A; -fx-stroke-width: 2.5; -fx-fill: transparent; " +
                    "-fx-stroke-linecap: round; -fx-stroke-linejoin: round; -fx-scale-x: 1.3; -fx-scale-y: 1.3;");
            iconCircle.getChildren().add(checkIcon);
        } else {
            iconCircle.setStyle("-fx-background-color: #FEE2E2; -fx-background-radius: 50; " +
                    "-fx-border-color: #FECACA; -fx-border-radius: 50; -fx-border-width: 3;");
            SVGPath xIcon = new SVGPath();
            xIcon.setContent("M6 18L18 6M6 6l12 12");
            xIcon.setStyle("-fx-stroke: #DC2626; -fx-stroke-width: 2.5; -fx-fill: transparent; " +
                    "-fx-stroke-linecap: round; -fx-scale-x: 1.1; -fx-scale-y: 1.1;");
            iconCircle.getChildren().add(xIcon);
        }

        Label lblTitle = new Label(title);
        lblTitle.setStyle("-fx-font-family: 'Inter', 'Segoe UI'; -fx-font-weight: bold; " +
                "-fx-text-fill: #111827; -fx-font-size: 18px;");

        Label lblMessage = new Label(message);
        lblMessage.setStyle("-fx-font-family: 'Inter', 'Segoe UI'; -fx-text-fill: #6B7280; " +
                "-fx-font-size: 13px; -fx-text-alignment: center;");
        lblMessage.setWrapText(true);
        lblMessage.setMaxWidth(260);
        lblMessage.setAlignment(Pos.CENTER);

        String btnColor = success ? "#16A34A" : "#DC2626";
        Button btnClose = new Button("Close");
        btnClose.setMaxWidth(Double.MAX_VALUE);
        btnClose.setPrefHeight(42);
        btnClose.setStyle("-fx-background-color: " + btnColor + "; -fx-background-radius: 8; " +
                "-fx-text-fill: #FFFFFF; -fx-font-family: 'Inter', 'Segoe UI'; " +
                "-fx-font-weight: bold; -fx-font-size: 14px; -fx-cursor: hand;");
        btnClose.setOnMouseEntered(e -> btnClose.setOpacity(0.88));
        btnClose.setOnMouseExited(e -> btnClose.setOpacity(1.0));
        btnClose.setOnAction(e -> hideAckPopup());

        card.getChildren().addAll(iconCircle, lblTitle, lblMessage, btnClose);

        overlay.getChildren().add(card);
        StackPane.setAlignment(card, Pos.CENTER);
        paneAckModal.getChildren().add(overlay);

        paneAckModal.setVisible(true);
        paneAckModal.setManaged(true);
        paneAckModal.setOpacity(0);

        card.setScaleX(0.85);
        card.setScaleY(0.85);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(180), paneAckModal);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(200), card);
        scaleIn.setFromX(0.85);
        scaleIn.setFromY(0.85);
        scaleIn.setToX(1.0);
        scaleIn.setToY(1.0);

        fadeIn.play();
        scaleIn.play();
    }

    private void hideAckPopup() {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(150), paneAckModal);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> {
            paneAckModal.setVisible(false);
            paneAckModal.setManaged(false);
            paneAckModal.getChildren().clear();
        });
        fadeOut.play();
    }

    private void loadRecentBatches() {
        paneBatchList.getChildren().clear();

        String sql = "SELECT dl.batch_id, dl.direction, s.student_code, s.full_name, " +
                "MAX(dl.log_time) AS batch_time, COUNT(*) AS device_count, " +
                "STRING_AGG(d.brand || ' ' || d.model, ', ') AS device_names, " +
                "CASE WHEN BOOL_OR(dl.status = 'VOIDED') THEN 'VOIDED' " +
                "     WHEN BOOL_OR(dl.status = 'AMENDED') THEN 'AMENDED' " +
                "     ELSE 'NORMAL' END AS batch_status " +
                "FROM device_logs dl " +
                "JOIN devices d ON d.device_id = dl.device_id " +
                "JOIN students s ON s.student_id = d.owner_id " +
                "GROUP BY dl.batch_id, dl.direction, s.student_code, s.full_name " +
                "ORDER BY batch_time DESC LIMIT 8";

        List<BatchRow> batches = new ArrayList<>();
        int streamCounter = 0;

        try (Connection conn = DBConnection.connect();
             PreparedStatement pst = conn.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
                BatchRow b = new BatchRow();
                b.studentCode = rs.getString("student_code");
                b.studentName = rs.getString("full_name");
                b.direction = rs.getString("direction");
                b.deviceCount = rs.getInt("device_count");
                b.deviceNames = rs.getString("device_names");
                b.status = rs.getString("batch_status");
                Timestamp ts = rs.getTimestamp("batch_time");
                b.time = ts != null ? ts.toInstant()
                                      .atZone(java.time.ZoneId.of("Asia/Manila"))
                                      .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")) : "";
                batches.add(b);
                streamCounter++;
            }
        } catch (SQLException e) { e.printStackTrace(); }

        lblBatchCount.setText(streamCounter + " Today");

        if (batches.isEmpty()) {
            paneNoBatches.setVisible(true);
            paneNoBatches.setManaged(true);
            return;
        }

        paneNoBatches.setVisible(false);
        paneNoBatches.setManaged(false);

        for (BatchRow b : batches) {
            paneBatchList.getChildren().add(buildBatchCard(b));
        }
    }

    private VBox buildBatchCard(BatchRow b) {
        VBox card = new VBox(6);
        card.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 10; -fx-border-color: #E5E7EB; -fx-border-radius: 10; -fx-border-width: 1; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.01), 6, 0, 0, 1);");
        card.setPadding(new Insets(14, 16, 14, 16));
        if ("VOIDED".equals(b.status)) {
            card.setOpacity(0.6);
        }

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        Label lblName = new Label(b.studentName);
        lblName.setStyle("-fx-font-family: 'Inter'; -fx-font-weight: 700; -fx-text-fill: #111827; -fx-font-size: 13px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label lblDir = new Label("IN".equals(b.direction) ? "INGRESS" : "EGRESS");
        lblDir.setStyle("IN".equals(b.direction)
                ? "-fx-background-color: #DCFCE7; -fx-text-fill: #15803D; -fx-padding: 3 8; -fx-background-radius: 6; -fx-font-size: 10px; -fx-font-weight: 700;"
                : "-fx-background-color: #FEF3C7; -fx-text-fill: #92400E; -fx-padding: 3 8; -fx-background-radius: 6; -fx-font-size: 10px; -fx-font-weight: 700;");

        header.getChildren().addAll(lblName, spacer, lblDir);

        if ("VOIDED".equals(b.status) || "AMENDED".equals(b.status)) {
            Label lblStatus = new Label(b.status);
            lblStatus.setStyle("VOIDED".equals(b.status)
                    ? "-fx-background-color: #FEE2E2; -fx-text-fill: #B91C1C; -fx-padding: 3 8; -fx-background-radius: 6; -fx-font-size: 10px; -fx-font-weight: 700;"
                    : "-fx-background-color: #FEF9C3; -fx-text-fill: #854D0E; -fx-padding: 3 8; -fx-background-radius: 6; -fx-font-size: 10px; -fx-font-weight: 700;");
            header.getChildren().add(lblStatus);
        }

        Label lblMeta = new Label(b.studentCode + "  •  " + b.deviceCount + " Asset Module(s)  •  " + b.time);
        lblMeta.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 11px; -fx-font-weight: 500;");

        Label lblDevices = new Label(b.deviceNames);
        lblDevices.setStyle("-fx-text-fill: #4B5563; -fx-font-size: 11px; -fx-font-style: italic;");
        lblDevices.setWrapText(true);

        card.getChildren().addAll(header, lblMeta, lblDevices);
        return card;
    }

    private static class BatchRow {
        String studentCode;
        String studentName;
        String direction;
        int deviceCount;
        String deviceNames;
        String status;
        String time;
    }
}