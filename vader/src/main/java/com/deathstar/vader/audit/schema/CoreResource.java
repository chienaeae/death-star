package com.deathstar.vader.audit.schema;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum CoreResource implements AuditResource<AuditAction> {
    USER(UserAction.class);

    private final Class<? extends AuditAction> actionClass;

    @Override
    @SuppressWarnings("unchecked")
    public Class<AuditAction> getActionClass() {
        return (Class<AuditAction>) (Class<?>) actionClass;
    }
}
