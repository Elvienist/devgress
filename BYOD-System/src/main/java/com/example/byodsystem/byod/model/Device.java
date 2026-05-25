package com.example.byodsystem.byod.model;

public class Device {
    private int deviceId;
    private String serialNumber;
    private String brand;
    private String model;
    private String deviceType;
    private String ownerName;
    private String ownerStudentId;
    private String status;

    public Device(int deviceId, String serialNumber, String brand, String model, String deviceType, String ownerName, String ownerStudentId, String status) {
        this.deviceId = deviceId;
        this.serialNumber = serialNumber;
        this.brand = brand;
        this.model = model;
        this.deviceType = deviceType;
        this.ownerName = ownerName;
        this.ownerStudentId = ownerStudentId;
        this.status = status;
    }

    public int getDeviceId() { return deviceId; }
    public String getSerialNumber() { return serialNumber; }
    public String getBrand() { return brand; }
    public String getModel() { return model; }
    public String getDeviceType() { return deviceType; }
    public String getOwnerName() { return ownerName; }
    public String getOwnerStudentId() { return ownerStudentId; }
    public String getStatus() { return status; }
    public String getDeviceInfo() { return brand + " " + model + "\n" + deviceType; }
    public String getOwnerDetails() { return ownerName + "\n" + ownerStudentId; }
}