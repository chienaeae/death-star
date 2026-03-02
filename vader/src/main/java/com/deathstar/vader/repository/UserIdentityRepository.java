package com.deathstar.vader.repository;

import com.deathstar.vader.domain.User;
import com.deathstar.vader.domain.UserIdentity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserIdentityRepository extends JpaRepository<UserIdentity, UUID> {
    Optional<UserIdentity> findByUserAndProvider(User user, String provider);
}
