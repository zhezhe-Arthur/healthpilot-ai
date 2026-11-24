
package com.healthpilot.server.weeklyassessment;

import com.healthpilot.server.healthprofile.repository.HealthProfileRepository;
import com.healthpilot.server.weeklyassessment.dto.UpdateWeeklyAssessmentRequest;
import com.healthpilot.server.weeklyassessment.dto.UpdateWeeklyFeedbackInput;
import com.healthpilot.server.weeklyassessment.entity.HealthProfileSnapshotEntity;
import com.healthpilot.server.weeklyassessment.entity.WeeklyFeedbackEntity;
import com.healthpilot.server.weeklyassessment.exception.WeeklyAssessmentBusinessException;
import com.healthpilot.server.weeklyassessment.repository.HealthProfileSnapshotRepository;
import com.healthpilot.server.weeklyassessment.repository.WeeklyFeedbackRepository;
import com.healthpilot.server.weeklyassessment.service.WeeklyAssessmentService;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WeeklyAssessmentServiceIsolationTest {

    private static final Long USER_A = 1001L;
    private static final Long USER_B = 1002L;

    private static final LocalDate WEEK =
            LocalDate.of(2026, 11, 2);

    @Mock
    private WeeklyFeedbackRepository feedbackRepository;

    @Mock
    private HealthProfileSnapshotRepository snapshotRepository;

    @Mock
    private HealthProfileRepository profileRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private WeeklyAssessmentService service;

    // 测试1：用户A不能读取用户B的健康评估
    @Test
    void shouldNotReadOtherUsersAssessment() {

        WeeklyFeedbackEntity userBFeedback =
                new WeeklyFeedbackEntity(
                        USER_B,
                        WEEK,
                        "most",
                        "better",
                        "normal",
                        null
                );

        HealthProfileSnapshotEntity userBSnapshot =
                new HealthProfileSnapshotEntity(USER_B, WEEK);

        // 模拟数据库：只有用户B拥有这一周的记录
        when(feedbackRepository.findByUserIdAndWeekStartDate(
                anyLong(), eq(WEEK)
        )).thenAnswer(invocation -> {
            Long userId = invocation.getArgument(0);

            return USER_B.equals(userId)
                    ? Optional.of(userBFeedback)
                    : Optional.empty();
        });

        when(snapshotRepository.findByUserIdAndWeekStartDate(
                USER_B, WEEK
        )).thenReturn(Optional.of(userBSnapshot));

        // 用户B可以正常查询自己的记录
        var resultB = service.get(USER_B, WEEK);

        assertEquals(WEEK, resultB.weekStartDate());
        assertEquals("most", resultB.feedback().execution());

        // 用户A查询相同日期，应该返回NOT_FOUND
        WeeklyAssessmentBusinessException exception =
                assertThrows(
                        WeeklyAssessmentBusinessException.class,
                        () -> service.get(USER_A, WEEK)
                );

        assertEquals(
                WeeklyAssessmentBusinessException.Reason.NOT_FOUND,
                exception.getReason()
        );

        // 验证用户A的查询始终带有自己的用户ID
        verify(feedbackRepository)
                .findByUserIdAndWeekStartDate(USER_A, WEEK);

        // 不允许用用户B的ID读取其快照
        verify(snapshotRepository, times(1))
                .findByUserIdAndWeekStartDate(USER_B, WEEK);
    }

    // 测试2：用户A不能修改用户B的健康评估
    @Test
    void shouldNotUpdateOtherUsersAssessment() {

        when(feedbackRepository.findByUserIdAndWeekStartDate(
                USER_A, WEEK
        )).thenReturn(Optional.empty());

        UpdateWeeklyAssessmentRequest request =
                new UpdateWeeklyAssessmentRequest(
                        0L,
                        new UpdateWeeklyFeedbackInput(
                                "all",
                                null,
                                null,
                                null
                        ),
                        null,
                        null,
                        null
                );

        WeeklyAssessmentBusinessException exception =
                assertThrows(
                        WeeklyAssessmentBusinessException.class,
                        () -> service.update(USER_A, WEEK, request)
                );

        assertEquals(
                WeeklyAssessmentBusinessException.Reason.NOT_FOUND,
                exception.getReason()
        );

        verify(feedbackRepository)
                .findByUserIdAndWeekStartDate(USER_A, WEEK);

        // 查询不到自己的数据，不允许发生任何写入
        verify(feedbackRepository, never())
                .saveAndFlush(any());

        verifyNoInteractions(snapshotRepository);
    }

    // 测试3：历史记录查询只能使用当前用户ID
    @Test
    void shouldListOnlyCurrentUsersAssessments() {

        LocalDate startDate = LocalDate.of(2026, 11, 1);
        LocalDate endDate = LocalDate.of(2026, 11, 12);

        WeeklyFeedbackEntity feedback =
                new WeeklyFeedbackEntity(
                        USER_A,
                        WEEK,
                        "most",
                        "better",
                        "normal",
                        null
                );

        HealthProfileSnapshotEntity snapshot =
                new HealthProfileSnapshotEntity(USER_A, WEEK);

        when(feedbackRepository
                .findByUserIdAndWeekStartDateBetweenOrderByWeekStartDateDesc(
                        USER_A, startDate, endDate
                )
        ).thenReturn(List.of(feedback));

        when(snapshotRepository
                .findByUserIdAndWeekStartDateBetweenOrderByWeekStartDateDesc(
                        USER_A, startDate, endDate
                )
        ).thenReturn(List.of(snapshot));

        var history = service.list(
                USER_A,
                startDate,
                endDate
        );

        assertEquals(1, history.size());
        assertEquals(WEEK, history.get(0).weekStartDate());

        // 验证两张表都按用户A的ID查询
        verify(feedbackRepository)
                .findByUserIdAndWeekStartDateBetweenOrderByWeekStartDateDesc(
                        USER_A, startDate, endDate
                );

        verify(snapshotRepository)
                .findByUserIdAndWeekStartDateBetweenOrderByWeekStartDateDesc(
                        USER_A, startDate, endDate
                );
    }
}
