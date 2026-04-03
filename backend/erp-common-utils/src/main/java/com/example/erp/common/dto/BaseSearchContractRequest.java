package com.example.erp.common.dto;

import com.erp.common.search.Op;
import com.erp.common.search.SearchException;
import com.erp.common.search.SearchFilter;
import com.erp.common.search.SearchRequest;
import com.example.erp.common.exception.CommonErrorCodes;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Base class for API-contract search request DTOs.
 *
 * Maps the frontend search format:
 * <pre>{ filters: [{ field, operator, value }], sorts: [{ field, direction }], page, size }</pre>
 * into the common-utils {@link SearchRequest} used by services.
 *
 * Architecture Rules:
 * - Rule 7.1: DTOs for API contract
 * - Eliminates duplicated Filter/Sort classes across search DTOs
 *
 * @author ERP Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseSearchContractRequest {

    private List<ContractFilter> filters = new ArrayList<>();
    private List<ContractSort> sorts = new ArrayList<>();
    private int page = 0;
    private int size = 20;

    /**
     * Operator aliases accepted from the frontend contract.
     */
    private static final Set<String> EQUALS_ALIASES = Set.of("EQUALS", "EQ");
    private static final Set<String> CONTAINS_ALIASES = Set.of("CONTAINS", "LIKE");
    private static final Set<String> STARTS_WITH_ALIASES = Set.of("STARTS_WITH");

    /**
     * Map contract request into common-utils SearchRequest.
     * Subclasses may override to exclude certain filters.
     *
     * @return SearchRequest for service layer
     */
    public SearchRequest toCommonSearchRequest() {
        return toCommonSearchRequest(Set.of());
    }

    /**
     * Map contract request into common-utils SearchRequest,
     * excluding filters whose field name matches any entry in {@code excludeFields}.
     *
     * @param excludeFields field names to skip (case-insensitive)
     * @return SearchRequest for service layer
     */
    protected SearchRequest toCommonSearchRequest(Set<String> excludeFields) {
        SearchRequest req = new SearchRequest();
        req.setPage(this.page);
        req.setSize(this.size);

        // Map sorts → sortBy / sortDir (common SearchRequest supports single sort)
        if (sorts != null && !sorts.isEmpty() && sorts.get(0) != null) {
            ContractSort s = sorts.get(0);
            req.setSortBy(s.getField());
            req.setSortDir(s.getDirection());
        }

        // Map filters → SearchFilter with Op
        if (filters != null && !filters.isEmpty()) {
            List<SearchFilter> mapped = new ArrayList<>();
            for (ContractFilter f : filters) {
                if (f == null || f.getField() == null || f.getOperator() == null) {
                    continue;
                }
                if (excludeFields.stream().anyMatch(ex -> ex.equalsIgnoreCase(f.getField()))) {
                    continue;
                }
                Op op = mapOperator(f.getOperator());
                mapped.add(new SearchFilter(f.getField(), op, f.getValue()));
            }
            req.setFilters(mapped);
        }

        return req;
    }

    /**
     * Map frontend operator string to common-utils Op enum.
     */
    protected static Op mapOperator(String operator) {
        String normalized = operator.trim().toUpperCase();
        if (EQUALS_ALIASES.contains(normalized)) return Op.EQ;
        if (CONTAINS_ALIASES.contains(normalized)) return Op.LIKE;
        if (STARTS_WITH_ALIASES.contains(normalized)) return Op.STARTS_WITH;
        throw new SearchException(
            CommonErrorCodes.SEARCH_INVALID_OPERATOR,
            "Invalid operator '" + operator + "'. Allowed: EQUALS, CONTAINS, STARTS_WITH"
        );
    }

    // =============================================
    // Shared nested DTOs matching frontend contract
    // =============================================

    /**
     * Filter DTO matching frontend contract: { field, operator, value }
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContractFilter {
        private String field;
        private String operator;
        private Object value;
    }

    /**
     * Sort DTO matching frontend contract: { field, direction }
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContractSort {
        private String field;
        private String direction = "ASC";
    }
}
