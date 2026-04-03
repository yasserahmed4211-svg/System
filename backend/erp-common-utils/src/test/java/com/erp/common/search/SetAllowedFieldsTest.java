package com.erp.common.search;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SetAllowedFields}.
 *
 * @author ERP System
 * @since 1.0
 */
class SetAllowedFieldsTest {

    @Test
    void testIsAllowed_SimpleField() {
        AllowedFields allowedFields = new SetAllowedFields(Set.of("username", "email", "enabled"));

        assertTrue(allowedFields.isAllowed("username"));
        assertTrue(allowedFields.isAllowed("email"));
        assertTrue(allowedFields.isAllowed("enabled"));
        assertFalse(allowedFields.isAllowed("password"));
        assertFalse(allowedFields.isAllowed("id"));
    }

    @Test
    void testIsAllowed_NestedField() {
        AllowedFields allowedFields = new SetAllowedFields(
                Set.of("username", "tenant.id", "tenant.name", "roles.name")
        );

        assertTrue(allowedFields.isAllowed("username"));
        assertTrue(allowedFields.isAllowed("tenant.id"));
        assertTrue(allowedFields.isAllowed("tenant.name"));
        assertTrue(allowedFields.isAllowed("roles.name"));
        assertFalse(allowedFields.isAllowed("tenant"));
        assertFalse(allowedFields.isAllowed("tenant.code"));
        assertFalse(allowedFields.isAllowed("roles.permissions.name"));
    }

    @Test
    void testIsAllowed_NullField() {
        AllowedFields allowedFields = new SetAllowedFields(Set.of("username"));

        assertFalse(allowedFields.isAllowed(null));
    }

    @Test
    void testIsAllowed_EmptySet() {
        AllowedFields allowedFields = new SetAllowedFields(Set.of());

        assertFalse(allowedFields.isAllowed("username"));
        assertFalse(allowedFields.isAllowed("anyField"));
    }

    @Test
    void testValidateField_Success() {
        AllowedFields allowedFields = new SetAllowedFields(Set.of("username", "email"));

        assertDoesNotThrow(() -> allowedFields.validateField("username"));
        assertDoesNotThrow(() -> allowedFields.validateField("email"));
    }

    @Test
    void testValidateField_ThrowsException() {
        AllowedFields allowedFields = new SetAllowedFields(Set.of("username", "email"));

        SearchException exception = assertThrows(
                SearchException.class,
                () -> allowedFields.validateField("password")
        );

        assertTrue(exception.getMessage().contains("password"));
        assertTrue(exception.getMessage().contains("not allowed"));
    }

    @Test
    void testConstructor_NullSet() {
        AllowedFields allowedFields = new SetAllowedFields(null);

        assertFalse(allowedFields.isAllowed("anyField"));
    }

    @Test
    void testGetAllowedFields() {
        Set<String> fields = Set.of("username", "email", "enabled");
        SetAllowedFields allowedFields = new SetAllowedFields(fields);

        Set<String> returnedFields = allowedFields.getAllowedFields();
        assertEquals(fields, returnedFields);

        // Verify immutability
        assertThrows(UnsupportedOperationException.class, () ->
                returnedFields.add("newField")
        );
    }
}
