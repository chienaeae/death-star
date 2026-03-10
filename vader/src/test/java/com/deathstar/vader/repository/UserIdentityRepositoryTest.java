package com.deathstar.vader.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.deathstar.vader.domain.User;
import com.deathstar.vader.domain.UserIdentity;
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
class UserIdentityRepositoryTest {

    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired private UserIdentityRepository userIdentityRepository;
    @Autowired private UserRepository userRepository;

    @Test
    void shouldFindUserIdentityByUserAndProvider() {
        User user = new User("identity@example.com");
        user = userRepository.save(user);

        UserIdentity identity = new UserIdentity(user, "LOCAL", "hashed_password");
        userIdentityRepository.save(identity);

        Optional<UserIdentity> foundIdentity =
                userIdentityRepository.findByUserAndProvider(user, "LOCAL");
        assertThat(foundIdentity).isPresent();
        assertThat(foundIdentity.get().getProviderId()).isEqualTo("hashed_password");
    }

    @Test
    void shouldReturnEmptyWhenProviderDoesNotMatch() {
        User user = new User("other@example.com");
        user = userRepository.save(user);

        UserIdentity identity = new UserIdentity(user, "LOCAL", "hashed_password");
        userIdentityRepository.save(identity);

        Optional<UserIdentity> foundIdentity =
                userIdentityRepository.findByUserAndProvider(user, "GOOGLE");
        assertThat(foundIdentity).isEmpty();
    }
}
