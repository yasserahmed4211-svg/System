package com.example.security.service;

import com.example.security.domain.UserAccount;
import com.example.security.repo.UserAccountRepository;
import com.example.erp.common.util.SecurityContextHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserAccountRepository userRepo;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String tenant = SecurityContextHelper.requireTenantId();

        UserAccount ua = userRepo.findByUsernameWithRoles(username, tenant)
                .orElseThrow(() -> new UsernameNotFoundException("USER_NOT_FOUND"));

        return buildUserDetails(ua);
    }
    
    /**
     * Load user by ID - used for JWT token authentication
     * More efficient than username lookup
     */
    @Transactional(readOnly = true)
    public UserDetails loadUserById(Long userId) throws UsernameNotFoundException {
        String tenant = SecurityContextHelper.requireTenantId();

        UserAccount ua = userRepo.findByIdWithRoles(userId, tenant)
                .orElseThrow(() -> new UsernameNotFoundException("USER_NOT_FOUND"));

        return buildUserDetails(ua);
    }

    private UserDetails buildUserDetails(UserAccount ua) {
        Set<String> authorities = new HashSet<>();
        authorities.addAll(ua.getRoles().stream().map(r -> r.getName()).collect(Collectors.toSet()));
        authorities.addAll(ua.getRoles().stream().flatMap(r -> r.getPermissions().stream()).map(p -> p.getName()).collect(Collectors.toSet()));

        return new org.springframework.security.core.userdetails.User(
                ua.getUsername(), ua.getPassword(),
                ua.isEnabled(), true, true, true,
                authorities.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toSet())
        );
    }
}
