
package com.healthpilot.server.healthplan.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "health_plans")
public class HealthPlanEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "week_start_date", nullable = false)
    private LocalDate weekStartDate;

    @Column(nullable = false, length = 20)
    private String status = "DRAFT";

    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected HealthPlanEntity() {
    }

    public HealthPlanEntity(
            Long userId,
            LocalDate weekStartDate,
            String aiSummary
    ) {
        this.userId = userId;
        this.weekStartDate = weekStartDate;
        this.aiSummary = aiSummary;
        this.status = "DRAFT";
    }

    public void confirm() {
        if (!"DRAFT".equals(status)) {
            throw new IllegalStateException("计划不是草稿状态");
        }

        this.status = "CONFIRMED";
        this.confirmedAt = LocalDateTime.now();
    }

    public void setAiSummary(String aiSummary) {
        this.aiSummary = aiSummary;
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

    public String getStatus() {
        return status;
    }

    public String getAiSummary() {
        return aiSummary;
    }

    public Long getVersion() {
        return version;
    }

    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
