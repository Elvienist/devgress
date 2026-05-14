package com.example.byodsystem.byod.controller;

import com.example.byodsystem.byod.model.Device;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class DeviceController {

    @FXML
    private TextField deviceIdField;

    @FXML
    private TextField studentIdField;

    @FXML
    private TextField brandField;

    @FXML
    private TextField modelField;

    @FXML
    private TextField serialField;

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> deviceTypeCombo;

    @FXML
    private TableView<Device> deviceTable;

    @FXML
    private TableColumn<Device, String> colDeviceId;

    @FXML
    private TableColumn<Device, String> colStudentId;

    @FXML
    private TableColumn<Device, String> colSerial;

    @FXML
    private TableColumn<Device, String> colType;

    @FXML
    private TableColumn<Device, String> colBrand;

    @FXML
    private TableColumn<Device, String> colModel;

    private final ObservableList<Device> devices = FXCollections.observableArrayList();

    @FXML
    public void initialize() {

        deviceTypeCombo.getItems().addAll("Laptop", "Tablet", "Phone");

        colDeviceId.setCellValueFactory(data -> data.getValue().deviceIdProperty());
        colStudentId.setCellValueFactory(data -> data.getValue().studentIdProperty());
        colSerial.setCellValueFactory(data -> data.getValue().serialProperty());
        colType.setCellValueFactory(data -> data.getValue().typeProperty());
        colBrand.setCellValueFactory(data -> data.getValue().brandProperty());
        colModel.setCellValueFactory(data -> data.getValue().modelProperty());

        deviceTable.setItems(devices);
    }

    @FXML
    private void registerDevice() {

        if (!validateInputs()) return;

        Device device = new Device(
                generateId(),
                studentIdField.getText(),
                serialField.getText(),
                deviceTypeCombo.getValue(),
                brandField.getText(),
                modelField.getText()
        );

        devices.add(device);
        deviceTable.setItems(devices);

        showAlert("Success", "Device Registered Successfully.");
        clearFields();
    }

    @FXML
    private void updateDevice() {

        Device selected = deviceTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert("Error", "Select a device first.");
            return;
        }

        selected = new Device(
                selected.getDeviceId(),
                studentIdField.getText(),
                serialField.getText(),
                deviceTypeCombo.getValue(),
                brandField.getText(),
                modelField.getText()
        );

        int index = deviceTable.getSelectionModel().getSelectedIndex();
        devices.set(index, selected);

        deviceTable.refresh();
        showAlert("Success", "Device Updated.");
    }

    @FXML
    private void deleteDevice() {

        Device selected = deviceTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert("Error", "Select a device first.");
            return;
        }

        devices.remove(selected);
        showAlert("Success", "Device Deleted.");
    }

    @FXML
    private void searchDevice() {

        String keyword = searchField.getText().toLowerCase();

        ObservableList<Device> filtered = FXCollections.observableArrayList();

        for (Device d : devices) {
            if (d.getStudentId().toLowerCase().contains(keyword) ||
                    d.getSerial().toLowerCase().contains(keyword) ||
                    d.getDeviceId().toLowerCase().contains(keyword)) {
                filtered.add(d);
            }
        }

        deviceTable.setItems(filtered);
    }

    @FXML
    private void clearFields() {

        deviceIdField.clear();
        studentIdField.clear();
        brandField.clear();
        modelField.clear();
        serialField.clear();

        deviceTypeCombo.setValue(null);
    }

    private boolean validateInputs() {

        if (studentIdField.getText().isEmpty()
                || serialField.getText().isEmpty()
                || deviceTypeCombo.getValue() == null) {

            showAlert("Error", "Please complete all required fields.");
            return false;
        }
        return true;
    }

    private String generateId() {
        return "DEV-" + (devices.size() + 1);
    }

    private void showAlert(String title, String message) {

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}