package vn.edu.hust.dms.common.settings;

import org.springframework.http.HttpStatus;
import vn.edu.hust.dms.common.error.DomainException;

/** 422: the submitted value does not fit the setting's type. */
public class InvalidSettingValueException extends DomainException {

    public InvalidSettingValueException(SettingKey key, String value) {
        super(HttpStatus.UNPROCESSABLE_CONTENT,
                "Setting " + key.key() + " expects " + key.type().description() + ", got '" + value + "'");
    }
}
