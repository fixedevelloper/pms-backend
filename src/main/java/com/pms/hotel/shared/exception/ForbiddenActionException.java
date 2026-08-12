package com.pms.hotel.shared.exception;

/** Raised for domain-level authorization refusals distinct from Spring Security's 403. */
public class ForbiddenActionException extends RuntimeException {

    public ForbiddenActionException(String message) {
        super(message);
    }
}
