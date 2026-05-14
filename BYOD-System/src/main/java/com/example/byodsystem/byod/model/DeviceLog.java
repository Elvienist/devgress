package com.example.byodsystem.byod.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class DeviceLog {

    private final StringProperty logId;
    private final StringProperty deviceId;
    private final StringProperty studentId;
    private final StringProperty deviceType;
    private final StringProperty timeIn;
    private final StringProperty timeOut;

    public DeviceLog(String logId, String deviceId, String studentId,
                     String deviceType, String timeIn, String timeOut) {

        this.logId = new SimpleStringProperty(logId);
        this.deviceId = new SimpleStringProperty(deviceId);
        this.studentId = new SimpleStringProperty(studentId);
        this.deviceType = new SimpleStringProperty(deviceType);
        this.timeIn = new SimpleStringProperty(timeIn);
        this.timeOut = new SimpleStringProperty(timeOut);
    }

    public String getLogId() { return logId.get(); }
    public String getDeviceId() { return deviceId.get(); }
    public String getStudentId() { return studentId.get(); }
    public String getDeviceType() { return deviceType.get(); }
    public String getTimeIn() { return timeIn.get(); }
    public String getTimeOut() { return timeOut.get(); }

    public void setTimeOut(String value) {
        this.timeOut.set(value);
    }

    public StringProperty logIdProperty() { return logId; }
    public StringProperty deviceIdProperty() { return deviceId; }
    public StringProperty studentIdProperty() { return studentId; }
    public StringProperty deviceTypeProperty() { return deviceType; }
    public StringProperty timeInProperty() { return timeIn; }
    public StringProperty timeOutProperty() { return timeOut; }
}
