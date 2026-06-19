-- =============================================================
-- BYOD Registration and Monitoring System
-- schema.sql
-- Run this file first before indexes.sql, seed.sql, and rls_policies.sql
-- Hosted on Supabase (PostgreSQL)
-- =============================================================


-- =============================================================
-- TABLE: students
-- Must be created before users (users references students)
-- =============================================================

CREATE TABLE students (
    student_id       SERIAL PRIMARY KEY,
    student_code     VARCHAR(20) UNIQUE NOT NULL,
    full_name        VARCHAR(100) NOT NULL,
    course           VARCHAR(100) NOT NULL,
    year_level       VARCHAR(20) NOT NULL,
    contact_number   VARCHAR(20),
    status           VARCHAR(20) DEFAULT 'ACTIVE'
                     CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


-- =============================================================
-- TABLE: users
-- Stores all system accounts: Admin, Officer, Student
-- student_ref_id is NULL for Admin and Officer accounts
-- password_hash must NEVER be returned in application queries
-- =============================================================

CREATE TABLE users (
    user_id          SERIAL PRIMARY KEY,
    username         VARCHAR(50) UNIQUE NOT NULL,
    full_name        VARCHAR(100) NOT NULL,
    role             VARCHAR(20) NOT NULL
                     CHECK (role IN ('ADMIN', 'OFFICER', 'STUDENT')),
    password_hash    VARCHAR(255) NOT NULL,
    student_ref_id   VARCHAR(20)
                     REFERENCES students(student_code)
                     ON DELETE SET NULL,
    status           VARCHAR(20) DEFAULT 'ACTIVE'
                     CHECK (status IN ('ACTIVE', 'INACTIVE')),
    first_login      BOOLEAN DEFAULT TRUE,
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login       TIMESTAMP
);


-- =============================================================
-- TABLE: devices
-- Each device is linked to one student via owner_id
-- ON DELETE RESTRICT prevents deleting a student with devices
-- =============================================================

CREATE TABLE devices (
    device_id        SERIAL PRIMARY KEY,
    serial_number    VARCHAR(100) UNIQUE NOT NULL,
    brand            VARCHAR(50) NOT NULL,
    model            VARCHAR(100) NOT NULL,
    device_type      VARCHAR(20) NOT NULL
                     CHECK (device_type IN ('LAPTOP', 'TABLET', 'PHONE', 'OTHER')),
    owner_id         INT NOT NULL
                     REFERENCES students(student_id)
                     ON DELETE RESTRICT,
    status           VARCHAR(20) DEFAULT 'ACTIVE'
                     CHECK (status IN ('ACTIVE', 'INACTIVE')),
    current_location VARCHAR(10) DEFAULT 'UNKNOWN'
                     CHECK (current_location IN ('IN', 'OUT', 'UNKNOWN')),
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


-- =============================================================
-- TABLE: device_logs
-- Records every ingress and egress gate event
-- Original entries are NEVER modified — see log_amendments
-- batch_id groups all devices confirmed in one gate action
-- =============================================================

CREATE TABLE device_logs (
    log_id           SERIAL PRIMARY KEY,
    device_id        INT NOT NULL
                     REFERENCES devices(device_id)
                     ON DELETE RESTRICT,
    operator_id      INT NOT NULL
                     REFERENCES users(user_id)
                     ON DELETE RESTRICT,
    direction        VARCHAR(3) NOT NULL
                     CHECK (direction IN ('IN', 'OUT')),
    log_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status           VARCHAR(20) DEFAULT 'NORMAL'
                     CHECK (status IN ('NORMAL', 'AMENDED', 'VOIDED')),
    batch_id         UUID NOT NULL
);


-- =============================================================
-- TABLE: log_amendments
-- Tracks corrections made beyond the correction time boundary
-- previous_data stores a JSON snapshot of the original log entry
-- =============================================================

CREATE TABLE log_amendments (
    amendment_id     SERIAL PRIMARY KEY,
    original_log_id  INT NOT NULL
                     REFERENCES device_logs(log_id)
                     ON DELETE RESTRICT,
    amendment_type   VARCHAR(10) NOT NULL
                     CHECK (amendment_type IN ('EDIT', 'VOID')),
    changed_by       INT NOT NULL
                     REFERENCES users(user_id)
                     ON DELETE RESTRICT,
    reason           TEXT NOT NULL,
    previous_data    JSONB,
    amended_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


-- =============================================================
-- TABLE: reports
-- Metadata archive for generated reports
-- Actual report data lives in the exported CSV file
-- =============================================================

CREATE TABLE reports (
    report_id        SERIAL PRIMARY KEY,
    report_title     VARCHAR(150) NOT NULL,
    report_type      VARCHAR(50) NOT NULL,
    generated_by     INT NOT NULL
                     REFERENCES users(user_id)
                     ON DELETE RESTRICT,
    date_start       DATE NOT NULL,
    date_end         DATE NOT NULL,
    total_records    INT DEFAULT 0,
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


-- =============================================================
-- TABLE: report_templates
-- Saved report configurations for admin reuse
-- ON DELETE CASCADE: templates are removed if creator is deleted
-- =============================================================

CREATE TABLE report_templates (
    template_id      SERIAL PRIMARY KEY,
    template_name    VARCHAR(100) NOT NULL,
    report_type      VARCHAR(50) NOT NULL,
    created_by       INT NOT NULL
                     REFERENCES users(user_id)
                     ON DELETE CASCADE,
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


-- =============================================================
-- TABLE: profile_update_requests
-- Student-submitted requests to update their own profile fields
-- Changes are applied only after admin approval
-- admin_response is required for REJECTED status (enforced at app level)
-- =============================================================

CREATE TABLE profile_update_requests (
    request_id       SERIAL PRIMARY KEY,
    student_id       INT NOT NULL
                     REFERENCES students(student_id)
                     ON DELETE CASCADE,
    field_name       VARCHAR(50) NOT NULL,
    current_value    VARCHAR(255),
    requested_value  VARCHAR(255) NOT NULL,
    reason           TEXT NOT NULL,
    status           VARCHAR(20) DEFAULT 'PENDING'
                     CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    admin_response   TEXT,
    submitted_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    resolved_at      TIMESTAMP,
    resolved_by      INT REFERENCES users(user_id)
                     ON DELETE SET NULL
);


-- =============================================================
-- TABLE: audit_log
-- Insert-only record of all system actions
-- No UPDATE or DELETE is ever permitted on this table
-- Enforced via Supabase RLS policy in rls_policies.sql
-- =============================================================

CREATE TABLE audit_log (
    audit_id         SERIAL PRIMARY KEY,
    operator_id      INT NOT NULL
                     REFERENCES users(user_id)
                     ON DELETE RESTRICT,
    action_type      VARCHAR(50) NOT NULL
                     CHECK (action_type IN (
                         'LOGIN', 'LOGOUT',
                         'RECORD_CREATED', 'RECORD_EDITED', 'RECORD_DELETED',
                         'RECORD_DEACTIVATED', 'RECORD_REACTIVATED',
                         'REPORT_GENERATED', 'CORRECTION_MADE', 'VOID_MADE',
                         'PASSWORD_CHANGED', 'PASSWORD_RESET',
                         'REQUEST_APPROVED', 'REQUEST_REJECTED',
                         'SETTINGS_CHANGED'
                     )),
    target_type      VARCHAR(50),
    target_id        INT,
    details          JSONB,
    performed_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


-- =============================================================
-- TABLE: settings
-- Single-row system configuration table
-- Only one record should ever exist
-- correction_window_min controls the gate correction time boundary
-- =============================================================

CREATE TABLE settings (
    setting_id            SERIAL PRIMARY KEY,
    institution_name      VARCHAR(150) NOT NULL,
    academic_year         VARCHAR(20) NOT NULL,
    correction_window_min INT NOT NULL DEFAULT 15,
    end_of_day_time       TIME NOT NULL DEFAULT '20:00:00',
    updated_by            INT REFERENCES users(user_id)
                          ON DELETE SET NULL,
    updated_at            TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
