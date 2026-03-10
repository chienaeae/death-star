package com.deathstar.vader.auth.repository;

import com.deathstar.vader.auth.RefreshToken;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    // The Guillotine: Atomically revoke all tokens belonging to a compromised session chain
    @Modifying(clearAutomatically = true)
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.familyId = :familyId")
    void revokeByFamilyId(@Param("familyId") UUID familyId);
}
