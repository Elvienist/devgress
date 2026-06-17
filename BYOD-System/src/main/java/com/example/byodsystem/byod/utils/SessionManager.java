package com.example.byodsystem.byod.utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.Button;
import java.io.IOException;

public class SessionManager {
    public static int currentOperatorId = 7;
    public static String currentOperatorName = "Admin User";
    public static String currentOperatorRole = "Security";

    public static void navigate(Button triggerButton, String fxmlName) {
        try {
            Stage stage = (Stage) triggerButton.getScene().getWindow();
            Parent root = FXMLLoader.load(SessionManager.class.getResource("/com/example/byodsystem/byod/fxml/" + fxmlName));
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}