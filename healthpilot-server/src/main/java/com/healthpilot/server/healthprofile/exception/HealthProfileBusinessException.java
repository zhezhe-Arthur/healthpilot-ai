
package com.healthpilot.server.healthprofile.exception;

public class HealthProfileBusinessException extends RuntimeException {

    public enum Reason {
        ALREADY_EXISTS,
        NOT_FOUND,
        INVALID_INPUT,
        VERSION_CONFLICT
    }

    private final Reason reason;

    public HealthProfileBusinessException(
            Reason reason,
            String message
    ) {
        super(message);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
