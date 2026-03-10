package com.deathstar.vader.controller;

import com.deathstar.vader.api.AuthApi;
import com.deathstar.vader.auth.User;
import com.deathstar.vader.auth.service.AuthService;
import com.deathstar.vader.dto.generated.AuthRegisterPostRequest;
import com.deathstar.vader.dto.generated.LoginRequest;
import com.deathstar.vader.dto.generated.TokenResponse;
import com.deathstar.vader.auth.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private final AuthService authService;
    private final UserRepository userRepository;

    private static final long REFRESH_TOKEN_MAX_AGE = 7 * 24 * 60 * 60;

    private HttpHeaders createCookieHeader(String refreshToken, long maxAge) {
        ResponseCookie cookie =
                ResponseCookie.from("refreshToken", refreshToken)
                        .httpOnly(true)
                        .secure(false)
                        .sameSite("Strict")
                        .path("/auth")
                        .maxAge(maxAge)
                        .build();

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, cookie.toString());
        return headers;
    }

    @Override
    public ResponseEntity<TokenResponse> authRegisterPost(AuthRegisterPostRequest request) {
        // Use generated AuthRegisterPostRequest
        AuthService.AuthResult result =
                authService.register(request.getEmail(), request.getPassword());

        return ResponseEntity.status(HttpStatus.CREATED)
                .headers(createCookieHeader(result.refreshToken(), REFRESH_TOKEN_MAX_AGE))
                .body(
                        new TokenResponse()
                                .accessToken(result.accessToken())
                                .expiresIn(result.expiresIn()));
    }

    @Override
    public ResponseEntity<TokenResponse> authLoginPost(LoginRequest loginRequest) {
        AuthService.AuthResult result =
                authService.login(loginRequest.getEmail(), loginRequest.getPassword());

        // Hack 2: Maintain complete control over HttpHeaders
        return ResponseEntity.ok()
                .headers(createCookieHeader(result.refreshToken(), REFRESH_TOKEN_MAX_AGE))
                .body(
                        new TokenResponse()
                                .accessToken(result.accessToken())
                                .expiresIn(result.expiresIn()));
    }

    @Override
    public ResponseEntity<TokenResponse> authRefreshPost(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("Refresh token is missing from cookies");
        }

        AuthService.AuthResult result = authService.refresh(refreshToken);

        return ResponseEntity.ok()
                .headers(createCookieHeader(result.refreshToken(), REFRESH_TOKEN_MAX_AGE))
                .body(
                        new TokenResponse()
                                .accessToken(result.accessToken())
                                .expiresIn(result.expiresIn()));
    }

    @Override
    public ResponseEntity<Void> authLogoutPost(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            authService.logout(refreshToken);
        }
        return ResponseEntity.ok().headers(createCookieHeader("", 0)).build();
    }

    @Override
    public ResponseEntity<com.deathstar.vader.dto.generated.User> authMeGet() {
        String userIdString =
                (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UUID userId = UUID.fromString(userIdString);

        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new IllegalArgumentException("User no longer exists"));

        // Map Domain to DTO
        com.deathstar.vader.dto.generated.User userDto =
                new com.deathstar.vader.dto.generated.User()
                        .id(user.getId())
                        .email(user.getEmail())
                        .status(
                                com.deathstar.vader.dto.generated.User.StatusEnum.fromValue(
                                        user.getStatus()))
                        .role(
                                com.deathstar.vader.dto.generated.User.RoleEnum.fromValue(
                                        user.getRole()));

        return ResponseEntity.ok(userDto);
    }
}
