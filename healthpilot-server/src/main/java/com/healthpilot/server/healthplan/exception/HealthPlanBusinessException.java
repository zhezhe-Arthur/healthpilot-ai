
package com.healthpilot.server.healthplan.exception;

public class HealthPlanBusinessException extends RuntimeException {

    public enum Reason {
        NOT_FOUND,
        INVALID_INPUT,
        VERSION_CONFLICT,
        PLAN_CONFIRMED
    }

    private final Reason reason;

    public HealthPlanBusinessException(
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
