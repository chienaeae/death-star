package com.deathstar.vader.workspace.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.deathstar.vader.workspace.entity.Workspace;
import com.deathstar.vader.workspace.entity.WorkspaceInvitation;
import com.deathstar.vader.workspace.entity.WorkspaceMember;
import com.deathstar.vader.workspace.entity.WorkspaceMemberId;
import com.deathstar.vader.workspace.repository.WorkspaceInvitationRepository;
import com.deathstar.vader.workspace.repository.WorkspaceMemberRepository;
import com.deathstar.vader.workspace.repository.WorkspaceRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkspaceServiceTest {

    @Mock private WorkspaceRepository workspaceRepository;
    @Mock private WorkspaceMemberRepository workspaceMemberRepository;
    @Mock private WorkspaceInvitationRepository workspaceInvitationRepository;

    @InjectMocks private WorkspaceService workspaceService;

    private UUID userId;
    private UUID workspaceId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        workspaceId = UUID.randomUUID();
    }

    @Test
    void createWorkspace_success() {
        Workspace w = workspaceService.createWorkspace(userId, "Test Auth Workspace", false);

        assertNotNull(w);
        assertEquals("Test Auth Workspace", w.getName());
        assertEquals(userId, w.getOwnerId());
        assertFalse(w.getIsDefault());

        verify(workspaceRepository).save(any(Workspace.class));
        ArgumentCaptor<WorkspaceMember> memberCaptor =
                ArgumentCaptor.forClass(WorkspaceMember.class);
        verify(workspaceMemberRepository).save(memberCaptor.capture());

        WorkspaceMember savedMember = memberCaptor.getValue();
        assertEquals("OWNER", savedMember.getRole());
        assertEquals(userId, savedMember.getId().getUserId());
        assertTrue(savedMember.getIsActive());
    }

    @Test
    void createWorkspace_default_success() {
        Workspace w = workspaceService.createWorkspace(userId, "Test Default Workspace", true);
        assertNotNull(w);
        assertTrue(w.getIsDefault());
    }

    @Test
    void setActiveWorkspace_success() {
        WorkspaceMember oldActive = new WorkspaceMember();
        oldActive.setId(new WorkspaceMemberId(UUID.randomUUID(), userId));
        oldActive.setIsActive(true);
        
        WorkspaceMember newActive = new WorkspaceMember();
        newActive.setId(new WorkspaceMemberId(workspaceId, userId));
        newActive.setIsActive(false);

        when(workspaceMemberRepository.existsById_WorkspaceIdAndId_UserId(workspaceId, userId)).thenReturn(true);
        when(workspaceMemberRepository.findById_UserId(userId)).thenReturn(java.util.List.of(oldActive, newActive));

        workspaceService.setActiveWorkspace(userId, workspaceId);

        assertFalse(oldActive.getIsActive());
        assertTrue(newActive.getIsActive());
        verify(workspaceMemberRepository).saveAll(any());
    }

    @Test
    void isMember_returnsTrue() {
        when(workspaceMemberRepository.existsById_WorkspaceIdAndId_UserId(workspaceId, userId))
                .thenReturn(true);

        assertTrue(workspaceService.isMember(userId, workspaceId));
    }

    @Test
    void getActiveWorkspace_success() {
        WorkspaceMember wm = new WorkspaceMember();
        wm.setId(new WorkspaceMemberId(workspaceId, userId));
        when(workspaceMemberRepository.findFirstById_UserIdAndIsActiveTrue(userId)).thenReturn(wm);

        UUID resultId = workspaceService.getActiveWorkspace(userId);
        assertEquals(workspaceId, resultId);
    }

    @Test
    void getActiveWorkspace_fallback_success() {
        WorkspaceMember wm = new WorkspaceMember();
        wm.setId(new WorkspaceMemberId(workspaceId, userId));
        when(workspaceMemberRepository.findFirstById_UserIdAndIsActiveTrue(userId))
                .thenReturn(null);
        when(workspaceMemberRepository.findFirstById_UserId(userId)).thenReturn(wm);

        UUID resultId = workspaceService.getActiveWorkspace(userId);
        assertEquals(workspaceId, resultId);
    }

    @Test
    void getActiveWorkspace_notFound_returnsNull() {
        when(workspaceMemberRepository.findFirstById_UserIdAndIsActiveTrue(userId))
                .thenReturn(null);
        when(workspaceMemberRepository.findFirstById_UserId(userId)).thenReturn(null);

        assertNull(workspaceService.getActiveWorkspace(userId));
    }

    @Test
    void inviteUser_success() {
        workspaceService.inviteUser(workspaceId, "test@empire.gov");

        ArgumentCaptor<WorkspaceInvitation> inviteCaptor =
                ArgumentCaptor.forClass(WorkspaceInvitation.class);
        verify(workspaceInvitationRepository).save(inviteCaptor.capture());

        WorkspaceInvitation savedInv = inviteCaptor.getValue();
        assertEquals("test@empire.gov", savedInv.getEmail());
        assertEquals(workspaceId, savedInv.getWorkspaceId());
        assertEquals(WorkspaceInvitation.STATUS_PENDING, savedInv.getStatus());
        assertNotNull(savedInv.getToken());
    }

    @Test
    void acceptInvite_success() {
        String token = "valid-token";
        WorkspaceInvitation inv = new WorkspaceInvitation();
        inv.setWorkspaceId(workspaceId);
        inv.setToken(token);
        inv.setStatus(WorkspaceInvitation.STATUS_PENDING);
        inv.setExpiresAt(OffsetDateTime.now().plusDays(1));

        when(workspaceInvitationRepository.findByToken(token)).thenReturn(Optional.of(inv));

        Workspace w = new Workspace();
        w.setId(workspaceId);
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(w));

        workspaceService.acceptInvite(token, userId);

        ArgumentCaptor<WorkspaceMember> memberCaptor =
                ArgumentCaptor.forClass(WorkspaceMember.class);
        verify(workspaceMemberRepository).save(memberCaptor.capture());
        assertEquals("MEMBER", memberCaptor.getValue().getRole());
        assertEquals(workspaceId, memberCaptor.getValue().getId().getWorkspaceId());
        assertEquals(userId, memberCaptor.getValue().getId().getUserId());

        ArgumentCaptor<WorkspaceInvitation> updateCaptor =
                ArgumentCaptor.forClass(WorkspaceInvitation.class);
        verify(workspaceInvitationRepository).save(updateCaptor.capture());
        assertEquals(WorkspaceInvitation.STATUS_ACCEPTED, updateCaptor.getValue().getStatus());
    }

    @Test
    void acceptInvite_expired_throwsException() {
        String token = "expired-token";
        WorkspaceInvitation inv = new WorkspaceInvitation();
        inv.setStatus("PENDING");
        inv.setExpiresAt(OffsetDateTime.now().minusDays(1));

        when(workspaceInvitationRepository.findByToken(token)).thenReturn(Optional.of(inv));

        assertThrows(
                IllegalArgumentException.class, () -> workspaceService.acceptInvite(token, userId));
    }
}
