package com.example.byodsystem.byod.controller;

import com.example.byodsystem.byod.model.DeviceLog;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.LocalDateTime;

public class MonitoringController {

    @FXML
    private ComboBox<String> deviceTypeCombo;

    @FXML
    private TextField studentIdField;

    @FXML
    private TextField deviceIdField;

    @FXML
    private TextField searchField;

    @FXML
    private TableView<DeviceLog> logTable;

    @FXML
    private TableColumn<DeviceLog, String> colLogId;

    @FXML
    private TableColumn<DeviceLog, String> colDeviceId;

    @FXML
    private TableColumn<DeviceLog, String> colStudentId;

    @FXML
    private TableColumn<DeviceLog, String> colDeviceType;

    @FXML
    private TableColumn<DeviceLog, String> colTimeIn;

    @FXML
    private TableColumn<DeviceLog, String> colTimeOut;

    private final ObservableList<DeviceLog> logs = FXCollections.observableArrayList();

    @FXML
    public void initialize() {

        deviceTypeCombo.getItems().addAll("Laptop", "Tablet", "Phone");

        colLogId.setCellValueFactory(data -> data.getValue().logIdProperty());
        colDeviceId.setCellValueFactory(data -> data.getValue().deviceIdProperty());
        colStudentId.setCellValueFactory(data -> data.getValue().studentIdProperty());
        colDeviceType.setCellValueFactory(data -> data.getValue().deviceTypeProperty());
        colTimeIn.setCellValueFactory(data -> data.getValue().timeInProperty());
        colTimeOut.setCellValueFactory(data -> data.getValue().timeOutProperty());

        logTable.setItems(logs);
    }

    @FXML
    private void searchRecord() {
        String keyword = searchField.getText().toLowerCase();

        ObservableList<DeviceLog> filtered = FXCollections.observableArrayList();

        for (DeviceLog log : logs) {
            if (log.getStudentId().toLowerCase().contains(keyword) ||
                    log.getDeviceId().toLowerCase().contains(keyword)) {
                filtered.add(log);
            }
        }

        logTable.setItems(filtered);
    }

    @FXML
    private void timeIn() {

        if (!validateInputs()) return;

        DeviceLog log = new DeviceLog(
                generateId(),
                deviceIdField.getText(),
                studentIdField.getText(),
                deviceTypeCombo.getValue(),
                LocalDateTime.now().toString(),
                ""
        );

        logs.add(log);
        logTable.setItems(logs);

        showAlert("Success", "Device Time In Recorded.");
        clearFields();
    }

    @FXML
    private void timeOut() {

        DeviceLog selected = logTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert("Error", "Select a record first.");
            return;
        }

        selected.setTimeOut(LocalDateTime.now().toString());
        logTable.refresh();

        showAlert("Success", "Device Time Out Recorded.");
    }

    private boolean validateInputs() {

        if (studentIdField.getText().isEmpty() ||
                deviceIdField.getText().isEmpty() ||
                deviceTypeCombo.getValue() == null) {

            showAlert("Error", "Please fill all fields.");
            return false;
        }
        return true;
    }

    private void clearFields() {
        studentIdField.clear();
        deviceIdField.clear();
        deviceTypeCombo.getSelectionModel().clearSelection();
    }

    private String generateId() {
        return "LOG-" + (logs.size() + 1);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}