package com.deathstar.vader.workspace.controller;

import com.deathstar.vader.api.WorkspacesApi;
import com.deathstar.vader.auth.User;
import com.deathstar.vader.auth.repository.UserRepository;
import com.deathstar.vader.dto.generated.Workspace;
import com.deathstar.vader.dto.generated.WorkspaceCreateRequest;
import com.deathstar.vader.dto.generated.WorkspaceInviteRequest;
import com.deathstar.vader.loom.spi.IdentityResolver;
import com.deathstar.vader.workspace.service.WorkspaceService;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class WorkspaceController implements WorkspacesApi {

    private final WorkspaceService workspaceService;
    private final IdentityResolver identityResolver;
    private final UserRepository userRepository;

    @Override
    public ResponseEntity<List<Workspace>> workspacesGet() {
        UUID userId = UUID.fromString(identityResolver.currentUserId());
        UUID activeWorkspaceId = workspaceService.getActiveWorkspace(userId);

        List<com.deathstar.vader.workspace.entity.Workspace> userWorkspaces =
                workspaceService.getUserWorkspaces(userId);

        Set<UUID> userIds =
                userWorkspaces.stream()
                        .map(com.deathstar.vader.workspace.entity.Workspace::getOwnerId)
                        .collect(Collectors.toSet());

        Map<UUID, String> ownerEmailMap =
                userRepository.findAllById(userIds).stream()
                        .collect(Collectors.toMap(User::getId, User::getEmail));

        List<Workspace> dtos =
                userWorkspaces.stream()
                        .map(w -> mapToDto(w, activeWorkspaceId, ownerEmailMap.get(w.getOwnerId())))
                        .toList();
        return ResponseEntity.ok(dtos);
    }

    @Override
    public ResponseEntity<Workspace> workspacesPost(WorkspaceCreateRequest request) {
        UUID userId = UUID.fromString(identityResolver.currentUserId());
        var w = workspaceService.createWorkspace(userId, request.getName(), false);
        UUID activeWorkspaceId = workspaceService.getActiveWorkspace(userId);

        String ownerEmail = userRepository.findById(userId).map(User::getEmail).orElse(null);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapToDto(w, activeWorkspaceId, ownerEmail));
    }

    @Override
    public ResponseEntity<Void> workspacesIdInvitationsPost(
            UUID id, WorkspaceInviteRequest request) {
        workspaceService.inviteUser(id, request.getEmail());
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Workspace> workspacesInvitationsTokenAcceptPost(String token) {
        UUID userId = UUID.fromString(identityResolver.currentUserId());
        var w = workspaceService.acceptInvite(token, userId);
        UUID activeWorkspaceId = workspaceService.getActiveWorkspace(userId);
        String ownerEmail =
                userRepository.findById(w.getOwnerId()).map(User::getEmail).orElse(null);
        return ResponseEntity.ok(mapToDto(w, activeWorkspaceId, ownerEmail));
    }

    @Override
    public ResponseEntity<Void> workspacesIdActivePost(UUID id) {
        UUID userId = UUID.fromString(identityResolver.currentUserId());
        workspaceService.setActiveWorkspace(userId, id);
        return ResponseEntity.ok().build();
    }

    private Workspace mapToDto(
            com.deathstar.vader.workspace.entity.Workspace w,
            UUID activeWorkspaceId,
            String ownerEmail) {
        Workspace dto = new Workspace();
        dto.setId(w.getId());
        dto.setName(w.getName());
        dto.setOwnerId(w.getOwnerId());
        dto.setCreatedAt(w.getCreatedAt());
        dto.setIsActive(w.getId().equals(activeWorkspaceId));
        dto.setIsDefault(w.getIsDefault());
        dto.setOwnerEmail(ownerEmail);
        return dto;
    }
}
