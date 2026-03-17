package com.deathstar.vader.board.repository;

import com.deathstar.vader.board.entity.BoardColumn;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardColumnRepository extends JpaRepository<BoardColumn, UUID> {}
