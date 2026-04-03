package com.erp.common.search;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SearchFilter} and {@link SearchRequest}.
 *
 * @author ERP System
 * @since 1.0
 */
class SearchRequestTest {

    @Test
    void testSearchFilter_Construction() {
        SearchFilter filter = new SearchFilter("username", Op.LIKE, "john");

        assertEquals("username", filter.getField());
        assertEquals(Op.LIKE, filter.getOp());
        assertEquals("john", filter.getValue());
    }

    @Test
    void testSearchFilter_SettersAndGetters() {
        SearchFilter filter = new SearchFilter();
        filter.setField("email");
        filter.setOp(Op.EQ);
        filter.setValue("test@example.com");

        assertEquals("email", filter.getField());
        assertEquals(Op.EQ, filter.getOp());
        assertEquals("test@example.com", filter.getValue());
    }

    @Test
    void testSearchFilter_NestedField() {
        SearchFilter filter = new SearchFilter("tenant.id", Op.EQ, 1L);

        assertEquals("tenant.id", filter.getField());
        assertTrue(filter.getField().contains("."));
    }

    @Test
    void testSearchFilter_CollectionField() {
        SearchFilter filter = new SearchFilter("roles.name", Op.IN, Arrays.asList("ADMIN", "USER"));

        assertEquals("roles.name", filter.getField());
        assertEquals(Op.IN, filter.getOp());
        assertTrue(filter.getValue() instanceof List);
    }

    @Test
    void testSearchFilter_NullValue() {
        SearchFilter filter = new SearchFilter("deletedAt", Op.IS_NULL, null);

        assertEquals("deletedAt", filter.getField());
        assertEquals(Op.IS_NULL, filter.getOp());
        assertNull(filter.getValue());
    }

    @Test
    void testSearchFilter_Equals() {
        SearchFilter filter1 = new SearchFilter("username", Op.LIKE, "john");
        SearchFilter filter2 = new SearchFilter("username", Op.LIKE, "john");
        SearchFilter filter3 = new SearchFilter("email", Op.LIKE, "john");

        assertEquals(filter1, filter2);
        assertNotEquals(filter1, filter3);
    }

    @Test
    void testSearchFilter_HashCode() {
        SearchFilter filter1 = new SearchFilter("username", Op.LIKE, "john");
        SearchFilter filter2 = new SearchFilter("username", Op.LIKE, "john");

        assertEquals(filter1.hashCode(), filter2.hashCode());
    }

    @Test
    void testSearchFilter_ToString() {
        SearchFilter filter = new SearchFilter("username", Op.LIKE, "john");
        String toString = filter.toString();

        assertTrue(toString.contains("username"));
        assertTrue(toString.contains("LIKE"));
        assertTrue(toString.contains("john"));
    }

    @Test
    void testSearchRequest_DefaultValues() {
        SearchRequest request = new SearchRequest();

        assertNotNull(request.getFilters());
        assertTrue(request.getFilters().isEmpty());
        assertEquals(0, request.getPage());
        assertEquals(20, request.getSize());
        assertNull(request.getSortDir()); // Default is null, PageableBuilder will use DESC
        assertNull(request.getSortBy());
    }

    @Test
    void testSearchRequest_AddFilter() {
        SearchRequest request = new SearchRequest();
        SearchFilter filter1 = new SearchFilter("username", Op.LIKE, "john");
        SearchFilter filter2 = new SearchFilter("enabled", Op.EQ, true);

        request.addFilter(filter1);
        request.addFilter(filter2);

        assertEquals(2, request.getFilters().size());
        assertTrue(request.getFilters().contains(filter1));
        assertTrue(request.getFilters().contains(filter2));
    }

    @Test
    void testSearchRequest_AddNullFilter() {
        SearchRequest request = new SearchRequest();
        request.addFilter(null);

        assertTrue(request.getFilters().isEmpty());
    }

    @Test
    void testSearchRequest_MethodChaining() {
        SearchRequest request = new SearchRequest()
                .addFilter(new SearchFilter("username", Op.LIKE, "john"))
                .addFilter(new SearchFilter("enabled", Op.EQ, true));

        assertEquals(2, request.getFilters().size());
    }

    @Test
    void testSearchRequest_SetFilters() {
        SearchRequest request = new SearchRequest();
        List<SearchFilter> filters = Arrays.asList(
                new SearchFilter("username", Op.LIKE, "john"),
                new SearchFilter("email", Op.EQ, "test@example.com")
        );

        request.setFilters(filters);

        assertEquals(2, request.getFilters().size());
        assertEquals(filters, request.getFilters());
    }

    @Test
    void testSearchRequest_SetNullFilters() {
        SearchRequest request = new SearchRequest();
        request.setFilters(null);

        assertNotNull(request.getFilters());
        assertTrue(request.getFilters().isEmpty());
    }

    @Test
    void testSearchRequest_PaginationParameters() {
        SearchRequest request = new SearchRequest();
        request.setPage(3);
        request.setSize(50);

        assertEquals(3, request.getPage());
        assertEquals(50, request.getSize());
    }

    @Test
    void testSearchRequest_SortingParameters() {
        SearchRequest request = new SearchRequest();
        request.setSortBy("createdAt");
        request.setSortDir("DESC");

        assertEquals("createdAt", request.getSortBy());
        assertEquals("DESC", request.getSortDir());
    }

    @Test
    void testSearchRequest_CompleteExample() {
        SearchRequest request = new SearchRequest();
        request.addFilter(new SearchFilter("username", Op.LIKE, "john"));
        request.addFilter(new SearchFilter("tenant.id", Op.EQ, 1L));
        request.addFilter(new SearchFilter("roles.name", Op.IN, Arrays.asList("ADMIN", "USER")));
        request.setPage(2);
        request.setSize(25);
        request.setSortBy("username");
        request.setSortDir("ASC");

        assertEquals(3, request.getFilters().size());
        assertEquals(2, request.getPage());
        assertEquals(25, request.getSize());
        assertEquals("username", request.getSortBy());
        assertEquals("ASC", request.getSortDir());
    }

    @Test
    void testSearchRequest_Equals() {
        SearchRequest request1 = new SearchRequest();
        request1.addFilter(new SearchFilter("username", Op.LIKE, "john"));
        request1.setPage(1);
        request1.setSize(20);

        SearchRequest request2 = new SearchRequest();
        request2.addFilter(new SearchFilter("username", Op.LIKE, "john"));
        request2.setPage(1);
        request2.setSize(20);

        assertEquals(request1, request2);
    }

    @Test
    void testSearchRequest_ToString() {
        SearchRequest request = new SearchRequest();
        request.addFilter(new SearchFilter("username", Op.LIKE, "john"));
        String toString = request.toString();

        assertTrue(toString.contains("username"));
        assertTrue(toString.contains("LIKE"));
    }
}
