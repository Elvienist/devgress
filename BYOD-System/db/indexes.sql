-- =============================================================
-- BYOD Registration and Monitoring System
-- indexes.sql
-- Run this file after schema.sql
-- =============================================================


-- =============================================================
-- device_logs
-- Most queried table in the system — indexed heavily
-- =============================================================

CREATE INDEX idx_device_logs_device_id   ON device_logs(device_id);
CREATE INDEX idx_device_logs_log_time    ON device_logs(log_time);
CREATE INDEX idx_device_logs_batch_id    ON device_logs(batch_id);
CREATE INDEX idx_device_logs_status      ON device_logs(status);


-- =============================================================
-- devices
-- Lookup by serial number and by owner
-- =============================================================

CREATE INDEX idx_devices_owner_id        ON devices(owner_id);
CREATE INDEX idx_devices_serial          ON devices(serial_number);


-- =============================================================
-- students
-- Lookup by student code (used in Gate Screen search)
-- =============================================================

CREATE INDEX idx_students_code           ON students(student_code);


-- =============================================================
-- audit_log
-- Filtered by operator and by time range in Audit Log screen
-- =============================================================

CREATE INDEX idx_audit_log_operator      ON audit_log(operator_id);
CREATE INDEX idx_audit_log_performed_at  ON audit_log(performed_at);


-- =============================================================
-- profile_update_requests
-- Filtered by student and by status in request screens
-- =============================================================

CREATE INDEX idx_requests_student_id     ON profile_update_requests(student_id);
CREATE INDEX idx_requests_status         ON profile_update_requests(status);
