package com.pms.hotel.shared.exception;

import java.util.Map;

/**
 * Raised when a request is well-formed but violates a domain rule
 * (e.g. overbooking, invalid status transition). Mapped to HTTP 422.
 */
public class BusinessRuleException extends RuntimeException {

    private final Map<String, Object> errors;

    public BusinessRuleException(String message) {
        this(message, Map.of());
    }

    public BusinessRuleException(String message, Map<String, Object> errors) {
        super(message);
        this.errors = errors;
    }

    public Map<String, Object> getErrors() {
        return errors;
    }
}
