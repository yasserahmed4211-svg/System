-- ========================================
-- Migration: V1__Create_Activity_Table.sql
-- Description: Create MD_ACTIVITY table with Arabic/English comments
-- Rule 9: Database Standards - Oracle naming conventions
-- ========================================

-- Create MD_ACTIVITY table
CREATE TABLE MD_ACTIVITY (
    ID_PK                    NUMBER          NOT NULL,
    CODE                     VARCHAR2(50)    NOT NULL,
    NAME                     VARCHAR2(200)   NOT NULL,
    DESCRIPTION              VARCHAR2(500),
    DEFAULT_STOCK_UOM_ID_FK  NUMBER,
    CONVERSION_TYPE          VARCHAR2(20)    NOT NULL,
    REQUIRES_ACTUAL_WEIGHT   NUMBER(1)       DEFAULT 0 NOT NULL,
    ALLOW_FRACTION           NUMBER(1)       DEFAULT 0 NOT NULL,
    IS_ACTIVE                NUMBER(1)       DEFAULT 1 NOT NULL,
    CREATED_AT               TIMESTAMP       NOT NULL,
    CREATED_BY               VARCHAR2(100)   NOT NULL,
    UPDATED_AT               TIMESTAMP,
    UPDATED_BY               VARCHAR2(100),
    TENANT_ID                VARCHAR2(50)    NOT NULL,
    
    CONSTRAINT MD_ACTIVITY_PK PRIMARY KEY (ID_PK),
    CONSTRAINT MD_ACTIVITY_UK UNIQUE (CODE, TENANT_ID),
    CONSTRAINT MD_ACTIVITY_UOM_FK FOREIGN KEY (DEFAULT_STOCK_UOM_ID_FK)
        REFERENCES LOOKUP_DETAIL(ID_PK) ON DELETE RESTRICT,
    CONSTRAINT MD_ACTIVITY_CONV_CHK CHECK (CONVERSION_TYPE IN ('FIXED', 'VARIABLE')),
    CONSTRAINT MD_ACTIVITY_WEIGHT_CHK CHECK (
        (CONVERSION_TYPE = 'VARIABLE' AND REQUIRES_ACTUAL_WEIGHT = 1) OR
        (CONVERSION_TYPE = 'FIXED')
    )
);

-- Create sequence for ID generation
CREATE SEQUENCE MD_ACTIVITY_SEQ START WITH 1 INCREMENT BY 1;

-- Create indexes for performance (Rule 10.4: Index Foreign Keys)
CREATE INDEX IDX_MD_ACTIVITY_UOM_FK ON MD_ACTIVITY(DEFAULT_STOCK_UOM_ID_FK);
CREATE INDEX IDX_MD_ACTIVITY_TENANT ON MD_ACTIVITY(TENANT_ID);
CREATE INDEX IDX_MD_ACTIVITY_ACTIVE ON MD_ACTIVITY(IS_ACTIVE);
CREATE INDEX IDX_MD_ACTIVITY_CODE ON MD_ACTIVITY(CODE);

-- ========================================
-- Table and Column Comments (Arabic + English)
-- ========================================

COMMENT ON TABLE MD_ACTIVITY IS 'Master Data: Activities - البيانات الأساسية: الأنشطة';

COMMENT ON COLUMN MD_ACTIVITY.ID_PK IS 'Primary Key - المفتاح الأساسي';
COMMENT ON COLUMN MD_ACTIVITY.CODE IS 'Activity Code (unique per tenant) - كود النشاط (فريد لكل مستأجر)';
COMMENT ON COLUMN MD_ACTIVITY.NAME IS 'Activity Name - اسم النشاط';
COMMENT ON COLUMN MD_ACTIVITY.DESCRIPTION IS 'Activity Description - وصف النشاط';
COMMENT ON COLUMN MD_ACTIVITY.DEFAULT_STOCK_UOM_ID_FK IS 'Default Stock Unit of Measure - وحدة القياس الافتراضية للمخزون';
COMMENT ON COLUMN MD_ACTIVITY.CONVERSION_TYPE IS 'Conversion Type: FIXED or VARIABLE - نوع التحويل: ثابت أو متغير';
COMMENT ON COLUMN MD_ACTIVITY.REQUIRES_ACTUAL_WEIGHT IS 'Requires Actual Weight (1=Yes, 0=No) - يتطلب الوزن الفعلي';
COMMENT ON COLUMN MD_ACTIVITY.ALLOW_FRACTION IS 'Allow Fractional Quantities (1=Yes, 0=No) - يسمح بالكسور';
COMMENT ON COLUMN MD_ACTIVITY.IS_ACTIVE IS 'Active Status (1=Active, 0=Inactive) - حالة النشاط';
COMMENT ON COLUMN MD_ACTIVITY.CREATED_AT IS 'Creation Timestamp - تاريخ الإنشاء';
COMMENT ON COLUMN MD_ACTIVITY.CREATED_BY IS 'Created By User - منشئ السجل';
COMMENT ON COLUMN MD_ACTIVITY.UPDATED_AT IS 'Last Update Timestamp - تاريخ آخر تحديث';
COMMENT ON COLUMN MD_ACTIVITY.UPDATED_BY IS 'Last Updated By User - آخر من عدل السجل';
COMMENT ON COLUMN MD_ACTIVITY.TENANT_ID IS 'Tenant ID for multi-tenancy - معرف المستأجر';
