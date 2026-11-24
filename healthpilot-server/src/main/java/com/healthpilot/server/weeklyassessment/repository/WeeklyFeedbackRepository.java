
package com.healthpilot.server.weeklyassessment.repository;

import com.healthpilot.server.weeklyassessment.entity.WeeklyFeedbackEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WeeklyFeedbackRepository
        extends JpaRepository<WeeklyFeedbackEntity, Long> {

    // 查询用户指定周的反馈
    Optional<WeeklyFeedbackEntity> findByUserIdAndWeekStartDate(
            Long userId,
            LocalDate weekStartDate
    );

    // 判断本周是否已经提交反馈
    boolean existsByUserIdAndWeekStartDate(
            Long userId,
            LocalDate weekStartDate
    );

    // 查询指定日期范围内的历史反馈，按时间倒序
    List<WeeklyFeedbackEntity>
    findByUserIdAndWeekStartDateBetweenOrderByWeekStartDateDesc(
            Long userId,
            LocalDate startDate,
            LocalDate endDate
    );
}
