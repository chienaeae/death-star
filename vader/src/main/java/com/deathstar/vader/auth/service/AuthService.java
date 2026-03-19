package com.deathstar.vader.auth.service;

import com.deathstar.vader.audit.AuditEvent;
import com.deathstar.vader.audit.AuditEventFactory;
import com.deathstar.vader.audit.schema.ActionStatus;
import com.deathstar.vader.audit.schema.CoreResource;
import com.deathstar.vader.audit.schema.UserAction;
import com.deathstar.vader.auth.*;
import com.deathstar.vader.auth.repository.RefreshTokenRepository;
import com.deathstar.vader.auth.repository.UserIdentityRepository;
import com.deathstar.vader.auth.repository.UserRepository;
import com.deathstar.vader.event.domain.EventRoute;
import com.deathstar.vader.event.spi.EventBus;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fully implemented IAM business logic. Handles Registration, Login, Refresh Token Rotation (RTR),
 * and Kill Switch integration.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserIdentityRepository identityRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final DistributedRevocationService revocationService;
    private final EventBus eventBus;
    private final AuditEventFactory auditEventFactory;

    // 7 Days lifetime for Refresh Tokens
    private static final long REFRESH_TOKEN_VALIDITY_DAYS = 7;
    public static final String PROVIDER_LOCAL = "LOCAL";

    @Transactional
    public AuthResult register(String email, String rawPassword) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already in use");
        }

        User user = new User(email);
        user = userRepository.save(user);

        // Decoupled Identity strategy
        String hashedPassword = passwordEncoder.encode(rawPassword);
        UserIdentity identity = new UserIdentity(user, PROVIDER_LOCAL, hashedPassword);
        identityRepository.save(identity);

        eventBus.publishDurable(
                EventRoute.AUDIT,
                "vader",
                auditEventFactory.createPayload(
                        new AuditEvent<>(
                                this,
                                user.getId().toString(),
                                UserAction.USER_REGISTER,
                                CoreResource.USER,
                                user.getId().toString(),
                                ActionStatus.SUCCESS,
                                Map.of())));

        return issueNewSession(user);
    }

    @Transactional
    public AuthResult login(String email, String rawPassword) {
        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new IllegalStateException("User account is locked");
        }

        UserIdentity identity =
                identityRepository
                        .findByUserAndProvider(user, PROVIDER_LOCAL)
                        .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!passwordEncoder.matches(rawPassword, identity.getProviderId())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        eventBus.publishDurable(
                EventRoute.AUDIT,
                "vader",
                auditEventFactory.createPayload(
                        new AuditEvent<>(
                                this,
                                user.getId().toString(),
                                UserAction.USER_LOGIN,
                                CoreResource.USER,
                                user.getId().toString(),
                                ActionStatus.SUCCESS,
                                Map.of())));

        return issueNewSession(user);
    }

    @Transactional
    public AuthResult refresh(String oldRefreshTokenPlainText) {
        String tokenHash = hashToken(oldRefreshTokenPlainText);

        RefreshToken tokenEntity =
                refreshTokenRepository
                        .findByTokenHash(tokenHash)
                        .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        User user = tokenEntity.getUser();
        UUID familyId = tokenEntity.getFamilyId();

        boolean isExpired = tokenEntity.getExpiresAt().isBefore(ZonedDateTime.now(ZoneOffset.UTC));

        // --- THE FIRST PRINCIPLE OF TOKEN LINEAGE (RTR & Family ID) ---
        if (tokenEntity.isRevoked() || isExpired) {
            log.error("REPLAY ATTACK DETECTED or EXPIRED TOKEN USED! Family: {}", familyId);

            // 1. The Guillotine: Atomically revoke ALL tokens in this family
            refreshTokenRepository.revokeByFamilyId(familyId);

            // 2. The Kill Switch: NATS broadcast to instantly drop JWT validation across all pods
            revocationService.broadcastRevocation(user.getId().toString());

            throw new IllegalStateException("Session compromised. Terminated.");
        }

        // --- Normal Rotation (Invalidate old, issue new) ---
        tokenEntity.setRevoked(true);
        refreshTokenRepository.save(tokenEntity);

        // Issue new tokens tied to the EXACT SAME familyId
        return issueTokens(user, familyId);
    }

    @Transactional
    public void logout(String refreshTokenPlainText) {
        String tokenHash = hashToken(refreshTokenPlainText);
        refreshTokenRepository
                .findByTokenHash(tokenHash)
                .ifPresent(
                        token -> {
                            token.setRevoked(true);
                            refreshTokenRepository.save(token);
                            eventBus.publishDurable(
                                    EventRoute.AUDIT,
                                    "vader",
                                    auditEventFactory.createPayload(
                                            new AuditEvent<>(
                                                    this,
                                                    token.getUser().getId().toString(),
                                                    UserAction.USER_LOGOUT,
                                                    CoreResource.USER,
                                                    token.getFamilyId().toString(),
                                                    ActionStatus.SUCCESS,
                                                    Map.of())));
                        });
    }

    // --- Internal Helpers ---

    private AuthResult issueNewSession(User user) {
        UUID newFamilyId = UUID.randomUUID();
        return issueTokens(user, newFamilyId);
    }

    private AuthResult issueTokens(User user, UUID familyId) {
        String accessToken =
                jwtProvider.generateAccessToken(user.getId().toString(), user.getRole());
        String plainRefreshToken = UUID.randomUUID().toString();

        ZonedDateTime expiresAt =
                ZonedDateTime.now(ZoneOffset.UTC).plusDays(REFRESH_TOKEN_VALIDITY_DAYS);

        RefreshToken refreshTokenEntity =
                new RefreshToken(user, familyId, hashToken(plainRefreshToken), expiresAt);
        refreshTokenRepository.save(refreshTokenEntity);

        return new AuthResult(accessToken, jwtProvider.getExpirationSeconds(), plainRefreshToken);
    }

    /**
     * Fast SHA-256 hashing for Token storage. We DO NOT use BCrypt here because tokens already have
     * extreme entropy (UUIDs).
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash); // Java 17+ feature for fast Hex conversion
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    public record AuthResult(String accessToken, long expiresIn, String refreshToken) {}
}
