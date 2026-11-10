
package com.healthpilot.server.healthcheckin.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "health_daily_checkins")
public class HealthDailyCheckinEntity {

    public enum CheckinSource {
        MANUAL,
        DEVICE,
        IMPORT,
        MIXED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(name = "record_date", nullable = false, updatable = false)
    private LocalDate recordDate;

    // 1～5分，数据库类型为TINYINT
    @Column(name = "mood_score")
    private Byte moodScore;

    @Column(name = "stress_score")
    private Byte stressScore;

    // 睡眠分钟数，数据库类型为SMALLINT
    @Column(name = "sleep_minutes")
    private Short sleepMinutes;

    @Column(name = "sleep_quality")
    private Byte sleepQuality;

    @Column(name = "weight_kg", precision = 6, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "steps")
    private Integer steps;

    @Column(name = "activity_minutes")
    private Short activityMinutes;

    @Column(name = "water_ml")
    private Integer waterMl;

    @Column(name = "diet_quality")
    private Byte dietQuality;

    @Column(name = "notes", length = 2000)
    private String notes;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "source", nullable = false, length = 16)
    private CheckinSource source = CheckinSource.MANUAL;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    protected HealthDailyCheckinEntity() {
    }

    public HealthDailyCheckinEntity(
            Long userId,
            LocalDate recordDate
    ) {
        this.userId = userId;
        this.recordDate = recordDate;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public LocalDate getRecordDate() {
        return recordDate;
    }

    public Byte getMoodScore() {
        return moodScore;
    }

    public Byte getStressScore() {
        return stressScore;
    }

    public Short getSleepMinutes() {
        return sleepMinutes;
    }

    public Byte getSleepQuality() {
        return sleepQuality;
    }

    public BigDecimal getWeightKg() {
        return weightKg;
    }

    public Integer getSteps() {
        return steps;
    }

    public Short getActivityMinutes() {
        return activityMinutes;
    }

    public Integer getWaterMl() {
        return waterMl;
    }

    public Byte getDietQuality() {
        return dietQuality;
    }

    public String getNotes() {
        return notes;
    }

    public CheckinSource getSource() {
        return source;
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

    public void setMoodScore(Byte moodScore) {
        this.moodScore = moodScore;
    }

    public void setStressScore(Byte stressScore) {
        this.stressScore = stressScore;
    }

    public void setSleepMinutes(Short sleepMinutes) {
        this.sleepMinutes = sleepMinutes;
    }

    public void setSleepQuality(Byte sleepQuality) {
        this.sleepQuality = sleepQuality;
    }

    public void setWeightKg(BigDecimal weightKg) {
        this.weightKg = weightKg;
    }

    public void setSteps(Integer steps) {
        this.steps = steps;
    }

    public void setActivityMinutes(Short activityMinutes) {
        this.activityMinutes = activityMinutes;
    }

    public void setWaterMl(Integer waterMl) {
        this.waterMl = waterMl;
    }

    public void setDietQuality(Byte dietQuality) {
        this.dietQuality = dietQuality;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void setSource(CheckinSource source) {
        this.source = source;
    }
}
