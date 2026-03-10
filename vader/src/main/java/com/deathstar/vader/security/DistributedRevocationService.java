package com.deathstar.vader.security;

import com.deathstar.vader.audit.AuditEvent;
import com.deathstar.vader.audit.schema.ActionStatus;
import com.deathstar.vader.audit.schema.CoreResource;
import com.deathstar.vader.audit.schema.UserAction;
import com.deathstar.vader.tracing.NatsTracingPropagator;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Map;
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

    private final Connection natsConnection;
    private final NatsTracingPropagator natsTracingPropagator;
    private final org.springframework.context.ApplicationEventPublisher applicationEventPublisher;

    private static final String REVOCATION_SUBJECT = "auth.revoked";

    // O(1) Local Blacklist: TTL must match JWT expiration to free memory automatically
    private final Cache<String, Boolean> revokedUsersCache =
            Caffeine.newBuilder()
                    .expireAfterWrite(15, TimeUnit.MINUTES)
                    .maximumSize(10_000)
                    .build();

    @PostConstruct
    public void init() {
        Dispatcher dispatcher =
                natsConnection.createDispatcher(
                        msg ->
                                natsTracingPropagator.processMessageWithTracing(
                                        msg,
                                        "receive_revocation_event",
                                        tracedMsg -> {
                                            String revokedUserId =
                                                    new String(
                                                            tracedMsg.getData(),
                                                            StandardCharsets.UTF_8);
                                            log.warn(
                                                    "[KILL SWITCH] Received revocation event for user: {}",
                                                    revokedUserId);
                                            revokedUsersCache.put(revokedUserId, true);
                                        }));
        dispatcher.subscribe(REVOCATION_SUBJECT);
    }

    /** Broadcasts a revocation event to all Vader instances in the K8s cluster. */
    public void broadcastRevocation(String userId) {
        log.warn("[KILL SWITCH] Broadcasting revocation for user: {}", userId);

        applicationEventPublisher.publishEvent(
                new AuditEvent<>(
                        this,
                        "SYSTEM",
                        UserAction.USER_REVOKED,
                        CoreResource.USER,
                        userId,
                        ActionStatus.SUCCESS,
                        Map.of()));

        natsConnection.publish(
                REVOCATION_SUBJECT,
                natsTracingPropagator.injectContext(),
                userId.getBytes(StandardCharsets.UTF_8));
    }

    /** O(1) RAM lookup to check if a user's JWT should be rejected immediately. */
    public boolean isRevokedLocally(String userId) {
        return revokedUsersCache.getIfPresent(userId) != null;
    }
}
