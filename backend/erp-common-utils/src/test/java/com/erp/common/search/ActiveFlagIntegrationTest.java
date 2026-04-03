package com.erp.common.search;

import com.example.erp.common.converter.BooleanNumberConverter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests demonstrating the complete Active Flag handling flow.
 * 
 * <h2>Test Scenarios:</h2>
 * <ul>
 *   <li>Search filtering with isActive = true (active only)</li>
 *   <li>Search filtering with isActive = false (inactive only)</li>
 *   <li>Search filtering with isActive = null (ALL records)</li>
 *   <li>Reading legacy data with various representations</li>
 * </ul>
 * 
 * This demonstrates the unified handling pattern for Active/Inactive flags.
 */
@DisplayName("Active Flag Integration Tests")
class ActiveFlagIntegrationTest {

    @Nested
    @DisplayName("Scenario: Search filtering with Boolean isActive parameter")
    class SearchFilteringScenarios {

        private final BooleanFieldValueConverter searchConverter = 
            BooleanFieldValueConverter.forActiveFields();

        @Test
        @DisplayName("isActive = true → query for active records (IS_ACTIVE = 1)")
        void searchActiveOnly() {
            // Simulates: frontend sends isActive=true
            Boolean filterValue = Boolean.TRUE;
            
            // Search converter transforms Boolean to Integer for DB query
            Integer dbQueryValue = (Integer) searchConverter.convert("isActive", filterValue, Op.EQ);
            
            // Verify: DB query will use IS_ACTIVE = 1
            assertEquals(1, dbQueryValue);
            
            // This would be used in a query like:
            // WHERE (:isActive IS NULL OR IS_ACTIVE = :isActive)
            // With dbQueryValue = 1, only active records are returned
        }

        @Test
        @DisplayName("isActive = false → query for inactive records (IS_ACTIVE = 0)")
        void searchInactiveOnly() {
            Boolean filterValue = Boolean.FALSE;
            
            Integer dbQueryValue = (Integer) searchConverter.convert("isActive", filterValue, Op.EQ);
            
            assertEquals(0, dbQueryValue);
            // WHERE IS_ACTIVE = 0 → only inactive records
        }

        @Test
        @DisplayName("isActive = null → query for ALL records (no filter)")
        void searchAll() {
            Boolean filterValue = null;
            
            Integer dbQueryValue = (Integer) searchConverter.convert("isActive", filterValue, Op.EQ);
            
            assertNull(dbQueryValue);
            // WHERE (:isActive IS NULL OR IS_ACTIVE = :isActive)
            // With null, the first condition is true → all records returned
        }
    }

    @Nested
    @DisplayName("Scenario: Reading data from DB")
    class ReadingDataScenarios {

        private final BooleanNumberConverter converter = new BooleanNumberConverter();

        @Test
        @DisplayName("DB value 1 → entity.isActive = true → DTO.isActive = true")
        void readActiveRecord() {
            // Simulate reading from DB: IS_ACTIVE = 1
            Integer dbValue = 1;
            
            // JPA converter transforms to Boolean
            Boolean entityValue = converter.convertToEntityAttribute(dbValue);
            assertTrue(entityValue);
            
            // DTO exposes Boolean directly to frontend
            // Frontend receives: { "isActive": true }
        }

        @Test
        @DisplayName("DB value 0 → entity.isActive = false → DTO.isActive = false")
        void readInactiveRecord() {
            Integer dbValue = 0;
            
            Boolean entityValue = converter.convertToEntityAttribute(dbValue);
            assertFalse(entityValue);
            
            // Frontend receives: { "isActive": false }
        }

        @Test
        @DisplayName("DB value NULL → entity.isActive = null → DTO.isActive = null")
        void readNullRecord() {
            Integer dbValue = null;
            
            Boolean entityValue = converter.convertToEntityAttribute(dbValue);
            assertNull(entityValue);
            
            // Frontend receives: { "isActive": null }
        }
    }

    @Nested
    @DisplayName("Scenario: Writing data to DB")
    class WritingDataScenarios {

