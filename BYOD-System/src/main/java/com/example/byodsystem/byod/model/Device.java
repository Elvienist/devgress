package com.example.byodsystem.byod.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Device {

    private final StringProperty deviceId;
    private final StringProperty studentId;
    private final StringProperty serial;
    private final StringProperty type;
    private final StringProperty brand;
    private final StringProperty model;

    public Device(String deviceId, String studentId, String serial,
                  String type, String brand, String model) {

        this.deviceId = new SimpleStringProperty(deviceId);
        this.studentId = new SimpleStringProperty(studentId);
        this.serial = new SimpleStringProperty(serial);
        this.type = new SimpleStringProperty(type);
        this.brand = new SimpleStringProperty(brand);
        this.model = new SimpleStringProperty(model);
    }

    public String getDeviceId() { return deviceId.get(); }
    public String getStudentId() { return studentId.get(); }
    public String getSerial() { return serial.get(); }
    public String getType() { return type.get(); }
    public String getBrand() { return brand.get(); }
    public String getModel() { return model.get(); }

    public StringProperty deviceIdProperty() { return deviceId; }
    public StringProperty studentIdProperty() { return studentId; }
    public StringProperty serialProperty() { return serial; }
    public StringProperty typeProperty() { return type; }
    public StringProperty brandProperty() { return brand; }
    public StringProperty modelProperty() { return model; }
}