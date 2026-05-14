package com.example.byodsystem.byod.controller;

import com.example.byodsystem.byod.model.DeviceLog;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class ReportController {

    @FXML
    private DatePicker startDatePicker;

    @FXML
    private DatePicker endDatePicker;

    @FXML
    private TableView<DeviceLog> reportTable;

    @FXML
    private TableColumn<DeviceLog, String> colLogId;

    @FXML
    private TableColumn<DeviceLog, String> colDeviceId;

    @FXML
    private TableColumn<DeviceLog, String> colStudentId;

    @FXML
    private TableColumn<DeviceLog, String> colTimeIn;

    @FXML
    private TableColumn<DeviceLog, String> colTimeOut;

    private final ObservableList<DeviceLog> allLogs = FXCollections.observableArrayList();

    @FXML
    public void initialize() {

        colLogId.setCellValueFactory(data -> data.getValue().logIdProperty());
        colDeviceId.setCellValueFactory(data -> data.getValue().deviceIdProperty());
        colStudentId.setCellValueFactory(data -> data.getValue().studentIdProperty());
        colTimeIn.setCellValueFactory(data -> data.getValue().timeInProperty());
        colTimeOut.setCellValueFactory(data -> data.getValue().timeOutProperty());

        reportTable.setItems(allLogs);
    }

    @FXML
    private void generateReport() {

        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();

        if (start == null || end == null) {
            showAlert("Error", "Please select date range.");
            return;
        }

        ObservableList<DeviceLog> filtered = FXCollections.observableArrayList();

        for (DeviceLog log : allLogs) {

            LocalDateTime timeIn = LocalDateTime.parse(log.getTimeIn());
            LocalDate date = timeIn.toLocalDate();

            if ((date.isEqual(start) || date.isAfter(start)) &&
                    (date.isEqual(end) || date.isBefore(end))) {

                filtered.add(log);
            }
        }

        reportTable.setItems(filtered);

        showAlert("Success", "Report Generated.");
    }

    @FXML
    private void exportCSV() {

        System.out.println("Exporting CSV... (to be implemented with FileWriter)");
        showAlert("Info", "CSV Export feature ready for JDBC/file integration.");
    }

    private void showAlert(String title, String message) {

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}