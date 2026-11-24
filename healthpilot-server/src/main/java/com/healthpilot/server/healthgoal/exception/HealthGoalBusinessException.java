
package com.healthpilot.server.healthgoal.exception;

public class HealthGoalBusinessException
        extends RuntimeException {

    public enum Reason {
        NOT_FOUND,
        INVALID_INPUT,
        VERSION_CONFLICT,
        INVALID_STATUS
    }

    private final Reason reason;

    public HealthGoalBusinessException(
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
