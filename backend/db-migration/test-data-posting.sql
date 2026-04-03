-- ============================================================
-- Test Data: GL Posting → Generate Journal Flow
-- Run AFTER V11 migration
-- ============================================================
-- This script:
--   1. Updates existing postings to READY_FOR_GL for testing
--   2. Adds accounting rules for CREDIT_NOTE / RECEIPT / PURCHASE
--   3. Assigns GL Posting permissions to Admin role
-- ============================================================

-- ================================================================
-- STEP 1: Update posting statuses for testing
-- ================================================================

-- Posting 2 (SALES/INVOICE, company 1) → READY_FOR_GL
-- Has details: RECEIVABLE(5750), REVENUE(5000), VAT_OUTPUT(750), customer=101
UPDATE ACC_POSTING_MST
   SET STATUS        = 'READY_FOR_GL',
       ERROR_MESSAGE = NULL,
       FIN_JOURNAL_ID_FK = NULL
 WHERE POSTING_ID = 2;

-- Posting 3 (SALES/CREDIT_NOTE, company 1) → already READY_FOR_GL
-- Has details: RETURNS(2000), RECEIVABLE(2300), VAT_OUTPUT(300), customer=100

-- Posting 4 (SALES/RECEIPT, company 1) → READY_FOR_GL
-- Has details: CASH(5000), RECEIVABLE(5000), customer=100
UPDATE ACC_POSTING_MST
   SET STATUS        = 'READY_FOR_GL',
       ERROR_MESSAGE = NULL,
       FIN_JOURNAL_ID_FK = NULL
 WHERE POSTING_ID = 4;

-- Posting 5 (PURCHASE/INVOICE, company 1) → READY_FOR_GL
-- Has details: PURCHASES(8000), VAT_INPUT(1200), PAYABLE(9200), supplier=200
UPDATE ACC_POSTING_MST
   SET STATUS        = 'READY_FOR_GL',
       ERROR_MESSAGE = NULL,
       FIN_JOURNAL_ID_FK = NULL
 WHERE POSTING_ID = 5;

-- ================================================================
-- STEP 2: Make sure all referenced accounts exist and are active
-- ================================================================
-- Verify current rule accounts (85=Receivable, 82=Revenue) exist:
-- SELECT ACCOUNT_CHART_PK, ACCOUNT_CHART_NO, ACCOUNT_CHART_NAME
--   FROM ACCOUNTS_CHART WHERE ACCOUNT_CHART_PK IN (82, 85);

-- We'll use existing leaf accounts. Adjust IDs below if your DB has different PKs.
-- Common mapping (from typical chart):
--   Account 82  → Revenue (إيرادات المبيعات)
--   Account 85  → Accounts Receivable (ذمم مدينة)
--   Account 18  → Cash in hand (النقدية في الصندوق) PK=18
--   Account 89  → Bank account (حساب البنك الرئيسي) PK=89
-- If these don't match your DB, update the ACCOUNT_ID_FK values below.

-- ================================================================
-- STEP 3: Fix existing rule lines (FULL_AMOUNT → TOTAL)
-- ================================================================
-- PostingEngineService only supports: TOTAL, FIXED, PERCENT, REMAINING
UPDATE ACC_RULE_LINE SET AMOUNT_SOURCE_TYPE = 'TOTAL' WHERE AMOUNT_SOURCE_TYPE = 'FULL_AMOUNT';

-- ================================================================
-- STEP 4: Add accounting rules for other doc types
-- ================================================================

-- ── Rule: SALES / CREDIT_NOTE (for posting 3) ──────────────
DECLARE
    v_rule_id NUMBER;
