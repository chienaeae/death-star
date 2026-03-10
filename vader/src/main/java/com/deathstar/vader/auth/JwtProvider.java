package com.deathstar.vader.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Handles the generation and cryptographic validation of Access Tokens (JWT). Stateless by design.
 */
@Component
public class JwtProvider {

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtProvider(
            @Value(
                            "${jwt.secret:defaultSuperSecretKeyThatNeedsToBeVeryLongAndSecureForHS256Algorithm123}")
                    String secret,
            @Value("${jwt.expiration-ms:900000}") long expirationMs) { // Default 15 mins
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.expirationMs = expirationMs;
    }

    public String generateAccessToken(String userId, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(userId)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    public String validateAndGetUserId(String token) {
        Claims claims =
                Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
        return claims.getSubject();
    }

    public long getExpirationSeconds() {
        return expirationMs / 1000;
    }
}
