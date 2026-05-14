package com.example.byodsystem.byod.controller;

import com.example.byodsystem.byod.model.Student;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class StudentController {

    @FXML
    private TextField studentIdField;

    @FXML
    private TextField nameField;

    @FXML
    private TextField courseField;

    @FXML
    private ComboBox<String> yearComboBox;

    @FXML
    private TextField searchField;

    @FXML
    private TableView<Student> studentTable;

    @FXML
    private TableColumn<Student, String> colStudentId;

    @FXML
    private TableColumn<Student, String> colName;

    @FXML
    private TableColumn<Student, String> colCourse;

    @FXML
    private TableColumn<Student, String> colYear;

    private final ObservableList<Student> students = FXCollections.observableArrayList();

    @FXML
    public void initialize() {

        yearComboBox.getItems().addAll(
                "1st Year",
                "2nd Year",
                "3rd Year",
                "4th Year"
        );

        colStudentId.setCellValueFactory(data -> data.getValue().studentIdProperty());
        colName.setCellValueFactory(data -> data.getValue().nameProperty());
        colCourse.setCellValueFactory(data -> data.getValue().courseProperty());
        colYear.setCellValueFactory(data -> data.getValue().yearProperty());

        studentTable.setItems(students);
    }

    @FXML
    private void addStudent() {

        if (!validateInputs()) return;

        Student student = new Student(
                studentIdField.getText(),
                nameField.getText(),
                courseField.getText(),
                yearComboBox.getValue()
        );

        students.add(student);
        studentTable.setItems(students);

        showAlert("Success", "Student Added Successfully.");
        clearFields();
    }

    @FXML
    private void updateStudent() {

        Student selected = studentTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert("Error", "Select a student first.");
            return;
        }

        int index = studentTable.getSelectionModel().getSelectedIndex();

        Student updated = new Student(
                studentIdField.getText(),
                nameField.getText(),
                courseField.getText(),
                yearComboBox.getValue()
        );

        students.set(index, updated);
        studentTable.refresh();

        showAlert("Success", "Student Updated.");
    }

    @FXML
    private void deleteStudent() {

        Student selected = studentTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert("Error", "Select a student first.");
            return;
        }

        students.remove(selected);

        showAlert("Success", "Student Deleted.");
    }

    @FXML
    private void searchStudent() {

        String keyword = searchField.getText().toLowerCase();

        ObservableList<Student> filtered = FXCollections.observableArrayList();

        for (Student s : students) {
            if (s.getStudentId().toLowerCase().contains(keyword) ||
                    s.getName().toLowerCase().contains(keyword) ||
                    s.getCourse().toLowerCase().contains(keyword)) {
                filtered.add(s);
            }
        }

        studentTable.setItems(filtered);
    }

    @FXML
    private void clearFields() {

        studentIdField.clear();
        nameField.clear();
        courseField.clear();
        yearComboBox.setValue(null);
    }

    private boolean validateInputs() {

        if (studentIdField.getText().isEmpty()
                || nameField.getText().isEmpty()
                || courseField.getText().isEmpty()
                || yearComboBox.getValue() == null) {

            showAlert("Error", "Please complete all fields.");
            return false;
        }
        return true;
    }

    private void showAlert(String title, String message) {

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}