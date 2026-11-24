
package com.healthpilot.server.healthgoal.service;

import com.healthpilot.server.healthgoal.dto.CreateHealthGoalRequest;
import com.healthpilot.server.healthgoal.dto.HealthGoalResponse;
import com.healthpilot.server.healthgoal.dto.UpdateHealthGoalRequest;
import com.healthpilot.server.healthgoal.entity.HealthGoalEntity;
import com.healthpilot.server.healthgoal.entity.HealthGoalEntity.GoalStatus;
import com.healthpilot.server.healthgoal.exception.HealthGoalBusinessException;
import com.healthpilot.server.healthgoal.repository.HealthGoalRepository;

import jakarta.persistence.EntityManager;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static com.healthpilot.server.healthgoal.exception.HealthGoalBusinessException.Reason.*;

@Service
public class HealthGoalService {

    private final HealthGoalRepository goalRepository;
    private final EntityManager entityManager;

    public HealthGoalService(
            HealthGoalRepository goalRepository,
            EntityManager entityManager
    ) {
        this.goalRepository = goalRepository;
        this.entityManager = entityManager;
    }

    // 创建健康目标
    @Transactional
    public HealthGoalResponse create(
            Long userId,
            CreateHealthGoalRequest request
    ) {
        String title = validateTitle(request.title());
        String unit = normalizeUnit(request.targetUnit());

        validateTargetPair(request.targetValue(), unit);

        HealthGoalEntity goal = new HealthGoalEntity(
                userId,
                request.category(),
                title
        );

        goal.setDescription(request.description());
        goal.setTargetValue(request.targetValue());
        goal.setTargetUnit(unit);
        goal.setTargetDate(request.targetDate());

        goalRepository.saveAndFlush(goal);

        // 获取数据库自动生成的时间
        entityManager.refresh(goal);

        return HealthGoalResponse.from(goal);
    }

    // 查询当前用户的健康目标列表
    @Transactional(readOnly = true)
    public List<HealthGoalResponse> getMyGoals(
            Long userId,
            GoalStatus status
    ) {
        List<HealthGoalEntity> goals;

        if (status == null) {
            goals = goalRepository
                    .findByUserIdOrderByCreatedAtDesc(userId);
        } else {
            goals = goalRepository
                    .findByUserIdAndStatusOrderByCreatedAtDesc(
                            userId,
                            status
                    );
        }

        return goals.stream()
                .map(HealthGoalResponse::from)
                .toList();
    }

    // 查询当前用户的指定目标
    @Transactional(readOnly = true)
    public HealthGoalResponse getMyGoal(
            Long userId,
            Long goalId
    ) {
        return HealthGoalResponse.from(
                findMyGoal(userId, goalId)
        );
    }

    // 修改目标信息或状态
    @Transactional
    public HealthGoalResponse update(
            Long userId,
            Long goalId,
            UpdateHealthGoalRequest request
    ) {
        HealthGoalEntity goal = findMyGoal(userId, goalId);

        // 检查前端提交的版本号
        if (!Objects.equals(
                goal.getVersion(),
                request.version()
        )) {
            throw new HealthGoalBusinessException(
                    VERSION_CONFLICT,
                    "健康目标已被修改，请刷新后重试"
            );
        }

        // 已完成、已取消的目标不允许重新编辑
        if (goal.getStatus() == GoalStatus.COMPLETED
                || goal.getStatus() == GoalStatus.CANCELLED) {
            throw new HealthGoalBusinessException(
                    INVALID_STATUS,
                    "已结束的健康目标不能修改"
            );
        }

        Set<String> clearFields = validateClearFields(request);

        // 校验目标状态是否允许切换
        if (request.status() != null) {
            validateStatusChange(
                    goal.getStatus(),
                    request.status()
            );
        }

        // PATCH：只修改客户端实际提供的字段
        if (request.category() != null) {
            goal.setCategory(request.category());
        }

        if (request.title() != null) {
            goal.setTitle(validateTitle(request.title()));
        }

        if (request.description() != null) {
            goal.setDescription(request.description());
        }

        if (request.targetValue() != null) {
            goal.setTargetValue(request.targetValue());
        }

        if (request.targetUnit() != null) {
            goal.setTargetUnit(
                    normalizeUnit(request.targetUnit())
            );
        }

        if (request.targetDate() != null) {
            goal.setTargetDate(request.targetDate());
        }

        // 显式清空可选字段
        if (clearFields.contains("description")) {
            goal.setDescription(null);
        }

        if (clearFields.contains("target")) {
            goal.setTargetValue(null);
            goal.setTargetUnit(null);
        }

        if (clearFields.contains("targetDate")) {
            goal.setTargetDate(null);
        }

        if (request.status() != null) {
            goal.setStatus(request.status());
        }

        // 修改之后再检查数值与单位是否匹配
        validateTargetPair(
                goal.getTargetValue(),
                goal.getTargetUnit()
        );

        goalRepository.flush();
        entityManager.refresh(goal);

        return HealthGoalResponse.from(goal);
    }

