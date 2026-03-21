package com.deathstar.vader.auth.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.deathstar.vader.auth.JwtProvider;
import com.deathstar.vader.auth.User;
import com.deathstar.vader.auth.repository.UserRepository;
import com.deathstar.vader.auth.service.AuthService;
import com.deathstar.vader.auth.service.DistributedRevocationService;
import com.deathstar.vader.dto.generated.AuthRegisterPostRequest;
import com.deathstar.vader.dto.generated.LoginRequest;
import com.deathstar.vader.workspace.service.WorkspaceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;

    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private AuthService authService;

    @MockitoBean private UserRepository userRepository;

    @MockitoBean private JwtProvider jwtProvider;

    @MockitoBean private DistributedRevocationService revocationService;

    @MockitoBean private WorkspaceService workspaceService;

    @Test
    @WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000")
    void shouldRegisterUser() throws Exception {
        AuthRegisterPostRequest request = new AuthRegisterPostRequest();
        request.setEmail("luke@rebellion.org");
        request.setPassword("force123");

        AuthService.AuthResult mockResult =
                new AuthService.AuthResult("access-token-xyz", 3600, "refresh-token-xyz");

        when(authService.register(eq("luke@rebellion.org"), eq("force123"))).thenReturn(mockResult);

        mockMvc.perform(
                        post("/auth/register")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(cookie().exists("refreshToken"))
                .andExpect(cookie().value("refreshToken", "refresh-token-xyz"))
                .andExpect(jsonPath("$.accessToken").value("access-token-xyz"))
                .andExpect(jsonPath("$.expiresIn").value(3600));

        verify(authService).register("luke@rebellion.org", "force123");
    }

    @Test
    @WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000")
    void shouldLoginUser() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("vader@empire.gov");
        request.setPassword("darkside");

        AuthService.AuthResult mockResult =
                new AuthService.AuthResult("access-token-abc", 3600, "refresh-token-abc");

        when(authService.login(eq("vader@empire.gov"), eq("darkside"))).thenReturn(mockResult);

        mockMvc.perform(
                        post("/auth/login")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("refreshToken"))
                .andExpect(cookie().value("refreshToken", "refresh-token-abc"))
                .andExpect(jsonPath("$.accessToken").value("access-token-abc"))
                .andExpect(jsonPath("$.expiresIn").value(3600));

        verify(authService).login("vader@empire.gov", "darkside");
    }

    @Test
    void shouldGetMe() throws Exception {
        UUID userId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        User mockUser = new User("vader@empire.gov");
        mockUser.setId(userId);
        mockUser.setStatus("ACTIVE");
        mockUser.setRole("USER");

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

        org.springframework.security.authentication.UsernamePasswordAuthenticationToken auth =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        userId.toString(),
                        null,
                        java.util.List.of(
                                new org.springframework.security.core.authority
                                        .SimpleGrantedAuthority("ROLE_USER")));

        mockMvc.perform(
                        get("/auth/me")
                                .with(
                                        org.springframework.security.test.web.servlet.request
                                                .SecurityMockMvcRequestPostProcessors
                                                .authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.email").value("vader@empire.gov"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.role").value("USER"));
    }
}
