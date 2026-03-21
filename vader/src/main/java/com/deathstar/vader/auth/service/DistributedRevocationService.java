package com.deathstar.vader.auth.service;

import com.deathstar.vader.audit.AuditEvent;
import com.deathstar.vader.audit.AuditEventFactory;
import com.deathstar.vader.audit.schema.ActionStatus;
import com.deathstar.vader.audit.schema.CoreResource;
import com.deathstar.vader.audit.schema.UserAction;
import com.deathstar.vader.event.domain.DomainEvent;
import com.deathstar.vader.event.domain.EventRoute;
import com.deathstar.vader.event.spi.EventPublisher;
import com.deathstar.vader.event.spi.EventSubscriber;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * The "Kill Switch" implementation. Listens to NATS for revoked users and caches their IDs locally
 * via Caffeine. The cache TTL strictly matches the Access Token lifespan (15 minutes).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DistributedRevocationService {

    private final EventPublisher eventPublisher;
    private final EventSubscriber eventSubscriber;
    private final AuditEventFactory auditEventFactory;

    // O(1) Local Blacklist: TTL must match JWT expiration to free memory automatically
    private final Cache<String, Boolean> revokedUsersCache =
            Caffeine.newBuilder()
                    .expireAfterWrite(15, TimeUnit.MINUTES)
                    .maximumSize(10_000)
                    .build();

    @PostConstruct
    public void init() {
        eventSubscriber.subscribe(
                EventRoute.AUTH,
                "revoked",
                "distributed-revocation-service",
                eventMessage -> {
                    String revokedUserId = (String) eventMessage.domainEvent().payload();
                    log.warn("[KILL SWITCH] Received revocation event for user: {}", revokedUserId);
                    revokedUsersCache.put(revokedUserId, true);
                });
    }

    /** Broadcasts a revocation event to all Vader instances in the K8s cluster. */
    public void broadcastRevocation(String userId) {
        log.warn("[KILL SWITCH] Broadcasting revocation for user: {}", userId);

        eventPublisher.publish(
                EventRoute.AUDIT,
                "auth.revocation",
                auditEventFactory.createPayload(
                        new AuditEvent<>(
                                this,
                                "SYSTEM",
                                UserAction.USER_REVOKED,
                                CoreResource.USER,
                                userId,
                                ActionStatus.SUCCESS,
                                Map.of())));

        eventPublisher.publish(
                EventRoute.AUTH,
                "revoked",
                new DomainEvent(UUID.randomUUID(), "USER_REVOKED", Instant.now(), userId, userId));
    }

    /** O(1) RAM lookup to check if a user's JWT should be rejected immediately. */
    public boolean isRevokedLocally(String userId) {
        return revokedUsersCache.getIfPresent(userId) != null;
    }
}
