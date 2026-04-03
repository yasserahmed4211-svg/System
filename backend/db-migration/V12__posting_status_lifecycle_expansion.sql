-- ============================================================
-- V12: Expand ACC_POSTING_MST status check constraint
-- ============================================================
-- Adds new lifecycle statuses: JOURNAL_CREATED, READY_FOR_POST, CANCELLED
-- Full lifecycle: READY_FOR_GL → JOURNAL_CREATED → READY_FOR_POST → POSTED → REVERSED
--                                                └→ CANCELLED
-- ============================================================

-- Step 1: Normalize any non-standard status values to DRAFT before constraint change
UPDATE ACC_POSTING_MST
   SET STATUS = 'DRAFT'
 WHERE STATUS NOT IN ('DRAFT', 'READY_FOR_GL', 'JOURNAL_CREATED', 'READY_FOR_POST', 'POSTED', 'REVERSED', 'CANCELLED', 'ERROR');

-- Step 2: Drop the existing check constraint (if it exists)
DECLARE
    v_exists NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_exists FROM USER_CONSTRAINTS
     WHERE CONSTRAINT_NAME = 'CK_POSTING_STATUS' AND TABLE_NAME = 'ACC_POSTING_MST';
    IF v_exists > 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE ACC_POSTING_MST DROP CONSTRAINT CK_POSTING_STATUS';
    END IF;
END;
/

-- Step 3: Re-create with all valid lifecycle statuses
ALTER TABLE ACC_POSTING_MST ADD CONSTRAINT CK_POSTING_STATUS
    CHECK (STATUS IN ('DRAFT', 'READY_FOR_GL', 'JOURNAL_CREATED', 'READY_FOR_POST', 'POSTED', 'REVERSED', 'CANCELLED', 'ERROR'));
