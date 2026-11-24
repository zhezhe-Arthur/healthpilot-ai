
package com.healthpilot.server.healthplan.service;

import com.healthpilot.server.healthplan.dto.*;
import com.healthpilot.server.healthplan.entity.HealthPlanEntity;
import com.healthpilot.server.healthplan.entity.HealthPlanItemEntity;
import com.healthpilot.server.healthplan.exception.HealthPlanBusinessException;
import com.healthpilot.server.healthplan.repository.HealthPlanItemRepository;
import com.healthpilot.server.healthplan.repository.HealthPlanRepository;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static com.healthpilot.server.healthplan.exception
        .HealthPlanBusinessException.Reason.*;

@Service
public class HealthPlanService {

    private final HealthPlanRepository planRepository;
    private final HealthPlanItemRepository itemRepository;
    private final Validator validator;

    public HealthPlanService(
            HealthPlanRepository planRepository,
            HealthPlanItemRepository itemRepository,
            Validator validator
    ) {
        this.planRepository = planRepository;
        this.itemRepository = itemRepository;
        this.validator = validator;
    }

    /**
     * 创建或重新生成七天计划草稿。
     * 重新生成时只允许替换 DRAFT 状态的计划。
     */
    @Transactional
    public HealthPlanResponse saveDraft(
            Long userId,
            SaveHealthPlanDraftRequest request
    ) {
        validateDraftRequest(request);

        LocalDate weekStartDate = request.weekStartDate();

        // 通过用户ID和周日期查询，确保数据归属正确
        HealthPlanEntity plan = planRepository
                .findByUserIdAndWeekStartDate(
                        userId,
                        weekStartDate
                )
                .orElse(null);

        boolean isNew = plan == null;

        if (isNew) {

            // 第一次生成计划，创建草稿
            plan = new HealthPlanEntity(
                    userId,
                    weekStartDate,
                    request.aiSummary()
            );

        } else {

            // 已确认的计划不允许重新生成
            if (!"DRAFT".equals(plan.getStatus())) {
                throw new HealthPlanBusinessException(
                        PLAN_CONFIRMED,
                        "当前计划已确认，不能重新生成"
                );
            }

            plan.setAiSummary(request.aiSummary());
        }

        // 保存主计划，确保获得数据库ID
        plan = planRepository.saveAndFlush(plan);

        if (!isNew) {

            // 替换草稿时删除旧任务
            itemRepository.deleteByPlanId(plan.getId());
            itemRepository.flush();
        }

        // 构造本次计划的全部任务
        List<HealthPlanItemEntity> entities =
                new ArrayList<>();

        for (HealthPlanItemInput input : request.items()) {

            HealthPlanItemEntity item =
                    new HealthPlanItemEntity(
                            plan.getId(),
                            input.category(),
                            input.title(),
                            input.description(),
                            input.scheduledWeekdays(),
                            input.targetQuantity(),
                            input.targetUnit(),
                            input.sortOrder()
                    );

            entities.add(item);
        }

        // 保存全部任务，失败时整个事务回滚
        List<HealthPlanItemEntity> savedItems =
                itemRepository.saveAllAndFlush(entities);

        return HealthPlanResponse.from(plan, savedItems);
    }

    /**
     * 查询当前用户指定周的计划。
     */
    @Transactional(readOnly = true)
    public HealthPlanResponse getByWeek(
            Long userId,
            LocalDate weekStartDate
    ) {
        validateWeekStartDate(weekStartDate);

        HealthPlanEntity plan = planRepository
                .findByUserIdAndWeekStartDate(
                        userId,
                        weekStartDate
                )
                .orElseThrow(() ->
                        new HealthPlanBusinessException(
                                NOT_FOUND,
                                "该周健康计划不存在"
                        )
                );

        return buildResponse(plan);
    }

    /**
     * 查询用户历史健康计划，按周日期倒序返回。
     */
    @Transactional(readOnly = true)
    public List<HealthPlanResponse> list(Long userId) {

        List<HealthPlanEntity> plans = planRepository
                .findByUserIdOrderByWeekStartDateDesc(userId);

        return plans.stream()
                .map(this::buildResponse)
                .toList();
    }

