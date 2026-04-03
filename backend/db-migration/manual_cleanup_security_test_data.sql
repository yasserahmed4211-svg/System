-- ============================================================
-- Manual cleanup script for security tables
-- Schema: TEST
-- IMPORTANT:
-- 1. Run manually only.
-- 2. Do NOT rename this file to Flyway version format unless you intend to execute it as a migration.
-- 3. This script deletes ALL data from the listed tables.
-- ============================================================

SET DEFINE OFF;

-- ============================================================
-- 1. CHILD TABLES FIRST
-- ============================================================

DELETE FROM TEST.ROLE_PERMISSIONS
;

DELETE FROM TEST.USER_ROLES
;

-- ============================================================
-- 2. PERMISSIONS BEFORE PAGES / ROLES BEFORE USERS
-- ============================================================

DELETE FROM TEST.PERMISSIONS
;

DELETE FROM TEST.ROLES
;

DELETE FROM TEST.USERS
;

-- ============================================================
-- 3. PAGES LAST AFTER PERMISSIONS
-- ============================================================

DELETE FROM TEST.SEC_PAGES
;

COMMIT;

-- ============================================================
-- Optional verification queries
-- ============================================================
-- SELECT COUNT(*) FROM TEST.ROLE_PERMISSIONS;
-- SELECT COUNT(*) FROM TEST.USER_ROLES;
-- SELECT COUNT(*) FROM TEST.PERMISSIONS;
-- SELECT COUNT(*) FROM TEST.ROLES;
-- SELECT COUNT(*) FROM TEST.USERS;
-- SELECT COUNT(*) FROM TEST.SEC_PAGES;
