package vn.edu.hust.dms.common.error;

import org.springframework.http.HttpStatus;

/** 404: the aggregate does not exist or is not visible to the caller. */
public class NotFoundException extends DomainException {

    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
