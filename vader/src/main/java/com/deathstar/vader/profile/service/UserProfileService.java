package com.deathstar.vader.profile.service;

import com.deathstar.vader.asset.Asset;
import com.deathstar.vader.asset.repository.AssetRepository;
import com.deathstar.vader.asset.service.AssetService;
import com.deathstar.vader.asset.storage.BlobStorage;
import com.deathstar.vader.auth.User;
import com.deathstar.vader.auth.repository.UserRepository;
import com.deathstar.vader.profile.UserProfile;
import com.deathstar.vader.profile.UserProfileRepository;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;
    private final AssetService assetService;
    private final AssetRepository assetRepository;
    private final BlobStorage blobStorage;
    private static final String ENTITY_TYPE = "USER_PROFILE";

    /**
     * Returns the profile for the given user. If no profile exists yet, creates an empty one (lazy
     * initialization).
     */
    @Transactional
    public UserProfile getProfile(UUID userId) {
        User user = resolveUser(userId);
        return profileRepository.findByUser(user).orElseGet(() -> createEmptyProfile(user));
    }

    /** Updates the profile fields and persists. Creates the profile if it doesn't exist. */
    @Transactional
    public UserProfile updateProfile(
            UUID userId, String displayName, String bio, UUID newAvatarAssetId) {
        User user = resolveUser(userId);
        UserProfile profile =
                profileRepository.findByUser(user).orElseGet(() -> createEmptyProfile(user));

        UUID oldAvatarAssetId = profile.getAvatarAssetId();

        profile.setDisplayName(displayName);
        profile.setBio(bio);
        profile.setUpdatedAt(ZonedDateTime.now());

        // Handle Avatar Linking Logic
        if (!Objects.equals(oldAvatarAssetId, newAvatarAssetId)) {
            if (oldAvatarAssetId != null) {
                assetService.unlinkAsset(oldAvatarAssetId, ENTITY_TYPE, user.getId().toString());
            }
            if (newAvatarAssetId != null) {
                assetService.linkAsset(newAvatarAssetId, ENTITY_TYPE, user.getId().toString());
            }
            profile.setAvatarAssetId(newAvatarAssetId);
        }

        return profileRepository.save(profile);
    }

    public String resolveAvatarUrl(UUID avatarAssetId) {
        if (avatarAssetId == null) {
            return null;
        }
        return assetRepository
                .findById(avatarAssetId)
                .map(Asset::getS3Key)
                .map(blobStorage::getPublicUrl)
                .orElse(null);
    }

    private User resolveUser(UUID userId) {
        return userRepository
                .findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private UserProfile createEmptyProfile(User user) {
        UserProfile profile = new UserProfile(user);
        return profileRepository.save(profile);
    }
}
