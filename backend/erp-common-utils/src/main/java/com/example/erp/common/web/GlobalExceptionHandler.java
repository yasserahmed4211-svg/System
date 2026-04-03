package com.example.erp.common.web;

import com.example.erp.common.exception.BusinessException;
import com.example.erp.common.exception.LocalizedException;
import com.example.erp.common.exception.NotFoundException;
import com.example.erp.common.i18n.LocalizationService;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.io.EOFException;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler that standardizes all error responses
 * using the ApiResponse envelope pattern.
 *
 * All exceptions are caught and transformed into:
 * {
 *   "success": false,
 *   "message": "Human-readable error message",
 *   "data": null,
 *   "error": {
 *     "code": "ERROR_CODE",
 *     "details": "Additional context",
 *     "fieldErrors": [...],
 *     "timestamp": "2025-12-27T10:30:00Z",
 *     "path": "/api/users"
 *   }
 * }
 *
 * @author ERP Team
 */
@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

    private final LocalizationService localizationService;

    // ============== Validation Errors (HTTP 400) ==============

    /**
     * Handle Bean Validation errors from @Valid on @RequestBody
     * Returns field-level validation errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        
        logWarn("Validation error at {}: {}", request.getRequestURI(), ex.getMessage());

        List<FieldErrorItem> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
            .map(error -> new FieldErrorItem(error.getField(), resolveValidationMessage(error.getDefaultMessage())))
                .collect(Collectors.toList());

        ApiError error = createError(
            "VALIDATION_ERROR",
            "Request validation failed. Please check the field errors.",
            fieldErrors,
            request.getRequestURI()
        );

        ApiResponse<Void> response = ApiResponse.fail("Validation failed", error);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle constraint violations from @Validated at method/class level
     * Returns field-level validation errors
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request) {
        
        logWarn("Constraint violation at {}: {}", request.getRequestURI(), ex.getMessage());

        List<FieldErrorItem> fieldErrors = ex.getConstraintViolations().stream()
                .map(violation -> new FieldErrorItem(
                        extractPropertyName(violation),
                resolveValidationMessage(violation.getMessage())
                ))
                .collect(Collectors.toList());

        ApiError error = createError(
            "VALIDATION_ERROR",
            "Constraint validation failed. Please check the field errors.",
            fieldErrors,
            request.getRequestURI()
        );

        ApiResponse<Void> response = ApiResponse.fail("Validation failed", error);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle BindException - binding errors during data binding
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Void>> handleBindException(
            BindException ex,
            HttpServletRequest request) {
        
        logWarn("Binding error at {}: {}", request.getRequestURI(), ex.getMessage());

        List<FieldErrorItem> fieldErrors = ex.getFieldErrors().stream()
            .map(error -> new FieldErrorItem(error.getField(), resolveValidationMessage(error.getDefaultMessage())))
                .collect(Collectors.toList());

        ApiError error = createError(
            "BINDING_ERROR",
            "Data binding failed. Please check request parameters.",
            fieldErrors,
            request.getRequestURI()
        );

        ApiResponse<Void> response = ApiResponse.fail("Binding error", error);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle HttpMessageNotReadableException - malformed JSON, invalid date formats, etc.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {
        
        logWarn("Message not readable at {}: {}", request.getRequestURI(), ex.getMessage());

        String details = "Invalid request body. Please check JSON format.";
        
        // Extract more specific error for common cases
        Throwable cause = ex.getCause();
        if (cause instanceof InvalidFormatException) {
            InvalidFormatException ife = (InvalidFormatException) cause;
            String fieldName = ife.getPath().stream()
                    .map(JsonMappingException.Reference::getFieldName)
                    .filter(name -> name != null)
                    .collect(Collectors.joining("."));
            
            details = String.format("Invalid value for field '%s'. Expected type: %s",
                    fieldName.isEmpty() ? "unknown" : fieldName,
                    ife.getTargetType().getSimpleName());
        } else if (ex.getMessage() != null && ex.getMessage().contains("JSON parse error")) {
            details = "Malformed JSON. Please check the request body syntax.";
        }

        ApiError error = createError("INVALID_JSON", details, request.getRequestURI());
        ApiResponse<Void> response = ApiResponse.fail("Invalid request body", error);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle MissingServletRequestParameterException - required @RequestParam missing
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParameter(
            MissingServletRequestParameterException ex,
            HttpServletRequest request) {
        
        logWarn("Missing parameter at {}: {}", request.getRequestURI(), ex.getParameterName());

        String details = String.format("Required parameter '%s' of type '%s' is missing",
                ex.getParameterName(), ex.getParameterType());

        ApiError error = createError("MISSING_PARAMETER", details, request.getRequestURI());
        ApiResponse<Void> response = ApiResponse.fail("Required parameter missing", error);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle MethodArgumentTypeMismatchException - wrong type for @RequestParam or @PathVariable
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {
        
        logWarn("Type mismatch at {}: {}", request.getRequestURI(), ex.getMessage());

        String requiredType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
        String details = String.format("Parameter '%s' has invalid type. Expected: %s, but got: %s",
                ex.getName(), requiredType, ex.getValue());

        ApiError error = createError("TYPE_MISMATCH", details, request.getRequestURI());
        ApiResponse<Void> response = ApiResponse.fail("Invalid parameter type", error);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle illegal arguments and bad request errors
     */
    @ExceptionHandler({
            IllegalArgumentException.class,
            IllegalStateException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(
            Exception ex,
            HttpServletRequest request) {
        
        logWarn("Illegal argument at {}: {}", request.getRequestURI(), ex.getMessage());

        ApiError error = createError("BAD_REQUEST", ex.getMessage(), request.getRequestURI());
        ApiResponse<Void> response = ApiResponse.fail("Bad request", error);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // ============== Not Found (HTTP 404) ==============

    /**
     * Handle resource not found
     */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(
            NotFoundException ex,
            HttpServletRequest request) {
        
        logWarn("Resource not found at {}: {}", request.getRequestURI(), ex.getMessage());

        ApiError error = createError("NOT_FOUND", ex.getMessage(), request.getRequestURI());
        ApiResponse<Void> response = ApiResponse.fail(ex.getMessage(), error);

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Handle no handler found for the request (requires spring.mvc.throw-exception-if-no-handler-found=true)
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoHandlerFound(
            NoHandlerFoundException ex,
            HttpServletRequest request) {
        
        logWarn("No handler found: {} {}", ex.getHttpMethod(), ex.getRequestURL());

        String details = String.format("No endpoint found for %s %s", ex.getHttpMethod(), ex.getRequestURL());
        ApiError error = createError("ENDPOINT_NOT_FOUND", details, request.getRequestURI());
        ApiResponse<Void> response = ApiResponse.fail("Endpoint not found", error);

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Handle username not found (Spring Security)
     */
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleUsernameNotFound(
            UsernameNotFoundException ex,
            HttpServletRequest request) {
        
        logWarn("User not found at {}: {}", request.getRequestURI(), ex.getMessage());

        ApiError error = createError("USER_NOT_FOUND", "User not found", request.getRequestURI());
        ApiResponse<Void> response = ApiResponse.fail("User not found", error);

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    // ============== Authentication & Authorization (HTTP 401, 403) ==============

    /**
     * Handle authentication errors (not authenticated)
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthentication(
            AuthenticationException ex,
            HttpServletRequest request) {
        
        logWarn("Authentication failed at {}: {}", request.getRequestURI(), ex.getMessage());

        ApiError error = createError("UNAUTHORIZED", "Authentication required", request.getRequestURI());
        ApiResponse<Void> response = ApiResponse.fail("Authentication failed", error);

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * Handle bad credentials (wrong username/password)
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(
            BadCredentialsException ex,
            HttpServletRequest request) {
        
        logWarn("Bad credentials at {}", request.getRequestURI());

        ApiError error = createError("INVALID_CREDENTIALS", "Invalid username or password", request.getRequestURI());
        ApiResponse<Void> response = ApiResponse.fail("Invalid credentials", error);

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * Handle access denied (authenticated but not authorized)
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request) {
        
        logWarn("Access denied at {}: {}", request.getRequestURI(), ex.getMessage());

        ApiError error = createError(
            "FORBIDDEN",
            "You don't have permission to access this resource",
            request.getRequestURI()
        );
        ApiResponse<Void> response = ApiResponse.fail("Access denied", error);

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    // ============== Method Not Allowed & Unsupported Media Type (HTTP 405, 415) ==============

    /**
     * Handle HTTP method not supported (e.g., POST to GET-only endpoint)
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request) {
        
        logWarn("Method not supported at {}: {}", request.getRequestURI(), ex.getMethod());

        String supportedMethods = ex.getSupportedHttpMethods() != null
                ? String.join(", ", ex.getSupportedHttpMethods().stream().map(m -> m.name()).toList())
                : "N/A";
        
        String details = String.format("Method '%s' is not supported for this endpoint. Supported methods: %s",
                ex.getMethod(), supportedMethods);

        ApiError error = createError("METHOD_NOT_ALLOWED", details, request.getRequestURI());
        ApiResponse<Void> response = ApiResponse.fail("HTTP method not allowed", error);

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
    }

    /**
     * Handle unsupported media type (e.g., sending XML when only JSON is supported)
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex,
            HttpServletRequest request) {
        
        logWarn("Media type not supported at {}: {}", request.getRequestURI(), ex.getContentType());

        String supportedTypes = ex.getSupportedMediaTypes().stream()
                .map(Object::toString)
                .collect(Collectors.joining(", "));
        
        String details = String.format("Media type '%s' is not supported. Supported types: %s",
                ex.getContentType(), supportedTypes);

        ApiError error = createError("UNSUPPORTED_MEDIA_TYPE", details, request.getRequestURI());
        ApiResponse<Void> response = ApiResponse.fail("Unsupported media type", error);

        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(response);
    }

    // ============== Business Logic Errors (HTTP 422) ==============

    /**
     * Handle business rule violations
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(
            BusinessException ex,
            HttpServletRequest request) {
        
        logWarn("Business exception at {}: {} - {}", request.getRequestURI(), ex.getCode(), ex.getMessage());

        ApiError error = createError(ex.getCode(), ex.getDetails(), request.getRequestURI());
        ApiResponse<Void> response = ApiResponse.fail(ex.getMessage(), error);

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
    }

    /**
     * Handle localized exceptions (with i18n support)
     */
    @ExceptionHandler(LocalizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleLocalized(
            LocalizedException ex,
            HttpServletRequest request) {
        
        logWarn("Localized exception at {}: {}", request.getRequestURI(), ex.getMessageKey());

        String localizedMessage = localizationService.getMessage(ex.getMessageKey(), ex.getArgs());

        ApiError error = createError(ex.getMessageKey(), localizedMessage, request.getRequestURI());
        ApiResponse<Void> response = ApiResponse.fail(localizedMessage, error);

        return ResponseEntity.status(ex.getStatus()).body(response);
    }

    // ============== Database Errors (HTTP 409, 500) ==============

    /**
     * Handle data integrity violations (unique constraints, foreign keys, not null)
     * Returns user-friendly messages without exposing DB constraint names
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {
        
        logError("Data integrity violation at {}", request.getRequestURI(), ex);

        String message = "Data integrity constraint violated";
        String details = extractFriendlyConstraintMessage(ex);

        ApiError error = createError("DB_CONSTRAINT_VIOLATION", details, request.getRequestURI());
        ApiResponse<Void> response = ApiResponse.fail(message, error);

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /**
     * Handle general database errors
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataAccess(
            DataAccessException ex,
            HttpServletRequest request) {
        
        logError("Database access error at {}", request.getRequestURI(), ex);

        // Expose root-cause message for diagnosis (DB constraint names are
        // already handled separately by handleDataIntegrity).
        String rootMsg = ex.getMostSpecificCause().getMessage();
        String details = (rootMsg != null && !rootMsg.isBlank())
                ? rootMsg
                : "Database operation failed";

        ApiError error = createError("DB_ERROR", details, request.getRequestURI());
        ApiResponse<Void> response = ApiResponse.fail("Database error", error);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    // ============== Generic Error Handler (HTTP 500) ==============

    /**
     * Client aborted the connection (browser navigated away, cancelled request, etc).
     * We intentionally do not try to write a JSON error response.
     */
    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public ResponseEntity<Void> handleAsyncRequestNotUsable(
            AsyncRequestNotUsableException ex,
            HttpServletRequest request) {

        if (isClientAbort(ex)) {
            log.debug("Client aborted connection at {}", request.getRequestURI());
            return ResponseEntity.noContent().build();
        }

        logWarn("Async request not usable at {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    /**
     * Writing the response failed (often due to a disconnected client).
     * When it is a client-abort case we suppress it to avoid secondary exceptions.
     */
    @ExceptionHandler(HttpMessageNotWritableException.class)
    public ResponseEntity<Void> handleMessageNotWritable(
            HttpMessageNotWritableException ex,
            HttpServletRequest request) {

        if (isClientAbort(ex)) {
            log.debug("Client aborted connection while writing response at {}", request.getRequestURI());
            return ResponseEntity.noContent().build();
        }

        logError("Failed to write response at {}", request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    /**
     * Catch-all handler for any unhandled exceptions
     * Logs full stack trace but returns generic message to client
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(
            Exception ex,
            HttpServletRequest request) {
        if (isClientAbort(ex)) {
            log.debug("Client aborted connection at {}", request.getRequestURI());
            return ResponseEntity.noContent().build();
        }

        logError("Unhandled exception at {}: {}", request.getRequestURI(), ex.getClass().getSimpleName(), ex);

        ApiError error = createError(
            "INTERNAL_ERROR",
            "An unexpected error occurred. Please contact support if this persists.",
            request.getRequestURI()
        );
        ApiResponse<Void> response = ApiResponse.fail("Internal server error", error);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    // ============== Helper Methods ==============

    /**
     * Create ApiError with timestamp and path
     */
    private ApiError createError(String code, String details, String path) {
        ApiError error = new ApiError(code, details);
        error.setTimestamp(Instant.now());
        error.setPath(path);
        return error;
    }

    /**
     * Create ApiError with fieldErrors, timestamp and path
     */
    private ApiError createError(String code, String details, List<FieldErrorItem> fieldErrors, String path) {
        ApiError error = new ApiError(code, details, fieldErrors);
        error.setTimestamp(Instant.now());
        error.setPath(path);
        return error;
    }

    private String resolveValidationMessage(String message) {
        if (message == null || message.isBlank()) {
            return localizationService.getMessage("validation.invalid");
        }

        if (message.startsWith("{") && message.endsWith("}")) {
            String key = message.substring(1, message.length() - 1);
            return localizationService.getMessage(key);
        }

        return message;
    }

    /**
     * Extract property name from constraint violation path
     */
    private String extractPropertyName(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath().toString();
        // Extract the last part (e.g., "user.email" -> "email")
        int lastDot = path.lastIndexOf('.');
        return lastDot >= 0 ? path.substring(lastDot + 1) : path;
    }

    /**
     * Extract user-friendly constraint details from DataIntegrityViolationException
     * Hides technical DB constraint names and returns friendly messages
     */
    private String extractFriendlyConstraintMessage(DataIntegrityViolationException ex) {
        String message = ex.getMostSpecificCause().getMessage();

        if (message == null) {
            return "A database constraint was violated. Please check your data.";
        }

        // Normalize message to lowercase for easier matching
        String lowerMessage = message.toLowerCase();

        // Unique constraint violation
        if (lowerMessage.contains("unique constraint") || 
            lowerMessage.contains("unique index") ||
            lowerMessage.contains("duplicate") ||
            lowerMessage.contains("uk_")) {
            return "This record already exists. Duplicate values are not allowed.";
        }
        
        // Foreign key constraint violation
        if (lowerMessage.contains("foreign key") || 
            lowerMessage.contains("fk_") ||
            lowerMessage.contains("referenced")) {
            return "Cannot complete this operation. This record is referenced by other data.";
        }
        
        // Not null constraint violation
        if (lowerMessage.contains("not null") || 
            lowerMessage.contains("nn_") ||
            lowerMessage.contains("cannot be null")) {
            return "Required field is missing or null.";
        }
        
        // Check constraint violation
        if (lowerMessage.contains("check constraint") || 
            lowerMessage.contains("ck_")) {
            return "Data validation failed. Please check the values provided.";
        }

        // Default message for unknown constraint types
        return "A database constraint was violated. Please verify your data.";
    }

    /**
     * Log warning (for 4xx errors)
     */
    private void logWarn(String message, Object... args) {
        log.warn(message, args);
    }

    /**
     * Log error (for 5xx errors)
     */
    private void logError(String message, Object... args) {
        // Last arg should be the exception
        if (args.length > 0 && args[args.length - 1] instanceof Throwable) {
            Object[] messageArgs = new Object[args.length - 1];
            System.arraycopy(args, 0, messageArgs, 0, args.length - 1);
            log.error(message, messageArgs, (Throwable) args[args.length - 1]);
        } else {
            log.error(message, args);
        }
    }

    private boolean isClientAbort(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            String simpleName = current.getClass().getSimpleName();
            if ("ClientAbortException".equals(simpleName)) {
                return true;
            }

            if (current instanceof EOFException) {
                return true;
            }

            if (current instanceof IOException ioException) {
                String message = ioException.getMessage();
                if (message != null) {
                    String lower = message.toLowerCase();
                    if (lower.contains("broken pipe")
                            || lower.contains("connection reset")
                            || lower.contains("connection reset by peer")
                            || lower.contains("forcibly closed")
                            || lower.contains("an existing connection was forcibly closed")) {
                        return true;
                    }
                }
            }

            current = current.getCause();
        }
        return false;
    }
}

