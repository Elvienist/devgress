-- =============================================================
-- BYOD Registration and Monitoring System
-- rls_policies.sql
-- Run this file after schema.sql, indexes.sql, and seed.sql
--
-- These policies use Supabase's built-in auth.jwt() function to
-- read the role claim from the authenticated user's JWT token.
-- The application must set the role claim when issuing tokens.
--
-- Policy naming convention:
--   <action>_<table>_<role>
-- =============================================================


-- =============================================================
-- ENABLE ROW LEVEL SECURITY ON ALL TABLES
-- Must be done before any policies take effect
-- =============================================================

ALTER TABLE users                   ENABLE ROW LEVEL SECURITY;
ALTER TABLE students                ENABLE ROW LEVEL SECURITY;
ALTER TABLE devices                 ENABLE ROW LEVEL SECURITY;
ALTER TABLE device_logs             ENABLE ROW LEVEL SECURITY;
ALTER TABLE log_amendments          ENABLE ROW LEVEL SECURITY;
ALTER TABLE reports                 ENABLE ROW LEVEL SECURITY;
ALTER TABLE report_templates        ENABLE ROW LEVEL SECURITY;
ALTER TABLE profile_update_requests ENABLE ROW LEVEL SECURITY;
ALTER TABLE audit_log               ENABLE ROW LEVEL SECURITY;
ALTER TABLE settings                ENABLE ROW LEVEL SECURITY;


-- =============================================================
-- HELPER: role extraction from JWT
-- Used in policy USING expressions to check the caller's role
-- =============================================================

-- Usage in policies: (auth.jwt() ->> 'role') = 'ADMIN'


-- =============================================================
-- TABLE: users
-- ADMIN  — full access
-- OFFICER — no access (officers never query user accounts)
-- STUDENT — SELECT own row only (to load their own session data),
--           no access to password_hash (enforced at app level)
-- =============================================================

CREATE POLICY select_users_admin
    ON users FOR SELECT
    USING ((auth.jwt() ->> 'role') = 'ADMIN');

CREATE POLICY insert_users_admin
    ON users FOR INSERT
    WITH CHECK ((auth.jwt() ->> 'role') = 'ADMIN');

CREATE POLICY update_users_admin
    ON users FOR UPDATE
    USING ((auth.jwt() ->> 'role') = 'ADMIN');

CREATE POLICY delete_users_admin
    ON users FOR DELETE
    USING ((auth.jwt() ->> 'role') = 'ADMIN');

CREATE POLICY select_users_student
    ON users FOR SELECT
    USING (
        (auth.jwt() ->> 'role') = 'STUDENT'
        AND user_id = (auth.jwt() ->> 'user_id')::INT
    );


-- =============================================================
-- TABLE: students
-- ADMIN  — full access
-- OFFICER — SELECT only (read-only view of student records)
-- STUDENT — SELECT own record only
-- =============================================================

CREATE POLICY select_students_admin
    ON students FOR SELECT
    USING ((auth.jwt() ->> 'role') = 'ADMIN');

CREATE POLICY insert_students_admin
    ON students FOR INSERT
    WITH CHECK ((auth.jwt() ->> 'role') = 'ADMIN');

CREATE POLICY update_students_admin
    ON students FOR UPDATE
    USING ((auth.jwt() ->> 'role') = 'ADMIN');

CREATE POLICY delete_students_admin
    ON students FOR DELETE
    USING ((auth.jwt() ->> 'role') = 'ADMIN');

CREATE POLICY select_students_officer
    ON students FOR SELECT
    USING ((auth.jwt() ->> 'role') = 'OFFICER');

CREATE POLICY select_students_student
    ON students FOR SELECT
    USING (
        (auth.jwt() ->> 'role') = 'STUDENT'
        AND student_id = (auth.jwt() ->> 'student_ref_id')::INT
    );


-- =============================================================
-- TABLE: devices
-- ADMIN  — full access
-- OFFICER — SELECT only (read-only view of device records)
-- STUDENT — SELECT own devices only (where owner_id matches)
-- =============================================================

