
package com.healthpilot.server.weeklyassessment;

import com.healthpilot.server.weeklyassessment.dto.*;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class WeeklyAssessmentDtoValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    // 正常创建请求应该通过校验
    @Test
    void shouldAcceptValidCreateRequest() {
        CreateWeeklyAssessmentRequest request =
                new CreateWeeklyAssessmentRequest(
                        LocalDate.of(2026, 11, 2),
                        validFeedback(),
                        validMetrics()
                );

        assertTrue(validator.validate(request).isEmpty());
    }

    // execution 必须属于规定的枚举值
    @Test
    void shouldRejectInvalidExecution() {
        WeeklyFeedbackInput feedback =
                new WeeklyFeedbackInput(
                        "unknown",
                        "better",
                        "normal",
                        null
                );

        CreateWeeklyAssessmentRequest request =
                new CreateWeeklyAssessmentRequest(
                        LocalDate.of(2026, 11, 2),
                        feedback,
                        validMetrics()
                );

        assertViolation(request, "feedback.execution");
    }

    // feedback 不能为空
    @Test
    void shouldRejectMissingFeedback() {
        CreateWeeklyAssessmentRequest request =
                new CreateWeeklyAssessmentRequest(
                        LocalDate.of(2026, 11, 2),
                        null,
                        validMetrics()
                );

        assertViolation(request, "feedback");
    }

    // sleepQuality 必须在 1～5 之间
    @Test
    void shouldRejectInvalidSleepQuality() {
        WeeklyHealthMetricsInput metrics =
                new WeeklyHealthMetricsInput(
                        new BigDecimal("72.50"),
                        (short) 420,
                        (byte) 6,
                        (byte) 4,
                        (byte) 2,
                        (byte) 3,
                        150
                );

        CreateWeeklyAssessmentRequest request =
                new CreateWeeklyAssessmentRequest(
                        LocalDate.of(2026, 11, 2),
                        validFeedback(),
                        metrics
                );

        assertViolation(
                request,
                "healthSnapshot.sleepQuality"
        );
    }

    // 平均睡眠分钟数不能超过一天
    @Test
    void shouldRejectExcessiveSleepMinutes() {
        WeeklyHealthMetricsInput metrics =
                new WeeklyHealthMetricsInput(
                        null,
                        (short) 1500,
                        null,
                        null,
                        null,
                        null,
                        null
                );

        CreateWeeklyAssessmentRequest request =
                new CreateWeeklyAssessmentRequest(
                        LocalDate.of(2026, 11, 2),
                        validFeedback(),
                        metrics
                );

        assertViolation(
                request,
                "healthSnapshot.avgSleepMinutes"
        );
    }

    // 健康指标允许不填写
    @Test
    void shouldAcceptMissingHealthMetrics() {
        CreateWeeklyAssessmentRequest request =
                new CreateWeeklyAssessmentRequest(
                        LocalDate.of(2026, 11, 2),
                        validFeedback(),
                        null
                );

        assertTrue(validator.validate(request).isEmpty());
    }

    // 部分更新只需要传入修改的字段
    @Test
    void shouldAcceptPartialUpdate() {
        UpdateWeeklyAssessmentRequest request =
                new UpdateWeeklyAssessmentRequest(
                        null,
                        null,
                        1L,
                        new WeeklyHealthMetricsInput(
                                new BigDecimal("72.30"),
                                null,
                                null,
                                null,
                                null,
                                null,
                                null
                        ),
                        null
                );

        assertTrue(validator.validate(request).isEmpty());
    }

    // clearFields 不允许未知字段
    @Test
    void shouldRejectInvalidClearField() {
        UpdateWeeklyAssessmentRequest request =
                new UpdateWeeklyAssessmentRequest(
                        1L,
                        null,
                        null,
                        null,
                        List.of("unknownField")
                );

        assertViolation(request, "clearFields");
    }

    private static WeeklyFeedbackInput validFeedback() {
        return new WeeklyFeedbackInput(
                "most",
                "better",
                "normal",
                "本周状态有所改善"
        );
    }

    private static WeeklyHealthMetricsInput validMetrics() {
        return new WeeklyHealthMetricsInput(
                new BigDecimal("72.50"),
                (short) 420,
                (byte) 4,
                (byte) 4,
                (byte) 2,
                (byte) 3,
                150
        );
    }

    // 检查指定字段是否产生校验错误
    private static <T> void assertViolation(
            T request,
            String fieldName
    ) {
        Set<ConstraintViolation<T>> violations =
                validator.validate(request);

        boolean found = violations.stream()
                .anyMatch(v -> v.getPropertyPath()
                        .toString()
                        .contains(fieldName));

        assertTrue(
                found,
                "预期字段产生校验错误：" + fieldName
                        + "，实际错误：" + violations
        );
    }
}
