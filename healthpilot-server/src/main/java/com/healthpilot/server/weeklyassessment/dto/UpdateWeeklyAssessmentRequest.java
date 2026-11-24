
package com.healthpilot.server.weeklyassessment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record UpdateWeeklyAssessmentRequest(

        // 修改主观反馈时需要提供
        @Min(0)
        Long feedbackVersion,

        @Valid
        UpdateWeeklyFeedbackInput feedback,

        // 修改健康指标时需要提供
        @Min(0)
        Long snapshotVersion,

        @Valid
        WeeklyHealthMetricsInput healthSnapshot,

        // 显式清空指定的可选字段
        List<
                @Pattern(regexp =
                        "note|weightKg|avgSleepMinutes|" +
                                "sleepQuality|moodScore|stressScore|" +
                                "dietQuality|activityMinutes")
                        String
                > clearFields

) {
}
