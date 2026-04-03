package com.erp.common.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Request DTO for dynamic searching with pagination and sorting.
 * <p>
 * Encapsulates a list of filters (applied with AND logic), along with
 * pagination parameters (page, size) and sorting options (sortBy, sortDir).
 * </p>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>
 * SearchRequest request = new SearchRequest();
 * request.addFilter(new SearchFilter("username", Op.LIKE, "john"));
 * request.addFilter(new SearchFilter("enabled", Op.EQ, true));
 * request.setPage(0);
 * request.setSize(20);
 * request.setSortBy("username");
 * request.setSortDir("ASC");
 * </pre>
 *
 * @author ERP System
 * @since 1.0
 */
public class SearchRequest {

    private List<SearchFilter> filters = new ArrayList<>();

    /**
     * Page number (0-based).
     */
    private int page = 0;

    /**
     * Page size (number of records per page).
     */
    private int size = 20;

    /**
     * Field name to sort by (supports dot notation).
     */
    private String sortBy;

    /**
     * Sort direction: "ASC" or "DESC". Defaults to null (which means DESC in PageableBuilder).
     */
    private String sortDir;

    /**
     * Default constructor.
     */
    public SearchRequest() {
    }

    /**
     * Adds a filter to this search request.
     *
     * @param filter the filter to add
     * @return this SearchRequest for method chaining
     */
    public SearchRequest addFilter(SearchFilter filter) {
        if (filter != null) {
            this.filters.add(filter);
        }
        return this;
    }

    public List<SearchFilter> getFilters() {
        return filters;
    }

    public void setFilters(List<SearchFilter> filters) {
        this.filters = filters != null ? filters : new ArrayList<>();
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String getSortDir() {
        return sortDir;
    }

    public void setSortDir(String sortDir) {
        this.sortDir = sortDir;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SearchRequest that = (SearchRequest) o;
        return page == that.page &&
               size == that.size &&
               Objects.equals(filters, that.filters) &&
               Objects.equals(sortBy, that.sortBy) &&
               Objects.equals(sortDir, that.sortDir);
    }

    @Override
    public int hashCode() {
        return Objects.hash(filters, page, size, sortBy, sortDir);
    }

    @Override
    public String toString() {
        return "SearchRequest{" +
               "filters=" + filters +
               ", page=" + page +
               ", size=" + size +
               ", sortBy='" + sortBy + '\'' +
               ", sortDir='" + sortDir + '\'' +
               '}';
    }
}
