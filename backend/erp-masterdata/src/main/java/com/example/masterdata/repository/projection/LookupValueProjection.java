package com.example.masterdata.repository.projection;

/**
 * Projection interface for lookup values with native query
 * Returns master lookup validation info and detail values in one query
 * 
 * Architecture Rule 27: Use projections for performance optimization
 * 
 * @author ERP Team
 */
public interface LookupValueProjection {
    
    /**
     * Master lookup ID
     */
    Long getMasterLookupId();
    
    /**
     * Master lookup key
     */
    String getLookupKey();
    
    /**
     * Master lookup active status
     */
    Integer getMasterIsActive();
    
    /**
     * Lookup detail code/value
     */
    String getCode();
    
    /**
     * Lookup detail Arabic name
     */
    String getNameAr();
    
    /**
     * Lookup detail English name
     */
    String getNameEn();
    
    /**
     * Lookup detail sort order
     */
    Integer getSortOrder();
    
    /**
     * Lookup detail active status
     */
    Integer getDetailIsActive();
}
