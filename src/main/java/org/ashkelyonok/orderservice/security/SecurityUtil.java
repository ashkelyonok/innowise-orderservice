package org.ashkelyonok.orderservice.security;

import lombok.RequiredArgsConstructor;
import org.ashkelyonok.orderservice.exception.InvalidOrderOperationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class SecurityUtil {

    private final JwtUtil jwtUtil;

    /**
     * Extracts the User ID from the JWT stored in the SecurityContext.
     */
    public Long getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new InvalidOrderOperationException("Unauthenticated access attempt");
        }

        Object credentials = authentication.getCredentials();

        if (!(credentials instanceof String token)) {
            throw new InvalidOrderOperationException("Invalid security context: missing token");
        }

        return jwtUtil.extractUserId(token);
    }

    /**
     * Enforces that the current user owns the resource, OR is an Admin.
     * @param resourceOwnerId The ID of the user who owns the data.
     */
    public void checkOwnership(Long resourceOwnerId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_ADMIN"));

        if (isAdmin) {
            return;
        }

        Long currentUserId = getAuthenticatedUserId();

        if (!Objects.equals(currentUserId, resourceOwnerId)) {
            throw new AccessDeniedException("Access Denied: You do not own this resource.");
        }
    }
}
