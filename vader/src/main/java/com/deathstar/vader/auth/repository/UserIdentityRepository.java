package com.deathstar.vader.auth.repository;

import com.deathstar.vader.auth.User;
import com.deathstar.vader.auth.UserIdentity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserIdentityRepository extends JpaRepository<UserIdentity, UUID> {
    Optional<UserIdentity> findByUserAndProvider(User user, String provider);
}
