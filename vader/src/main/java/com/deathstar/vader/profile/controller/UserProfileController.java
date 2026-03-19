package com.deathstar.vader.profile.controller;

import com.deathstar.vader.api.UsersApi;
import com.deathstar.vader.dto.generated.UserProfileRequest;
import com.deathstar.vader.profile.UserProfile;
import com.deathstar.vader.profile.service.UserProfileService;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserProfileController implements UsersApi {

    private final UserProfileService profileService;

    @Override
    public ResponseEntity<com.deathstar.vader.dto.generated.UserProfile> usersProfileGet() {
        UUID userId = extractUserId();
        UserProfile profile = profileService.getProfile(userId);
        return ResponseEntity.ok(mapToDto(profile));
    }

    @Override
    public ResponseEntity<com.deathstar.vader.dto.generated.UserProfile> usersProfilePut(
            UserProfileRequest request) {
        UUID userId = extractUserId();

        UUID avatarAssetId = request.getAvatarAssetId();

        UserProfile profile =
                profileService.updateProfile(
                        userId, request.getDisplayName(), request.getBio(), avatarAssetId);
        return ResponseEntity.ok(mapToDto(profile));
    }

    private UUID extractUserId() {
        String userIdString =
                (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return UUID.fromString(userIdString);
    }

    private com.deathstar.vader.dto.generated.UserProfile mapToDto(UserProfile entity) {
        String avatarUrlStr = profileService.resolveAvatarUrl(entity.getAvatarAssetId());
        URI avatarUri = avatarUrlStr != null ? URI.create(avatarUrlStr) : null;

        return new com.deathstar.vader.dto.generated.UserProfile()
                .displayName(entity.getDisplayName())
                .bio(entity.getBio())
                .avatarUrl(avatarUri)
                .email(entity.getUser().getEmail())
                .createdAt(
                        entity.getUser().getCreatedAt() != null
                                ? entity.getUser().getCreatedAt().toOffsetDateTime()
                                : null);
    }
}
