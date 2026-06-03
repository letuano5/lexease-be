package com.lexease.guardians;

import com.lexease.guardians.dtos.req.CreateGuardianChildLinkRequest;
import com.lexease.guardians.dtos.res.GuardianChildLinkResponse;
import com.lexease.shared.api.ApiException;
import com.lexease.shared.api.ErrorCode;
import com.lexease.shared.audit.AuditAction;
import com.lexease.shared.audit.AuditService;
import com.lexease.shared.audit.AuditTargetType;
import com.lexease.shared.security.UserPrincipal;
import com.lexease.users.UserAccount;
import com.lexease.users.UserRepository;
import com.lexease.users.UserRole;
import com.lexease.users.UserStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GuardianChildLinkService {
    private final GuardianChildLinkRepository linkRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final Clock clock;

    public GuardianChildLinkService(
            GuardianChildLinkRepository linkRepository,
            UserRepository userRepository,
            AuditService auditService,
            Clock clock
    ) {
        this.linkRepository = linkRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional
    public GuardianChildLinkResponse create(UserPrincipal principal, CreateGuardianChildLinkRequest request) {
        requireRole(principal, UserRole.GUARDIAN);
        UserAccount guardian = loadUser(principal.id());
        UserAccount child = userRepository.findByEmail(request.childEmail().trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCode.CHILD_NOT_FOUND, "Child not found"));
        if (child.getRole() != UserRole.CHILD || child.getStatus() != UserStatus.ACTIVE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_CHILD, "Target user is not an active child");
        }
        if (guardian.getId().equals(child.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_LINK, "Guardian and child must be different users");
        }
        if (linkRepository.existsByGuardianIdAndChildIdAndStatusNot(
                guardian.getId(),
                child.getId(),
                GuardianChildLinkStatus.REVOKED)) {
            throw new ApiException(HttpStatus.CONFLICT, ErrorCode.LINK_ALREADY_EXISTS, "Guardian-child link already exists");
        }

        GuardianChildLink link = linkRepository.save(new GuardianChildLink(
                UUID.randomUUID(),
                guardian,
                child,
                GuardianChildLinkStatus.PENDING,
                guardian,
                Instant.now(clock)));
        auditService.log(guardian.getId(), AuditAction.GUARDIAN_CHILD_LINK_REQUESTED, AuditTargetType.GUARDIAN_CHILD_LINK, link.getId());
        return GuardianChildLinkResponse.from(link);
    }

    @Transactional(readOnly = true)
    public List<GuardianChildLinkResponse> list(UserPrincipal principal) {
        List<GuardianChildLink> links = switch (principal.role()) {
            case GUARDIAN -> linkRepository.findByGuardianIdAndStatusOrderByCreatedAtDesc(
                    principal.id(),
                    GuardianChildLinkStatus.ACCEPTED);
            case CHILD -> linkRepository.findByChildIdOrderByCreatedAtDesc(principal.id());
            case ADMIN -> linkRepository.findByGuardianIdOrChildId(principal.id(), principal.id());
        };
        return links.stream()
                .map(GuardianChildLinkResponse::from)
                .toList();
    }

    @Transactional
    public GuardianChildLinkResponse accept(UserPrincipal principal, UUID linkId) {
        GuardianChildLink link = loadLink(linkId);
        if (link.getStatus() != GuardianChildLinkStatus.PENDING) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.LINK_NOT_PENDING, "Link is not pending");
        }
        if (!canResolvePendingLink(principal, link)) {
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCode.LINK_ACCEPT_FORBIDDEN, "Cannot accept this link");
        }
        link.accept(Instant.now(clock));
        auditService.log(principal.id(), AuditAction.GUARDIAN_CHILD_LINK_ACCEPTED, AuditTargetType.GUARDIAN_CHILD_LINK, link.getId());
        return GuardianChildLinkResponse.from(link);
    }

    @Transactional
    public GuardianChildLinkResponse reject(UserPrincipal principal, UUID linkId) {
        GuardianChildLink link = loadLink(linkId);
        if (link.getStatus() != GuardianChildLinkStatus.PENDING) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.LINK_NOT_PENDING, "Link is not pending");
        }
        if (!canResolvePendingLink(principal, link)) {
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCode.LINK_REJECT_FORBIDDEN, "Cannot reject this link");
        }
        link.reject();
        auditService.log(principal.id(), AuditAction.GUARDIAN_CHILD_LINK_REJECTED, AuditTargetType.GUARDIAN_CHILD_LINK, link.getId());
        return GuardianChildLinkResponse.from(link);
    }

    @Transactional
    public void revoke(UserPrincipal principal, UUID linkId) {
        GuardianChildLink link = loadLink(linkId);
        if (!canRevoke(principal, link)) {
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCode.LINK_REVOKE_FORBIDDEN, "Cannot revoke this link");
        }
        link.revoke();
        auditService.log(principal.id(), AuditAction.GUARDIAN_CHILD_LINK_REVOKED, AuditTargetType.GUARDIAN_CHILD_LINK, link.getId());
    }

    private boolean canResolvePendingLink(UserPrincipal principal, GuardianChildLink link) {
        if (principal.role() == UserRole.ADMIN) {
            return true;
        }
        if (principal.role() == UserRole.CHILD) {
            return principal.id().equals(link.getChild().getId());
        }
        return principal.role() == UserRole.GUARDIAN
                && linkRepository.existsByGuardianIdAndChildIdAndStatus(
                principal.id(),
                link.getChild().getId(),
                GuardianChildLinkStatus.ACCEPTED);
    }

    private boolean canRevoke(UserPrincipal principal, GuardianChildLink link) {
        if (principal.role() == UserRole.ADMIN) {
            return true;
        }
        if (principal.role() == UserRole.CHILD) {
            return principal.id().equals(link.getChild().getId());
        }
        return principal.role() == UserRole.GUARDIAN
                && (principal.id().equals(link.getGuardian().getId())
                || linkRepository.existsByGuardianIdAndChildIdAndStatus(
                principal.id(),
                link.getChild().getId(),
                GuardianChildLinkStatus.ACCEPTED));
    }

    private GuardianChildLink loadLink(UUID linkId) {
        return linkRepository.findById(linkId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCode.LINK_NOT_FOUND, "Link not found"));
    }

    private UserAccount loadUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, ErrorCode.USER_NOT_FOUND, "User not found"));
    }

    private void requireRole(UserPrincipal principal, UserRole role) {
        if (principal.role() != role) {
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN, "Access denied");
        }
    }
}
