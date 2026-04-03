package com.erp.common.search;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link PageableBuilder}.
 *
 * @author ERP System
 * @since 1.0
 */
class PageableBuilderTest {

    @Test
    void testFrom_ValidRequest() {
        SearchRequest request = new SearchRequest();
        request.setPage(2);
        request.setSize(25);
        request.setSortBy("username");
        request.setSortDir("DESC");

        Set<String> allowedSortFields = Set.of("id", "username", "email");
        Pageable pageable = PageableBuilder.from(request, allowedSortFields);

        assertEquals(2, pageable.getPageNumber());
        assertEquals(25, pageable.getPageSize());
        assertEquals(Sort.by(Sort.Direction.DESC, "username"), pageable.getSort());
    }

    @Test
    void testFrom_DefaultValues() {
        SearchRequest request = new SearchRequest();
        // No sort specified

        Set<String> allowedSortFields = Set.of("id", "username");
        Pageable pageable = PageableBuilder.from(request, allowedSortFields);

        assertEquals(0, pageable.getPageNumber());
        assertEquals(PageableBuilder.DEFAULT_PAGE_SIZE, pageable.getPageSize());
        assertEquals(Sort.by(Sort.Direction.DESC, PageableBuilder.DEFAULT_SORT_FIELD), pageable.getSort());
    }

    @Test
    void testFrom_NullRequest() {
        Set<String> allowedSortFields = Set.of("id", "username");
        Pageable pageable = PageableBuilder.from(null, allowedSortFields);

        assertEquals(0, pageable.getPageNumber());
        assertEquals(PageableBuilder.DEFAULT_PAGE_SIZE, pageable.getPageSize());
        assertEquals(Sort.by(Sort.Direction.DESC, PageableBuilder.DEFAULT_SORT_FIELD), pageable.getSort());
    }

    @Test
    void testFrom_ExceedsMaxSize_ThrowsException() {
        SearchRequest request = new SearchRequest();
        request.setSize(200); // Exceeds MAX_PAGE_SIZE

        Set<String> allowedSortFields = Set.of("id");

        SearchException exception = assertThrows(
                SearchException.class,
                () -> PageableBuilder.from(request, allowedSortFields)
        );

        assertTrue(exception.getMessage().contains("Page size must not exceed 100"));
    }

    @Test
    void testFrom_NegativePage() {
        SearchRequest request = new SearchRequest();
        request.setPage(-5);

        Set<String> allowedSortFields = Set.of("id");
        Pageable pageable = PageableBuilder.from(request, allowedSortFields);

        assertEquals(0, pageable.getPageNumber());
    }

    @Test
    void testFrom_InvalidSize() {
        SearchRequest request = new SearchRequest();
        request.setSize(0);

        Set<String> allowedSortFields = Set.of("id");
        Pageable pageable = PageableBuilder.from(request, allowedSortFields);

        assertEquals(PageableBuilder.DEFAULT_PAGE_SIZE, pageable.getPageSize());
    }

    @Test
    void testFrom_InvalidSortField_ThrowsException() {
        SearchRequest request = new SearchRequest();
        request.setSortBy("invalidField");

        Set<String> allowedSortFields = Set.of("id", "username");

        SearchException exception = assertThrows(
                SearchException.class,
                () -> PageableBuilder.from(request, allowedSortFields)
        );

        assertTrue(exception.getMessage().contains("invalidField"));
        assertTrue(exception.getMessage().contains("not allowed"));
    }

    @Test
    void testFrom_NullAllowedSortFields_NoValidation() {
        SearchRequest request = new SearchRequest();
        request.setSortBy("anyField");

        Pageable pageable = PageableBuilder.from(request, null);

        assertEquals(Sort.by(Sort.Direction.DESC, "anyField"), pageable.getSort());
    }

