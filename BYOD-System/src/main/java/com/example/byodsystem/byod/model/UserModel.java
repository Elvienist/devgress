package com.example.byodsystem.byod.model;

public class UserModel {
    private int userId;
    private String username;
    private String fullName;
    private String role;
    private String passwordHash;
    private boolean firstLogin;
    private String status;

    public UserModel(int userId, String username, String fullName, String role,
                     String passwordHash, boolean firstLogin, String status) {
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.role = role;
        this.passwordHash = passwordHash;
        this.firstLogin = firstLogin;
        this.status = status;
    }

    public int getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getRole() { return role; }
    public String getPasswordHash() { return passwordHash; }
    public boolean isFirstLogin() { return firstLogin; }
    public String getStatus() { return status; }
}