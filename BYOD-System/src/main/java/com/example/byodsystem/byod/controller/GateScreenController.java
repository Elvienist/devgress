package com.example.byodsystem.byod.controller;

import com.example.byodsystem.byod.database.DBConnection;
import com.example.byodsystem.byod.service.EmailService;
import com.example.byodsystem.byod.service.EmailService.DeviceEntry;
import com.example.byodsystem.byod.service.UserSession;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class GateScreenController {

    @FXML private Label lblCurrentDate;
    @FXML private Label lblRoleBadge;
    @FXML private Label lblUserInitial;

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

    @FXML private StackPane paneScheduleModal;
    @FXML private Label lblScheduleBadge;
    @FXML private VBox paneScheduleStudentSummary;
    @FXML private Label lblScheduleSummaryName;
    @FXML private Label lblScheduleSummaryDevice;
    @FXML private DatePicker dpIngressDate;
    @FXML private ComboBox<String> cbIngressHour;
    @FXML private ComboBox<String> cbIngressMinute;
    @FXML private DatePicker dpEgressDate;
    @FXML private ComboBox<String> cbEgressHour;
    @FXML private ComboBox<String> cbEgressMinute;
    @FXML private Label lblScheduleError;
    @FXML private Label lblClosingTimeWarning;
    @FXML private Button btnSubmitSchedule;

    @FXML private StackPane paneAckModal;
    @FXML private StackPane paneRegistrationModal;
    @FXML private StackPane paneQuickLogModal;

    private int    currentStudentId   = -1;
    private String currentStudentCode = "";
    private String currentStudentName = "";
    private String currentStudentMeta = "";
    private String currentStudentEmail= "";
    private String currentStudentCourse="";
    private String currentStudentYear = "";

    private final Map<Integer, SelectedDevice> selectedDevices = new LinkedHashMap<>();

    private static class SelectedDevice {
        final String brand, model, serial, deviceType;
        SelectedDevice(String brand, String model, String serial, String deviceType) {
            this.brand = brand; this.model = model;
            this.serial = serial; this.deviceType = deviceType;
        }
        String displayName() { return brand + " " + model; }
    }

    private static final DateTimeFormatter DATE_FMT    = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");
    private static final DateTimeFormatter TIME_FMT    = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy hh:mm a");

    @FXML
    public void initialize() {
        UserSession session = UserSession.getInstance();
        String role = session.getRole() != null ? session.getRole().toUpperCase() : "ADMIN";
        lblRoleBadge.setText(role.charAt(0) + role.substring(1).toLowerCase());
        String username = session.getUsername();
        lblUserInitial.setText(username != null && !username.isEmpty()
                ? username.substring(0, 1).toUpperCase() : "G");

        lblCurrentDate.setText(LocalDateTime.now().format(DATE_FMT));
        txtSearchStudent.textProperty().addListener((obs, o, n) -> handleSearch(n));

        initializeTimeComboBoxes();
        updateModeUI();
        loadRecentBatches();
    }

    private void initializeTimeComboBoxes() {
        List<String> minutes = new ArrayList<>();
        for (int m = 0; m < 60; m += 5) minutes.add(String.format("%02d", m));
        minutes.add("59");
        Collections.sort(minutes);

        if (cbIngressMinute != null) { cbIngressMinute.getItems().addAll(minutes); cbIngressMinute.setPromptText("MM"); }
        if (cbEgressMinute  != null) { cbEgressMinute.getItems().addAll(minutes);  cbEgressMinute.setPromptText("MM"); }

        if (cbIngressHour != null) cbIngressHour.setPromptText("HH");
        if (cbEgressHour  != null) cbEgressHour.setPromptText("HH");

        if (dpIngressDate != null) { dpIngressDate.setConverter(dateConverter()); dpIngressDate.getEditor().setEditable(false); }
        if (dpEgressDate  != null) { dpEgressDate.setConverter(dateConverter());  dpEgressDate.getEditor().setEditable(false); }
    }

    private void refreshTimeHours() {
        LocalTime[] hours = loadOperatingHours();
        int openHour  = (hours != null) ? hours[0].getHour() : 8;
        int closeHour = (hours != null) ? hours[1].getHour() : 20;

        List<String> validHours = new ArrayList<>();
        for (int h = openHour; h <= closeHour; h++) validHours.add(String.format("%02d", h));

        if (cbIngressHour != null) {
            cbIngressHour.getItems().setAll(validHours);
            cbIngressHour.setValue(null);
            cbIngressHour.setPromptText("HH");
        }
        if (cbEgressHour != null) {
            cbEgressHour.getItems().setAll(validHours);
            cbEgressHour.setValue(null);
            cbEgressHour.setPromptText("HH");
        }
    }

    private StringConverter<LocalDate> dateConverter() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM d, yyyy");
        return new StringConverter<>() {
            @Override public String toString(LocalDate d) { return d != null ? d.format(fmt) : ""; }
            @Override public LocalDate fromString(String s) {
                try { return LocalDate.parse(s, fmt); } catch (Exception e) { return null; }
            }
        };
    }

    private void clearSearch() {
        txtSearchStudent.clear();
        paneSearchResults.getChildren().clear();
    }

    private void updateModeUI() {
        if (svgModeIconNode != null) {
            svgModeIconNode.setContent("M8 7h12m0 0l-4-4m4 4l-4 4m0 6H4m0 0l4 4m-4-4l4-4");
            svgModeIconNode.setStyle("-fx-stroke: #7A0000;");
            svgModeIconNode.getParent().setStyle("-fx-background-color: #FDF6E3; -fx-background-radius: 12; -fx-alignment: center;");
        }
        if (lblModeStatus != null) lblModeStatus.setText("Gate Request Mode");
        if (lblModeHint   != null) lblModeHint.setText(
                "Search for a registered student below to schedule both their ingress and egress at the same time.");
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
        String sql = "SELECT student_id, student_code, full_name, section, " +
                "COALESCE(email, '') AS email " +
                "FROM students " +
                "WHERE status = 'ACTIVE' AND (LOWER(student_code) LIKE ? OR LOWER(full_name) LIKE ?) " +
                "ORDER BY full_name ASC LIMIT 5";

        try (Connection conn = DBConnection.connect();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, lower); pst.setString(2, lower);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    paneSearchResults.getChildren().add(buildResultRow(
                            rs.getInt("student_id"), rs.getString("student_code"),
                            rs.getString("full_name"), rs.getString("section"),
                            rs.getString("email")));
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private HBox buildResultRow(int studentId, String code, String name,
                                String section, String email) {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 10; -fx-border-color: #C9A84C; -fx-border-radius: 10; -fx-border-width: 1; -fx-cursor: hand; -fx-padding: 14 20;");

        StackPane iconCircle = new StackPane();
        iconCircle.setMaxSize(36, 36); iconCircle.setMinSize(36, 36);
        iconCircle.setStyle("-fx-background-color: #FDF6E3; -fx-background-radius: 50;");
        SVGPath userSvg = new SVGPath();
        userSvg.setContent("M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z");
        userSvg.setStyle("-fx-stroke: #7A0000; -fx-fill: transparent; -fx-stroke-width: 1.8;");
        iconCircle.getChildren().add(userSvg);

        VBox info = new VBox(2);
        Label lblName = new Label(name);
        lblName.setStyle("-fx-font-family: 'Inter', 'Segoe UI'; -fx-font-weight: bold; -fx-text-fill: #1E293B; -fx-font-size: 14px;");
        Label lblMeta = new Label(code + "  •  " + section);
        lblMeta.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12px;");
        info.getChildren().addAll(lblName, lblMeta);

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        SVGPath chevron = new SVGPath();
        chevron.setContent("M9 5l7 7-7 7");
        chevron.setStyle("-fx-stroke: #C9A84C; -fx-stroke-width: 2; -fx-fill: transparent; -fx-stroke-linecap: round;");

        row.getChildren().addAll(iconCircle, info, spacer, chevron);
        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color: #FDF6E3; -fx-background-radius: 10; -fx-border-color: #7A0000; -fx-border-radius: 10; -fx-border-width: 1.5; -fx-cursor: hand; -fx-padding: 14 20;"));
        row.setOnMouseExited(e  -> row.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 10; -fx-border-color: #C9A84C; -fx-border-radius: 10; -fx-border-width: 1; -fx-cursor: hand; -fx-padding: 14 20;"));
        row.setOnMouseClicked(e -> openDeviceSelection(studentId, code, name, section, email));
        return row;
    }

    private void openDeviceSelection(int studentId, String code, String name,
                                     String section, String email) {
        currentStudentId    = studentId;
        currentStudentCode  = code;
        currentStudentName  = name;
        currentStudentMeta  = code + " · " + section;
        currentStudentEmail = email != null ? email : "";
        currentStudentCourse= section;
        currentStudentYear  = "";
        selectedDevices.clear();

        lblModalStudentName.setText(currentStudentName);
        lblModalStudentMeta.setText(currentStudentMeta);
        if (profileDividerLine1 != null)
            profileDividerLine1.setStyle("-fx-background-color: #C9A84C; -fx-min-height: 2; -fx-max-height: 2;");

        lblModalBadge.setText("GATE REQUEST");
        modalSelectContainer.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 16; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.22), 28, 0, 0, 8);");
        lblModalBadge.setStyle("-fx-background-color: #C9A84C; -fx-text-fill: #FFFFFF; -fx-background-radius: 20; -fx-padding: 5 16; -fx-font-family: 'Inter', 'Segoe UI'; -fx-font-weight: bold; -fx-font-size: 11px; -fx-letter-spacing: 1px;");
        btnReviewSelection.setStyle("-fx-background-color: #7A0000; -fx-background-radius: 10; -fx-cursor: hand; -fx-text-fill: white; -fx-padding: 0 24; -fx-font-weight: bold;");

        loadEligibleDevices(studentId);
        paneDeviceSelectModal.setVisible(true);
        paneDeviceSelectModal.setManaged(true);
    }

    private void loadEligibleDevices(int studentId) {
        paneDeviceCheckList.getChildren().clear();
        selectedDevices.clear();

        String sql = "SELECT d.device_id, d.serial_number, d.brand, d.model, d.device_type, " +
                "COALESCE(d.current_location, 'UNKNOWN') AS current_location " +
                "FROM devices d " +
                "WHERE d.owner_id = ? AND d.status = 'ACTIVE' " +
                "ORDER BY d.device_id ASC";

        Set<Integer> pendingDeviceIds = getDevicesWithPendingRequestForStudent(studentId);

        int count = 0;
        try (Connection conn = DBConnection.connect();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, studentId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    int deviceId = rs.getInt("device_id");
                    boolean hasPending = pendingDeviceIds.contains(deviceId);
                    paneDeviceCheckList.getChildren().add(buildDeviceRow(
                            deviceId, rs.getString("serial_number"),
                            rs.getString("brand"), rs.getString("model"),
                            rs.getString("device_type"), rs.getString("current_location"),
                            hasPending));
                    count++;
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }

        if (count == 0) {
            Label lbl = new Label("No registered devices found for this student.");
            lbl.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 13px; -fx-padding: 30 10; -fx-font-style: italic;");
            lbl.setWrapText(true);
            paneDeviceCheckList.getChildren().add(lbl);
        }

        lblModalSectionTitle.setText("SELECT DEVICES FOR GATE REQUEST — " + count + " REGISTERED");
        updateSelectionCount();
    }

    private Set<Integer> getDevicesWithPendingRequestForStudent(int studentId) {
        Set<Integer> ids = new HashSet<>();
        String sql = "SELECT DISTINCT grd.device_id " +
                "FROM gate_request_devices grd " +
                "JOIN gate_requests gr ON gr.request_id = grd.request_id " +
                "JOIN devices d ON d.device_id = grd.device_id " +
                "WHERE d.owner_id = ? AND gr.status = 'PENDING'";
        try (Connection conn = DBConnection.connect();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, studentId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) ids.add(rs.getInt("device_id"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return ids;
    }

    private HBox buildDeviceRow(int deviceId, String serial, String brand, String model,
                                String type, String location, boolean hasPending) {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);

        if (hasPending) {
            row.setStyle("-fx-background-color: #F3F4F6; -fx-background-radius: 10; -fx-border-color: #D1D5DB; -fx-border-radius: 10; -fx-border-width: 1; -fx-padding: 14; -fx-opacity: 0.7;");
        } else {
            row.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 10; -fx-border-color: #C9A84C; -fx-border-radius: 10; -fx-border-width: 1; -fx-cursor: hand; -fx-padding: 14;");
        }

        StackPane iconBox = new StackPane();
        iconBox.setMaxSize(40, 40); iconBox.setMinSize(40, 40);
        iconBox.setStyle(hasPending
                ? "-fx-background-color: #E5E7EB; -fx-background-radius: 10;"
                : "-fx-background-color: #FDF6E3; -fx-background-radius: 10;");
        SVGPath deviceIcon = new SVGPath();
        deviceIcon.setContent(switch (type) {
            case "LAPTOP" -> "M4 6a2 2 0 012-2h12a2 2 0 012 2v8a2 2 0 01-2 2H6a2 2 0 01-2-2V6zM2 20h20";
            case "TABLET" -> "M12 18h.01M7 21h10a2 2 0 002-2V5a2 2 0 00-2-2H7a2 2 0 00-2 2v14a2 2 0 002 2z";
            default       -> "M20 7H4a2 2 0 00-2 2v6a2 2 0 002 2h16a2 2 0 002-2V9a2 2 0 00-2-2z";
        });
        deviceIcon.setStyle(hasPending
                ? "-fx-stroke: #9CA3AF; -fx-fill: transparent; -fx-stroke-width: 1.8;"
                : "-fx-stroke: #7A0000; -fx-fill: transparent; -fx-stroke-width: 1.8;");
        iconBox.getChildren().add(deviceIcon);

        boolean isSelected = selectedDevices.containsKey(deviceId);
        StackPane checkbox = new StackPane();
        checkbox.setMaxSize(22, 22); checkbox.setMinSize(22, 22);

        if (hasPending) {
            checkbox.setStyle("-fx-background-color: #E5E7EB; -fx-background-radius: 6; -fx-border-color: #D1D5DB; -fx-border-width: 2; -fx-border-radius: 6;");
        } else {
            checkbox.setStyle(isSelected
                    ? "-fx-background-color: #7A0000; -fx-background-radius: 6;"
                    : "-fx-background-color: #FFFFFF; -fx-background-radius: 6; -fx-border-color: #C9A84C; -fx-border-width: 2; -fx-border-radius: 6;");
        }

        SVGPath checkMark = new SVGPath();
        checkMark.setContent("M5 13l4 4L19 7");
        checkMark.setStyle("-fx-stroke: #FFFFFF; -fx-fill: transparent; -fx-stroke-width: 2.2; -fx-stroke-linecap: round;");
        checkMark.setVisible(!hasPending && isSelected);
        checkbox.getChildren().add(checkMark);

        VBox nameContainer = new VBox(2); HBox.setHgrow(nameContainer, Priority.ALWAYS);
        Label lblDevice = new Label(brand + " " + model);
        lblDevice.setStyle(hasPending
                ? "-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #9CA3AF;"
                : "-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #1E293B;");
        Label lblSerial = new Label(serial + "  •  " + type + "  •  Currently: " + location);
        lblSerial.setStyle("-fx-font-size: 11px; -fx-text-fill: #94A3B8;");
        nameContainer.getChildren().addAll(lblDevice, lblSerial);

        if (hasPending) {
            Label lblPending = new Label("⏳ Request Pending");
            lblPending.setStyle("-fx-background-color: #FEF3C7; -fx-text-fill: #92400E; -fx-padding: 4 10; " +
                    "-fx-background-radius: 20; -fx-font-size: 10px; -fx-font-weight: bold; " +
                    "-fx-border-color: #F59E0B; -fx-border-radius: 20; -fx-border-width: 1;");
            row.getChildren().addAll(iconBox, nameContainer, lblPending, checkbox);
        } else {
            Label lblBothWays = new Label("↔ IN + OUT");
            lblBothWays.setStyle("-fx-background-color: #C9A84C; -fx-text-fill: #FFFFFF; -fx-padding: 5 12; -fx-background-radius: 20; -fx-font-size: 11px; -fx-font-weight: bold;");
            row.getChildren().addAll(iconBox, nameContainer, lblBothWays, checkbox);

            row.setOnMouseClicked(e -> {
                if (selectedDevices.containsKey(deviceId)) {
                    selectedDevices.remove(deviceId);
                    checkbox.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 6; -fx-border-color: #C9A84C; -fx-border-width: 2; -fx-border-radius: 6;");
                    checkMark.setVisible(false);
                    row.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 10; -fx-border-color: #C9A84C; -fx-border-radius: 10; -fx-border-width: 1; -fx-cursor: hand; -fx-padding: 14;");
                } else {
                    selectedDevices.put(deviceId, new SelectedDevice(brand, model, serial, type));
                    checkbox.setStyle("-fx-background-color: #7A0000; -fx-background-radius: 6;");
                    checkMark.setVisible(true);
                    row.setStyle("-fx-background-color: #FDF0F0; -fx-background-radius: 10; -fx-border-color: #7A0000; -fx-border-radius: 10; -fx-border-width: 1.5; -fx-cursor: hand; -fx-padding: 14;");
                }
                updateSelectionCount();
            });

            row.setOnMouseEntered(e -> {
                if (!selectedDevices.containsKey(deviceId))
                    row.setStyle("-fx-background-color: #FDF6E3; -fx-background-radius: 10; -fx-border-color: #C9A84C; -fx-border-radius: 10; -fx-border-width: 1; -fx-cursor: hand; -fx-padding: 14;");
            });
            row.setOnMouseExited(e -> {
                if (!selectedDevices.containsKey(deviceId))
                    row.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 10; -fx-border-color: #C9A84C; -fx-border-radius: 10; -fx-border-width: 1; -fx-cursor: hand; -fx-padding: 14;");
            });
        }

        return row;
    }

    private void updateSelectionCount() {
        int count = selectedDevices.size();
        lblSelectedCount.setText(count + " device" + (count == 1 ? "" : "s") + " selected");
        btnReviewSelection.setDisable(count == 0);
    }

    @FXML
    public void handleReviewSelection() {
        if (selectedDevices.isEmpty()) return;

        lblConfirmStudentName.setText(currentStudentName);
        lblConfirmStudentMeta.setText(currentStudentMeta);
        lblConfirmBadge.setText("GATE REQUEST");
        modalConfirmContainer.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 16; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.22), 28, 0, 0, 8);");
        lblConfirmBadge.setStyle("-fx-background-color: #C9A84C; -fx-text-fill: #FFFFFF; -fx-background-radius: 20; -fx-padding: 5 16; -fx-font-weight: bold; -fx-font-size: 11px; -fx-letter-spacing: 1px;");
        btnConfirmAction.setStyle("-fx-background-color: #7A0000; -fx-background-radius: 10; -fx-cursor: hand; -fx-text-fill: white; -fx-padding: 0 28; -fx-font-weight: bold; -fx-font-size: 14px;");
        if (profileDividerLine2 != null)
            profileDividerLine2.setStyle("-fx-background-color: #C9A84C; -fx-min-height: 1; -fx-max-height: 1;");

        lblConfirmInnerName.setStyle("-fx-font-family: 'Inter','Segoe UI'; -fx-font-weight: 800; -fx-text-fill: #0F172A; -fx-font-size: 17px;");
        lblConfirmInnerCode.setStyle("-fx-font-family: 'Inter','Segoe UI'; -fx-text-fill: #64748B; -fx-font-size: 12px;");
        lblConfirmInnerName.setText(currentStudentName);
        lblConfirmInnerCode.setText(currentStudentCode + " · " + currentStudentCourse);
        lblConfirmFor.setText("For: Campus Ingress & Egress (Both scheduled)");

        paneConfirmDeviceList.getChildren().clear();
        for (Map.Entry<Integer, SelectedDevice> entry : selectedDevices.entrySet()) {
            HBox row = new HBox(12); row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-background-color: #FDF6E3; -fx-background-radius: 12; -fx-border-color: #C9A84C; -fx-border-radius: 12; -fx-border-width: 1; -fx-padding: 12 16;");
            StackPane dot = new StackPane(); dot.setMaxSize(10, 10); dot.setMinSize(10, 10);
            dot.setStyle("-fx-background-color: #7A0000; -fx-background-radius: 50;");
            Label lblDeviceName = new Label(entry.getValue().displayName());
            lblDeviceName.setStyle("-fx-font-size: 13px; -fx-text-fill: #1E293B; -fx-font-weight: 700;");
            Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
            Label lblBoth = new Label("↔ IN + OUT");
            lblBoth.setStyle("-fx-background-color: #C9A84C; -fx-text-fill: #FFFFFF; -fx-padding: 5 12; -fx-background-radius: 20; -fx-font-size: 11px; -fx-font-weight: bold;");
            row.getChildren().addAll(dot, lblDeviceName, spacer, lblBoth);
            paneConfirmDeviceList.getChildren().add(row);
        }

        lblConfirmWarning.setText("Schedule ingress AND egress for " + selectedDevices.size()
                + (selectedDevices.size() == 1 ? " device" : " devices")
                + "? Set both times in the next step.");

        paneDeviceSelectModal.setVisible(false); paneDeviceSelectModal.setManaged(false);
        paneConfirmModal.setVisible(true); paneConfirmModal.setManaged(true);
    }

    @FXML public void handleBackToSelection() {
        paneConfirmModal.setVisible(false); paneConfirmModal.setManaged(false);
        paneDeviceSelectModal.setVisible(true); paneDeviceSelectModal.setManaged(true);
    }

    @FXML public void handleConfirmAction() {
        if (selectedDevices.isEmpty()) return;
        showScheduleModal();
    }

    private void showScheduleModal() {
        if (paneScheduleModal == null) return;

        if (lblScheduleBadge != null)
            lblScheduleBadge.setText("Input New Device Log");

        if (lblScheduleSummaryName != null)
            lblScheduleSummaryName.setText(currentStudentName + "  (" + currentStudentCode + ")");
        if (lblScheduleSummaryDevice != null) {
            List<String> devNames = new ArrayList<>();
            for (SelectedDevice sd : selectedDevices.values()) devNames.add(sd.displayName());
            lblScheduleSummaryDevice.setText(devNames.isEmpty() ? "No devices selected"
                    : String.join("  ·  ", devNames));
        }

        if (lblScheduleError != null) { lblScheduleError.setVisible(false); lblScheduleError.setManaged(false); }
        if (lblClosingTimeWarning != null) { lblClosingTimeWarning.setVisible(false); lblClosingTimeWarning.setManaged(false); }

        refreshTimeHours();

        if (dpIngressDate   != null) dpIngressDate.setValue(null);
        if (dpEgressDate    != null) dpEgressDate.setValue(null);
        if (cbIngressHour   != null) cbIngressHour.setValue(null);
        if (cbIngressMinute != null) cbIngressMinute.setValue(null);
        if (cbEgressHour    != null) cbEgressHour.setValue(null);
        if (cbEgressMinute  != null) cbEgressMinute.setValue(null);

        paneConfirmModal.setVisible(false); paneConfirmModal.setManaged(false);
        paneScheduleModal.setVisible(true); paneScheduleModal.setManaged(true);
    }

    private LocalTime[] loadOperatingHours() {
        String sql = "SELECT start_of_day_time, end_of_day_time FROM settings ORDER BY setting_id DESC LIMIT 1";
        try (Connection conn = DBConnection.connect();
             PreparedStatement pst = conn.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {
            if (rs.next()) {
                LocalTime open  = rs.getTime("start_of_day_time").toLocalTime();
                LocalTime close = rs.getTime("end_of_day_time").toLocalTime();
                return new LocalTime[]{open, close};
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    private boolean isWithinOperatingHours(LocalDateTime dt, LocalTime open, LocalTime close) {
        LocalTime t = dt.toLocalTime();
        return !t.isBefore(open) && !t.isAfter(close);
    }

    @FXML
    public void handleSubmitSchedule() {
        LocalDate ingressDate = dpIngressDate != null ? dpIngressDate.getValue() : null;
        LocalDate egressDate  = dpEgressDate  != null ? dpEgressDate.getValue()  : null;
        String ingressH = cbIngressHour   != null ? cbIngressHour.getValue()   : null;
        String ingressM = cbIngressMinute != null ? cbIngressMinute.getValue() : null;
        String egressH  = cbEgressHour    != null ? cbEgressHour.getValue()    : null;
        String egressM  = cbEgressMinute  != null ? cbEgressMinute.getValue()  : null;

        if (ingressDate == null || egressDate == null || ingressH == null || ingressM == null
                || egressH == null || egressM == null) {
            showScheduleError("Please fill in all date and time fields."); return;
        }

        LocalDateTime ingressTime = LocalDateTime.of(ingressDate,
                LocalTime.of(Integer.parseInt(ingressH), Integer.parseInt(ingressM)));
        LocalDateTime egressTime  = LocalDateTime.of(egressDate,
                LocalTime.of(Integer.parseInt(egressH),  Integer.parseInt(egressM)));
        LocalDateTime now = LocalDateTime.now();

        if (ingressDate.isBefore(now.toLocalDate())) {
            showScheduleError("Ingress date must be today or a future date."); return;
        }
        if (ingressDate.isEqual(now.toLocalDate()) && !ingressTime.isAfter(now)) {
            showScheduleError("Ingress time must be after the current time."); return;
        }
        if (!egressTime.isAfter(ingressTime)) {
            showScheduleError("Egress time must be after the ingress time."); return;
        }

        LocalTime[] hours = loadOperatingHours();
        if (hours != null) {
            LocalTime open = hours[0], close = hours[1];
            boolean ingressOk = isWithinOperatingHours(ingressTime, open, close);
            boolean egressOk  = isWithinOperatingHours(egressTime,  open, close);
            if (!ingressOk || !egressOk) {
                String msg = "⚠ ";
                if (!ingressOk && !egressOk)
                    msg += "Both times are outside school hours (" + open + " – " + close + ").";
                else if (!ingressOk)
                    msg += "Ingress time is outside school hours (" + open + " – " + close + ").";
                else
                    msg += "Egress time is outside school hours (" + open + " – " + close + ").";
                showScheduleError(msg); return;
            }
        }

        paneScheduleModal.setVisible(false); paneScheduleModal.setManaged(false);
        submitDualGateRequest(ingressTime, egressTime);
    }

    @FXML public void handleCloseScheduleModal() {
        if (paneScheduleModal != null) { paneScheduleModal.setVisible(false); paneScheduleModal.setManaged(false); }
        paneConfirmModal.setVisible(true); paneConfirmModal.setManaged(true);
    }

    private void showScheduleError(String msg) {
        if (lblScheduleError != null) {
            lblScheduleError.setText(msg);
            lblScheduleError.setVisible(true); lblScheduleError.setManaged(true);
        }
    }

    private void submitDualGateRequest(LocalDateTime ingressTime, LocalDateTime egressTime) {
        UserSession session = UserSession.getInstance();
        int requestedBy = session.getUserId();
        List<Integer> deviceIds = new ArrayList<>(selectedDevices.keySet());

        String insertRequestSql = "INSERT INTO gate_requests " +
                "(student_id, direction, scheduled_time, requested_by, status) " +
                "VALUES (?, ?, ?, ?, 'PENDING') RETURNING request_id";
        String insertDeviceSql  = "INSERT INTO gate_request_devices (request_id, device_id) VALUES (?, ?)";
        String insertAuditSql   = "INSERT INTO audit_log " +
                "(operator_id, action_type, target_type, target_id, details, performed_at) " +
                "VALUES (?, 'GATE_REQUEST_CREATED', 'gate_requests', ?, ?::jsonb, NOW() AT TIME ZONE 'Asia/Manila')";

        try (Connection conn = DBConnection.connect()) {
            conn.setAutoCommit(false);
            try {
                insertRequest(conn, insertRequestSql, insertDeviceSql, insertAuditSql,
                        currentStudentId, "IN", ingressTime, requestedBy, deviceIds);
                insertRequest(conn, insertRequestSql, insertDeviceSql, insertAuditSql,
                        currentStudentId, "OUT", egressTime, requestedBy, deviceIds);
                conn.commit();

                List<SelectedDevice> emailDevices = new ArrayList<>(selectedDevices.values());
                sendCombinedGatePassEmailSafely(currentStudentEmail, currentStudentName, currentStudentCode,
                        currentStudentCourse, currentStudentYear, ingressTime, egressTime, emailDevices);

                handleCloseModal();
                clearSearch();
                loadRecentBatches();
                showSuccessRegistrationPopup(ingressTime, egressTime, deviceIds);

            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
                showAckPopup(false, "Request Failed", "Could not submit the gate request. Please try again.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAckPopup(false, "Connection Error", "Database connection failed.");
        }
    }

    private int insertRequest(Connection conn, String reqSql, String devSql, String auditSql,
                              int studentId, String direction, LocalDateTime time,
                              int requestedBy, List<Integer> deviceIds) throws SQLException {
        int requestId;
        try (PreparedStatement pst = conn.prepareStatement(reqSql)) {
            pst.setInt(1, studentId); pst.setString(2, direction);
            pst.setTimestamp(3, Timestamp.valueOf(time)); pst.setInt(4, requestedBy);
            try (ResultSet rs = pst.executeQuery()) { rs.next(); requestId = rs.getInt(1); }
        }
        try (PreparedStatement pst = conn.prepareStatement(devSql)) {
            for (int deviceId : deviceIds) {
                pst.setInt(1, requestId); pst.setInt(2, deviceId); pst.addBatch();
            }
            pst.executeBatch();
        }
        String details = "{\"request_id\":" + requestId + ",\"student_id\":" + studentId
                + ",\"direction\":\"" + direction + "\",\"scheduled_time\":\"" + time
                + "\",\"device_count\":" + deviceIds.size() + "}";
        try (PreparedStatement pst = conn.prepareStatement(auditSql)) {
            pst.setInt(1, requestedBy); pst.setInt(2, requestId); pst.setString(3, details);
            pst.executeUpdate();
        }
        return requestId;
    }

    private void sendCombinedGatePassEmailSafely(String toEmail, String studentName, String studentCode,
                                                 String course, String yearLevel,
                                                 LocalDateTime ingressTime, LocalDateTime egressTime,
                                                 List<SelectedDevice> devices) {
        if (toEmail == null || toEmail.isBlank()) return;
        try {
            List<DeviceEntry> entries = new ArrayList<>();
            for (SelectedDevice d : devices) entries.add(new DeviceEntry(d.brand, d.model, d.serial, d.deviceType));
            EmailService.sendCombinedGatePassEmail(toEmail, studentName, studentCode, course, yearLevel,
                    ingressTime, egressTime, entries);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private boolean qlStudentRegistered = false;
    private boolean qlDeviceRegistered  = false;
    private int    qlExistingStudentId = -1;
    private int    qlExistingDeviceId  = -1;
    private String qlStudentName="", qlStudentCode="", qlStudentCourse="",
            qlStudentYear="", qlStudentEmail="";
    private String qlDeviceBrand="", qlDeviceModel="", qlDeviceSerial="",
            qlDeviceType="";

    @FXML
    public void handleOpenQuickLog() {
        if (paneQuickLogModal == null) return;
        qlStudentRegistered = false;
        qlDeviceRegistered  = false;
        qlExistingStudentId = -1;
        qlExistingDeviceId  = -1;
        showQuickLogStep0_StudentCheck();
    }

    private void showQuickLogStep0_StudentCheck() {
        buildQuickModal("Input New Device Log", step -> {
            Label lbl = sectionLabel("STUDENT REGISTRATION STATUS");
            Label question = new Label("Is the student already registered in the system?");
            question.setStyle("-fx-font-size: 13px; -fx-text-fill: #374151; -fx-font-weight: 600; -fx-wrap-text: true;");
            question.setWrapText(true);
            Label hint = new Label("If yes, you can search and pick them. If no, you will enter their details.");
            hint.setStyle("-fx-font-size: 11px; -fx-text-fill: #6B7280;");
            hint.setWrapText(true);
            HBox toggle = new HBox(0);
            Button btnYes = toggleBtn("Yes — Student is registered", true,  false);
            Button btnNo  = toggleBtn("No — Not yet registered",      false, true);
            styleToggle(btnYes, false); styleToggle(btnNo, false);
            btnYes.setOnAction(e -> { qlStudentRegistered = true;  styleToggle(btnYes, true); styleToggle(btnNo, false); });
            btnNo.setOnAction(e  -> { qlStudentRegistered = false; styleToggle(btnNo,  true); styleToggle(btnYes,false); });
            toggle.getChildren().addAll(btnYes, btnNo);
            HBox.setHgrow(btnYes, Priority.ALWAYS); HBox.setHgrow(btnNo, Priority.ALWAYS);
            Label errLbl = errorLabel();
            Button btnNext = primaryBtn("Next  →");
            btnNext.setOnAction(e -> {
                boolean yesSelected = btnYes.getStyle().contains("#7A0000");
                boolean noSelected  = btnNo.getStyle().contains("#7A0000");
                if (!yesSelected && !noSelected) { showErr(errLbl, "Please select an option."); return; }
                if (qlStudentRegistered) showQuickLogStep1a_PickStudent();
                else showQuickLogStep1b_EnterStudent();
            });
            step.getChildren().addAll(lbl, question, hint, toggle, errLbl, btnNext);
        });
    }

    private void showQuickLogStep1a_PickStudent() {
        buildQuickModal("Input New Device Log", step -> {
            Label lbl = sectionLabel("SELECT STUDENT");
            TextField search = styledField("Search by name or student code...");
            VBox results = new VBox(6);
            results.setStyle("-fx-padding: 0;");
            search.textProperty().addListener((obs, o, q) -> {
                results.getChildren().clear();
                if (q == null || q.trim().isEmpty()) return;
                String like = "%" + q.trim().toLowerCase() + "%";
                String sql  = "SELECT student_id, student_code, full_name, section " +
                        "FROM students WHERE status='ACTIVE' " +
                        "AND (LOWER(full_name) LIKE ? OR LOWER(student_code) LIKE ?) " +
                        "ORDER BY full_name LIMIT 5";
                try (Connection conn = DBConnection.connect();
                     PreparedStatement pst = conn.prepareStatement(sql)) {
                    pst.setString(1, like); pst.setString(2, like);
                    try (ResultSet rs = pst.executeQuery()) {
                        while (rs.next()) {
                            int    sid     = rs.getInt("student_id");
                            String code    = rs.getString("student_code");
                            String name    = rs.getString("full_name");
                            String section = rs.getString("section");
                            HBox row = new HBox(10);
                            row.setAlignment(Pos.CENTER_LEFT);
                            row.setStyle("-fx-background-color:#F8FAFC;-fx-background-radius:8;-fx-border-color:#E2E8F0;-fx-border-radius:8;-fx-border-width:1;-fx-padding:10 12;-fx-cursor:hand;");
                            VBox info = new VBox(2);
                            Label n = new Label(name);
                            n.setStyle("-fx-font-weight:700;-fx-font-size:13px;-fx-text-fill:#1E293B;");
                            Label m = new Label(code + " · " + section);
                            m.setStyle("-fx-font-size:11px;-fx-text-fill:#64748B;");
                            info.getChildren().addAll(n, m);
                            row.getChildren().add(info);
                            row.setOnMouseEntered(ev -> row.setStyle("-fx-background-color:#FDF6E3;-fx-background-radius:8;-fx-border-color:#C9A84C;-fx-border-radius:8;-fx-border-width:1;-fx-padding:10 12;-fx-cursor:hand;"));
                            row.setOnMouseExited(ev -> {
                                if (qlExistingStudentId == sid)
                                    row.setStyle("-fx-background-color:#FDF0F0;-fx-background-radius:8;-fx-border-color:#7A0000;-fx-border-radius:8;-fx-border-width:1.5;-fx-padding:10 12;-fx-cursor:hand;");
                                else
                                    row.setStyle("-fx-background-color:#F8FAFC;-fx-background-radius:8;-fx-border-color:#E2E8F0;-fx-border-radius:8;-fx-border-width:1;-fx-padding:10 12;-fx-cursor:hand;");
                            });
                            row.setOnMouseClicked(ev -> {
                                qlExistingStudentId = sid;
                                qlStudentName = name; qlStudentCode = code;
                                qlStudentCourse = section; qlStudentYear = "";
                                try (Connection c2 = DBConnection.connect();
                                     PreparedStatement ps2 = c2.prepareStatement(
                                             "SELECT COALESCE(email,'') FROM students WHERE student_id=?")) {
                                    ps2.setInt(1, sid);
                                    try (ResultSet rs2 = ps2.executeQuery()) {
                                        if (rs2.next()) qlStudentEmail = rs2.getString(1);
                                    }
                                } catch (Exception ex) { ex.printStackTrace(); }
                                results.getChildren().forEach(ch -> ch.setStyle(
                                        "-fx-background-color:#F8FAFC;-fx-background-radius:8;-fx-border-color:#E2E8F0;-fx-border-radius:8;-fx-border-width:1;-fx-padding:10 12;-fx-cursor:hand;"));
                                row.setStyle("-fx-background-color:#FDF0F0;-fx-background-radius:8;-fx-border-color:#7A0000;-fx-border-radius:8;-fx-border-width:1.5;-fx-padding:10 12;-fx-cursor:hand;");
                            });
                            results.getChildren().add(row);
                        }
                    }
                } catch (Exception ex) { ex.printStackTrace(); }
            });
            ScrollPane sp = new ScrollPane(results);
            sp.setFitToWidth(true); sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            sp.setStyle("-fx-background-color:transparent;-fx-background:transparent;-fx-border-color:transparent;");
            sp.setPrefHeight(160); sp.setMaxHeight(160);
            Label errLbl = errorLabel();
            HBox btns = new HBox(10);
            Button btnBack = secondaryBtn("← Back");
            btnBack.setOnAction(e -> showQuickLogStep0_StudentCheck());
            Button btnNext = primaryBtn("Next  →");
            btnNext.setOnAction(e -> {
                if (qlExistingStudentId < 0) { showErr(errLbl, "Please select a student."); return; }
                showQuickLogStep2_DeviceCheck();
            });
            btns.getChildren().addAll(btnBack, btnNext);
            HBox.setHgrow(btnNext, Priority.ALWAYS);
            step.getChildren().addAll(lbl, search, sp, errLbl, btns);
        });
    }

    private void showQuickLogStep1b_EnterStudent() {
        buildQuickModal("Input New Device Log", step -> {
            Label lbl = sectionLabel("NEW STUDENT INFORMATION");
            TextField txtName    = styledField("Full Name *");
            TextField txtCode    = styledField("Student Code *");
            TextField txtSection = styledField("Section *");
            TextField txtEmail   = styledField("Email Address *");
            if (!qlStudentName.isEmpty())   txtName.setText(qlStudentName);
            if (!qlStudentCode.isEmpty())   txtCode.setText(qlStudentCode);
            if (!qlStudentCourse.isEmpty()) txtSection.setText(qlStudentCourse);
            if (!qlStudentEmail.isEmpty())  txtEmail.setText(qlStudentEmail);
            Label errLbl = errorLabel();
            HBox btns = new HBox(10);
            Button btnBack = secondaryBtn("← Back");
            btnBack.setOnAction(e -> showQuickLogStep0_StudentCheck());
            Button btnNext = primaryBtn("Next  →");
            btnNext.setOnAction(e -> {
                qlStudentName   = txtName.getText().trim();
                qlStudentCode   = txtCode.getText().trim();
                qlStudentCourse = txtSection.getText().trim();
                qlStudentYear   = "";
                qlStudentEmail  = txtEmail.getText().trim();
                if (qlStudentName.isEmpty() || qlStudentCode.isEmpty() || qlStudentCourse.isEmpty()
                        || qlStudentEmail.isEmpty()) {
                    showErr(errLbl, "Please fill in all required fields."); return;
                }
               
                qlDeviceRegistered = false;
                showQuickLogStep3b_EnterDevice();
            });
            btns.getChildren().addAll(btnBack, btnNext);
            HBox.setHgrow(btnNext, Priority.ALWAYS);
            step.getChildren().addAll(lbl, txtName, txtCode, txtSection, txtEmail, errLbl, btns);
        });
    }

    private void showQuickLogStep2_DeviceCheck() {
        buildQuickModal("Input New Device Log", step -> {
            Label lbl = sectionLabel("DEVICE REGISTRATION STATUS");
            Label question = new Label("Is the device already registered in the system?");
            question.setStyle("-fx-font-size: 13px; -fx-text-fill: #374151; -fx-font-weight: 600;");
            question.setWrapText(true);
            Label hint = new Label("If yes, select it. If no, enter the device's details.");
            hint.setStyle("-fx-font-size: 11px; -fx-text-fill: #6B7280;");
            hint.setWrapText(true);
            HBox toggle = new HBox(0);
            Button btnYes = toggleBtn("Yes — Device is registered", true,  false);
            Button btnNo  = toggleBtn("No — Not yet registered",     false, true);
            styleToggle(btnYes, false); styleToggle(btnNo, false);
            btnYes.setOnAction(e -> { qlDeviceRegistered = true;  styleToggle(btnYes, true); styleToggle(btnNo,  false); });
            btnNo.setOnAction(e  -> { qlDeviceRegistered = false; styleToggle(btnNo,  true); styleToggle(btnYes, false); });
            toggle.getChildren().addAll(btnYes, btnNo);
            HBox.setHgrow(btnYes, Priority.ALWAYS); HBox.setHgrow(btnNo, Priority.ALWAYS);
            Label errLbl = errorLabel();
            HBox btns = new HBox(10);
            Button btnBack = secondaryBtn("← Back");
            btnBack.setOnAction(e -> {
                if (qlStudentRegistered) showQuickLogStep1a_PickStudent();
                else showQuickLogStep1b_EnterStudent();
            });
            Button btnNext = primaryBtn("Next  →");
            btnNext.setOnAction(e -> {
                boolean yesSelected = btnYes.getStyle().contains("#7A0000");
                boolean noSelected  = btnNo.getStyle().contains("#7A0000");
                if (!yesSelected && !noSelected) { showErr(errLbl, "Please select an option."); return; }
                if (qlDeviceRegistered) showQuickLogStep3a_PickDevice();
                else showQuickLogStep3b_EnterDevice();
            });
            btns.getChildren().addAll(btnBack, btnNext);
            HBox.setHgrow(btnNext, Priority.ALWAYS);
            step.getChildren().addAll(lbl, question, hint, toggle, errLbl, btns);
        });
    }

    private void showQuickLogStep3a_PickDevice() {
        buildQuickModal("Input New Device Log", step -> {
            Label lbl = sectionLabel("SELECT DEVICE");

            Set<Integer> pendingIds = qlStudentRegistered && qlExistingStudentId > 0
                    ? getDevicesWithPendingRequestForStudent(qlExistingStudentId)
                    : new HashSet<>();

            String sql = qlStudentRegistered && qlExistingStudentId > 0
                    ? "SELECT device_id, brand, model, serial_number, device_type " +
                      "FROM devices WHERE status='ACTIVE' AND owner_id=? ORDER BY device_id"
                    : "SELECT d.device_id, d.brand, d.model, d.serial_number, d.device_type " +
                      "FROM devices d JOIN students s ON s.student_id=d.owner_id " +
                      "WHERE d.status='ACTIVE' ORDER BY d.device_id LIMIT 50";

            VBox deviceList = new VBox(8);
            try (Connection conn = DBConnection.connect();
                 PreparedStatement pst = conn.prepareStatement(sql)) {
                if (qlStudentRegistered && qlExistingStudentId > 0) pst.setInt(1, qlExistingStudentId);
                try (ResultSet rs = pst.executeQuery()) {
                    while (rs.next()) {
                        int    did    = rs.getInt("device_id");
                        String brand  = rs.getString("brand");
                        String model  = rs.getString("model");
                        String serial = rs.getString("serial_number");
                        String type   = rs.getString("device_type");
                        boolean pending = pendingIds.contains(did);

                        HBox row = new HBox(12);
                        row.setAlignment(Pos.CENTER_LEFT);

                        if (pending) {
                            row.setStyle("-fx-background-color:#F3F4F6;-fx-background-radius:8;-fx-border-color:#D1D5DB;-fx-border-radius:8;-fx-border-width:1;-fx-padding:10 12;-fx-opacity:0.7;");
                        } else {
                            row.setStyle("-fx-background-color:#F8FAFC;-fx-background-radius:8;-fx-border-color:#E2E8F0;-fx-border-radius:8;-fx-border-width:1;-fx-padding:10 12;-fx-cursor:hand;");
                        }

                        StackPane iconBox = new StackPane();
                        iconBox.setMaxSize(32,32); iconBox.setMinSize(32,32);
                        iconBox.setStyle(pending
                                ? "-fx-background-color:#E5E7EB;-fx-background-radius:8;"
                                : "-fx-background-color:#FDF6E3;-fx-background-radius:8;");
                        SVGPath ic = new SVGPath();
                        ic.setContent(switch(type) {
                            case "LAPTOP" -> "M4 6a2 2 0 012-2h12a2 2 0 012 2v8a2 2 0 01-2 2H6a2 2 0 01-2-2V6zM2 20h20";
                            case "TABLET" -> "M12 18h.01M7 21h10a2 2 0 002-2V5a2 2 0 00-2-2H7a2 2 0 00-2 2v14a2 2 0 002 2z";
                            default       -> "M20 7H4a2 2 0 00-2 2v6a2 2 0 002 2h16a2 2 0 002-2V9a2 2 0 00-2-2z";
                        });
                        ic.setStyle(pending
                                ? "-fx-stroke:#9CA3AF;-fx-fill:transparent;-fx-stroke-width:1.5;"
                                : "-fx-stroke:#7A0000;-fx-fill:transparent;-fx-stroke-width:1.5;");
                        iconBox.getChildren().add(ic);

                        VBox info = new VBox(2);
                        Label n = new Label(brand + " " + model);
                        n.setStyle(pending
                                ? "-fx-font-weight:700;-fx-font-size:13px;-fx-text-fill:#9CA3AF;"
                                : "-fx-font-weight:700;-fx-font-size:13px;-fx-text-fill:#1E293B;");
                        Label m = new Label(serial + " · " + type);
                        m.setStyle("-fx-font-size:11px;-fx-text-fill:#94A3B8;");
                        info.getChildren().addAll(n, m);
                        row.getChildren().addAll(iconBox, info);

                        if (pending) {
                            Label badge = new Label("⏳ Request Pending");
                            badge.setStyle("-fx-background-color:#FEF3C7;-fx-text-fill:#92400E;-fx-padding:3 8;" +
                                    "-fx-background-radius:20;-fx-font-size:10px;-fx-font-weight:bold;" +
                                    "-fx-border-color:#F59E0B;-fx-border-radius:20;-fx-border-width:1;");
                            row.getChildren().add(badge);
                        } else {
                            row.setOnMouseEntered(ev -> row.setStyle("-fx-background-color:#FDF6E3;-fx-background-radius:8;-fx-border-color:#C9A84C;-fx-border-radius:8;-fx-border-width:1;-fx-padding:10 12;-fx-cursor:hand;"));
                            row.setOnMouseExited(ev -> {
                                if (qlExistingDeviceId == did)
                                    row.setStyle("-fx-background-color:#FDF0F0;-fx-background-radius:8;-fx-border-color:#7A0000;-fx-border-radius:8;-fx-border-width:1.5;-fx-padding:10 12;-fx-cursor:hand;");
                                else
                                    row.setStyle("-fx-background-color:#F8FAFC;-fx-background-radius:8;-fx-border-color:#E2E8F0;-fx-border-radius:8;-fx-border-width:1;-fx-padding:10 12;-fx-cursor:hand;");
                            });
                            row.setOnMouseClicked(ev -> {
                                qlExistingDeviceId = did;
                                qlDeviceBrand = brand; qlDeviceModel = model;
                                qlDeviceSerial = serial; qlDeviceType = type;
                                deviceList.getChildren().forEach(ch ->
                                        ch.setStyle("-fx-background-color:#F8FAFC;-fx-background-radius:8;-fx-border-color:#E2E8F0;-fx-border-radius:8;-fx-border-width:1;-fx-padding:10 12;-fx-cursor:hand;"));
                                row.setStyle("-fx-background-color:#FDF0F0;-fx-background-radius:8;-fx-border-color:#7A0000;-fx-border-radius:8;-fx-border-width:1.5;-fx-padding:10 12;-fx-cursor:hand;");
                            });
                        }
                        deviceList.getChildren().add(row);
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }

            if (deviceList.getChildren().isEmpty()) {
                Label none = new Label("No registered devices found.");
                none.setStyle("-fx-text-fill:#94A3B8;-fx-font-style:italic;-fx-font-size:12px;");
                deviceList.getChildren().add(none);
            }

            ScrollPane sp = new ScrollPane(deviceList);
            sp.setFitToWidth(true); sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            sp.setStyle("-fx-background-color:transparent;-fx-background:transparent;-fx-border-color:transparent;");
            sp.setPrefHeight(200); sp.setMaxHeight(200);
            Label errLbl = errorLabel();
            HBox btns = new HBox(10);
            Button btnBack = secondaryBtn("← Back");
            btnBack.setOnAction(e -> showQuickLogStep2_DeviceCheck());
            Button btnNext = primaryBtn("Next  →");
            btnNext.setOnAction(e -> {
                if (qlExistingDeviceId < 0) { showErr(errLbl, "Please select a device."); return; }
                showQuickLogStep4_Schedule();
            });
            btns.getChildren().addAll(btnBack, btnNext);
            HBox.setHgrow(btnNext, Priority.ALWAYS);
            step.getChildren().addAll(lbl, sp, errLbl, btns);
        });
    }

    private void showQuickLogStep3b_EnterDevice() {
        buildQuickModal("Input New Device Log", step -> {
            Label lbl = sectionLabel("NEW DEVICE INFORMATION");
            TextField txtBrand  = styledField("Brand *");
            TextField txtModel  = styledField("Model *");
            TextField txtSerial = styledField("Serial Number *");
            ComboBox<String> cbType = styledCombo("Device Type *", "LAPTOP", "TABLET", "OTHER");
            TextField txtOtherType = styledField("Please specify device type *");
            txtOtherType.setVisible(false); txtOtherType.setManaged(false);
            cbType.setOnAction(e -> {
                boolean isOther = "OTHER".equals(cbType.getValue());
                txtOtherType.setVisible(isOther); txtOtherType.setManaged(isOther);
            });
            if (!qlDeviceBrand.isEmpty())  txtBrand.setText(qlDeviceBrand);
            if (!qlDeviceModel.isEmpty())  txtModel.setText(qlDeviceModel);
            if (!qlDeviceSerial.isEmpty()) txtSerial.setText(qlDeviceSerial);
            if (!qlDeviceType.isEmpty()) {
                if (qlDeviceType.equals("LAPTOP") || qlDeviceType.equals("TABLET")) {
                    cbType.setValue(qlDeviceType);
                } else {
                    cbType.setValue("OTHER");
                    txtOtherType.setVisible(true); txtOtherType.setManaged(true);
                    txtOtherType.setText(qlDeviceType);
                }
            }
            Label errLbl = errorLabel();
            HBox btns = new HBox(10);
            Button btnBack = secondaryBtn("← Back");

            btnBack.setOnAction(e -> {
                if (qlStudentRegistered) showQuickLogStep2_DeviceCheck();
                else showQuickLogStep1b_EnterStudent();
            });
            Button btnNext = primaryBtn("Next  →");
            btnNext.setOnAction(e -> {
                qlDeviceBrand  = txtBrand.getText().trim();
                qlDeviceModel  = txtModel.getText().trim();
                qlDeviceSerial = txtSerial.getText().trim();
                String chosenType = cbType.getValue() != null ? cbType.getValue() : "";
                if ("OTHER".equals(chosenType)) {
                    String specified = txtOtherType.getText().trim();
                    if (specified.isEmpty()) { showErr(errLbl, "Please specify the device type."); return; }
                    qlDeviceType = specified;
                } else {
                    qlDeviceType = chosenType;
                }
                if (qlDeviceBrand.isEmpty() || qlDeviceModel.isEmpty()
                        || qlDeviceSerial.isEmpty() || qlDeviceType.isEmpty()) {
                    showErr(errLbl, "Please fill in all required fields."); return;
                }
                showQuickLogStep4_Schedule();
            });
            btns.getChildren().addAll(btnBack, btnNext);
            HBox.setHgrow(btnNext, Priority.ALWAYS);
            step.getChildren().addAll(lbl, txtBrand, txtModel, txtSerial, cbType, txtOtherType, errLbl, btns);
        });
    }

    private void showQuickLogStep4_Schedule() {
        buildQuickModal("Input New Device Log", step -> {
            Label lbl = sectionLabel("SCHEDULE");
            VBox summary = new VBox(4);
            summary.setStyle("-fx-background-color:#F8FAFC;-fx-background-radius:8;-fx-border-color:#E2E8F0;-fx-border-radius:8;-fx-border-width:1;-fx-padding:12 14;");
            Label sName = new Label(qlStudentName + "  (" + qlStudentCode + ")");
            sName.setStyle("-fx-font-weight:700;-fx-font-size:13px;-fx-text-fill:#1E293B;");
            Label sDev  = new Label(qlDeviceBrand + " " + qlDeviceModel + "  ·  " + qlDeviceSerial);
            sDev.setStyle("-fx-font-size:11px;-fx-text-fill:#64748B;");
            summary.getChildren().addAll(sName, sDev);

            Label lblIn = new Label("Ingress Date & Time *");
            lblIn.setStyle("-fx-font-size:12px;-fx-font-weight:600;-fx-text-fill:#374151;");
            HBox inRow = buildInlineTimeRow();
            DatePicker dpIn = (DatePicker) ((VBox) inRow.getChildren().get(0)).getChildren().get(0);
            @SuppressWarnings("unchecked")
            ComboBox<String> cbInH = (ComboBox<String>) ((HBox)((VBox) inRow.getChildren().get(0)).getChildren().get(1)).getChildren().get(0);
            @SuppressWarnings("unchecked")
            ComboBox<String> cbInM = (ComboBox<String>) ((HBox)((VBox) inRow.getChildren().get(0)).getChildren().get(1)).getChildren().get(2);

            Label lblOut = new Label("Egress Date & Time *");
            lblOut.setStyle("-fx-font-size:12px;-fx-font-weight:600;-fx-text-fill:#374151;");
            HBox outRow = buildInlineTimeRow();
            DatePicker dpOut = (DatePicker) ((VBox) outRow.getChildren().get(0)).getChildren().get(0);
            @SuppressWarnings("unchecked")
            ComboBox<String> cbOutH = (ComboBox<String>) ((HBox)((VBox) outRow.getChildren().get(0)).getChildren().get(1)).getChildren().get(0);
            @SuppressWarnings("unchecked")
            ComboBox<String> cbOutM = (ComboBox<String>) ((HBox)((VBox) outRow.getChildren().get(0)).getChildren().get(1)).getChildren().get(2);

            Label errLbl = errorLabel();
            HBox btns = new HBox(10);
            Button btnBack = secondaryBtn("← Back");
            btnBack.setOnAction(e -> {
                if (qlDeviceRegistered) showQuickLogStep3a_PickDevice();
                else showQuickLogStep3b_EnterDevice();
            });
            Button btnSubmit = primaryBtn("Submit Request");
            btnSubmit.setOnAction(e -> {
                if (dpIn.getValue() == null || dpOut.getValue() == null
                        || cbInH.getValue() == null || cbInM.getValue() == null
                        || cbOutH.getValue() == null || cbOutM.getValue() == null) {
                    showErr(errLbl, "Please fill in all date and time fields."); return;
                }
                LocalDateTime qInTime  = LocalDateTime.of(dpIn.getValue(),  LocalTime.of(Integer.parseInt(cbInH.getValue()),  Integer.parseInt(cbInM.getValue())));
                LocalDateTime qOutTime = LocalDateTime.of(dpOut.getValue(), LocalTime.of(Integer.parseInt(cbOutH.getValue()), Integer.parseInt(cbOutM.getValue())));
                LocalDateTime now = LocalDateTime.now();
                if (dpIn.getValue().isBefore(now.toLocalDate())) { showErr(errLbl, "Ingress date must be today or a future date."); return; }
                if (dpIn.getValue().isEqual(now.toLocalDate()) && !qInTime.isAfter(now)) { showErr(errLbl, "Ingress time must be after the current time."); return; }
                if (!qOutTime.isAfter(qInTime)) { showErr(errLbl, "Egress time must be after the ingress time."); return; }
                LocalTime[] hours = loadOperatingHours();
                if (hours != null) {
                    if (!isWithinOperatingHours(qInTime, hours[0], hours[1])) {
                        showErr(errLbl, "Ingress time is outside school hours (" + hours[0] + " – " + hours[1] + ")."); return;
                    }
                    if (!isWithinOperatingHours(qOutTime, hours[0], hours[1])) {
                        showErr(errLbl, "Egress time is outside school hours (" + hours[0] + " – " + hours[1] + ")."); return;
                    }
                }
                hideQuickLogModal();
                submitQuickDeviceLog(qInTime, qOutTime);
            });
            btns.getChildren().addAll(btnBack, btnSubmit);
            HBox.setHgrow(btnSubmit, Priority.ALWAYS);
            step.getChildren().addAll(lbl, summary, lblIn, inRow, lblOut, outRow, errLbl, btns);
        });
    }

    private void submitQuickDeviceLog(LocalDateTime ingressTime, LocalDateTime egressTime) {
        UserSession session = UserSession.getInstance();
        int requestedBy = session.getUserId();

        try (Connection conn = DBConnection.connect()) {
            conn.setAutoCommit(false);
            try {
                int studentId = qlStudentRegistered ? qlExistingStudentId : registerNewStudent(conn);
                int deviceId  = qlDeviceRegistered  ? qlExistingDeviceId  : registerNewDevice(conn, studentId);

                List<Integer> deviceIds = List.of(deviceId);
                String reqSql   = "INSERT INTO gate_requests (student_id,direction,scheduled_time,requested_by,status) VALUES(?,?,?,?,'PENDING') RETURNING request_id";
                String devSql   = "INSERT INTO gate_request_devices(request_id,device_id) VALUES(?,?)";
                String auditSql = "INSERT INTO audit_log(operator_id,action_type,target_type,target_id,details,performed_at) VALUES(?,'GATE_REQUEST_CREATED','gate_requests',?,?::jsonb,NOW() AT TIME ZONE 'Asia/Manila')";

                insertRequest(conn, reqSql, devSql, auditSql, studentId, "IN",  ingressTime, requestedBy, deviceIds);
                insertRequest(conn, reqSql, devSql, auditSql, studentId, "OUT", egressTime,  requestedBy, deviceIds);
                conn.commit();

                List<SelectedDevice> emailDevs = List.of(
                        new SelectedDevice(qlDeviceBrand, qlDeviceModel, qlDeviceSerial, qlDeviceType));
                sendCombinedGatePassEmailSafely(qlStudentEmail, qlStudentName, qlStudentCode,
                        qlStudentCourse, qlStudentYear, ingressTime, egressTime, emailDevs);

                loadRecentBatches();
                showSuccessRegistrationPopup(ingressTime, egressTime, deviceIds);

            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
                showAckPopup(false, "Submission Failed",
                        "Could not submit the request. Ensure the student code and serial number are not already registered, then try again.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAckPopup(false, "Connection Error", "Database connection failed.");
        }
    }

    private int registerNewStudent(Connection conn) throws SQLException {
        String sql = "INSERT INTO students(student_code,full_name,section,email,status) " +
                "VALUES(?,?,?,?,'ACTIVE') RETURNING student_id";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, qlStudentCode); pst.setString(2, qlStudentName);
            pst.setString(3, qlStudentCourse); pst.setString(4, qlStudentEmail);
            try (ResultSet rs = pst.executeQuery()) { rs.next(); return rs.getInt(1); }
        }
    }

    private int registerNewDevice(Connection conn, int studentId) throws SQLException {
        String sql = "INSERT INTO devices(serial_number,brand,model,device_type,owner_id,current_location,status) " +
                "VALUES(?,?,?,?,?,'OUT','ACTIVE') RETURNING device_id";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, qlDeviceSerial); pst.setString(2, qlDeviceBrand);
            pst.setString(3, qlDeviceModel);  pst.setString(4, qlDeviceType);
            pst.setInt(5, studentId);
            try (ResultSet rs = pst.executeQuery()) { rs.next(); return rs.getInt(1); }
        }
    }

    @FunctionalInterface
    private interface StepBuilder { void build(VBox stepContent); }

    private void buildQuickModal(String title, StepBuilder builder) {
        if (paneQuickLogModal == null) return;
        paneQuickLogModal.getChildren().clear();

        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.45);");
        overlay.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        VBox shell = new VBox(0);
        shell.setMaxWidth(480); shell.setPrefWidth(480);
        shell.setMaxHeight(Region.USE_PREF_SIZE);
        shell.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 12; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.22), 28, 0, 0, 8);");

        StackPane headerStack = new StackPane();
        headerStack.setStyle("-fx-background-color: #7A0000; -fx-background-radius: 12 12 0 0;");
        headerStack.setPadding(new Insets(16, 20, 16, 20));

        HBox headerContent = new HBox(12);
        headerContent.setAlignment(Pos.CENTER_LEFT);
        StackPane iconCircle = new StackPane();
        iconCircle.setMaxSize(32,32); iconCircle.setMinSize(32,32);
        iconCircle.setStyle("-fx-background-color: rgba(255,255,255,0.15); -fx-background-radius: 50;");
        SVGPath personIcon = new SVGPath();
        personIcon.setContent("M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z");
        personIcon.setStyle("-fx-stroke: #FFFFFF; -fx-fill: transparent; -fx-stroke-width: 1.5;");
        iconCircle.getChildren().add(personIcon);
        Label lblTitle = new Label(title);
        lblTitle.setStyle("-fx-font-family:'Inter','Segoe UI';-fx-font-weight:800;-fx-text-fill:#FFFFFF;-fx-font-size:15px;");
        HBox.setHgrow(lblTitle, Priority.ALWAYS);
        headerContent.getChildren().addAll(iconCircle, lblTitle);
        StackPane.setAlignment(headerContent, Pos.CENTER_LEFT);

        Button btnClose = new Button("✕");
        btnClose.setStyle("-fx-background-color:transparent;-fx-text-fill:rgba(255,255,255,0.8);-fx-font-size:15px;-fx-cursor:hand;-fx-padding:4 8;-fx-font-weight:bold;");
        btnClose.setOnAction(e -> hideQuickLogModal());
        StackPane.setAlignment(btnClose, Pos.TOP_RIGHT);
        headerStack.getChildren().addAll(headerContent, btnClose);

        VBox body = new VBox(14);
        body.setPadding(new Insets(24));
        builder.build(body);

        ScrollPane sp = new ScrollPane(body);
        sp.setFitToWidth(true); sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setStyle("-fx-background-color:transparent;-fx-background:transparent;-fx-border-color:transparent;");
        sp.setMaxHeight(500);

        shell.getChildren().addAll(headerStack, sp);
        overlay.getChildren().add(shell);
        StackPane.setAlignment(shell, Pos.CENTER);
        paneQuickLogModal.getChildren().add(overlay);

        paneQuickLogModal.setOpacity(0);
        paneQuickLogModal.setVisible(true);
        paneQuickLogModal.setManaged(true);
        FadeTransition fi = new FadeTransition(Duration.millis(180), paneQuickLogModal);
        fi.setFromValue(0); fi.setToValue(1); fi.play();
    }

    private void hideQuickLogModal() {
        if (paneQuickLogModal == null) return;
        FadeTransition fo = new FadeTransition(Duration.millis(150), paneQuickLogModal);
        fo.setFromValue(1); fo.setToValue(0);
        fo.setOnFinished(e -> {
            paneQuickLogModal.setVisible(false);
            paneQuickLogModal.setManaged(false);
            paneQuickLogModal.getChildren().clear();
        });
        fo.play();
    }

    private Label sectionLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size:10px;-fx-font-weight:700;-fx-text-fill:#C9A84C;-fx-letter-spacing:1px;");
        return lbl;
    }

    private Label errorLabel() {
        Label lbl = new Label();
        lbl.setStyle("-fx-text-fill:#DC2626;-fx-font-size:12px;-fx-font-weight:600;");
        lbl.setWrapText(true);
        lbl.setVisible(false); lbl.setManaged(false);
        return lbl;
    }

    private void showErr(Label lbl, String msg) {
        lbl.setText(msg); lbl.setVisible(true); lbl.setManaged(true);
    }

    private TextField styledField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setPrefHeight(40);
        tf.setStyle("-fx-background-radius:8;-fx-border-color:#D1D5DB;-fx-border-radius:8;-fx-font-size:13px;-fx-padding:0 12;-fx-pref-height:40;");
        return tf;
    }

    private ComboBox<String> styledCombo(String prompt, String... items) {
        ComboBox<String> cb = new ComboBox<>();
        cb.getItems().addAll(items);
        cb.setPromptText(prompt);
        cb.setMaxWidth(Double.MAX_VALUE);
        cb.setPrefHeight(40); cb.setMinHeight(40); cb.setMaxHeight(40);
        cb.setStyle("-fx-background-radius:8;-fx-border-color:#D1D5DB;-fx-border-radius:8;-fx-font-size:13px;-fx-pref-height:40;");
        return cb;
    }

    private Button toggleBtn(String text, boolean leftSide, boolean rightSide) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setPrefHeight(40);
        String radius = leftSide ? "8 0 0 8" : "0 8 8 0";
        btn.setStyle("-fx-background-radius:" + radius + ";-fx-background-color:#FDF6E3;" +
                "-fx-text-fill:#7A0000;-fx-font-size:13px;-fx-cursor:hand;-fx-padding:10 0;" +
                "-fx-border-color:#C9A84C;-fx-border-radius:" + radius + ";-fx-border-width:1;");
        return btn;
    }

    private void styleToggle(Button btn, boolean selected) {
        String existing = btn.getStyle();
        String radius = existing.contains("8 0 0 8") ? "8 0 0 8" : "0 8 8 0";
        if (selected) {
            btn.setStyle("-fx-background-color:#7A0000;-fx-text-fill:#FFFFFF;-fx-font-weight:bold;" +
                    "-fx-background-radius:" + radius + ";-fx-padding:10 0;-fx-font-size:13px;-fx-cursor:hand;");
        } else {
            btn.setStyle("-fx-background-color:#FDF6E3;-fx-text-fill:#7A0000;" +
                    "-fx-background-radius:" + radius + ";-fx-padding:10 0;-fx-font-size:13px;-fx-cursor:hand;" +
                    "-fx-border-color:#C9A84C;-fx-border-radius:" + radius + ";-fx-border-width:1;");
        }
    }

    private Button primaryBtn(String text) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE); btn.setPrefHeight(42);
        btn.setStyle("-fx-background-color:#7A0000;-fx-background-radius:8;-fx-text-fill:#FFFFFF;" +
                "-fx-font-weight:bold;-fx-font-size:13px;-fx-cursor:hand;");
        return btn;
    }

    private Button secondaryBtn(String text) {
        Button btn = new Button(text);
        btn.setPrefHeight(42); btn.setPrefWidth(100);
        btn.setStyle("-fx-background-color:#FDF6E3;-fx-background-radius:8;-fx-text-fill:#7A0000;" +
                "-fx-font-size:13px;-fx-cursor:hand;-fx-border-color:#C9A84C;-fx-border-radius:8;-fx-border-width:1;");
        return btn;
    }

    private HBox buildInlineTimeRow() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM d, yyyy");
        DatePicker dp = new DatePicker();
        dp.getEditor().setEditable(false);
        dp.setConverter(new StringConverter<>() {
            @Override public String toString(LocalDate d)   { return d != null ? d.format(fmt) : ""; }
            @Override public LocalDate fromString(String s) { try { return LocalDate.parse(s, fmt); } catch (Exception e) { return null; } }
        });
        dp.setPrefHeight(38); dp.setMaxWidth(Double.MAX_VALUE);
        dp.setStyle("-fx-background-radius:8;-fx-border-color:#D1D5DB;-fx-border-radius:8;-fx-font-size:12px;");

        LocalTime[] opHours = loadOperatingHours();
        int openHour  = (opHours != null) ? opHours[0].getHour() : 8;
        int closeHour = (opHours != null) ? opHours[1].getHour() : 20;

        List<String> hrs = new ArrayList<>();
        for (int h = openHour; h <= closeHour; h++) hrs.add(String.format("%02d", h));

        List<String> mins = new ArrayList<>();
        for (int m = 0; m < 60; m += 5) mins.add(String.format("%02d", m));
        mins.add("59"); Collections.sort(mins);

        ComboBox<String> cbH = new ComboBox<>(); cbH.getItems().addAll(hrs);
        cbH.setPromptText("HH"); cbH.setPrefWidth(68);
        cbH.setPrefHeight(38); cbH.setStyle("-fx-background-radius:8;-fx-border-color:#D1D5DB;-fx-border-radius:8;-fx-font-size:12px;");
        Label colon = new Label(":"); colon.setStyle("-fx-text-fill:#374151;-fx-font-weight:bold;");
        ComboBox<String> cbM = new ComboBox<>(); cbM.getItems().addAll(mins);
        cbM.setPromptText("MM"); cbM.setPrefWidth(68);
        cbM.setPrefHeight(38); cbM.setStyle("-fx-background-radius:8;-fx-border-color:#D1D5DB;-fx-border-radius:8;-fx-font-size:12px;");

        HBox timePart = new HBox(4, cbH, colon, cbM); timePart.setAlignment(Pos.CENTER_LEFT);
        VBox vb = new VBox(6, dp, timePart); HBox.setHgrow(vb, Priority.ALWAYS);
        HBox outer = new HBox(vb); HBox.setHgrow(vb, Priority.ALWAYS);
        return outer;
    }

    private void showSuccessRegistrationPopup(LocalDateTime ingressTime, LocalDateTime egressTime, List<Integer> deviceIds) {
        if (paneRegistrationModal == null) return;
        paneRegistrationModal.getChildren().clear();

        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.45);");
        overlay.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        VBox shell = new VBox(20); shell.setAlignment(Pos.CENTER);
        shell.setMaxWidth(400); shell.setMaxHeight(Region.USE_PREF_SIZE);
        shell.setPadding(new Insets(32));
        shell.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 16; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.20), 28, 0, 0, 8);");

        StackPane iconCircle = new StackPane(); iconCircle.setMaxSize(64,64); iconCircle.setMinSize(64,64);
        iconCircle.setStyle("-fx-background-color: #FDF6E3; -fx-background-radius: 50; -fx-border-color: #C9A84C; -fx-border-radius: 50; -fx-border-width: 3;");
        SVGPath chk = new SVGPath(); chk.setContent("M5 13l4 4L19 7");
        chk.setStyle("-fx-stroke: #C9A84C; -fx-stroke-width: 2.5; -fx-fill: transparent; -fx-stroke-linecap: round; -fx-scale-x: 1.3; -fx-scale-y: 1.3;");
        iconCircle.getChildren().add(chk);

        Label lblTitle = new Label("Gate Request Submitted Successfully");
        lblTitle.setStyle("-fx-font-family:'Inter','Segoe UI';-fx-font-weight:800;-fx-text-fill:#111827;-fx-font-size:18px;");
        lblTitle.setWrapText(true); lblTitle.setMaxWidth(320); lblTitle.setAlignment(Pos.CENTER);
        lblTitle.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        Label lblSub = new Label("The device ingress and egress request was submitted successfully.");
        lblSub.setStyle("-fx-font-size:13px;-fx-text-fill:#6B7280;");
        lblSub.setWrapText(true); lblSub.setMaxWidth(320); lblSub.setAlignment(Pos.CENTER);

        VBox timesBox = new VBox(6);
        timesBox.setStyle("-fx-background-color:#F9FAFB;-fx-background-radius:8;-fx-border-color:#E5E7EB;-fx-border-radius:8;-fx-padding:14;");
        HBox inRow = new HBox(10); inRow.setAlignment(Pos.CENTER_LEFT);
        Label inLabel = new Label("Ingress"); inLabel.setStyle("-fx-background-color:#7A0000;-fx-text-fill:#FFFFFF;-fx-padding:3 8;-fx-background-radius:4;-fx-font-size:11px;-fx-font-weight:bold;");
        Label inTime  = new Label(ingressTime.format(DISPLAY_FMT)); inTime.setStyle("-fx-font-size:12px;-fx-text-fill:#374151;-fx-font-weight:600;");
        inRow.getChildren().addAll(inLabel, inTime);
        HBox outRow = new HBox(10); outRow.setAlignment(Pos.CENTER_LEFT);
        Label outLabel = new Label("Egress"); outLabel.setStyle("-fx-background-color:#C9A84C;-fx-text-fill:#FFFFFF;-fx-padding:3 8;-fx-background-radius:4;-fx-font-size:11px;-fx-font-weight:bold;");
        Label outTime  = new Label(egressTime.format(DISPLAY_FMT)); outTime.setStyle("-fx-font-size:12px;-fx-text-fill:#374151;-fx-font-weight:600;");
        outRow.getChildren().addAll(outLabel, outTime);
        timesBox.getChildren().addAll(inRow, outRow);

        Label lblDevCount = new Label(deviceIds.size() + " device(s) included");
        lblDevCount.setStyle("-fx-font-size:11px;-fx-text-fill:#9CA3AF;-fx-font-style:italic;"); lblDevCount.setAlignment(Pos.CENTER);

        Button btnDone = new Button("Done  ✓"); btnDone.setMaxWidth(Double.MAX_VALUE); btnDone.setPrefHeight(42);
        btnDone.setStyle("-fx-background-color:#7A0000;-fx-background-radius:8;-fx-text-fill:#FFFFFF;-fx-font-weight:bold;-fx-font-size:14px;-fx-cursor:hand;");
        btnDone.setOnAction(e -> hideRegistrationPopup());

        shell.getChildren().addAll(iconCircle, lblTitle, lblSub, timesBox, lblDevCount, btnDone);
        overlay.getChildren().add(shell); StackPane.setAlignment(shell, Pos.CENTER);
        paneRegistrationModal.getChildren().add(overlay);

        paneRegistrationModal.setOpacity(0); paneRegistrationModal.setVisible(true); paneRegistrationModal.setManaged(true);
        shell.setScaleX(0.85); shell.setScaleY(0.85);
        FadeTransition fi = new FadeTransition(Duration.millis(200), paneRegistrationModal); fi.setFromValue(0); fi.setToValue(1); fi.play();
        ScaleTransition si = new ScaleTransition(Duration.millis(220), shell); si.setFromX(0.85); si.setFromY(0.85); si.setToX(1.0); si.setToY(1.0); si.play();
    }

    private void hideRegistrationPopup() {
        if (paneRegistrationModal == null) return;
        FadeTransition fo = new FadeTransition(Duration.millis(150), paneRegistrationModal);
        fo.setFromValue(1); fo.setToValue(0);
        fo.setOnFinished(e -> { paneRegistrationModal.setVisible(false); paneRegistrationModal.setManaged(false); paneRegistrationModal.getChildren().clear(); });
        fo.play();
    }

    @FXML public void handleCloseModal() {
        paneDeviceSelectModal.setVisible(false); paneDeviceSelectModal.setManaged(false);
        paneConfirmModal.setVisible(false);      paneConfirmModal.setManaged(false);
        if (paneScheduleModal != null) { paneScheduleModal.setVisible(false); paneScheduleModal.setManaged(false); }
        selectedDevices.clear();
    }

    private void showAckPopup(boolean success, String title, String message) {
        if (paneAckModal == null) return;
        paneAckModal.getChildren().clear();

        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.35);");
        overlay.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        VBox card = new VBox(20); card.setAlignment(Pos.CENTER);
        card.setMaxWidth(340); card.setMaxHeight(Region.USE_PREF_SIZE);
        card.setPadding(new Insets(36, 32, 32, 32));
        card.setStyle("-fx-background-color:#FFFFFF;-fx-background-radius:16;-fx-effect:dropshadow(three-pass-box,rgba(0,0,0,0.18),24,0,0,6);");

        StackPane iconCircle = new StackPane(); iconCircle.setMaxSize(64,64); iconCircle.setMinSize(64,64);
        SVGPath icon = new SVGPath();
        if (success) {
            iconCircle.setStyle("-fx-background-color:#FDF6E3;-fx-background-radius:50;-fx-border-color:#C9A84C;-fx-border-radius:50;-fx-border-width:3;");
            icon.setContent("M5 13l4 4L19 7"); icon.setStyle("-fx-stroke:#C9A84C;-fx-stroke-width:2.5;-fx-fill:transparent;-fx-stroke-linecap:round;-fx-scale-x:1.3;-fx-scale-y:1.3;");
        } else {
            iconCircle.setStyle("-fx-background-color:#FDF0F0;-fx-background-radius:50;-fx-border-color:#7A0000;-fx-border-radius:50;-fx-border-width:3;");
            icon.setContent("M6 18L18 6M6 6l12 12"); icon.setStyle("-fx-stroke:#7A0000;-fx-stroke-width:2.5;-fx-fill:transparent;-fx-stroke-linecap:round;-fx-scale-x:1.1;-fx-scale-y:1.1;");
        }
        iconCircle.getChildren().add(icon);

        Label lTitle = new Label(title); lTitle.setStyle("-fx-font-family:'Inter','Segoe UI';-fx-font-weight:bold;-fx-text-fill:#1E293B;-fx-font-size:18px;");
        Label lMsg   = new Label(message); lMsg.setStyle("-fx-font-family:'Inter','Segoe UI';-fx-text-fill:#64748B;-fx-font-size:13px;-fx-text-alignment:center;");
        lMsg.setWrapText(true); lMsg.setMaxWidth(260); lMsg.setAlignment(Pos.CENTER);

        String btnColor = success ? "#C9A84C" : "#7A0000";
        Button btnClose = new Button("Close"); btnClose.setMaxWidth(Double.MAX_VALUE); btnClose.setPrefHeight(42);
        btnClose.setStyle("-fx-background-color:" + btnColor + ";-fx-background-radius:8;-fx-text-fill:#FFFFFF;-fx-font-weight:bold;-fx-font-size:14px;-fx-cursor:hand;");
        btnClose.setOnAction(e -> hideAckPopup());

        card.getChildren().addAll(iconCircle, lTitle, lMsg, btnClose);
        overlay.getChildren().add(card); StackPane.setAlignment(card, Pos.CENTER);
        paneAckModal.getChildren().add(overlay);
        paneAckModal.setVisible(true); paneAckModal.setManaged(true); paneAckModal.setOpacity(0);
        card.setScaleX(0.85); card.setScaleY(0.85);
        FadeTransition fi = new FadeTransition(Duration.millis(180), paneAckModal); fi.setFromValue(0); fi.setToValue(1); fi.play();
        ScaleTransition si = new ScaleTransition(Duration.millis(200), card); si.setFromX(0.85); si.setFromY(0.85); si.setToX(1.0); si.setToY(1.0); si.play();
    }

    private void hideAckPopup() {
        FadeTransition fo = new FadeTransition(Duration.millis(150), paneAckModal);
        fo.setFromValue(1); fo.setToValue(0);
        fo.setOnFinished(e -> { paneAckModal.setVisible(false); paneAckModal.setManaged(false); paneAckModal.getChildren().clear(); });
        fo.play();
    }

    private void loadRecentBatches() {
        paneBatchList.getChildren().clear();
        String sql = "SELECT dl.batch_id, dl.direction, s.student_code, s.full_name, " +
                "MAX(dl.log_time) AS batch_time, COUNT(*) AS device_count, " +
                "STRING_AGG(d.brand || ' ' || d.model, ', ') AS device_names, " +
                "CASE WHEN BOOL_OR(dl.status='VOIDED') THEN 'VOIDED' " +
                "     WHEN BOOL_OR(dl.status='AMENDED') THEN 'AMENDED' " +
                "     ELSE 'NORMAL' END AS batch_status " +
                "FROM device_logs dl " +
                "JOIN devices d ON d.device_id = dl.device_id " +
                "JOIN students s ON s.student_id = d.owner_id " +
                "GROUP BY dl.batch_id, dl.direction, s.student_code, s.full_name " +
                "ORDER BY batch_time DESC LIMIT 8";

        List<BatchRow> batches = new ArrayList<>();
        int total = 0;
        try (Connection conn = DBConnection.connect();
             PreparedStatement pst = conn.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
                BatchRow b = new BatchRow();
                b.studentCode = rs.getString("student_code");
                b.studentName = rs.getString("full_name");
                b.direction   = rs.getString("direction");
                b.deviceCount = rs.getInt("device_count");
                b.deviceNames = rs.getString("device_names");
                b.status      = rs.getString("batch_status");
                Timestamp ts  = rs.getTimestamp("batch_time");
                b.time = ts != null ? ts.toInstant().atZone(java.time.ZoneId.of("Asia/Manila")).format(TIME_FMT) : "";
                batches.add(b); total++;
            }
        } catch (SQLException e) { e.printStackTrace(); }

        lblBatchCount.setText(total + " Today");
        if (batches.isEmpty()) { paneNoBatches.setVisible(true); paneNoBatches.setManaged(true); return; }
        paneNoBatches.setVisible(false); paneNoBatches.setManaged(false);
        for (BatchRow b : batches) paneBatchList.getChildren().add(buildBatchCard(b));
    }

    private VBox buildBatchCard(BatchRow b) {
        VBox card = new VBox(6);
        card.setStyle("-fx-background-color:#FFFFFF;-fx-background-radius:10;-fx-border-color:#C9A84C;-fx-border-radius:10;-fx-border-width:1;");
        card.setPadding(new Insets(14,16,14,16));
        if ("VOIDED".equals(b.status)) card.setOpacity(0.6);

        HBox header = new HBox(8); header.setAlignment(Pos.CENTER_LEFT);
        Label lblName = new Label(b.studentName); lblName.setStyle("-fx-font-weight:700;-fx-text-fill:#111827;-fx-font-size:13px;");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Label lblDir = new Label("IN".equals(b.direction) ? "INGRESS" : "EGRESS");
        lblDir.setStyle("IN".equals(b.direction)
                ? "-fx-background-color:#7A0000;-fx-text-fill:#FFFFFF;-fx-padding:3 8;-fx-background-radius:6;-fx-font-size:10px;-fx-font-weight:700;"
                : "-fx-background-color:#C9A84C;-fx-text-fill:#FFFFFF;-fx-padding:3 8;-fx-background-radius:6;-fx-font-size:10px;-fx-font-weight:700;");
        header.getChildren().addAll(lblName, spacer, lblDir);
        if ("VOIDED".equals(b.status) || "AMENDED".equals(b.status)) {
            Label lblSt = new Label(b.status);
            lblSt.setStyle("VOIDED".equals(b.status)
                    ? "-fx-background-color:#FDF6E3;-fx-text-fill:#7A0000;-fx-padding:3 8;-fx-background-radius:6;-fx-font-size:10px;-fx-font-weight:700;-fx-border-color:#C9A84C;-fx-border-radius:6;-fx-border-width:1;"
                    : "-fx-background-color:#FDF6E3;-fx-text-fill:#C9A84C;-fx-padding:3 8;-fx-background-radius:6;-fx-font-size:10px;-fx-font-weight:700;-fx-border-color:#C9A84C;-fx-border-radius:6;-fx-border-width:1;");
            header.getChildren().add(lblSt);
        }
        Label lblMeta    = new Label(b.studentCode + "  •  " + b.deviceCount + " Device(s)  •  " + b.time);
        lblMeta.setStyle("-fx-text-fill:#6B7280;-fx-font-size:11px;");
        Label lblDevices = new Label(b.deviceNames);
        lblDevices.setStyle("-fx-text-fill:#4B5563;-fx-font-size:11px;-fx-font-style:italic;");
        lblDevices.setWrapText(true);
        card.getChildren().addAll(header, lblMeta, lblDevices);
        return card;
    }

    private static class BatchRow {
        String studentCode, studentName, direction, deviceNames, status, time;
        int deviceCount;
    }
}