    @Test
    void testFrom_CustomDefaultSortField() {
        SearchRequest request = new SearchRequest();
        // No sortBy specified

        Set<String> allowedSortFields = Set.of("id", "username", "createdAt");
        Pageable pageable = PageableBuilder.from(request, allowedSortFields, "createdAt");

        assertEquals(Sort.by(Sort.Direction.DESC, "createdAt"), pageable.getSort());
    }

    @Test
    void testFrom_InvalidSortWithCustomDefault() {
        SearchRequest request = new SearchRequest();
        request.setSortBy("invalidField");

        Set<String> allowedSortFields = Set.of("id", "username", "createdAt");

        SearchException exception = assertThrows(
                SearchException.class,
                () -> PageableBuilder.from(request, allowedSortFields, "createdAt")
        );

        assertTrue(exception.getMessage().contains("invalidField"));
    }

    @Test
    void testFrom_SortDirectionCaseInsensitive() {
        SearchRequest request = new SearchRequest();
        request.setSortBy("username");
        request.setSortDir("asc"); // lowercase ASC to test non-default

        Set<String> allowedSortFields = Set.of("username");
        Pageable pageable = PageableBuilder.from(request, allowedSortFields);

        assertEquals(Sort.by(Sort.Direction.ASC, "username"), pageable.getSort());
    }

    @Test
    void testUnsorted_ValidRequest() {
        SearchRequest request = new SearchRequest();
        request.setPage(1);
        request.setSize(30);

        Pageable pageable = PageableBuilder.unsorted(request);

        assertEquals(1, pageable.getPageNumber());
        assertEquals(30, pageable.getPageSize());
        assertTrue(pageable.getSort().isUnsorted());
    }

    @Test
    void testUnsorted_NullRequest() {
        Pageable pageable = PageableBuilder.unsorted(null);

        assertEquals(0, pageable.getPageNumber());
        assertEquals(PageableBuilder.DEFAULT_PAGE_SIZE, pageable.getPageSize());
        assertTrue(pageable.getSort().isUnsorted());
    }

    @Test
    void testFrom_NullSortDir_DefaultsToDesc() {
        SearchRequest request = new SearchRequest();
        request.setSortBy("username");
        request.setSortDir(null);

        Set<String> allowedSortFields = Set.of("username");
        Pageable pageable = PageableBuilder.from(request, allowedSortFields);

        assertEquals(Sort.by(Sort.Direction.DESC, "username"), pageable.getSort());
    }

    @Test
    void testFrom_EmptySortDir_DefaultsToDesc() {
        SearchRequest request = new SearchRequest();
        request.setSortBy("username");
        request.setSortDir("");

        Set<String> allowedSortFields = Set.of("username");
        Pageable pageable = PageableBuilder.from(request, allowedSortFields);

        assertEquals(Sort.by(Sort.Direction.DESC, "username"), pageable.getSort());
    }

    @Test
    void testFrom_InvalidSortDir_DefaultsToDesc() {
        SearchRequest request = new SearchRequest();
        request.setSortBy("username");
        request.setSortDir("INVALID");

        Set<String> allowedSortFields = Set.of("username");
        Pageable pageable = PageableBuilder.from(request, allowedSortFields);

        assertEquals(Sort.by(Sort.Direction.DESC, "username"), pageable.getSort());
    }

    @Test
    void testFrom_EmptySortBy_UsesDefault() {
        SearchRequest request = new SearchRequest();
        request.setSortBy("");
        request.setSortDir("ASC");

        Set<String> allowedSortFields = Set.of("id", "username");
        Pageable pageable = PageableBuilder.from(request, allowedSortFields);

        assertEquals(Sort.by(Sort.Direction.ASC, "id"), pageable.getSort());
    }

    @Test
    void testFrom_WhitespaceSortBy_UsesDefault() {
        SearchRequest request = new SearchRequest();
        request.setSortBy("   ");
        request.setSortDir("ASC");

        Set<String> allowedSortFields = Set.of("id", "username");
        Pageable pageable = PageableBuilder.from(request, allowedSortFields);

        assertEquals(Sort.by(Sort.Direction.ASC, "id"), pageable.getSort());
    }
}
