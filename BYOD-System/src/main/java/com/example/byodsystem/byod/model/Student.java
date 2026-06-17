package com.example.byodsystem.byod.model;

public class Student {
    private int studentId;
    private String studentCode;
    private String fullName;
    private String course;
    private String yearLevel;
    private String contactNumber;
    private String status;

    // UI-Specific Calculated Field (Fixes compilation error)
    private int devicesCount;

    // Existing Constructor
    public Student(int studentId, String studentCode, String fullName, String course, String yearLevel, String contactNumber, String status) {
        this.studentId = studentId;
        this.studentCode = studentCode;
        this.fullName = fullName;
        this.course = course;
        this.yearLevel = yearLevel;
        this.contactNumber = contactNumber;
        this.status = status;
    }

    // --- GETTERS AND SETTERS ---
    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }

    public String getStudentCode() { return studentCode; }
    public void setStudentCode(String studentCode) { this.studentCode = studentCode; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getCourse() { return course; }
    public void setCourse(String course) { this.course = course; }

    public String getYearLevel() { return yearLevel; }
    public void setYearLevel(String yearLevel) { this.yearLevel = yearLevel; }

    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    // UI-Specific Getter and Setter to satisfy Controller Factory
    public int getDevicesCount() { return devicesCount; }
    public void setDevicesCount(int devicesCount) { this.devicesCount = devicesCount; }
}