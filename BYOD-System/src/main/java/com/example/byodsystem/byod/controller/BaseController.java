package com.example.byodsystem.byod.controller;

import com.example.byodsystem.byod.service.UserSession;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import java.io.IOException;

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
    @FXML private Button btnSidebarUpdateRequest;

    @FXML private Button btnSidebarChangePassword;
    @FXML private Button btnSidebarLogout;

    private Button currentActiveButton = null;

    @FXML
    public void initialize() {
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
        btnSidebarUpdateRequest.setVisible(false);
        btnSidebarUpdateRequest.setManaged(false);

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
            btnSidebarUpdateRequest.setVisible(true);
            btnSidebarUpdateRequest.setManaged(true);

            handleNavigateToProfile();
        }
    }

    private void setupHoverAnimations() {
        Button[] buttons = {
                btnSidebarDashboard, btnSidebarUserManagement, btnSidebarRequests,
                btnSidebarReports, btnSidebarAuditLog, btnSidebarSettings,
                btnSidebarGateScreen, btnSidebarActivityLog, btnSidebarStudentsDevices,
                btnSidebarProfile, btnSidebarDeviceLog, btnSidebarUpdateRequest,
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
                btnSidebarProfile, btnSidebarDeviceLog, btnSidebarUpdateRequest,
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
        //loadSubView("/com/example/byodsystem/byod/fxml/usermanagement.fxml");
    }

    @FXML
    public void handleNavigateToReports() {
        updateActiveButtonStyle(btnSidebarReports);
        //loadSubView("/com/example/byodsystem/byod/fxml/reports.fxml");
    }

    @FXML
    public void handleNavigateToAuditLog() {
        updateActiveButtonStyle(btnSidebarAuditLog);
        //loadSubView("/com/example/byodsystem/byod/fxml/auditlog.fxml");
    }

    @FXML
    public void handleNavigateToSettings() {
        updateActiveButtonStyle(btnSidebarSettings);
       // loadSubView("/com/example/byodsystem/byod/fxml/settings.fxml");
    }

    @FXML
    public void handleNavigateToGateScreen() {
        updateActiveButtonStyle(btnSidebarGateScreen);
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
        loadSubView("/com/example/byodsystem/byod/fxml/studentsdevices.fxml");
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
    public void handleNavigateToUpdateRequest() {
        updateActiveButtonStyle(btnSidebarUpdateRequest);
        loadSubView("/com/example/byodsystem/byod/fxml/studentupdaterequest.fxml");
    }

    @FXML
    public void handleNavigateToChangePassword() {
        updateActiveButtonStyle(btnSidebarChangePassword);
        loadSubView("/com/example/byodsystem/byod/fxml/changepassword.fxml");
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
            int studentRefId = (session.getStudentRefId() != null) ? session.getStudentRefId() : 0;

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
        updateActiveButtonStyle(btnSidebarLogout);
        try {
            UserSession.getInstance().cleanSession();
            Parent loginRoot = FXMLLoader.load(getClass().getResource("/com/example/byodsystem/byod/fxml/login.fxml"));
            Stage stage = (Stage) mainContainer.getScene().getWindow();
            stage.setScene(new Scene(loginRoot));
        } catch (IOException e) {
            System.err.println("Fatal error navigating back to Login View");
            e.printStackTrace();
        }
    }
}