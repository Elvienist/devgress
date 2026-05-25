package com.example.byodsystem.byod.model;

public class Student {
    private int studentRecordId;
    private String studentId;
    private String fullName;
    private String course;
    private String yearLevel;
    private String contactNumber;
    private int deviceCount;

    public Student(int studentRecordId, String studentId, String fullName, String course, String yearLevel, String contactNumber, int deviceCount) {
        this.studentRecordId = studentRecordId;
        this.studentId = studentId;
        this.fullName = fullName;
        this.course = course;
        this.yearLevel = yearLevel;
        this.contactNumber = contactNumber;
        this.deviceCount = deviceCount;
    }

    public int getStudentRecordId() { return studentRecordId; }
    public String getStudentId() { return studentId; }
    public String getFullName() { return fullName; }
    public String getCourse() { return course; }
    public String getYearLevel() { return yearLevel; }
    public String getContactNumber() { return contactNumber; }
    public int getDeviceCount() { return deviceCount; }
}