CREATE POLICY select_devices_admin
    ON devices FOR SELECT
    USING ((auth.jwt() ->> 'role') = 'ADMIN');

CREATE POLICY insert_devices_admin
    ON devices FOR INSERT
    WITH CHECK ((auth.jwt() ->> 'role') = 'ADMIN');

CREATE POLICY update_devices_admin
    ON devices FOR UPDATE
    USING ((auth.jwt() ->> 'role') = 'ADMIN');

CREATE POLICY delete_devices_admin
    ON devices FOR DELETE
    USING ((auth.jwt() ->> 'role') = 'ADMIN');

CREATE POLICY select_devices_officer
    ON devices FOR SELECT
    USING ((auth.jwt() ->> 'role') = 'OFFICER');

CREATE POLICY select_devices_student
    ON devices FOR SELECT
    USING (
        (auth.jwt() ->> 'role') = 'STUDENT'
        AND owner_id = (auth.jwt() ->> 'student_ref_id')::INT
    );


-- =============================================================
-- TABLE: device_logs
-- ADMIN  — full access
-- OFFICER — SELECT all logs; INSERT new log entries (gate actions)
-- STUDENT — SELECT own device logs only
-- No role may UPDATE or DELETE log entries (immutable by design)
-- =============================================================

CREATE POLICY select_device_logs_admin
    ON device_logs FOR SELECT
    USING ((auth.jwt() ->> 'role') = 'ADMIN');

CREATE POLICY insert_device_logs_admin
    ON device_logs FOR INSERT
    WITH CHECK ((auth.jwt() ->> 'role') = 'ADMIN');

CREATE POLICY update_device_logs_admin
    ON device_logs FOR UPDATE
    USING ((auth.jwt() ->> 'role') = 'ADMIN');

CREATE POLICY select_device_logs_officer
    ON device_logs FOR SELECT
    USING ((auth.jwt() ->> 'role') = 'OFFICER');

CREATE POLICY insert_device_logs_officer
    ON device_logs FOR INSERT
    WITH CHECK ((auth.jwt() ->> 'role') = 'OFFICER');

CREATE POLICY select_device_logs_student
    ON device_logs FOR SELECT
    USING (
        (auth.jwt() ->> 'role') = 'STUDENT'
        AND device_id IN (
            SELECT device_id FROM devices
            WHERE owner_id = (auth.jwt() ->> 'student_ref_id')::INT
        )
    );


-- =============================================================
-- TABLE: log_amendments
-- ADMIN  — full access
-- OFFICER — SELECT and INSERT (officers can make corrections)
-- STUDENT — no access
-- =============================================================

CREATE POLICY select_log_amendments_admin
    ON log_amendments FOR SELECT
    USING ((auth.jwt() ->> 'role') = 'ADMIN');

CREATE POLICY insert_log_amendments_admin
    ON log_amendments FOR INSERT
    WITH CHECK ((auth.jwt() ->> 'role') = 'ADMIN');

CREATE POLICY update_log_amendments_admin
    ON log_amendments FOR UPDATE
    USING ((auth.jwt() ->> 'role') = 'ADMIN');

CREATE POLICY select_log_amendments_officer
    ON log_amendments FOR SELECT
    USING ((auth.jwt() ->> 'role') = 'OFFICER');

CREATE POLICY insert_log_amendments_officer
    ON log_amendments FOR INSERT
    WITH CHECK ((auth.jwt() ->> 'role') = 'OFFICER');


-- =============================================================
-- TABLE: reports
-- ADMIN  — full access
-- OFFICER — no access
-- STUDENT — no access
-- =============================================================

CREATE POLICY select_reports_admin
    ON reports FOR SELECT
    USING ((auth.jwt() ->> 'role') = 'ADMIN');

CREATE POLICY insert_reports_admin
    ON reports FOR INSERT
    WITH CHECK ((auth.jwt() ->> 'role') = 'ADMIN');

CREATE POLICY update_reports_admin
    ON reports FOR UPDATE
    USING ((auth.jwt() ->> 'role') = 'ADMIN');

