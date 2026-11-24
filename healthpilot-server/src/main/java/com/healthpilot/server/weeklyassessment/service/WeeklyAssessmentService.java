
package com.healthpilot.server.weeklyassessment.service;

import com.healthpilot.server.healthprofile.entity.HealthProfileEntity;
import com.healthpilot.server.healthprofile.repository.HealthProfileRepository;
import com.healthpilot.server.weeklyassessment.dto.*;
import com.healthpilot.server.weeklyassessment.entity.HealthProfileSnapshotEntity;
import com.healthpilot.server.weeklyassessment.entity.WeeklyFeedbackEntity;
import com.healthpilot.server.weeklyassessment.exception.WeeklyAssessmentBusinessException;
import com.healthpilot.server.weeklyassessment.repository.HealthProfileSnapshotRepository;
import com.healthpilot.server.weeklyassessment.repository.WeeklyFeedbackRepository;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.healthpilot.server.weeklyassessment.exception
        .WeeklyAssessmentBusinessException.Reason.*;

@Service
public class WeeklyAssessmentService {

    private static final Set<String> CLEARABLE_FIELDS = Set.of(
            "note",
            "weightKg",
            "avgSleepMinutes",
            "sleepQuality",
            "moodScore",
            "stressScore",
            "dietQuality",
            "activityMinutes"
    );

    private final WeeklyFeedbackRepository feedbackRepository;
    private final HealthProfileSnapshotRepository snapshotRepository;
    private final HealthProfileRepository profileRepository;
    private final EntityManager entityManager;

    public WeeklyAssessmentService(
            WeeklyFeedbackRepository feedbackRepository,
            HealthProfileSnapshotRepository snapshotRepository,
            HealthProfileRepository profileRepository,
            EntityManager entityManager
    ) {
        this.feedbackRepository = feedbackRepository;
        this.snapshotRepository = snapshotRepository;
        this.profileRepository = profileRepository;
        this.entityManager = entityManager;
    }

    // 创建每周评估，两张表在同一个事务中写入
    @Transactional
    public WeeklyAssessmentResponse create(
            Long userId,
            CreateWeeklyAssessmentRequest request
    ) {
        HealthProfileEntity profile = getProfile(userId);
        validateCompletedWeek(request.weekStartDate(), profile);

        LocalDate week = request.weekStartDate();

        if (feedbackRepository.existsByUserIdAndWeekStartDate(userId, week)
                || snapshotRepository.existsByUserIdAndWeekStartDate(userId, week)) {
            throw error(ALREADY_EXISTS, "该周健康评估已存在");
        }

        WeeklyFeedbackInput input = request.feedback();

        WeeklyFeedbackEntity feedback = new WeeklyFeedbackEntity(
                userId,
                week,
                input.execution(),
                input.sleep(),
                input.feeling(),
                normalizeNote(input.note())
        );

        HealthProfileSnapshotEntity snapshot =
                new HealthProfileSnapshotEntity(userId, week);

        copyProfile(profile, snapshot);

        if (request.healthSnapshot() != null) {
            applyMetrics(snapshot, request.healthSnapshot());
        }

        feedbackRepository.saveAndFlush(feedback);
        snapshotRepository.saveAndFlush(snapshot);

        entityManager.refresh(feedback);
        entityManager.refresh(snapshot);

        return WeeklyAssessmentResponse.from(feedback, snapshot);
    }

    // 查询指定周的评估
    @Transactional(readOnly = true)
    public WeeklyAssessmentResponse get(
            Long userId,
            LocalDate weekStartDate
    ) {
        validateMonday(weekStartDate);

        WeeklyFeedbackEntity feedback =
                findFeedback(userId, weekStartDate);

        HealthProfileSnapshotEntity snapshot =
                findSnapshot(userId, weekStartDate);

        return WeeklyAssessmentResponse.from(feedback, snapshot);
    }

    // 查询历史评估
    @Transactional(readOnly = true)
    public List<WeeklyAssessmentResponse> list(
            Long userId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        if (startDate == null || endDate == null
                || startDate.isAfter(endDate)
                || ChronoUnit.DAYS.between(startDate, endDate) > 366) {
            throw error(INVALID_INPUT, "日期范围不合法，最多查询366天");
        }

        List<WeeklyFeedbackEntity> feedbacks =
                feedbackRepository
                        .findByUserIdAndWeekStartDateBetweenOrderByWeekStartDateDesc(
                                userId, startDate, endDate
                        );

        List<HealthProfileSnapshotEntity> snapshots =
                snapshotRepository
                        .findByUserIdAndWeekStartDateBetweenOrderByWeekStartDateDesc(
                                userId, startDate, endDate
                        );

        Map<LocalDate, HealthProfileSnapshotEntity> snapshotMap =
                snapshots.stream().collect(Collectors.toMap(
                        HealthProfileSnapshotEntity::getWeekStartDate,
                        Function.identity()
                ));

        return feedbacks.stream().map(feedback -> {
            HealthProfileSnapshotEntity snapshot =
                    snapshotMap.get(feedback.getWeekStartDate());

            if (snapshot == null) {
                throw new IllegalStateException(
                        "每周评估数据不完整，请检查数据库"
                );
            }

            return WeeklyAssessmentResponse.from(feedback, snapshot);
        }).toList();
    }