BEGIN
    SELECT NVL(MAX(RULE_ID), 100) + 1 INTO v_rule_id FROM ACC_RULE_HDR;

    DECLARE
        v_exists NUMBER;
    BEGIN
        SELECT COUNT(*) INTO v_exists FROM ACC_RULE_HDR
         WHERE SOURCE_MODULE = 'SALES' AND SOURCE_DOC_TYPE = 'CREDIT_NOTE' AND COMPANY_ID_FK = 1;

        IF v_exists = 0 THEN
            INSERT INTO ACC_RULE_HDR (RULE_ID, SOURCE_MODULE, SOURCE_DOC_TYPE, COMPANY_ID_FK, IS_ACTIVE, CREATED_AT, CREATED_BY, UPDATED_AT, UPDATED_BY)
            VALUES (v_rule_id, 'SALES', 'CREDIT_NOTE', 1, 1, SYSTIMESTAMP, 'SYSTEM', SYSTIMESTAMP, 'SYSTEM');

            -- Credit Note: DR Revenue, CR Receivable (reverse of invoice)
            INSERT INTO ACC_RULE_LINE (RULE_LINE_ID, RULE_ID_FK, BUSINESS_SIDE, ACCOUNT_ID_FK, ENTRY_SIDE, PRIORITY, AMOUNT_SOURCE_TYPE, AMOUNT_SOURCE_VALUE, CREATED_AT, CREATED_BY, UPDATED_AT, UPDATED_BY, PAYMENT_TYPE_CODE, ENTITY_TYPE)
            VALUES ((SELECT NVL(MAX(RULE_LINE_ID),100)+1 FROM ACC_RULE_LINE), v_rule_id, NULL, 82, 'DEBIT', 1, 'TOTAL', NULL, SYSTIMESTAMP, 'SYSTEM', SYSTIMESTAMP, 'SYSTEM', 'CASH', 'CUSTOMER');

            INSERT INTO ACC_RULE_LINE (RULE_LINE_ID, RULE_ID_FK, BUSINESS_SIDE, ACCOUNT_ID_FK, ENTRY_SIDE, PRIORITY, AMOUNT_SOURCE_TYPE, AMOUNT_SOURCE_VALUE, CREATED_AT, CREATED_BY, UPDATED_AT, UPDATED_BY, PAYMENT_TYPE_CODE, ENTITY_TYPE)
            VALUES ((SELECT NVL(MAX(RULE_LINE_ID),100)+1 FROM ACC_RULE_LINE), v_rule_id, NULL, 85, 'CREDIT', 2, 'TOTAL', NULL, SYSTIMESTAMP, 'SYSTEM', SYSTIMESTAMP, 'SYSTEM', 'CASH', 'CUSTOMER');
        END IF;
    END;
END;
/

-- ── Rule: SALES / RECEIPT (for posting 4) ──────────────────
DECLARE
    v_rule_id NUMBER;
