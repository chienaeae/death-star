package com.deathstar.vader.workspace.repository;

import com.deathstar.vader.workspace.entity.WorkspaceMember;
import com.deathstar.vader.workspace.entity.WorkspaceMemberId;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkspaceMemberRepository
        extends JpaRepository<WorkspaceMember, WorkspaceMemberId> {
    boolean existsById_WorkspaceIdAndId_UserId(UUID workspaceId, UUID userId);

    WorkspaceMember findFirstById_UserId(UUID userId);

    WorkspaceMember findFirstById_UserIdAndIsActiveTrue(UUID userId);

    java.util.List<WorkspaceMember> findById_UserId(UUID userId);
}
