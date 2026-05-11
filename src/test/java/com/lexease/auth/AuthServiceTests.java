package com.lexease.auth;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lexease.shared.api.ApiException;
import com.lexease.shared.audit.AuditService;
import com.lexease.users.UserAccount;
import com.lexease.users.UserRepository;
import com.lexease.users.UserRole;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTests {
    @Mock
    private UserRepository userRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private AuditService auditService;

    @Test
    void registerRejectsPublicAdmin() {
        AuthService authService = new AuthService(
                userRepository,
                new BCryptPasswordEncoder(),
                jwtService,
                refreshTokenService,
                auditService,
                Clock.fixed(Instant.parse("2026-05-25T00:00:00Z"), ZoneOffset.UTC));

        assertThatThrownBy(() -> authService.register(new RegisterRequest(
                "admin@example.com",
                "password123",
                "Admin",
                UserRole.ADMIN)))
                .isInstanceOf(ApiException.class)
                .hasMessage("Admin cannot register publicly");
        verify(userRepository, never()).save(any());
    }

    @Test
    void loginRejectsUnknownEmail() {
        AuthService authService = new AuthService(
                userRepository,
                new BCryptPasswordEncoder(),
                jwtService,
                refreshTokenService,
                auditService,
                Clock.fixed(Instant.parse("2026-05-25T00:00:00Z"), ZoneOffset.UTC));
        when(userRepository.findByEmail("child@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest(
                "child@example.com",
                "password123",
                null)))
                .isInstanceOf(ApiException.class)
                .hasMessage("Invalid email or password");
    }
}
