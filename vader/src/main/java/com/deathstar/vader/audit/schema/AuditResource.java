package com.deathstar.vader.audit.schema;

public interface AuditResource<T extends AuditAction> {
    String name();

    Class<T> getActionClass();
}
