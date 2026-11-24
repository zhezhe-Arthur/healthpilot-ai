
package com.healthpilot.server.weeklyassessment.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "health_weekly_feedbacks")
public class WeeklyFeedbackEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "week_start_date", nullable = false)
    private LocalDate weekStartDate;

    @Column(name = "execution", nullable = false, length = 16)
    private String execution;

    @Column(name = "sleep", nullable = false, length = 16)
    private String sleep;

    @Column(name = "feeling", nullable = false, length = 16)
    private String feeling;

    @Column(name = "note", length = 1000)
    private String note;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected WeeklyFeedbackEntity() {
    }

    public WeeklyFeedbackEntity(
            Long userId,
            LocalDate weekStartDate,
            String execution,
            String sleep,
            String feeling,
            String note
    ) {
        this.userId = userId;
        this.weekStartDate = weekStartDate;
        this.execution = execution;
        this.sleep = sleep;
        this.feeling = feeling;
        this.note = note;
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

    public String getExecution() {
        return execution;
    }

    public void setExecution(String execution) {
        this.execution = execution;
    }

    public String getSleep() {
        return sleep;
    }

    public void setSleep(String sleep) {
        this.sleep = sleep;
    }

    public String getFeeling() {
        return feeling;
    }

    public void setFeeling(String feeling) {
        this.feeling = feeling;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
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