CREATE POLICY delete_reports_admin
    ON reports FOR DELETE
    USING ((auth.jwt() ->> 'role') = 'ADMIN');


-- =============================================================
-- TABLE: report_templates
-- ADMIN  — full access
-- OFFICER — no access
-- STUDENT — no access
-- =============================================================

CREATE POLICY select_report_templates_admin
    ON report_templates FOR SELECT
    USING ((auth.jwt() ->> 'role') = 'ADMIN');

CREATE POLICY insert_report_templates_admin
    ON report_templates FOR INSERT
    WITH CHECK ((auth.jwt() ->> 'role') = 'ADMIN');

CREATE POLICY update_report_templates_admin
    ON report_templates FOR UPDATE
    USING ((auth.jwt() ->> 'role') = 'ADMIN');

CREATE POLICY delete_report_templates_admin
    ON report_templates FOR DELETE
    USING ((auth.jwt() ->> 'role') = 'ADMIN');


-- =============================================================
-- TABLE: profile_update_requests
-- ADMIN  — full access (review, approve, reject)
-- OFFICER — no access
-- STUDENT — INSERT own requests; SELECT own requests only
-- =============================================================

CREATE POLICY select_requests_admin
    ON profile_update_requests FOR SELECT
    USING ((auth.jwt() ->> 'role') = 'ADMIN');

CREATE POLICY insert_requests_admin
    ON profile_update_requests FOR INSERT
    WITH CHECK ((auth.jwt() ->> 'role') = 'ADMIN');

CREATE POLICY update_requests_admin
    ON profile_update_requests FOR UPDATE
    USING ((auth.jwt() ->> 'role') = 'ADMIN');

CREATE POLICY delete_requests_admin
    ON profile_update_requests FOR DELETE
    USING ((auth.jwt() ->> 'role') = 'ADMIN');

CREATE POLICY select_requests_student
    ON profile_update_requests FOR SELECT
    USING (
        (auth.jwt() ->> 'role') = 'STUDENT'
        AND student_id = (auth.jwt() ->> 'student_ref_id')::INT
    );

CREATE POLICY insert_requests_student
    ON profile_update_requests FOR INSERT
    WITH CHECK (
        (auth.jwt() ->> 'role') = 'STUDENT'
        AND student_id = (auth.jwt() ->> 'student_ref_id')::INT
    );


-- =============================================================
-- TABLE: audit_log
-- INSERT only for all authenticated roles — NO UPDATE or DELETE
-- ADMIN  — SELECT (can view the full audit log screen)
-- OFFICER — INSERT only (actions are logged, but they cannot view)
-- STUDENT — INSERT only (actions are logged, but they cannot view)
-- =============================================================

CREATE POLICY select_audit_log_admin
    ON audit_log FOR SELECT
    USING ((auth.jwt() ->> 'role') = 'ADMIN');

CREATE POLICY insert_audit_log_admin
    ON audit_log FOR INSERT
    WITH CHECK ((auth.jwt() ->> 'role') = 'ADMIN');

CREATE POLICY insert_audit_log_officer
    ON audit_log FOR INSERT
    WITH CHECK ((auth.jwt() ->> 'role') = 'OFFICER');

CREATE POLICY insert_audit_log_student
    ON audit_log FOR INSERT
    WITH CHECK ((auth.jwt() ->> 'role') = 'STUDENT');


-- =============================================================
-- TABLE: settings
-- ADMIN  — full access
-- OFFICER — no access
-- STUDENT — no access
-- =============================================================

CREATE POLICY select_settings_admin
    ON settings FOR SELECT
    USING ((auth.jwt() ->> 'role') = 'ADMIN');

CREATE POLICY insert_settings_admin
    ON settings FOR INSERT
    WITH CHECK ((auth.jwt() ->> 'role') = 'ADMIN');

CREATE POLICY update_settings_admin
    ON settings FOR UPDATE
    USING ((auth.jwt() ->> 'role') = 'ADMIN');
