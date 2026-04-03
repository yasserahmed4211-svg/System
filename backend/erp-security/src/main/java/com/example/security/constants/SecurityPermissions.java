package com.example.security.constants;

/**
 * صلاحيات النظام - ثوابت مركزية
 * يتم استخدامها في @PreAuthorize والتحقق من الصلاحيات
 */
public final class SecurityPermissions {
    
    private SecurityPermissions() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
    
    // ==================== User Permissions ====================
    
    /** عرض المستخدمين */
    public static final String USER_VIEW = "PERM_USER_VIEW";
    
    /** إنشاء مستخدم جديد */
    public static final String USER_CREATE = "PERM_USER_CREATE";
    
    /** تعديل مستخدم موجود */
    public static final String USER_UPDATE = "PERM_USER_UPDATE";
    
    /** حذف مستخدم */
    public static final String USER_DELETE = "PERM_USER_DELETE";

    /** إدارة أدوار المستخدم */
    public static final String USER_MANAGE_ROLES = "PERM_USER_UPDATE";

    // ==================== Role Permissions ====================
    
    /** عرض الأدوار */
    public static final String ROLE_VIEW = "PERM_ROLE_VIEW";
    
    /** إنشاء دور جديد */
    public static final String ROLE_CREATE = "PERM_ROLE_CREATE";
    
    /** تعديل دور موجود */
    public static final String ROLE_UPDATE = "PERM_ROLE_UPDATE";
    
    /** حذف دور */
    public static final String ROLE_DELETE = "PERM_ROLE_DELETE";
    

    // ==================== Permission Management ====================
    
    /** عرض الصلاحيات */
    public static final String PERMISSION_VIEW = "PERM_PERMISSION_VIEW";
    
    /** إنشاء صلاحية جديدة */
    public static final String PERMISSION_CREATE = "PERM_PERMISSION_CREATE";
    
    /** حذف صلاحية */
    public static final String PERMISSION_DELETE = "PERM_PERMISSION_DELETE";
    
    // ==================== Menu Permissions ====================
    
    /** عرض عناصر القائمة */
    public static final String MENU_VIEW = "PERM_MENU_VIEW";
    
    /** إنشاء عنصر قائمة جديد */
    public static final String MENU_CREATE = "PERM_MENU_CREATE";
    
    /** تعديل عنصر قائمة موجود */
    public static final String MENU_UPDATE = "PERM_MENU_UPDATE";
    
    /** حذف عنصر قائمة */
    public static final String MENU_DELETE = "PERM_MENU_DELETE";
    
    // ==================== Page Permissions (NEW) ====================
    
    /** عرض الصفحات */
    public static final String PAGE_VIEW = "PERM_PAGE_VIEW";
    
    /** إنشاء صفحة جديدة */
    public static final String PAGE_CREATE = "PERM_PAGE_CREATE";
    
    /** تعديل صفحة موجودة */
    public static final String PAGE_UPDATE = "PERM_PAGE_UPDATE";
    
    /** حذف صفحة */
    public static final String PAGE_DELETE = "PERM_PAGE_DELETE";

    // ==================== Master Lookup Permissions ====================
    
    /** عرض قوائم البحث الرئيسية */
    public static final String MASTER_LOOKUP_VIEW = "PERM_MASTER_LOOKUP_VIEW";
    
    /** إنشاء قائمة بحث رئيسية جديدة */
    public static final String MASTER_LOOKUP_CREATE = "PERM_MASTER_LOOKUP_CREATE";
    
    /** تعديل قائمة بحث رئيسية موجودة */
    public static final String MASTER_LOOKUP_UPDATE = "PERM_MASTER_LOOKUP_UPDATE";
    
    /** حذف قائمة بحث رئيسية */
    public static final String MASTER_LOOKUP_DELETE = "PERM_MASTER_LOOKUP_DELETE";

    // ==================== Lookup Detail Permissions ====================
    
    /** عرض تفاصيل قوائم البحث */
    public static final String LOOKUP_DETAIL_VIEW = "PERM_LOOKUP_DETAIL_VIEW";
    
    /** إنشاء تفصيل قائمة بحث جديد */
    public static final String LOOKUP_DETAIL_CREATE = "PERM_LOOKUP_DETAIL_CREATE";
    
