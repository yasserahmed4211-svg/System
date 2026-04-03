package com.example.erp.common.web;

import com.example.erp.common.domain.status.ServiceResult;
import com.example.erp.common.domain.status.StatusCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Interface for mapping business status codes to HTTP status codes.
 * 
 * Architecture Rules:
 * - This interface belongs to the WEB layer ONLY
 * - Service layer must NOT depend on this interface
 * - HTTP mapping logic is centralized here, not scattered across services
 *
 * Usage:
 * - Controllers and exception handlers use this to map StatusCode to HttpStatus
 * - Service layer only works with StatusCode (domain layer)
 *
 * @author ERP Team
 */
public interface OperationCode {

    /**
     * Maps a business StatusCode to the appropriate HTTP status code.
     *
     * @param statusCode The business status code from domain layer
     * @return The corresponding HTTP status code
     */
    HttpStatus toHttpStatus(StatusCode statusCode);

    /**
     * Maps a business StatusCode to HTTP status code, with a custom default.
     *
     * @param statusCode The business status code
     * @param defaultStatus The default HTTP status if mapping not found
     * @return The corresponding HTTP status code
     */
    HttpStatus toHttpStatus(StatusCode statusCode, HttpStatus defaultStatus);

    /**
     * Checks if the given status code should result in a successful HTTP response (2xx).
     *
     * @param statusCode The business status code
     * @return true if the status maps to a 2xx HTTP status
     */
    boolean isSuccessHttpStatus(StatusCode statusCode);

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
    <T> ResponseEntity<ApiResponse<T>> craftResponse(ServiceResult<T> result);
}
