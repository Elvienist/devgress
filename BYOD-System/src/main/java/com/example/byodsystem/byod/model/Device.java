package com.example.byodsystem.byod.model;

public class Device {
    private int deviceId;
    private String serialNumber;
    private String brand;
    private String model;
    private String deviceType;
    private int ownerId;
    private String status;

    private String ownerName;


    public Device(int deviceId, String serialNumber, String brand, String model, String deviceType, int ownerId, String status) {
        this.deviceId = deviceId;
        this.serialNumber = serialNumber;
        this.brand = brand;
        this.model = model;
        this.deviceType = deviceType;
        this.ownerId = ownerId;
        this.status = status;
    }

    // --- GETTERS AND SETTERS ---
    public int getDeviceId() { return deviceId; }
    public void setDeviceId(int deviceId) { this.deviceId = deviceId; }

    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }

    public int getOwnerId() { return ownerId; }
    public void setOwnerId(int ownerId) { this.ownerId = ownerId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
}