    /**
     * 用户确认计划。
     * 确认后不允许通过 saveDraft 覆盖该计划。
     */
    @Transactional
    public HealthPlanResponse confirm(
            Long userId,
            Long planId,
            ConfirmHealthPlanRequest request
    ) {
        if (request == null
                || request.version() == null
                || request.version() < 0) {

            throw new HealthPlanBusinessException(
                    INVALID_INPUT,
                    "必须提供合法的计划版本号"
            );
        }

        HealthPlanEntity plan = planRepository
                .findByIdAndUserId(planId, userId)
                .orElseThrow(() ->
                        new HealthPlanBusinessException(
                                NOT_FOUND,
                                "健康计划不存在"
                        )
                );

        // 检查客户端版本是否与数据库一致
        if (!Objects.equals(
                plan.getVersion(),
                request.version()
        )) {
            throw new HealthPlanBusinessException(
                    VERSION_CONFLICT,
                    "计划版本已变化，请重新获取计划"
            );
        }

        if (!"DRAFT".equals(plan.getStatus())) {
            throw new HealthPlanBusinessException(
                    PLAN_CONFIRMED,
                    "该健康计划已经确认"
            );
        }

        List<HealthPlanItemEntity> items =
                itemRepository
                        .findByPlanIdOrderBySortOrderAscIdAsc(
                                plan.getId()
                        );

        if (items.isEmpty()) {
            throw new HealthPlanBusinessException(
                    INVALID_INPUT,
                    "没有任务的计划不能确认"
            );
        }

        // DRAFT -> CONFIRMED
        plan.confirm();

        // flush触发SQL更新和乐观锁检查
        planRepository.saveAndFlush(plan);

        return HealthPlanResponse.from(plan, items);
    }

    /**
     * Entity与任务列表统一转换为响应DTO。
     */
    private HealthPlanResponse buildResponse(
            HealthPlanEntity plan
    ) {
        List<HealthPlanItemEntity> items =
                itemRepository
                        .findByPlanIdOrderBySortOrderAscIdAsc(
                                plan.getId()
                        );

        return HealthPlanResponse.from(plan, items);
    }

    /**
     * 校验周日期必须为星期一。
     */
    private void validateWeekStartDate(LocalDate date) {

        if (date == null
                || date.getDayOfWeek() != DayOfWeek.MONDAY) {

            throw new HealthPlanBusinessException(
                    INVALID_INPUT,
                    "weekStartDate 必须是星期一"
            );
        }
    }

    /**
     * 校验草稿DTO及任务日期。
     */
    private void validateDraftRequest(
            SaveHealthPlanDraftRequest request
    ) {
        if (request == null) {
            throw new HealthPlanBusinessException(
                    INVALID_INPUT,
                    "计划内容不能为空"
            );
        }

        // 即使由内部AI调用Service，也进行DTO校验
        Set<ConstraintViolation<SaveHealthPlanDraftRequest>>
                violations = validator.validate(request);

        if (!violations.isEmpty()) {

            ConstraintViolation<SaveHealthPlanDraftRequest>
                    violation = violations.iterator().next();

            throw new HealthPlanBusinessException(
                    INVALID_INPUT,
                    violation.getPropertyPath()
                            + ": "
                            + violation.getMessage()
            );
        }

        validateWeekStartDate(request.weekStartDate());

        for (HealthPlanItemInput item : request.items()) {

            // DTO的正则校验格式，这里额外检查重复星期
            String[] weekdays =
                    item.scheduledWeekdays().split(",");

            Set<String> uniqueDays =
                    new HashSet<>(Arrays.asList(weekdays));

            if (uniqueDays.size() != weekdays.length) {
                throw new HealthPlanBusinessException(
                        INVALID_INPUT,
                        "同一任务的执行星期不能重复："
                                + item.title()
                );
            }
        }
    }
}
