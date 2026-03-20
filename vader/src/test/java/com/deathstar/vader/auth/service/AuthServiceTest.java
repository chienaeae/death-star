package com.deathstar.vader.auth.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.deathstar.vader.audit.AuditEventFactory;
import com.deathstar.vader.auth.JwtProvider;
import com.deathstar.vader.auth.RefreshToken;
import com.deathstar.vader.auth.User;
import com.deathstar.vader.auth.UserIdentity;
import com.deathstar.vader.auth.repository.*;
import com.deathstar.vader.auth.repository.UserRepository;
import com.deathstar.vader.event.spi.EventPublisher;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;

    @Mock private UserIdentityRepository identityRepository;

    @Mock private RefreshTokenRepository refreshTokenRepository;

    @Mock private PasswordEncoder passwordEncoder;

    @Mock private JwtProvider jwtProvider;

    @Mock private DistributedRevocationService revocationService;

    @Mock private EventPublisher eventPublisher;

    @Mock private AuditEventFactory auditEventFactory;

    @InjectMocks private AuthService authService;

    private User testUser;
    private UserIdentity testIdentity;
    private final String email = "test@empire.com";
    private final String rawPassword = "password123";
    private final String hashedPassword = "hashedPassword123";

    @BeforeEach
    void setUp() {
        testUser = new User(email);
        testUser.setId(UUID.randomUUID());
        testIdentity = new UserIdentity(testUser, AuthService.PROVIDER_LOCAL, hashedPassword);
    }

    @Test
    void register_success() {
        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(passwordEncoder.encode(rawPassword)).thenReturn(hashedPassword);
        when(jwtProvider.generateAccessToken(anyString(), anyString())).thenReturn("access-token");
        when(jwtProvider.getExpirationSeconds()).thenReturn(3600L);

        AuthService.AuthResult result = authService.register(email, rawPassword);

        assertNotNull(result);
        assertEquals("access-token", result.accessToken());
        assertEquals(3600L, result.expiresIn());
        assertNotNull(result.refreshToken());

        verify(userRepository).save(any(User.class));
        verify(identityRepository).save(any(UserIdentity.class));
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void register_emailAlreadyInUse() {
        when(userRepository.existsByEmail(email)).thenReturn(true);

        assertThrows(
                IllegalArgumentException.class, () -> authService.register(email, rawPassword));

        verify(userRepository, never()).save(any(User.class));
        verify(identityRepository, never()).save(any(UserIdentity.class));
    }

    @Test
    void login_success() {
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
        when(identityRepository.findByUserAndProvider(testUser, AuthService.PROVIDER_LOCAL))
                .thenReturn(Optional.of(testIdentity));
        when(passwordEncoder.matches(rawPassword, hashedPassword)).thenReturn(true);
        when(jwtProvider.generateAccessToken(anyString(), anyString())).thenReturn("access-token");
        when(jwtProvider.getExpirationSeconds()).thenReturn(3600L);

        AuthService.AuthResult result = authService.login(email, rawPassword);

        assertNotNull(result);
        assertEquals("access-token", result.accessToken());
        assertEquals(3600L, result.expiresIn());
        assertNotNull(result.refreshToken());

        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void login_userNotFound() {
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> authService.login(email, rawPassword));
    }

    @Test
    void login_userLocked() {
        testUser.setStatus("LOCKED");
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));

        assertThrows(IllegalStateException.class, () -> authService.login(email, rawPassword));
    }

    @Test
    void login_identityNotFound() {
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
        when(identityRepository.findByUserAndProvider(testUser, AuthService.PROVIDER_LOCAL))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> authService.login(email, rawPassword));
    }

    @Test
    void login_invalidPassword() {
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
        when(identityRepository.findByUserAndProvider(testUser, AuthService.PROVIDER_LOCAL))
                .thenReturn(Optional.of(testIdentity));
        when(passwordEncoder.matches(rawPassword, hashedPassword)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> authService.login(email, rawPassword));
    }

    @Test
    void refresh_success() {
        String plainToken = "test-refresh-token";
        String tokenHash = hashToken(plainToken);
        UUID familyId = UUID.randomUUID();
        ZonedDateTime validExpiry = ZonedDateTime.now(ZoneOffset.UTC).plusDays(1);
        RefreshToken validToken = new RefreshToken(testUser, familyId, tokenHash, validExpiry);

        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(validToken));
        when(jwtProvider.generateAccessToken(anyString(), anyString()))
                .thenReturn("new-access-token");
        when(jwtProvider.getExpirationSeconds()).thenReturn(3600L);

        AuthService.AuthResult result = authService.refresh(plainToken);

        assertNotNull(result);
        assertEquals("new-access-token", result.accessToken());
        assertTrue(validToken.isRevoked()); // the old token should be revoked

        verify(refreshTokenRepository, times(2))
                .save(any(RefreshToken.class)); // 1 to update old, 1 to save new
    }

    @Test
    void refresh_invalidToken() {
        String plainToken = "test-refresh-token";
        String tokenHash = hashToken(plainToken);

        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> authService.refresh(plainToken));
    }

    @Test
    void refresh_revokedToken_replayAttack() {
        String plainToken = "compromised-token";
        String tokenHash = hashToken(plainToken);
        UUID familyId = UUID.randomUUID();
        ZonedDateTime validExpiry = ZonedDateTime.now(ZoneOffset.UTC).plusDays(1);
        RefreshToken revokedToken = new RefreshToken(testUser, familyId, tokenHash, validExpiry);
        revokedToken.setRevoked(true);

        when(refreshTokenRepository.findByTokenHash(tokenHash))
                .thenReturn(Optional.of(revokedToken));

        assertThrows(IllegalStateException.class, () -> authService.refresh(plainToken));

        verify(refreshTokenRepository).revokeByFamilyId(familyId);
        verify(revocationService).broadcastRevocation(testUser.getId().toString());
    }

    @Test
    void refresh_expiredToken_replayAttack() {
        String plainToken = "expired-token";
        String tokenHash = hashToken(plainToken);
        UUID familyId = UUID.randomUUID();
        ZonedDateTime expiredTime = ZonedDateTime.now(ZoneOffset.UTC).minusDays(1);
        RefreshToken expiredToken = new RefreshToken(testUser, familyId, tokenHash, expiredTime);

        when(refreshTokenRepository.findByTokenHash(tokenHash))
                .thenReturn(Optional.of(expiredToken));

        assertThrows(IllegalStateException.class, () -> authService.refresh(plainToken));

        verify(refreshTokenRepository).revokeByFamilyId(familyId);
        verify(revocationService).broadcastRevocation(testUser.getId().toString());
    }

    @Test
    void logout_success() {
        String plainToken = "test-refresh-token";
        String tokenHash = hashToken(plainToken);
        UUID familyId = UUID.randomUUID();
        ZonedDateTime validExpiry = ZonedDateTime.now(ZoneOffset.UTC).plusDays(1);
        RefreshToken tokenToRevoke = new RefreshToken(testUser, familyId, tokenHash, validExpiry);

        when(refreshTokenRepository.findByTokenHash(tokenHash))
                .thenReturn(Optional.of(tokenToRevoke));

        authService.logout(plainToken);

        assertTrue(tokenToRevoke.isRevoked());
        verify(refreshTokenRepository).save(tokenToRevoke);
    }

    @Test
    void logout_tokenNotFound() {
        String plainToken = "test-refresh-token";
        String tokenHash = hashToken(plainToken);

        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.empty());

        authService.logout(plainToken); // Should not throw

        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }
}