    /** تعديل تفصيل قائمة بحث موجود */
    public static final String LOOKUP_DETAIL_UPDATE = "PERM_LOOKUP_DETAIL_UPDATE";
    
    /** حذف تفصيل قائمة بحث */
    public static final String LOOKUP_DETAIL_DELETE = "PERM_LOOKUP_DETAIL_DELETE";

    // ==================== Activity Permissions ====================

    /** عرض الأنشطة */
    public static final String ACTIVITY_VIEW = "PERM_ACTIVITY_VIEW";

    /** إنشاء نشاط جديد */
    public static final String ACTIVITY_CREATE = "PERM_ACTIVITY_CREATE";

    /** تعديل نشاط موجود */
    public static final String ACTIVITY_UPDATE = "PERM_ACTIVITY_UPDATE";

    /** حذف نشاط */
    public static final String ACTIVITY_DELETE = "PERM_ACTIVITY_DELETE";

    // ==================== GL Account Permissions ====================

    /** عرض دليل الحسابات */
    public static final String GL_ACCOUNT_VIEW = "PERM_GL_ACCOUNT_VIEW";

    /** إنشاء حساب جديد */
    public static final String GL_ACCOUNT_CREATE = "PERM_GL_ACCOUNT_CREATE";

    /** تعديل حساب موجود */
    public static final String GL_ACCOUNT_UPDATE = "PERM_GL_ACCOUNT_UPDATE";

    /** إلغاء تفعيل حساب */
    public static final String GL_ACCOUNT_DELETE = "PERM_GL_ACCOUNT_DELETE";

    // ==================== GL Rule Permissions ====================

    /** عرض القواعد المحاسبية */
    public static final String GL_RULE_VIEW = "PERM_GL_RULE_VIEW";

    /** إنشاء قاعدة محاسبية جديدة */
    public static final String GL_RULE_CREATE = "PERM_GL_RULE_CREATE";

    /** تعديل قاعدة محاسبية موجودة */
    public static final String GL_RULE_UPDATE = "PERM_GL_RULE_UPDATE";

    /** إلغاء تفعيل قاعدة محاسبية */
    public static final String GL_RULE_DELETE = "PERM_GL_RULE_DELETE";

    // ==================== GL Journal Permissions ====================

    /** عرض قيود اليومية */
    public static final String GL_JOURNAL_VIEW = "PERM_GL_JOURNAL_VIEW";

    /** إنشاء قيد يومية جديد */
    public static final String GL_JOURNAL_CREATE = "PERM_GL_JOURNAL_CREATE";

    /** تعديل قيد يومية موجود */
    public static final String GL_JOURNAL_UPDATE = "PERM_GL_JOURNAL_UPDATE";

    /** إلغاء تفعيل قيد يومية */
    public static final String GL_JOURNAL_DELETE = "PERM_GL_JOURNAL_DELETE";

    /** اعتماد قيد يومية */
    public static final String GL_JOURNAL_APPROVE = "PERM_GL_JOURNAL_APPROVE";

    /** ترحيل قيد يومية */
    public static final String GL_JOURNAL_POST = "PERM_GL_JOURNAL_POST";

    /** عكس قيد يومية */
    public static final String GL_JOURNAL_REVERSE = "PERM_GL_JOURNAL_REVERSE";

    /** إلغاء قيد يومية */
    public static final String GL_JOURNAL_CANCEL = "PERM_GL_JOURNAL_CANCEL";

    // ==================== GL Posting Engine Permissions ====================

    /** عرض مستندات الترحيل */
    public static final String GL_POSTING_VIEW = "PERM_GL_POSTING_VIEW";

    /** إنشاء مستند ترحيل */
    public static final String GL_POSTING_CREATE = "PERM_GL_POSTING_CREATE";

    /** تعديل/تنفيذ ترحيل */
    public static final String GL_POSTING_UPDATE = "PERM_GL_POSTING_UPDATE";

    // ==================== System Admin ====================

    /** صلاحية مدير النظام الكاملة */
    public static final String SYSTEM_ADMIN = "PERM_SYSTEM_ADMIN";
}
