package com.deathstar.vader.repository;

import com.deathstar.vader.domain.Todo;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA Repository for Todo. Under Project Loom, JDBC blocking calls (like
 * findAllByOrderByCreatedAtDesc) will automatically yield the underlying OS carrier thread.
 */
public interface TodoRepository extends JpaRepository<Todo, UUID> {

    // Optimized by the 'idx_todos_created_at' index defined in Flyway
    List<Todo> findAllByOrderByCreatedAtDesc();
}
