package com.deathstar.vader.profile.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.deathstar.vader.auth.JwtProvider;
import com.deathstar.vader.auth.User;
import com.deathstar.vader.auth.service.DistributedRevocationService;
import com.deathstar.vader.dto.generated.UserProfileRequest;
import com.deathstar.vader.profile.UserProfile;
import com.deathstar.vader.profile.service.UserProfileService;
import com.deathstar.vader.workspace.service.WorkspaceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserProfileController.class)
class UserProfileControllerTest {

    @Autowired private MockMvc mockMvc;

    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private UserProfileService profileService;

    @MockitoBean private JwtProvider jwtProvider;

    @MockitoBean private DistributedRevocationService revocationService;

    @MockitoBean private WorkspaceService workspaceService;

    private static final UUID USER_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

    private UsernamePasswordAuthenticationToken createAuth() {
        return new UsernamePasswordAuthenticationToken(
                USER_ID.toString(),
                null,
                java.util.List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    private UserProfile createMockProfile(String displayName, String bio, UUID avatarAssetId) {
        User user = new User("vader@empire.gov");
        user.setId(USER_ID);

        UserProfile profile = new UserProfile(user);
        profile.setDisplayName(displayName);
        profile.setBio(bio);
        profile.setAvatarAssetId(avatarAssetId);
        return profile;
    }

    @Test
    void shouldGetDefaultProfile() throws Exception {
        UserProfile mockProfile = createMockProfile(null, null, null);
        when(profileService.getProfile(eq(USER_ID))).thenReturn(mockProfile);

        mockMvc.perform(
                        get("/users/profile")
                                .with(
                                        SecurityMockMvcRequestPostProcessors.authentication(
                                                createAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("vader@empire.gov"))
                .andExpect(jsonPath("$.displayName").doesNotExist())
                .andExpect(jsonPath("$.bio").doesNotExist());
    }

    @Test
    void shouldUpdateProfile() throws Exception {
        UUID mockAvatarId = UUID.randomUUID();
        UserProfile updatedProfile =
                createMockProfile("Darth Vader", "Dark Lord of the Sith", mockAvatarId);

        when(profileService.updateProfile(
                        eq(USER_ID),
                        eq("Darth Vader"),
                        eq("Dark Lord of the Sith"),
                        eq(mockAvatarId)))
                .thenReturn(updatedProfile);
        when(profileService.resolveAvatarUrl(eq(mockAvatarId)))
                .thenReturn("https://minio.local/avatar.jpg");

        UserProfileRequest request = new UserProfileRequest();
        request.setDisplayName("Darth Vader");
        request.setBio("Dark Lord of the Sith");
        request.setAvatarAssetId(mockAvatarId);

        mockMvc.perform(
                        put("/users/profile")
                                .with(
                                        SecurityMockMvcRequestPostProcessors.authentication(
                                                createAuth()))
                                .with(
                                        org.springframework.security.test.web.servlet.request
                                                .SecurityMockMvcRequestPostProcessors.csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Darth Vader"))
                .andExpect(jsonPath("$.bio").value("Dark Lord of the Sith"))
                .andExpect(jsonPath("$.avatarUrl").value("https://minio.local/avatar.jpg"))
                .andExpect(jsonPath("$.email").value("vader@empire.gov"));
    }
}
