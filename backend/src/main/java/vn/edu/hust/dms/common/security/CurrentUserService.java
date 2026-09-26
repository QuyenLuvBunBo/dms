package vn.edu.hust.dms.common.security;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.stereotype.Service;

import java.util.Optional;

/** Resolves the user behind the current request, for audit rows and ownership checks. */
@Service
public class CurrentUserService {

    private final SecurityContextHolderStrategy holderStrategy = SecurityContextHolder.getContextHolderStrategy();

    public Optional<UserPrincipal> current() {
        Authentication authentication = holderStrategy.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return Optional.empty();
        }
        return Optional.of(principal);
    }

    public Optional<Long> currentUserId() {
        return current().map(UserPrincipal::id);
    }

    public UserPrincipal require() {
        return current().orElseThrow(() -> new AuthenticationCredentialsNotFoundException("No authenticated user"));
    }
}
