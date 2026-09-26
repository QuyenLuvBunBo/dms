package vn.edu.hust.dms.common.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import vn.edu.hust.dms.common.user.Role;
import vn.edu.hust.dms.common.user.UserAccount;

import java.util.Collection;
import java.util.List;

/** The authenticated user as stored in the session: an immutable snapshot of a {@link UserAccount}. */
public final class UserPrincipal implements UserDetails {

    private final Long id;
    private final String username;
    private final String passwordHash;
    private final String fullName;
    private final Role role;
    private final boolean enabled;

    private UserPrincipal(Long id, String username, String passwordHash, String fullName, Role role, boolean enabled) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.role = role;
        this.enabled = enabled;
    }

    public static UserPrincipal from(UserAccount account) {
        return new UserPrincipal(account.getId(), account.getUsername(), account.getPasswordHash(),
                account.getFullName(), account.getRole(), account.isEnabled());
    }

    public Long id() {
        return id;
    }

    public String fullName() {
        return fullName;
    }

    public Role role() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.authority()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String toString() {
        return "UserPrincipal[id=" + id + ", username=" + username + ", role=" + role + "]";
    }
}
