package com.example.byodsystem.byod.model;

public class ActivityLog {

    private int logId;
    private int deviceId;
    private int studentId;
    private String deviceLabel;
    private String studentName;
    private String timeIn;
    private String timeOut;
    private String status;
    private String amendReason;
    private String voidReason;

    public ActivityLog(int logId, int deviceId, int studentId, String deviceLabel, String studentName,
                       String timeIn, String timeOut, String status, String amendReason, String voidReason) {
        this.logId = logId;
        this.deviceId = deviceId;
        this.studentId = studentId;
        this.deviceLabel = deviceLabel;
        this.studentName = studentName;
        this.timeIn = timeIn;
        this.timeOut = timeOut;
        this.status = status;
        this.amendReason = amendReason;
        this.voidReason = voidReason;
    }

    public int getLogId() { return logId; }
    public int getDeviceId() { return deviceId; }
    public int getStudentId() { return studentId; }
    public String getDeviceLabel() { return deviceLabel; }
    public String getStudentName() { return studentName; }
    public String getTimeIn() { return timeIn; }
    public String getTimeOut() { return timeOut; }
    public String getStatus() { return status; }
    public String getAmendReason() { return amendReason; }
    public String getVoidReason() { return voidReason; }

    public void setTimeIn(String timeIn) { this.timeIn = timeIn; }
    public void setTimeOut(String timeOut) { this.timeOut = timeOut; }
    public void setStatus(String status) { this.status = status; }
}
