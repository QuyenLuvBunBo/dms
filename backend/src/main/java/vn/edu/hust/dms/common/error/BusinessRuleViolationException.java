package vn.edu.hust.dms.common.error;

import org.springframework.http.HttpStatus;

import java.util.Map;

/** 422: a business rule (BR-xx in CLAUDE.md) rejects the request. */
public class BusinessRuleViolationException extends DomainException {

    private final String ruleId;

    public BusinessRuleViolationException(String ruleId, String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, message);
        this.ruleId = ruleId;
    }

    public String ruleId() {
        return ruleId;
    }

    @Override
    public Map<String, Object> properties() {
        return Map.of("rule", ruleId);
    }
}
