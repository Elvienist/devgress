package com.example.byodsystem.byod.controller;

import com.example.byodsystem.byod.database.DBConnection;
import com.example.byodsystem.byod.model.ActivityLog;
import com.example.byodsystem.byod.service.UserSession;
import com.example.byodsystem.byod.service.AuditLogger;
import com.example.byodsystem.byod.model.Device;
import com.example.byodsystem.byod.model.Student;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.stage.FileChooser;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;
import javafx.util.StringConverter;
import org.mindrot.jbcrypt.BCrypt;
import java.sql.*;

public class StudentsDevicesController {

    @FXML private VBox paneStudentsView;
    @FXML private VBox paneDevicesView;
    @FXML private VBox paneStudentFormModal;
    @FXML private VBox paneDeviceFormModal;
    @FXML private VBox paneStudentProfileView;
    @FXML private VBox paneDeviceProfileView;
    @FXML private VBox paneUnclosedLogBanner;

    @FXML private Label lblProfileStudentCode, lblProfileStudentName, lblProfileStudentSection, lblProfileStudentEmail, lblProfileStudentStatus;
    @FXML private Label lblTotalEntries, lblTotalExits, lblLastSeen;
    @FXML private Button btnToggleStudentLog;
    @FXML private TableView<ActivityLog> tblStudentActivityLog;
    @FXML private TableColumn<ActivityLog, String> colSLogDevice, colSLogTimeIn, colSLogTimeOut, colSLogStatus;
    @FXML private TableColumn<ActivityLog, ActivityLog> colSLogDetails;

    @FXML private Label lblProfileDeviceSerial, lblProfileDeviceType, lblProfileDeviceBrand, lblProfileDeviceModel, lblProfileDeviceOwner, lblDeviceStateBadge;
    @FXML private TableView<ActivityLog> tblDeviceActivityLog;
    @FXML private TableColumn<ActivityLog, String> colDLogTimeIn, colDLogTimeOut, colDLogStatus;
    @FXML private TableColumn<ActivityLog, ActivityLog> colDLogDetails;

    @FXML private TextField txtStudentCode, txtStudentName, txtSection, txtEmail;
    @FXML private TextField txtSerialNumber, txtBrand, txtModel;
    @FXML private ComboBox<String> cmbDeviceType;
    @FXML private ComboBox<Student> cmbDeviceOwner;
    @FXML private TextField txtSearchStudent, txtSearchDevice;
    @FXML private TextField txtSearchDeviceOwner;

    @FXML private Label lblErrorStudentCode;
    @FXML private Label lblErrorStudentName;
    @FXML private Label lblErrorSection;
    @FXML private Label lblErrorEmail;

    @FXML private Label lblErrorSerialNumber;
    @FXML private Label lblErrorDeviceType;
    @FXML private Label lblErrorBrand;
    @FXML private Label lblErrorModel;
    @FXML private Label lblErrorDeviceOwner;

    @FXML private TableView<Student> tblStudents;
    @FXML private TableColumn<Student, String> colStudentCode, colStudentName, colStudentSection, colStudentDevicesCount, colStudentStatus;
    @FXML private TableColumn<Student, Void> colStudentActions;

    @FXML private TableView<Device> tblDevices;
    @FXML private TableColumn<Device, String> colDeviceSerial, colDeviceBrand, colDeviceModel, colDeviceType, colDeviceOwner, colDeviceState, colDeviceLocation;
    @FXML private TableColumn<Device, Void> colDeviceActions;

    @FXML private Button btnStudentFilterActive, btnStudentFilterInactive;
    @FXML private Button btnDeviceFilterActive, btnDeviceFilterInactive;
    @FXML private Button btnDeviceFilterInside, btnDeviceFilterOutside;

    @FXML private VBox paneMassAddModal;
    @FXML private VBox paneMassAddTimerModal;
    @FXML private Label lblSelectedFile;
    @FXML private Label lblCountdown;
    @FXML private Button btnConfirmMassAdd;

    @FXML private Button btnAddStudent;
    @FXML private Button btnAddDevice;
    @FXML private Button btnMassAdd;

    private File selectedCsvFile = null;
    private Timeline massAddTimeline;

    private final ObservableList<Student> studentMasterList = FXCollections.observableArrayList();
    private final ObservableList<Device> deviceMasterList = FXCollections.observableArrayList();

    private FilteredList<Student> filteredStudents;
    private FilteredList<Device> filteredDevices;
    private FilteredList<Student> filteredOwners;

    private Student currentlySelectedStudent = null;
    private Device currentlySelectedDevice = null;
    private Student currentProfileStudent = null;
    private Device currentProfileDevice = null;

    private String currentStudentStatusFilter = "ACTIVE";
    private String currentDeviceStatusFilter = "ACTIVE";
    private String currentDeviceLocationFilter = "ALL";

    private String currentUserRole = "ADMIN";

    private StackPane rootStackPane = null;

    public void setUserRole(String role) {
        this.currentUserRole = role;
        configureTableColumns();
    }

    @FXML
    public void initialize() {
        cmbDeviceType.setItems(FXCollections.observableArrayList("LAPTOP", "TABLET", "OTHER"));

        filteredStudents = new FilteredList<>(studentMasterList, p -> true);
        filteredDevices = new FilteredList<>(deviceMasterList, p -> true);
        filteredOwners = new FilteredList<>(studentMasterList, p -> true);

        cmbDeviceOwner.setConverter(new StringConverter<Student>() {
            @Override
            public String toString(Student student) {
                if (student == null) return "";
                return student.getStudentCode() + " - " + student.getFullName();
            }
            @Override
            public Student fromString(String string) { return null; }
        });

        UserSession session = UserSession.getInstance();
        String role = session.getRole() != null ? session.getRole().toUpperCase() : "";

        if ("OFFICER".equals(role)) {
            if (btnAddStudent != null) { btnAddStudent.setVisible(false); btnAddStudent.setManaged(false); }
            if (btnAddDevice  != null) { btnAddDevice.setVisible(false);  btnAddDevice.setManaged(false);  }
            if (btnMassAdd    != null) { btnMassAdd.setVisible(false);    btnMassAdd.setManaged(false);    }
        }

        configureTableColumns();
        loadStudentsFromDatabase();
        loadDevicesFromDatabase();
        setupSearchAndFilters();
        setupOwnerLiveSearch();
        updateFilterButtonStyles();
    }

    private StackPane getRootStack() {
        if (rootStackPane != null) return rootStackPane;
        javafx.scene.Node node = paneStudentsView;
        while (node != null) {
            if (node instanceof StackPane) {
                rootStackPane = (StackPane) node;
                break;
            }
            node = node.getParent();
        }
        return rootStackPane;
    }

