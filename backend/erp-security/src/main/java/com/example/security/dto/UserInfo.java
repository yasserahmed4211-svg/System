package com.example.security.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;

/**
 * User information DTO returned after successful authentication.
 * Contains user details along with authentication tokens.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Complete user information with authentication tokens")
public record UserInfo(
    @Schema(description = "JWT access token for API authentication")
    String accessToken,
    
    @Schema(description = "Access token expiration time in seconds", example = "900")
    Long expiresIn,
    
    @Schema(description = "Refresh token for obtaining new access tokens")
    String refreshToken,
    
    @Schema(description = "Refresh token expiration time in seconds", example = "604800")
    Long refreshExpiresIn,
    
    @Schema(description = "User ID")
    Long userId,
    
    @Schema(description = "Username")
    String username,
    
    @Schema(description = "Tenant ID")
    String tenantId,
    
    @Schema(description = "Whether the user account is enabled")
    boolean enabled,
    
    @Schema(description = "User's assigned roles")
    Set<String> roles,
    
    @Schema(description = "User's permissions (flattened from all roles)")
    Set<String> permissions
) {
    /**
     * Constructor for creating UserInfo from UserDto and tokens.
     */
    public UserInfo(String accessToken, long expiresIn, String refreshToken, long refreshExpiresIn, UserDto user) {
        this(
            accessToken,
            expiresIn,
            refreshToken,
            refreshExpiresIn,
            user.id(),
            user.username(),
            user.tenantId(),
            user.enabled(),
            user.roles(),
            user.permissions()
        );
    }
}
