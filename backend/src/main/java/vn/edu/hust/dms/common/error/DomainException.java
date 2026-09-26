package vn.edu.hust.dms.common.error;

import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * Base of every business error. Subclasses fix the HTTP status the API advice maps them to:
 * 409 for invalid state transitions, 422 for business rule violations, 404 for missing aggregates.
 */
public abstract class DomainException extends RuntimeException {

    private final HttpStatus status;

    protected DomainException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }

    /** Extra members added to the ProblemDetail body. */
    public Map<String, Object> properties() {
        return Map.of();
    }
}
