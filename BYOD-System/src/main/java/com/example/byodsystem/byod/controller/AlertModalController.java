package com.example.byodsystem.byod.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;

public class AlertModalController {

    @FXML private StackPane iconCircle;
    @FXML private SVGPath iconPath;
    @FXML private Label lblTitle;
    @FXML private Label lblMessage;
    @FXML private Button btnCancel;
    @FXML private Button btnAction;

    public enum AlertType { POSITIVE, NEGATIVE, CONFIRM }

    private Runnable onConfirm;

    public void setup(AlertType type, String title, String message, Runnable onConfirm) {
        lblTitle.setText(title);
        lblMessage.setText(message);
        this.onConfirm = onConfirm;

        switch (type) {
            case POSITIVE -> {
                iconPath.setContent("M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 14.5l-4-4 1.41-1.41L10 13.67l6.59-6.59L18 8.5l-8 8z");
                iconPath.setFill(Color.web("#2E7D32"));
                iconCircle.setStyle("-fx-background-color: #E8F5E9; -fx-background-radius: 50%;");
                btnAction.setText("Close");
                btnAction.setStyle("-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 9 32;");
                btnCancel.setVisible(false);
                btnCancel.setManaged(false);
            }
            case NEGATIVE -> {
                iconPath.setContent("M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z");
                iconPath.setFill(Color.web("#DC2626"));
                iconCircle.setStyle("-fx-background-color: #FEE2E2; -fx-background-radius: 50%;");
                btnAction.setText("Close");
                btnAction.setStyle("-fx-background-color: #DC2626; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 9 32;");
                btnCancel.setVisible(false);
                btnCancel.setManaged(false);
            }
            case CONFIRM -> {
                iconPath.setContent("M1 21h22L12 2 1 21zm12-3h-2v-2h2v2zm0-4h-2v-4h2v4z");
                iconPath.setFill(Color.web("#D97706"));
                iconCircle.setStyle("-fx-background-color: #FEF3C7; -fx-background-radius: 50%;");
                btnAction.setText("Confirm");
                btnAction.setStyle("-fx-background-color: #7A0000; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 9 32;");
                btnCancel.setVisible(true);
                btnCancel.setManaged(true);
            }
        }
    }

    @FXML
    private void handleAction() {
        if (onConfirm != null) onConfirm.run();
        closeStage();
    }

    @FXML
    private void handleClose() {
        closeStage();
    }

    private void closeStage() {
        Stage stage = (Stage) btnAction.getScene().getWindow();
        stage.close();
    }
}