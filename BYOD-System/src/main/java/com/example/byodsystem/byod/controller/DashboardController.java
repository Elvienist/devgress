package com.example.byodsystem.byod.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

public class DashboardController {

    @FXML
    private StackPane mainPane;

    @FXML
    public void initialize() {
        loadView("/com/example/byodsystem/byod/Students.fxml");
    }

    @FXML
    private void openStudents() {
        loadView("/com/example/byodsystem/byod/Students.fxml");
    }

    @FXML
    private void openDevices() {
        loadView("/com/example/byodsystem/byod/Devices.fxml");
    }

    @FXML
    private void openMonitoring() {
        loadView("/com/example/byodsystem/byod/Monitoring.fxml");
    }

    @FXML
    private void openReports() {
        loadView("/com/example/byodsystem/byod/Reports.fxml");
    }

    private void loadView(String fxmlPath) {
        try {
            Node view = FXMLLoader.load(getClass().getResource(fxmlPath));
            mainPane.getChildren().clear();
            mainPane.getChildren().add(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
