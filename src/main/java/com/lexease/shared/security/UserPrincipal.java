package com.lexease.shared.security;

import com.lexease.users.UserAccount;
import com.lexease.users.UserRole;
import com.lexease.users.UserStatus;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class UserPrincipal implements UserDetails {
    private static final String ROLE_AUTHORITY_PREFIX = "ROLE_";
    private final UUID id;
    private final String email;
    private final UserRole role;
    private final UserStatus status;
    private final List<GrantedAuthority> authorities;

    public UserPrincipal(UserAccount user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.role = user.getRole();
        this.status = user.getStatus();
        this.authorities = List.of(new SimpleGrantedAuthority(ROLE_AUTHORITY_PREFIX + user.getRole().name()));
    }

    public UUID id() {
        return id;
    }

    public UserRole role() {
        return role;
    }

    public UserStatus status() {
        return status;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return status == UserStatus.ACTIVE;
    }
}
