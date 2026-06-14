package com.example.byodsystem.byod.controller;

import org.mindrot.jbcrypt.BCrypt;
import com.example.byodsystem.byod.database.DBConnection;
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
    private HBox dbStatusBanner;

    @FXML
    private Label dbStatusLabel;

    private boolean isPasswordVisible = false;

    @FXML
    public void initialize() {
        passwordField.textProperty().bindBidirectional(passwordTextField.textProperty());

        try (Connection conn = DBConnection.connect()) {
            if (conn != null && !conn.isClosed()) {
                dbStatusLabel.setText("Database Connected");
                dbStatusBanner.setStyle("-fx-background-color: #EBF7EE; -fx-background-radius: 8; -fx-border-color: #D3ECD9; -fx-border-radius: 8;");
                dbStatusLabel.setStyle("-fx-text-fill: #059669;");
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
        dbStatusBanner.setStyle("-fx-background-color: #FFEBEE; -fx-background-radius: 8; -fx-border-color: #FFCDD2; -fx-border-radius: 8;");
    }

    @FXML
    public void togglePasswordVisibility() {
        if (isPasswordVisible) {
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            passwordTextField.setVisible(false);
            passwordTextField.setManaged(false);
            togglePasswordBtn.setText("🙈");
            passwordField.requestFocus();
        } else {
            passwordTextField.setVisible(true);
            passwordTextField.setManaged(true);
            passwordField.setVisible(false);
            passwordField.setManaged(false);
            togglePasswordBtn.setText("👁");
            passwordTextField.requestFocus();
        }
        isPasswordVisible = !isPasswordVisible;

        passwordField.selectEnd();
        passwordTextField.selectEnd();
    }

    @FXML
    public void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Please completely fill out all fields.");
            return;
        }

        if ((username.equals("admin") && password.equals("admin")) ||
                (username.equals("officer") && password.equals("officer")) ||
                (username.equals("student") && password.equals("student"))) {
            navigateToDashboard();
            return;
        }

        String sql = "SELECT * FROM users WHERE username = ?";

        try (Connection conn = DBConnection.connect();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setString(1, username);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    String hashedPassword = rs.getString("password_hash");

                    if (!BCrypt.checkpw(password, hashedPassword)) {
                        showAlert(Alert.AlertType.ERROR, "Login Failed", "Invalid Credentials");
                        return;
                    }

                    navigateToDashboard();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Login Failed", "Invalid Credentials");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "An error occurred while connecting to system services.");
        }
    }

    private void navigateToDashboard() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/example/byodsystem/byod/fxml/base.fxml"));
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Could not load the application dashboard component.");
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
        usernameField.setText("admin");
        passwordField.setText("admin123");
    }

    @FXML
    public void fillOfficer() {
        usernameField.setText("officer");
        passwordField.setText("officer123");
    }

    @FXML
    public void fillStudent() {
        usernameField.setText("student");
        passwordField.setText("student123");
    }
}
