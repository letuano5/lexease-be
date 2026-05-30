package com.lexease.notifications;

import com.lexease.notifications.dtos.req.RegisterDeviceTokenRequest;
import com.lexease.notifications.dtos.res.DeviceTokenResponse;
import com.lexease.shared.api.ApiException;
import com.lexease.shared.api.ErrorCode;
import com.lexease.users.UserAccount;
import com.lexease.users.UserRepository;
import com.lexease.users.UserRole;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeviceTokenService {
    private final DeviceTokenRepository deviceTokenRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public DeviceTokenService(DeviceTokenRepository deviceTokenRepository, UserRepository userRepository, Clock clock) {
        this.deviceTokenRepository = deviceTokenRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Transactional
    public DeviceTokenResponse register(UUID currentUserId, RegisterDeviceTokenRequest request) {
        UserAccount user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCode.USER_NOT_FOUND, "User not found"));
        Instant now = Instant.now(clock);
        DeviceToken token = deviceTokenRepository.findByToken(request.deviceToken())
                .orElseGet(() -> new DeviceToken(
                        UUID.randomUUID(),
                        user,
                        request.platform(),
                        blankToNull(request.deviceId()),
                        request.deviceToken(),
                        now));
        token.refresh(user, request.platform(), blankToNull(request.deviceId()), now);
        return toResponse(deviceTokenRepository.save(token));
    }

    @Transactional
    public void deactivate(UUID currentUserId, UserRole currentRole, UUID tokenId) {
        DeviceToken token = deviceTokenRepository.findById(tokenId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCode.DEVICE_TOKEN_NOT_FOUND, "Device token not found"));
        if (currentRole != UserRole.ADMIN && !token.getUser().getId().equals(currentUserId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN, "Cannot deactivate another user's device token");
        }
        token.deactivate();
    }

    @Transactional
    public void deactivateInvalidTokens(Iterable<UUID> tokenIds) {
        for (UUID tokenId : tokenIds) {
            deviceTokenRepository.findById(tokenId).ifPresent(DeviceToken::deactivate);
        }
    }

    private DeviceTokenResponse toResponse(DeviceToken token) {
        return new DeviceTokenResponse(
                token.getId(),
                token.getUser().getId(),
                token.getPlatform(),
                token.getDeviceId(),
                token.isActive(),
                token.getLastSeenAt(),
                token.getCreatedAt());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
