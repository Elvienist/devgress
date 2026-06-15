package com.example.byodsystem.byod.controller;

import org.mindrot.jbcrypt.BCrypt;
import com.example.byodsystem.byod.database.DBConnection;
import com.example.byodsystem.byod.service.UserSession;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private TextField passwordTextField;

    @FXML
    private Button togglePasswordBtn;

    @FXML
    private Button loginButton;

    @FXML
    private HBox dbStatusBanner;

    @FXML
    private Label dbStatusLabel;

    @FXML
    private Label errorLabel;

    private boolean isPasswordVisible = false;

    @FXML
    public void initialize() {
        passwordField.textProperty().bindBidirectional(passwordTextField.textProperty());

        clearInlineError();

        try (Connection conn = DBConnection.connect()) {
            if (conn != null && !conn.isClosed()) {
                dbStatusLabel.setText("Database Connected");
                dbStatusBanner.setStyle(
                        "-fx-background-color: #EBF7EE; " +
                                "-fx-background-radius: 8; " +
                                "-fx-border-color: #D3ECD9; " +
                                "-fx-border-radius: 8;"
                );
                dbStatusLabel.setStyle("-fx-text-fill: #059669;");
                if (loginButton != null) {
                    loginButton.setDisable(false);
                }
            } else {
                setDatabaseFailedStatus();
            }
        } catch (Exception e) {
            setDatabaseFailedStatus();
        }
    }

    private void setDatabaseFailedStatus() {
        dbStatusLabel.setText("Database Connection Failed");
        dbStatusLabel.setStyle("-fx-text-fill: #C62828;");
        dbStatusBanner.setStyle(
                "-fx-background-color: #FFEBEE; " +
                        "-fx-background-radius: 8; " +
                        "-fx-border-color: #FFCDD2; " +
                        "-fx-border-radius: 8;"
        );
        if (loginButton != null) {
            loginButton.setDisable(true);
        }
    }

    private void showInlineError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void clearInlineError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    @FXML
    public void togglePasswordVisibility() {
        if (isPasswordVisible) {
            passwordField.setVisible(true);
            passwordField.setManaged(true);

            passwordTextField.setVisible(false);
            passwordTextField.setManaged(false);

            togglePasswordBtn.setText("👁");
            passwordField.requestFocus();
        } else {
            passwordTextField.setVisible(true);
            passwordTextField.setManaged(true);

            passwordField.setVisible(false);
            passwordField.setManaged(false);

            togglePasswordBtn.setText("🙈");
            passwordTextField.requestFocus();
        }

        isPasswordVisible = !isPasswordVisible;

        passwordField.selectEnd();
        passwordTextField.selectEnd();
    }

    @FXML
    public void handleLogin() {
        clearInlineError();

        String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText().trim();

        if (username.isEmpty() && password.isEmpty()) {
            showInlineError("Please completely fill out all fields.");
            usernameField.requestFocus();
            return;
        }

        if (username.isEmpty()) {
            showInlineError("Username cannot be empty.");
            usernameField.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            showInlineError("Password cannot be empty.");
            passwordField.requestFocus();
            return;
        }

        String sql = "SELECT user_id, password_hash, role, first_login, student_ref_id, status FROM users WHERE username = ?";

        try (Connection conn = DBConnection.connect();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setString(1, username);

            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    int userId = rs.getInt("user_id");
                    String hashedPassword = rs.getString("password_hash");
                    String userStatus = rs.getString("status");
                    String role = rs.getString("role");
                    boolean isFirstLogin = rs.getBoolean("first_login");
                    Integer studentRefId = rs.getObject("student_ref_id") != null ? rs.getInt("student_ref_id") : null;

                    if ("INACTIVE".equalsIgnoreCase(userStatus)) {
                        showInlineError("This account has been deactivated.");
                        return;
                    }

                    if (!BCrypt.checkpw(password, hashedPassword)) {
                        showInlineError("Incorrect password.");
                        passwordField.clear();
                        passwordTextField.clear();
                        passwordField.requestFocus();
                        return;
                    }

                    UserSession session = UserSession.getInstance();
                    session.setUserId(userId);
                    session.setUsername(username);
                    session.setRole(role);
                    session.setFirstLogin(isFirstLogin);
                    session.setStudentRefId(studentRefId);

                    logAuditEvent(userId, "LOGIN", "USER", userId, "{\"message\": \"User logged in successfully.\"}");

                    updateLastLoginTimestamp(userId);

                    navigateToDashboard();

                } else {
                    showInlineError("Username not found.");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(
                    Alert.AlertType.ERROR,
                    "Database Error",
                    "An error occurred while connecting to system services."
            );
        }
    }

    private void updateLastLoginTimestamp(int userId) {
        String sql = "UPDATE users SET last_login = CURRENT_TIMESTAMP WHERE user_id = ?";
        try (Connection conn = DBConnection.connect();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, userId);
            pst.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Failed to record login timestamp: " + e.getMessage());
        }
    }

    private void logAuditEvent(int operatorId, String actionType, String targetType, Integer targetId, String jsonDetails) {
        String sql = "INSERT INTO audit_log (operator_id, action_type, target_type, target_id, details) VALUES (?, ?, ?, ?, ?::jsonb)";
        try (Connection conn = DBConnection.connect();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setInt(1, operatorId);
            pst.setString(2, actionType);

            if (targetType != null) {
                pst.setString(3, targetType);
            } else {
                pst.setNull(3, java.sql.Types.VARCHAR);
            }

            if (targetId != null) {
                pst.setInt(4, targetId);
            } else {
                pst.setNull(4, java.sql.Types.INTEGER);
            }

            if (jsonDetails != null) {
                pst.setString(5, jsonDetails);
            } else {
                pst.setNull(5, java.sql.Types.OTHER);
            }

            pst.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Audit Log Failed: " + e.getMessage());
        }
    }

    private void navigateToDashboard() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/example/byodsystem/byod/fxml/base.fxml"));
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(
                    Alert.AlertType.ERROR,
                    "Navigation Error",
                    "Could not load the application dashboard component."
            );
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    public void fillAdmin() {
        clearInlineError();
        usernameField.setText("admin");
        passwordField.setText("admin123");
    }

    @FXML
    public void fillOfficer() {
        clearInlineError();
        usernameField.setText("officer");
        passwordField.setText("officer1234");
    }

    @FXML
    public void fillStudent() {
        clearInlineError();
        usernameField.setText("2024-00001");
        passwordField.setText("user2024-00001");
    }
}