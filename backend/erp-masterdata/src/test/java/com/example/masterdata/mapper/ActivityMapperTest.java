package com.example.masterdata.mapper;

import com.example.masterdata.dto.ActivityCreateRequest;
import com.example.masterdata.dto.ActivityResponse;
import com.example.masterdata.dto.ActivityUpdateRequest;
import com.example.masterdata.entity.Activity;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ActivityMapper
 *
 * Covers:
 * - toEntity from CreateRequest
 * - updateEntity from UpdateRequest
 * - toResponse from Entity
 *
 * @author ERP Team
 */
class ActivityMapperTest {

    private final ActivityMapper activityMapper = new ActivityMapper();

    // =========================================================================
    // toEntity
    // =========================================================================

    @Test
    void toEntity_mapsAllFieldsAndUppercasesCode() {
        ActivityCreateRequest request = ActivityCreateRequest.builder()
                .code("manufacturing")  // should be uppercased
                .name("Manufacturing")
                .description("Test desc")
                .conversionType(Activity.ConversionType.FIXED)
                .requiresActualWeight(false)
                .allowFraction(true)
                .build();

        Activity entity = activityMapper.toEntity(request);

        assertNotNull(entity);
        assertEquals("MANUFACTURING", entity.getCode());  // uppercase enforced
        assertEquals("Manufacturing", entity.getName());
        assertEquals("Test desc", entity.getDescription());
        assertEquals(Activity.ConversionType.FIXED, entity.getConversionType());
        assertFalse(entity.getRequiresActualWeight());
        assertTrue(entity.getAllowFraction());
        assertTrue(entity.getIsActive(), "new activities must be active by default");
    }

    @Test
    void toEntity_variableConversionType_mappedCorrectly() {
        ActivityCreateRequest request = ActivityCreateRequest.builder()
                .code("VARIABLE_ACT")
                .name("Variable Activity")
                .conversionType(Activity.ConversionType.VARIABLE)
                .requiresActualWeight(true)
                .allowFraction(false)
                .build();

        Activity entity = activityMapper.toEntity(request);

        assertEquals(Activity.ConversionType.VARIABLE, entity.getConversionType());
        assertTrue(entity.getRequiresActualWeight());
    }

    // =========================================================================
    // updateEntity
    // =========================================================================

    @Test
    void updateEntity_updatesAllMutableFields() {
        Activity existing = Activity.builder()
                .code("OLD_CODE")
                .name("Old Name")
                .description("Old desc")
                .conversionType(Activity.ConversionType.FIXED)
                .requiresActualWeight(false)
                .allowFraction(false)
                .build();

        ActivityUpdateRequest update = ActivityUpdateRequest.builder()
                .name("New Name")
                .description("New desc")
                .conversionType(Activity.ConversionType.VARIABLE)
                .requiresActualWeight(true)
                .allowFraction(true)
                .build();

        activityMapper.updateEntity(existing, update);

        assertEquals("New Name", existing.getName());
        assertEquals("New desc", existing.getDescription());
        assertEquals(Activity.ConversionType.VARIABLE, existing.getConversionType());
        assertTrue(existing.getRequiresActualWeight());
        assertTrue(existing.getAllowFraction());
        // code must NOT change – it is immutable in the service contract
        assertEquals("OLD_CODE", existing.getCode());
    }

    // =========================================================================
    // toResponse
    // =========================================================================

    @Test
    void toResponse_mapsAllFields() {
        Activity entity = Activity.builder()
                .id(5L)
                .code("MFGR")
                .name("Manufacturing")
                .description("Desc")
                .conversionType(Activity.ConversionType.FIXED)
                .requiresActualWeight(false)
                .allowFraction(true)
                .isActive(true)
                .createdAt(Instant.now())
                .createdBy("admin")
                .build();

        ActivityResponse response = activityMapper.toResponse(entity);

        assertNotNull(response);
        assertEquals(5L, response.getId());
        assertEquals("MFGR", response.getCode());
        assertEquals("Manufacturing", response.getName());
        assertEquals(Activity.ConversionType.FIXED, response.getConversionType());
        assertFalse(response.getRequiresActualWeight());
        assertTrue(response.getAllowFraction());
        assertTrue(response.getIsActive());
        assertEquals("admin", response.getCreatedBy());
    }
}
