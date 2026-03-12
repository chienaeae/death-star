package com.deathstar.vader.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.deathstar.vader.auth.User;
import com.deathstar.vader.auth.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class UserRepositoryTest {

    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired private UserRepository repository;

    @Test
    void shouldFindUserByEmail() {
        User user = new User("test@example.com");
        repository.save(user);

        Optional<User> foundUser = repository.findByEmail("test@example.com");
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void shouldReturnEmptyWhenEmailNotFound() {
        Optional<User> foundUser = repository.findByEmail("notfound@example.com");
        assertThat(foundUser).isEmpty();
    }

    @Test
    void shouldReturnTrueWhenEmailExists() {
        User user = new User("exists@example.com");
        repository.save(user);

        boolean exists = repository.existsByEmail("exists@example.com");
        assertThat(exists).isTrue();
    }

    @Test
    void shouldReturnFalseWhenEmailDoesNotExist() {
        boolean exists = repository.existsByEmail("missing@example.com");
        assertThat(exists).isFalse();
    }
}
