package com.deathstar.vader.board.repository;

import com.deathstar.vader.board.entity.Board;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardRepository extends JpaRepository<Board, UUID> {
    List<Board> findAllByTenantId(String tenantId);
}
