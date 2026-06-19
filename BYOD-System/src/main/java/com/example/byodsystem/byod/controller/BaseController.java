package com.example.byodsystem.byod.controller;

import com.example.byodsystem.byod.database.DBConnection;
import com.example.byodsystem.byod.service.AuditLogger;
import com.example.byodsystem.byod.service.UserSession;
import javafx.application.Platform;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

public class BaseController {

    @FXML private BorderPane mainContainer;
    @FXML private StackPane contentArea;

    @FXML private Button btnSidebarDashboard;
    @FXML private Button btnSidebarUserManagement;
    @FXML private Button btnSidebarRequests;
    @FXML private Button btnSidebarReports;
    @FXML private Button btnSidebarAuditLog;
    @FXML private Button btnSidebarSettings;

    @FXML private Button btnSidebarGateScreen;
    @FXML private Button btnSidebarActivityLog;
    @FXML private Button btnSidebarStudentsDevices;

    @FXML private Button btnSidebarProfile;
    @FXML private Button btnSidebarDeviceLog;

    @FXML private Button btnSidebarChangePassword;
    @FXML private Button btnSidebarLogout;

    @FXML private VBox sidebarContainer;
    @FXML private HBox sidebarHeader;
    @FXML private VBox sidebarBrandLabels;
    @FXML private VBox sidebarNavContainer;
    @FXML private VBox sidebarBottomContainer;
    @FXML private Button btnCollapseSidebar;

    private static final double EXPANDED_WIDTH = 260.0;
    private static final double COLLAPSED_WIDTH = 70.0;
    private boolean collapsed = false;

    private Button currentActiveButton = null;

    @FXML
    public void initialize() {
        Platform.runLater(() -> {
            if (mainContainer != null && mainContainer.getScene() != null) {
                Stage stage = (Stage) mainContainer.getScene().getWindow();
                if (stage != null) {
                    stage.setFullScreen(true);
                    stage.setFullScreenExitHint("");
                }
            }
        });

        UserSession session = UserSession.getInstance();
        String role = session.getRole() != null ? session.getRole().toUpperCase() : "";

        btnSidebarDashboard.setVisible(false);
        btnSidebarDashboard.setManaged(false);
        btnSidebarUserManagement.setVisible(false);
        btnSidebarUserManagement.setManaged(false);
        btnSidebarRequests.setVisible(false);
        btnSidebarRequests.setManaged(false);
        btnSidebarReports.setVisible(false);
        btnSidebarReports.setManaged(false);
        btnSidebarAuditLog.setVisible(false);
        btnSidebarAuditLog.setManaged(false);
        btnSidebarSettings.setVisible(false);
        btnSidebarSettings.setManaged(false);

        btnSidebarGateScreen.setVisible(false);
        btnSidebarGateScreen.setManaged(false);
        btnSidebarActivityLog.setVisible(false);
        btnSidebarActivityLog.setManaged(false);
        btnSidebarStudentsDevices.setVisible(false);
        btnSidebarStudentsDevices.setManaged(false);

        btnSidebarProfile.setVisible(false);
        btnSidebarProfile.setManaged(false);
        btnSidebarDeviceLog.setVisible(false);
        btnSidebarDeviceLog.setManaged(false);

        setupHoverAnimations();

        if ("ADMIN".equals(role)) {
            btnSidebarDashboard.setVisible(true);
            btnSidebarDashboard.setManaged(true);
            btnSidebarUserManagement.setVisible(true);
            btnSidebarUserManagement.setManaged(true);
            btnSidebarRequests.setVisible(true);
            btnSidebarRequests.setManaged(true);
            btnSidebarReports.setVisible(true);
            btnSidebarReports.setManaged(true);
            btnSidebarAuditLog.setVisible(true);
            btnSidebarAuditLog.setManaged(true);
            btnSidebarSettings.setVisible(true);
            btnSidebarSettings.setManaged(true);

            btnSidebarGateScreen.setVisible(true);
            btnSidebarGateScreen.setManaged(true);
            btnSidebarActivityLog.setVisible(true);
            btnSidebarActivityLog.setManaged(true);
            btnSidebarStudentsDevices.setVisible(true);
            btnSidebarStudentsDevices.setManaged(true);

            handleNavigateToDashboard();
        } else if ("OFFICER".equals(role)) {
            btnSidebarGateScreen.setVisible(true);
            btnSidebarGateScreen.setManaged(true);
            btnSidebarActivityLog.setVisible(true);
            btnSidebarActivityLog.setManaged(true);
            btnSidebarStudentsDevices.setVisible(true);
            btnSidebarStudentsDevices.setManaged(true);

            handleNavigateToGateScreen();
        } else if ("STUDENT".equals(role)) {
            btnSidebarProfile.setVisible(true);
            btnSidebarProfile.setManaged(true);
            btnSidebarDeviceLog.setVisible(true);
            btnSidebarDeviceLog.setManaged(true);

            handleNavigateToProfile();
        }
    }

