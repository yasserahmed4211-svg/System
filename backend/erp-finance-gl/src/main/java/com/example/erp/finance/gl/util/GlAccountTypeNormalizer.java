package com.example.erp.finance.gl.util;

import java.util.Map;

/**
 * Normalizes account type aliases to canonical GL_ACCOUNT_TYPE lookup codes.
 *
 * <p>Legacy datasets may store textual values (for example ASSETS) while
 * current lookup codes use numeric values (for example 1). This utility keeps
 * service behavior consistent across both representations.</p>
 */
public final class GlAccountTypeNormalizer {

    private static final Map<String, String> LEGACY_TO_CODE = Map.ofEntries(
            Map.entry("ASSET", "1"),
            Map.entry("ASSETS", "1"),
            Map.entry("LIABILITY", "2"),
            Map.entry("LIABILITIES", "2"),
            Map.entry("EQUITY", "3"),
            Map.entry("REVENUE", "4"),
            Map.entry("EXPENSE", "5"),
            Map.entry("EXPENSES", "5"),
            Map.entry("COGS", "6"),
            Map.entry("OTHER_COST", "7"),
            Map.entry("INTERNAL", "8"),
            Map.entry("STATISTICAL", "9")
    );

    private GlAccountTypeNormalizer() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Returns canonical account type code used by GL lookup validation.
     * Unknown values are returned uppercased to keep validation behavior strict.
     */
    public static String normalize(String rawValue) {
        if (rawValue == null) {
            return null;
        }
        String normalized = rawValue.trim().toUpperCase();
        if (normalized.isBlank()) {
            return normalized;
        }
        return LEGACY_TO_CODE.getOrDefault(normalized, normalized);
    }
}