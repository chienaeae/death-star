package com.deathstar.vader.workspace.repository;

import com.deathstar.vader.workspace.entity.Workspace;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkspaceRepository extends JpaRepository<Workspace, UUID> {

    @Query(
            "SELECT w FROM Workspace w JOIN WorkspaceMember wm ON w.id = wm.id.workspaceId WHERE wm.id.userId = :userId ORDER BY w.createdAt ASC")
    List<Workspace> findAllByUserId(@Param("userId") UUID userId);
}
