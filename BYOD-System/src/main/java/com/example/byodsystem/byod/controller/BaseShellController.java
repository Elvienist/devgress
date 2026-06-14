package com.example.byodsystem.byod.controller;

import com.example.byodsystem.byod.service.UserSession;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import java.io.IOException;

public class BaseShellController {

    @FXML
    private BorderPane mainContainer;

    @FXML
    private StackPane contentArea;

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

    @FXML
    public void initialize() {
        UserSession session = UserSession.getInstance();
        String role = session.getRole() != null ? session.getRole().toUpperCase() : "";

        configureSidebarAccess(role);

        if (session.isFirstLogin()) {
            handleNavigateToChangePassword();
        } else {
            routeDefaultViewByRole(role);
        }
    }

    private void configureSidebarAccess(String role) {
        setButtonVisibility(btnSidebarDashboard, false);
        setButtonVisibility(btnSidebarUserManagement, false);
        setButtonVisibility(btnSidebarRequests, false);
        setButtonVisibility(btnSidebarReports, false);
        setButtonVisibility(btnSidebarAuditLog, false);
        setButtonVisibility(btnSidebarSettings, false);
        setButtonVisibility(btnSidebarGateScreen, false);
        setButtonVisibility(btnSidebarActivityLog, false);
        setButtonVisibility(btnSidebarStudentsDevices, false);
        setButtonVisibility(btnSidebarProfile, false);
        setButtonVisibility(btnSidebarDeviceLog, false);
        setButtonVisibility(btnSidebarUpdateRequest, false);

        setButtonVisibility(btnSidebarChangePassword, true);
        setButtonVisibility(btnSidebarLogout, true);

        switch (role) {
            case "ADMIN":
                setButtonVisibility(btnSidebarDashboard, true);
                setButtonVisibility(btnSidebarUserManagement, true);
                setButtonVisibility(btnSidebarRequests, true);
                setButtonVisibility(btnSidebarReports, true);
                setButtonVisibility(btnSidebarAuditLog, true);
                setButtonVisibility(btnSidebarSettings, true);
                break;

            case "OFFICER":
                setButtonVisibility(btnSidebarGateScreen, true);
                setButtonVisibility(btnSidebarActivityLog, true);
                setButtonVisibility(btnSidebarStudentsDevices, true);
                break;

            case "STUDENT":
                setButtonVisibility(btnSidebarProfile, true);
                setButtonVisibility(btnSidebarDeviceLog, true);
                setButtonVisibility(btnSidebarUpdateRequest, true);
                break;

            default:
                break;
        }
    }

    private void setButtonVisibility(Button btn, boolean isVisible) {
        if (btn != null) {
            btn.setVisible(isVisible);
            btn.setManaged(isVisible);
        }
    }

    private void routeDefaultViewByRole(String role) {
        switch (role) {
            case "ADMIN":
                handleNavigateToDashboardContent();
                break;
            case "OFFICER":
                handleNavigateToGateScreen();
                break;
            case "STUDENT":
                handleNavigateToProfile();
                break;
            default:
                handleNavigateToDashboardContent();
                break;
        }
    }

    private void updateActiveButtonStyle(Button activeTarget) {
        Button[] navigationButtons = {
                btnSidebarDashboard, btnSidebarUserManagement, btnSidebarRequests, btnSidebarReports,
                btnSidebarAuditLog, btnSidebarSettings, btnSidebarGateScreen, btnSidebarActivityLog,
                btnSidebarStudentsDevices, btnSidebarProfile, btnSidebarDeviceLog, btnSidebarUpdateRequest,
                btnSidebarChangePassword, btnSidebarLogout
        };

        for (Button btn : navigationButtons) {
            if (btn != null) {
                btn.setStyle("-fx-background-color: transparent; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 0 0 0 15; -fx-text-fill: #495057;");
            }
        }

        if (activeTarget != null) {
            activeTarget.setStyle("-fx-background-color: #7A0000; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 0 0 0 15; -fx-text-fill: #FFFFFF;");
        }
    }

    private void changeSubView(String fxmlPath, Button targetButton) {
        updateActiveButtonStyle(targetButton);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();

            if (contentArea != null) {
                contentArea.getChildren().clear();
                contentArea.getChildren().add(view);
            } else {
                System.err.println("Error: contentArea StackPane is null. Check fx:id in base.fxml");
            }
        } catch (IOException e) {
            System.err.println("Fatal: Could not transition sub-view panel: " + fxmlPath);
            e.printStackTrace();
        }
    }

    @FXML
    public void handleNavigateToDashboardContent() {
        changeSubView("/com/example/byodsystem/byod/fxml/dashboard.fxml", btnSidebarDashboard);
    }

    @FXML
    public void handleNavigateToUserManagement() {
       // changeSubView("/com/example/byodsystem/byod/fxml/usermanagement.fxml", btnSidebarUserManagement);
    }

    @FXML
    public void handleNavigateToRequests() {
        //changeSubView("/com/example/byodsystem/byod/fxml/requests.fxml", btnSidebarRequests);
    }

    @FXML
    public void handleNavigateToReports() {
        //changeSubView("/com/example/byodsystem/byod/fxml/reports.fxml", btnSidebarReports);
    }

    @FXML
    public void handleNavigateToAuditLog() {
       // changeSubView("/com/example/byodsystem/byod/fxml/auditlog.fxml", btnSidebarAuditLog);
    }

    @FXML
    public void handleNavigateToSettings() {
       // changeSubView("/com/example/byodsystem/byod/fxml/settings.fxml", btnSidebarSettings);
    }

    @FXML
    public void handleNavigateToGateScreen() {
        changeSubView("/com/example/byodsystem/byod/fxml/gatescreen.fxml", btnSidebarGateScreen);
    }

    @FXML
    public void handleNavigateToActivityLog() {
        //changeSubView("/com/example/byodsystem/byod/fxml/activitylog.fxml", btnSidebarActivityLog);
    }

    @FXML
    public void handleNavigateToStudentsDevices() {
        //changeSubView("/com/example/byodsystem/byod/fxml/studentsdevices.fxml", btnSidebarStudentsDevices);
    }

    @FXML
    public void handleNavigateToProfile() {
        changeSubView("/com/example/byodsystem/byod/fxml/studentprofile.fxml", btnSidebarProfile);
    }

    @FXML
    public void handleNavigateToDeviceLog() {
       // changeSubView("/com/example/byodsystem/byod/fxml/devicelog.fxml", btnSidebarDeviceLog);
    }

    @FXML
    public void handleNavigateToUpdateRequest() {
       // changeSubView("/com/example/byodsystem/byod/fxml/updaterequest.fxml", btnSidebarUpdateRequest);
    }

    @FXML
    public void handleNavigateToChangePassword() {
        changeSubView("/com/example/byodsystem/byod/fxml/changepassword.fxml", btnSidebarChangePassword);
    }

    @FXML
    public void handleNavigateToProfileRequests() {
        updateActiveButtonStyle(btnSidebarRequests);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/byodsystem/byod/fxml/studentprofilerequest.fxml"));
            Parent view = loader.load();

            StudentProfileRequestController controller = loader.getController();
            UserSession session = UserSession.getInstance();
            controller.initializeSession(session.getUserId(), session.isFirstLogin(), session.getStudentRefId());

            if (contentArea != null) {
                contentArea.getChildren().clear();
                contentArea.getChildren().add(view);
            }
        } catch (IOException e) {
            System.err.println("Fatal: Could not transition profile request panel.");
            e.printStackTrace();
        }
    }

    @FXML
    public void handleNavigateToStudentProfile() {
        handleNavigateToProfile();
    }

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
