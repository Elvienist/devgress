package com.example.byodsystem.byod.controller;

import com.example.byodsystem.byod.database.DBConnection;
import com.example.byodsystem.byod.service.AuditLogger;
import com.example.byodsystem.byod.service.UserSession;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class OfficerGateRequestController {

    @FXML private VBox paneMainContent;
    @FXML private VBox paneClosed;
    @FXML private Label lblClosedHours;

    @FXML private Label lblDate;
    @FXML private Label lblPendingBadge;

    @FXML private Button btnTabPending;
    @FXML private Button btnTabRejected;
    @FXML private Button btnTabApproved;
    @FXML private Button btnTabAll;
    @FXML private HBox paneFilterTabs;
    private Button btnTabIn;
    private Button btnTabOut;
    @FXML private HBox paneOverdueRow;
    private Button btnTabOverdue;
    @FXML private HBox paneLateEgressRow;
    private Button btnTabLateEgress;

    @FXML private TextField txtSearch;

    @FXML private ScrollPane scrollRequests;
    @FXML private VBox paneRequestList;
    @FXML private VBox paneEmpty;
    @FXML private Label lblEmptyTitle;
    @FXML private Label lblEmptySub;

    @FXML private StackPane paneDetailModal;

    @FXML private StackPane paneConfirmModal;

    @FXML private Label lblLiveClock;
    @FXML private Label lblLiveDate;

    @FXML private Label lblStatIn;
    @FXML private Label lblStatOut;
    @FXML private Label lblStatPending;
    @FXML private Label lblStatRejected;

    private Timeline clockTimeline;

    private static final DateTimeFormatter DATE_HEADER  = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");
    private static final DateTimeFormatter DT_DISPLAY   = DateTimeFormatter.ofPattern("MMM d, yyyy  hh:mm a");
    private static final DateTimeFormatter TIME_ONLY    = DateTimeFormatter.ofPattern("hh:mm a");
    private static final DateTimeFormatter CLOCK_FMT    = DateTimeFormatter.ofPattern("hh:mm:ss a");
    private static final DateTimeFormatter CLOCK_DATE   = DateTimeFormatter.ofPattern("EEEE, MMMM d");

    private String activeFilter = "PENDING";

    private static final String OVERDUE_REASON = "Overdue: student did not arrive at the scheduled time.";

    private static class GateRequestRow {
        int    requestId;
        int    studentId;
        String studentName;
        String studentCode;
        String studentSection;
        String studentEmail;
        String requestedByName;
        String direction;
        LocalDateTime scheduledTimeIn;
        LocalDateTime scheduledTimeOut;
        String status;
        boolean isLate;
        List<DeviceEntry> devices = new ArrayList<>();
    }

    private static class DeviceEntry {
        int    deviceId;
        String brand, model, serial, deviceType, currentLocation;
    }

    private GateRequestRow selectedRequest;
    private String pendingAction;

    @FXML
    public void initialize() {
        lblDate.setText(LocalDate.now().format(DATE_HEADER));

        if (!isModuleOpen()) {
            showClosedState();
            return;
        }
        showMainContent();
        setupFilterTabs();
        startLiveClock();
        autoRejectOverdue();
        loadRequests();
        loadStats();
    }

    private boolean isModuleOpen() {
        LocalTime[] hours = loadOperatingHours();
        if (hours == null) return true;
        LocalTime now = LocalTime.now();
        return !now.isBefore(hours[0]) && !now.isAfter(hours[1]);
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
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void showClosedState() {
        if (paneMainContent != null) { paneMainContent.setVisible(false); paneMainContent.setManaged(false); }
        if (paneClosed != null) {
            LocalTime[] hours = loadOperatingHours();
            if (hours != null && lblClosedHours != null) {
                lblClosedHours.setText("Gate requests can only be reviewed between "
                        + hours[0].format(TIME_ONLY) + " and " + hours[1].format(TIME_ONLY) + ".");
            }
            paneClosed.setVisible(true);
            paneClosed.setManaged(true);
        }
    }

    private void showMainContent() {
        if (paneMainContent != null) { paneMainContent.setVisible(true); paneMainContent.setManaged(true); }
        if (paneClosed != null) { paneClosed.setVisible(false); paneClosed.setManaged(false); }
    }

    private int clockTickCount = 0;

    private void startLiveClock() {
        tickClock();
        clockTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            tickClock();
            clockTickCount++;
            if (clockTickCount % 60 == 0) {
                autoRejectOverdue();
                loadRequests();
            }
        }));
        clockTimeline.setCycleCount(Timeline.INDEFINITE);
        clockTimeline.play();
    }

    private void tickClock() {
        LocalDateTime now = LocalDateTime.now();
        if (lblLiveClock != null) lblLiveClock.setText(now.format(CLOCK_FMT));
        if (lblLiveDate  != null) lblLiveDate.setText(now.format(CLOCK_DATE));
    }

    private void loadStats() {
        String sql = """
                SELECT
                  COUNT(*) FILTER (WHERE direction = 'IN'  AND status = 'APPROVED'
                                   AND DATE(resolved_at AT TIME ZONE 'Asia/Manila') = CURRENT_DATE) AS in_count,
                  COUNT(*) FILTER (WHERE direction = 'OUT' AND status = 'APPROVED'
                                   AND DATE(resolved_at AT TIME ZONE 'Asia/Manila') = CURRENT_DATE) AS out_count,
                  COUNT(*) FILTER (WHERE status = 'PENDING') AS pending_count,
                  COUNT(*) FILTER (WHERE status = 'REJECTED'
                                   AND DATE(resolved_at AT TIME ZONE 'Asia/Manila') = CURRENT_DATE) AS rejected_count
                FROM gate_requests
                """;

        try (Connection conn = DBConnection.connect();
             PreparedStatement pst = conn.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {

            if (rs.next()) {
                int inCount       = rs.getInt("in_count");
                int outCount      = rs.getInt("out_count");
                int pendingCount  = rs.getInt("pending_count");
                int rejectedCount = rs.getInt("rejected_count");

                Platform.runLater(() -> {
                    if (lblStatIn      != null) lblStatIn.setText(inCount       + " approved");
                    if (lblStatOut     != null) lblStatOut.setText(outCount      + " approved");
                    if (lblStatPending != null) lblStatPending.setText(pendingCount  + " waiting");
                    if (lblStatRejected!= null) lblStatRejected.setText(rejectedCount + " today");
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void setupFilterTabs() {
        btnTabIn  = new Button("↓  Ingress (IN)");
        btnTabOut = new Button("↑  Egress  (OUT)");
        if (paneFilterTabs != null) {
            paneFilterTabs.getChildren().addAll(btnTabIn, btnTabOut);
        }

        btnTabOverdue = new Button("⏰  Overdue");
        if (paneOverdueRow != null) {
            paneOverdueRow.getChildren().add(btnTabOverdue);
        }

        btnTabLateEgress = new Button("⚠️  Late Egress");
        if (paneLateEgressRow != null) {
            paneLateEgressRow.getChildren().add(btnTabLateEgress);
        }

        applyTabStyle(btnTabPending,  true);
        applyTabStyle(btnTabRejected, false);
        applyTabStyle(btnTabApproved, false);
        applyTabStyle(btnTabAll,      false);
        applyTabStyle(btnTabIn,       false);
        applyTabStyle(btnTabOut,      false);
        applyTabStyle(btnTabOverdue,  false);
        applyTabStyle(btnTabLateEgress, false);

        btnTabPending.setOnAction(e  -> switchFilter("PENDING"));
        btnTabRejected.setOnAction(e -> switchFilter("REJECTED"));
        btnTabApproved.setOnAction(e -> switchFilter("APPROVED"));
        btnTabAll.setOnAction(e      -> switchFilter("ALL"));
        btnTabIn.setOnAction(e       -> switchFilter("IN"));
        btnTabOut.setOnAction(e      -> switchFilter("OUT"));
        btnTabOverdue.setOnAction(e  -> switchFilter("OVERDUE"));
        btnTabLateEgress.setOnAction(e -> switchFilter("LATE_EGRESS"));

        setOverdueRowVisible(false);
        setLateEgressRowVisible(false);

        if (txtSearch != null) {
            txtSearch.textProperty().addListener((obs, oldVal, newVal) -> loadRequests());
        }
    }

    private void setOverdueRowVisible(boolean visible) {
        if (paneOverdueRow == null) return;
        paneOverdueRow.setVisible(visible);
        paneOverdueRow.setManaged(visible);
    }

    private void setLateEgressRowVisible(boolean visible) {
        if (paneLateEgressRow == null) return;
        paneLateEgressRow.setVisible(visible);
        paneLateEgressRow.setManaged(visible);
    }

    private void switchFilter(String filter) {
        activeFilter = filter;
        applyTabStyle(btnTabPending,    "PENDING".equals(filter));
        applyTabStyle(btnTabRejected,   "REJECTED".equals(filter));
        applyTabStyle(btnTabApproved,   "APPROVED".equals(filter));
        applyTabStyle(btnTabAll,        "ALL".equals(filter));
        applyTabStyle(btnTabIn,         "IN".equals(filter));
        applyTabStyle(btnTabOut,        "OUT".equals(filter));
        applyTabStyle(btnTabOverdue,    "OVERDUE".equals(filter));
        applyTabStyle(btnTabLateEgress, "LATE_EGRESS".equals(filter));

        // Overdue sub-row: visible when IN direction or OVERDUE filter is active
        setOverdueRowVisible("IN".equals(filter) || "OVERDUE".equals(filter));
        // Late-egress sub-row: visible when OUT direction or LATE_EGRESS filter is active
        setLateEgressRowVisible("OUT".equals(filter) || "LATE_EGRESS".equals(filter));

        loadRequests();
    }

    private void applyTabStyle(Button btn, boolean active) {
        if (btn == null) return;
        if (active) {
            btn.setStyle("-fx-background-color: #7A0000; -fx-text-fill: #FFFFFF; " +
                    "-fx-background-radius: 20; -fx-font-size: 12px; -fx-font-weight: bold; " +
                    "-fx-cursor: hand; -fx-padding: 6 20;");
        } else {
            btn.setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #64748B; " +
                    "-fx-background-radius: 20; -fx-font-size: 12px; " +
                    "-fx-cursor: hand; -fx-padding: 6 20;");
        }
    }

    private void loadRequests() {
        paneRequestList.getChildren().clear();

        List<GateRequestRow> rows = fetchRequests();

        long pendingCount = rows.stream().filter(r -> "PENDING".equals(r.status)).count();
        lblPendingBadge.setText(pendingCount + " Pending");
        lblPendingBadge.setVisible(pendingCount > 0);
        lblPendingBadge.setManaged(pendingCount > 0);

        String query = (txtSearch != null) ? txtSearch.getText() : null;
        List<GateRequestRow> displayRows = filterBySearch(rows, query);

        if (displayRows.isEmpty()) {
            if (lblEmptyTitle != null) {
                lblEmptyTitle.setText(
                        (query != null && !query.isBlank()) ? "No matches found" : emptyTitleFor(activeFilter));
            }
            if (lblEmptySub != null) {
                lblEmptySub.setText(
                        (query != null && !query.isBlank())
                                ? "No requests match \"" + query.trim() + "\" in this view."
                                : emptySubFor(activeFilter));
            }
            paneEmpty.setVisible(true);
            paneEmpty.setManaged(true);
            scrollRequests.setVisible(false);
            scrollRequests.setManaged(false);
        } else {
            paneEmpty.setVisible(false);
            paneEmpty.setManaged(false);
            scrollRequests.setVisible(true);
            scrollRequests.setManaged(true);
            for (GateRequestRow row : displayRows) {
                paneRequestList.getChildren().add(buildRequestCard(row));
            }
        }

        loadStats();
    }

    private List<GateRequestRow> filterBySearch(List<GateRequestRow> rows, String query) {
        if (query == null || query.isBlank()) return rows;
        String q = query.trim().toLowerCase();

        List<GateRequestRow> result = new ArrayList<>();
        for (GateRequestRow row : rows) {
            boolean matches =
                    String.valueOf(row.studentId).contains(q)
                            || (row.studentCode != null && row.studentCode.toLowerCase().contains(q))
                            || (row.studentName != null && row.studentName.toLowerCase().contains(q))
                            || (row.studentSection != null && row.studentSection.toLowerCase().contains(q));

            if (!matches) {
                for (DeviceEntry de : row.devices) {
                    if ((de.serial != null && de.serial.toLowerCase().contains(q))
                            || (de.brand != null && de.brand.toLowerCase().contains(q))
                            || (de.model != null && de.model.toLowerCase().contains(q))
                            || (de.deviceType != null && de.deviceType.toLowerCase().contains(q))
                            || (de.currentLocation != null && de.currentLocation.toLowerCase().contains(q))) {
                        matches = true;
                        break;
                    }
                }
            }

            if (matches) result.add(row);
        }
        return result;
    }

    private String emptyTitleFor(String filter) {
        return switch (filter) {
            case "PENDING"     -> "No pending requests";
            case "REJECTED"    -> "No rejected requests today";
            case "APPROVED"    -> "No approved requests today";
            case "IN"          -> "No ingress requests";
            case "OUT"         -> "No egress requests";
            case "OVERDUE"     -> "No overdue requests";
            case "LATE_EGRESS" -> "No late egress requests";
            default             -> "No gate requests";
        };
    }

    private String emptySubFor(String filter) {
        return switch (filter) {
            case "PENDING"     -> "New gate requests awaiting review will appear here.";
            case "REJECTED"    -> "Requests you reject today will appear here.";
            case "APPROVED"    -> "Requests you approve today will appear here.";
            case "IN"          -> "Ingress (IN) gate requests will appear here.";
            case "OUT"         -> "Egress (OUT) requests appear here once their paired ingress is approved and the scheduled egress date is at least 3 days from today.";
            case "OVERDUE"     -> "Ingress requests that passed their scheduled time and were auto-rejected appear here.";
            case "LATE_EGRESS" -> "Pending egress requests that have passed their scheduled time appear here. They are never auto-rejected.";
            default             -> "Pending requests and today's decisions will appear here.";
        };
    }

    private static final String FETCH_OVERDUE_SQL = """
            SELECT gr.request_id, gr.student_id, gr.direction, gr.scheduled_time, gr.status,
                   s.full_name AS student_name, s.student_code, s.section,
                   COALESCE(s.email, '') AS student_email,
                   u.full_name AS requested_by_name,
                   TRUE AS is_late
            FROM gate_requests gr
            JOIN students s ON s.student_id = gr.student_id
            JOIN users u ON u.user_id = gr.requested_by
            CROSS JOIN (
                SELECT end_of_day_time FROM settings ORDER BY setting_id DESC LIMIT 1
            ) cfg
            WHERE gr.direction = 'IN'
              AND gr.status = 'REJECTED'
              AND gr.rejection_reason = ?
              AND DATE(gr.scheduled_time AT TIME ZONE 'Asia/Manila') >= CURRENT_DATE - INTERVAL '7 days'
              AND DATE(gr.scheduled_time AT TIME ZONE 'Asia/Manila') < CURRENT_DATE
            ORDER BY gr.scheduled_time ASC
            """;

    private static final String FETCH_REQUESTS_BASE_SQL = """
            SELECT gr.request_id, gr.student_id, gr.direction, gr.scheduled_time, gr.status,
                   s.full_name AS student_name, s.student_code, s.section,
                   COALESCE(s.email, '') AS student_email,
                   u.full_name AS requested_by_name,
                   (gr.status = 'PENDING' AND gr.scheduled_time < NOW()) AS is_late
            FROM gate_requests gr
            JOIN students s ON s.student_id = gr.student_id
            JOIN users u ON u.user_id = gr.requested_by
            CROSS JOIN (
                SELECT end_of_day_time FROM settings ORDER BY setting_id DESC LIMIT 1
            ) cfg
            WHERE (
              -- IN: show all PENDING ingress requests scheduled TODAY only
              -- (past-day ones are swept by autoRejectOverdue; today's stay until end_of_day)
              (gr.direction = 'IN' AND gr.status = 'PENDING'
               AND DATE(gr.scheduled_time AT TIME ZONE 'Asia/Manila') = CURRENT_DATE)

              -- OUT: show PENDING egress only when BOTH conditions are met:
              --   1. The paired IN request for the same student + same device set is APPROVED.
              --   2. Visibility window: shown as early as 3 days BEFORE the scheduled date.
              --      Hidden only when 4+ days away (scheduled_date > CURRENT_DATE + 3 days).
              --      Past-due egress is always visible and rises to top under ASC sort.
              OR (gr.direction = 'OUT' AND gr.status = 'PENDING'
                  -- Visible from 3 days before the scheduled date up to and including past-due.
                  -- Hidden only when 4+ days away (scheduled date > CURRENT_DATE + 3 days).
                  AND DATE(gr.scheduled_time AT TIME ZONE 'Asia/Manila') <= CURRENT_DATE + INTERVAL '3 days'
                  AND EXISTS (
                    SELECT 1 FROM gate_requests gr_in
                    WHERE gr_in.student_id = gr.student_id
                      AND gr_in.direction = 'IN'
                      AND gr_in.status = 'APPROVED'
                      AND gr_in.scheduled_time <= gr.scheduled_time
                      AND NOT EXISTS (
                        SELECT 1 FROM gate_request_devices grd_out
                        WHERE grd_out.request_id = gr.request_id
                          AND grd_out.device_id NOT IN (
                            SELECT device_id FROM gate_request_devices WHERE request_id = gr_in.request_id
                          )
                      )
                  ))

              -- APPROVED/REJECTED (non-overdue): resolved within the last 7 days.
              -- Limits data volume so officers aren't flooded with historical records.
              OR (gr.status IN ('APPROVED','REJECTED')
                  AND gr.rejection_reason IS DISTINCT FROM ?
                  AND gr.resolved_at >= NOW() - INTERVAL '7 days')
            )
            """;

    private List<GateRequestRow> fetchRequests() {
        List<GateRequestRow> list = new ArrayList<>();

        boolean isOverdueFilter = "OVERDUE".equals(activeFilter);

        String sql;
        if (isOverdueFilter) {
            sql = FETCH_OVERDUE_SQL;
        } else {
            StringBuilder sb = new StringBuilder(FETCH_REQUESTS_BASE_SQL);
            if ("PENDING".equals(activeFilter))      sb.append(" AND gr.status = 'PENDING'");
            if ("REJECTED".equals(activeFilter))     sb.append(" AND gr.status = 'REJECTED'");
            if ("APPROVED".equals(activeFilter))     sb.append(" AND gr.status = 'APPROVED'");

            // IN / OUT direction filters: PENDING only — never show APPROVED or REJECTED rows.
            // The base SQL already includes the matching PENDING clauses for each direction;
            // these guards strip out any APPROVED/REJECTED rows the base WHERE would otherwise surface.
            if ("IN".equals(activeFilter))  sb.append(" AND gr.direction = 'IN'  AND gr.status = 'PENDING'");
            if ("OUT".equals(activeFilter)) sb.append(" AND gr.direction = 'OUT' AND gr.status = 'PENDING'");

            // LATE_EGRESS: pending OUT requests whose scheduled time has already passed
            if ("LATE_EGRESS".equals(activeFilter))
                sb.append(" AND gr.direction = 'OUT' AND gr.status = 'PENDING' AND gr.scheduled_time < NOW()");

            // ALL filter: cap APPROVED/REJECTED to the last 7 days so officers aren't flooded.
            // PENDING rows in ALL are unaffected (they have no resolved_at).
            if ("ALL".equals(activeFilter))
                sb.append(" AND (gr.status = 'PENDING' OR gr.resolved_at >= NOW() - INTERVAL '7 days')");

            // Sort order per filter:
            //   APPROVED / REJECTED → most recently scheduled first (DESC)
            //   ALL, PENDING, IN, OUT, LATE_EGRESS → earliest scheduled first (ASC)
            if ("APPROVED".equals(activeFilter) || "REJECTED".equals(activeFilter)) {
                sb.append(" ORDER BY gr.scheduled_time DESC");
            } else {
                sb.append(" ORDER BY gr.scheduled_time ASC");
            }
            sql = sb.toString();
        }

        try (Connection conn = DBConnection.connect();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setString(1, OVERDUE_REASON);

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    GateRequestRow row = new GateRequestRow();
                    row.requestId        = rs.getInt("request_id");
                    row.studentId        = rs.getInt("student_id");
                    row.studentName      = rs.getString("student_name");
                    row.studentCode      = rs.getString("student_code");
                    row.studentSection   = rs.getString("section");
                    row.studentEmail     = rs.getString("student_email");
                    row.requestedByName  = rs.getString("requested_by_name");
                    row.direction        = rs.getString("direction");
                    row.status           = rs.getString("status");
                    row.isLate           = rs.getBoolean("is_late");

                    Timestamp ts = rs.getTimestamp("scheduled_time");
                    if (ts != null) {
                        row.scheduledTimeIn  = ts.toLocalDateTime();
                        row.scheduledTimeOut = ts.toLocalDateTime();
                    }

                    row.devices = fetchDevicesForRequest(conn, row.requestId);
                    list.add(row);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    private List<DeviceEntry> fetchDevicesForRequest(Connection conn, int requestId) {
        List<DeviceEntry> devices = new ArrayList<>();
        String sql = """
                SELECT d.device_id, d.brand, d.model, d.serial_number, d.device_type,
                       COALESCE(d.current_location, 'UNKNOWN') AS current_location
                FROM gate_request_devices grd
                JOIN devices d ON d.device_id = grd.device_id
                WHERE grd.request_id = ?
                ORDER BY d.device_id ASC
                """;
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, requestId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    DeviceEntry de = new DeviceEntry();
                    de.deviceId        = rs.getInt("device_id");
                    de.brand           = rs.getString("brand");
                    de.model           = rs.getString("model");
                    de.serial          = rs.getString("serial_number");
                    de.deviceType      = rs.getString("device_type");
                    de.currentLocation = rs.getString("current_location");
                    devices.add(de);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return devices;
    }

    private VBox buildRequestCard(GateRequestRow row) {
        boolean isPending  = "PENDING".equals(row.status);
        boolean isApproved = "APPROVED".equals(row.status);
        boolean isOverdue  = "OVERDUE".equals(activeFilter);
        // isLate: pending request (IN or OUT) that has passed its scheduled time
        boolean isLate     = isPending && row.isLate;

        VBox card = new VBox(0);
        card.setStyle(
                "-fx-background-color: #FFFFFF; " +
                        "-fx-background-radius: 14; " +
                        "-fx-border-radius: 14; " +
                        "-fx-border-color: " + (isOverdue ? "#C2410C" : isLate ? "#D97706" : isPending ? "#C9A84C" : isApproved ? "#16A34A" : "#DC2626") + "; " +
                        "-fx-border-width: 1.5; " +
                        "-fx-cursor: hand; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.06), 8, 0, 0, 2);"
        );
        card.setPadding(new Insets(14, 16, 14, 16));
        VBox.setMargin(card, new Insets(0, 0, 10, 0));

        HBox topRow = new HBox(12);
        topRow.setAlignment(Pos.CENTER_LEFT);

        StackPane avatar = new StackPane();
        avatar.setMinSize(40, 40); avatar.setMaxSize(40, 40);
        avatar.setStyle("-fx-background-color: #FDF6E3; -fx-background-radius: 50;");
        Label avatarLbl = new Label(row.studentName.substring(0, 1).toUpperCase());
        avatarLbl.setStyle("-fx-text-fill: #7A0000; -fx-font-weight: bold; -fx-font-size: 16px;");
        avatar.getChildren().add(avatarLbl);

        VBox nameBlock = new VBox(2);
        HBox.setHgrow(nameBlock, Priority.ALWAYS);
        Label lblName = new Label(row.studentName);
        lblName.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");
        Label lblMeta = new Label(row.studentCode + "  ·  " + row.studentSection);
        lblMeta.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");
        nameBlock.getChildren().addAll(lblName, lblMeta);

        Label statusBadge = new Label(isOverdue ? "OVERDUE" : isLate ? "LATE" : isPending ? "PENDING" : isApproved ? "APPROVED" : "REJECTED");
        statusBadge.setStyle(
                "-fx-background-color: " + (isOverdue ? "#FFEDD5" : isLate ? "#FEF3C7" : isPending ? "#FEF9C3" : isApproved ? "#DCFCE7" : "#FEE2E2") + "; " +
                        "-fx-text-fill: "        + (isOverdue ? "#C2410C" : isLate ? "#B45309" : isPending ? "#92400E" : isApproved ? "#15803D" : "#991B1B") + "; " +
                        "-fx-background-radius: 20; -fx-padding: 4 12; " +
                        "-fx-font-size: 10px; -fx-font-weight: bold;"
        );

        topRow.getChildren().addAll(avatar, nameBlock, statusBadge);

        Region divider = new Region();
        divider.setStyle("-fx-background-color: #F1F5F9;");
        divider.setMinHeight(1); divider.setMaxHeight(1);
        VBox.setMargin(divider, new Insets(10, 0, 10, 0));

        HBox infoRow = new HBox(24);
        infoRow.setAlignment(Pos.CENTER_LEFT);

        String dirLabel = "IN".equals(row.direction) ? "↓  Ingress" : "↑  Egress";
        VBox dirBlock = infoBlock("Direction", dirLabel);
        VBox timeBlock = infoBlock("Scheduled",
                row.scheduledTimeIn != null ? row.scheduledTimeIn.format(TIME_ONLY) : "—");
        VBox devBlock  = infoBlock("Devices",
                row.devices.size() + (row.devices.size() == 1 ? " device" : " devices"));

        infoRow.getChildren().addAll(dirBlock, timeBlock, devBlock);

        Label lblReqBy = new Label("Requested by: " + row.requestedByName);
        lblReqBy.setStyle("-fx-font-size: 11px; -fx-text-fill: #94A3B8; -fx-padding: 6 0 0 0;");

        card.getChildren().addAll(topRow, divider, infoRow, lblReqBy);

        card.setOnMouseEntered(e -> card.setStyle(card.getStyle()
                .replace("-fx-background-color: #FFFFFF", "-fx-background-color: #FFFBF0")));
        card.setOnMouseExited(e -> card.setStyle(card.getStyle()
                .replace("-fx-background-color: #FFFBF0", "-fx-background-color: #FFFFFF")));

        card.setOnMouseClicked(e -> openDetailModal(row));

        return card;
    }

    private VBox infoBlock(String label, String value) {
        VBox box = new VBox(2);
        Label lbl = new Label(label.toUpperCase());
        lbl.setStyle("-fx-font-size: 9px; -fx-font-weight: 700; -fx-text-fill: #94A3B8; -fx-letter-spacing: 0.5px;");
        Label val = new Label(value);
        val.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #1E293B;");
        box.getChildren().addAll(lbl, val);
        return box;
    }

    private void openDetailModal(GateRequestRow row) {
        selectedRequest = row;
        paneDetailModal.getChildren().clear();

        boolean isPending = "PENDING".equals(row.status);

        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.5);");
        overlay.setAlignment(Pos.CENTER);
        overlay.setOnMouseClicked(e -> closeDetailModal());
        overlay.prefWidthProperty().bind(paneDetailModal.widthProperty());
        overlay.prefHeightProperty().bind(paneDetailModal.heightProperty());

        VBox modal = new VBox(0);
        modal.setMaxWidth(520);
        modal.setMaxHeight(Region.USE_PREF_SIZE);
        modal.setStyle(
                "-fx-background-color: #FFFFFF; -fx-background-radius: 18; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.28), 30, 0, 0, 8);"
        );
        modal.setOnMouseClicked(javafx.event.Event::consume);

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16, 20, 14, 20));
        header.setStyle("-fx-border-color: #F1F5F9; -fx-border-width: 0 0 1 0;");

        StackPane headerIcon = new StackPane();
        headerIcon.setMinSize(44, 44); headerIcon.setMaxSize(44, 44);
        headerIcon.setStyle("-fx-background-color: #FDF6E3; -fx-background-radius: 12;");
        Label iconLbl = new Label("🚪");
        iconLbl.setStyle("-fx-font-size: 20px;");
        headerIcon.getChildren().add(iconLbl);

        VBox headerText = new VBox(2);
        HBox.setHgrow(headerText, Priority.ALWAYS);
        Label lblModalTitle = new Label("Gate Request  #" + row.requestId);
        lblModalTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");
        Label lblModalSub = new Label("Review student and device details");
        lblModalSub.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");
        headerText.getChildren().addAll(lblModalTitle, lblModalSub);

        Button btnClose = new Button("✕");
        btnClose.setStyle(
                "-fx-background-color: #F1F5F9; -fx-background-radius: 50; " +
                        "-fx-text-fill: #64748B; -fx-font-size: 12px; -fx-cursor: hand; " +
                        "-fx-min-width: 30; -fx-min-height: 30; -fx-max-width: 30; -fx-max-height: 30;");
        btnClose.setOnAction(e -> closeDetailModal());
        header.getChildren().addAll(headerIcon, headerText, btnClose);

        VBox body = new VBox(12);
        body.setPadding(new Insets(16, 20, 16, 20));

        body.getChildren().add(sectionLabel("STUDENT INFORMATION"));

        HBox studentCard = new HBox(14);
        studentCard.setAlignment(Pos.CENTER_LEFT);
        studentCard.setStyle(
                "-fx-background-color: #F8FAFC; -fx-background-radius: 12; " +
                        "-fx-border-color: #E2E8F0; -fx-border-radius: 12; -fx-border-width: 1; -fx-padding: 12 14;"
        );
        StackPane sAvatar = new StackPane();
        sAvatar.setMinSize(44, 44); sAvatar.setMaxSize(44, 44);
        sAvatar.setStyle("-fx-background-color: #7A0000; -fx-background-radius: 50;");
        Label sAvatarLbl = new Label(row.studentName.substring(0, 1).toUpperCase());
        sAvatarLbl.setStyle("-fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-font-size: 18px;");
        sAvatar.getChildren().add(sAvatarLbl);

        VBox sInfo = new VBox(3);
        Label sName = new Label(row.studentName);
        sName.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #0F172A;");
        Label sCode = new Label(row.studentCode + "  ·  " + row.studentSection);
        sCode.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");
        Label sEmail = new Label(row.studentEmail);
        sEmail.setStyle("-fx-font-size: 11px; -fx-text-fill: #94A3B8;");
        sInfo.getChildren().addAll(sName, sCode, sEmail);
        studentCard.getChildren().addAll(sAvatar, sInfo);
        body.getChildren().add(studentCard);

        String dirSection = "IN".equals(row.direction) ? "INGRESS" : "EGRESS";
        body.getChildren().add(sectionLabel("SCHEDULED TIME  ·  " + dirSection));
        HBox schedRow = new HBox(12);
        schedRow.setAlignment(Pos.CENTER_LEFT);
        String schedIcon = "IN".equals(row.direction) ? "↓ Ingress" : "↑ Egress";
        if (row.scheduledTimeIn != null) {
            schedRow.getChildren().add(scheduleChip(schedIcon, row.scheduledTimeIn.format(DT_DISPLAY)));
        }
        body.getChildren().add(schedRow);

        body.getChildren().add(sectionLabel("DEVICES  (" + row.devices.size() + ")"));
        for (DeviceEntry de : row.devices) {
            body.getChildren().add(buildDeviceChip(de));
        }

        Label reqByLbl = new Label("Requested by: " + row.requestedByName);
        reqByLbl.setStyle(
                "-fx-background-color: #F8FAFC; -fx-border-color: #E2E8F0; " +
                        "-fx-border-radius: 8; -fx-background-radius: 8; -fx-border-width: 1; " +
                        "-fx-padding: 8 14; -fx-font-size: 12px; -fx-text-fill: #475569;"
        );
        body.getChildren().add(reqByLbl);

        if (isPending) {
            boolean isEgress = "OUT".equals(row.direction);

            Region footerDiv = new Region();
            footerDiv.setStyle("-fx-background-color: #F1F5F9;");
            footerDiv.setMinHeight(1); footerDiv.setMaxHeight(1);

            HBox footer = new HBox(12);
            footer.setAlignment(Pos.CENTER_RIGHT);
            footer.setPadding(new Insets(12, 20, 16, 20));

            if (!isEgress) {
                Button btnReject = new Button("Reject");
                btnReject.setPrefHeight(40); btnReject.setPrefWidth(120);
                btnReject.setStyle(
                        "-fx-background-color: #FFFFFF; -fx-text-fill: #DC2626; " +
                                "-fx-border-color: #DC2626; -fx-border-width: 1.5; -fx-border-radius: 10; " +
                                "-fx-background-radius: 10; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 13px;"
                );
                btnReject.setOnAction(e -> openConfirmModal("REJECT"));
                footer.getChildren().add(btnReject);
            }

            Button btnApprove = new Button("Approve");
            btnApprove.setPrefHeight(40); btnApprove.setPrefWidth(140);
            btnApprove.setStyle(
                    "-fx-background-color: #7A0000; -fx-text-fill: #FFFFFF; " +
                            "-fx-background-radius: 10; -fx-border-radius: 10; " +
                            "-fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 13px;"
            );
            btnApprove.setOnAction(e -> openConfirmModal("APPROVE"));
            footer.getChildren().add(btnApprove);

            modal.getChildren().addAll(header, body, footerDiv, footer);
        } else {
            modal.getChildren().addAll(header, body);
        }

        overlay.getChildren().add(modal);
        paneDetailModal.getChildren().add(overlay);

        paneDetailModal.setOpacity(0);
        paneDetailModal.setVisible(true);
        paneDetailModal.setManaged(true);
        FadeTransition ft = new FadeTransition(Duration.millis(180), paneDetailModal);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
    }

    private void closeDetailModal() {
        FadeTransition ft = new FadeTransition(Duration.millis(150), paneDetailModal);
        ft.setFromValue(1); ft.setToValue(0);
        ft.setOnFinished(e -> {
            paneDetailModal.setVisible(false);
            paneDetailModal.setManaged(false);
            paneDetailModal.getChildren().clear();
        });
        ft.play();
    }

    private Label sectionLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #C9A84C; -fx-letter-spacing: 1px;");
        return lbl;
    }

    private HBox scheduleChip(String label, String time) {
        HBox chip = new HBox(8);
        chip.setAlignment(Pos.CENTER_LEFT);
        chip.setStyle(
                "-fx-background-color: #F0FDF4; -fx-background-radius: 10; " +
                        "-fx-border-color: #86EFAC; -fx-border-radius: 10; -fx-border-width: 1; -fx-padding: 10 14;"
        );
        HBox.setHgrow(chip, Priority.ALWAYS);
        VBox chipText = new VBox(2);
        Label chipLabel = new Label(label);
        chipLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #15803D;");
        Label chipTime = new Label(time);
        chipTime.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #0F172A;");
        chipText.getChildren().addAll(chipLabel, chipTime);
        chip.getChildren().add(chipText);
        return chip;
    }

    private HBox buildDeviceChip(DeviceEntry de) {
        HBox chip = new HBox(12);
        chip.setAlignment(Pos.CENTER_LEFT);
        VBox.setMargin(chip, new Insets(0, 0, 6, 0));
        chip.setStyle(
                "-fx-background-color: #FFFFFF; -fx-background-radius: 10; " +
                        "-fx-border-color: #C9A84C; -fx-border-radius: 10; -fx-border-width: 1; -fx-padding: 10 12;"
        );

        StackPane iconBox = new StackPane();
        iconBox.setMinSize(36, 36); iconBox.setMaxSize(36, 36);
        iconBox.setStyle("-fx-background-color: #FDF6E3; -fx-background-radius: 8;");
        SVGPath icon = new SVGPath();
        icon.setContent(switch (de.deviceType) {
            case "LAPTOP" -> "M4 6a2 2 0 012-2h12a2 2 0 012 2v8a2 2 0 01-2 2H6a2 2 0 01-2-2V6zM2 20h20";
            case "TABLET" -> "M12 18h.01M7 21h10a2 2 0 002-2V5a2 2 0 00-2-2H7a2 2 0 00-2 2v14a2 2 0 002 2z";
            default       -> "M20 7H4a2 2 0 00-2 2v6a2 2 0 002 2h16a2 2 0 002-2V9a2 2 0 00-2-2z";
        });
        icon.setStyle("-fx-stroke: #7A0000; -fx-fill: transparent; -fx-stroke-width: 1.8;");
        iconBox.getChildren().add(icon);

        VBox info = new VBox(2);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label devName = new Label(de.brand + " " + de.model);
        devName.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #1E293B;");
        Label devSerial = new Label("S/N: " + de.serial + "  ·  " + de.deviceType);
        devSerial.setStyle("-fx-font-size: 11px; -fx-text-fill: #94A3B8;");
        info.getChildren().addAll(devName, devSerial);

        String loc = de.currentLocation != null ? de.currentLocation : "UNKNOWN";
        Label locBadge = new Label(loc);
        locBadge.setStyle(
                "-fx-background-color: " + ("IN".equals(loc) ? "#DCFCE7" : "OUT".equals(loc) ? "#FEF3C7" : "#F1F5F9") + "; " +
                        "-fx-text-fill: "        + ("IN".equals(loc) ? "#15803D" : "OUT".equals(loc) ? "#92400E" : "#64748B") + "; " +
                        "-fx-background-radius: 20; -fx-padding: 3 10; -fx-font-size: 10px; -fx-font-weight: bold;"
        );

        chip.getChildren().addAll(iconBox, info, locBadge);
        return chip;
    }

    private void openConfirmModal(String action) {
        pendingAction = action;
        paneConfirmModal.getChildren().clear();

        boolean isApprove = "APPROVE".equals(action);

        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.55);");
        overlay.setAlignment(Pos.CENTER);
        overlay.setOnMouseClicked(javafx.event.Event::consume);
        overlay.prefWidthProperty().bind(paneConfirmModal.widthProperty());
        overlay.prefHeightProperty().bind(paneConfirmModal.heightProperty());

        VBox dialog = new VBox(12);
        dialog.setMaxWidth(420);
        dialog.setMaxHeight(Region.USE_PREF_SIZE);
        dialog.setAlignment(Pos.CENTER);
        dialog.setPadding(new Insets(24, 28, 22, 28));
        dialog.setStyle(
                "-fx-background-color: #FFFFFF; -fx-background-radius: 18; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.28), 24, 0, 0, 6);"
        );
        dialog.setOnMouseClicked(javafx.event.Event::consume);

        StackPane iconCircle = new StackPane();
        iconCircle.setMinSize(60, 60); iconCircle.setMaxSize(60, 60);
        iconCircle.setStyle("-fx-background-color: " + (isApprove ? "#DCFCE7" : "#FEE2E2") + "; -fx-background-radius: 30;");
        Label iconLbl = new Label(isApprove ? "✓" : "✕");
        iconLbl.setStyle("-fx-text-fill: " + (isApprove ? "#15803D" : "#DC2626") + "; -fx-font-size: 24px; -fx-font-weight: bold;");
        iconCircle.getChildren().add(iconLbl);

        Label lblTitle = new Label(isApprove ? "Approve Request?" : "Reject Request?");
        lblTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");

        Label lblSub = new Label(isApprove
                ? "This will approve Gate Request #" + selectedRequest.requestId +
                  " for " + selectedRequest.studentName +
                  " (" + ("IN".equals(selectedRequest.direction) ? "Ingress" : "Egress") +
                  ") and log all " + selectedRequest.devices.size() + " device(s) into the system."
                : "This will reject Gate Request #" + selectedRequest.requestId +
                  " for " + selectedRequest.studentName +
                  " (" + ("IN".equals(selectedRequest.direction) ? "Ingress" : "Egress") +
                  "). A rejection reason is required.");
        lblSub.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748B; -fx-wrap-text: true;");
        lblSub.setWrapText(true);
        lblSub.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        lblSub.setMaxWidth(340);

        TextArea txtReason = new TextArea();
        Label lblReasonErr = new Label("Rejection reason is required.");
        if (!isApprove) {
            txtReason.setPromptText("Enter reason for rejection...");
            txtReason.setPrefRowCount(2);
            txtReason.setMaxHeight(64);
            txtReason.setWrapText(true);
            txtReason.setStyle(
                    "-fx-background-color: #FFFFFF; -fx-background-radius: 8; " +
                            "-fx-border-color: #D1D5DB; -fx-border-radius: 8; -fx-border-width: 0.75; " +
                            "-fx-font-size: 12px; -fx-padding: 7 10;"
            );
            txtReason.prefWidthProperty().bind(dialog.widthProperty().multiply(0.99));
            txtReason.maxWidthProperty().bind(dialog.widthProperty().multiply(0.99));
            lblReasonErr.setStyle("-fx-text-fill: #DC2626; -fx-font-size: 12px; -fx-font-weight: 600;");
            lblReasonErr.setVisible(false);
            lblReasonErr.setManaged(false);
        }

        // Buttons
        Button btnCancel = new Button("Cancel");
        btnCancel.setPrefWidth(130); btnCancel.setPrefHeight(42);
        btnCancel.setStyle(
                "-fx-background-color: #FFFFFF; -fx-text-fill: #374151; " +
                        "-fx-border-color: #D1D5DB; -fx-border-width: 1.5; -fx-border-radius: 10; " +
                        "-fx-background-radius: 10; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 13px;"
        );
        btnCancel.setOnAction(e -> closeConfirmModal());

        Button btnConfirm = new Button(isApprove ? "Approve" : "Reject");
        btnConfirm.setPrefWidth(140); btnConfirm.setPrefHeight(42);
        btnConfirm.setStyle(
                "-fx-background-color: " + (isApprove ? "#7A0000" : "#DC2626") + "; -fx-text-fill: #FFFFFF; " +
                        "-fx-background-radius: 10; -fx-border-radius: 10; " +
                        "-fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 13px;"
        );
        btnConfirm.setOnAction(e -> {
            if (!isApprove) {
                String reason = txtReason.getText().trim();
                if (reason.isEmpty()) {
                    lblReasonErr.setVisible(true);
                    lblReasonErr.setManaged(true);
                    txtReason.setStyle(
                            "-fx-background-color: #FFFFFF; -fx-background-radius: 8; " +
                                    "-fx-border-color: #DC2626; -fx-border-radius: 8; -fx-border-width: 0.75; " +
                                    "-fx-font-size: 12px; -fx-padding: 7 10;"
                    );
                    return;
                }
                performReject(reason);
            } else {
                performApprove();
            }
        });

        HBox btnRow = new HBox(12, btnCancel, btnConfirm);
        btnRow.setAlignment(Pos.CENTER);

        dialog.getChildren().addAll(iconCircle, lblTitle, lblSub);
        if (!isApprove) {
            dialog.getChildren().addAll(txtReason, lblReasonErr);
        }
        dialog.getChildren().add(btnRow);

        overlay.getChildren().add(dialog);
        paneConfirmModal.getChildren().add(overlay);

        paneConfirmModal.setOpacity(0);
        paneConfirmModal.setVisible(true);
        paneConfirmModal.setManaged(true);
        FadeTransition ft = new FadeTransition(Duration.millis(180), paneConfirmModal);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
    }

    private void closeConfirmModal() {
        FadeTransition ft = new FadeTransition(Duration.millis(150), paneConfirmModal);
        ft.setFromValue(1); ft.setToValue(0);
        ft.setOnFinished(e -> {
            paneConfirmModal.setVisible(false);
            paneConfirmModal.setManaged(false);
            paneConfirmModal.getChildren().clear();
        });
        ft.play();
    }

    private static final String APPROVE_UPDATE_SQL = """
            UPDATE gate_requests
            SET status = 'APPROVED', approved_by = ?, resolved_at = NOW()
            WHERE request_id = ?
            """;

    private static final String FETCH_SCHEDULED_TIME_SQL = """
            SELECT scheduled_time FROM gate_requests WHERE request_id = ?
            """;

    private static final String INSERT_DEVICE_LOG_SQL = """
            INSERT INTO device_logs
            (device_id, operator_id, direction, log_time, scheduled_time,
             official_time_in, official_time_out, status, batch_id, gate_request_id)
            VALUES (?, ?, ?, NOW(), ?, ?, ?, 'NORMAL', ?::uuid, ?)
            """;

    private static final String UPDATE_DEVICE_LOCATION_SQL = """
            UPDATE devices SET current_location = ?, updated_at = NOW() WHERE device_id = ?
            """;

    private void performApprove() {
        int officerId = UserSession.getInstance().getUserId();
        int requestId = selectedRequest.requestId;
        boolean isIngress = "IN".equals(selectedRequest.direction);

        try (Connection conn = DBConnection.connect()) {
            conn.setAutoCommit(false);

            try (PreparedStatement pst = conn.prepareStatement(APPROVE_UPDATE_SQL)) {
                pst.setInt(1, officerId);
                pst.setInt(2, requestId);
                pst.executeUpdate();
            }

            Timestamp scheduledTs = null;
            try (PreparedStatement pst = conn.prepareStatement(FETCH_SCHEDULED_TIME_SQL)) {
                pst.setInt(1, requestId);
                try (ResultSet rs = pst.executeQuery()) {
                    if (rs.next()) scheduledTs = rs.getTimestamp("scheduled_time");
                }
            }

            UUID batchId = UUID.randomUUID();
            Timestamp now = new Timestamp(System.currentTimeMillis());

            for (DeviceEntry de : selectedRequest.devices) {
                try (PreparedStatement pst = conn.prepareStatement(INSERT_DEVICE_LOG_SQL)) {
                    pst.setInt(1, de.deviceId);
                    pst.setInt(2, officerId);
                    pst.setString(3, selectedRequest.direction);
                    pst.setTimestamp(4, scheduledTs);
                    if (isIngress) {
                        pst.setTimestamp(5, now);
                        pst.setTimestamp(6, null);
                    } else {
                        pst.setTimestamp(5, null);
                        pst.setTimestamp(6, now);
                    }
                    pst.setString(7, batchId.toString());
                    pst.setInt(8, requestId);
                    pst.executeUpdate();
                }

                String newLocation = isIngress ? "IN" : "OUT";
                try (PreparedStatement pst = conn.prepareStatement(UPDATE_DEVICE_LOCATION_SQL)) {
                    pst.setString(1, newLocation);
                    pst.setInt(2, de.deviceId);
                    pst.executeUpdate();
                }
            }

            AuditLogger.log(conn, officerId, "GATE_REQUEST_APPROVED", "gate_requests", requestId,
                    "{\"request_id\":" + requestId +
                            ",\"direction\":\"" + selectedRequest.direction + "\"" +
                            ",\"student\":\"" + selectedRequest.studentName + "\"" +
                            ",\"devices\":" + selectedRequest.devices.size() + "}");

            conn.commit();

            closeConfirmModal();
            closeDetailModal();
            Platform.runLater(this::loadRequests);

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }


    // ─── Auto-reject overdue requests ────────────────────────────────────────

    /**
     * Sweeps for PENDING IN requests whose scheduled DATE is before today (past days),
     * or whose scheduled DATE is today AND the day has officially ended (current time ≥ end_of_day_time).
     *
     * Egress (OUT) requests are NEVER auto-rejected — they remain pending indefinitely
     * until an officer acts on them. If the paired IN was already rejected, the OUT
     * request should have been cascade-rejected at that time by performReject().
     *
     * approved_by is left NULL — this is a system action, not an officer decision.
     */
    private void autoRejectOverdue() {
        // First, check if the current day has officially ended
        LocalTime[] hours = loadOperatingHours();
        LocalTime endOfDay = (hours != null) ? hours[1] : null;
        boolean dayHasEnded = (endOfDay != null) && LocalTime.now().isAfter(endOfDay);

        // Build the find query:
        // - Past-day PENDING IN requests (always overdue regardless of time)
        // - Today's PENDING IN requests, but only if end_of_day has passed
        String findSql;
        if (dayHasEnded) {
            // Sweep both past days AND today (day officially ended)
            findSql = "SELECT gr.request_id, gr.student_id, s.full_name AS student_name " +
                    "FROM gate_requests gr " +
                    "JOIN students s ON s.student_id = gr.student_id " +
                    "WHERE gr.direction = 'IN' " +
                    "  AND gr.status    = 'PENDING' " +
                    "  AND DATE(gr.scheduled_time AT TIME ZONE 'Asia/Manila') <= CURRENT_DATE";
        } else {
            // Only sweep past days; today's pending IN requests stay pending until end_of_day
            findSql = "SELECT gr.request_id, gr.student_id, s.full_name AS student_name " +
                    "FROM gate_requests gr " +
                    "JOIN students s ON s.student_id = gr.student_id " +
                    "WHERE gr.direction = 'IN' " +
                    "  AND gr.status    = 'PENDING' " +
                    "  AND DATE(gr.scheduled_time AT TIME ZONE 'Asia/Manila') < CURRENT_DATE";
        }

        int officerId;
        try {
            officerId = UserSession.getInstance().getUserId();
        } catch (Exception ex) {
            officerId = 0;
        }

        try (Connection conn = DBConnection.connect()) {
            conn.setAutoCommit(false);

            List<int[]> overdueList = new ArrayList<>();
            List<String> overdueNames = new ArrayList<>();
            try (PreparedStatement pst = conn.prepareStatement(findSql);
                 ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    overdueList.add(new int[]{rs.getInt("request_id"), rs.getInt("student_id")});
                    overdueNames.add(rs.getString("student_name"));
                }
            }

            if (overdueList.isEmpty()) {
                conn.rollback();
                return;
            }

            String rejectInSql =
                    "UPDATE gate_requests " +
                            "SET status = 'REJECTED', approved_by = NULL, " +
                            "    rejection_reason = ?, resolved_at = NOW() " +
                            "WHERE request_id = ?";

            for (int i = 0; i < overdueList.size(); i++) {
                int requestId  = overdueList.get(i)[0];
                String studentName = overdueNames.get(i);

                // Reject the overdue IN request only — OUT requests are never auto-rejected
                try (PreparedStatement pst = conn.prepareStatement(rejectInSql)) {
                    pst.setString(1, OVERDUE_REASON);
                    pst.setInt(2, requestId);
                    pst.executeUpdate();
                }
                AuditLogger.log(conn, officerId, "GATE_REQUEST_REJECTED",
                        "gate_requests", requestId,
                        "{\"request_id\":" + requestId +
                                ",\"direction\":\"IN\"" +
                                ",\"student\":\"" + studentName + "\"" +
                                ",\"reason\":\"" + OVERDUE_REASON + "\"" +
                                ",\"auto\":true}");
            }

            conn.commit();

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void performReject(String reason) {
        // Egress (OUT) requests must never be rejectable — once the paired ingress
        // is approved, the device is already inside, so this is a hard rule rather
        // than just a UI affordance. The Reject button is hidden for OUT in the
        // modal, but this guard protects the action itself regardless of caller.
        if ("OUT".equals(selectedRequest.direction)) {
            System.err.println("Blocked attempt to reject an egress (OUT) request #" + selectedRequest.requestId
                    + " — egress requests are approve-only.");
            return;
        }

        int officerId = UserSession.getInstance().getUserId();
        int requestId = selectedRequest.requestId;
        boolean isIngress = "IN".equals(selectedRequest.direction);

        try (Connection conn = DBConnection.connect()) {
            conn.setAutoCommit(false);

            // Reject this request
            String updateReq =
                    "UPDATE gate_requests " +
                            "SET status = 'REJECTED', approved_by = ?, rejection_reason = ?, resolved_at = NOW() " +
                            "WHERE request_id = ?";
            try (PreparedStatement pst = conn.prepareStatement(updateReq)) {
                pst.setInt(1, officerId);
                pst.setString(2, reason);
                pst.setInt(3, requestId);
                pst.executeUpdate();
            }

            AuditLogger.log(conn, officerId, "GATE_REQUEST_REJECTED", "gate_requests", requestId,
                    "{\"request_id\":" + requestId +
                            ",\"direction\":\"" + selectedRequest.direction + "\"" +
                            ",\"student\":\"" + selectedRequest.studentName + "\"" +
                            ",\"reason\":\"" + reason.replace("\"", "'") + "\"}");

            // If rejecting an IN request, cascade-reject the paired pending OUT request
            // for the same student that shares the same device set.
            if (isIngress) {
                String cascadeRejectSql =
                        "UPDATE gate_requests gr_out " +
                                "SET status = 'REJECTED', approved_by = ?, " +
                                "    rejection_reason = 'Paired ingress request was rejected.', " +
                                "    resolved_at = NOW() " +
                                "WHERE gr_out.direction = 'OUT' " +
                                "  AND gr_out.status = 'PENDING' " +
                                "  AND gr_out.student_id = ? " +
                                "  AND NOT EXISTS (" +
                                "    SELECT 1 FROM gate_request_devices grd_out " +
                                "    WHERE grd_out.request_id = gr_out.request_id " +
                                "      AND grd_out.device_id NOT IN (" +
                                "        SELECT device_id FROM gate_request_devices WHERE request_id = ?" +
                                "      )" +
                                "  ) RETURNING request_id";
                try (PreparedStatement pst = conn.prepareStatement(cascadeRejectSql)) {
                    pst.setInt(1, officerId);
                    pst.setInt(2, selectedRequest.studentId);
                    pst.setInt(3, requestId);
                    try (ResultSet rs = pst.executeQuery()) {
                        while (rs.next()) {
                            int cascadedId = rs.getInt("request_id");
                            AuditLogger.log(conn, officerId, "GATE_REQUEST_REJECTED",
                                    "gate_requests", cascadedId,
                                    "{\"request_id\":" + cascadedId +
                                            ",\"direction\":\"OUT\"" +
                                            ",\"student\":\"" + selectedRequest.studentName + "\"" +
                                            ",\"reason\":\"Paired ingress request #" + requestId + " was rejected.\"}");
                        }
                    }
                }
            }

            conn.commit();

            closeConfirmModal();
            closeDetailModal();
            Platform.runLater(this::loadRequests);

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
}