    private void showOverlayPopup(String title, String subtitle) {
        StackPane root = getRootStack();
        if (root == null) return;

        boolean isError = title.toLowerCase().contains("error") || title.toLowerCase().contains("fail");
        boolean isWarning = title.toLowerCase().contains("validation");

        String iconColor = isError ? "#DC2626" : isWarning ? "#D97706" : "#2E7D32";
        String iconBg    = isError ? "#FEE2E2" : isWarning ? "#FEF3C7" : "#E8F5E9";
        String btnColor  = isError ? "#DC2626" : isWarning ? "#D97706" : "#2E7D32";
        String iconSvg   = isError
                ? "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"
                : isWarning
                  ? "M1 21h22L12 2 1 21zm12-3h-2v-2h2v2zm0-4h-2v-4h2v4z"
                  : "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 14.5l-4-4 1.41-1.41L10 13.67l6.59-6.59L18 8.5l-8 8z";

        Region dimLayer = new Region();
        dimLayer.setStyle("-fx-background-color: rgba(0,0,0,0.45);");
        dimLayer.prefWidthProperty().bind(root.widthProperty());
        dimLayer.prefHeightProperty().bind(root.heightProperty());

        root.getChildren().forEach(child -> {
            if (!(child instanceof StackPane && child.getId() != null && child.getId().equals("__overlay__"))) {
                child.setEffect(new GaussianBlur(6));
            }
        });

        SVGPath icon = new SVGPath();
        icon.setContent(iconSvg);
        icon.setFill(Color.web(iconColor));
        icon.setScaleX(2.2);
        icon.setScaleY(2.2);
        StackPane iconCircle = new StackPane(icon);
        iconCircle.setStyle("-fx-background-color: " + iconBg + "; -fx-background-radius: 50%;");
        iconCircle.setPrefSize(56, 56);
        iconCircle.setMinSize(56, 56);
        iconCircle.setMaxSize(56, 56);

        Label lblTitle = new Label(title);
        lblTitle.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #1E293B;");
        lblTitle.setAlignment(Pos.CENTER);

        Label lblSubtitle = new Label(subtitle);
        lblSubtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748B;");
        lblSubtitle.setAlignment(Pos.CENTER);
        lblSubtitle.setWrapText(true);
        lblSubtitle.setMaxWidth(240);

        Button btnClose = new Button("Close");
        btnClose.setStyle("-fx-background-color: " + btnColor + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 9 32;");

        VBox card = new VBox(14, iconCircle, lblTitle, lblSubtitle, btnClose);
        card.setAlignment(Pos.CENTER);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 18; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.22), 24, 0, 0, 6);");
        card.setPadding(new Insets(28, 32, 28, 32));
        card.setMaxWidth(300);
        card.setMaxHeight(Region.USE_PREF_SIZE);

        StackPane overlay = new StackPane(dimLayer, card);
        overlay.setId("__overlay__");
        overlay.setAlignment(Pos.CENTER);
        overlay.prefWidthProperty().bind(root.widthProperty());
        overlay.prefHeightProperty().bind(root.heightProperty());

        root.getChildren().add(overlay);

        Runnable dismiss = () -> {
            root.getChildren().remove(overlay);
            root.getChildren().forEach(child -> child.setEffect(null));
        };
        btnClose.setOnAction(e -> dismiss.run());

