package com.deathstar.vader.core.security;

import com.deathstar.vader.auth.*;
import com.deathstar.vader.auth.service.DistributedRevocationService;
import com.deathstar.vader.loom.infrastructure.ScopedValueIdentityResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * The First Line of Defense. Validates the JWT strictly in memory. Queries DB ONLY if strictly
 * necessary (never for AT).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final DistributedRevocationService revocationService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt)) {
                // 1. Cryptographic validation (O(1) CPU Time)
                String userId = jwtProvider.validateAndGetUserId(jwt);

                // 2. The Kill Switch Check (O(1) RAM Lookup, No DB I/O)
                if (revocationService.isRevokedLocally(userId)) {
                    log.warn("Intercepted revoked user attempting access: {}", userId);
                    response.sendError(
                            HttpServletResponse.SC_UNAUTHORIZED, "Token revoked by Kill Switch");
                    return;
                }

                // 3. Assemble Authentication context
                // In a highly optimized system, we trust the JWT claims (e.g., roles) to avoid DB
                // trips.
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userId, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));

                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            log.error("Could not set user authentication in security context", ex);
        }

        // If authenticated, wrap the remainder of the request in the ScopedValue
        org.springframework.security.core.Authentication auth =
                SecurityContextHolder.getContext().getAuthentication();
        if (auth != null
                && auth.isAuthenticated()
                && !(auth
                        instanceof
                        org.springframework.security.authentication.AnonymousAuthenticationToken)) {
            String authenticatedUserId = auth.getName();
            try {
                java.lang.ScopedValue.where(
                                ScopedValueIdentityResolver.USER_ID, authenticatedUserId)
                        .where(ScopedValueIdentityResolver.TENANT_ID, authenticatedUserId)
                        .run(
                                () -> {
                                    try {
                                        filterChain.doFilter(request, response);
                                    } catch (Exception e) {
                                        throw new RuntimeException(e);
                                    }
                                });
            } catch (RuntimeException e) {
                if (e.getCause() instanceof ServletException) {
                    throw (ServletException) e.getCause();
                } else if (e.getCause() instanceof IOException) {
                    throw (IOException) e.getCause();
                }
                throw e;
            }
        } else {
            filterChain.doFilter(request, response);
        }
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
