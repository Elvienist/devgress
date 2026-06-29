package com.example.byodsystem.byod.controller;

import com.example.byodsystem.byod.database.DBConnection;
import com.example.byodsystem.byod.service.AuditLogger;
import com.example.byodsystem.byod.service.EmailService;
import com.example.byodsystem.byod.service.UserSession;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import com.example.byodsystem.byod.utils.AlertHelper;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ActivityLogController {

    @FXML private Label lblCurrentDate;
    @FXML private Label lblRoleBadge;
    @FXML private Label lblUserInitial;

    @FXML private Label lblTotalEntries;
    @FXML private Label lblTotalExits;
    @FXML private Label lblUniqueStudents;

    @FXML private TextField txtSearch;
    @FXML private TextField txtDateFilter;
    @FXML private Button    btnFilter;

    @FXML private VBox paneTableRows;

    // ── Gate Requests sidebar ──
    @FXML private Label lblGateReqPendingBadge;
    @FXML private VBox  paneGateReqEmpty;
    @FXML private VBox  paneGateRequestList;

    // ── Amend / Void popup (floating modal, dark blurred overlay) ──
    @FXML private StackPane paneAmendVoidModal;

    private static final DateTimeFormatter DT_DISPLAY   = DateTimeFormatter.ofPattern("MMM d, yyyy  hh:mm a");

    /**
     * A paired gate-request group: one PENDING ingress (IN) request and,
     * optionally, a PENDING egress (OUT) request covering the same student
     * and device set.  Both share the same device list and student details.
     *
     * Rules enforced by the UI and re-checked at write time:
     *   • Either or both schedules may be amended independently.
     *   • Egress scheduled_time must always be STRICTLY AFTER ingress.
     *   • Both new schedules must be strictly in the future (> now).
     *   • Only the ingress (IN) request may be voided; voiding it
     *     cascade-rejects the paired egress (if any).
     *   • Egress can never be voided.
     */
    private static class GateRequestPair {
        // ── Ingress (always present) ──
        int    inRequestId;
        LocalDateTime inScheduledTime;

        // ── Egress (may be null if no paired OUT request is pending) ──
        Integer       outRequestId;      // null = no pending egress
        LocalDateTime outScheduledTime;  // null if outRequestId == null

        // ── Shared student / device details ──
        int    studentId;
        String studentName;
        String studentCode;
        String studentSection;
        String studentEmail;
        String status;   // always "PENDING"
        List<DeviceEntry> devices = new ArrayList<>();
    }

    private static class DeviceEntry {
        int    deviceId;
        String brand, model, serial, deviceType;
    }

    @FXML
    public void initialize() {
        UserSession session = UserSession.getInstance();
        String role = session.getRole() != null ? session.getRole() : "Admin";
        lblRoleBadge.setText(role.substring(0, 1).toUpperCase() + role.substring(1).toLowerCase());
        String username = session.getUsername();
        lblUserInitial.setText(username != null && !username.isEmpty()
                ? String.valueOf(username.charAt(0)).toUpperCase() : "A");
        lblCurrentDate.setText(LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")));

        txtSearch.textProperty().addListener((obs, o, n) -> loadLogs());
        txtDateFilter.textProperty().addListener((obs, o, n) -> loadLogs());

        loadStats();
        loadLogs();
        loadGateRequests();
    }

    private void loadStats() {
        String sql = "SELECT " +
                "  COUNT(*) FILTER (WHERE direction = 'IN')  AS total_in, " +
                "  COUNT(*) FILTER (WHERE direction = 'OUT') AS total_out, " +
                "  COUNT(DISTINCT d.owner_id)                AS unique_students " +
                "FROM device_logs dl " +
                "JOIN devices d ON d.device_id = dl.device_id";
        try (Connection conn = DBConnection.connect();
             PreparedStatement pst = conn.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {
            if (rs.next()) {
                lblTotalEntries.setText(String.valueOf(rs.getInt("total_in")));
                lblTotalExits.setText(String.valueOf(rs.getInt("total_out")));
                lblUniqueStudents.setText(String.valueOf(rs.getInt("unique_students")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // ACTIVITY LOG TABLE — device_logs is the permanent, immutable record
    // of every gate movement.
    // ════════════════════════════════════════════════════════════════════

    private void loadLogs() {
        paneTableRows.getChildren().clear();

        String search = txtSearch.getText() == null ? "" : txtSearch.getText().trim();
        String date   = txtDateFilter.getText() == null ? "" : txtDateFilter.getText().trim();

        java.sql.Date sqlDate = null;
        if (!date.isEmpty()) {
            try { sqlDate = java.sql.Date.valueOf(date); } catch (IllegalArgumentException ex) { }
        }

        StringBuilder sql = new StringBuilder(
                "SELECT dl.log_id, dl.log_time, dl.direction, dl.status, dl.batch_id, " +
                        "       s.full_name, s.student_code, s.section, s.email, " +
                        "       d.brand, d.model, d.serial_number, " +
                        "       u.username AS operator_name " +
                        "FROM device_logs dl " +
                        "JOIN devices  d ON d.device_id  = dl.device_id " +
                        "JOIN students s ON s.student_id  = d.owner_id " +
                        "JOIN users    u ON u.user_id     = dl.operator_id " +
                        "WHERE 1=1 "
        );

        if (!search.isEmpty()) {
            sql.append("AND (LOWER(s.full_name) LIKE LOWER(?) " +
                    "  OR LOWER(s.student_code) LIKE LOWER(?) " +
                    "  OR LOWER(d.serial_number) LIKE LOWER(?)) ");
        }
        if (sqlDate != null) {
            sql.append("AND CAST(dl.log_time AS DATE) = ? ");
        }
        sql.append("ORDER BY dl.log_time DESC LIMIT 100");

        try (Connection conn = DBConnection.connect();
             PreparedStatement pst = conn.prepareStatement(sql.toString())) {

            int idx = 1;
            if (!search.isEmpty()) {
                String like = "%" + search + "%";
                pst.setString(idx++, like);
                pst.setString(idx++, like);
                pst.setString(idx++, like);
            }
            if (sqlDate != null) pst.setDate(idx, sqlDate);

            try (ResultSet rs = pst.executeQuery()) {
                boolean odd = true;
                while (rs.next()) {
                    String ts = rs.getTimestamp("log_time") != null
                            ? rs.getTimestamp("log_time").toInstant()
                              .atZone(java.time.ZoneId.of("Asia/Manila"))
                              .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "—";
                    paneTableRows.getChildren().add(buildRow(
                            ts,
                            rs.getString("full_name"), rs.getString("student_code"),
                            rs.getString("brand") + " " + rs.getString("model"),
                            rs.getString("serial_number"),
                            rs.getString("direction"),
                            rs.getString("operator_name"),
                            rs.getString("status"), odd));
                    odd = !odd;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private HBox buildRow(String ts, String fullName, String stuCode,
                          String device, String serial,
                          String dir, String operator, String status, boolean odd) {

        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 16, 10, 16));
        String baseStyle = "-fx-background-color: " + (odd ? "#FFFFFF" : "#F3F4F6") + ";" +
                "-fx-border-color: #E5E7EB; -fx-border-width: 0 0 1 0;" +
                "-fx-focus-color: transparent; -fx-faint-focus-color: transparent;";
        row.setStyle(baseStyle);

        Label lblTs = new Label(ts);
        lblTs.setPrefWidth(140); lblTs.setMinWidth(140); lblTs.setMaxWidth(140);
        lblTs.setWrapText(true);
        lblTs.setStyle("-fx-text-fill: #374151; -fx-font-size: 12px;");

        VBox student = new VBox(2);
        student.setPrefWidth(160); student.setMinWidth(160); student.setMaxWidth(160);
        Label lblName = new Label(fullName);
        lblName.setWrapText(true);
        lblName.setStyle("-fx-text-fill: #111111; -fx-font-weight: bold; -fx-font-size: 13px;");
        Label lblCode = new Label(stuCode);
        lblCode.setWrapText(true);
        lblCode.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 11px;");
        student.getChildren().addAll(lblName, lblCode);

        Label lblDevice = new Label(device);
        lblDevice.setPrefWidth(160); lblDevice.setMinWidth(160); lblDevice.setMaxWidth(160);
        lblDevice.setWrapText(true);
        lblDevice.setStyle("-fx-text-fill: #374151; -fx-font-size: 12px;");

        Label lblSerial = new Label(serial);
        lblSerial.setPrefWidth(120); lblSerial.setMinWidth(120); lblSerial.setMaxWidth(120);
        lblSerial.setWrapText(true);
        lblSerial.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 12px;");

        boolean isIn = "IN".equals(dir);
        Label lblDir = new Label(isIn ? "IN" : "OUT");
        lblDir.setPrefWidth(60); lblDir.setMinWidth(60); lblDir.setMaxWidth(60);
        lblDir.setAlignment(Pos.CENTER);
        lblDir.setStyle(isIn
                ? "-fx-background-color: #D1FAE5; -fx-text-fill: #065F46; -fx-background-radius: 4; -fx-padding: 2 4; -fx-font-weight: bold; -fx-font-size: 11px;"
                : "-fx-background-color: #CFFAFE; -fx-text-fill: #0E7490; -fx-background-radius: 4; -fx-padding: 2 4; -fx-font-weight: bold; -fx-font-size: 11px;");

        Label lblOp = new Label(operator);
        lblOp.setPrefWidth(130); lblOp.setMinWidth(130); lblOp.setMaxWidth(130);
        lblOp.setWrapText(true);
        lblOp.setStyle("-fx-text-fill: #374151; -fx-font-size: 12px;");

        Label lblStatus = buildStatusBadge(status);
        lblStatus.setPrefWidth(90); lblStatus.setMinWidth(90); lblStatus.setMaxWidth(90);

        row.getChildren().addAll(lblTs, student, lblDevice, lblSerial, lblDir, lblOp, lblStatus);
        row.setFocusTraversable(false);
        return row;
    }

    private Label buildStatusBadge(String status) {
        Label lbl = new Label(status != null ? status : "—");
        lbl.setAlignment(Pos.CENTER);
        switch (status == null ? "" : status.toUpperCase()) {
            case "NORMAL"  -> lbl.setStyle("-fx-background-color: #F3F4F6; -fx-text-fill: #374151; -fx-background-radius: 4; -fx-padding: 2 8; -fx-font-size: 11px; -fx-border-color: #D1D5DB; -fx-border-radius: 4;");
            case "AMENDED" -> lbl.setStyle("-fx-background-color: #FEF3C7; -fx-text-fill: #92400E; -fx-background-radius: 4; -fx-padding: 2 8; -fx-font-size: 11px; -fx-font-weight: bold;");
            case "VOIDED"  -> lbl.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #991B1B; -fx-background-radius: 4; -fx-padding: 2 8; -fx-font-size: 11px; -fx-font-weight: bold;");
            default        -> lbl.setStyle("-fx-background-color: #F3F4F6; -fx-text-fill: #6B7280; -fx-background-radius: 4; -fx-padding: 2 8; -fx-font-size: 11px;");
        }
        return lbl;
    }

    @FXML
    public void handleFilter() { loadLogs(); }

    // ════════════════════════════════════════════════════════════════════
    // GATE REQUESTS PANEL
    //
    // Pending requests are displayed as PAIRS: one card per (IN + OUT)
    // combination for the same student and same device set.
    // Both schedules are shown on the card and both can be amended in the
    // same modal.  Only the ingress can be voided.
    //
    // SQL strategy: fetch all PENDING IN requests, then for each one look
    // for a PENDING OUT from the same student covering the same devices.
    // ════════════════════════════════════════════════════════════════════

    /**
     * Fetches every PENDING ingress, then for each one tries to find a
     * matching PENDING egress (same student, same device set).  Results
     * are returned as {@link GateRequestPair} objects (one per ingress).
     */
    private static final String FETCH_PENDING_IN_SQL = """
            SELECT gr.request_id, gr.student_id, gr.scheduled_time,
                   s.full_name AS student_name, s.student_code, s.section,
                   COALESCE(s.email, '') AS student_email
            FROM gate_requests gr
            JOIN students s ON s.student_id = gr.student_id
            WHERE gr.status = 'PENDING'
              AND gr.direction = 'IN'
            ORDER BY gr.scheduled_time ASC
            """;

    /**
     * For a given PENDING IN request, find the PENDING OUT that:
     *   1. Belongs to the same student.
     *   2. Covers exactly the same device set.
     * "Exactly the same device set" = the OUT request has no device that
     * is not in the IN request AND the IN request has no device that is
     * not in the OUT request.
     */
    private static final String FETCH_PAIRED_OUT_SQL = """
            SELECT gr.request_id, gr.scheduled_time
            FROM gate_requests gr
            WHERE gr.status = 'PENDING'
              AND gr.direction = 'OUT'
              AND gr.student_id = ?
              AND NOT EXISTS (
                SELECT 1 FROM gate_request_devices grd_out
                WHERE grd_out.request_id = gr.request_id
                  AND grd_out.device_id NOT IN (
                    SELECT device_id FROM gate_request_devices WHERE request_id = ?
                  )
              )
              AND NOT EXISTS (
                SELECT 1 FROM gate_request_devices grd_in
                WHERE grd_in.request_id = ?
                  AND grd_in.device_id NOT IN (
                    SELECT device_id FROM gate_request_devices WHERE request_id = gr.request_id
                  )
              )
            ORDER BY gr.scheduled_time ASC
            LIMIT 1
            """;

    private static final String FETCH_DEVICES_FOR_REQUEST_SQL = """
            SELECT d.device_id, d.brand, d.model, d.serial_number, d.device_type
            FROM gate_request_devices grd
            JOIN devices d ON d.device_id = grd.device_id
            WHERE grd.request_id = ?
            ORDER BY d.device_id ASC
            """;

    private void loadGateRequests() {
        paneGateRequestList.getChildren().clear();

        List<GateRequestPair> pairs = fetchPendingGateRequestPairs();

        // Count badge: count individual pending requests (IN + OUT separately)
        int pendingCount = pairs.stream().mapToInt(p -> 1 + (p.outRequestId != null ? 1 : 0)).sum();
        lblGateReqPendingBadge.setText(pendingCount + " Pending");

        if (pairs.isEmpty()) {
            paneGateReqEmpty.setVisible(true);
            paneGateReqEmpty.setManaged(true);
            paneGateRequestList.setVisible(false);
            paneGateRequestList.setManaged(false);
        } else {
            paneGateReqEmpty.setVisible(false);
            paneGateReqEmpty.setManaged(false);
            paneGateRequestList.setVisible(true);
            paneGateRequestList.setManaged(true);
            for (GateRequestPair pair : pairs) {
                paneGateRequestList.getChildren().add(buildGateRequestCard(pair));
            }
        }
    }

    private List<GateRequestPair> fetchPendingGateRequestPairs() {
        List<GateRequestPair> list = new ArrayList<>();
        // Track which OUT request IDs have already been paired so they are
        // never shown as orphans (if an OUT has no matching IN, it is still
        // visible — we add it as a pair with a synthetic IN-only placeholder
        // below).  For now the primary grouping is always anchored on IN.
        java.util.Set<Integer> pairedOutIds = new java.util.HashSet<>();

        try (Connection conn = DBConnection.connect()) {
            // ── Step 1: all PENDING IN requests ──
            try (PreparedStatement pst = conn.prepareStatement(FETCH_PENDING_IN_SQL);
                 ResultSet rs = pst.executeQuery()) {

                while (rs.next()) {
                    GateRequestPair pair = new GateRequestPair();
                    pair.inRequestId    = rs.getInt("request_id");
                    pair.studentId      = rs.getInt("student_id");
                    pair.studentName    = rs.getString("student_name");
                    pair.studentCode    = rs.getString("student_code");
                    pair.studentSection = rs.getString("section");
                    pair.studentEmail   = rs.getString("student_email");
                    pair.status         = "PENDING";

                    Timestamp ts = rs.getTimestamp("scheduled_time");
                    if (ts != null) pair.inScheduledTime = ts.toLocalDateTime();

                    pair.devices = fetchDevicesForRequest(conn, pair.inRequestId);

                    // ── Step 2: try to find a paired OUT ──
                    try (PreparedStatement pst2 = conn.prepareStatement(FETCH_PAIRED_OUT_SQL)) {
                        pst2.setInt(1, pair.studentId);
                        pst2.setInt(2, pair.inRequestId);
                        pst2.setInt(3, pair.inRequestId);
                        try (ResultSet rs2 = pst2.executeQuery()) {
                            if (rs2.next()) {
                                pair.outRequestId = rs2.getInt("request_id");
                                Timestamp ts2 = rs2.getTimestamp("scheduled_time");
                                if (ts2 != null) pair.outScheduledTime = ts2.toLocalDateTime();
                                pairedOutIds.add(pair.outRequestId);
                            }
                        }
                    }

                    list.add(pair);
                }
            }

            // ── Step 3: any PENDING OUT that has no matching IN (orphan) ──
            // Show them as a pair with null inRequestId so the UI can still
            // display and amend them (void is still blocked for OUT).
            String orphanOutSql = """
                    SELECT gr.request_id, gr.student_id, gr.scheduled_time,
                           s.full_name AS student_name, s.student_code, s.section,
                           COALESCE(s.email, '') AS student_email
                    FROM gate_requests gr
                    JOIN students s ON s.student_id = gr.student_id
                    WHERE gr.status = 'PENDING'
                      AND gr.direction = 'OUT'
                    ORDER BY gr.scheduled_time ASC
                    """;
            try (PreparedStatement pst = conn.prepareStatement(orphanOutSql);
                 ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    int outId = rs.getInt("request_id");
                    if (pairedOutIds.contains(outId)) continue; // already paired
                    GateRequestPair pair = new GateRequestPair();
                    pair.inRequestId    = -1; // sentinel: no IN
                    pair.studentId      = rs.getInt("student_id");
                    pair.studentName    = rs.getString("student_name");
                    pair.studentCode    = rs.getString("student_code");
                    pair.studentSection = rs.getString("section");
                    pair.studentEmail   = rs.getString("student_email");
                    pair.status         = "PENDING";
                    pair.outRequestId   = outId;
                    Timestamp ts = rs.getTimestamp("scheduled_time");
                    if (ts != null) pair.outScheduledTime = ts.toLocalDateTime();
                    pair.devices = fetchDevicesForRequest(conn, outId);
                    list.add(pair);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    private List<DeviceEntry> fetchDevicesForRequest(Connection conn, int requestId) {
        List<DeviceEntry> devices = new ArrayList<>();
        try (PreparedStatement pst = conn.prepareStatement(FETCH_DEVICES_FOR_REQUEST_SQL)) {
            pst.setInt(1, requestId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    DeviceEntry de = new DeviceEntry();
                    de.deviceId   = rs.getInt("device_id");
                    de.brand      = rs.getString("brand");
                    de.model      = rs.getString("model");
                    de.serial     = rs.getString("serial_number");
                    de.deviceType = rs.getString("device_type");
                    devices.add(de);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return devices;
    }

    // ── Combined ingress + egress card ──
    private VBox buildGateRequestCard(GateRequestPair pair) {
        boolean hasIn  = pair.inRequestId > 0;
        boolean hasOut = pair.outRequestId != null;

        VBox card = new VBox(0);
        card.setStyle(
                "-fx-background-color: #FFFFFF; -fx-background-radius: 12; " +
                        "-fx-border-color: #C9A84C; -fx-border-radius: 12; -fx-border-width: 1.2; " +
                        "-fx-cursor: hand;"
        );
        card.setPadding(new Insets(12, 14, 12, 14));
        VBox.setMargin(card, new Insets(0, 0, 10, 0));

        // ── Top row: avatar + name + badge(s) ──
        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);

        StackPane avatar = new StackPane();
        avatar.setMinSize(32, 32); avatar.setMaxSize(32, 32);
        avatar.setStyle("-fx-background-color: #FDF6E3; -fx-background-radius: 50;");
        Label avatarLbl = new Label(pair.studentName.substring(0, 1).toUpperCase());
        avatarLbl.setStyle("-fx-text-fill: #7A0000; -fx-font-weight: bold; -fx-font-size: 13px;");
        avatar.getChildren().add(avatarLbl);

        VBox nameBlock = new VBox(1);
        HBox.setHgrow(nameBlock, Priority.ALWAYS);
        Label lblName = new Label(pair.studentName);
        lblName.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");
        Label lblMeta = new Label(pair.studentCode);
        lblMeta.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748B;");
        nameBlock.getChildren().addAll(lblName, lblMeta);

        HBox badges = new HBox(4);
        badges.setAlignment(Pos.CENTER_RIGHT);
        if (hasIn) {
            Label inBadge = new Label("↓ IN");
            inBadge.setStyle("-fx-background-color: #D1FAE5; -fx-text-fill: #065F46; " +
                    "-fx-background-radius: 12; -fx-padding: 3 9; -fx-font-size: 10px; -fx-font-weight: bold;");
            badges.getChildren().add(inBadge);
        }
        if (hasOut) {
            Label outBadge = new Label("↑ OUT");
            outBadge.setStyle("-fx-background-color: #CFFAFE; -fx-text-fill: #0E7490; " +
                    "-fx-background-radius: 12; -fx-padding: 3 9; -fx-font-size: 10px; -fx-font-weight: bold;");
            badges.getChildren().add(outBadge);
        }

        topRow.getChildren().addAll(avatar, nameBlock, badges);

        // ── Schedule row(s) ──
        VBox schedBlock = new VBox(3);
        schedBlock.setPadding(new Insets(8, 0, 0, 0));
        if (hasIn) {
            Label lblIn = new Label("↓ Ingress: " +
                    (pair.inScheduledTime != null ? pair.inScheduledTime.format(DT_DISPLAY) : "—"));
            lblIn.setStyle("-fx-font-size: 11px; -fx-text-fill: #065F46;");
            schedBlock.getChildren().add(lblIn);
        }
        if (hasOut) {
            Label lblOut = new Label("↑ Egress:   " +
                    (pair.outScheduledTime != null ? pair.outScheduledTime.format(DT_DISPLAY) : "—"));
            lblOut.setStyle("-fx-font-size: 11px; -fx-text-fill: #0E7490;");
            schedBlock.getChildren().add(lblOut);
        }

        Label lblDevices = new Label(pair.devices.size() + (pair.devices.size() == 1 ? " device" : " devices"));
        lblDevices.setStyle("-fx-font-size: 10px; -fx-text-fill: #94A3B8; -fx-padding: 4 0 0 0;");

        card.getChildren().addAll(topRow, schedBlock, lblDevices);

        String cardBaseStyle  = card.getStyle();
        String cardHoverStyle = cardBaseStyle.replace("-fx-background-color: #FFFFFF", "-fx-background-color: #FFFBF0");
        card.setOnMouseEntered(e -> card.setStyle(cardHoverStyle));
        card.setOnMouseExited(e  -> card.setStyle(cardBaseStyle));
        card.setOnMouseClicked(e -> openAmendVoidModal(pair));

        return card;
    }

    // ════════════════════════════════════════════════════════════════════
    // AMEND / VOID MODAL — combined ingress + egress
    //
    // Both schedules are shown in the "Amend Schedule" section.
    // Either or both may be changed.  Cross-validation rules:
    //   1. New ingress must be strictly in the future (> now).
    //   2. New egress must be strictly in the future (> now).
    //   3. Egress must be strictly AFTER ingress at all times.
    // Inline error label is shown beneath the egress picker when rule 3
    // is violated; submit is blocked until the constraint is satisfied.
    // ════════════════════════════════════════════════════════════════════

    private static final String FETCH_OPERATING_HOURS_SQL = """
            SELECT start_of_day_time, end_of_day_time FROM settings ORDER BY setting_id DESC LIMIT 1
            """;

    private LocalTime[] loadOperatingHours() {
        try (Connection conn = DBConnection.connect();
             PreparedStatement pst = conn.prepareStatement(FETCH_OPERATING_HOURS_SQL);
             ResultSet rs = pst.executeQuery()) {
            if (rs.next()) {
                return new LocalTime[]{
                        rs.getTime("start_of_day_time").toLocalTime(),
                        rs.getTime("end_of_day_time").toLocalTime()
                };
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return new LocalTime[]{LocalTime.of(8, 0), LocalTime.of(20, 0)};
    }

    private List<LocalTime> buildTimeSlots(LocalTime open, LocalTime close) {
        List<LocalTime> slots = new ArrayList<>();
        LocalTime t = open;
        while (t.isBefore(close)) {
            slots.add(t);
            LocalTime next = t.plusMinutes(30);
            if (!next.isAfter(t)) break;
            t = next;
        }
        return slots;
    }

    private List<LocalDate> buildSelectableDates() {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 21; i++) dates.add(today.plusDays(i));
        return dates;
    }

    private LocalTime roundToNearestSlot(LocalTime time, List<LocalTime> slots) {
        if (slots == null || slots.isEmpty() || time == null) return null;
        LocalTime closest = slots.get(0);
        long bestDiff = Math.abs(java.time.Duration.between(time, closest).toMinutes());
        for (LocalTime slot : slots) {
            long diff = Math.abs(java.time.Duration.between(time, slot).toMinutes());
            if (diff < bestDiff) { bestDiff = diff; closest = slot; }
        }
        return closest;
    }

    private void openAmendVoidModal(GateRequestPair pair) {
        paneAmendVoidModal.getChildren().clear();

        boolean hasIn  = pair.inRequestId > 0;
        boolean hasOut = pair.outRequestId != null;

        // ── Backdrop ──
        Region backdrop = new Region();
        backdrop.setStyle("-fx-background-color: rgba(15,23,42,0.55);");
        backdrop.setEffect(new javafx.scene.effect.GaussianBlur(8));

        StackPane overlay = new StackPane();
        overlay.setAlignment(Pos.CENTER);
        overlay.setOnMouseClicked(e -> closeAmendVoidModal());
        overlay.prefWidthProperty().bind(paneAmendVoidModal.widthProperty());
        overlay.prefHeightProperty().bind(paneAmendVoidModal.heightProperty());
        backdrop.prefWidthProperty().bind(overlay.widthProperty());
        backdrop.prefHeightProperty().bind(overlay.heightProperty());
        overlay.getChildren().add(backdrop);

        VBox modal = new VBox(0);
        modal.setMaxWidth(860);
        modal.setMaxHeight(Region.USE_PREF_SIZE);
        modal.setStyle(
                "-fx-background-color: #FFFFFF; -fx-background-radius: 18; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.30), 30, 0, 0, 8);"
        );
        modal.setOnMouseClicked(javafx.event.Event::consume);

        // ── Header ──
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16, 20, 14, 20));
        header.setStyle("-fx-background-color: #7A0000; -fx-background-radius: 18 18 0 0;");

        StackPane headerIcon = new StackPane();
        headerIcon.setMinSize(48, 36); headerIcon.setMaxSize(48, 36);
        headerIcon.setStyle("-fx-background-color: rgba(255,255,255,0.15); -fx-background-radius: 10;");
        javafx.scene.shape.SVGPath doorIcon = new javafx.scene.shape.SVGPath();
        doorIcon.setContent("M4 3h11v18H4z M9 3v18 M15 8l5-2v14l-5-2 M11 12v.01");
        doorIcon.setStyle("-fx-stroke: #FFFFFF; -fx-fill: transparent; -fx-stroke-width: 1.4; " +
                "-fx-stroke-line-cap: round; -fx-stroke-line-join: round;");
        doorIcon.setScaleX(0.95); doorIcon.setScaleY(0.95);
        headerIcon.getChildren().add(doorIcon);

        VBox headerText = new VBox(2);
        HBox.setHgrow(headerText, Priority.ALWAYS);
        // Title shows both request IDs if paired
        String titleIds = hasIn
                ? "Gate Request  #" + pair.inRequestId + (hasOut ? "  +  #" + pair.outRequestId : "")
                : "Gate Request  #" + pair.outRequestId;
        Label lblModalTitle = new Label(titleIds);
        lblModalTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #FFFFFF;");
        Label lblModalSub = new Label("Amend the ingress and/or egress schedule, or void the ingress request");
        lblModalSub.setStyle("-fx-font-size: 12px; -fx-text-fill: rgba(255,255,255,0.75);");
        headerText.getChildren().addAll(lblModalTitle, lblModalSub);

        Button btnClose = new Button("✕");
        btnClose.setStyle(
                "-fx-background-color: rgba(255,255,255,0.15); -fx-background-radius: 50; " +
                        "-fx-text-fill: #FFFFFF; -fx-font-size: 12px; -fx-cursor: hand; " +
                        "-fx-min-width: 30; -fx-min-height: 30; -fx-max-width: 30; -fx-max-height: 30;");
        btnClose.setOnAction(e -> closeAmendVoidModal());
        header.getChildren().addAll(headerIcon, headerText, btnClose);

        // ── Body ──
        HBox body = new HBox(24);
        body.setPadding(new Insets(16, 20, 16, 20));
        body.setAlignment(Pos.TOP_LEFT);

        VBox leftCol = new VBox(12);
        leftCol.setPrefWidth(330);
        HBox.setHgrow(leftCol, Priority.ALWAYS);

        VBox rightCol = new VBox(12);
        rightCol.setPrefWidth(380);
        HBox.setHgrow(rightCol, Priority.ALWAYS);

        Region colDivider = new Region();
        colDivider.setStyle("-fx-background-color: #E2E8F0;");
        colDivider.setMinWidth(1); colDivider.setMaxWidth(1);

        // ── Left: student ──
        leftCol.getChildren().add(sectionLabel("STUDENT INFORMATION"));
        HBox studentCard = new HBox(14);
        studentCard.setAlignment(Pos.CENTER_LEFT);
        studentCard.setStyle("-fx-background-color: #F8FAFC; -fx-background-radius: 12; " +
                "-fx-border-color: #E2E8F0; -fx-border-radius: 12; -fx-border-width: 1; -fx-padding: 12 14;");
        StackPane sAvatar = new StackPane();
        sAvatar.setMinSize(44, 44); sAvatar.setMaxSize(44, 44);
        sAvatar.setStyle("-fx-background-color: #7A0000; -fx-background-radius: 50;");
        Label sAvatarLbl = new Label(pair.studentName.substring(0, 1).toUpperCase());
        sAvatarLbl.setStyle("-fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-font-size: 18px;");
        sAvatar.getChildren().add(sAvatarLbl);
        VBox sInfo = new VBox(3);
        Label sName  = new Label(pair.studentName);
        sName.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #0F172A;");
        Label sCode  = new Label(pair.studentCode + "  ·  " + pair.studentSection);
        sCode.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");
        Label sEmail = new Label(pair.studentEmail);
        sEmail.setStyle("-fx-font-size: 11px; -fx-text-fill: #94A3B8;");
        sInfo.getChildren().addAll(sName, sCode, sEmail);
        studentCard.getChildren().addAll(sAvatar, sInfo);
        leftCol.getChildren().add(studentCard);

        // ── Left: current schedule chips (both IN + OUT) ──
        leftCol.getChildren().add(sectionLabel("CURRENT SCHEDULE"));
        if (hasIn) {
            leftCol.getChildren().add(scheduleChip("↓ Ingress",
                    pair.inScheduledTime != null ? pair.inScheduledTime.format(DT_DISPLAY) : "—",
                    false));
        }
        if (hasOut) {
            leftCol.getChildren().add(scheduleChip("↑ Egress",
                    pair.outScheduledTime != null ? pair.outScheduledTime.format(DT_DISPLAY) : "—",
                    true));
        }

        // ── Left: devices ──
        leftCol.getChildren().add(sectionLabel("DEVICES  (" + pair.devices.size() + ")"));
        for (DeviceEntry de : pair.devices) leftCol.getChildren().add(buildDeviceChip(de));

        // ── Right: ACTION toggle ──
        rightCol.getChildren().add(sectionLabel("ACTION"));
        ToggleGroup actionGroup = new ToggleGroup();
        ToggleButton btnActAmend = new ToggleButton("✎  Amend Schedule");
        styleActionToggle(btnActAmend);
        btnActAmend.setToggleGroup(actionGroup);
        btnActAmend.setSelected(true);

        ToggleButton btnActVoid = new ToggleButton("🚫  Void Request");
        styleActionToggle(btnActVoid);
        btnActVoid.setToggleGroup(actionGroup);
        // Void is only available when there IS a pending IN request
        btnActVoid.setDisable(!hasIn);
        if (!hasIn) btnActVoid.setTooltip(new Tooltip("No pending ingress request to void."));

        HBox actionRow = new HBox(10, btnActAmend, btnActVoid);
        actionRow.setAlignment(Pos.CENTER_LEFT);
        rightCol.getChildren().add(actionRow);

        // ── Right: Amend fields ──
        LocalTime[] operatingHours = loadOperatingHours();
        LocalTime openTime  = operatingHours[0];
        LocalTime closeTime = operatingHours[1];
        List<LocalTime> timeSlots  = buildTimeSlots(openTime, closeTime);
        List<LocalDate> dateChoices = buildSelectableDates();

        LocalDate minDate = dateChoices.isEmpty() ? LocalDate.now() : dateChoices.get(0);
        LocalDate maxDate = dateChoices.isEmpty() ? LocalDate.now() : dateChoices.get(dateChoices.size() - 1);

        DateTimeFormatter timeSlotFmt  = DateTimeFormatter.ofPattern("hh:mm a");
        DateTimeFormatter dateChoiceFmt = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy");

        VBox amendFields = new VBox(12);

        Label lblAmendHint = new Label("Amend either or both schedules. " +
                "Egress must always be strictly after ingress. Both must be in the future.");
        lblAmendHint.setWrapText(true);
        lblAmendHint.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");
        amendFields.getChildren().add(lblAmendHint);

        // ── Inline cross-validation error label (shown between the two pickers) ──
        Label lblTimeConflictErr = new Label();
        lblTimeConflictErr.setWrapText(true);
        lblTimeConflictErr.setStyle(
                "-fx-text-fill: #DC2626; -fx-font-size: 11px; -fx-font-weight: bold; " +
                        "-fx-background-color: #FEF2F2; -fx-padding: 6 10; -fx-background-radius: 6; " +
                        "-fx-border-color: #FECACA; -fx-border-radius: 6; -fx-border-width: 1;");
        lblTimeConflictErr.setVisible(false);
        lblTimeConflictErr.setManaged(false);

        // ── Helper: build one date+time picker block ──
        // Returns [DatePicker, ComboBox<LocalTime>] in an Object[]
        java.util.function.Function<LocalDateTime, Object[]> buildDtPicker = (currentDT) -> {
            HBox dtRow = new HBox(10);
            dtRow.setAlignment(Pos.TOP_LEFT);

            VBox dateBox = new VBox(4);
            HBox.setHgrow(dateBox, Priority.ALWAYS);
            Label lblDateLbl = new Label("Date");
            lblDateLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #374151;");
            DatePicker dp = new DatePicker();
            dp.setMaxWidth(Double.MAX_VALUE); dp.setPrefHeight(34);
            dp.setConverter(new javafx.util.StringConverter<>() {
                @Override public String toString(LocalDate d) { return d == null ? "" : d.format(dateChoiceFmt); }
                @Override public LocalDate fromString(String s) {
                    try { return LocalDate.parse(s, dateChoiceFmt); } catch (Exception ex) { return null; }
                }
            });
            dp.setDayCellFactory(picker -> new DateCell() {
                @Override public void updateItem(LocalDate item, boolean empty) {
                    super.updateItem(item, empty);
                    setDisable(empty || item.isBefore(minDate) || item.isAfter(maxDate));
                }
            });
            dp.setStyle("-fx-font-size: 12px; -fx-background-radius: 6; -fx-border-color: #D1D5DB; -fx-border-radius: 6;");
            LocalDate preDate = currentDT != null ? currentDT.toLocalDate() : minDate;
            if (preDate.isBefore(minDate)) preDate = minDate;
            if (preDate.isAfter(maxDate))  preDate = maxDate;
            dp.setValue(preDate);
            dateBox.getChildren().addAll(lblDateLbl, dp);

            VBox timeBox = new VBox(4);
            HBox.setHgrow(timeBox, Priority.ALWAYS);
            Label lblTimeLbl = new Label("Time");
            lblTimeLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #374151;");
            ComboBox<LocalTime> cmb = new ComboBox<>();
            cmb.getItems().addAll(timeSlots);
            cmb.setMaxWidth(Double.MAX_VALUE); cmb.setPrefHeight(34);
            cmb.setConverter(new javafx.util.StringConverter<>() {
                @Override public String toString(LocalTime t) { return t == null ? "" : t.format(timeSlotFmt); }
                @Override public LocalTime fromString(String s) { return null; }
            });
            cmb.setStyle("-fx-font-size: 12px; -fx-background-radius: 6; -fx-border-color: #D1D5DB; -fx-border-radius: 6;");
            LocalTime preTime = currentDT != null ? roundToNearestSlot(currentDT.toLocalTime(), timeSlots) : null;
            cmb.setValue(preTime != null ? preTime : (timeSlots.isEmpty() ? null : timeSlots.get(0)));
            timeBox.getChildren().addAll(lblTimeLbl, cmb);

            dtRow.getChildren().addAll(dateBox, timeBox);
            return new Object[]{dtRow, dp, cmb};
        };

        // ── Ingress picker (only if hasIn) ──
        DatePicker    dpIn  = null;
        ComboBox<LocalTime> cmbIn = null;
        if (hasIn) {
            Label lblInHeader = new Label("↓  NEW INGRESS SCHEDULE");
            lblInHeader.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #065F46;");
            amendFields.getChildren().add(lblInHeader);
            Object[] inParts = buildDtPicker.apply(pair.inScheduledTime);
            amendFields.getChildren().add((javafx.scene.Node) inParts[0]);
            dpIn  = (DatePicker) inParts[1];
            cmbIn = (ComboBox<LocalTime>) inParts[2];
        }

        // ── Inline error (between IN and OUT pickers) ──
        amendFields.getChildren().add(lblTimeConflictErr);

        // ── Egress picker (only if hasOut) ──
        DatePicker    dpOut  = null;
        ComboBox<LocalTime> cmbOut = null;
        if (hasOut) {
            Label lblOutHeader = new Label("↑  NEW EGRESS SCHEDULE");
            lblOutHeader.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #0E7490;");
            amendFields.getChildren().add(lblOutHeader);
            Object[] outParts = buildDtPicker.apply(pair.outScheduledTime);
            amendFields.getChildren().add((javafx.scene.Node) outParts[0]);
            dpOut  = (DatePicker) outParts[1];
            cmbOut = (ComboBox<LocalTime>) outParts[2];
        }

        Label lblHoursNote = new Label("Gate hours: " + openTime.format(timeSlotFmt)
                + " – " + closeTime.format(timeSlotFmt)
                + "  (closing time is not selectable)");
        lblHoursNote.setWrapText(true);
        lblHoursNote.setStyle("-fx-font-size: 10px; -fx-text-fill: #94A3B8;");
        amendFields.getChildren().add(lblHoursNote);

        rightCol.getChildren().add(amendFields);

        // ── Reason ──
        VBox reasonBox = new VBox(6);
        Label lblReasonTitle = new Label("Reason  (required)");
        lblReasonTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #374151;");
        TextArea txtReason = new TextArea();
        txtReason.setPromptText("State the reason for this amendment or void action...");
        txtReason.setPrefRowCount(3);
        txtReason.setMaxHeight(80);
        txtReason.setWrapText(true);
        txtReason.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 8; " +
                "-fx-border-color: #D1D5DB; -fx-border-radius: 8; -fx-border-width: 1; -fx-font-size: 12px;");
        Label lblReasonErr = new Label("A reason is required to amend or void this request.");
        lblReasonErr.setStyle("-fx-text-fill: #DC2626; -fx-font-size: 11px; -fx-font-weight: bold;");
        lblReasonErr.setVisible(false); lblReasonErr.setManaged(false);
        reasonBox.getChildren().addAll(lblReasonTitle, txtReason, lblReasonErr);
        rightCol.getChildren().add(reasonBox);

        body.getChildren().addAll(leftCol, colDivider, rightCol);

        // ── Footer ──
        Region footerDiv = new Region();
        footerDiv.setStyle("-fx-background-color: #F1F5F9;");
        footerDiv.setMinHeight(1); footerDiv.setMaxHeight(1);

        HBox footer = new HBox(12);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(12, 20, 16, 20));

        Button btnCancel = new Button("Cancel");
        btnCancel.setPrefHeight(40); btnCancel.setPrefWidth(110);
        btnCancel.setStyle("-fx-background-color: #FFFFFF; -fx-text-fill: #374151; " +
                "-fx-border-color: #D1D5DB; -fx-border-width: 1.5; -fx-border-radius: 10; " +
                "-fx-background-radius: 10; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 13px;");
        btnCancel.setOnAction(e -> closeAmendVoidModal());

        Button btnSubmit = new Button("Submit Amendment");
        btnSubmit.setPrefHeight(40); btnSubmit.setPrefWidth(180);
        String submitStyleAmend = "-fx-background-color: #7A0000; -fx-text-fill: #FFFFFF; " +
                "-fx-background-radius: 10; -fx-border-radius: 10; " +
                "-fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 13px;";
        String submitStyleVoid  = "-fx-background-color: #DC2626; -fx-text-fill: #FFFFFF; " +
                "-fx-background-radius: 10; -fx-border-radius: 10; " +
                "-fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 13px;";
        btnSubmit.setStyle(submitStyleAmend);

        // ── Capture final references for use in lambdas ──
        final DatePicker    dpInFinal  = dpIn;
        final ComboBox<LocalTime> cmbInFinal  = cmbIn;
        final DatePicker    dpOutFinal = dpOut;
        final ComboBox<LocalTime> cmbOutFinal = cmbOut;

        /**
         * validateSchedules() — returns null if OK, or an error string if:
         *   a) Either new datetime is in the past / now.
         *   b) Egress <= ingress.
         * Only relevant in AMEND mode when both pickers are present.
         */
        java.util.function.Supplier<String> validateSchedules = () -> {
            LocalDateTime now = LocalDateTime.now();

            LocalDateTime newIn  = null;
            LocalDateTime newOut = null;

            if (dpInFinal != null && cmbInFinal != null) {
                LocalDate d = dpInFinal.getValue();
                LocalTime t = cmbInFinal.getValue();
                if (d != null && t != null) newIn = LocalDateTime.of(d, t);
            }
            if (dpOutFinal != null && cmbOutFinal != null) {
                LocalDate d = dpOutFinal.getValue();
                LocalTime t = cmbOutFinal.getValue();
                if (d != null && t != null) newOut = LocalDateTime.of(d, t);
            }

            // Rule 1: new ingress must be in the future
            if (newIn != null && !newIn.isAfter(now)) {
                return "Ingress schedule must be a future date and time (after right now).";
            }
            // Rule 2: new egress must be in the future
            if (newOut != null && !newOut.isAfter(now)) {
                return "Egress schedule must be a future date and time (after right now).";
            }
            // Rule 3: egress must be strictly AFTER ingress
            // Compare against the EFFECTIVE ingress: the newly-chosen one if set,
            // otherwise the original (unchanged) ingress schedule.
            if (newOut != null) {
                LocalDateTime effectiveIn = newIn != null ? newIn
                        : (pair.inScheduledTime != null ? pair.inScheduledTime : null);
                if (effectiveIn != null && !newOut.isAfter(effectiveIn)) {
                    return "Egress must be scheduled strictly after the ingress. "
                            + "Current ingress: "
                            + effectiveIn.format(DT_DISPLAY) + ".";
                }
            }
            // Rule 3 mirror: if only ingress changed, check it stays before existing egress
            if (newIn != null && newOut == null && pair.outScheduledTime != null) {
                if (!pair.outScheduledTime.isAfter(newIn)) {
                    return "The new ingress time (" + newIn.format(DT_DISPLAY) + ") "
                            + "must be before the existing egress schedule ("
                            + pair.outScheduledTime.format(DT_DISPLAY) + "). "
                            + "Please also amend the egress or pick an earlier ingress time.";
                }
            }
            return null; // all good
        };

        Runnable refreshActionState = () -> {
            Toggle selected = actionGroup.getSelectedToggle();
            if (selected == null) {
                amendFields.setVisible(false); amendFields.setManaged(false);
                btnSubmit.setVisible(false);   btnSubmit.setManaged(false);
                lblTimeConflictErr.setVisible(false); lblTimeConflictErr.setManaged(false);
                return;
            }
            btnSubmit.setVisible(true); btnSubmit.setManaged(true);

            if (selected == btnActVoid) {
                amendFields.setVisible(false); amendFields.setManaged(false);
                lblTimeConflictErr.setVisible(false); lblTimeConflictErr.setManaged(false);
                btnSubmit.setText("Void Request");
                btnSubmit.setStyle(submitStyleVoid);
                btnSubmit.setDisable(false);
            } else {
                amendFields.setVisible(true); amendFields.setManaged(true);
                btnSubmit.setText("Submit Amendment");
                btnSubmit.setStyle(submitStyleAmend);

                // Run cross-validation and show/hide inline error
                String err = validateSchedules.get();
                if (err != null) {
                    lblTimeConflictErr.setText("⚠  " + err);
                    lblTimeConflictErr.setVisible(true); lblTimeConflictErr.setManaged(true);
                    btnSubmit.setDisable(true);
                } else {
                    lblTimeConflictErr.setVisible(false); lblTimeConflictErr.setManaged(false);
                    // Also disable if required pickers have no value
                    boolean ready = true;
                    if (dpInFinal  != null && dpInFinal.getValue()  == null) ready = false;
                    if (cmbInFinal != null && cmbInFinal.getValue() == null) ready = false;
                    if (dpOutFinal != null && dpOutFinal.getValue() == null) ready = false;
                    if (cmbOutFinal != null && cmbOutFinal.getValue() == null) ready = false;
                    btnSubmit.setDisable(!ready);
                }
            }
        };

        actionGroup.selectedToggleProperty().addListener((obs, was, isNow) -> refreshActionState.run());
        if (dpInFinal  != null) dpInFinal.valueProperty().addListener((obs, was, isNow) -> refreshActionState.run());
        if (cmbInFinal != null) cmbInFinal.valueProperty().addListener((obs, was, isNow) -> refreshActionState.run());
        if (dpOutFinal != null) dpOutFinal.valueProperty().addListener((obs, was, isNow) -> refreshActionState.run());
        if (cmbOutFinal != null) cmbOutFinal.valueProperty().addListener((obs, was, isNow) -> refreshActionState.run());
        refreshActionState.run();

        btnSubmit.setOnAction(e -> {
            String reason = txtReason.getText() == null ? "" : txtReason.getText().trim();
            if (reason.isEmpty()) {
                lblReasonErr.setVisible(true); lblReasonErr.setManaged(true);
                txtReason.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 8; " +
                        "-fx-border-color: #DC2626; -fx-border-radius: 8; -fx-border-width: 1.5; -fx-font-size: 12px;");
                return;
            }

            if (btnActVoid.isSelected()) {
                performVoid(pair, reason);
            } else {
                // Final guard — re-run validation at submit time
                String errMsg = validateSchedules.get();
                if (errMsg != null) {
                    lblTimeConflictErr.setText("⚠  " + errMsg);
                    lblTimeConflictErr.setVisible(true); lblTimeConflictErr.setManaged(true);
                    btnSubmit.setDisable(true);
                    return;
                }

                LocalDateTime newIn  = null;
                LocalDateTime newOut = null;
                if (dpInFinal != null && cmbInFinal != null
                        && dpInFinal.getValue() != null && cmbInFinal.getValue() != null) {
                    newIn = LocalDateTime.of(dpInFinal.getValue(), cmbInFinal.getValue());
                }
                if (dpOutFinal != null && cmbOutFinal != null
                        && dpOutFinal.getValue() != null && cmbOutFinal.getValue() != null) {
                    newOut = LocalDateTime.of(dpOutFinal.getValue(), cmbOutFinal.getValue());
                }
                performAmend(pair, newIn, newOut, reason);
            }
        });

        footer.getChildren().addAll(btnCancel, btnSubmit);
        modal.getChildren().addAll(header, body, footerDiv, footer);
        overlay.getChildren().add(modal);
        paneAmendVoidModal.getChildren().add(overlay);

        paneAmendVoidModal.setOpacity(0);
        paneAmendVoidModal.setVisible(true);
        paneAmendVoidModal.setManaged(true);
        FadeTransition ft = new FadeTransition(Duration.millis(180), paneAmendVoidModal);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
    }

    private void styleActionToggle(ToggleButton btn) {
        btn.setPrefHeight(38);
        btn.setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #64748B; " +
                "-fx-background-radius: 8; -fx-cursor: hand; -fx-font-size: 12px; -fx-font-weight: bold; " +
                "-fx-padding: 0 16;");
        btn.selectedProperty().addListener((obs, was, isNow) -> {
            if (isNow) {
                btn.setStyle("-fx-background-color: #7A0000; -fx-text-fill: #FFFFFF; " +
                        "-fx-background-radius: 8; -fx-cursor: hand; -fx-font-size: 12px; -fx-font-weight: bold; " +
                        "-fx-padding: 0 16;");
            } else {
                btn.setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #64748B; " +
                        "-fx-background-radius: 8; -fx-cursor: hand; -fx-font-size: 12px; -fx-font-weight: bold; " +
                        "-fx-padding: 0 16;");
            }
        });
    }

    private void closeAmendVoidModal() {
        FadeTransition ft = new FadeTransition(Duration.millis(150), paneAmendVoidModal);
        ft.setFromValue(1); ft.setToValue(0);
        ft.setOnFinished(e -> {
            paneAmendVoidModal.setVisible(false);
            paneAmendVoidModal.setManaged(false);
            paneAmendVoidModal.getChildren().clear();
        });
        ft.play();
    }

    private Label sectionLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #C9A84C; -fx-letter-spacing: 1px;");
        return lbl;
    }

    /** Two-tone schedule chip: green for ingress, cyan for egress. */
    private HBox scheduleChip(String label, String time, boolean isEgress) {
        HBox chip = new HBox(8);
        chip.setAlignment(Pos.CENTER_LEFT);
        String bg     = isEgress ? "#EFF6FF" : "#F0FDF4";
        String border = isEgress ? "#BFDBFE" : "#86EFAC";
        String lColor = isEgress ? "#1D4ED8" : "#15803D";
        chip.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 10; " +
                "-fx-border-color: " + border + "; -fx-border-radius: 10; -fx-border-width: 1; -fx-padding: 10 14;");
        HBox.setHgrow(chip, Priority.ALWAYS);
        VBox chipText = new VBox(2);
        Label chipLabel = new Label(label);
        chipLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: " + lColor + ";");
        Label chipTime  = new Label(time);
        chipTime.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #0F172A;");
        chipText.getChildren().addAll(chipLabel, chipTime);
        chip.getChildren().add(chipText);
        return chip;
    }

    private HBox buildDeviceChip(DeviceEntry de) {
        HBox chip = new HBox(12);
        chip.setAlignment(Pos.CENTER_LEFT);
        VBox.setMargin(chip, new Insets(0, 0, 6, 0));
        chip.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 10; " +
                "-fx-border-color: #C9A84C; -fx-border-radius: 10; -fx-border-width: 1; -fx-padding: 10 12;");
        StackPane iconBox = new StackPane();
        iconBox.setMinSize(36, 36); iconBox.setMaxSize(36, 36);
        iconBox.setStyle("-fx-background-color: #FDF6E3; -fx-background-radius: 8;");
        Label icon = new Label("💻");
        icon.setStyle("-fx-font-size: 14px;");
        iconBox.getChildren().add(icon);
        VBox info = new VBox(2);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label devName   = new Label(de.brand + " " + de.model);
        devName.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #1E293B;");
        Label devSerial = new Label("S/N: " + de.serial + "  ·  " + de.deviceType);
        devSerial.setStyle("-fx-font-size: 11px; -fx-text-fill: #94A3B8;");
        info.getChildren().addAll(devName, devSerial);
        chip.getChildren().addAll(iconBox, info);
        return chip;
    }

    // ════════════════════════════════════════════════════════════════════
    // AMEND — updates scheduled_time for whichever request(s) changed.
    // Both IN and OUT are updated in a single transaction.
    // Server-side guard re-checks:
    //   • Both requests are still PENDING.
    //   • Egress > ingress (using effective times).
    //   • Both new times are in the future.
    // Every successful change is ALSO written to log_amendments, which
    // keeps a full history of every edit made to a gate_requests row
    // (separate from audit_log, which is the system-wide action trail).
    // ════════════════════════════════════════════════════════════════════

    private static final String FETCH_REQUEST_FOR_AMEND_SQL =
            "SELECT status, scheduled_time FROM gate_requests WHERE request_id = ?";
    private static final String UPDATE_SCHEDULE_SQL =
            "UPDATE gate_requests SET scheduled_time = ? WHERE request_id = ?";

    private static final String INSERT_LOG_AMENDMENT_SQL = """
            INSERT INTO log_amendments
                (request_id, direction, amendment_type, changed_by, reason, previous_data, new_data)
            VALUES (?, ?, ?, ?, ?, ?::jsonb, ?::jsonb)
            """;

    /**
     * Writes one row to log_amendments — the full history table for every
     * edit or void made to a gate_requests row before it's resolved at the
     * gate. Called inside the same transaction as the gate_requests update,
     * so a failure here rolls back the schedule change too.
     */
    private void insertLogAmendment(Connection conn, int requestId, String direction,
                                    String amendmentType, int changedBy, String reason,
                                    String previousDataJson, String newDataJson) throws SQLException {
        try (PreparedStatement pst = conn.prepareStatement(INSERT_LOG_AMENDMENT_SQL)) {
            pst.setInt(1, requestId);
            pst.setString(2, direction);
            pst.setString(3, amendmentType);
            pst.setInt(4, changedBy);
            pst.setString(5, reason);
            pst.setString(6, previousDataJson);
            pst.setString(7, newDataJson);
            pst.executeUpdate();
        }
    }

    private void performAmend(GateRequestPair pair,
                              LocalDateTime newIn,
                              LocalDateTime newOut,
                              String reason) {
        int officerId = UserSession.getInstance().getUserId();
        LocalDateTime now = LocalDateTime.now();

        // UI already validated, but re-check at write time for safety.
        if (newIn  != null && !newIn.isAfter(now)) {
            AlertHelper.showNegative(paneAmendVoidModal.getScene().getWindow(),
                    "Invalid Schedule", "The new ingress time must be in the future.");
            return;
        }
        if (newOut != null && !newOut.isAfter(now)) {
            AlertHelper.showNegative(paneAmendVoidModal.getScene().getWindow(),
                    "Invalid Schedule", "The new egress time must be in the future.");
            return;
        }
        // Determine effective times for cross-check
        LocalDateTime effectiveIn  = newIn  != null ? newIn  : pair.inScheduledTime;
        LocalDateTime effectiveOut = newOut != null ? newOut : pair.outScheduledTime;
        if (effectiveIn != null && effectiveOut != null && !effectiveOut.isAfter(effectiveIn)) {
            AlertHelper.showNegative(paneAmendVoidModal.getScene().getWindow(),
                    "Invalid Schedule",
                    "Egress (" + effectiveOut.format(DT_DISPLAY) + ") must be strictly after "
                            + "ingress (" + effectiveIn.format(DT_DISPLAY) + ").");
            return;
        }

        try (Connection conn = DBConnection.connect()) {
            conn.setAutoCommit(false);

            // ── Amend IN ──
            if (newIn != null && pair.inRequestId > 0) {
                String status = fetchRequestStatus(conn, pair.inRequestId);
                if (status == null) {
                    conn.rollback();
                    AlertHelper.showNegative(paneAmendVoidModal.getScene().getWindow(),
                            "Not Found", "Ingress request #" + pair.inRequestId + " no longer exists.");
                    return;
                }
                if (!"PENDING".equals(status)) {
                    conn.rollback();
                    AlertHelper.showNegative(paneAmendVoidModal.getScene().getWindow(),
                            "Cannot Amend",
                            "Ingress request #" + pair.inRequestId
                                    + " is no longer pending (" + status.toLowerCase() + ").");
                    closeAmendVoidModal(); loadGateRequests(); return;
                }
                try (PreparedStatement pst = conn.prepareStatement(UPDATE_SCHEDULE_SQL)) {
                    pst.setTimestamp(1, Timestamp.valueOf(newIn));
                    pst.setInt(2, pair.inRequestId);
                    pst.executeUpdate();
                }
                AuditLogger.log(conn, officerId, "CORRECTION_MADE", "gate_requests", pair.inRequestId,
                        "{\"request_id\":" + pair.inRequestId +
                                ",\"direction\":\"IN\"" +
                                ",\"student\":\"" + pair.studentName.replace("\"", "'") + "\"" +
                                ",\"previous_scheduled_time\":\"" + pair.inScheduledTime + "\"" +
                                ",\"new_scheduled_time\":\"" + newIn + "\"" +
                                ",\"reason\":\"" + reason.replace("\"", "'") + "\"}");

                insertLogAmendment(conn, pair.inRequestId, "IN", "EDIT", officerId, reason,
                        "{\"scheduled_time\":\"" + pair.inScheduledTime + "\"}",
                        "{\"scheduled_time\":\"" + newIn + "\"}");
            }

            // ── Amend OUT ──
            if (newOut != null && pair.outRequestId != null) {
                String status = fetchRequestStatus(conn, pair.outRequestId);
                if (status == null) {
                    conn.rollback();
                    AlertHelper.showNegative(paneAmendVoidModal.getScene().getWindow(),
                            "Not Found", "Egress request #" + pair.outRequestId + " no longer exists.");
                    return;
                }
                if (!"PENDING".equals(status)) {
                    conn.rollback();
                    AlertHelper.showNegative(paneAmendVoidModal.getScene().getWindow(),
                            "Cannot Amend",
                            "Egress request #" + pair.outRequestId
                                    + " is no longer pending (" + status.toLowerCase() + ").");
                    closeAmendVoidModal(); loadGateRequests(); return;
                }
                try (PreparedStatement pst = conn.prepareStatement(UPDATE_SCHEDULE_SQL)) {
                    pst.setTimestamp(1, Timestamp.valueOf(newOut));
                    pst.setInt(2, pair.outRequestId);
                    pst.executeUpdate();
                }
                AuditLogger.log(conn, officerId, "CORRECTION_MADE", "gate_requests", pair.outRequestId,
                        "{\"request_id\":" + pair.outRequestId +
                                ",\"direction\":\"OUT\"" +
                                ",\"student\":\"" + pair.studentName.replace("\"", "'") + "\"" +
                                ",\"previous_scheduled_time\":\"" + pair.outScheduledTime + "\"" +
                                ",\"new_scheduled_time\":\"" + newOut + "\"" +
                                ",\"reason\":\"" + reason.replace("\"", "'") + "\"}");

                insertLogAmendment(conn, pair.outRequestId, "OUT", "EDIT", officerId, reason,
                        "{\"scheduled_time\":\"" + pair.outScheduledTime + "\"}",
                        "{\"scheduled_time\":\"" + newOut + "\"}");
            }

            conn.commit();

            // Email the student with updated schedule(s)
            List<EmailService.DeviceEntry> emailDevices = new ArrayList<>();
            for (DeviceEntry de : pair.devices) {
                emailDevices.add(new EmailService.DeviceEntry(de.brand, de.model, de.serial, de.deviceType));
            }
            // Send for whichever direction(s) were amended.
            // NOTE: amending only changes the scheduled time — it does NOT approve
            // the request. sendAmendmentNoticeEmail correctly reflects PENDING status
            // and sends a plain HTML notice (no PDF attachment), unlike sendGatePassEmail
            // which is reserved for actual gate approvals.
            if (newIn != null && pair.inRequestId > 0) {
                EmailService.sendAmendmentNoticeEmail(
                        pair.studentEmail, pair.studentName, pair.studentCode,
                        pair.studentSection, "IN",
                        pair.inScheduledTime, newIn, reason, emailDevices);
            }
            if (newOut != null && pair.outRequestId != null) {
                EmailService.sendAmendmentNoticeEmail(
                        pair.studentEmail, pair.studentName, pair.studentCode,
                        pair.studentSection, "OUT",
                        pair.outScheduledTime, newOut, reason, emailDevices);
            }

            String amended = (newIn != null && newOut != null) ? "Ingress and egress schedules were"
                    : (newIn != null ? "Ingress schedule was" : "Egress schedule was");
            AlertHelper.showPositive(paneAmendVoidModal.getScene().getWindow(),
                    "Amendment Saved",
                    amended + " updated. The student has been notified — the request remains pending until approved at the gate.");

            closeAmendVoidModal();
            loadGateRequests();
            loadLogs();
            loadStats();

        } catch (SQLException ex) {
            ex.printStackTrace();
            AlertHelper.showNegative(paneAmendVoidModal.getScene().getWindow(),
                    "Error", "Amendment failed. No changes were saved.");
        }
    }

    private String fetchRequestStatus(Connection conn, int requestId) throws SQLException {
        try (PreparedStatement pst = conn.prepareStatement(FETCH_REQUEST_FOR_AMEND_SQL)) {
            pst.setInt(1, requestId);
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next() ? rs.getString("status") : null;
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // VOID — only ever applies to the PENDING INGRESS of a pair.
    // Voiding it cascade-rejects the paired pending egress too.
    // Egress cannot be voided under any circumstances.
    // Both the voided ingress and any cascaded egress are also recorded
    // in log_amendments (amendment_type = 'VOID').
    // ════════════════════════════════════════════════════════════════════

    private static final String VOID_REQUEST_SQL = """
            UPDATE gate_requests
            SET status = 'REJECTED', approved_by = ?, rejection_reason = ?, resolved_at = NOW()
            WHERE request_id = ?
            """;

    private static final String CASCADE_VOID_EGRESS_SQL = """
            UPDATE gate_requests gr_out
            SET status = 'REJECTED', approved_by = ?,
                rejection_reason = 'Paired ingress request was voided.',
                resolved_at = NOW()
            WHERE gr_out.direction = 'OUT'
              AND gr_out.status = 'PENDING'
              AND gr_out.student_id = ?
              AND NOT EXISTS (
                SELECT 1 FROM gate_request_devices grd_out
                WHERE grd_out.request_id = gr_out.request_id
                  AND grd_out.device_id NOT IN (
                    SELECT device_id FROM gate_request_devices WHERE request_id = ?
                  )
              ) RETURNING request_id
            """;

    private void performVoid(GateRequestPair pair, String reason) {
        if (pair.inRequestId <= 0) {
            AlertHelper.showNegative(paneAmendVoidModal.getScene().getWindow(),
                    "Cannot Void", "There is no pending ingress request to void. " +
                            "Egress requests cannot be voided — amend the schedule instead.");
            return;
        }

        int officerId = UserSession.getInstance().getUserId();

        try (Connection conn = DBConnection.connect()) {
            conn.setAutoCommit(false);

            String currentStatus = fetchRequestStatus(conn, pair.inRequestId);
            if (currentStatus == null) {
                conn.rollback();
                AlertHelper.showNegative(paneAmendVoidModal.getScene().getWindow(),
                        "Not Found", "Gate request #" + pair.inRequestId + " no longer exists.");
                return;
            }
            if (!"PENDING".equals(currentStatus)) {
                conn.rollback();
                AlertHelper.showNegative(paneAmendVoidModal.getScene().getWindow(),
                        "Cannot Void",
                        "Only pending requests can be voided. This request has already been "
                                + currentStatus.toLowerCase() + " and cannot be voided.");
                closeAmendVoidModal(); loadGateRequests(); return;
            }

            try (PreparedStatement pst = conn.prepareStatement(VOID_REQUEST_SQL)) {
                pst.setInt(1, officerId);
                pst.setString(2, "VOIDED: " + reason);
                pst.setInt(3, pair.inRequestId);
                pst.executeUpdate();
            }

            AuditLogger.log(conn, officerId, "VOID_MADE", "gate_requests", pair.inRequestId,
                    "{\"request_id\":" + pair.inRequestId +
                            ",\"direction\":\"IN\"" +
                            ",\"student\":\"" + pair.studentName.replace("\"", "'") + "\"" +
                            ",\"reason\":\"" + reason.replace("\"", "'") + "\"}");

            insertLogAmendment(conn, pair.inRequestId, "IN", "VOID", officerId, reason,
                    "{\"scheduled_time\":\"" + pair.inScheduledTime + "\",\"status\":\"PENDING\"}",
                    "{\"status\":\"REJECTED\"}");

            // Cascade-reject any paired pending egress
            try (PreparedStatement pst = conn.prepareStatement(CASCADE_VOID_EGRESS_SQL)) {
                pst.setInt(1, officerId);
                pst.setInt(2, pair.studentId);
                pst.setInt(3, pair.inRequestId);
                try (ResultSet rs = pst.executeQuery()) {
                    while (rs.next()) {
                        int cascadedId = rs.getInt("request_id");
                        AuditLogger.log(conn, officerId, "GATE_REQUEST_REJECTED",
                                "gate_requests", cascadedId,
                                "{\"request_id\":" + cascadedId +
                                        ",\"direction\":\"OUT\"" +
                                        ",\"student\":\"" + pair.studentName.replace("\"", "'") + "\"" +
                                        ",\"reason\":\"Paired ingress request #" + pair.inRequestId + " was voided.\"}");

                        insertLogAmendment(conn, cascadedId, "OUT", "VOID", officerId,
                                "Paired ingress request #" + pair.inRequestId + " was voided.",
                                "{\"scheduled_time\":\"" + pair.outScheduledTime + "\",\"status\":\"PENDING\"}",
                                "{\"status\":\"REJECTED\"}");
                    }
                }
            }

            conn.commit();

            List<EmailService.DeviceEntry> emailDevices = new ArrayList<>();
            for (DeviceEntry de : pair.devices) {
                emailDevices.add(new EmailService.DeviceEntry(de.brand, de.model, de.serial, de.deviceType));
            }
            EmailService.sendVoidNoticeEmail(
                    pair.studentEmail, pair.studentName, pair.studentCode, pair.studentSection,
                    "IN", pair.inScheduledTime, reason, emailDevices);

            AlertHelper.showPositive(paneAmendVoidModal.getScene().getWindow(),
                    "Request Voided",
                    "Ingress Request #" + pair.inRequestId + " has been voided" +
                            (pair.outRequestId != null
                                    ? " and the paired egress request #" + pair.outRequestId + " was automatically rejected"
                                    : "") +
                            ". The student has been notified.");

            closeAmendVoidModal();
            loadGateRequests();
            loadLogs();
            loadStats();

        } catch (SQLException ex) {
            ex.printStackTrace();
            AlertHelper.showNegative(paneAmendVoidModal.getScene().getWindow(),
                    "Error", "Void action failed. No changes were saved.");
        }
    }
}