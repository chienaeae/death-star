package com.deathstar.vader.workspace.service;

import com.deathstar.vader.workspace.entity.Workspace;
import com.deathstar.vader.workspace.entity.WorkspaceInvitation;
import com.deathstar.vader.workspace.entity.WorkspaceMember;
import com.deathstar.vader.workspace.entity.WorkspaceMemberId;
import com.deathstar.vader.workspace.repository.WorkspaceInvitationRepository;
import com.deathstar.vader.workspace.repository.WorkspaceMemberRepository;
import com.deathstar.vader.workspace.repository.WorkspaceRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final WorkspaceInvitationRepository workspaceInvitationRepository;

    @Transactional
    public Workspace createWorkspace(UUID ownerId, String name, boolean isDefault) {
        Workspace w = new Workspace();
        w.setId(UUID.randomUUID());
        w.setName(name);
        w.setOwnerId(ownerId);
        w.setIsDefault(isDefault);
        workspaceRepository.save(w);

        WorkspaceMember wm = new WorkspaceMember();
        wm.setId(new WorkspaceMemberId(w.getId(), ownerId));
        wm.setRole("OWNER");

        // If this is the user's first workspace, make it active
        if (workspaceMemberRepository.findFirstById_UserId(ownerId) == null) {
            wm.setIsActive(true);
        }

        workspaceMemberRepository.save(wm);

        return w;
    }

    @Transactional
    public Workspace createDefaultWorkspace(UUID userId) {
        return createWorkspace(userId, "Personal Workspace", true);
    }

    @Transactional(readOnly = true)
    public boolean isMember(UUID userId, UUID workspaceId) {
        return workspaceMemberRepository.existsById_WorkspaceIdAndId_UserId(workspaceId, userId);
    }

    @Transactional
    public void setActiveWorkspace(UUID userId, UUID workspaceId) {
        if (!isMember(userId, workspaceId)) {
            throw new IllegalArgumentException("User is not a member of this workspace");
        }

        List<WorkspaceMember> members = workspaceMemberRepository.findById_UserId(userId);
        for (WorkspaceMember m : members) {
            boolean active = m.getId().getWorkspaceId().equals(workspaceId);
            m.setIsActive(active);
        }
        workspaceMemberRepository.saveAll(members);
    }

    @Transactional(readOnly = true)
    public UUID getActiveWorkspace(UUID userId) {
        WorkspaceMember wm = workspaceMemberRepository.findFirstById_UserIdAndIsActiveTrue(userId);
        if (wm != null) {
            return wm.getId().getWorkspaceId();
        }

        wm = workspaceMemberRepository.findFirstById_UserId(userId);
        if (wm != null) {
            return wm.getId().getWorkspaceId();
        }
        return null;
    }

    @Transactional(readOnly = true)
    public List<Workspace> getUserWorkspaces(UUID userId) {
        return workspaceRepository.findAllByUserId(userId);
    }

    @Transactional
    public void inviteUser(UUID workspaceId, String email) {
        WorkspaceInvitation inv = new WorkspaceInvitation();
        inv.setId(UUID.randomUUID());
        inv.setWorkspaceId(workspaceId);
        inv.setEmail(email);
        inv.setToken(UUID.randomUUID().toString());
        inv.setStatus(WorkspaceInvitation.STATUS_PENDING);
        inv.setExpiresAt(OffsetDateTime.now().plusDays(7));
        workspaceInvitationRepository.save(inv);
        // In a real system, send email here
    }

    @Transactional
    public Workspace acceptInvite(String token, UUID userId) {
        WorkspaceInvitation inv =
                workspaceInvitationRepository
                        .findByToken(token)
                        .orElseThrow(() -> new IllegalArgumentException("Invalid token"));

        if (!WorkspaceInvitation.STATUS_PENDING.equals(inv.getStatus())) {
            throw new IllegalArgumentException("Invite not pending");
        }
        if (inv.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new IllegalArgumentException("Invite expired");
        }

        WorkspaceMember wm = new WorkspaceMember();
        wm.setId(new WorkspaceMemberId(inv.getWorkspaceId(), userId));
        wm.setRole("MEMBER");

        if (workspaceMemberRepository.findFirstById_UserId(userId) == null) {
            wm.setIsActive(true);
        }

        workspaceMemberRepository.save(wm);

        inv.setStatus(WorkspaceInvitation.STATUS_ACCEPTED);
        workspaceInvitationRepository.save(inv);

        return workspaceRepository
                .findById(inv.getWorkspaceId())
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found"));
    }
}