    // 修改每周评估，分别检查反馈和快照版本号
    @Transactional
    public WeeklyAssessmentResponse update(
            Long userId,
            LocalDate weekStartDate,
            UpdateWeeklyAssessmentRequest request
    ) {
        validateMonday(weekStartDate);

        WeeklyFeedbackEntity feedback =
                findFeedback(userId, weekStartDate);

        HealthProfileSnapshotEntity snapshot =
                findSnapshot(userId, weekStartDate);

        Set<String> clearFields = validateClearFields(request);

        UpdateWeeklyFeedbackInput feedbackInput = request.feedback();
        WeeklyHealthMetricsInput metricsInput = request.healthSnapshot();

        boolean changeFeedback =
                hasFeedbackValues(feedbackInput)
                        || clearFields.contains("note");

        boolean changeSnapshot =
                hasMetricValues(metricsInput)
                        || clearFields.stream().anyMatch(
                        field -> !field.equals("note")
                );

        if (!changeFeedback && !changeSnapshot) {
            throw error(INVALID_INPUT, "至少需要修改一个字段");
        }

        if (changeFeedback) {
            requireVersion(
                    feedback.getVersion(),
                    request.feedbackVersion(),
                    "每周反馈"
            );
        }

        if (changeSnapshot) {
            requireVersion(
                    snapshot.getVersion(),
                    request.snapshotVersion(),
                    "健康画像快照"
            );
        }

        if (changeFeedback) {
            applyFeedback(feedback, feedbackInput);

            if (clearFields.contains("note")) {
                feedback.setNote(null);
            }

            feedbackRepository.saveAndFlush(feedback);
        }

        if (changeSnapshot) {
            if (metricsInput != null) {
                applyMetrics(snapshot, metricsInput);
            }

            clearMetrics(snapshot, clearFields);
            snapshotRepository.saveAndFlush(snapshot);
        }

        entityManager.flush();

        if (changeFeedback) {
            entityManager.refresh(feedback);
        }

        if (changeSnapshot) {
            entityManager.refresh(snapshot);
        }

        return WeeklyAssessmentResponse.from(feedback, snapshot);
    }

