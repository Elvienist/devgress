-- =============================================================
-- BYOD Registration and Monitoring System
-- seed.sql
-- Run this file after schema.sql and indexes.sql
--
-- IMPORTANT: Replace <bcrypt_hash_here> with an actual bcrypt
-- hash of your chosen admin password BEFORE running this file.
-- Never insert a plaintext password.
--
-- To generate a bcrypt hash:
--   Option A: Use an online tool such as bcrypt-generator.com
--   Option B: Run a one-time Java utility using org.mindrot.jbcrypt:
--             String hash = BCrypt.hashpw("yourpassword", BCrypt.gensalt());
--             System.out.println(hash);
-- =============================================================


-- =============================================================
-- Default system settings (required — exactly one row)
-- Update institution_name and academic_year to match your school
-- =============================================================

INSERT INTO settings (
    institution_name,
    academic_year,
    correction_window_min,
    end_of_day_time
)
VALUES (
    'Your Institution Name',
    '2024-2025',
    15,
    '20:00:00'
);


-- =============================================================
-- Default admin account
-- Username: admin
-- Password: set by you — hash it first, paste hash below
-- first_login is TRUE — admin must change password on first login
-- =============================================================

INSERT INTO users (
    username,
    full_name,
    role,
    password_hash,
    first_login
)
VALUES (
    'admin',
    'System Administrator',
    'ADMIN',
    '$2a$12$HRU4OnvV3d9gA71yrKsqMufW.ko8nTYOSRcXrb44f4D.7O7NXSP3e',
    TRUE
);