        private final BooleanNumberConverter converter = new BooleanNumberConverter();

        @Test
        @DisplayName("DTO.isActive = true → entity.isActive = true → DB value 1")
        void writeActiveRecord() {
            // Request from frontend: { "isActive": true }
            Boolean dtoValue = true;
            
            // Set on entity
            Boolean entityValue = dtoValue;
            
            // JPA converter transforms for DB
            Integer dbValue = converter.convertToDatabaseColumn(entityValue);
            
            assertEquals(1, dbValue);
            // INSERT/UPDATE with IS_ACTIVE = 1
        }

        @Test
        @DisplayName("DTO.isActive = false → entity.isActive = false → DB value 0")
        void writeInactiveRecord() {
            Boolean dtoValue = false;
            Boolean entityValue = dtoValue;
            Integer dbValue = converter.convertToDatabaseColumn(entityValue);
            
            assertEquals(0, dbValue);
            // INSERT/UPDATE with IS_ACTIVE = 0
        }
    }

    @Nested
    @DisplayName("Scenario: Legacy data handling")
    class LegacyDataHandling {

        @Test
        @DisplayName("String 'true'/'false' from API converted correctly")
        void legacyStringValues() {
            BooleanFieldValueConverter converter = BooleanFieldValueConverter.forActiveFields();
            
            // Old API might send string values
            assertEquals(1, converter.convert("isActive", "true", Op.EQ));
            assertEquals(0, converter.convert("isActive", "false", Op.EQ));
            assertEquals(1, converter.convert("isActive", "1", Op.EQ));
            assertEquals(0, converter.convert("isActive", "0", Op.EQ));
            assertEquals(1, converter.convert("isActive", "yes", Op.EQ));
            assertEquals(0, converter.convert("isActive", "no", Op.EQ));
        }

        @Test
        @DisplayName("Numeric values from API converted correctly")
        void legacyNumericValues() {
            BooleanFieldValueConverter converter = BooleanFieldValueConverter.forActiveFields();
            
            // Integer values pass through validation
            assertEquals(1, converter.convert("isActive", 1, Op.EQ));
            assertEquals(0, converter.convert("isActive", 0, Op.EQ));
            
            // Long values converted
            assertEquals(1, converter.convert("isActive", 1L, Op.EQ));
            assertEquals(0, converter.convert("isActive", 0L, Op.EQ));
        }

        @Test
        @DisplayName("Invalid legacy values rejected")
        void invalidLegacyValues() {
            BooleanFieldValueConverter converter = BooleanFieldValueConverter.forActiveFields();
            
            // Invalid strings rejected
            assertThrows(SearchException.class, 
                () -> converter.convert("isActive", "active", Op.EQ));
            assertThrows(SearchException.class, 
                () -> converter.convert("isActive", "Y", Op.EQ)); // Use "yes" instead
            
            // Invalid numbers rejected  
            assertThrows(SearchException.class, 
                () -> converter.convert("isActive", 2, Op.EQ));
            assertThrows(SearchException.class, 
                () -> converter.convert("isActive", -1, Op.EQ));
        }
    }

    @Nested
    @DisplayName("Scenario: End-to-end API contract")
    class ApiContractScenarios {

        @Test
        @DisplayName("API never exposes 0/1 or Y/N - only Boolean")
        void apiContractValidation() {
            BooleanNumberConverter dbConverter = new BooleanNumberConverter();
            
            // When reading from DB
            Integer dbActive = 1;
            Integer dbInactive = 0;
            
            // Entity has Boolean
            Boolean entityActive = dbConverter.convertToEntityAttribute(dbActive);
            Boolean entityInactive = dbConverter.convertToEntityAttribute(dbInactive);
            
            // DTO/API exposes Boolean
            // This is what frontend sees (not 0/1 or Y/N)
            assertTrue(entityActive);
            assertFalse(entityInactive);
            
            // Jackson will serialize as:
            // { "isActive": true } or { "isActive": false }
            // NEVER: { "isActive": 1 } or { "isActive": "Y" }
        }
    }
}
