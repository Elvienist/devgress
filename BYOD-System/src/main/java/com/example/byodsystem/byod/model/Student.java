package com.example.byodsystem.byod.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Student {

    private final StringProperty studentId;
    private final StringProperty name;
    private final StringProperty course;
    private final StringProperty year;

    public Student(String studentId, String name, String course, String year) {
        this.studentId = new SimpleStringProperty(studentId);
        this.name = new SimpleStringProperty(name);
        this.course = new SimpleStringProperty(course);
        this.year = new SimpleStringProperty(year);
    }

    public String getStudentId() { return studentId.get(); }
    public String getName() { return name.get(); }
    public String getCourse() { return course.get(); }
    public String getYear() { return year.get(); }

    public StringProperty studentIdProperty() { return studentId; }
    public StringProperty nameProperty() { return name; }
    public StringProperty courseProperty() { return course; }
    public StringProperty yearProperty() { return year; }
}