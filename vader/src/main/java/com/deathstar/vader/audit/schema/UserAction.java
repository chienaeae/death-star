package com.deathstar.vader.audit.schema;

public enum UserAction implements AuditAction {
    USER_REGISTER,
    USER_LOGIN,
    USER_LOGOUT,
    USER_REVOKED
}
