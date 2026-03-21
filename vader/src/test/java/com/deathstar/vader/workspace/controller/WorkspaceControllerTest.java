package com.deathstar.vader.workspace.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.deathstar.vader.auth.JwtProvider;
import com.deathstar.vader.auth.service.DistributedRevocationService;
import com.deathstar.vader.dto.generated.WorkspaceCreateRequest;
import com.deathstar.vader.dto.generated.WorkspaceInviteRequest;
import com.deathstar.vader.loom.spi.IdentityResolver;
import com.deathstar.vader.workspace.entity.Workspace;
import com.deathstar.vader.workspace.service.WorkspaceService;
import com.deathstar.vader.auth.repository.UserRepository;
import com.deathstar.vader.auth.User;
import java.util.Optional;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(WorkspaceController.class)
class WorkspaceControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private WorkspaceService workspaceService;
    @MockitoBean private IdentityResolver identityResolver;

    @MockitoBean private JwtProvider jwtProvider;
    @MockitoBean private DistributedRevocationService revocationService;
    @MockitoBean private UserRepository userRepository;

    private UUID userId;
    private UUID workspaceId;

    @BeforeEach
    void setUp() {
        userId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        workspaceId = UUID.randomUUID();
        when(identityResolver.currentUserId()).thenReturn(userId.toString());
    }

    @Test
    @WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000")
    void workspacesGet_success() throws Exception {
        Workspace w = new Workspace();
        w.setId(workspaceId);
        w.setName("API Workspace");
        w.setOwnerId(userId);
        w.setIsDefault(true);

        when(workspaceService.getUserWorkspaces(userId)).thenReturn(List.of(w));
        when(workspaceService.getActiveWorkspace(userId)).thenReturn(workspaceId);

        mockMvc.perform(get("/workspaces").with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(workspaceId.toString()))
                .andExpect(jsonPath("$[0].name").value("API Workspace"))
                .andExpect(jsonPath("$[0].isDefault").value(true))
                .andExpect(jsonPath("$[0].isActive").value(true));
    }

    @Test
    @WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000")
    void workspacesPost_success() throws Exception {
        WorkspaceCreateRequest req = new WorkspaceCreateRequest();
        req.setName("New Workspace");

        Workspace w = new Workspace();
        w.setId(workspaceId);
        w.setName("New Workspace");
        w.setOwnerId(userId);
        w.setIsDefault(false);

        when(workspaceService.createWorkspace(userId, "New Workspace", false)).thenReturn(w);
        when(workspaceService.getActiveWorkspace(userId)).thenReturn(workspaceId);

        mockMvc.perform(
                        post("/workspaces")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req))
                                .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(workspaceId.toString()))
                .andExpect(jsonPath("$.name").value("New Workspace"))
                .andExpect(jsonPath("$.isDefault").value(false))
                .andExpect(jsonPath("$.isActive").value(true));
    }

    @Test
    @WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000")
    void workspacesIdInvitationsPost_success() throws Exception {
        WorkspaceInviteRequest req = new WorkspaceInviteRequest();
        req.setEmail("test@empire.gov");

        mockMvc.perform(
                        post("/workspaces/{id}/invitations", workspaceId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req))
                                .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk());

        verify(workspaceService).inviteUser(workspaceId, "test@empire.gov");
    }

    @Test
    @WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000")
    void workspacesInvitationsTokenAcceptPost_success() throws Exception {
        String token = "invite-token-123";

        Workspace w = new Workspace();
        w.setId(workspaceId);
        w.setOwnerId(userId);

        when(workspaceService.acceptInvite(token, userId)).thenReturn(w);
        when(workspaceService.getActiveWorkspace(userId)).thenReturn(workspaceId);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        mockMvc.perform(
                        post("/workspaces/invitations/{token}/accept", token)
                                .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk());

        verify(workspaceService).acceptInvite(token, userId);
    }
}
