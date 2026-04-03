-- ========================================
-- FLYWAY MIGRATION: V2__Create_Category_Table.sql
-- Module: Master Data (MD)
-- Purpose: Create MD_CATEGORY table for testing FK constraints
-- Author: ERP Team
-- ========================================

-- جدول الفئات (Categories)
-- هذا الجدول لاختبار قيود المفاتيح الأجنبية مع جدول النشاطات
-- This table is for testing FK constraints with Activity table
CREATE TABLE MD_CATEGORY (
    ID_PK                   NUMBER          NOT NULL,
    CODE                    VARCHAR2(50)    NOT NULL,
    NAME                    VARCHAR2(200)   NOT NULL,
    ACTIVITY_ID_FK          NUMBER          NOT NULL,  -- مرجع للنشاط (Reference to Activity)
    IS_ACTIVE               NUMBER(1)       DEFAULT 1 NOT NULL,
    
    -- Audit fields
    CREATED_BY              VARCHAR2(100)   NOT NULL,
    CREATED_AT              TIMESTAMP       NOT NULL,
    UPDATED_BY              VARCHAR2(100),
    UPDATED_AT              TIMESTAMP,
    TENANT_ID               VARCHAR2(100)   NOT NULL,
    
    -- Primary Key
    CONSTRAINT MD_CATEGORY_PK PRIMARY KEY (ID_PK),
    
    -- Unique Constraint: Code must be unique per tenant
    CONSTRAINT MD_CATEGORY_UK_CODE_TENANT UNIQUE (CODE, TENANT_ID),
    
    -- Foreign Key: Category must reference a valid Activity
    CONSTRAINT MD_CATEGORY_ACTIVITY_FK 
        FOREIGN KEY (ACTIVITY_ID_FK) 
        REFERENCES MD_ACTIVITY (ID_PK) 
        ON DELETE RESTRICT,  -- منع حذف النشاط إذا كان مرتبط بفئات
    
    -- Check Constraint: IS_ACTIVE must be 0 or 1
    CONSTRAINT MD_CATEGORY_CK_ACTIVE 
        CHECK (IS_ACTIVE IN (0, 1))
);

-- Index on FK column (ACTIVITY_ID_FK) for better join performance
CREATE INDEX IDX_MD_CATEGORY_ACTIVITY_FK ON MD_CATEGORY(ACTIVITY_ID_FK);

-- Index on TENANT_ID for multi-tenancy isolation
CREATE INDEX IDX_MD_CATEGORY_TENANT ON MD_CATEGORY(TENANT_ID);

-- Index on IS_ACTIVE for filtering active categories
CREATE INDEX IDX_MD_CATEGORY_ACTIVE ON MD_CATEGORY(IS_ACTIVE);

-- Index on CODE for fast lookup
CREATE INDEX IDX_MD_CATEGORY_CODE ON MD_CATEGORY(CODE);

-- Sequence for ID_PK
CREATE SEQUENCE MD_CATEGORY_SEQ
    START WITH 1
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

-- ========================================
-- TABLE AND COLUMN COMMENTS
-- ========================================

-- Table comment (English)
COMMENT ON TABLE MD_CATEGORY IS 
'Master Data: Categories table - for testing FK constraints with Activity';

-- Table comment (Arabic)
COMMENT ON TABLE MD_CATEGORY IS 
'الفئات (Categories) - لاختبار قيود المفاتيح الأجنبية مع النشاطات';

-- Column comments
COMMENT ON COLUMN MD_CATEGORY.ID_PK IS 'Primary Key / المعرف الأساسي';
COMMENT ON COLUMN MD_CATEGORY.CODE IS 'Category Code (unique per tenant) / رمز الفئة (فريد لكل مستأجر)';
COMMENT ON COLUMN MD_CATEGORY.NAME IS 'Category Name / اسم الفئة';
COMMENT ON COLUMN MD_CATEGORY.ACTIVITY_ID_FK IS 'Foreign Key to Activity / مرجع للنشاط';
COMMENT ON COLUMN MD_CATEGORY.IS_ACTIVE IS 'Active Status (0=Inactive, 1=Active) / حالة التفعيل (0=غير نشط، 1=نشط)';
COMMENT ON COLUMN MD_CATEGORY.CREATED_BY IS 'User who created the record / المستخدم الذي أنشأ السجل';
COMMENT ON COLUMN MD_CATEGORY.CREATED_AT IS 'Creation timestamp / وقت الإنشاء';
COMMENT ON COLUMN MD_CATEGORY.UPDATED_BY IS 'User who last updated the record / المستخدم الذي حدث السجل آخر مرة';
COMMENT ON COLUMN MD_CATEGORY.UPDATED_AT IS 'Last update timestamp / وقت آخر تحديث';
COMMENT ON COLUMN MD_CATEGORY.TENANT_ID IS 'Tenant ID for multi-tenancy isolation / معرف المستأجر للعزل المتعدد';
