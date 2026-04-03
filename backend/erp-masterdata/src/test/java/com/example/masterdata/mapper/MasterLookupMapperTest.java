package com.example.masterdata.mapper;

import com.example.masterdata.dto.*;
import com.example.masterdata.entity.MdLookupDetail;
import com.example.masterdata.entity.MdMasterLookup;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MasterLookupMapper and LookupDetailMapper
 *
 * Covers:
 * - Entity → DTO conversion (toResponse)
 * - DTO → Entity conversion (toEntity)
 * - Update mapping (updateEntityFromRequest)
 * - Null safety
 *
 * @author ERP Team
 */
class MasterLookupMapperTest {

    private final MasterLookupMapper masterLookupMapper = new MasterLookupMapper();
    private final LookupDetailMapper lookupDetailMapper = new LookupDetailMapper();

    // =========================================================================
    // MasterLookupMapper: toEntity
    // =========================================================================

    @Test
    void toEntity_fromCreateRequest_mapsAllFields() {
        MasterLookupCreateRequest request = MasterLookupCreateRequest.builder()
                .lookupKey("COLOR")
                .lookupName("اللون")
                .lookupNameEn("Color")
                .description("Product color")
                .isActive(true)
                .build();

        MdMasterLookup entity = masterLookupMapper.toEntity(request);

        assertNotNull(entity);
        assertEquals("COLOR", entity.getLookupKey());
        assertEquals("اللون", entity.getLookupName());
        assertEquals("Color", entity.getLookupNameEn());
        assertEquals("Product color", entity.getDescription());
        assertTrue(entity.getIsActive());
    }

    @Test
    void toEntity_nullRequest_returnsNull() {
        assertNull(masterLookupMapper.toEntity((MasterLookupCreateRequest) null));
    }

    @Test
    void toEntity_nullIsActive_defaultsToTrue() {
        MasterLookupCreateRequest request = MasterLookupCreateRequest.builder()
                .lookupKey("COLOR")
                .lookupName("اللون")
                .isActive(null) // null – should default to true
                .build();

        MdMasterLookup entity = masterLookupMapper.toEntity(request);

        assertNotNull(entity);
        assertTrue(entity.getIsActive(), "default isActive must be TRUE");
    }

    // =========================================================================
    // MasterLookupMapper: updateEntityFromRequest
    // =========================================================================

    @Test
    void updateEntityFromRequest_updatesAllUpdateableFields() {
        MdMasterLookup entity = MdMasterLookup.builder()
                .lookupKey("COLOR")
                .lookupName("اللون القديم")
                .lookupNameEn("Old Color")
                .description("Old description")
                .build();

        MasterLookupUpdateRequest request = MasterLookupUpdateRequest.builder()
                .lookupName("اللون الجديد")
                .lookupNameEn("New Color")
                .description("New description")
                .build();

        masterLookupMapper.updateEntityFromRequest(entity, request);

        assertEquals("اللون الجديد", entity.getLookupName());
        assertEquals("New Color", entity.getLookupNameEn());
        assertEquals("New description", entity.getDescription());
        // lookupKey must NOT be changed – it is immutable
        assertEquals("COLOR", entity.getLookupKey());
    }

    @Test
    void updateEntityFromRequest_nullEntity_doesNotThrow() {
        MasterLookupUpdateRequest request = MasterLookupUpdateRequest.builder()
                .lookupName("اللون").build();
        assertDoesNotThrow(() -> masterLookupMapper.updateEntityFromRequest(null, request));
    }

    @Test
    void updateEntityFromRequest_nullRequest_doesNotThrow() {
        MdMasterLookup entity = MdMasterLookup.builder().lookupKey("COLOR").build();
        assertDoesNotThrow(() -> masterLookupMapper.updateEntityFromRequest(entity, null));
    }

    // =========================================================================
    // MasterLookupMapper: toResponse
    // =========================================================================

    @Test
    void toResponse_fromEntity_mapsAllFields() {
        MdMasterLookup entity = MdMasterLookup.builder()
                .id(1L)
                .lookupKey("GRADE")
                .lookupName("الدرجة")
                .lookupNameEn("Grade")
                .description("Grade values")
                .isActive(true)
                .createdAt(Instant.now())
                .createdBy("admin")
                .build();

        MasterLookupResponse response = masterLookupMapper.toResponse(entity);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("GRADE", response.getLookupKey());
        assertEquals("الدرجة", response.getLookupName());
        assertEquals("Grade", response.getLookupNameEn());
        assertTrue(response.getIsActive());
    }

    // =========================================================================
    // LookupDetailMapper: toEntity
    // =========================================================================

    @Test
    void lookupDetailMapper_toEntity_mapsAllFields() {
        MdMasterLookup parent = MdMasterLookup.builder()
                .id(10L)
                .lookupKey("COLOR")
                .lookupName("اللون")
                .build();

        LookupDetailCreateRequest request = LookupDetailCreateRequest.builder()
                .masterLookupId(10L)
                .code("RED")
                .nameAr("أحمر")
                .nameEn("Red")
                .extraValue("#FF0000")
                .sortOrder(1)
                .isActive(true)
                .build();

        MdLookupDetail detail = lookupDetailMapper.toEntity(request, parent);

        assertNotNull(detail);
        assertEquals("RED", detail.getCode());
        assertEquals("أحمر", detail.getNameAr());
        assertEquals("Red", detail.getNameEn());
        assertEquals("#FF0000", detail.getExtraValue());
        assertEquals(1, detail.getSortOrder());
        assertTrue(detail.getIsActive());
        assertSame(parent, detail.getMasterLookup());
    }

    @Test
    void lookupDetailMapper_toEntity_nullRequest_returnsNull() {
        assertNull(lookupDetailMapper.toEntity(null, MdMasterLookup.builder().build()));
    }

    @Test
    void lookupDetailMapper_toEntity_nullSortOrder_defaultsToZero() {
        MdMasterLookup parent = MdMasterLookup.builder().id(1L).build();
        LookupDetailCreateRequest request = LookupDetailCreateRequest.builder()
                .code("RED")
                .nameAr("أحمر")
                .sortOrder(null)
                .build();

        MdLookupDetail detail = lookupDetailMapper.toEntity(request, parent);

        assertEquals(0, detail.getSortOrder(), "null sortOrder must default to 0");
    }

    // =========================================================================
    // LookupDetailMapper: updateEntityFromRequest
    // =========================================================================

    @Test
    void lookupDetailMapper_updateEntity_onlyUpdatesAllowedFields() {
        MdMasterLookup parent = MdMasterLookup.builder().id(10L).build();
        MdLookupDetail entity = MdLookupDetail.builder()
                .code("RED")
                .nameAr("أحمر قديم")
                .nameEn("Old Red")
                .sortOrder(0)
                .masterLookup(parent)
                .build();

        LookupDetailUpdateRequest request = LookupDetailUpdateRequest.builder()
                .nameAr("أحمر جديد")
                .nameEn("New Red")
                .extraValue("#CC0000")
                .sortOrder(5)
                .build();

        lookupDetailMapper.updateEntityFromRequest(entity, request);

        assertEquals("أحمر جديد", entity.getNameAr());
        assertEquals("New Red", entity.getNameEn());
        assertEquals("#CC0000", entity.getExtraValue());
        assertEquals(5, entity.getSortOrder());
        // code and masterLookup must NOT change – immutable per contract
        assertEquals("RED", entity.getCode());
        assertSame(parent, entity.getMasterLookup());
    }
}
