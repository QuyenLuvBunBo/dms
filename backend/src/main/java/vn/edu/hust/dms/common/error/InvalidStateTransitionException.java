package vn.edu.hust.dms.common.error;

import org.springframework.http.HttpStatus;

/** 409: the aggregate is not in a state that allows the requested transition. */
public class InvalidStateTransitionException extends DomainException {

    public InvalidStateTransitionException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
