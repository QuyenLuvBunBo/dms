package vn.edu.hust.dms.common.audit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findBySubjectTypeAndSubjectIdOrderByCreatedAtAscIdAsc(AuditSubjectType subjectType, long subjectId);
}
