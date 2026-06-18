package com.example.byodsystem.byod.controller;

import org.mindrot.jbcrypt.BCrypt;
import com.example.byodsystem.byod.database.DBConnection;
import com.example.byodsystem.byod.service.UserSession;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.stage.Window;
import com.example.byodsystem.byod.utils.AlertHelper;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ChangePasswordController {

    @FXML private PasswordField currentPasswordField;
    @FXML private TextField currentPasswordTextField;
    @FXML private Button toggleCurrentPasswordBtn;

    @FXML private PasswordField newPasswordField;
    @FXML private TextField newPasswordTextField;
    @FXML private Button toggleNewPasswordBtn;

    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField confirmPasswordTextField;
    @FXML private Button toggleConfirmPasswordBtn;

    @FXML private Label errorLabel;

    private boolean isCurrentVisible = false;
    private boolean isNewVisible = false;
    private boolean isConfirmVisible = false;

    @FXML
    public void initialize() {
        clearInlineError();
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
    public void toggleCurrentPasswordVisibility() {
        if (isCurrentVisible) {
            currentPasswordField.setText(currentPasswordTextField.getText());
            currentPasswordField.setVisible(true);
            currentPasswordField.setManaged(true);
            currentPasswordTextField.setVisible(false);
            currentPasswordTextField.setManaged(false);
            toggleCurrentPasswordBtn.setText("🙈");
            currentPasswordField.requestFocus();
            currentPasswordField.selectEnd();
        } else {
            currentPasswordTextField.setText(currentPasswordField.getText());
            currentPasswordTextField.setVisible(true);
            currentPasswordTextField.setManaged(true);
            currentPasswordField.setVisible(false);
            currentPasswordField.setManaged(false);
            toggleCurrentPasswordBtn.setText("👁");
            currentPasswordTextField.requestFocus();
            currentPasswordTextField.selectEnd();
        }
        isCurrentVisible = !isCurrentVisible;
    }

    @FXML
    public void toggleNewPasswordVisibility() {
        if (isNewVisible) {
            newPasswordField.setText(newPasswordTextField.getText());
            newPasswordField.setVisible(true);
            newPasswordField.setManaged(true);
            newPasswordTextField.setVisible(false);
            newPasswordTextField.setManaged(false);
            toggleNewPasswordBtn.setText("🙈");
            newPasswordField.requestFocus();
            newPasswordField.selectEnd();
        } else {
            newPasswordTextField.setText(newPasswordField.getText());
            newPasswordTextField.setVisible(true);
            newPasswordTextField.setManaged(true);
            newPasswordField.setVisible(false);
            newPasswordField.setManaged(false);
            toggleNewPasswordBtn.setText("👁");
            newPasswordTextField.requestFocus();
            newPasswordTextField.selectEnd();
        }
        isNewVisible = !isNewVisible;
    }

    @FXML
    public void toggleConfirmPasswordVisibility() {
        if (isConfirmVisible) {
            confirmPasswordField.setText(confirmPasswordTextField.getText());
            confirmPasswordField.setVisible(true);
            confirmPasswordField.setManaged(true);
            confirmPasswordTextField.setVisible(false);
            confirmPasswordTextField.setManaged(false);
            toggleConfirmPasswordBtn.setText("🙈");
            confirmPasswordField.requestFocus();
            confirmPasswordField.selectEnd();
        } else {
            confirmPasswordTextField.setText(confirmPasswordField.getText());
            confirmPasswordTextField.setVisible(true);
            confirmPasswordTextField.setManaged(true);
            confirmPasswordField.setVisible(false);
            confirmPasswordField.setManaged(false);
            toggleConfirmPasswordBtn.setText("👁");
            confirmPasswordTextField.requestFocus();
            confirmPasswordTextField.selectEnd();
        }
        isConfirmVisible = !isConfirmVisible;
    }

    @FXML
    public void handleUpdatePassword() {
        clearInlineError();

        String currentPass = currentPasswordField.isVisible() ? currentPasswordField.getText() : currentPasswordTextField.getText();
        String newPass = newPasswordField.isVisible() ? newPasswordField.getText() : newPasswordTextField.getText();
        String confirmPass = confirmPasswordField.isVisible() ? confirmPasswordField.getText() : confirmPasswordTextField.getText();

        currentPass = (currentPass == null) ? "" : currentPass.trim();
        newPass = (newPass == null) ? "" : newPass.trim();
        confirmPass = (confirmPass == null) ? "" : confirmPass.trim();

        if (currentPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
            showInlineError("All fields are required.");
            return;
        }

        if (!newPass.matches("^(?=.*[a-zA-Z])(?=.*[0-9]).{8,}$")) {
            showInlineError("Invalid password format. Must include letters and numbers, min 8 characters.");
            return;
        }

        if (!newPass.equals(confirmPass)) {
            showInlineError("New password and confirmation do not match.");
            currentPasswordField.clear();
            currentPasswordTextField.clear();
            newPasswordField.clear();
            newPasswordTextField.clear();
            confirmPasswordField.clear();
            confirmPasswordTextField.clear();
            return;
        }

        String query = "SELECT user_id, password_hash FROM users";

        try (Connection conn = DBConnection.connect();
             PreparedStatement pst = conn.prepareStatement(query);
             ResultSet rs = pst.executeQuery()) {

            int targetUserId = -1;
            boolean userFound = false;

            while (rs.next()) {
                String storedHash = rs.getString("password_hash");
                if (BCrypt.checkpw(currentPass, storedHash)) {
                    targetUserId = rs.getInt("user_id");
                    userFound = true;
                    break;
                }
            }

            if (!userFound) {
                showInlineError("Incorrect current password.");
                return;
            }

            if (updateUserPassword(targetUserId, newPass)) {
                logAuditEvent(targetUserId, "PASSWORD_CHANGED", "USER", targetUserId, "{\"message\": \"Account password successfully modified.\"}");

                UserSession.getInstance().setFirstLogin(false);

                Window owner = confirmPasswordTextField.getScene().getWindow();
                AlertHelper.showPositive(owner, "Success", "Password updated successfully! Please log in again with your new credentials.");
                currentPasswordField.clear();
                currentPasswordTextField.clear();
                newPasswordField.clear();
                newPasswordTextField.clear();
                confirmPasswordField.clear();
                confirmPasswordTextField.clear();

                navigateToLogin();
            } else {
                showInlineError("Failed to update password in database.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            Window owner = confirmPasswordTextField.getScene().getWindow();
            AlertHelper.showNegative(owner, "Database Error", "An error occurred while connecting to system services.");
        }
    }

    public boolean updateUserPassword(int userId, String newPlainPassword) {
        String sql = "UPDATE users SET password_hash = ?, first_login = FALSE WHERE user_id = ?";
        String hashedNewPassword = BCrypt.hashpw(newPlainPassword, BCrypt.gensalt());

        try (Connection conn = DBConnection.connect();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, hashedNewPassword);
            pst.setInt(2, userId);
            return pst.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void logAuditEvent(int operatorId, String actionType, String targetType, Integer targetId, String jsonDetails) {
        String sql = "INSERT INTO audit_log (operator_id, action_type, target_type, target_id, details, performed_at) VALUES (?, ?, ?, ?, ?::jsonb, NOW() AT TIME ZONE 'Asia/Manila')";
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

    private void navigateToLogin() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/example/byodsystem/byod/fxml/login.fxml"));
            Stage stage = (Stage) currentPasswordField.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
            Window owner = confirmPasswordTextField.getScene().getWindow();
            AlertHelper.showNegative(owner, "Navigation Error", "Could not return to the login user interface component.");
        }
    }
}