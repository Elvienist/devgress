package com.example.byodsystem.byod.service;

import com.example.byodsystem.byod.dao.AuthDAO;
import com.example.byodsystem.byod.model.User;
import com.example.byodsystem.byod.utils.AuditLogger;
import org.mindrot.jbcrypt.BCrypt;

public class AuthService {

    private final AuthDAO authDAO;
    private final AuditLogger auditLogger;

    public AuthService() {
        this.authDAO = new AuthDAO();
        this.auditLogger = new AuditLogger();
    }

    public enum PasswordChangeResult {
        SUCCESS, MISMATCH, INVALID_FORMAT, WRONG_CURRENT
    }

    public User authenticate(String username, String password) {
        User user = authDAO.getByUsername(username);

        if (user == null) {
            return null;
        }

        if (!BCrypt.checkpw(password, user.getPasswordHash())) {
            return null;
        }

        authDAO.updateLastLogin(user.getUserId());
        auditLogger.log(user.getUserId(), "LOGIN", null, null, null);

        return user;
    }

    public PasswordChangeResult changePassword(int userId, String currentPassword, String newPassword, String confirmPassword) {
        if (!newPassword.equals(confirmPassword)) {
            return PasswordChangeResult.MISMATCH;
        }

        if (!isValidPassword(newPassword)) {
            return PasswordChangeResult.INVALID_FORMAT;
        }

        User user = authDAO.getById(userId);

        if (currentPassword != null) {
            if (!BCrypt.checkpw(currentPassword, user.getPasswordHash())) {
                return PasswordChangeResult.WRONG_CURRENT;
            }
        }

        String newHash = BCrypt.hashpw(newPassword, BCrypt.gensalt());

        authDAO.updatePassword(userId, newHash);
        authDAO.clearFirstLogin(userId);

        auditLogger.log(userId, "PASSWORD_CHANGED", "users", String.valueOf(userId), null);

        return PasswordChangeResult.SUCCESS;
    }

    private boolean isValidPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }

        boolean hasLetter = false;
        boolean hasNumber = false;

        for (char c : password.toCharArray()) {
            if (Character.isLetter(c)) hasLetter = true;
            if (Character.isDigit(c)) hasNumber = true;
        }

        return hasLetter && hasNumber;
    }

    public boolean testConnection() {
        return com.example.byodsystem.byod.database.DBConnection.testConnection();
    }
}