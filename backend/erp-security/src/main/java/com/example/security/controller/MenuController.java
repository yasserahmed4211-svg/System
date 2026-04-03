package com.example.security.controller;

import com.example.erp.common.web.ApiResponse;
import com.example.erp.common.web.OperationCode;
import com.example.security.constants.SecurityPermissions;
import com.example.security.dto.MenuItemDto;
import com.example.security.service.MenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for Menu Management - User Menu Retrieval
 * 
 * ⚠️ NOTE: After SEC_MENU_ITEM removal, this controller only handles user menu retrieval.
 * Admin menu management is now done via Page Management (PageController).
 */
@RestController
@RequestMapping("/api/menu")
@RequiredArgsConstructor
@Tag(name = "Menu Management", description = "APIs for user menu retrieval based on permissions")
public class MenuController {

    private final MenuService menuService;
    private final OperationCode operationCode;

    /**
     * 🎯 PRIMARY API: Get menu for current logged-in user
     * 
     * Returns hierarchical menu structure based on VIEW permissions from SEC_PAGES.
     * Called immediately after login to build Angular menu.
     * 
     * @return List of root menu items with nested children
     */
    @GetMapping("/user-menu")
    @Operation(
        summary = "Get user menu based on permissions", 
        description = "Returns menu tree structure for current logged-in user. Shows only pages where user has VIEW permission."
    )
    public ResponseEntity<ApiResponse<java.util.List<MenuItemDto>>> getUserMenu() {
        return operationCode.craftResponse(menuService.getUserMenu());
    }

    /**
     * Get menu for specific user (Admin use)
     * 
     * @param userId User ID to retrieve menu for
     * @return List of root menu items for that user
     */
    @GetMapping("/user-menu/{userId}")
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).USER_VIEW)")
    @Operation(
        summary = "Get menu for specific user (Admin)", 
        description = "Admin: View menu structure for any user. Useful for debugging permission issues."
    )
    public ResponseEntity<ApiResponse<java.util.List<MenuItemDto>>> getUserMenuById(@PathVariable Long userId) {
        return operationCode.craftResponse(menuService.getUserMenu(userId));
    }
}
