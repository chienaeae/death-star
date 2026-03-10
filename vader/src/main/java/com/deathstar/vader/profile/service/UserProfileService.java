package com.deathstar.vader.profile.service;

import com.deathstar.vader.auth.User;
import com.deathstar.vader.auth.repository.UserRepository;
import com.deathstar.vader.profile.UserProfile;
import com.deathstar.vader.profile.UserProfileRepository;
import java.time.ZonedDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;

    /**
     * Returns the profile for the given user. If no profile exists yet, creates an empty one
     * (lazy initialization).
     */
    @Transactional
    public UserProfile getProfile(UUID userId) {
        User user = resolveUser(userId);
        return profileRepository.findByUser(user).orElseGet(() -> createEmptyProfile(user));
    }

    /** Updates the profile fields and persists. Creates the profile if it doesn't exist. */
    @Transactional
    public UserProfile updateProfile(UUID userId, String displayName, String bio) {
        User user = resolveUser(userId);
        UserProfile profile =
                profileRepository.findByUser(user).orElseGet(() -> createEmptyProfile(user));

        profile.setDisplayName(displayName);
        profile.setBio(bio);
        profile.setUpdatedAt(ZonedDateTime.now());

        return profileRepository.save(profile);
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
