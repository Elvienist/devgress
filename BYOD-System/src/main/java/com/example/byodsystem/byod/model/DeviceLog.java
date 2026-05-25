package com.example.byodsystem.byod.model;

public class DeviceLog {
    private int logId;
    private String studentName;
    private String studentId;
    private String deviceDetails;
    private String serialNumber;
    private String timestamp;
    private String direction;

    public DeviceLog(int logId, String studentName, String studentId, String deviceDetails, String serialNumber, String timestamp, String direction) {
        this.logId = logId;
        this.studentName = studentName;
        this.studentId = studentId;
        this.deviceDetails = deviceDetails;
        this.serialNumber = serialNumber;
        this.timestamp = timestamp;
        this.direction = direction;
    }

    public int getLogId() { return logId; }
    public String getStudentName() { return studentName; }
    public String getStudentId() { return studentId; }
    public String getDeviceDetails() { return deviceDetails; }
    public String getSerialNumber() { return serialNumber; }
    public String getTimestamp() { return timestamp; }
    public String getDirection() { return direction; }
    public String getMetaRow() { return studentId + " • " + deviceDetails + "\n" + serialNumber; }
}