    // 按目标ID和当前用户ID共同查询
    private HealthGoalEntity findMyGoal(
            Long userId,
            Long goalId
    ) {
        return goalRepository
                .findByIdAndUserId(goalId, userId)
                .orElseThrow(() ->
                        new HealthGoalBusinessException(
                                NOT_FOUND,
                                "健康目标不存在"
                        )
                );
    }

    // 数值目标与单位必须同时存在，或同时为空
    private void validateTargetPair(
            BigDecimal value,
            String unit
    ) {
        if ((value == null) != (unit == null)) {
            throw new HealthGoalBusinessException(
                    INVALID_INPUT,
                    "目标数值和单位必须同时填写或同时清空"
            );
        }

        if (value != null && value.signum() < 0) {
            throw new HealthGoalBusinessException(
                    INVALID_INPUT,
                    "目标数值不能为负数"
            );
        }

        if (unit != null && unit.isBlank()) {
            throw new HealthGoalBusinessException(
                    INVALID_INPUT,
                    "目标单位不能为空白字符串"
            );
        }
    }

    // 统一处理目标名称
    private String validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new HealthGoalBusinessException(
                    INVALID_INPUT,
                    "目标名称不能为空"
            );
        }

        String result = title.trim();

        if (result.length() > 120) {
            throw new HealthGoalBusinessException(
                    INVALID_INPUT,
                    "目标名称不能超过120个字符"
            );
        }

        return result;
    }

    // 去掉单位两侧空格
    private String normalizeUnit(String unit) {
        if (unit == null) {
            return null;
        }

        String result = unit.trim();

        if (result.isEmpty() || result.length() > 20) {
            throw new HealthGoalBusinessException(
                    INVALID_INPUT,
                    "目标单位不合法"
            );
        }

        return result;
    }

    // 验证状态流转
    private void validateStatusChange(
            GoalStatus current,
            GoalStatus next
    ) {
        if (current == next) {
            return;
        }

        boolean allowed = switch (current) {
            case ACTIVE -> true;

            case PAUSED -> next == GoalStatus.ACTIVE
                    || next == GoalStatus.COMPLETED
                    || next == GoalStatus.CANCELLED;

            case COMPLETED, CANCELLED -> false;
        };

        if (!allowed) {
            throw new HealthGoalBusinessException(
                    INVALID_STATUS,
                    "不允许从当前状态切换到目标状态"
            );
        }
    }

    // 检查clearFields是否重复、是否合法、是否与修改冲突
    private Set<String> validateClearFields(
            UpdateHealthGoalRequest request
    ) {
        List<String> fields = request.clearFields();

        if (fields == null || fields.isEmpty()) {
            return Set.of();
        }

        Set<String> result = new HashSet<>(fields);

        if (result.size() != fields.size()) {
            throw new HealthGoalBusinessException(
                    INVALID_INPUT,
                    "清空字段不能重复"
            );
        }

        Set<String> allowed = Set.of(
                "description",
                "target",
                "targetDate"
        );

        if (!allowed.containsAll(result)) {
            throw new HealthGoalBusinessException(
                    INVALID_INPUT,
                    "包含不支持清空的字段"
            );
        }

        boolean conflict =
                (result.contains("description")
                        && request.description() != null)
                        || (result.contains("target")
                        && (request.targetValue() != null
                        || request.targetUnit() != null))
                        || (result.contains("targetDate")
                        && request.targetDate() != null);

        if (conflict) {
            throw new HealthGoalBusinessException(
                    INVALID_INPUT,
                    "同一字段不能同时修改和清空"
            );
        }

        return result;
    }
}
