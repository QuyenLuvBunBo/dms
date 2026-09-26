package vn.edu.hust.dms.support;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import vn.edu.hust.dms.common.user.Role;
import vn.edu.hust.dms.common.user.UserAccount;
import vn.edu.hust.dms.common.user.UserAccountRepository;

import java.time.Clock;
import java.util.concurrent.atomic.AtomicInteger;

/** Creates login accounts for tests. The password hash is computed once because BCrypt is slow. */
@Component
public class TestUsers {

    public static final String PASSWORD = "test-password";

    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final AtomicInteger counter = new AtomicInteger();
    private volatile String cachedHash;

    public TestUsers(UserAccountRepository users, PasswordEncoder passwordEncoder, Clock clock) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    public record Credentials(Long id, String username, String password, Role role) {
    }

    public Credentials create(Role role) {
        return create(role, true);
    }

    public Credentials create(Role role, boolean enabled) {
        String username = role.name().toLowerCase() + "-" + counter.incrementAndGet();
        UserAccount account = new UserAccount(username, passwordHash(), "Test " + role.name(),
                username + "@test.example", role, clock.instant());
        account.setEnabled(enabled);
        account = users.save(account);
        return new Credentials(account.getId(), username, PASSWORD, role);
    }

    private String passwordHash() {
        String hash = cachedHash;
        if (hash == null) {
            hash = passwordEncoder.encode(PASSWORD);
            cachedHash = hash;
        }
        return hash;
    }
}