    private HealthProfileEntity getProfile(Long userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> error(
                        NOT_FOUND,
                        "请先创建基础健康画像"
                ));
    }

    private WeeklyFeedbackEntity findFeedback(
            Long userId,
            LocalDate weekStartDate
    ) {
        return feedbackRepository
                .findByUserIdAndWeekStartDate(userId, weekStartDate)
                .orElseThrow(() -> error(
                        NOT_FOUND,
                        "该周健康评估不存在"
                ));
    }

    private HealthProfileSnapshotEntity findSnapshot(
            Long userId,
            LocalDate weekStartDate
    ) {
        return snapshotRepository
                .findByUserIdAndWeekStartDate(userId, weekStartDate)
                .orElseThrow(() -> error(
                        NOT_FOUND,
                        "该周健康画像快照不存在"
                ));
    }

    private void validateMonday(LocalDate weekStartDate) {
        if (weekStartDate == null
                || weekStartDate.getDayOfWeek() != DayOfWeek.MONDAY) {
            throw error(INVALID_INPUT, "周开始日期必须是星期一");
        }
    }

    private void validateCompletedWeek(
            LocalDate weekStartDate,
            HealthProfileEntity profile
    ) {
        validateMonday(weekStartDate);

        String timezone = profile.getTimezone();

        ZoneId zone = timezone == null || timezone.isBlank()
                ? ZoneId.systemDefault()
                : ZoneId.of(timezone);

        LocalDate today = LocalDate.now(zone);

        if (!weekStartDate.plusDays(6).isBefore(today)) {
            throw error(
                    INVALID_INPUT,
                    "只能为已经结束的自然周提交正式评估"
            );
        }
    }

    // 从当前健康画像复制基础字段，保留当时的状态
    private void copyProfile(
            HealthProfileEntity profile,
            HealthProfileSnapshotEntity snapshot
    ) {
        if (profile.getBirthYear() != null) {
            snapshot.setBirthYear(
                    Short.valueOf(profile.getBirthYear().toString())
            );
        }

        snapshot.setHeightCm(profile.getHeightCm());

        if (profile.getActivityLevel() != null) {
            snapshot.setActivityLevel(
                    profile.getActivityLevel().toString()
            );
        }

        snapshot.setSleepSchedule(profile.getSleepSchedule());
        snapshot.setHealthNotes(profile.getHealthNotes());
        snapshot.setTimezone(profile.getTimezone());
    }

    private void applyMetrics(
            HealthProfileSnapshotEntity snapshot,
            WeeklyHealthMetricsInput input
    ) {
        if (input.weightKg() != null) {
            snapshot.setWeightKg(input.weightKg());
        }
        if (input.avgSleepMinutes() != null) {
            snapshot.setAvgSleepMinutes(input.avgSleepMinutes());
        }
        if (input.sleepQuality() != null) {
            snapshot.setSleepQuality(input.sleepQuality());
        }
        if (input.moodScore() != null) {
            snapshot.setMoodScore(input.moodScore());
        }
        if (input.stressScore() != null) {
            snapshot.setStressScore(input.stressScore());
        }
        if (input.dietQuality() != null) {
            snapshot.setDietQuality(input.dietQuality());
        }
        if (input.activityMinutes() != null) {
            snapshot.setActivityMinutes(input.activityMinutes());
        }
    }

    private void applyFeedback(
            WeeklyFeedbackEntity feedback,
            UpdateWeeklyFeedbackInput input
    ) {
        if (input == null) {
            return;
        }

        if (input.execution() != null) {
            feedback.setExecution(input.execution());
        }
        if (input.sleep() != null) {
            feedback.setSleep(input.sleep());
        }
        if (input.feeling() != null) {
            feedback.setFeeling(input.feeling());
        }
        if (input.note() != null) {
            if (input.note().isBlank()) {
                throw error(
                        INVALID_INPUT,
                        "清空备注请使用 clearFields"
                );
            }
            feedback.setNote(input.note());
        }
    }

    private Set<String> validateClearFields(
            UpdateWeeklyAssessmentRequest request
    ) {
        Set<String> result = new HashSet<>();

        if (request.clearFields() == null) {
            return result;
        }

        for (String field : request.clearFields()) {
            if (!CLEARABLE_FIELDS.contains(field)
                    || !result.add(field)) {
                throw error(INVALID_INPUT, "clearFields 存在无效或重复字段");
            }

            if ("note".equals(field)) {
                if (request.feedback() != null
                        && request.feedback().note() != null) {
                    throw error(INVALID_INPUT, "同一字段不能同时更新和清空");
                }
            } else if (metricFieldPresent(
                    request.healthSnapshot(), field)) {
                throw error(INVALID_INPUT, "同一字段不能同时更新和清空");
            }
        }

        return result;
    }

    private boolean metricFieldPresent(
            WeeklyHealthMetricsInput input,
            String field
    ) {
        if (input == null) {
            return false;
        }

        return switch (field) {
            case "weightKg" -> input.weightKg() != null;
            case "avgSleepMinutes" -> input.avgSleepMinutes() != null;
            case "sleepQuality" -> input.sleepQuality() != null;
            case "moodScore" -> input.moodScore() != null;
            case "stressScore" -> input.stressScore() != null;
            case "dietQuality" -> input.dietQuality() != null;
            case "activityMinutes" -> input.activityMinutes() != null;
            default -> false;
        };
    }

    private void clearMetrics(
            HealthProfileSnapshotEntity snapshot,
            Set<String> clearFields
    ) {
        for (String field : clearFields) {
            switch (field) {
                case "weightKg" -> snapshot.setWeightKg(null);
                case "avgSleepMinutes" -> snapshot.setAvgSleepMinutes(null);
                case "sleepQuality" -> snapshot.setSleepQuality(null);
                case "moodScore" -> snapshot.setMoodScore(null);
                case "stressScore" -> snapshot.setStressScore(null);
                case "dietQuality" -> snapshot.setDietQuality(null);
                case "activityMinutes" -> snapshot.setActivityMinutes(null);
                default -> {
                    // note 属于反馈表，不在这里处理
                }
            }
        }
    }

    private boolean hasFeedbackValues(UpdateWeeklyFeedbackInput input) {
        return input != null && (
                input.execution() != null
                        || input.sleep() != null
                        || input.feeling() != null
                        || input.note() != null
        );
    }

    private boolean hasMetricValues(WeeklyHealthMetricsInput input) {
        return input != null && (
                input.weightKg() != null
                        || input.avgSleepMinutes() != null
                        || input.sleepQuality() != null
                        || input.moodScore() != null
                        || input.stressScore() != null
                        || input.dietQuality() != null
                        || input.activityMinutes() != null
        );
    }

    private void requireVersion(
            Long currentVersion,
            Long submittedVersion,
            String dataName
    ) {
        if (submittedVersion == null) {
            throw error(INVALID_INPUT, dataName + "缺少 version");
        }

        if (!Objects.equals(currentVersion, submittedVersion)) {
            throw error(
                    VERSION_CONFLICT,
                    dataName + "版本冲突，请重新获取最新数据"
            );
        }
    }

    private String normalizeNote(String note) {
        return note == null || note.isBlank() ? null : note;
    }

    private WeeklyAssessmentBusinessException error(
            WeeklyAssessmentBusinessException.Reason reason,
            String message
    ) {
        return new WeeklyAssessmentBusinessException(reason, message);
    }
}
