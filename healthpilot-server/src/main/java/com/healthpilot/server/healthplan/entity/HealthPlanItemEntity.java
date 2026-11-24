
package com.healthpilot.server.healthplan.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "health_plan_items")
public class HealthPlanItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @Column(nullable = false, length = 20)
    private String category;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 500)
    private String description;

    @Column(name = "scheduled_weekdays", nullable = false, length = 20)
    private String scheduledWeekdays;

    @Column(name = "target_quantity", nullable = false,
            precision = 8, scale = 2)
    private BigDecimal targetQuantity;

    @Column(name = "target_unit", nullable = false, length = 20)
    private String targetUnit;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected HealthPlanItemEntity() {
    }

    public HealthPlanItemEntity(
            Long planId,
            String category,
            String title,
            String description,
            String scheduledWeekdays,
            BigDecimal targetQuantity,
            String targetUnit,
            Integer sortOrder
    ) {
        this.planId = planId;
        this.category = category;
        this.title = title;
        this.description = description;
        this.scheduledWeekdays = scheduledWeekdays;
        this.targetQuantity = targetQuantity;
        this.targetUnit = targetUnit;
        this.sortOrder = sortOrder;
    }

    public Long getId() {
        return id;
    }

    public Long getPlanId() {
        return planId;
    }

    public String getCategory() {
        return category;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getScheduledWeekdays() {
        return scheduledWeekdays;
    }

    public BigDecimal getTargetQuantity() {
        return targetQuantity;
    }

    public String getTargetUnit() {
        return targetUnit;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
