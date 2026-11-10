
package com.healthpilot.server.healthprofile.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "health_profiles")
public class HealthProfileEntity {

    public enum ActivityLevel {
        SEDENTARY,
        LIGHT,
        MODERATE,
        ACTIVE,
        VERY_ACTIVE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 画像所属用户，创建后不允许修改
    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(name = "birth_year")
    private Short birthYear;

    @Column(name = "height_cm", precision = 5, scale = 2)
    private BigDecimal heightCm;

    // 数据库实际类型为VARCHAR，不使用MySQL原生ENUM
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "activity_level", length = 20)
    private ActivityLevel activityLevel;

    @Column(name = "sleep_schedule", length = 100)
    private String sleepSchedule;

    @Column(name = "health_notes", length = 1000)
    private String healthNotes;

    @Column(name = "timezone", nullable = false, length = 64)
    private String timezone;

    // Hibernate更新实体时自动检查和递增版本号
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    // 时间由MySQL默认值维护，Java只负责读取
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    protected HealthProfileEntity() {
    }

    public HealthProfileEntity(Long userId, String timezone) {
        this.userId = userId;
        this.timezone = timezone;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Short getBirthYear() {
        return birthYear;
    }

    public BigDecimal getHeightCm() {
        return heightCm;
    }

    public ActivityLevel getActivityLevel() {
        return activityLevel;
    }

    public String getSleepSchedule() {
        return sleepSchedule;
    }

    public String getHealthNotes() {
        return healthNotes;
    }

    public String getTimezone() {
        return timezone;
    }

    public Long getVersion() {
        return version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setBirthYear(Short birthYear) {
        this.birthYear = birthYear;
    }

    public void setHeightCm(BigDecimal heightCm) {
        this.heightCm = heightCm;
    }

    public void setActivityLevel(ActivityLevel activityLevel) {
        this.activityLevel = activityLevel;
    }

    public void setSleepSchedule(String sleepSchedule) {
        this.sleepSchedule = sleepSchedule;
    }

    public void setHealthNotes(String healthNotes) {
        this.healthNotes = healthNotes;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }
}
