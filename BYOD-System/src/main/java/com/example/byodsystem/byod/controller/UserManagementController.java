package com.example.byodsystem.byod.controller;

import com.example.byodsystem.byod.database.DBConnection;
import com.example.byodsystem.byod.service.AuditLogger;
import com.example.byodsystem.byod.service.UserSession;
import com.example.byodsystem.byod.utils.AlertHelper;
import javafx.animation.PauseTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;
import javafx.stage.Window;

import java.net.URL;
import java.sql.*;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class UserManagementController implements Initializable {

    @FXML private Label lblDate;
    @FXML private Label lblUserName;
    @FXML private Label lblUserRole;
    @FXML private Label lblUserInitial;
    @FXML private Label lblUserCount;

    @FXML private TextField txtSearch;

    @FXML private TableView<UserManagementController.UserRow> tblUsers;
    @FXML private TableColumn<UserManagementController.UserRow, String> colUsername;
    @FXML private TableColumn<UserManagementController.UserRow, String> colName;
    @FXML private TableColumn<UserManagementController.UserRow, String> colRole;
    @FXML private TableColumn<UserManagementController.UserRow, String> colStatus;
    @FXML private TableColumn<UserManagementController.UserRow, String> colCreated;
    @FXML private TableColumn<UserManagementController.UserRow, String> colLastLogin;
    @FXML private TableColumn<UserManagementController.UserRow, String> colActions;

    @FXML private Button btnFilterAll;
    @FXML private Button btnFilterAdmin;
    @FXML private Button btnFilterSecurity;
    @FXML private Button btnFilterStudent;

    @FXML private StackPane addUserOverlay;
    @FXML private TextField txtAddFullName;
    @FXML private TextField txtAddUsername;
    @FXML private VBox boxAddStudentRef;
    @FXML private TextField txtAddStudentRef;
    @FXML private Label lblAddRefError;
    @FXML private ComboBox<String> cmbAddRole;
    @FXML private Button btnAddStatusActive;
    @FXML private Button btnAddStatusInactive;
    @FXML private PasswordField txtAddPassword;
    @FXML private PasswordField txtAddConfirmPassword;
    @FXML private Label lblAddGeneralError;

    @FXML private StackPane editUserOverlay;
    @FXML private TextField txtEditFullName;
    @FXML private TextField txtEditUsername;
    @FXML private VBox boxEditStudentRef;
    @FXML private TextField txtEditStudentRef;
    @FXML private Label lblEditRefError;
    @FXML private ComboBox<String> cmbEditRole;
    @FXML private Button btnEditStatusActive;
    @FXML private Button btnEditStatusInactive;
    @FXML private Label lblEditGeneralError;

    private final ObservableList<UserManagementController.UserRow> allUsers = FXCollections.observableArrayList();
    private final int currentUserId = UserSession.getInstance().getUserId();
    private String activeFilter = "ALL";

    private String selectedAddStatus = "ACTIVE";
    private String selectedEditStatus = "ACTIVE";
    private UserManagementController.UserRow targetedRowForAction = null;
    private String originalEditFullName = "";
    private String originalEditRole = "";
    private String originalEditStatus = "";
    private String originalEditStudentRef = "";

    private StackPane rootStackPane = null;

    private static final String STYLE_TAB_ACTIVE = "-fx-background-color: #7A0000; -fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 6 16; -fx-background-radius: 20; -fx-font-weight: bold; -fx-cursor: hand;";
    private static final String STYLE_TAB_INACTIVE = "-fx-background-color: transparent; -fx-text-fill: #4B5563; -fx-font-size: 13px; -fx-padding: 6 16; -fx-background-radius: 20; -fx-cursor: hand;";

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setDateLabel();
        setupUserInfo();
        setupTable();

        cmbAddRole.setItems(FXCollections.observableArrayList("ADMIN", "OFFICER", "STUDENT"));
        cmbEditRole.setItems(FXCollections.observableArrayList("ADMIN", "OFFICER", "STUDENT"));

        cmbAddRole.valueProperty().addListener((obs, oldVal, newVal) -> {
            boolean isStudent = "STUDENT".equals(newVal);
            boxAddStudentRef.setVisible(isStudent);
            boxAddStudentRef.setManaged(isStudent);
        });

        cmbEditRole.valueProperty().addListener((obs, oldVal, newVal) -> {
            boolean isStudent = "STUDENT".equals(newVal);
            boxEditStudentRef.setVisible(isStudent);
            boxEditStudentRef.setManaged(isStudent);
        });

        updateTabStyles();
        updateStatusToggleVisuals(true, "ACTIVE");
        loadUsers();
    }

    private void setDateLabel() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");
        lblDate.setText(java.time.LocalDate.now(ZoneId.of("Asia/Manila")).format(fmt));
    }

    private void setupUserInfo() {
        String name = UserSession.getInstance().getFullName();
        String role = UserSession.getInstance().getRole();
        lblUserName.setText(name != null ? name : "Administrator");
        lblUserRole.setText(role != null ? role : "ADMIN");
        lblUserInitial.setText(name != null && !name.isEmpty() ? String.valueOf(name.charAt(0)).toUpperCase() : "A");
    }

    private void setupTable() {
        tblUsers.setFixedCellSize(45.0);

        colUsername.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getUsername()));
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFullName()));

        colRole.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getRole()));
        colRole.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); return; }
                Label badge = new Label(item);
                String color = "ADMIN".equals(item) ? "#7A0000" : "OFFICER".equals(item) ? "#E67E00" : "#2C6E49";
                badge.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 4 10; -fx-background-radius: 4;");

                HBox container = new HBox(badge);
                container.setAlignment(Pos.CENTER);
                setGraphic(container); setText(null);
            }
        });

        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); return; }
                Label badge = new Label(item);
                boolean isActive = "ACTIVE".equals(item);
                badge.setStyle("-fx-background-color: " + (isActive ? "#D4EDDA" : "#F8D7DA") + "; "
                        + "-fx-text-fill: " + (isActive ? "#155724" : "#721c24") + "; "
                        + "-fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 4 10; -fx-background-radius: 4;");

                HBox container = new HBox(badge);
                container.setAlignment(Pos.CENTER);
                setGraphic(container); setText(null);
            }
        });

        colCreated.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCreatedAt()));
        colLastLogin.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getLastLogin()));

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit   = new Button();
            private final Button btnToggle = new Button("\u21BA");
            {
                // ── Edit button — SVG pencil icon ────────────────────────────
                SVGPath pencil = new SVGPath();
                pencil.setContent("M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04a1 1 0 0 0 0-1.41l-2.34-2.34a1 1 0 0 0-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z");
                pencil.setFill(javafx.scene.paint.Color.web("#E67E00"));
                pencil.setScaleX(0.85);
                pencil.setScaleY(0.85);
                btnEdit.setGraphic(pencil);
                btnEdit.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 4 8;");

                // ── Toggle button ────────────────────────────────────────────
                btnToggle.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-font-size: 18px; -fx-padding: 2 8; -fx-font-weight: bold; -fx-text-fill: #2C6E49;");

                btnEdit.setOnAction(e -> openEditUserOverlay(getTableView().getItems().get(getIndex())));
                btnToggle.setOnAction(e -> toggleUserStatus(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                HBox box = new HBox(12, btnEdit, btnToggle);
                box.setAlignment(Pos.CENTER);
                setGraphic(box); setText(null);
            }
        });
    }

    private void loadUsers() {
        allUsers.clear();
        String sql = "SELECT user_id, username, full_name, role, status, " +
                "TO_CHAR(created_at AT TIME ZONE 'Asia/Manila', 'YYYY-MM-DD') AS created, " +
                "TO_CHAR(last_login AT TIME ZONE 'Asia/Manila', 'YYYY-MM-DD HH24:MI') AS last_login " +
                "FROM users ORDER BY created_at";
        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                allUsers.add(new UserManagementController.UserRow(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getString("full_name"),
                        rs.getString("role"),
                        rs.getString("status"),
                        rs.getString("created"),
                        rs.getString("last_login") != null ? rs.getString("last_login") : "—"
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showOverlayPopup("Database Error", "Failed to load users from the database.");
        }
        applyFilterAndSearch();
    }

    private void applyFilterAndSearch() {
        String searchTxt = (txtSearch != null && txtSearch.getText() != null) ? txtSearch.getText().trim().toLowerCase() : "";
        ObservableList<UserManagementController.UserRow> filtered = FXCollections.observableArrayList();

        for (UserManagementController.UserRow r : allUsers) {
            boolean matchesFilter = "ALL".equals(activeFilter) || r.getRole().equalsIgnoreCase(activeFilter);
            boolean matchesSearch = searchTxt.isEmpty() ||
                    r.getUsername().toLowerCase().contains(searchTxt) ||
                    r.getFullName().toLowerCase().contains(searchTxt);

            if (matchesFilter && matchesSearch) {
                filtered.add(r);
            }
        }
        tblUsers.setItems(filtered);
        lblUserCount.setText(filtered.size() + (filtered.size() == 1 ? " user" : " users"));
    }

    private boolean checkStudentIdExists(String studentId) {
        if (studentId == null || studentId.trim().isEmpty()) {
            return false;
        }
        String sql = "SELECT COUNT(*) FROM students WHERE student_code = ?";
        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, studentId.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @FXML private void handleSearch() { applyFilterAndSearch(); }

    @FXML private void filterAll()      { activeFilter = "ALL";     updateTabStyles(); applyFilterAndSearch(); }
    @FXML private void filterAdmin()    { activeFilter = "ADMIN";   updateTabStyles(); applyFilterAndSearch(); }
    @FXML private void filterStudent()  { activeFilter = "STUDENT"; updateTabStyles(); applyFilterAndSearch(); }
    @FXML private void filterSecurity() { activeFilter = "OFFICER"; updateTabStyles(); applyFilterAndSearch(); }

    private void updateTabStyles() {
        if (btnFilterAll != null) btnFilterAll.setStyle("ALL".equals(activeFilter) ? STYLE_TAB_ACTIVE : STYLE_TAB_INACTIVE);
        if (btnFilterAdmin != null) btnFilterAdmin.setStyle("ADMIN".equals(activeFilter) ? STYLE_TAB_ACTIVE : STYLE_TAB_INACTIVE);
        if (btnFilterSecurity != null) btnFilterSecurity.setStyle("OFFICER".equals(activeFilter) ? STYLE_TAB_ACTIVE : STYLE_TAB_INACTIVE);
        if (btnFilterStudent != null) btnFilterStudent.setStyle("STUDENT".equals(activeFilter) ? STYLE_TAB_ACTIVE : STYLE_TAB_INACTIVE);
    }

    @FXML
    private void handleCreateUser() {
        txtAddFullName.clear();
        txtAddUsername.clear();
        txtAddStudentRef.clear();
        txtAddPassword.clear();
        txtAddConfirmPassword.clear();
        lblAddRefError.setVisible(false);
        lblAddGeneralError.setVisible(false);
        lblAddGeneralError.setManaged(false);

        cmbAddRole.getSelectionModel().select("STUDENT");
        boxAddStudentRef.setVisible(true);
        boxAddStudentRef.setManaged(true);

        selectedAddStatus = "ACTIVE";
        updateStatusToggleVisuals(true, "ACTIVE");

        showOverlay(addUserOverlay);
    }

    @FXML private void handleAddModalClose() { hideOverlay(addUserOverlay); }

    @FXML
    private void handleAddModalSubmit() {
        String username = txtAddUsername.getText().trim();
        String fullName = txtAddFullName.getText().trim();
        String password = txtAddPassword.getText();
        String confirmPw = txtAddConfirmPassword.getText();
        String role = cmbAddRole.getValue();
        String studentRef = txtAddStudentRef.getText().trim();

        if (username.isEmpty() || fullName.isEmpty() || role == null || password.isEmpty()) {
            showInlineAddError("Please fill in all required fields.");
            return;
        }
        if (!password.equals(confirmPw)) {
            showInlineAddError("Passwords do not match.");
            return;
        }

        if ("STUDENT".equals(role)) {
            if (studentRef.isEmpty() || !checkStudentIdExists(studentRef)) {
                lblAddRefError.setVisible(true);
                return;
            } else {
                lblAddRefError.setVisible(false);
            }
        }

        try (Connection conn = DBConnection.connect()) {
            String hash = org.mindrot.jbcrypt.BCrypt.hashpw(password, org.mindrot.jbcrypt.BCrypt.gensalt());
            String sql = "INSERT INTO users (username, full_name, role, status, password_hash, student_ref_id) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, username);
                ps.setString(2, fullName);
                ps.setString(3, role);
                ps.setString(4, selectedAddStatus);
                ps.setString(5, hash);
                if ("STUDENT".equals(role)) {
                    ps.setString(6, studentRef);
                } else {
                    ps.setNull(6, Types.VARCHAR);
                }
                ps.executeUpdate();

                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        AuditLogger.log(conn, currentUserId, "RECORD_CREATED", "User", rs.getInt(1),
                                "{\"username\":\"" + username + "\",\"role\":\"" + role + "\"}");
                    }
                }
            }
            hideOverlay(addUserOverlay);
            showOverlayPopup("Success", "User account saved successfully.");
            loadUsers();
        } catch (SQLException ex) {
            showOverlayPopup("Database Error", ex.getMessage());
        }
    }

    private void openEditUserOverlay(UserManagementController.UserRow row) {
        targetedRowForAction = row;
        txtEditFullName.setText(row.getFullName());
        txtEditUsername.setText(row.getUsername());
        txtEditStudentRef.clear();
        lblEditRefError.setVisible(false);
        cmbEditRole.setValue(row.getRole());

        boolean isStudent = "STUDENT".equals(row.getRole());
        originalEditFullName = row.getFullName();
        originalEditRole = row.getRole();
        originalEditStatus = row.getStatus();
        originalEditStudentRef = txtEditStudentRef.getText().trim();
        boxEditStudentRef.setVisible(isStudent);
        boxEditStudentRef.setManaged(isStudent);

        if (isStudent) {
            String fetchRefSql = "SELECT student_ref_id FROM users WHERE user_id = ?";
            try (Connection conn = DBConnection.connect();
                 PreparedStatement ps = conn.prepareStatement(fetchRefSql)) {
                ps.setInt(1, row.getUserId());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getString("student_ref_id") != null) {
                        txtEditStudentRef.setText(rs.getString("student_ref_id"));
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        selectedEditStatus = row.getStatus();
        updateStatusToggleVisuals(false, selectedEditStatus);

        showOverlay(editUserOverlay);
    }

    @FXML private void handleEditModalClose() { hideOverlay(editUserOverlay); }

    @FXML
    private void handleEditModalSubmit() {

        if (targetedRowForAction == null) return;
        String fullName = txtEditFullName.getText().trim();
        String role = cmbEditRole.getValue();
        String studentRef = txtEditStudentRef.getText().trim();

        if (fullName.isEmpty() || role == null) {
            showInlineEditError("Full Name and Role fields cannot be empty.");
            return;
        }

        if ("STUDENT".equals(role)) {
            if (studentRef.isEmpty() || !checkStudentIdExists(studentRef)) {
                lblEditRefError.setText("This Student ID is required and must exist in our system.");
                lblEditRefError.setVisible(true);
                return;
            } else {
                lblEditRefError.setVisible(false);
            }
        }
        boolean changed = !fullName.equals(originalEditFullName)
                || !role.equals(originalEditRole)
                || !selectedEditStatus.equals(originalEditStatus)
                || !studentRef.equals(originalEditStudentRef);

        if (!changed) {
            hideOverlay(editUserOverlay);
            return;
        }

        Window owner = txtEditFullName.getScene().getWindow();
        AlertHelper.showConfirm(owner, "Save Changes",
                "You are about to update the account details for '" + targetedRowForAction.getUsername() + "'. Proceed?", () -> {
                    try (Connection conn = DBConnection.connect()) {
                        String sql = "UPDATE users SET full_name=?, role=?, status=?, student_ref_id=? WHERE user_id=?";
                        try (PreparedStatement ps = conn.prepareStatement(sql)) {
                            ps.setString(1, fullName);
                            ps.setString(2, role);
                            ps.setString(3, selectedEditStatus);
                            if ("STUDENT".equals(role)) {
                                ps.setString(4, studentRef);
                            } else {
                                ps.setNull(4, Types.VARCHAR);
                            }
                            ps.setInt(5, targetedRowForAction.getUserId());
                            ps.executeUpdate();
                        }
                        AuditLogger.log(conn, currentUserId, "RECORD_EDITED", "User", targetedRowForAction.getUserId(),
                                "{\"username\":\"" + targetedRowForAction.getUsername() + "\",\"role\":\"" + role + "\"}");

                        hideOverlay(editUserOverlay);
                        showOverlayPopup("Success", "Changes saved successfully.");
                        loadUsers();
                    } catch (SQLException ex) {
                        showOverlayPopup("Database Error", "Failed to save changes: " + ex.getMessage());
                    }
                });
    }

    private void toggleUserStatus(UserManagementController.UserRow row) {
        String nextStatus = "ACTIVE".equals(row.getStatus()) ? "INACTIVE" : "ACTIVE";

        Window owner = tblUsers.getScene().getWindow();
        AlertHelper.showConfirm(owner, "Change Account Status",
                "Change status of '" + row.getUsername() + "' to " + nextStatus + "?", () -> {
            try (Connection conn = DBConnection.connect();
                 PreparedStatement ps = conn.prepareStatement("UPDATE users SET status=? WHERE user_id=?")) {
                ps.setString(1, nextStatus);
                ps.setInt(2, row.getUserId());
                ps.executeUpdate();

                if ("STUDENT".equals(row.getRole())) {
                    String syncSql = "UPDATE students SET status = ? " +
                            "WHERE student_code = (" +
                            "SELECT student_ref_id FROM users WHERE user_id = ?)";
                    try (PreparedStatement syncPst = conn.prepareStatement(syncSql)) {
                        syncPst.setString(1, nextStatus);
                        syncPst.setInt(2, row.getUserId());
                        syncPst.executeUpdate();
                    }
                }

                AuditLogger.log(conn, currentUserId, "ACTIVE".equals(nextStatus) ? "RECORD_REACTIVATED" : "RECORD_DEACTIVATED",
                        "User", row.getUserId(), "{\"username\":\"" + row.getUsername() + "\"}");
                showOverlayPopup("Success", "Account status updated to " + nextStatus + ".");
                loadUsers();
            } catch (SQLException e) {
                showOverlayPopup("Database Error", "Error updating account status.");
            }
        });
    }


    @FXML private void handleAddStatusActive() { selectedAddStatus = "ACTIVE"; updateStatusToggleVisuals(true, "ACTIVE"); }
    @FXML private void handleAddStatusInactive() { selectedAddStatus = "INACTIVE"; updateStatusToggleVisuals(true, "INACTIVE"); }
    @FXML private void handleEditStatusActive() { selectedEditStatus = "ACTIVE"; updateStatusToggleVisuals(false, "ACTIVE"); }
    @FXML private void handleEditStatusInactive() { selectedEditStatus = "INACTIVE"; updateStatusToggleVisuals(false, "INACTIVE"); }

    private void updateStatusToggleVisuals(boolean isAddModal, String targetStatus) {
        String activeBtnStyle = "-fx-background-color: #7A0000; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 4;";
        String inactiveBtnStyle = "-fx-background-color: #E5E7EB; -fx-text-fill: #4B5563; -fx-background-radius: 4; -fx-cursor: hand;";

        if (isAddModal) {
            btnAddStatusActive.setStyle("ACTIVE".equals(targetStatus) ? activeBtnStyle : inactiveBtnStyle);
            btnAddStatusInactive.setStyle("INACTIVE".equals(targetStatus) ? activeBtnStyle : inactiveBtnStyle);
        } else {
            btnEditStatusActive.setStyle("ACTIVE".equals(targetStatus) ? activeBtnStyle : inactiveBtnStyle);
            btnEditStatusInactive.setStyle("INACTIVE".equals(targetStatus) ? activeBtnStyle : inactiveBtnStyle);
        }
    }

    private void showOverlay(StackPane overlay) {
        overlay.setManaged(true);
        overlay.setVisible(true);
    }

    private void hideOverlay(StackPane overlay) {
        overlay.setVisible(false);
        overlay.setManaged(false);
    }

    private StackPane getRootStack() {
        if (rootStackPane != null) return rootStackPane;
        javafx.scene.Node node = tblUsers;
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
        String iconPath  = isError
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
        icon.setContent(iconPath);
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

    public static class UserRow {
        private final int userId;
        private final String username, fullName, role, status, createdAt, lastLogin;

        public UserRow(int userId, String username, String fullName, String role, String status, String createdAt, String lastLogin) {
            this.userId = userId; this.username = username; this.fullName = fullName; this.role = role; this.status = status; this.createdAt = createdAt; this.lastLogin = lastLogin;
        }
        public int getUserId()     { return userId; }
        public String getUsername()  { return username; }
        public String getFullName()  { return fullName; }
        public String getRole()      { return role; }
        public String getStatus()    { return status; }
        public String getCreatedAt() { return createdAt; }
        public String getLastLogin() { return lastLogin; }
    }

    private void showInlineAddError(String message) {
        lblAddGeneralError.setText(message);
        lblAddGeneralError.setVisible(true);
        lblAddGeneralError.setManaged(true);
    }
    private void showInlineEditError(String message) {
        lblEditRefError.setText(message);
        lblEditRefError.setVisible(true);
    }
}