BEGIN
    SELECT NVL(MAX(RULE_ID), 100) + 1 INTO v_rule_id FROM ACC_RULE_HDR;

    DECLARE
        v_exists NUMBER;
    BEGIN
        SELECT COUNT(*) INTO v_exists FROM ACC_RULE_HDR
         WHERE SOURCE_MODULE = 'SALES' AND SOURCE_DOC_TYPE = 'RECEIPT' AND COMPANY_ID_FK = 1;

        IF v_exists = 0 THEN
            INSERT INTO ACC_RULE_HDR (RULE_ID, SOURCE_MODULE, SOURCE_DOC_TYPE, COMPANY_ID_FK, IS_ACTIVE, CREATED_AT, CREATED_BY, UPDATED_AT, UPDATED_BY)
            VALUES (v_rule_id, 'SALES', 'RECEIPT', 1, 1, SYSTIMESTAMP, 'SYSTEM', SYSTIMESTAMP, 'SYSTEM');

            -- Receipt: DR Cash/Bank, CR Receivable
            INSERT INTO ACC_RULE_LINE (RULE_LINE_ID, RULE_ID_FK, BUSINESS_SIDE, ACCOUNT_ID_FK, ENTRY_SIDE, PRIORITY, AMOUNT_SOURCE_TYPE, AMOUNT_SOURCE_VALUE, CREATED_AT, CREATED_BY, UPDATED_AT, UPDATED_BY, PAYMENT_TYPE_CODE, ENTITY_TYPE)
            VALUES ((SELECT NVL(MAX(RULE_LINE_ID),100)+1 FROM ACC_RULE_LINE), v_rule_id, NULL, 18, 'DEBIT', 1, 'TOTAL', NULL, SYSTIMESTAMP, 'SYSTEM', SYSTIMESTAMP, 'SYSTEM', 'CASH', 'CUSTOMER');

            INSERT INTO ACC_RULE_LINE (RULE_LINE_ID, RULE_ID_FK, BUSINESS_SIDE, ACCOUNT_ID_FK, ENTRY_SIDE, PRIORITY, AMOUNT_SOURCE_TYPE, AMOUNT_SOURCE_VALUE, CREATED_AT, CREATED_BY, UPDATED_AT, UPDATED_BY, PAYMENT_TYPE_CODE, ENTITY_TYPE)
            VALUES ((SELECT NVL(MAX(RULE_LINE_ID),100)+1 FROM ACC_RULE_LINE), v_rule_id, NULL, 85, 'CREDIT', 2, 'TOTAL', NULL, SYSTIMESTAMP, 'SYSTEM', SYSTIMESTAMP, 'SYSTEM', 'CASH', 'CUSTOMER');
        END IF;
    END;
END;
/

-- ── Rule: PURCHASE / INVOICE (for posting 5) ───────────────
DECLARE
    v_rule_id NUMBER;
    v_payable_account NUMBER;
BEGIN
    SELECT NVL(MAX(RULE_ID), 100) + 1 INTO v_rule_id FROM ACC_RULE_HDR;

    -- Adjust this to your actual payable leaf account PK
    v_payable_account := 8; -- PK=8 → الخصوم المتداولة

    DECLARE
        v_exists NUMBER;
    BEGIN
        SELECT COUNT(*) INTO v_exists FROM ACC_RULE_HDR
         WHERE SOURCE_MODULE = 'PURCHASE' AND SOURCE_DOC_TYPE = 'INVOICE' AND COMPANY_ID_FK = 1;

        IF v_exists = 0 THEN
            INSERT INTO ACC_RULE_HDR (RULE_ID, SOURCE_MODULE, SOURCE_DOC_TYPE, COMPANY_ID_FK, IS_ACTIVE, CREATED_AT, CREATED_BY, UPDATED_AT, UPDATED_BY)
            VALUES (v_rule_id, 'PURCHASE', 'INVOICE', 1, 1, SYSTIMESTAMP, 'SYSTEM', SYSTIMESTAMP, 'SYSTEM');

            -- Purchase Invoice: DR Purchases (expense=14), CR Payable (liability=8)
            INSERT INTO ACC_RULE_LINE (RULE_LINE_ID, RULE_ID_FK, BUSINESS_SIDE, ACCOUNT_ID_FK, ENTRY_SIDE, PRIORITY, AMOUNT_SOURCE_TYPE, AMOUNT_SOURCE_VALUE, CREATED_AT, CREATED_BY, UPDATED_AT, UPDATED_BY, PAYMENT_TYPE_CODE, ENTITY_TYPE)
            VALUES ((SELECT NVL(MAX(RULE_LINE_ID),100)+1 FROM ACC_RULE_LINE), v_rule_id, NULL, 14, 'DEBIT', 1, 'TOTAL', NULL, SYSTIMESTAMP, 'SYSTEM', SYSTIMESTAMP, 'SYSTEM', 'CASH', 'VENDOR');

            INSERT INTO ACC_RULE_LINE (RULE_LINE_ID, RULE_ID_FK, BUSINESS_SIDE, ACCOUNT_ID_FK, ENTRY_SIDE, PRIORITY, AMOUNT_SOURCE_TYPE, AMOUNT_SOURCE_VALUE, CREATED_AT, CREATED_BY, UPDATED_AT, UPDATED_BY, PAYMENT_TYPE_CODE, ENTITY_TYPE)
            VALUES ((SELECT NVL(MAX(RULE_LINE_ID),100)+1 FROM ACC_RULE_LINE), v_rule_id, NULL, v_payable_account, 'CREDIT', 2, 'TOTAL', NULL, SYSTIMESTAMP, 'SYSTEM', SYSTIMESTAMP, 'SYSTEM', 'CASH', 'VENDOR');
        END IF;
    END;
