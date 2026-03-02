package com.deathstar.vader.dto.auth;

import java.util.UUID;

/**
 * Data Transfer Objects for IAM endpoints. Utilizing Java Records for immutable, zero-boilerplate
 * structures.
 */
public class AuthDto {

    public record AuthRequest(String email, String password) {}

    public record TokenResponse(String accessToken, long expiresIn) {}

    public record UserProfileResponse(UUID id, String email, String status, String role) {}
}
