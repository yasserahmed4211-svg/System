package com.example.security.controller;

import com.example.erp.common.web.ApiResponse;
import com.example.erp.common.web.OperationCode;
import com.example.security.dto.UserSearchContractRequest;
import com.example.security.constants.SecurityPermissions;
import com.example.security.domain.Role;
import com.example.security.dto.AssignRolesRequest;
import com.example.security.dto.CreateUserRequest;
import com.example.security.dto.UpdateUserRequest;
import com.example.security.dto.UserDto;
import com.example.security.mapper.UserMapper;
import com.example.security.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * User Controller - Authorization handled at service layer (Rule 19.1)
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "APIs for managing users, roles, and permissions")
public class UserController {

    private final UserService userService;
    private final OperationCode operationCode;

    @PostMapping
    @Operation(summary = "Create new user", description = "Creates a new user account with specified roles")
    public ResponseEntity<ApiResponse<UserDto>> create(@RequestBody @Valid CreateUserRequest req){
        return operationCode.craftResponse(userService.createUser(req));
    }

    @GetMapping
    @Operation(
        summary = "List all users",
        description = "Get paginated list of users. Sort format: 'fieldName,direction' (e.g., 'username,asc' or 'createdAt,desc')"
    )
    public ResponseEntity<ApiResponse<Page<UserDto>>> all(
            @Parameter(description = "Pagination parameters. Use sort format: 'fieldName,asc' or 'fieldName,desc'. Allowed fields: id, username, enabled, createdAt, updatedAt",
                       schema = @Schema(implementation = Pageable.class))
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        // Enforce pagination limits using centralized utility (Rule 21 & 22)
        pageable = com.example.erp.common.web.util.PageableUtils.enforceConstraints(pageable);
        return operationCode.craftResponse(userService.listUsers(pageable));
    }

    @PostMapping("/search")
    @Operation(
        summary = "Dynamic search for users",
        description = """
            Advanced search with dynamic filtering, sorting, and pagination.
            
            **Allowed Search Fields**: id, username, enabled, createdAt, tenantId
            **Allowed Sort Fields**: id, username, enabled, createdAt
            
            **Supported Operators**:
            - EQ: Equals (=)
            - NE: Not Equals (!=)
            - GT: Greater Than (>)
            - GE: Greater or Equal (>=)
            - LT: Less Than (<)
            - LE: Less or Equal (<=)
            - LIKE: Contains (case-insensitive)
            - IN: In list
            - IS_NULL: Is null
            - IS_NOT_NULL: Is not null
            - BETWEEN: Between two values
            
            **Example Request Body**:
            ```json
            {
              "filters": [
                {"field": "username", "op": "LIKE", "value": "admin"},
                {"field": "enabled", "op": "EQ", "value": true}
              ],
              "page": 0,
              "size": 20,
              "sortBy": "createdAt",
              "sortDir": "DESC"
            }
            ```
            """
    )
    public ResponseEntity<ApiResponse<Page<UserDto>>> search(@RequestBody @Valid UserSearchContractRequest searchRequest) {
        return operationCode.craftResponse(userService.searchUsers(searchRequest.toCommonSearchRequest()));
    }

    // ربط مستخدم بأدوار (استبدال كامل) - Authorization at service layer
    @PutMapping("/{userId}/roles")
    @Operation(
        summary = "Assign roles to user",
        description = "Assigns roles to a user (full replace - removes existing roles)"
    )
    public ResponseEntity<ApiResponse<UserDto>> assignRoles(@PathVariable Long userId, @RequestBody @Valid AssignRolesRequest req) {
        return operationCode.craftResponse(userService.assignRoles(userId, req.getRoleNames()));
    }

    // عرض أدوار مستخدم معين - Authorization at service layer
    @GetMapping("/{userId}/roles")
    @Operation(
        summary = "Get user roles",
        description = "Returns list of role names assigned to the user"
    )
    public ResponseEntity<ApiResponse<List<String>>> getUserRoles(@PathVariable Long userId) {
        return operationCode.craftResponse(userService.getUserRoleNames(userId));
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
        summary = "Delete user", 
        description = "Deletes a user if they have no child relationships (e.g., active refresh tokens)"
    )
    public void delete(@PathVariable Long userId) {
        userService.deleteUser(userId);
    }

    @PutMapping("/{userId}")
    @Operation(
        summary = "Update user",
        description = "Updates user information (username, password, enabled status). All fields are optional."
    )
    public ResponseEntity<ApiResponse<UserDto>> update(@PathVariable Long userId, @RequestBody @Valid UpdateUserRequest req) {
        return operationCode.craftResponse(userService.updateUser(userId, req));
    }
}
