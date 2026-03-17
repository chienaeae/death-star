package com.deathstar.vader.loom.spi;

import com.deathstar.loom.core.spi.IdentityResolver;
import org.springframework.stereotype.Component;

/** Custom Identity Resolver securely parsing tenant contexts across virtual threads. */
@Component
public class ScopedValueIdentityResolver implements IdentityResolver {

    // Modern Java 23 way to pass contextual data over ThreadLocals for Virtual Threads
    public static final ScopedValue<String> TENANT_ID = ScopedValue.newInstance();
    public static final ScopedValue<String> USER_ID = ScopedValue.newInstance();

    @Override
    public String currentTenantId() {
        return TENANT_ID.isBound() ? TENANT_ID.get() : "system";
    }

    @Override
    public String currentUserId() {
        return USER_ID.isBound() ? USER_ID.get() : "system";
    }
}
