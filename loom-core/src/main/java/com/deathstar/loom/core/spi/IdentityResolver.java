package com.deathstar.loom.core.spi;

/**
 * SPI for resolving the current security context / tenant isolation securely across virtual
 * threads. Typically implemented using Java 23 Scoped Values.
 */
public interface IdentityResolver {

    /** Resolves the current Tenant ID. */
    String currentTenantId();

    /** Resolves the current executing subject/user. */
    String currentUserId();
}
