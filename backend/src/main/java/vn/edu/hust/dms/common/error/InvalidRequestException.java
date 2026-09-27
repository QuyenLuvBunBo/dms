package vn.edu.hust.dms.common.error;

import org.springframework.http.HttpStatus;

/**
 * 422: a well-formed request that makes no sense for the data it refers to, e.g. a room type of
 * another building. Business rules (BR-xx) use {@link BusinessRuleViolationException} instead.
 */
public class InvalidRequestException extends DomainException {

    public InvalidRequestException(String message) {
        super(HttpStatus.UNPROCESSABLE_CONTENT, message);
    }
}
