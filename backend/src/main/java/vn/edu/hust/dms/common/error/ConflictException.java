package vn.edu.hust.dms.common.error;

import org.springframework.http.HttpStatus;

/**
 * 409: the request conflicts with existing data, e.g. a duplicate code, a room that still has
 * occupants, or a room that already has as many beds as its capacity.
 */
public class ConflictException extends DomainException {

    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
