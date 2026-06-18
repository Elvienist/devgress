package com.example.byodsystem.byod.utils;

import com.example.byodsystem.byod.controller.AlertModalController;
import com.example.byodsystem.byod.controller.AlertModalController.AlertType;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.effect.GaussianBlur;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

public class AlertHelper {

    private static final String FXML_PATH = "/com/example/byodsystem/byod/fxml/alert-modal.fxml";

    public static void showPositive(Window owner, String title, String message) {
        show(owner, AlertType.POSITIVE, title, message, null);
    }

    public static void showNegative(Window owner, String title, String message) {
        show(owner, AlertType.NEGATIVE, title, message, null);
    }

    public static void showConfirm(Window owner, String title, String message, Runnable onConfirm) {
        show(owner, AlertType.CONFIRM, title, message, onConfirm);
    }

    private static void show(Window owner, AlertType type, String title, String message, Runnable onConfirm) {
        try {
            FXMLLoader loader = new FXMLLoader(AlertHelper.class.getResource(FXML_PATH));
            Parent root = loader.load();

            AlertModalController controller = loader.getController();
            controller.setup(type, title, message, onConfirm);

            // Blur the owner window content
            if (owner instanceof Stage ownerStage && ownerStage.getScene() != null) {
                ownerStage.getScene().getRoot().setEffect(new GaussianBlur(6));
            }

            Stage stage = new Stage();
            stage.initOwner(owner);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initStyle(StageStyle.TRANSPARENT);
            stage.setResizable(false);
            stage.setWidth(owner.getWidth());
            stage.setHeight(owner.getHeight());
            stage.setX(owner.getX());
            stage.setY(owner.getY());

            Scene scene = new Scene(root, owner.getWidth(), owner.getHeight());
            scene.setFill(null);
            stage.setScene(scene);
            stage.showAndWait();

            // Remove blur after close
            if (owner instanceof Stage ownerStage && ownerStage.getScene() != null) {
                ownerStage.getScene().getRoot().setEffect(null);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}