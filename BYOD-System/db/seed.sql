-- Insert default settings
INSERT INTO settings (institution_name, academic_year, correction_window_min, end_of_day_time)
VALUES ('Your Institution Name', '2024-2025', 15, '20:00:00');

-- Insert default admin user
-- Generate bcrypt hash externally before inserting
INSERT INTO users (username, full_name, role, password_hash, first_login)
VALUES ('admin', 'System Administrator', 'ADMIN', '<bcrypt_hash_here>', TRUE);
