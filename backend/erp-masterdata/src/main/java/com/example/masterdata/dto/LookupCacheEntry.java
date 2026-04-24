package com.example.masterdata.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Cacheable wrapper for lookup consumption data.
 *
 * <p>Holds a status flag and the resolved list so that the cache can
 * distinguish three states without storing {@code null} (which Redis
 * cannot cache when {@code disableCachingNullValues} is set):</p>
 * <ul>
 *   <li>Key not found — returned as {@code null} (not cached)</li>
 *   <li>Master inactive — {@code inactive = true, values = []}</li>
 *   <li>Master active — {@code inactive = false, values = [...]}</li>
 * </ul>
 *
 * <p>Fully serializable by Jackson (no-args constructor + getters from
 * {@code @Data}) so {@code GenericJackson2JsonRedisSerializer} can
 * round-trip the object without errors.</p>
 *
 * @author ERP Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LookupCacheEntry {

    /** {@code true} when the master lookup is inactive. */
    private boolean inactive;

    /** Active detail values; may be empty for active masters with no details. */
    private List<LookupValueResponse> values;
}
