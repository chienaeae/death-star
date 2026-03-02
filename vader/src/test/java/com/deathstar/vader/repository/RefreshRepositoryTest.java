package com.deathstar.vader.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.deathstar.vader.domain.RefreshToken;
import com.deathstar.vader.domain.User;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;
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
class RefreshRepositoryTest {

    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private UserRepository userRepository;

    @Test
    void shouldFindRefreshTokenByTokenHash() {
        User user = new User("refresh@example.com");
        user = userRepository.save(user);

        UUID familyId = UUID.randomUUID();
        RefreshToken token = new RefreshToken(user, familyId, "hash123", ZonedDateTime.now().plusDays(1));
        refreshTokenRepository.save(token);

        Optional<RefreshToken> foundToken = refreshTokenRepository.findByTokenHash("hash123");
        assertThat(foundToken).isPresent();
        assertThat(foundToken.get().getFamilyId()).isEqualTo(familyId);
    }

    @Test
    void shouldRevokeAllTokensByFamilyId() {
        User user = new User("revoke@example.com");
        user = userRepository.save(user);

        UUID familyId = UUID.randomUUID();
        RefreshToken token1 = new RefreshToken(user, familyId, "hash1", ZonedDateTime.now().plusDays(1));
        RefreshToken token2 = new RefreshToken(user, familyId, "hash2", ZonedDateTime.now().plusDays(2));
        
        UUID otherFamilyId = UUID.randomUUID();
        RefreshToken otherToken = new RefreshToken(user, otherFamilyId, "hash3", ZonedDateTime.now().plusDays(1));

        refreshTokenRepository.save(token1);
        refreshTokenRepository.save(token2);
        refreshTokenRepository.save(otherToken);

        refreshTokenRepository.revokeByFamilyId(familyId);

        // Fetch tokens again to verify state
        assertThat(refreshTokenRepository.findByTokenHash("hash1").get().isRevoked()).isTrue();
        assertThat(refreshTokenRepository.findByTokenHash("hash2").get().isRevoked()).isTrue();
        // Ensure other token remains unaffected
        assertThat(refreshTokenRepository.findByTokenHash("hash3").get().isRevoked()).isFalse();
    }
}