END;
/

-- ================================================================
-- STEP 4: Assign GL Posting permissions to Admin role
-- ================================================================

-- Find Admin role and assign all GL Posting permissions
DECLARE
    v_role_id NUMBER;
BEGIN
    SELECT ID INTO v_role_id FROM ROLES WHERE UPPER(NAME) = 'ADMIN' AND ROWNUM = 1;

    FOR rec IN (
        SELECT ID AS PERM_ID FROM PERMISSIONS
         WHERE NAME IN (
            'PERM_GL_POSTING_VIEW',
            'PERM_GL_POSTING_CREATE',
            'PERM_GL_POSTING_UPDATE',
            'PERM_GL_POSTING_DELETE'
         )
    ) LOOP
        BEGIN
            INSERT INTO ROLE_PERMISSIONS (ROLE_ID, PERM_ID)
            VALUES (v_role_id, rec.PERM_ID);
        EXCEPTION
            WHEN DUP_VAL_ON_INDEX THEN NULL; -- already assigned
        END;
    END LOOP;
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        DBMS_OUTPUT.PUT_LINE('WARNING: Admin role not found. Assign permissions manually.');
END;
/

COMMIT;

-- ================================================================
-- VERIFICATION QUERIES
-- ================================================================

-- 1. Check postings ready for testing:
SELECT POSTING_ID, SOURCE_MODULE, SOURCE_DOC_TYPE, STATUS, TOTAL_AMOUNT, FIN_JOURNAL_ID_FK
  FROM ACC_POSTING_MST
 WHERE STATUS = 'READY_FOR_GL'
 ORDER BY POSTING_ID;

-- 2. Check accounting rules exist for those postings:
SELECT h.RULE_ID, h.SOURCE_MODULE, h.SOURCE_DOC_TYPE, h.COMPANY_ID_FK, h.IS_ACTIVE,
       COUNT(l.RULE_LINE_ID) AS LINE_COUNT
  FROM ACC_RULE_HDR h
  LEFT JOIN ACC_RULE_LINE l ON l.RULE_ID_FK = h.RULE_ID
 GROUP BY h.RULE_ID, h.SOURCE_MODULE, h.SOURCE_DOC_TYPE, h.COMPANY_ID_FK, h.IS_ACTIVE
 ORDER BY h.RULE_ID;

-- 3. Check rule line details:
SELECT l.RULE_LINE_ID, l.RULE_ID_FK, l.ENTRY_SIDE, l.PRIORITY,
       l.ACCOUNT_ID_FK, a.ACCOUNT_CHART_NO, a.ACCOUNT_CHART_NAME,
       l.AMOUNT_SOURCE_TYPE, l.ENTITY_TYPE
  FROM ACC_RULE_LINE l
  JOIN ACCOUNTS_CHART a ON a.ACCOUNT_CHART_PK = l.ACCOUNT_ID_FK
 ORDER BY l.RULE_ID_FK, l.PRIORITY;

-- 4. Check permissions assigned to Admin role:
SELECT p.NAME
  FROM ROLE_PERMISSIONS rp
  JOIN PERMISSIONS p ON p.ID = rp.PERM_ID
  JOIN ROLES r ON r.ID = rp.ROLE_ID
 WHERE UPPER(r.NAME) = 'ADMIN'
   AND p.NAME LIKE 'PERM_GL_POSTING%'
 ORDER BY p.NAME;
