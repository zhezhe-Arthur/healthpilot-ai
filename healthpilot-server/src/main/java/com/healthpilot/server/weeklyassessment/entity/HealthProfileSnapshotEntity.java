
package com.healthpilot.server.weeklyassessment.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "health_profile_snapshots")
public class HealthProfileSnapshotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "week_start_date", nullable = false)
    private LocalDate weekStartDate;

    // 基础健康画像的历史快照
    @Column(name = "birth_year")
    private Short birthYear;

    @Column(name = "height_cm", precision = 5, scale = 2)
    private BigDecimal heightCm;

    @Column(name = "activity_level", length = 20)
    private String activityLevel;

    @Column(name = "sleep_schedule", length = 100)
    private String sleepSchedule;

    @Column(name = "health_notes", length = 1000)
    private String healthNotes;

    @Column(name = "timezone", length = 64)
    private String timezone;

    // 每周自主更新的健康指标
    @Column(name = "weight_kg", precision = 6, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "avg_sleep_minutes")
    private Short avgSleepMinutes;

    @Column(name = "sleep_quality")
    private Byte sleepQuality;

    @Column(name = "mood_score")
    private Byte moodScore;

    @Column(name = "stress_score")
    private Byte stressScore;

    @Column(name = "diet_quality")
    private Byte dietQuality;

    @Column(name = "activity_minutes")
    private Integer activityMinutes;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected HealthProfileSnapshotEntity() {
    }

    public HealthProfileSnapshotEntity(Long userId, LocalDate weekStartDate) {
        this.userId = userId;
        this.weekStartDate = weekStartDate;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public LocalDate getWeekStartDate() {
        return weekStartDate;
    }

    public Short getBirthYear() {
        return birthYear;
    }

    public void setBirthYear(Short birthYear) {
        this.birthYear = birthYear;
    }

    public BigDecimal getHeightCm() {
        return heightCm;
    }

    public void setHeightCm(BigDecimal heightCm) {
        this.heightCm = heightCm;
    }

    public String getActivityLevel() {
        return activityLevel;
    }

    public void setActivityLevel(String activityLevel) {
        this.activityLevel = activityLevel;
    }

    public String getSleepSchedule() {
        return sleepSchedule;
    }

    public void setSleepSchedule(String sleepSchedule) {
        this.sleepSchedule = sleepSchedule;
    }

    public String getHealthNotes() {
        return healthNotes;
    }

    public void setHealthNotes(String healthNotes) {
        this.healthNotes = healthNotes;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public BigDecimal getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(BigDecimal weightKg) {
        this.weightKg = weightKg;
    }

    public Short getAvgSleepMinutes() {
        return avgSleepMinutes;
    }

    public void setAvgSleepMinutes(Short avgSleepMinutes) {
        this.avgSleepMinutes = avgSleepMinutes;
    }

    public Byte getSleepQuality() {
        return sleepQuality;
    }

    public void setSleepQuality(Byte sleepQuality) {
        this.sleepQuality = sleepQuality;
    }

    public Byte getMoodScore() {
        return moodScore;
    }

    public void setMoodScore(Byte moodScore) {
        this.moodScore = moodScore;
    }

    public Byte getStressScore() {
        return stressScore;
    }

    public void setStressScore(Byte stressScore) {
        this.stressScore = stressScore;
    }

    public Byte getDietQuality() {
        return dietQuality;
    }

    public void setDietQuality(Byte dietQuality) {
        this.dietQuality = dietQuality;
    }

    public Integer getActivityMinutes() {
        return activityMinutes;
    }

    public void setActivityMinutes(Integer activityMinutes) {
        this.activityMinutes = activityMinutes;
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
}
