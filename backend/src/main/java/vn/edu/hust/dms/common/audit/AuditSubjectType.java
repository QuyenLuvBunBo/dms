package vn.edu.hust.dms.common.audit;

/** The aggregates whose state transitions are audited (see the domain model in CLAUDE.md). */
public enum AuditSubjectType {
    REGISTRATION,
    RESIDENCE,
    INVOICE,
    REPAIR_TICKET,
    WARNING_REVIEW,
    ROOM_ASSET,
    USER
}
