package vn.edu.hust.dms.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hust.dms.common.user.UserAccount;
import vn.edu.hust.dms.common.user.UserAccountRepository;

import java.time.Clock;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Seeds the demo data set. Kept profile-independent so it can be tested; the demo profile
 * runs it on startup through {@link DemoDataSeeder}. Later phases add buildings, rooms, students
 * and an admission round here.
 */
@Service
public class DemoDataService {

    private static final Logger log = LoggerFactory.getLogger(DemoDataService.class);

    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public DemoDataService(UserAccountRepository users, PasswordEncoder passwordEncoder, Clock clock) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    /** @return true when data was seeded, false when the database already had users */
    @Transactional
    public boolean seedIfEmpty() {
        long existing = users.count();
        if (existing > 0) {
            log.info("Demo data skipped: database already has {} user(s)", existing);
            return false;
        }
        String passwordHash = passwordEncoder.encode(DemoAccount.PASSWORD);
        for (DemoAccount account : DemoAccount.values()) {
            users.save(new UserAccount(account.username(), passwordHash, account.fullName(), account.email(),
                    account.role(), clock.instant()));
        }
        log.info("Seeded demo accounts {} with password '{}'",
                Arrays.stream(DemoAccount.values()).map(DemoAccount::username).collect(Collectors.joining(", ")),
                DemoAccount.PASSWORD);
        return true;
    }
}