    private void setupHoverAnimations() {
        Button[] buttons = {
                btnSidebarDashboard, btnSidebarUserManagement, btnSidebarRequests,
                btnSidebarReports, btnSidebarAuditLog, btnSidebarSettings,
                btnSidebarGateScreen, btnSidebarActivityLog, btnSidebarStudentsDevices,
                btnSidebarProfile, btnSidebarDeviceLog,
                btnSidebarChangePassword, btnSidebarLogout
        };

        for (Button btn : buttons) {
            if (btn != null) {
                btn.setOnMouseEntered(e -> {
                    if (btn != currentActiveButton) {
                        btn.setStyle("-fx-background-color: #F3F4F6; -fx-text-fill: #111111; -fx-background-radius: 12; -fx-alignment: BASELINE_LEFT; -fx-graphic-text-gap: 16.0; -fx-padding: 0 0 0 16; -fx-cursor: hand;");
                        if (btn.getGraphic() instanceof Label) {
                            ((Label) btn.getGraphic()).setStyle("-fx-text-fill: #111111;");
                        }
                    }
                });
                btn.setOnMouseExited(e -> {
                    if (btn != currentActiveButton) {
                        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #4B5563; -fx-background-radius: 0; -fx-alignment: BASELINE_LEFT; -fx-graphic-text-gap: 16.0; -fx-padding: 0 0 0 16; -fx-cursor: hand;");
                        if (btn.getGraphic() instanceof Label) {
                            ((Label) btn.getGraphic()).setStyle("-fx-text-fill: #4B5563;");
                        }
                    }
                });
            }
        }
    }

    private void updateActiveButtonStyle(Button activeTemplateButton) {
        currentActiveButton = activeTemplateButton;
        Button[] buttons = {
                btnSidebarDashboard, btnSidebarUserManagement, btnSidebarRequests,
                btnSidebarReports, btnSidebarAuditLog, btnSidebarSettings,
                btnSidebarGateScreen, btnSidebarActivityLog, btnSidebarStudentsDevices,
                btnSidebarProfile, btnSidebarDeviceLog,
                btnSidebarChangePassword, btnSidebarLogout
        };

        for (Button btn : buttons) {
            if (btn != null) {
                if (btn == activeTemplateButton) {
                    btn.setStyle("-fx-background-color: #7A0000; -fx-text-fill: #FFFFFF; -fx-background-radius: 12; -fx-font-weight: bold; -fx-alignment: BASELINE_LEFT; -fx-graphic-text-gap: 16.0; -fx-padding: 0 0 0 16; -fx-cursor: hand;");
                    if (btn.getGraphic() instanceof Label) {
                        ((Label) btn.getGraphic()).setStyle("-fx-text-fill: #FFFFFF; -fx-font-weight: bold;");
                    }
                } else {
                    btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #4B5563; -fx-background-radius: 0; -fx-font-weight: normal; -fx-alignment: BASELINE_LEFT; -fx-graphic-text-gap: 16.0; -fx-padding: 0 0 0 16; -fx-cursor: hand;");
                    if (btn.getGraphic() instanceof Label) {
                        ((Label) btn.getGraphic()).setStyle("-fx-text-fill: #4B5563; -fx-font-weight: normal;");
                    }
                }
            }
        }
    }

    @FXML
    public void handleNavigateToDashboard() {
        updateActiveButtonStyle(btnSidebarDashboard);
        loadSubView("/com/example/byodsystem/byod/fxml/dashboard.fxml");
    }

    @FXML
    public void handleNavigateToUserManagement() {
        updateActiveButtonStyle(btnSidebarUserManagement);
        loadSubView("/com/example/byodsystem/byod/fxml/usermanagement.fxml");
    }

    @FXML
    public void handleNavigateToReports() {
        updateActiveButtonStyle(btnSidebarReports);
        loadSubView("/com/example/byodsystem/byod/fxml/reports.fxml");
    }

    @FXML
    public void handleNavigateToAuditLog() {
        updateActiveButtonStyle(btnSidebarAuditLog);
        loadSubView("/com/example/byodsystem/byod/fxml/auditlog.fxml");
    }

    @FXML
    public void handleNavigateToSettings() {
        updateActiveButtonStyle(btnSidebarSettings);
        loadSubView("/com/example/byodsystem/byod/fxml/settings.fxml");
    }

