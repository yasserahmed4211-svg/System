package com.example.security.controller;

import com.example.security.dto.AuthRequest;
import com.example.security.dto.AuthResponse;
import com.example.security.dto.UserInfo;
import com.example.security.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "APIs for user authentication and token management")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(
        summary = "User login",
        description = "Authenticates user and returns JWT access token. Use this token in the Authorize button above."
    )
    public AuthResponse login(@RequestBody @Valid AuthRequest req,
                              HttpServletRequest request,
                              HttpServletResponse response){
        var t = authService.login(req.username(), req.password(), request, response);
        return new AuthResponse(t.access(), 900);
    }

    @PostMapping("/login-token")
    @Operation(
        summary = "User login with complete user information",
        description = "Authenticates user and returns access token, refresh token, and complete user information including roles and permissions"
    )
    public UserInfo loginWithToken(@RequestBody @Valid AuthRequest req,
                                    HttpServletRequest request,
                                    HttpServletResponse response){
        return authService.loginWithUserInfo(req.username(), req.password(), request, response);
    }

    @PostMapping("/refresh")
    @Operation(
        summary = "Refresh access token",
        description = "Refreshes the access token using the refresh token cookie"
    )
    public AuthResponse refresh(HttpServletRequest request, HttpServletResponse response){
        var t = authService.refresh(request, response);
        return new AuthResponse(t.access(), 900);
    }

    @PostMapping("/logout")
    @Operation(
        summary = "User logout",
        description = "Logs out the user and invalidates all tokens"
    )
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response){
        authService.logout(request, response);
        return ResponseEntity.noContent().build();
    }
}
