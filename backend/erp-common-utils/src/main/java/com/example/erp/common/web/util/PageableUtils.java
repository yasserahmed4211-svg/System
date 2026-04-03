package com.example.erp.common.web.util;

import com.example.erp.common.exception.BusinessException;
import com.example.erp.common.exception.CommonErrorCodes;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * Pagination Utilities
 * 
 * Centralizes pagination constraint enforcement to avoid duplication
 * across multiple controllers.
 * 
 * Rules enforced:
 * - Maximum page number: 10,000
 * - Minimum page size: 1
 * - Maximum page size: 100
 * 
 * @author Architecture Review Team
 * @see com.example.erp.common.web.config.CommonWebConfig for default pagination settings
 */
public final class PageableUtils {

    /**
     * Maximum allowed page number to prevent excessive pagination
     */
    public static final int MAX_PAGE_NUMBER = 10_000;
    
    /**
     * Minimum allowed page size
     */
    public static final int MIN_PAGE_SIZE = 1;
    
    /**
     * Maximum allowed page size to prevent memory issues
     */
    public static final int MAX_PAGE_SIZE = 100;

    /**
     * Private constructor to prevent instantiation
     */
    private PageableUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Enforce pagination constraints on a Pageable object
     * 
     * This method ensures that:
     * - Page number does not exceed MAX_PAGE_NUMBER
     * - Page size is between MIN_PAGE_SIZE and MAX_PAGE_SIZE
     * - Sort order is preserved from original Pageable
     * 
     * Example usage in controllers:
     * <pre>
     * {@code
     * @GetMapping
     * public Page<UserResponse> getAllUsers(Pageable pageable) {
     *     pageable = PageableUtils.enforceConstraints(pageable);
     *     return userService.getAllUsers(pageable);
     * }
     * }
     * </pre>
     * 
     * @param pageable Original Pageable object from request
     * @return New Pageable with enforced constraints
     * @throws BusinessException if pageable is null
     */
    public static Pageable enforceConstraints(Pageable pageable) {
        if (pageable == null) {
            throw new BusinessException(CommonErrorCodes.PAGEABLE_NULL, "Pageable cannot be null");
        }

        // Enforce page number constraint
        int pageNumber = Math.min(pageable.getPageNumber(), MAX_PAGE_NUMBER);
        
        // Enforce page size constraints
        int pageSize = Math.min(
            Math.max(pageable.getPageSize(), MIN_PAGE_SIZE), 
            MAX_PAGE_SIZE
        );
        
        // Preserve sort order from original Pageable
        return PageRequest.of(pageNumber, pageSize, pageable.getSort());
    }

    /**
     * Enforce pagination constraints with custom maximum page size
     * 
     * Useful when a specific endpoint needs a different maximum page size
     * than the global default.
     * 
     * @param pageable Original Pageable object from request
     * @param customMaxSize Custom maximum page size for this request
     * @return New Pageable with enforced constraints
     * @throws BusinessException if pageable is null or customMaxSize is invalid
     */
    public static Pageable enforceConstraints(Pageable pageable, int customMaxSize) {
        if (pageable == null) {
            throw new BusinessException(CommonErrorCodes.PAGEABLE_NULL, "Pageable cannot be null");
        }
        
        if (customMaxSize < MIN_PAGE_SIZE) {
            throw new BusinessException(
                CommonErrorCodes.PAGEABLE_INVALID_MAX_SIZE,
                "Custom max size must be at least " + MIN_PAGE_SIZE
            );
        }

        int pageNumber = Math.min(pageable.getPageNumber(), MAX_PAGE_NUMBER);
        
        int pageSize = Math.min(
            Math.max(pageable.getPageSize(), MIN_PAGE_SIZE), 
            customMaxSize
        );
        
        return PageRequest.of(pageNumber, pageSize, pageable.getSort());
    }

    /**
     * Check if a Pageable object violates constraints
     * 
     * Useful for logging or monitoring purposes to identify
     * clients that are requesting excessive page sizes or numbers.
     * 
     * @param pageable Pageable to check
     * @return true if constraints are violated, false otherwise
     */
    public static boolean violatesConstraints(Pageable pageable) {
        if (pageable == null) {
            return false;
        }
        
        return pageable.getPageNumber() > MAX_PAGE_NUMBER
            || pageable.getPageSize() < MIN_PAGE_SIZE
            || pageable.getPageSize() > MAX_PAGE_SIZE;
    }
}
