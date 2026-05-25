package com.example.byodsystem.byod.controller;

import com.example.byodsystem.byod.database.DBConnection;
import com.example.byodsystem.byod.model.Student;
import com.example.byodsystem.byod.utils.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class StudentController {
    @FXML private Button btnDashboard, btnStudents, btnDevices, btnLogs, btnActivity, btnReports, btnSettings, btnLogout;
    @FXML private Label lblDate, lblAdminName, lblAdminRole;
    @FXML private TextField txtStudentID, txtFullName, txtCourse, txtYearLevel, txtContact, txtSearch;
    @FXML private Button btnSave, btnClear;

    @FXML private TableView<Student> tblStudents;
    @FXML private TableColumn<Student, String> colID, colName, colCourse, colYear, colContact;
    @FXML private TableColumn<Student, Integer> colDevices;

    private ObservableList<Student> studentList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        lblAdminName.setText(SessionManager.currentOperatorName);
        lblAdminRole.setText(SessionManager.currentOperatorRole);
        lblDate.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, MMM dd, yyyy")));
        setupSidebarActions();
        configureTableColumns();
        loadStudents("");

        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> loadStudents(newVal.trim()));
        btnSave.setOnAction(e -> saveStudent());
        btnClear.setOnAction(e -> clearForm());
    }

    private void configureTableColumns() {
        colID.setCellValueFactory(new PropertyValueFactory<>("studentId"));
        colName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colCourse.setCellValueFactory(new PropertyValueFactory<>("course"));
        colYear.setCellValueFactory(new PropertyValueFactory<>("yearLevel"));
        colContact.setCellValueFactory(new PropertyValueFactory<>("contactNumber"));
        colDevices.setCellValueFactory(new PropertyValueFactory<>("deviceCount"));
    }

    private void loadStudents(String searchKeyword) {
        studentList.clear();
        String query = "SELECT s.*, (SELECT COUNT(*) FROM devices d WHERE d.owner_id = s.student_record_id) AS dev_count " +
                "FROM students s WHERE s.role = 'STUDENT' AND (s.student_id ILIKE ? OR s.full_name ILIKE ? OR s.course ILIKE ?)";

        try (Connection conn = DBConnection.connect(); PreparedStatement ps = conn.prepareStatement(query)) {
            String wildcard = "%" + searchKeyword + "%";
            ps.setString(1, wildcard);
            ps.setString(2, wildcard);
            ps.setString(3, wildcard);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    studentList.add(new Student(
                            rs.getInt("student_record_id"),
                            rs.getString("student_id"),
                            rs.getString("full_name"),
                            rs.getString("course"),
                            rs.getString("year_level"),
                            rs.getString("contact_number"),
                            rs.getInt("dev_count")
                    ));
                }
            }
            tblStudents.setItems(studentList);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveStudent() {
        String query = "INSERT INTO students (student_id, full_name, course, year_level, contact_number, email, password_hash, role) " +
                "VALUES (?, ?, ?, ?, ?, ?, 'student123', 'STUDENT') " +
                "ON CONFLICT (student_id) DO UPDATE SET full_name=?, course=?, year_level=?, contact_number=?";

        try (Connection conn = DBConnection.connect(); PreparedStatement ps = conn.prepareStatement(query)) {
            String id = txtStudentID.getText().trim();
            String name = txtFullName.getText().trim();
            String crs = txtCourse.getText().trim();
            String yr = txtYearLevel.getText().trim();
            String cont = txtContact.getText().trim();
            String mockEmail = name.toLowerCase().replace(" ", "") + "@univ.edu.ph";

            ps.setString(1, id);
            ps.setString(2, name);
            ps.setString(3, crs);
            ps.setString(4, yr);
            ps.setString(5, cont);
            ps.setString(6, mockEmail);
            ps.setString(7, name);
            ps.setString(8, crs);
            ps.setString(9, yr);
            ps.setString(10, cont);

            ps.executeUpdate();
            clearForm();
            loadStudents("");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void clearForm() {
        txtStudentID.clear();
        txtFullName.clear();
        txtCourse.clear();
        txtYearLevel.clear();
        txtContact.clear();
    }

    private void setupSidebarActions() {
        btnDashboard.setOnAction(e -> SessionManager.navigate(btnDashboard, "dashboard.fxml"));
        btnStudents.setOnAction(e -> SessionManager.navigate(btnStudents, "students.fxml"));
        btnDevices.setOnAction(e -> SessionManager.navigate(btnDevices, "devices.fxml"));
        btnLogs.setOnAction(e -> SessionManager.navigate(btnLogs, "monitoring.fxml"));
        btnActivity.setOnAction(e -> SessionManager.navigate(btnActivity, "reports.fxml"));
        btnReports.setOnAction(e -> SessionManager.navigate(btnReports, "reports.fxml"));
        btnLogout.setOnAction(e -> SessionManager.navigate(btnLogout, "login.fxml"));
    }
}