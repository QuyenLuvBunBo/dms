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

    private enum ApplicationStatus { DRAFT, SUBMITTED }

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
                audit.record(AuditSubjectType.APPLICATION, 42L, "SUBMITTED", "APPROVED"));
        AuditLog viaEnum = transaction.execute(status ->
                audit.record(AuditSubjectType.APPLICATION, 43L, ApplicationStatus.DRAFT, ApplicationStatus.SUBMITTED));

        AuditLog stored = auditLogs.findById(saved.getId()).orElseThrow();
        assertThat(stored.getSubjectType()).isEqualTo(AuditSubjectType.APPLICATION);
        assertThat(stored.getSubjectId()).isEqualTo(42L);
        assertThat(stored.getFromStatus()).isEqualTo("SUBMITTED");
        assertThat(stored.getToStatus()).isEqualTo("APPROVED");
        assertThat(stored.getUserId()).isEqualTo(admin.id());
        assertThat(stored.getCreatedAt()).isEqualTo(TestClockConfig.INITIAL);
        assertThat(auditLogs.findById(viaEnum.getId()).orElseThrow())
                .extracting(AuditLog::getFromStatus, AuditLog::getToStatus)
                .containsExactly("DRAFT", "SUBMITTED");
    }

    @Test
    @DisplayName("AUDIT record() with no authenticated user stores a null user id")
    void systemActionHasNoUser() {
        AuditLog saved = transaction.execute(status ->
                audit.record(AuditSubjectType.BED_OFFER, 7L, (String) null, "EXPIRED"));

        AuditLog stored = auditLogs.findById(saved.getId()).orElseThrow();
        assertThat(stored.getUserId()).isNull();
        assertThat(stored.getFromStatus()).isNull();
        assertThat(stored.getToStatus()).isEqualTo("EXPIRED");
    }

    @Test
    @DisplayName("AUDIT record() outside a transaction fails with IllegalTransactionStateException (Propagation.MANDATORY)")
    void requiresCallerTransaction() {
        assertThatThrownBy(() -> audit.record(AuditSubjectType.CONTRACT, 1L, "PENDING", "ACTIVE"))
                .isInstanceOf(IllegalTransactionStateException.class);
        assertThat(auditLogs.count()).isZero();
    }

    @Test
    @DisplayName("AUDIT advancing the MutableClock by 3 days changes created_at of the next row")
    void createdAtFollowsTheTestClock() {
        AuditLog first = transaction.execute(status ->
                audit.record(AuditSubjectType.CONTRACT, 1L, "PENDING", "ACTIVE"));

        clock.advanceDays(3);
        AuditLog second = transaction.execute(status ->
                audit.record(AuditSubjectType.CONTRACT, 1L, "ACTIVE", "TERMINATED"));

        assertThat(first.getCreatedAt()).isEqualTo(TestClockConfig.INITIAL);
        assertThat(second.getCreatedAt()).isEqualTo(TestClockConfig.INITIAL.plus(Duration.ofDays(3)));
        assertThat(auditLogs.findBySubjectTypeAndSubjectIdOrderByCreatedAtAscIdAsc(AuditSubjectType.CONTRACT, 1L))
                .extracting(AuditLog::getToStatus)
                .containsExactly("ACTIVE", "TERMINATED");
    }

    private void actAs(Credentials credentials) {
        UserPrincipal principal = UserPrincipal.from(users.findById(credentials.id()).orElseThrow());
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
    }
}
