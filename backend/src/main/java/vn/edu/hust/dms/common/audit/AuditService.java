package vn.edu.hust.dms.common.audit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hust.dms.common.security.CurrentUserService;

import java.time.Clock;

/**
 * Writes one audit row per state transition. Propagation is MANDATORY on purpose: a transition
 * must call this inside its own transaction so the audit row and the change commit or roll back together.
 */
@Service
public class AuditService {

    private final AuditLogRepository auditLogs;
    private final CurrentUserService currentUser;
    private final Clock clock;

    public AuditService(AuditLogRepository auditLogs, CurrentUserService currentUser, Clock clock) {
        this.auditLogs = auditLogs;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public AuditLog record(AuditSubjectType subjectType, long subjectId, String fromStatus, String toStatus) {
        AuditLog entry = new AuditLog(subjectType, subjectId, fromStatus, toStatus,
                currentUser.currentUserId().orElse(null), clock.instant());
        return auditLogs.save(entry);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public <E extends Enum<E>> AuditLog record(AuditSubjectType subjectType, long subjectId, E fromStatus, E toStatus) {
        return record(subjectType, subjectId, fromStatus == null ? null : fromStatus.name(), toStatus.name());
    }
}
