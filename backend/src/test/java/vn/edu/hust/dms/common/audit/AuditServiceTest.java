package vn.edu.hust.dms.common.audit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;
import vn.edu.hust.dms.common.security.UserPrincipal;
import vn.edu.hust.dms.common.user.Role;
import vn.edu.hust.dms.common.user.UserAccountRepository;
import vn.edu.hust.dms.support.AbstractIntegrationTest;
import vn.edu.hust.dms.support.TestClockConfig;
import vn.edu.hust.dms.support.TestUsers;
import vn.edu.hust.dms.support.TestUsers.Credentials;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuditServiceTest extends AbstractIntegrationTest {

    private enum SampleStatus { HELD, CONFIRMED }

    @Autowired
    private AuditService audit;

    @Autowired
    private AuditLogRepository auditLogs;

    @Autowired
    private TransactionTemplate transaction;

    @Autowired
    private TestUsers testUsers;

    @Autowired
    private UserAccountRepository users;

    @Test
    @DisplayName("AUDIT record() stores subject, from/to status, the current user id and the clock instant")
    void recordsTransitionWithUserAndTime() {
        Credentials admin = testUsers.create(Role.ADMIN);
        actAs(admin);

        AuditLog saved = transaction.execute(status ->
                audit.record(AuditSubjectType.REGISTRATION, 42L, "HELD", "CONFIRMED"));
        AuditLog viaEnum = transaction.execute(status ->
                audit.record(AuditSubjectType.REGISTRATION, 43L, SampleStatus.HELD, SampleStatus.CONFIRMED));

        AuditLog stored = auditLogs.findById(saved.getId()).orElseThrow();
        assertThat(stored.getSubjectType()).isEqualTo(AuditSubjectType.REGISTRATION);
        assertThat(stored.getSubjectId()).isEqualTo(42L);
        assertThat(stored.getFromStatus()).isEqualTo("HELD");
        assertThat(stored.getToStatus()).isEqualTo("CONFIRMED");
        assertThat(stored.getUserId()).isEqualTo(admin.id());
        assertThat(stored.getCreatedAt()).isEqualTo(TestClockConfig.INITIAL);
        assertThat(auditLogs.findById(viaEnum.getId()).orElseThrow())
                .extracting(AuditLog::getFromStatus, AuditLog::getToStatus)
                .containsExactly("HELD", "CONFIRMED");
    }

    @Test
    @DisplayName("AUDIT record() with no authenticated user stores a null user id")
    void systemActionHasNoUser() {
        AuditLog saved = transaction.execute(status ->
                audit.record(AuditSubjectType.ROOM_ASSET, 7L, (String) null, "GOOD"));

        AuditLog stored = auditLogs.findById(saved.getId()).orElseThrow();
        assertThat(stored.getUserId()).isNull();
        assertThat(stored.getFromStatus()).isNull();
        assertThat(stored.getToStatus()).isEqualTo("GOOD");
    }

    @Test
    @DisplayName("AUDIT record() outside a transaction fails with IllegalTransactionStateException (Propagation.MANDATORY)")
    void requiresCallerTransaction() {
        assertThatThrownBy(() -> audit.record(AuditSubjectType.RESIDENCE, 1L, "PENDING_CHECK_IN", "ACTIVE"))
                .isInstanceOf(IllegalTransactionStateException.class);
        assertThat(auditLogs.count()).isZero();
    }

    @Test
    @DisplayName("AUDIT advancing the MutableClock by 3 days changes created_at of the next row")
    void createdAtFollowsTheTestClock() {
        AuditLog first = transaction.execute(status ->
                audit.record(AuditSubjectType.RESIDENCE, 1L, "PENDING_CHECK_IN", "ACTIVE"));

        clock.advanceDays(3);
        AuditLog second = transaction.execute(status ->
                audit.record(AuditSubjectType.RESIDENCE, 1L, "ACTIVE", "ENDED"));

        assertThat(first.getCreatedAt()).isEqualTo(TestClockConfig.INITIAL);
        assertThat(second.getCreatedAt()).isEqualTo(TestClockConfig.INITIAL.plus(Duration.ofDays(3)));
        assertThat(auditLogs.findBySubjectTypeAndSubjectIdOrderByCreatedAtAscIdAsc(AuditSubjectType.RESIDENCE, 1L))
                .extracting(AuditLog::getToStatus)
                .containsExactly("ACTIVE", "ENDED");
    }

    private void actAs(Credentials credentials) {
        UserPrincipal principal = UserPrincipal.from(users.findById(credentials.id()).orElseThrow());
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
    }
}
