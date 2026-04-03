package com.example.security.mapper;

import com.example.security.domain.Permission;
import com.example.security.domain.Role;
import com.example.security.domain.UserAccount;
import com.example.security.dto.UserDto;

import java.util.Set;
import java.util.stream.Collectors;

public final class UserMapper {
    private UserMapper(){}

    public static UserDto toDto(UserAccount u){
        Set<String> roles = u.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
        Set<String> perms = u.getRoles().stream()
                .flatMap(r -> r.getPermissions().stream())
                .map(Permission::getName)
                .collect(Collectors.toSet());
        return new UserDto(u.getId(), u.getUsername(), u.getTenantId(), u.isEnabled(), roles, perms);
    }
}