        PauseTransition auto = new PauseTransition(Duration.seconds(6));
        auto.setOnFinished(e -> {
            if (root.getChildren().contains(overlay)) dismiss.run();
        });
        auto.play();
    }

    private void showConfirmationPopup(String title, String subtitle, Runnable onConfirm) {
        showConfirmationPopup(title, subtitle, "Confirm", onConfirm);
    }

    private void showConfirmationPopup(String title, String subtitle, String confirmLabel, Runnable onConfirm) {
        StackPane root = getRootStack();
        if (root == null) return;

        Region dimLayer = new Region();
        dimLayer.setStyle("-fx-background-color: rgba(0,0,0,0.45);");
        dimLayer.prefWidthProperty().bind(root.widthProperty());
        dimLayer.prefHeightProperty().bind(root.heightProperty());

        root.getChildren().forEach(child -> {
            if (!(child instanceof StackPane && child.getId() != null && child.getId().equals("__confirm__"))) {
                child.setEffect(new GaussianBlur(6));
            }
        });

        SVGPath warnIcon = new SVGPath();
        warnIcon.setContent(
                "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 " +
                        "10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"
        );
        warnIcon.setFill(Color.web("#D97706"));
        warnIcon.setScaleX(2.2);
        warnIcon.setScaleY(2.2);
        StackPane iconCircle = new StackPane(warnIcon);
        iconCircle.setStyle("-fx-background-color: #FEF3C7; -fx-background-radius: 50%;");
        iconCircle.setPrefSize(56, 56);
        iconCircle.setMinSize(56, 56);
        iconCircle.setMaxSize(56, 56);

        Label lblTitle = new Label(title);
        lblTitle.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #1E293B;");
        lblTitle.setAlignment(Pos.CENTER);

        Label lblSubtitle = new Label(subtitle);
        lblSubtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748B;");
        lblSubtitle.setAlignment(Pos.CENTER);
        lblSubtitle.setWrapText(true);
        lblSubtitle.setMaxWidth(240);

        Button btnCancel = new Button("Cancel");
        btnCancel.setStyle(
                "-fx-background-color: #F1F5F9; " +
                        "-fx-text-fill: #475569; " +
                        "-fx-font-weight: bold; " +
                        "-fx-font-size: 13px; " +
                        "-fx-background-radius: 8; " +
                        "-fx-cursor: hand; " +
                        "-fx-padding: 9 24; " +
                        "-fx-border-color: #E2E8F0; " +
                        "-fx-border-radius: 8;"
        );

        Button btnConfirm = new Button(confirmLabel);
        btnConfirm.setStyle(
                "-fx-background-color: #D97706; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-weight: bold; " +
                        "-fx-font-size: 13px; " +
                        "-fx-background-radius: 8; " +
                        "-fx-cursor: hand; " +
                        "-fx-padding: 9 24;"
        );

        HBox btnRow = new HBox(12, btnCancel, btnConfirm);
        btnRow.setAlignment(Pos.CENTER);

        VBox card = new VBox(14, iconCircle, lblTitle, lblSubtitle, btnRow);
        card.setAlignment(Pos.CENTER);
        card.setStyle(
                "-fx-background-color: white; " +
                        "-fx-background-radius: 18; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.22), 24, 0, 0, 6);"
        );
        card.setPadding(new Insets(28, 32, 28, 32));
        card.setMaxWidth(320);
        card.setMaxHeight(Region.USE_PREF_SIZE);

        StackPane overlay = new StackPane(dimLayer, card);
        overlay.setId("__confirm__");
        overlay.setAlignment(Pos.CENTER);
        overlay.prefWidthProperty().bind(root.widthProperty());
        overlay.prefHeightProperty().bind(root.heightProperty());

        root.getChildren().add(overlay);

        Runnable dismiss = () -> {
            root.getChildren().remove(overlay);
            root.getChildren().forEach(child -> child.setEffect(null));
        };

        btnCancel.setOnAction(e -> dismiss.run());

        btnConfirm.setOnAction(e -> {
            dismiss.run();
            onConfirm.run();
        });
    }

    private void createUserForStudent(Connection conn, String studentCode, String fullName) throws SQLException {
        String checkSql = "SELECT COUNT(*) FROM users WHERE username = ?";
        try (PreparedStatement chk = conn.prepareStatement(checkSql)) {
            chk.setString(1, studentCode);
            try (ResultSet rs = chk.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) return;
            }
        }

        String rawPassword = "NEW" + studentCode;
        String passwordHash = BCrypt.hashpw(rawPassword, BCrypt.gensalt());

        String insertSql =
                "INSERT INTO users (username, full_name, role, password_hash, student_ref_id, status, first_login) " +
                        "VALUES (?, ?, 'STUDENT', ?, ?, 'ACTIVE', TRUE)";
        try (PreparedStatement pst = conn.prepareStatement(insertSql)) {
            pst.setString(1, studentCode);
            pst.setString(2, fullName);
            pst.setString(3, passwordHash);
            pst.setString(4, studentCode);
            pst.executeUpdate();
        }
    }

    private void setupOwnerLiveSearch() {
        txtSearchDeviceOwner.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredOwners.setPredicate(student -> {
                if (newValue == null || newValue.trim().isEmpty()) return true;
                String lowerFilter = newValue.toLowerCase().trim();
                boolean matchesCode = student.getStudentCode() != null && student.getStudentCode().toLowerCase().contains(lowerFilter);
                boolean matchesName = student.getFullName() != null && student.getFullName().toLowerCase().contains(lowerFilter);
                return matchesCode || matchesName;
            });
        });
    }

    @FXML public void showStudentsTab() {
        paneStudentsView.setVisible(true);
        paneDevicesView.setVisible(false);
        paneStudentProfileView.setVisible(false);
        paneDeviceProfileView.setVisible(false);
        handleCloseModal();
    }

    @FXML public void showDevicesTab() {
        paneStudentsView.setVisible(false);
        paneDevicesView.setVisible(true);
        paneStudentProfileView.setVisible(false);
        paneDeviceProfileView.setVisible(false);
        handleCloseModal();
    }

    @FXML public void handleOpenAddStudentForm() {
        currentlySelectedStudent = null;
        clearStudentFields();
        clearStudentInlineErrors();
        paneStudentFormModal.setVisible(true);
        paneStudentFormModal.toFront();
    }

    @FXML public void handleOpenAddDeviceForm() {
        currentlySelectedDevice = null;
        clearDeviceFields();
        clearDeviceInlineErrors();
        txtSearchDeviceOwner.clear();
        filteredOwners.setPredicate(p -> true);
        cmbDeviceOwner.setItems(filteredOwners);
        paneDeviceFormModal.setVisible(true);
        paneDeviceFormModal.toFront();
    }

    @FXML public void handleCloseModal() {
        paneStudentFormModal.setVisible(false);
        paneDeviceFormModal.setVisible(false);
        paneMassAddModal.setVisible(false);
        paneMassAddTimerModal.setVisible(false);
    }

    private void configureTableColumns() {
        colStudentCode.setCellValueFactory(new PropertyValueFactory<>("studentCode"));
        colStudentName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colStudentSection.setCellValueFactory(new PropertyValueFactory<>("course")); // "course" property holds the section value
        colStudentDevicesCount.setCellValueFactory(new PropertyValueFactory<>("devicesCount"));
        colStudentStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        colDeviceSerial.setCellValueFactory(new PropertyValueFactory<>("serialNumber"));
        colDeviceBrand.setCellValueFactory(new PropertyValueFactory<>("brand"));
        colDeviceModel.setCellValueFactory(new PropertyValueFactory<>("model"));
        colDeviceType.setCellValueFactory(new PropertyValueFactory<>("deviceType"));
        colDeviceOwner.setCellValueFactory(new PropertyValueFactory<>("ownerName"));
        colDeviceState.setCellValueFactory(new PropertyValueFactory<>("status"));
        colDeviceLocation.setCellValueFactory(new PropertyValueFactory<>("currentLocation"));

        setupPillCellFactories();
        setupActionButtonsCellFactories();
        configureProfileLogColumns();
        centerAllColumnHeaders();
    }

    @SuppressWarnings("unchecked")
    private void centerAllColumnHeaders() {
        javafx.application.Platform.runLater(() -> {
            TableColumn<?, ?>[] cols = new TableColumn<?, ?>[] {
                    colStudentCode, colStudentName, colStudentSection,
                    colStudentDevicesCount, colStudentStatus, colStudentActions,
                    colDeviceSerial, colDeviceBrand, colDeviceModel, colDeviceType,
                    colDeviceOwner, colDeviceState, colDeviceLocation, colDeviceActions,
                    colSLogDevice, colSLogTimeIn, colSLogTimeOut, colSLogStatus, colSLogDetails,
                    colDLogTimeIn, colDLogTimeOut, colDLogStatus, colDLogDetails
            };
            for (TableColumn<?, ?> col : cols) {
                if (col == null) continue;
                javafx.scene.Node header = col.getStyleableNode();
                if (header == null) continue;
                header.lookupAll(".label").forEach(node -> {
                    if (node instanceof Label lbl) {
                        lbl.setAlignment(Pos.CENTER);
                        lbl.setMaxWidth(Double.MAX_VALUE);
                    }
                });
            }
        });
    }

    private void setupPillCellFactories() {
        colStudentStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label lbl = new Label(item.toUpperCase());
                    lbl.setAlignment(Pos.CENTER);
                    lbl.setPrefWidth(78);
                    if ("ACTIVE".equalsIgnoreCase(item)) {
                        lbl.setStyle("-fx-background-color: #DCFCE7; -fx-text-fill: #15803D; -fx-padding: 6 12; -fx-background-radius: 50; -fx-font-weight: bold; -fx-font-size: 12px;");
                    } else {
                        lbl.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #B91C1C; -fx-padding: 6 12; -fx-background-radius: 50; -fx-font-weight: bold; -fx-font-size: 12px;");
                    }
                    setGraphic(new StackPane(lbl));
                }
            }
        });

        colDeviceState.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label lbl = new Label(item.toUpperCase());
                    lbl.setAlignment(Pos.CENTER);
                    lbl.setPrefWidth(78);
                    if ("INSIDE".equalsIgnoreCase(item) || "ACTIVE".equalsIgnoreCase(item)) {
                        lbl.setStyle("-fx-background-color: #DCFCE7; -fx-text-fill: #15803D; -fx-padding: 6 12; -fx-background-radius: 50; -fx-font-weight: bold; -fx-font-size: 12px;");
                    } else {
                        lbl.setStyle("-fx-background-color: #DBEAFE; -fx-text-fill: #1E40AF; -fx-padding: 6 12; -fx-background-radius: 50; -fx-font-weight: bold; -fx-font-size: 12px;");
                    }
                    setGraphic(new StackPane(lbl));
                }
            }
        });

        colDeviceLocation.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    String label;
                    String style;
                    if ("IN".equalsIgnoreCase(item)) {
                        label = "INSIDE";
                        style = "-fx-background-color: #DCFCE7; -fx-text-fill: #15803D; -fx-padding: 6 12; -fx-background-radius: 50; -fx-font-weight: bold; -fx-font-size: 12px;";
                    } else if ("OUT".equalsIgnoreCase(item)) {
                        label = "OUTSIDE";
                        style = "-fx-background-color: #DBEAFE; -fx-text-fill: #1E40AF; -fx-padding: 6 12; -fx-background-radius: 50; -fx-font-weight: bold; -fx-font-size: 12px;";
                    } else {
                        label = "UNKNOWN";
                        style = "-fx-background-color: #F1F5F9; -fx-text-fill: #64748B; -fx-padding: 6 12; -fx-background-radius: 50; -fx-font-weight: bold; -fx-font-size: 12px;";
                    }
                    Label lbl = new Label(label);
                    lbl.setAlignment(Pos.CENTER);
                    lbl.setPrefWidth(86);
                    lbl.setStyle(style);
                    setGraphic(new StackPane(lbl));
                }
            }
        });
    }

    private boolean isOfficer() {
        return "OFFICER".equalsIgnoreCase(currentUserRole);
    }

    private boolean deviceHasOpenLog(int deviceId) {
        String sql = "SELECT COUNT(*) FROM device_logs dl_in " +
                "WHERE dl_in.device_id = ? AND dl_in.direction = 'IN' " +
                "AND NOT EXISTS (" +
                "    SELECT 1 FROM device_logs dl_out " +
                "    WHERE dl_out.device_id = dl_in.device_id " +
                "      AND dl_out.direction = 'OUT' " +
                "      AND dl_out.log_time > dl_in.log_time" +
                ")";
        try (Connection conn = DBConnection.connect();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, deviceId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    private void setupActionButtonsCellFactories() {

        colStudentActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnView = new Button("👁 View");
            private final Button btnEdit = new Button("📝 Edit");
            private final HBox   buttons = new HBox(8, btnView, btnEdit);
            {
                buttons.setAlignment(Pos.CENTER);

                btnView.setStyle("-fx-background-color: #F1F5F9; -fx-cursor: hand; -fx-text-fill: #475569; -fx-font-size: 12px; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 6 12; -fx-border-color: #E2E8F0; -fx-border-radius: 6;");
                btnEdit.setStyle("-fx-background-color: #FEF3C7; -fx-cursor: hand; -fx-text-fill: #D97706; -fx-font-size: 12px; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 6 12; -fx-border-color: #FCD34D; -fx-border-radius: 6;");

                btnView.setOnAction(e -> {
                    Student selected = getTableView().getItems().get(getIndex());
                    handleViewStudentProfile(selected);
                });

                btnEdit.setOnAction(e -> {
                    Student selected = getTableView().getItems().get(getIndex());
                    currentlySelectedStudent = selected;
                    clearStudentInlineErrors();
                    txtStudentCode.setText(currentlySelectedStudent.getStudentCode());
                    txtStudentName.setText(currentlySelectedStudent.getFullName());
                    txtSection.setText(currentlySelectedStudent.getCourse()); // getCourse() returns section
                    txtEmail.setText(currentlySelectedStudent.getContactNumber()); // getContactNumber() returns email
                    paneStudentFormModal.setVisible(true);
                    paneStudentFormModal.toFront();
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                if (isOfficer()) {
                    setGraphic(null);
                } else {
                    setGraphic(buttons);
                }
            }
        });

        colDeviceActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnView = new Button("👁 View");
            private final Button btnEdit = new Button("📝 Edit");
            private final HBox   buttons = new HBox(8, btnView, btnEdit);
            {
                buttons.setAlignment(Pos.CENTER);

                btnView.setStyle("-fx-background-color: #F1F5F9; -fx-cursor: hand; -fx-text-fill: #475569; -fx-font-size: 12px; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 6 12; -fx-border-color: #E2E8F0; -fx-border-radius: 6;");
                btnEdit.setStyle("-fx-background-color: #FEF3C7; -fx-cursor: hand; -fx-text-fill: #D97706; -fx-font-size: 12px; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 6 12; -fx-border-color: #FCD34D; -fx-border-radius: 6;");

                btnView.setOnAction(e -> {
                    Device selected = getTableView().getItems().get(getIndex());
                    handleViewDeviceProfile(selected);
                });

                btnEdit.setOnAction(e -> {
                    currentlySelectedDevice = getTableView().getItems().get(getIndex());
                    clearDeviceInlineErrors();
                    txtSearchDeviceOwner.clear();
                    filteredOwners.setPredicate(p -> true);
                    cmbDeviceOwner.setItems(filteredOwners);
                    txtSerialNumber.setText(currentlySelectedDevice.getSerialNumber());
                    cmbDeviceType.setValue(currentlySelectedDevice.getDeviceType().toUpperCase());
                    txtBrand.setText(currentlySelectedDevice.getBrand());
                    txtModel.setText(currentlySelectedDevice.getModel());
                    for (Student s : studentMasterList) {
                        if (s.getStudentId() == currentlySelectedDevice.getOwnerId()) {
                            cmbDeviceOwner.setValue(s);
                            break;
                        }
                    }
                    paneDeviceFormModal.setVisible(true);
                    paneDeviceFormModal.toFront();
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                if (isOfficer()) {
                    setGraphic(null);
                } else {
                    setGraphic(buttons);
                }
            }
        });
    }

    private void setupSearchAndFilters() {
        txtSearchStudent.textProperty().addListener((obs, oldVal, newVal) -> applyStudentFilter());
        txtSearchDevice.textProperty().addListener((obs, oldVal, newVal) -> applyDeviceFilter());
        tblStudents.setItems(filteredStudents);
        tblDevices.setItems(filteredDevices);
    }

    private void applyStudentFilter() {
        filteredStudents.setPredicate(student -> {
            boolean matchesStatus = student.getStatus().equalsIgnoreCase(currentStudentStatusFilter);
            if (!matchesStatus) return false;
            String search = txtSearchStudent.getText();
            if (search == null || search.trim().isEmpty()) return true;
            String lower = search.toLowerCase();
            return student.getStudentCode().toLowerCase().contains(lower) ||
                    student.getFullName().toLowerCase().contains(lower) ||
                    student.getCourse().toLowerCase().contains(lower);
        });
    }

    private void applyDeviceFilter() {
        filteredDevices.setPredicate(device -> {
            boolean matchesStatus = device.getStatus().equalsIgnoreCase(currentDeviceStatusFilter) ||
                    ("ACTIVE".equalsIgnoreCase(currentDeviceStatusFilter) && "INSIDE".equalsIgnoreCase(device.getStatus()));
            if (!matchesStatus) return false;
            boolean matchesLocation = "ALL".equalsIgnoreCase(currentDeviceLocationFilter) ||
                    (device.getCurrentLocation() != null && device.getCurrentLocation().equalsIgnoreCase(currentDeviceLocationFilter));
            if (!matchesLocation) return false;
            String search = txtSearchDevice.getText();
            if (search == null || search.trim().isEmpty()) return true;
            String lower = search.toLowerCase();
            return device.getSerialNumber().toLowerCase().contains(lower) ||
                    device.getBrand().toLowerCase().contains(lower) ||
                    device.getModel().toLowerCase().contains(lower) ||
                    device.getOwnerName().toLowerCase().contains(lower);
        });
    }

    @FXML public void handleStudentFilterActive() {
        currentStudentStatusFilter = "ACTIVE";
        applyStudentFilter();
        updateFilterButtonStyles();
    }

    @FXML public void handleStudentFilterInactive() {
        currentStudentStatusFilter = "INACTIVE";
        applyStudentFilter();
        updateFilterButtonStyles();
    }

    @FXML public void handleDeviceFilterActive() {
        currentDeviceStatusFilter = "ACTIVE";
        applyDeviceFilter();
        updateFilterButtonStyles();
    }

    @FXML public void handleDeviceFilterInactive() {
        currentDeviceStatusFilter = "INACTIVE";
        applyDeviceFilter();
        updateFilterButtonStyles();
    }

    @FXML public void handleDeviceFilterInside() {
        currentDeviceLocationFilter = "IN".equalsIgnoreCase(currentDeviceLocationFilter) ? "ALL" : "IN";
        applyDeviceFilter();
        updateFilterButtonStyles();
    }

    @FXML public void handleDeviceFilterOutside() {
        currentDeviceLocationFilter = "OUT".equalsIgnoreCase(currentDeviceLocationFilter) ? "ALL" : "OUT";
        applyDeviceFilter();
        updateFilterButtonStyles();
    }

    private void updateFilterButtonStyles() {
        if ("ACTIVE".equalsIgnoreCase(currentStudentStatusFilter)) {
            btnStudentFilterActive.setStyle("-fx-background-color: #E6F4EA; -fx-text-fill: #137333; -fx-border-color: #A3E635; -fx-background-radius: 8; -fx-border-radius: 8; -fx-font-weight: bold; -fx-cursor: hand;");
            btnStudentFilterInactive.setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #475569; -fx-border-color: #E2E8F0; -fx-background-radius: 8; -fx-border-radius: 8; -fx-cursor: hand;");
        } else {
            btnStudentFilterActive.setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #475569; -fx-border-color: #E2E8F0; -fx-background-radius: 8; -fx-border-radius: 8; -fx-cursor: hand;");
            btnStudentFilterInactive.setStyle("-fx-background-color: #FCE8E6; -fx-text-fill: #C5221F; -fx-border-color: #FCA5A5; -fx-background-radius: 8; -fx-border-radius: 8; -fx-font-weight: bold; -fx-cursor: hand;");
        }
        if ("ACTIVE".equalsIgnoreCase(currentDeviceStatusFilter)) {
            btnDeviceFilterActive.setStyle("-fx-background-color: #E6F4EA; -fx-text-fill: #137333; -fx-border-color: #A3E635; -fx-background-radius: 8; -fx-border-radius: 8; -fx-font-weight: bold; -fx-cursor: hand;");
            btnDeviceFilterInactive.setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #475569; -fx-border-color: #E2E8F0; -fx-background-radius: 8; -fx-border-radius: 8; -fx-cursor: hand;");
        } else {
            btnDeviceFilterActive.setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #475569; -fx-border-color: #E2E8F0; -fx-background-radius: 8; -fx-border-radius: 8; -fx-cursor: hand;");
            btnDeviceFilterInactive.setStyle("-fx-background-color: #FCE8E6; -fx-text-fill: #C5221F; -fx-border-color: #FCA5A5; -fx-background-radius: 8; -fx-border-radius: 8; -fx-font-weight: bold; -fx-cursor: hand;");
        }
        if ("IN".equalsIgnoreCase(currentDeviceLocationFilter)) {
            btnDeviceFilterInside.setStyle("-fx-background-color: #E6F4EA; -fx-text-fill: #137333; -fx-border-color: #A3E635; -fx-background-radius: 8; -fx-border-radius: 8; -fx-font-weight: bold; -fx-cursor: hand;");
            btnDeviceFilterOutside.setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #475569; -fx-border-color: #E2E8F0; -fx-background-radius: 8; -fx-border-radius: 8; -fx-cursor: hand;");
        } else if ("OUT".equalsIgnoreCase(currentDeviceLocationFilter)) {
            btnDeviceFilterInside.setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #475569; -fx-border-color: #E2E8F0; -fx-background-radius: 8; -fx-border-radius: 8; -fx-cursor: hand;");
            btnDeviceFilterOutside.setStyle("-fx-background-color: #DBEAFE; -fx-text-fill: #1E40AF; -fx-border-color: #93C5FD; -fx-background-radius: 8; -fx-border-radius: 8; -fx-font-weight: bold; -fx-cursor: hand;");
        } else {
            btnDeviceFilterInside.setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #475569; -fx-border-color: #E2E8F0; -fx-background-radius: 8; -fx-border-radius: 8; -fx-cursor: hand;");
            btnDeviceFilterOutside.setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #475569; -fx-border-color: #E2E8F0; -fx-background-radius: 8; -fx-border-radius: 8; -fx-cursor: hand;");
        }
    }

    private void loadStudentsFromDatabase() {
        studentMasterList.clear();
        String sql = "SELECT s.student_id, s.student_code, s.full_name, s.section, s.email, s.status, " +
                "(SELECT COUNT(*) FROM devices d WHERE d.owner_id = s.student_id) as dev_count " +
                "FROM students s ORDER BY s.student_code ASC";
        try (Connection conn = DBConnection.connect();
             PreparedStatement pst = conn.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
                Student s = new Student(
                        rs.getInt("student_id"),
                        rs.getString("student_code"),
                        rs.getString("full_name"),
                        rs.getString("section"),   // maps to course/section field
                        null,                      // year_level removed
                        rs.getString("email"),     // email replaces contact_number
                        rs.getString("status")
                );
                s.setDevicesCount(rs.getInt("dev_count"));
                studentMasterList.add(s);
            }
            applyStudentFilter();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void loadDevicesFromDatabase() {
        deviceMasterList.clear();
        String sql = "SELECT d.*, s.full_name as owner_name FROM devices d " +
                "LEFT JOIN students s ON d.owner_id = s.student_id ORDER BY d.device_id DESC";
        try (Connection conn = DBConnection.connect();
             PreparedStatement pst = conn.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
                Device d = new Device(
                        rs.getInt("device_id"),
                        rs.getString("serial_number"),
                        rs.getString("brand"),
                        rs.getString("model"),
                        rs.getString("device_type"),
                        rs.getInt("owner_id"),
                        rs.getString("status")
                );
                d.setOwnerName(rs.getString("owner_name"));
                d.setCurrentLocation(rs.getString("current_location"));
                deviceMasterList.add(d);
            }
            applyDeviceFilter();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @FXML
    public void handleOpenMassAddForm() {
        selectedCsvFile = null;
        lblSelectedFile.setText("No file selected");
        btnConfirmMassAdd.setDisable(true);
        paneMassAddModal.setVisible(true);
        paneMassAddModal.toFront();
    }

    @FXML
    public void handleUploadCsv() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select CSV File for Mass Addition");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));

        selectedCsvFile = fc.showOpenDialog(paneStudentsView.getScene().getWindow());
        if (selectedCsvFile != null) {
            lblSelectedFile.setText(selectedCsvFile.getName());
            btnConfirmMassAdd.setDisable(false);
        } else {
            lblSelectedFile.setText("No file selected");
            btnConfirmMassAdd.setDisable(true);
        }
    }

    @FXML
    public void handleCancelMassAdd() {
        if (massAddTimeline != null) {
            massAddTimeline.stop();
        }
        handleCloseModal();
    }

    @FXML
    public void handleConfirmMassAdd() {
        if (selectedCsvFile == null) return;

        paneMassAddModal.setVisible(false);
        paneMassAddTimerModal.setVisible(true);
        paneMassAddTimerModal.toFront();

        final int[] secondsLeft = {10};
        lblCountdown.setText(String.valueOf(secondsLeft[0]));

        if (massAddTimeline != null) massAddTimeline.stop();

        massAddTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            secondsLeft[0]--;
            lblCountdown.setText(String.valueOf(secondsLeft[0]));
            if (secondsLeft[0] <= 0) {
                massAddTimeline.stop();
                processMassAddition();
            }
        }));
        massAddTimeline.setCycleCount(10);
        massAddTimeline.play();
    }

    private void processMassAddition() {
        handleCloseModal();
        if (selectedCsvFile == null) return;

        int successCount = 0;
        int skipCount = 0;

        String insertSql = "INSERT INTO students (student_code, full_name, section, email) " +
                "VALUES (?, ?, ?, ?)";
        String checkSql = "SELECT COUNT(*) FROM students WHERE student_code = ?";

        try (Connection conn = DBConnection.connect();
             BufferedReader br = new BufferedReader(new FileReader(selectedCsvFile))) {

            String line;
            boolean isFirstLine = true;

            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                if (isFirstLine && line.toLowerCase().contains("code")) {
                    isFirstLine = false;
                    continue;
                }
                isFirstLine = false;

                String[] data = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                if (data.length < 3) continue;

                String code    = data[0].replace("\"", "").trim();
                String name    = data[1].replace("\"", "").trim();
                String section = data[2].replace("\"", "").trim();
                String email   = data.length >= 4 ? data[3].replace("\"", "").trim() : "";

                try (PreparedStatement checkPst = conn.prepareStatement(checkSql)) {
                    checkPst.setString(1, code);
                    try (ResultSet rs = checkPst.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) {
                            skipCount++;
                            continue;
                        }
                    }
                }

                try (PreparedStatement pst = conn.prepareStatement(insertSql)) {
                    pst.setString(1, code);
                    pst.setString(2, name);
                    pst.setString(3, section);
                    pst.setString(4, email);
                    pst.executeUpdate();
                }

                successCount++;
            }

            loadStudentsFromDatabase();
            showOverlayPopup("Mass Addition Complete", successCount + " records added.\n" + skipCount + " duplicates skipped.");

        } catch (Exception e) {
            e.printStackTrace();
            showOverlayPopup("Error", "Failed to process the CSV file.");
        }
    }

    @FXML
    public void handleSaveStudent() {
        clearStudentInlineErrors();

        String code    = txtStudentCode.getText()    == null ? "" : txtStudentCode.getText().trim();
        String name    = txtStudentName.getText()    == null ? "" : txtStudentName.getText().trim();
        String section = txtSection.getText() == null ? "" : txtSection.getText().trim();
        String email   = txtEmail.getText()   == null ? "" : txtEmail.getText().trim();

        boolean hasError = false;
        if (code.isEmpty())    { showInlineError(lblErrorStudentCode, "Student Code is required.");   hasError = true; }
        if (name.isEmpty())    { showInlineError(lblErrorStudentName, "Full Name is required.");      hasError = true; }
        if (section.isEmpty()) { showInlineError(lblErrorSection,     "Section is required.");        hasError = true; }
        if (email.isEmpty())   { showInlineError(lblErrorEmail,       "Email is required.");          hasError = true; }
        if (hasError) return;

        try (Connection conn = DBConnection.connect()) {

            String checkSql = currentlySelectedStudent == null
                    ? "SELECT COUNT(*) FROM students WHERE student_code = ?"
                    : "SELECT COUNT(*) FROM students WHERE student_code = ? AND student_id != ?";
            try (PreparedStatement checkPst = conn.prepareStatement(checkSql)) {
                checkPst.setString(1, code);
                if (currentlySelectedStudent != null) checkPst.setInt(2, currentlySelectedStudent.getStudentId());
                try (ResultSet rs = checkPst.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        showInlineError(lblErrorStudentCode, "Student Code '" + code + "' is already registered.");
                        return;
                    }
                }
            }

            boolean isAdding = (currentlySelectedStudent == null);
            int targetStudentId = -1;

            if (isAdding) {
                String sql = "INSERT INTO students (student_code, full_name, section, email) " +
                        "VALUES (?, ?, ?, ?) RETURNING student_id";
                try (PreparedStatement pst = conn.prepareStatement(sql)) {
                    pst.setString(1, code);
                    pst.setString(2, name);
                    pst.setString(3, section);
                    pst.setString(4, email);
                    try (ResultSet rs = pst.executeQuery()) {
                        if (rs.next()) targetStudentId = rs.getInt(1);
                    }
                }

            } else {
                targetStudentId = currentlySelectedStudent.getStudentId();
                String sql = "UPDATE students SET student_code=?, full_name=?, section=?, email=? " +
                        "WHERE student_id=?";
                try (PreparedStatement pst = conn.prepareStatement(sql)) {
                    pst.setString(1, code);
                    pst.setString(2, name);
                    pst.setString(3, section);
                    pst.setString(4, email);
                    pst.setInt(5, targetStudentId);
                    pst.executeUpdate();
                }
            }

            loadStudentsFromDatabase();
            handleCloseModal();

            final int finalTargetId = targetStudentId;
            for (Student s : studentMasterList) {
                if (s.getStudentId() == finalTargetId) {
                    handleViewStudentProfile(s);
                    break;
                }
            }

            if (isAdding) {
                showOverlayPopup("Success", "New student has been\nsuccessfully added.");
            } else {
                showOverlayPopup("Success", "Student record has been\nsuccessfully updated.");
            }

        } catch (SQLException e) { e.printStackTrace(); }
    }

    @FXML
    public void handleToggleStudentStatus() {
        if (currentlySelectedStudent == null) return;

        String targetStatus = "ACTIVE".equalsIgnoreCase(
                currentlySelectedStudent.getStatus()) ? "INACTIVE" : "ACTIVE";
        String actionType = "INACTIVE".equals(targetStatus)
                ? "RECORD_DEACTIVATED" : "RECORD_REACTIVATED";

        String confirmTitle = "INACTIVE".equals(targetStatus)
                ? "Deactivate Student?" : "Reactivate Student?";
        String confirmMsg = "INACTIVE".equals(targetStatus)
                ? "Are you sure you want to deactivate\n" + currentlySelectedStudent.getFullName() + "?"
                : "Are you sure you want to reactivate\n" + currentlySelectedStudent.getFullName() + "?";

        showConfirmationPopup(confirmTitle, confirmMsg, () -> {
            String studentSql = "UPDATE students SET status = ? WHERE student_id = ?";

            try (Connection conn = DBConnection.connect()) {
                try (PreparedStatement pst = conn.prepareStatement(studentSql)) {
                    pst.setString(1, targetStatus);
                    pst.setInt(2, currentlySelectedStudent.getStudentId());
                    pst.executeUpdate();
                }

                AuditLogger.log(conn,
                        UserSession.getInstance().getUserId(),
                        actionType,
                        "Student",
                        currentlySelectedStudent.getStudentId(),
                        "{\"student_code\": \"" +
                                currentlySelectedStudent.getStudentCode() + "\"}"
                );

                loadStudentsFromDatabase();
                handleCloseModal();
                showOverlayPopup("Success", "Student status updated to " + targetStatus + ".");
            } catch (SQLException e) { e.printStackTrace(); }
        });
    }

    @FXML
    public void handleSaveDevice() {
        clearDeviceInlineErrors();

        String serial  = txtSerialNumber.getText() == null ? "" : txtSerialNumber.getText().trim();
        String type    = cmbDeviceType.getValue();
        String brand   = txtBrand.getText()        == null ? "" : txtBrand.getText().trim();
        String model   = txtModel.getText()        == null ? "" : txtModel.getText().trim();
        Student owner  = cmbDeviceOwner.getValue();

        boolean hasError = false;
        if (serial.isEmpty()) { showInlineError(lblErrorSerialNumber, "Serial Number is required.");                       hasError = true; }
        if (type == null)     { showInlineError(lblErrorDeviceType,   "Select a device type classification.");             hasError = true; }
        if (brand.isEmpty())  { showInlineError(lblErrorBrand,        "Hardware brand name is required.");                 hasError = true; }
        if (model.isEmpty())  { showInlineError(lblErrorModel,        "Hardware model reference is required.");            hasError = true; }
        if (owner == null)    { showInlineError(lblErrorDeviceOwner,  "Please assign an owner to this hardware device."); hasError = true; }
        if (hasError) return;

        try (Connection conn = DBConnection.connect()) {

            String checkSql = currentlySelectedDevice == null
                    ? "SELECT COUNT(*) FROM devices WHERE serial_number = ?"
                    : "SELECT COUNT(*) FROM devices WHERE serial_number = ? AND device_id != ?";
            try (PreparedStatement checkPst = conn.prepareStatement(checkSql)) {
                checkPst.setString(1, serial);
                if (currentlySelectedDevice != null) checkPst.setInt(2, currentlySelectedDevice.getDeviceId());
                try (ResultSet rs = checkPst.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        showInlineError(lblErrorSerialNumber, "Serial number '" + serial + "' is already registered.");
                        return;
                    }
                }
            }

            boolean isAdding = (currentlySelectedDevice == null);
            int targetDeviceId = -1;

            if (isAdding) {
                String sql = "INSERT INTO devices (serial_number, brand, model, device_type, owner_id, status) " +
                        "VALUES (?, ?, ?, ?, ?, 'ACTIVE') RETURNING device_id";
                try (PreparedStatement pst = conn.prepareStatement(sql)) {
                    pst.setString(1, serial);
                    pst.setString(2, brand);
                    pst.setString(3, model);
                    pst.setString(4, type);
                    pst.setInt(5, owner.getStudentId());
                    try (ResultSet rs = pst.executeQuery()) {
                        if (rs.next()) targetDeviceId = rs.getInt(1);
                    }
                }
            } else {
                targetDeviceId = currentlySelectedDevice.getDeviceId();
                String sql = "UPDATE devices SET serial_number=?, brand=?, model=?, device_type=?, owner_id=? WHERE device_id=?";
                try (PreparedStatement pst = conn.prepareStatement(sql)) {
                    pst.setString(1, serial);
                    pst.setString(2, brand);
                    pst.setString(3, model);
                    pst.setString(4, type);
                    pst.setInt(5, owner.getStudentId());
                    pst.setInt(6, targetDeviceId);
                    pst.executeUpdate();
                }
            }

            loadDevicesFromDatabase();
            handleCloseModal();

            final int finalTargetId = targetDeviceId;
            for (Device d : deviceMasterList) {
                if (d.getDeviceId() == finalTargetId) {
                    handleViewDeviceProfile(d);
                    break;
                }
            }

            if (isAdding) {
                showOverlayPopup("Success", "New device has been\nsuccessfully registered.");
            } else {
                showOverlayPopup("Success", "Device record has been\nsuccessfully updated.");
            }

        } catch (SQLException e) { e.printStackTrace(); }
    }

    @FXML
    public void handleToggleDeviceStatus() {
        if (currentlySelectedDevice == null) return;
        if ("ACTIVE".equalsIgnoreCase(currentlySelectedDevice.getStatus()) && deviceHasOpenLog(currentlySelectedDevice.getDeviceId())) {
            showInlineError(lblErrorDeviceOwner, "Cannot deactivate: device has an open check-in log.");
            return;
        }
        String targetStatus = "ACTIVE".equalsIgnoreCase(currentlySelectedDevice.getStatus()) ? "INACTIVE" : "ACTIVE";

        String confirmTitle = "INACTIVE".equals(targetStatus)
                ? "Deactivate Device?" : "Reactivate Device?";
        String confirmMsg = "INACTIVE".equals(targetStatus)
                ? "Are you sure you want to deactivate\n" + currentlySelectedDevice.getSerialNumber() + "?"
                : "Are you sure you want to reactivate\n" + currentlySelectedDevice.getSerialNumber() + "?";

        showConfirmationPopup(confirmTitle, confirmMsg, () -> {
            String sql = "UPDATE devices SET status = ? WHERE device_id = ?";
            try (Connection conn = DBConnection.connect();
                 PreparedStatement pst = conn.prepareStatement(sql)) {
                pst.setString(1, targetStatus);
                pst.setInt(2, currentlySelectedDevice.getDeviceId());
                pst.executeUpdate();
                loadDevicesFromDatabase();
                handleCloseModal();
                showOverlayPopup("Success", "Device status updated to " + targetStatus + ".");
            } catch (SQLException e) { e.printStackTrace(); }
        });
    }

    private void showInlineError(Label errorLabel, String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        }
    }

    private void clearStudentInlineErrors() {
        for (Label lbl : new Label[]{lblErrorStudentCode, lblErrorStudentName, lblErrorSection, lblErrorEmail}) {
            if (lbl != null) { lbl.setVisible(false); lbl.setManaged(false); }
        }
    }

    private void clearDeviceInlineErrors() {
        for (Label lbl : new Label[]{lblErrorSerialNumber, lblErrorDeviceType, lblErrorBrand, lblErrorModel, lblErrorDeviceOwner}) {
            if (lbl != null) { lbl.setVisible(false); lbl.setManaged(false); }
        }
    }

    private void clearStudentFields() {
        txtStudentCode.clear(); txtStudentName.clear(); txtSection.clear();
        txtEmail.clear();
    }

    private void clearDeviceFields() {
        txtSerialNumber.clear(); cmbDeviceType.setValue(null);
        txtBrand.clear(); txtModel.clear(); cmbDeviceOwner.setValue(null);
    }

    private void handleViewStudentProfile(Student student) {
        currentProfileStudent = student;

        lblProfileStudentCode.setText(student.getStudentCode());
        lblProfileStudentName.setText(student.getFullName());
        lblProfileStudentSection.setText(student.getCourse());
        lblProfileStudentEmail.setText(
                student.getContactNumber() == null || student.getContactNumber().isEmpty() ? "—" : student.getContactNumber());

        if ("ACTIVE".equalsIgnoreCase(student.getStatus())) {
            lblProfileStudentStatus.setText("ACTIVE");
            lblProfileStudentStatus.setStyle("-fx-background-color: #DCFCE7; -fx-text-fill: #15803D; -fx-padding: 6 14; -fx-background-radius: 50; -fx-font-weight: bold; -fx-font-size: 12px;");
        } else {
            lblProfileStudentStatus.setText("INACTIVE");
            lblProfileStudentStatus.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #B91C1C; -fx-padding: 6 14; -fx-background-radius: 50; -fx-font-weight: bold; -fx-font-size: 12px;");
        }

        loadStudentActivityLog(student.getStudentId());

        tblStudentActivityLog.setVisible(false);
        tblStudentActivityLog.setManaged(false);
        btnToggleStudentLog.setText("View Full Activity Log for this Student");

        paneStudentsView.setVisible(false);
        paneDevicesView.setVisible(false);
        paneDeviceProfileView.setVisible(false);
        paneStudentProfileView.setVisible(true);
        handleCloseModal();
    }

    @FXML public void handleBackFromStudentProfile() {
        paneStudentProfileView.setVisible(false);
        paneStudentsView.setVisible(true);
    }

    @FXML
    public void handleToggleStudentActivityLog() {
        boolean nowVisible = !tblStudentActivityLog.isVisible();
        tblStudentActivityLog.setVisible(nowVisible);
        tblStudentActivityLog.setManaged(nowVisible);
        btnToggleStudentLog.setText(nowVisible ? "Hide Activity Log" : "View Full Activity Log for this Student");
    }

    @FXML
    public void handleEditStudentFromProfile() {
        if (currentProfileStudent == null) return;
        currentlySelectedStudent = currentProfileStudent;
        clearStudentInlineErrors();
        txtStudentCode.setText(currentlySelectedStudent.getStudentCode());
        txtStudentName.setText(currentlySelectedStudent.getFullName());
        txtSection.setText(currentlySelectedStudent.getCourse()); // section
        txtEmail.setText(currentlySelectedStudent.getContactNumber()); // email
        paneStudentFormModal.setVisible(true);
        paneStudentFormModal.toFront();
    }

    private void handleViewDeviceProfile(Device device) {
        currentProfileDevice = device;

        lblProfileDeviceSerial.setText(device.getSerialNumber());
        lblProfileDeviceType.setText(device.getDeviceType());
        lblProfileDeviceBrand.setText(device.getBrand());
        lblProfileDeviceModel.setText(device.getModel());
        lblProfileDeviceOwner.setText(device.getOwnerName() == null ? "—" : device.getOwnerName());

        loadDeviceActivityLog(device.getDeviceId());

        paneStudentsView.setVisible(false);
        paneDevicesView.setVisible(false);
        paneStudentProfileView.setVisible(false);
        paneDeviceProfileView.setVisible(true);
        handleCloseModal();
    }

    @FXML public void handleBackFromDeviceProfile() {
        paneDeviceProfileView.setVisible(false);
        paneDevicesView.setVisible(true);
    }

    @FXML
    public void handleEditDeviceFromProfile() {
        if (currentProfileDevice == null) return;
        currentlySelectedDevice = currentProfileDevice;
        clearDeviceInlineErrors();
        txtSearchDeviceOwner.clear();
        filteredOwners.setPredicate(p -> true);
        cmbDeviceOwner.setItems(filteredOwners);
        txtSerialNumber.setText(currentlySelectedDevice.getSerialNumber());
        cmbDeviceType.setValue(currentlySelectedDevice.getDeviceType().toUpperCase());
        txtBrand.setText(currentlySelectedDevice.getBrand());
        txtModel.setText(currentlySelectedDevice.getModel());
        for (Student s : studentMasterList) {
            if (s.getStudentId() == currentlySelectedDevice.getOwnerId()) {
                cmbDeviceOwner.setValue(s);
                break;
            }
        }
        paneDeviceFormModal.setVisible(true);
        paneDeviceFormModal.toFront();
    }

    @FXML
    public void handleGoToOwnerProfile() {
        if (currentProfileDevice == null) return;
        for (Student s : studentMasterList) {
            if (s.getStudentId() == currentProfileDevice.getOwnerId()) {
                handleViewStudentProfile(s);
                return;
            }
        }
    }

    private void loadStudentActivityLog(int studentId) {
        ObservableList<ActivityLog> logs = FXCollections.observableArrayList();
        int totalEntries = 0, totalExits = 0;
        String lastSeen = null;

        String sql =
                "SELECT " +
                        "    dl_in.log_id, dl_in.device_id, " +
                        "    d.serial_number, d.brand, d.model, " +
                        "    TO_CHAR(dl_in.log_time  AT TIME ZONE 'Asia/Manila', 'YYYY-MM-DD HH24:MI:SS') AS time_in, " +
                        "    TO_CHAR(dl_out.log_time AT TIME ZONE 'Asia/Manila', 'YYYY-MM-DD HH24:MI:SS') AS time_out, " +
                        "    dl_in.status, " +
                        "    la_edit.reason AS amend_reason, " +
                        "    la_void.reason AS void_reason " +
                        "FROM device_logs dl_in " +
                        "JOIN devices d ON d.device_id = dl_in.device_id " +
                        "LEFT JOIN LATERAL ( " +
                        "    SELECT log_time FROM device_logs dl2 " +
                        "    WHERE dl2.device_id = dl_in.device_id AND dl2.direction = 'OUT' AND dl2.log_time > dl_in.log_time " +
                        "    ORDER BY dl2.log_time ASC LIMIT 1 " +
                        ") dl_out ON true " +
                        "LEFT JOIN log_amendments la_edit ON la_edit.original_log_id = dl_in.log_id AND la_edit.amendment_type = 'EDIT' " +
                        "LEFT JOIN log_amendments la_void ON la_void.original_log_id = dl_in.log_id AND la_void.amendment_type = 'VOID' " +
                        "WHERE dl_in.direction = 'IN' AND d.owner_id = ? " +
                        "ORDER BY dl_in.log_time DESC";

        try (Connection conn = DBConnection.connect();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, studentId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    String timeIn  = rs.getString("time_in");
                    String timeOut = rs.getString("time_out");
                    String deviceLabel = rs.getString("brand") + " " + rs.getString("model")
                            + " (" + rs.getString("serial_number") + ")";
                    ActivityLog log = new ActivityLog(
                            rs.getInt("log_id"), rs.getInt("device_id"), studentId,
                            deviceLabel, null, timeIn, timeOut,
                            rs.getString("status"), rs.getString("amend_reason"), rs.getString("void_reason"));
                    logs.add(log);
                    totalEntries++;
                    if (timeOut != null && !timeOut.isEmpty()) totalExits++;
                    if (lastSeen == null)
                        lastSeen = (timeOut != null && !timeOut.isEmpty()) ? timeOut : timeIn;
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }

        tblStudentActivityLog.setItems(logs);
        lblTotalEntries.setText(String.valueOf(totalEntries));
        lblTotalExits.setText(String.valueOf(totalExits));
        lblLastSeen.setText(lastSeen == null ? "—" : lastSeen);
    }

    private void loadDeviceActivityLog(int deviceId) {
        ObservableList<ActivityLog> logs = FXCollections.observableArrayList();
        boolean hasUnclosed = false;
        int ownerId = (currentProfileDevice != null) ? currentProfileDevice.getOwnerId() : 0;

        String sql =
                "SELECT " +
                        "    dl_in.log_id, dl_in.device_id, " +
                        "    TO_CHAR(dl_in.log_time  AT TIME ZONE 'Asia/Manila', 'YYYY-MM-DD HH24:MI:SS') AS time_in, " +
                        "    TO_CHAR(dl_out.log_time AT TIME ZONE 'Asia/Manila', 'YYYY-MM-DD HH24:MI:SS') AS time_out, " +
                        "    dl_in.status, " +
                        "    la_edit.reason AS amend_reason, " +
                        "    la_void.reason AS void_reason " +
                        "FROM device_logs dl_in " +
                        "LEFT JOIN LATERAL ( " +
                        "    SELECT log_time FROM device_logs dl2 " +
                        "    WHERE dl2.device_id = dl_in.device_id AND dl2.direction = 'OUT' AND dl2.log_time > dl_in.log_time " +
                        "    ORDER BY dl2.log_time ASC LIMIT 1 " +
                        ") dl_out ON true " +
                        "LEFT JOIN log_amendments la_edit ON la_edit.original_log_id = dl_in.log_id AND la_edit.amendment_type = 'EDIT' " +
                        "LEFT JOIN log_amendments la_void ON la_void.original_log_id = dl_in.log_id AND la_void.amendment_type = 'VOID' " +
                        "WHERE dl_in.direction = 'IN' AND dl_in.device_id = ? " +
                        "ORDER BY dl_in.log_time DESC";

        try (Connection conn = DBConnection.connect();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, deviceId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    String timeOut = rs.getString("time_out");
                    if (timeOut == null || timeOut.isEmpty()) hasUnclosed = true;
                    ActivityLog log = new ActivityLog(
                            rs.getInt("log_id"), rs.getInt("device_id"), ownerId,
                            null, null, rs.getString("time_in"), timeOut,
                            rs.getString("status"), rs.getString("amend_reason"), rs.getString("void_reason"));
                    logs.add(log);
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }

        tblDeviceActivityLog.setItems(logs);

        if (hasUnclosed) {
            lblDeviceStateBadge.setText("INSIDE");
            lblDeviceStateBadge.setStyle("-fx-background-color: #DCFCE7; -fx-text-fill: #15803D; -fx-padding: 8 16; -fx-background-radius: 50; -fx-font-weight: bold; -fx-font-size: 13px;");
            paneUnclosedLogBanner.setVisible(true);
            paneUnclosedLogBanner.setManaged(true);
        } else {
            lblDeviceStateBadge.setText("OUTSIDE");
            lblDeviceStateBadge.setStyle("-fx-background-color: #DBEAFE; -fx-text-fill: #1E40AF; -fx-padding: 8 16; -fx-background-radius: 50; -fx-font-weight: bold; -fx-font-size: 13px;");
            paneUnclosedLogBanner.setVisible(false);
            paneUnclosedLogBanner.setManaged(false);
        }
    }

    private void configureProfileLogColumns() {
        colSLogDevice.setCellValueFactory(new PropertyValueFactory<>("deviceLabel"));
        colSLogTimeIn.setCellValueFactory(new PropertyValueFactory<>("timeIn"));
        colSLogTimeOut.setCellValueFactory(new PropertyValueFactory<>("timeOut"));
        colSLogStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colDLogTimeIn.setCellValueFactory(new PropertyValueFactory<>("timeIn"));
        colDLogTimeOut.setCellValueFactory(new PropertyValueFactory<>("timeOut"));
        colDLogStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        colSLogTimeOut.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : (item == null || item.isEmpty() ? "— Still Inside" : item));
            }
        });
        colDLogTimeOut.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : (item == null || item.isEmpty() ? "— Still Inside" : item));
            }
        });

        colSLogStatus.setCellFactory(col -> buildStatusTagCell());
        colDLogStatus.setCellFactory(col -> buildStatusTagCell());

        colSLogDetails.setCellValueFactory(param -> new javafx.beans.property.SimpleObjectProperty<>(param.getValue()));
        colDLogDetails.setCellValueFactory(param -> new javafx.beans.property.SimpleObjectProperty<>(param.getValue()));
        colSLogDetails.setCellFactory(col -> buildDetailsCell());
        colDLogDetails.setCellFactory(col -> buildDetailsCell());
    }

    private TableCell<ActivityLog, String> buildStatusTagCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label lbl = new Label(item.toUpperCase());
                    lbl.setAlignment(Pos.CENTER);
                    lbl.setPrefWidth(90);
                    String base = "-fx-padding: 6 12; -fx-background-radius: 50; -fx-font-weight: bold; -fx-font-size: 12px;";
                    if ("AMENDED".equalsIgnoreCase(item)) {
                        lbl.setStyle(base + " -fx-background-color: #FEF3C7; -fx-text-fill: #D97706;");
                    } else if ("VOIDED".equalsIgnoreCase(item)) {
                        lbl.setStyle(base + " -fx-background-color: #FEE2E2; -fx-text-fill: #B91C1C;");
                    } else {
                        lbl.setStyle(base + " -fx-background-color: #DCFCE7; -fx-text-fill: #15803D;");
                    }
                    setGraphic(new StackPane(lbl));
                }
            }
        };
    }

    private TableCell<ActivityLog, ActivityLog> buildDetailsCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(ActivityLog item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }

                String status = item.getStatus() == null ? "" : item.getStatus().toUpperCase();
                if (!"AMENDED".equals(status) && !"VOIDED".equals(status)) { setGraphic(null); setText(null); return; }

                String detailText = "VOIDED".equals(status)
                        ? ("Void reason: "      + (item.getVoidReason()  == null || item.getVoidReason().isEmpty()  ? "No reason provided." : item.getVoidReason()))
                        : ("Amendment reason: " + (item.getAmendReason() == null || item.getAmendReason().isEmpty() ? "No reason provided." : item.getAmendReason()));

                Label detailLabel = new Label(detailText);
                detailLabel.setWrapText(true);
                detailLabel.setStyle("-fx-text-fill: #475569; -fx-font-size: 12px;");
                detailLabel.setMaxWidth(Double.MAX_VALUE);
                detailLabel.setVisible(false);
                detailLabel.setManaged(false);

                Button toggle = new Button("Show details ▾");
                toggle.setStyle("-fx-background-color: transparent; -fx-text-fill: #7A0000; -fx-font-weight: bold; -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 0;");
                toggle.setOnAction(e -> {
                    boolean nowVisible = !detailLabel.isVisible();
                    detailLabel.setVisible(nowVisible);
                    detailLabel.setManaged(nowVisible);
                    toggle.setText(nowVisible ? "Hide details ▴" : "Show details ▾");
                });

                setGraphic(new VBox(4, toggle, detailLabel));
                setText(null);
            }
        };
    }
}
