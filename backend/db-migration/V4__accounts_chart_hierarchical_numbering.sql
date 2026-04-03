-- ============================================================
-- V4: Chart of Accounts - Hierarchical Numbering Enhancement
-- Adds indexes for auto-numbering performance and ensures
-- proper constraints for hierarchical account structure.
-- ============================================================

-- ── INDEX: ACCOUNT_CHART_NO for fast numbering lookups ──────
-- This index supports the auto-numbering queries that find
-- MAX(ACCOUNT_CHART_NO) for generating sequential child codes.
BEGIN
    EXECUTE IMMEDIATE 'CREATE INDEX IDX_ACCOUNTS_CHART_NO ON ACCOUNTS_CHART (ACCOUNT_CHART_NO)';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE = -1408 OR SQLCODE = -955 THEN
            NULL; -- Index already exists
        ELSE
            RAISE;
        END IF;
END;
/

-- ── COMPOSITE INDEX: Parent + AccountNo for child code generation ──
-- Optimizes the query: MAX(ACCOUNT_CHART_NO) WHERE ACCOUNT_CHART_FK = :parentPk
BEGIN
    EXECUTE IMMEDIATE 'CREATE INDEX IDX_ACCOUNTS_CHART_PARENT_NO ON ACCOUNTS_CHART (ACCOUNT_CHART_FK, ACCOUNT_CHART_NO)';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE = -1408 OR SQLCODE = -955 THEN
            NULL; -- Index already exists
        ELSE
            RAISE;
        END IF;
END;
/

-- ── COMPOSITE INDEX: Organization + AccountType for root code generation ──
-- Optimizes the query: MAX(ACCOUNT_CHART_NO) WHERE ORGANIZATION_FK = :orgFk AND ACCOUNT_TYPE = :type AND ACCOUNT_CHART_FK IS NULL
BEGIN
    EXECUTE IMMEDIATE 'CREATE INDEX IDX_ACCOUNTS_CHART_ORG_TYPE ON ACCOUNTS_CHART (ORGANIZATION_FK, ACCOUNT_TYPE)';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE = -1408 OR SQLCODE = -955 THEN
            NULL; -- Index already exists
        ELSE
            RAISE;
        END IF;
END;
/

-- ── Verify unique constraint exists ─────────────────────────
-- UK_ACCOUNTS_CHART_NO_ORG on (ACCOUNT_CHART_NO, ORGANIZATION_FK)
-- Already created in V1, but verify it exists
DECLARE
    v_cnt NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_cnt
    FROM user_constraints
    WHERE constraint_name = 'UK_ACCOUNTS_CHART_NO_ORG'
      AND table_name = 'ACCOUNTS_CHART';

    IF v_cnt = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE ACCOUNTS_CHART ADD CONSTRAINT UK_ACCOUNTS_CHART_NO_ORG UNIQUE (ACCOUNT_CHART_NO, ORGANIZATION_FK)';
    END IF;
END;
/
