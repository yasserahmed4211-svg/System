package com.example.erp.common.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Simple message-only response DTO for operations that don't return data.
 * 
 * Used for:
 * - Delete confirmations
 * - Status updates
 * - Simple acknowledgements
 *
 * For full response with data, use ApiResponse instead.
 *
 * @author ERP Team
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageResponse {

    /**
     * Localized message to display to user
     */
    private String message;

    /**
     * Machine-readable code (optional, for client-side handling)
     */
    private String code;

    /**
     * Create a message-only response
     */
    public static MessageResponse of(String message) {
        return new MessageResponse(message, null);
    }

    /**
     * Create a message response with code
     */
    public static MessageResponse of(String message, String code) {
        return new MessageResponse(message, code);
    }

    /**
     * Create a success message
     */
    public static MessageResponse success(String message) {
        return new MessageResponse(message, "SUCCESS");
    }

    /**
     * Create a deleted confirmation message
     */
    public static MessageResponse deleted() {
        return new MessageResponse("Deleted successfully", "DELETED");
    }

    /**
     * Create a deleted confirmation with custom message
     */
    public static MessageResponse deleted(String message) {
        return new MessageResponse(message, "DELETED");
    }
}
