
package com.healthpilot.server.weeklyassessment.exception;

public class WeeklyAssessmentBusinessException extends RuntimeException {

    public enum Reason {
        NOT_FOUND,
        INVALID_INPUT,
        ALREADY_EXISTS,
        VERSION_CONFLICT
    }

    private final Reason reason;

    public WeeklyAssessmentBusinessException(
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
