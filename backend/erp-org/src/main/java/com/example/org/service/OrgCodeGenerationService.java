package com.example.org.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Centralized Code Generation Service for Organization Module.
 * <p>
 * Uses DB sequences to guarantee unique, never-reused business codes
 * for all four org entities: LegalEntity, Region, Branch, Department.
 */
@Service
@Slf4j
public class OrgCodeGenerationService {

    @PersistenceContext
    private EntityManager entityManager;

    private static final String LE_PREFIX = "LE-";
    private static final String RGN_PREFIX = "RGN-";
    private static final String BRN_PREFIX = "BRN-";
    private static final String DEP_PREFIX = "DEP-";

    /**
     * Generate next LegalEntity code: LE-001, LE-002, ...
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String generateLegalEntityCode() {
        long nextVal = getNextSequenceValue("SEQ_LEGAL_ENTITY_CODE");
        String code = LE_PREFIX + String.format("%03d", nextVal);
        log.debug("Generated legal entity code: {}", code);
        return code;
    }

    /**
     * Generate next Region code: RGN-001, RGN-002, ...
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String generateRegionCode() {
        long nextVal = getNextSequenceValue("SEQ_REGION_CODE");
        String code = RGN_PREFIX + String.format("%03d", nextVal);
        log.debug("Generated region code: {}", code);
        return code;
    }

    /**
     * Generate next Branch code: BRN-001, BRN-002, ...
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String generateBranchCode() {
        long nextVal = getNextSequenceValue("SEQ_BRANCH_CODE");
        String code = BRN_PREFIX + String.format("%03d", nextVal);
        log.debug("Generated branch code: {}", code);
        return code;
    }

    /**
     * Generate next Department code: DEP-001, DEP-002, ...
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String generateDepartmentCode() {
        long nextVal = getNextSequenceValue("SEQ_DEPARTMENT_CODE");
        String code = DEP_PREFIX + String.format("%03d", nextVal);
        log.debug("Generated department code: {}", code);
        return code;
    }

    private long getNextSequenceValue(String sequenceName) {
        Number result = (Number) entityManager
                .createNativeQuery("SELECT " + sequenceName + ".NEXTVAL FROM DUAL")
                .getSingleResult();
        return result.longValue();
    }
}
