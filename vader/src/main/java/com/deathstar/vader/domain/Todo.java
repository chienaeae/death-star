package com.deathstar.vader.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.ZonedDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Represents the Todo entity mapped to the PostgreSQL database. We rely on standard JPA (Hibernate)
 * which works perfectly with Virtual Threads, eliminating the need for complex R2DBC reactive
 * transaction managers.
 */
@Entity
@Table(name = "todos")
@Getter
@Setter
@NoArgsConstructor
public class Todo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String title;

    private boolean completed = false;

    // Automatically managed by Hibernate
    @CreationTimestamp private ZonedDateTime createdAt;

    public Todo(String title) {
        this.title = title;
    }
}