    @FXML
    public void handleNavigateToGateScreen() {
        updateActiveButtonStyle(btnSidebarGateScreen);
        collapseSidebar();
        loadSubView("/com/example/byodsystem/byod/fxml/gatescreen.fxml");
    }

    @FXML
    public void handleNavigateToActivityLog() {
        updateActiveButtonStyle(btnSidebarActivityLog);
        loadSubView("/com/example/byodsystem/byod/fxml/activity.fxml");
    }

    @FXML
    public void handleNavigateToStudentsDevices() {
        updateActiveButtonStyle(btnSidebarStudentsDevices);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/byodsystem/byod/fxml/studentsdevices.fxml"));
            Parent view = loader.load();

            StudentsDevicesController controller = loader.getController();
            String role = UserSession.getInstance().getRole();
            controller.setUserRole(role);

            if (contentArea != null) {
                contentArea.getChildren().clear();
                contentArea.getChildren().add(view);
            }
        } catch (IOException e) {
            System.err.println("Fatal: Could not transition Students & Devices panel.");
            e.printStackTrace();
        }
    }

    @FXML
    public void handleNavigateToProfile() {
        updateActiveButtonStyle(btnSidebarProfile);
        loadSubView("/com/example/byodsystem/byod/fxml/studentprofile.fxml");
    }

    @FXML
    public void handleNavigateToDeviceLog() {
        updateActiveButtonStyle(btnSidebarDeviceLog);
        loadSubView("/com/example/byodsystem/byod/fxml/studentdevicelog.fxml");
    }

    @FXML
    public void handleNavigateToChangePassword() {
        updateActiveButtonStyle(btnSidebarChangePassword);
        loadSubView("/com/example/byodsystem/byod/fxml/changepassword.fxml");
    }

    @FXML
    public void handleToggleSidebar() {
        if (collapsed) {
            expandSidebar();
        } else {
            collapseSidebar();
        }
    }

    @FXML
    public void handleSidebarAreaClicked() {
        if (collapsed) {
            expandSidebar();
        }
    }

    private void collapseSidebar() {
        collapsed = true;
        setNavLabelsVisible(false);
        btnCollapseSidebar.setText("›");
        animateSidebarWidth(COLLAPSED_WIDTH);
    }

    private void expandSidebar() {
        collapsed = false;
        btnCollapseSidebar.setText("‹");
        animateSidebarWidth(EXPANDED_WIDTH);
        setNavLabelsVisible(true);
    }

    private void animateSidebarWidth(double targetWidth) {
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.millis(220),
                        new KeyValue(sidebarContainer.prefWidthProperty(), targetWidth),
                        new KeyValue(sidebarContainer.minWidthProperty(), targetWidth),
                        new KeyValue(sidebarContainer.maxWidthProperty(), targetWidth)
                )
        );
        timeline.play();
    }

    private void setNavLabelsVisible(boolean visible) {
        sidebarBrandLabels.setVisible(visible);
        sidebarBrandLabels.setManaged(visible);

        for (Node node : sidebarNavContainer.getChildren()) {
            setButtonTextVisible(node, visible);
        }
        for (Node node : sidebarBottomContainer.getChildren()) {
            setButtonTextVisible(node, visible);
        }
    }

    private void setButtonTextVisible(Node node, boolean visible) {
        if (node instanceof Button) {
            Button btn = (Button) node;
            if (!btn.getProperties().containsKey("fullText")) {
                btn.getProperties().put("fullText", btn.getText());
            }
            String fullText = (String) btn.getProperties().get("fullText");
            btn.setText(visible ? fullText : "");
        }
    }

    private void loadSubView(String fxmlPath) {
        try {
            Parent view = FXMLLoader.load(getClass().getResource(fxmlPath));
            if (contentArea != null) {
                contentArea.getChildren().clear();
                contentArea.getChildren().add(view);
            }
        } catch (IOException e) {
            System.err.println("Fatal: Could not load layout sub-view: " + fxmlPath);
            e.printStackTrace();
        }
    }

    @FXML
    public void handleNavigateToProfileRequests() {
        updateActiveButtonStyle(btnSidebarRequests);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/byodsystem/byod/fxml/studentprofilerequest.fxml"));
            Parent view = loader.load();

            StudentProfileRequestController controller = loader.getController();
            UserSession session = UserSession.getInstance();

            int userId = session.getUserId();
            boolean isFirstLogin = session.isFirstLogin();
            String studentRefId = (session.getStudentRefId() != null) ? session.getStudentRefId() : null;

            controller.initializeSession(userId, isFirstLogin, studentRefId);

            if (contentArea != null) {
                contentArea.getChildren().clear();
                contentArea.getChildren().add(view);
            }
        } catch (IOException e) {
            System.err.println("Fatal: Could not transition profile request panel.");
            e.printStackTrace();
        }
    }

    @FXML public void handleNavigateToStudentProfile() { handleNavigateToProfile(); }

    @FXML
    public void handleLogout() {
        showLogoutConfirmation();
    }

    private void showLogoutConfirmation() {
        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.55);");
        overlay.setAlignment(Pos.CENTER);
        overlay.setOnMouseClicked(javafx.event.Event::consume);

        overlay.prefWidthProperty().bind(mainContainer.getScene().widthProperty());
        overlay.prefHeightProperty().bind(mainContainer.getScene().heightProperty());

        StackPane iconCircle = new StackPane();
        iconCircle.setMinSize(64, 64);
        iconCircle.setMaxSize(64, 64);
        iconCircle.setStyle("-fx-background-color: #FEF3C7; -fx-background-radius: 32;");

        Label iconLabel = new Label("⚠");
        iconLabel.setStyle("-fx-text-fill: #D97706; -fx-font-size: 28px;");
        iconCircle.getChildren().add(iconLabel);

        Label lblTitle = new Label("Confirm Logout");
        lblTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #111111; -fx-font-family: 'Segoe UI', Inter, sans-serif;");

        Label lblSub = new Label("Are you sure you want to logout?");
        lblSub.setStyle("-fx-font-size: 13px; -fx-text-fill: #6B7280; -fx-font-family: 'Segoe UI', Inter, sans-serif;");
        lblSub.setWrapText(true);
        lblSub.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

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

        Button btnConfirm = new Button("Logout");
        btnConfirm.setPrefWidth(120);
        btnConfirm.setPrefHeight(40);
        btnConfirm.setStyle(
                "-fx-background-color: #7A0000; " +
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
                        "-fx-padding: 36 48 36 48; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.25), 15, 0, 0, 4);"
        );
        dialog.setMaxWidth(380);
        dialog.setMaxHeight(260);
        dialog.setMinWidth(380);

        overlay.getChildren().add(dialog);
        StackPane.setAlignment(dialog, Pos.CENTER);

        GaussianBlur lightBlur = new GaussianBlur(3.5);
        if (sidebarContainer != null) sidebarContainer.setEffect(lightBlur);
        if (contentArea != null) contentArea.setEffect(lightBlur);

        Parent currentRoot = mainContainer.getScene().getRoot();
        if (currentRoot instanceof StackPane && "windowRootWrapper".equals(currentRoot.getId())) {
            ((StackPane) currentRoot).getChildren().add(overlay);
        } else {
            StackPane windowRootWrapper = new StackPane();
            windowRootWrapper.setId("windowRootWrapper");
            mainContainer.getScene().setRoot(windowRootWrapper);
            windowRootWrapper.getChildren().addAll(currentRoot, overlay);
        }

        btnCancel.setOnAction(e -> {
            Parent activeRoot = mainContainer.getScene().getRoot();
            if (activeRoot instanceof StackPane) {
                ((StackPane) activeRoot).getChildren().remove(overlay);
            }
            if (sidebarContainer != null) sidebarContainer.setEffect(null);
            if (contentArea != null) contentArea.setEffect(null);
        });

        btnConfirm.setOnAction(e -> {
            Parent activeRoot = mainContainer.getScene().getRoot();
            if (activeRoot instanceof StackPane) {
                ((StackPane) activeRoot).getChildren().remove(overlay);
            }
            if (sidebarContainer != null) sidebarContainer.setEffect(null);
            if (contentArea != null) contentArea.setEffect(null);
            performLogout();
        });
    }

    private void performLogout() {
        updateActiveButtonStyle(btnSidebarLogout);
        UserSession session = UserSession.getInstance();
        int userId = session.getUserId();
        String username = session.getUsername() != null ? session.getUsername() : "Unknown User";

        try (Connection conn = DBConnection.connect()) {
            AuditLogger.log(conn, userId, "LOGOUT", "USER", userId,
                    "{\"username\":\"" + username + "\",\"action\":\"Logged out safely from base framework.\"}");
        } catch (SQLException e) {
            System.err.println("Database error tracking logout session audit trace.");
            e.printStackTrace();
        }

        try {
            session.cleanSession();
            Parent loginRoot = FXMLLoader.load(getClass().getResource("/com/example/byodsystem/byod/fxml/login.fxml"));
            Stage stage = (Stage) mainContainer.getScene().getWindow();
            stage.setScene(new Scene(loginRoot));
            stage.setFullScreen(true);
        } catch (IOException e) {
            System.err.println("Fatal error navigating back to Login");
            e.printStackTrace();
        }
    }
}