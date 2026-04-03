package com.example.erp.common.web;

import com.example.erp.common.domain.status.ServiceResult;
import com.example.erp.common.domain.status.Status;
import com.example.erp.common.domain.status.StatusCategory;
import com.example.erp.common.domain.status.StatusCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

/**
 * Implementation of OperationCode that maps business status codes to HTTP status codes.
 * 
 * Architecture Rules:
 * - This is the ONLY place where HTTP status mapping exists
 * - Service layer must NOT import or depend on this class
 * - All HTTP-specific logic is isolated in the web layer
 *
 * Mapping Strategy:
 * 1. First check for exact Status match in statusMappings
 * 2. Fall back to category-based mapping using categoryMappings
 * 3. Default to INTERNAL_SERVER_ERROR if no mapping found
 *
 * @author ERP Team
 */
@Component
public class OperationCodeImpl implements OperationCode {

    /**
     * Exact status to HTTP status mappings
     */
    private static final Map<Status, HttpStatus> statusMappings = new EnumMap<>(Status.class);

    /**
     * Category to HTTP status mappings (fallback)
     */
    private static final Map<StatusCategory, HttpStatus> categoryMappings = new EnumMap<>(StatusCategory.class);

    static {
        // ==================== Category Mappings (Fallback) ====================
        categoryMappings.put(StatusCategory.SUCCESS, HttpStatus.OK);
        categoryMappings.put(StatusCategory.CLIENT_ERROR, HttpStatus.BAD_REQUEST);
        categoryMappings.put(StatusCategory.BUSINESS_ERROR, HttpStatus.UNPROCESSABLE_ENTITY);
        categoryMappings.put(StatusCategory.NOT_FOUND, HttpStatus.NOT_FOUND);
        categoryMappings.put(StatusCategory.AUTH_ERROR, HttpStatus.UNAUTHORIZED);
        categoryMappings.put(StatusCategory.SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        categoryMappings.put(StatusCategory.CONFLICT, HttpStatus.CONFLICT);

        // ==================== Exact Status Mappings ====================
        
        // Success
        statusMappings.put(Status.SUCCESS, HttpStatus.OK);
        statusMappings.put(Status.OK, HttpStatus.OK);
        statusMappings.put(Status.CREATED, HttpStatus.CREATED);
        statusMappings.put(Status.UPDATED, HttpStatus.OK);
        statusMappings.put(Status.DELETED, HttpStatus.OK);

        // Client Errors
        statusMappings.put(Status.VALIDATION_ERROR, HttpStatus.BAD_REQUEST);
        statusMappings.put(Status.INVALID_INPUT, HttpStatus.BAD_REQUEST);
        statusMappings.put(Status.MISSING_REQUIRED_FIELD, HttpStatus.BAD_REQUEST);
        statusMappings.put(Status.INVALID_FORMAT, HttpStatus.BAD_REQUEST);
        statusMappings.put(Status.INVALID_JSON, HttpStatus.BAD_REQUEST);
        statusMappings.put(Status.BAD_REQUEST, HttpStatus.BAD_REQUEST);
        statusMappings.put(Status.TYPE_MISMATCH, HttpStatus.BAD_REQUEST);
        statusMappings.put(Status.MISSING_PARAMETER, HttpStatus.BAD_REQUEST);
        statusMappings.put(Status.BINDING_ERROR, HttpStatus.BAD_REQUEST);
        statusMappings.put(Status.METHOD_NOT_ALLOWED, HttpStatus.METHOD_NOT_ALLOWED);
        statusMappings.put(Status.UNSUPPORTED_MEDIA_TYPE, HttpStatus.UNSUPPORTED_MEDIA_TYPE);

        // Not Found
        statusMappings.put(Status.NOT_FOUND, HttpStatus.NOT_FOUND);
        statusMappings.put(Status.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND);
        statusMappings.put(Status.ENDPOINT_NOT_FOUND, HttpStatus.NOT_FOUND);
        statusMappings.put(Status.USER_NOT_FOUND, HttpStatus.NOT_FOUND);

        // Business Errors
        statusMappings.put(Status.BUSINESS_RULE_VIOLATION, HttpStatus.UNPROCESSABLE_ENTITY);
        statusMappings.put(Status.OPERATION_NOT_ALLOWED, HttpStatus.UNPROCESSABLE_ENTITY);
        statusMappings.put(Status.INVALID_STATE, HttpStatus.UNPROCESSABLE_ENTITY);
        statusMappings.put(Status.PRECONDITION_FAILED, HttpStatus.PRECONDITION_FAILED);

        // Conflict
        statusMappings.put(Status.DUPLICATE, HttpStatus.CONFLICT);
        statusMappings.put(Status.ALREADY_EXISTS, HttpStatus.CONFLICT);
        statusMappings.put(Status.DB_CONSTRAINT_VIOLATION, HttpStatus.CONFLICT);
        statusMappings.put(Status.CONCURRENT_MODIFICATION, HttpStatus.CONFLICT);

        // Auth Errors
        statusMappings.put(Status.UNAUTHORIZED, HttpStatus.UNAUTHORIZED);
        statusMappings.put(Status.INVALID_CREDENTIALS, HttpStatus.UNAUTHORIZED);
        statusMappings.put(Status.TOKEN_EXPIRED, HttpStatus.UNAUTHORIZED);
        statusMappings.put(Status.FORBIDDEN, HttpStatus.FORBIDDEN);
        statusMappings.put(Status.ACCESS_DENIED, HttpStatus.FORBIDDEN);

        // Server Errors
        statusMappings.put(Status.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        statusMappings.put(Status.DB_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        statusMappings.put(Status.SERVICE_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE);
        statusMappings.put(Status.TIMEOUT, HttpStatus.GATEWAY_TIMEOUT);
    }

    @Override
    public HttpStatus toHttpStatus(StatusCode statusCode) {
        return toHttpStatus(statusCode, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public HttpStatus toHttpStatus(StatusCode statusCode, HttpStatus defaultStatus) {
        if (statusCode == null) {
            return defaultStatus;
        }

        // First try exact match if it's a Status enum
        if (statusCode instanceof Status) {
            HttpStatus exact = statusMappings.get((Status) statusCode);
            if (exact != null) {
                return exact;
            }
        }

        // Fall back to category-based mapping
        HttpStatus categoryMapping = categoryMappings.get(statusCode.getCategory());
        return categoryMapping != null ? categoryMapping : defaultStatus;
    }

    @Override
    public boolean isSuccessHttpStatus(StatusCode statusCode) {
        HttpStatus httpStatus = toHttpStatus(statusCode);
        return httpStatus.is2xxSuccessful();
    }

    /**
     * Get the HTTP status for a status code string (convenience method).
     * Useful when working with error codes from exceptions.
     *
     * @param code The status code string (e.g., "NOT_FOUND")
     * @return The HTTP status, or INTERNAL_SERVER_ERROR if not found
     */
    public HttpStatus toHttpStatusFromCode(String code) {
        Status status = Status.fromCode(code);
        return status != null ? toHttpStatus(status) : HttpStatus.INTERNAL_SERVER_ERROR;
    }

    /**
     * Get the HTTP status for a status code string with default fallback.
     *
     * @param code The status code string
     * @param defaultStatus The default HTTP status if not found
     * @return The HTTP status
     */
    public HttpStatus toHttpStatusFromCode(String code, HttpStatus defaultStatus) {
        Status status = Status.fromCode(code);
        return status != null ? toHttpStatus(status) : defaultStatus;
    }

    /**
     * Translates a ServiceResult to an HTTP ResponseEntity with ApiResponse envelope.
     * 
     * Architecture Rule: This is the ONLY translation point from service layer to HTTP.
     * Controllers MUST use this method instead of manually building responses.
     *
     * @param result The service layer result
     * @param <T> The type of data payload
     * @return ResponseEntity with appropriate HTTP status and ApiResponse body
     */
    @Override
    public <T> ResponseEntity<ApiResponse<T>> craftResponse(ServiceResult<T> result) {
        if (result == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail("Internal error", new ApiError("INTERNAL_ERROR")));
        }

        HttpStatus httpStatus = toHttpStatus(result.getStatus());
        
        if (result.isSuccess()) {
            // Success response
            String message = result.getMessage() != null ? result.getMessage() : "OK";
            ApiResponse<T> response = ApiResponse.ok(result.getData(), message);
            return ResponseEntity.status(httpStatus).body(response);
        } else {
            // Error response
            String message = result.getMessage() != null ? result.getMessage() : "Error";
            ApiError error = new ApiError(result.getCode(), result.getDetails());
            ApiResponse<T> response = ApiResponse.fail(message, error);
            return ResponseEntity.status(httpStatus).body(response);
        }
    }
}
