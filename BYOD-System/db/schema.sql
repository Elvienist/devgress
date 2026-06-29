-- =============================================================
-- BYOD Registration and Monitoring System
-- schema_updated.sql
-- Run this file first before indexes.sql, seed.sql, and rls_policies.sql
-- Hosted on Supabase (PostgreSQL)
-- =============================================================


-- =============================================================
-- TABLE: students
-- =============================================================

CREATE TABLE students (
                          student_id       SERIAL PRIMARY KEY,
                          student_code     VARCHAR(20) UNIQUE NOT NULL,
                          full_name        VARCHAR(100) NOT NULL,
                          section          VARCHAR(50) NOT NULL,
                          email            VARCHAR(150) NOT NULL,
                          status           VARCHAR(20) DEFAULT 'ACTIVE'
                              CHECK (status IN ('ACTIVE', 'INACTIVE')),
                          created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          updated_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


-- =============================================================
-- TABLE: users
-- student_ref_id is NULL for Admin and Officer accounts
-- password_hash must NEVER be returned in application queries
-- =============================================================

CREATE TABLE users (
                       user_id          SERIAL PRIMARY KEY,
                       username         VARCHAR(50) UNIQUE NOT NULL,
                       full_name        VARCHAR(100) NOT NULL,
                       role             VARCHAR(20) NOT NULL
                           CHECK (role IN ('ADMIN', 'OFFICER')),
                       password_hash    VARCHAR(255) NOT NULL,
                       status           VARCHAR(20) DEFAULT 'ACTIVE'
                           CHECK (status IN ('ACTIVE', 'INACTIVE')),
                       first_login      BOOLEAN DEFAULT TRUE,
                       created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       last_login       TIMESTAMP
);


-- =============================================================
-- TABLE: devices
-- =============================================================

CREATE TABLE devices (
                         device_id        SERIAL PRIMARY KEY,
                         serial_number    VARCHAR(100) UNIQUE NOT NULL,
                         brand            VARCHAR(50) NOT NULL,
                         model            VARCHAR(100) NOT NULL,
                         device_type      VARCHAR(20) NOT NULL,
                         owner_id         INT NOT NULL
                             REFERENCES students(student_id)
                                 ON DELETE RESTRICT,
                         current_location VARCHAR(10) DEFAULT 'OUT'
                             CHECK (current_location IN ('IN', 'OUT', 'UNKNOWN')),
                         status           VARCHAR(20) DEFAULT 'ACTIVE'
                             CHECK (status IN ('ACTIVE', 'INACTIVE')),
                         created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         updated_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


-- =============================================================
-- TABLE: gate_requests
-- Admin creates a scheduled ingress/egress request.
-- Officer at the gate approves (or rejects) it.
-- Once approved the actual device_logs rows are inserted and
-- official_time_in / official_time_out are stamped on each log.
-- =============================================================

CREATE TABLE gate_requests (
                               request_id       SERIAL PRIMARY KEY,
                               student_id       INT NOT NULL
                                   REFERENCES students(student_id)
                                       ON DELETE RESTRICT,
                               direction        VARCHAR(3) NOT NULL
                                   CHECK (direction IN ('IN', 'OUT')),
                               scheduled_time   TIMESTAMP NOT NULL,
                               requested_by     INT NOT NULL
                                   REFERENCES users(user_id)
                                       ON DELETE RESTRICT,
                               approved_by      INT
                                   REFERENCES users(user_id)
                                                        ON DELETE SET NULL,
                               status           VARCHAR(20) DEFAULT 'PENDING'
                                   CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
                               rejection_reason TEXT,
                               created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                               resolved_at      TIMESTAMP
);


-- =============================================================
-- TABLE: gate_request_devices
-- Links each gate request to the specific devices included.
-- =============================================================

CREATE TABLE gate_request_devices (
                                      id               SERIAL PRIMARY KEY,
                                      request_id       INT NOT NULL
                                          REFERENCES gate_requests(request_id)
                                              ON DELETE CASCADE,
                                      device_id        INT NOT NULL
                                          REFERENCES devices(device_id)
                                              ON DELETE RESTRICT,
                                      UNIQUE (request_id, device_id)
);


-- =============================================================
-- TABLE: device_logs
-- Records every ingress and egress gate event.
-- scheduled_time  — the time originally requested by the admin.
-- official_time_in / official_time_out — stamped by the officer
--   when they approve the gate request at the physical gate.
-- Original entries are NEVER modified — see log_amendments.
-- batch_id groups all devices confirmed in one gate action.
-- =============================================================

CREATE TABLE device_logs (
                             log_id              SERIAL PRIMARY KEY,
                             device_id           INT NOT NULL
                                 REFERENCES devices(device_id)
                                     ON DELETE RESTRICT,
                             operator_id         INT NOT NULL
                                 REFERENCES users(user_id)
                                     ON DELETE RESTRICT,
                             direction           VARCHAR(3) NOT NULL
                                 CHECK (direction IN ('IN', 'OUT')),
                             log_time            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             scheduled_time      TIMESTAMP,
                             official_time_in    TIMESTAMP,
                             official_time_out   TIMESTAMP,
                             status              VARCHAR(20) DEFAULT 'NORMAL'
                                 CHECK (status IN ('NORMAL', 'AMENDED', 'VOIDED')),
                             batch_id            UUID NOT NULL,
                             gate_request_id     INT
                                 REFERENCES gate_requests(request_id)
                                                         ON DELETE SET NULL
);


-- =============================================================
-- TABLE: log_amendments
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
-- TABLE: audit_log
-- Insert-only. No UPDATE or DELETE ever permitted.
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
                                                      'GATE_REQUEST_CREATED', 'GATE_REQUEST_APPROVED',
                                                      'GATE_REQUEST_REJECTED',
                                                      'SETTINGS_CHANGED'
                                   )),
                           target_type      VARCHAR(50),
                           target_id        INT,
                           details          JSONB,
                           performed_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


-- =============================================================
-- TABLE: settings
-- =============================================================

CREATE TABLE settings (
                          setting_id            SERIAL PRIMARY KEY,
                          institution_name      VARCHAR(150) NOT NULL,
                          academic_year         VARCHAR(20) NOT NULL,
                          correction_window_min INT NOT NULL DEFAULT 15,
                          start_of_day_time        TIME NOT NULL DEFAULT '08:00:00',
                          end_of_day_time       TIME NOT NULL DEFAULT '20:00:00',
                          updated_by            INT REFERENCES users(user_id)
                                                                 ON DELETE SET NULL,
                          updated_at            TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


-- =============================================================
-- MIGRATION NOTES (run these if upgrading an existing database)
-- =============================================================
--
-- ALTER TABLE students ADD COLUMN IF NOT EXISTS email VARCHAR(150);
--
-- ALTER TABLE devices ADD COLUMN IF NOT EXISTS current_location VARCHAR(10)
--     DEFAULT 'OUT' CHECK (current_location IN ('IN', 'OUT', 'UNKNOWN'));
--
-- ALTER TABLE device_logs
--     ADD COLUMN IF NOT EXISTS scheduled_time    TIMESTAMP,
--     ADD COLUMN IF NOT EXISTS official_time_in  TIMESTAMP,
--     ADD COLUMN IF NOT EXISTS official_time_out TIMESTAMP,
--     ADD COLUMN IF NOT EXISTS gate_request_id   INT
--         REFERENCES gate_requests(request_id) ON DELETE SET NULL;
--
-- =============================================================
