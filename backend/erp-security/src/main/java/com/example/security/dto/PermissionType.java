package com.example.security.dto;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Enum for CRUD permission types
 * Maps to permission keys: PERM_<CODE>_<TYPE>
 * 
 * Permission Naming Convention:
 * - Format: PERM_{PAGE_CODE}_{TYPE}
 * - PAGE_CODE may contain underscores (e.g., MASTER_LOOKUP)
 * - TYPE is always one of: VIEW, CREATE, UPDATE, DELETE
 * 
 * Performance: Uses compiled regex pattern for O(1) parsing
 */
public enum PermissionType {
    VIEW,
    CREATE,
    UPDATE,
    DELETE;

    private static final String PERM_PREFIX = "PERM_";
    
    // Pre-compiled regex for optimal performance in ERP systems with many permissions
    // Pattern: PERM_<PAGE_CODE>_<TYPE> where TYPE is VIEW|CREATE|UPDATE|DELETE
    private static final Pattern PERMISSION_PATTERN = Pattern.compile(
        "^PERM_(.+)_(VIEW|CREATE|UPDATE|DELETE)$"
    );

    /**
     * Build full permission key from page code and type
     * Example: buildPermissionKey("MASTER_LOOKUP") -> "PERM_MASTER_LOOKUP_VIEW"
     */
    public String buildPermissionKey(String pageCode) {
        return PERM_PREFIX + pageCode.toUpperCase() + "_" + this.name();
    }

    /**
     * Parse permission name and extract both page code and type in single operation.
     * Optimized for ERP systems with many permissions - uses compiled regex.
     * 
     * @param permissionName Full permission name (e.g., "PERM_MASTER_LOOKUP_CREATE")
     * @return ParsedPermission containing pageCode and type, or null if invalid
     */
    public static ParsedPermission parse(String permissionName) {
        if (permissionName == null) {
            return null;
        }
        
        Matcher matcher = PERMISSION_PATTERN.matcher(permissionName);
        if (!matcher.matches()) {
            return null;
        }
        
        String pageCode = matcher.group(1);
        PermissionType type = valueOf(matcher.group(2));
        
        return new ParsedPermission(pageCode, type);
    }

    /**
     * Check if a permission name is a valid page permission
     */
    public static boolean isPagePermission(String permissionName) {
        return permissionName != null && PERMISSION_PATTERN.matcher(permissionName).matches();
    }

    /**
     * Immutable record for parsed permission data
     * More efficient than returning two Optional objects
     */
    public record ParsedPermission(String pageCode, PermissionType type) {
        
        public boolean isView() {
            return type == VIEW;
        }
        
        public boolean isCrud() {
            return type != VIEW;
        }
    }
}
