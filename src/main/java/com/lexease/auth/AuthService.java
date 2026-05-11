package com.lexease.auth;

import com.lexease.shared.api.ApiException;
import com.lexease.shared.audit.AuditService;
import com.lexease.users.UserAccount;
import com.lexease.users.UserRepository;
import com.lexease.users.UserResponse;
import com.lexease.users.UserRole;
import com.lexease.users.UserStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuditService auditService;
    private final Clock clock;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            AuditService auditService,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (request.role() == UserRole.ADMIN) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ADMIN_REGISTER_DISABLED", "Admin cannot register publicly");
        }

        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_ALREADY_REGISTERED", "Email already registered");
        }

        Instant now = Instant.now(clock);
        UserAccount user = new UserAccount(
                UUID.randomUUID(),
                email,
                passwordEncoder.encode(request.password()),
                request.displayName().trim(),
                request.role(),
                UserStatus.ACTIVE,
                now,
                now);
        user = userRepository.save(user);
        auditService.log(user.getId(), "USER_REGISTERED", "USER", user.getId());
        return authResponse(user, refreshTokenService.create(user, null).rawToken());
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        UserAccount user = userRepository.findByEmail(normalizeEmail(request.email()))
                .orElseThrow(() -> invalidCredentials());
        if (user.getStatus() != UserStatus.ACTIVE || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw invalidCredentials();
        }
        auditService.log(user.getId(), "USER_LOGIN", "USER", user.getId());
        return authResponse(user, refreshTokenService.create(user, request.deviceId()).rawToken());
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        RefreshToken oldToken = refreshTokenService.consumeForRotation(request.refreshToken());
        UserAccount user = oldToken.getUser();
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "USER_DISABLED", "User is disabled");
        }
        String newRefreshToken = refreshTokenService.create(user, oldToken.getDeviceId()).rawToken();
        auditService.log(user.getId(), "REFRESH_TOKEN_ROTATED", "USER", user.getId());
        return authResponse(user, newRefreshToken);
    }

    @Transactional
    public void logout(LogoutRequest request) {
        refreshTokenService.revoke(request.refreshToken());
    }

    private AuthResponse authResponse(UserAccount user, String refreshToken) {
        return new AuthResponse(
                jwtService.createAccessToken(user),
                refreshToken,
                jwtService.accessTokenTtlSeconds(),
                UserResponse.from(user));
    }

    private ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password");
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
