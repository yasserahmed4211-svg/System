package com.example.erp.common.converter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BooleanCharYNConverterTest {

    private final BooleanCharYNConverter converter = new BooleanCharYNConverter();

    @Test
    void convertToDatabaseColumn_mapsTrueFalseNull() {
        assertEquals("Y", converter.convertToDatabaseColumn(Boolean.TRUE));
        assertEquals("N", converter.convertToDatabaseColumn(Boolean.FALSE));
        assertNull(converter.convertToDatabaseColumn(null));
    }

    @Test
    void convertToEntityAttribute_mapsValidValues() {
        assertEquals(Boolean.TRUE, converter.convertToEntityAttribute("Y"));
        assertEquals(Boolean.FALSE, converter.convertToEntityAttribute("N"));
        assertEquals(Boolean.TRUE, converter.convertToEntityAttribute("y"));
        assertEquals(Boolean.FALSE, converter.convertToEntityAttribute("n"));
        assertEquals(Boolean.TRUE, converter.convertToEntityAttribute("  Y  "));
        assertEquals(Boolean.FALSE, converter.convertToEntityAttribute("  N  "));
        assertNull(converter.convertToEntityAttribute(null));
        assertNull(converter.convertToEntityAttribute(" "));
        assertNull(converter.convertToEntityAttribute(""));
    }

    @Test
    void convertToEntityAttribute_rejectsInvalidValues() {
        assertThrows(IllegalArgumentException.class, () -> converter.convertToEntityAttribute("1"));
        assertThrows(IllegalArgumentException.class, () -> converter.convertToEntityAttribute("0"));
        assertThrows(IllegalArgumentException.class, () -> converter.convertToEntityAttribute("T"));
        assertThrows(IllegalArgumentException.class, () -> converter.convertToEntityAttribute("YES"));
    }
}
