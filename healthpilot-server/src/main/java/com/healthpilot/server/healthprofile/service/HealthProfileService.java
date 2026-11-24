
package com.healthpilot.server.healthprofile.service;

import com.healthpilot.server.healthprofile.dto.CreateHealthProfileRequest;
import com.healthpilot.server.healthprofile.dto.UpdateHealthProfileRequest;
import com.healthpilot.server.healthprofile.dto.HealthProfileResponse;
import com.healthpilot.server.healthprofile.entity.HealthProfileEntity;
import com.healthpilot.server.healthprofile.exception.HealthProfileBusinessException;
import com.healthpilot.server.healthprofile.repository.HealthProfileRepository;

import jakarta.persistence.EntityManager;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static com.healthpilot.server.healthprofile.exception.HealthProfileBusinessException.Reason.*;

@Service
public class HealthProfileService {

    private final HealthProfileRepository profileRepository;
    private final EntityManager entityManager;

    public HealthProfileService(
            HealthProfileRepository profileRepository,
            EntityManager entityManager
    ) {
        this.profileRepository = profileRepository;
        this.entityManager = entityManager;
    }

    /**
     * 首次建立健康画像。
     * 同一个用户只能创建一份画像。
     */
    @Transactional
    public HealthProfileResponse create(
            Long userId,
            CreateHealthProfileRequest request
    ) {
        if (profileRepository.existsByUserId(userId)) {
            throw new HealthProfileBusinessException(
                    ALREADY_EXISTS,
                    "健康画像已经存在"
            );
        }

        String timezone = validateTimezone(request.timezone());

        HealthProfileEntity profile =
                new HealthProfileEntity(userId, timezone);

        profile.setBirthYear(request.birthYear());
        profile.setHeightCm(request.heightCm());
        profile.setActivityLevel(request.activityLevel());
        profile.setSleepSchedule(request.sleepSchedule());
        profile.setHealthNotes(request.healthNotes());

        try {
            // 立即执行插入，以便尽早发现数据库约束错误
            profileRepository.saveAndFlush(profile);

            // 读取MySQL生成的创建时间、更新时间
            entityManager.refresh(profile);

            return HealthProfileResponse.from(profile);

        } catch (DataIntegrityViolationException e) {
            // 参数已校验，此处主要防止并发创建重复画像
            throw new HealthProfileBusinessException(
                    ALREADY_EXISTS,
                    "健康画像已经存在"
            );
        }
    }

    /**
     * 根据当前登录用户ID查询画像。
     * 不允许直接传入其他用户的ID。
     */
    @Transactional(readOnly = true)
    public HealthProfileResponse getMyProfile(Long userId) {

        HealthProfileEntity profile = profileRepository
                .findByUserId(userId)
                .orElseThrow(() ->
                        new HealthProfileBusinessException(
                                NOT_FOUND,
                                "尚未建立健康画像"
                        )
                );

        return HealthProfileResponse.from(profile);
    }

    /**
     * 局部更新画像。
     *
     * version用于检测前端是否基于旧数据修改。
     * JPA @Version用于防止事务并发更新造成数据覆盖。
     */
    @Transactional
    public HealthProfileResponse update(
            Long userId,
            UpdateHealthProfileRequest request
    ) {
        HealthProfileEntity profile = profileRepository
                .findByUserId(userId)
                .orElseThrow(() ->
                        new HealthProfileBusinessException(
                                NOT_FOUND,
                                "尚未建立健康画像"
                        )
                );

        // 前端携带的版本必须与数据库当前版本一致
        if (!Objects.equals(
                profile.getVersion(),
                request.version()
        )) {
            throw new HealthProfileBusinessException(
                    VERSION_CONFLICT,
                    "健康画像已被修改，请刷新后重试"
            );
        }

        Set<String> clearFields = validateClearFields(request);

        // 没有提供的字段保持原来的值
        if (request.birthYear() != null) {
            profile.setBirthYear(request.birthYear());
        }

        if (request.heightCm() != null) {
            profile.setHeightCm(request.heightCm());
        }

        if (request.activityLevel() != null) {
            profile.setActivityLevel(request.activityLevel());
        }

        if (request.sleepSchedule() != null) {
            profile.setSleepSchedule(request.sleepSchedule());
        }

        if (request.healthNotes() != null) {
            profile.setHealthNotes(request.healthNotes());
        }

        if (request.timezone() != null) {
            profile.setTimezone(
                    validateTimezone(request.timezone())
            );
        }

        // 明确清空用户指定的可选字段
        for (String field : clearFields) {
            switch (field) {
                case "birthYear" -> profile.setBirthYear(null);
                case "heightCm" -> profile.setHeightCm(null);
                case "activityLevel" ->
                        profile.setActivityLevel(null);
                case "sleepSchedule" ->
                        profile.setSleepSchedule(null);
                case "healthNotes" ->
                        profile.setHealthNotes(null);
                default -> throw new HealthProfileBusinessException(
                        INVALID_INPUT,
                        "不支持清空字段：" + field
                );
            }
        }

        // 将实体变更同步到数据库
        // Hibernate会自动检查并递增@Version
        profileRepository.flush();

        // 重新读取数据库自动维护的时间字段
        entityManager.refresh(profile);

        return HealthProfileResponse.from(profile);
    }

    /**
     * 检查需要清空的字段，避免同一字段既设置又清空。
     */
    private Set<String> validateClearFields(
            UpdateHealthProfileRequest request
    ) {
        List<String> fields = request.clearFields();

        if (fields == null || fields.isEmpty()) {
            return Set.of();
        }

        Set<String> clear = new HashSet<>(fields);

        if (clear.size() != fields.size()) {
            throw new HealthProfileBusinessException(
                    INVALID_INPUT,
                    "清空字段不能重复"
            );
        }

        Set<String> allowed = Set.of(
                "birthYear",
                "heightCm",
                "activityLevel",
                "sleepSchedule",
                "healthNotes"
        );

        if (!allowed.containsAll(clear)) {
            throw new HealthProfileBusinessException(
                    INVALID_INPUT,
                    "包含不支持清空的字段"
            );
        }

        boolean conflict =
                (clear.contains("birthYear")
                        && request.birthYear() != null)
                        || (clear.contains("heightCm")
                        && request.heightCm() != null)
                        || (clear.contains("activityLevel")
                        && request.activityLevel() != null)
                        || (clear.contains("sleepSchedule")
                        && request.sleepSchedule() != null)
                        || (clear.contains("healthNotes")
                        && request.healthNotes() != null);

        if (conflict) {
            throw new HealthProfileBusinessException(
                    INVALID_INPUT,
                    "同一字段不能同时修改和清空"
            );
        }

        return clear;
    }

    /**
     * 验证IANA时区标识。
     * 例如：Asia/Shanghai、Asia/Seoul、UTC。
     */
    private String validateTimezone(String timezone) {

        try {
            if (timezone == null || timezone.isBlank()) {
                throw new IllegalArgumentException();
            }

            String normalized = timezone.trim();
            ZoneId.of(normalized);

            return normalized;

        } catch (DateTimeException | IllegalArgumentException e) {
            throw new HealthProfileBusinessException(
                    INVALID_INPUT,
                    "无效的时区标识"
            );
        }
    }
}
