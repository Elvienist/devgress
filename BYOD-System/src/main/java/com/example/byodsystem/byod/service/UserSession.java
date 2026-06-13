package com.example.byodsystem.byod.service;

public class UserSession {
    private static UserSession instance;

    private int userId;
    private String username;
    private String role;
    private Integer studentRefId;
    private boolean isFirstLogin;

    private UserSession() {}

    public static synchronized UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Integer getStudentRefId() { return studentRefId; }
    public void setStudentRefId(Integer studentRefId) { this.studentRefId = studentRefId; }

    public boolean isFirstLogin() { return isFirstLogin; }
    public void setFirstLogin(boolean firstLogin) { this.isFirstLogin = firstLogin; }

    public void cleanSession() {
        userId = 0;
        username = null;
        role = null;
        studentRefId = null;
        isFirstLogin = false;
    }
}