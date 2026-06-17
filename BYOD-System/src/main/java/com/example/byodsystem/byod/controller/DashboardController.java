package com.example.byodsystem.byod.controller;

import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.TextField;

public class DashboardController {

    @FXML
    private BarChart<String, Number> entryPatternChart;

    @FXML
    private TextField searchField;

    @FXML
    public void initialize() {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.getData().add(new XYChart.Data<>("7 AM", 20));
        series.getData().add(new XYChart.Data<>("8 AM", 175));
        series.getData().add(new XYChart.Data<>("9 AM", 120));
        series.getData().add(new XYChart.Data<>("10 AM", 40));
        series.getData().add(new XYChart.Data<>("11 AM", 15));
        series.getData().add(new XYChart.Data<>("12 PM", 50));
        series.getData().add(new XYChart.Data<>("1 PM", 45));
        series.getData().add(new XYChart.Data<>("2 PM", 60));
        series.getData().add(new XYChart.Data<>("3 PM", 85));
        series.getData().add(new XYChart.Data<>("4 PM", 130));
        series.getData().add(new XYChart.Data<>("5 PM", 160));
        series.getData().add(new XYChart.Data<>("6 PM", 110));

        entryPatternChart.getData().add(series);
    }

    @FXML
    public void handleSearch() {
        if (searchField != null && !searchField.getText().isEmpty()) {
            System.out.println("Executing search parameter query logic: " + searchField.getText());
        }
    }

    @FXML
    public void handleReviewRequests() {
        System.out.println("Forwarding to local instance review routine...");
    }
}
