package com.deathstar.vader.audit;

import com.deathstar.vader.audit.schema.AuditAction;
import com.deathstar.vader.audit.schema.AuditResource;
import com.deathstar.vader.audit.schema.AuditStatus;
import java.util.Map;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class AuditEvent<T extends AuditAction> extends ApplicationEvent {

    private final String actorId;
    private final T action;
    private final AuditResource<T> resourceType;
    private final String resourceId;
    private final AuditStatus status;
    private final Map<String, String> metadata;

    public AuditEvent(
            Object source,
            String actorId,
            T action,
            AuditResource<T> resourceType,
            String resourceId,
            AuditStatus status,
            Map<String, String> metadata) {
        super(source);
        this.actorId = actorId;
        this.action = action;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.status = status;
        this.metadata = metadata;
    }
}
