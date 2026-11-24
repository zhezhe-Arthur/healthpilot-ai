
package com.healthpilot.server.healthplan.repository;

import com.healthpilot.server.healthplan.entity.HealthPlanItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HealthPlanItemRepository
        extends JpaRepository<HealthPlanItemEntity, Long> {

    // 按计划ID查询任务，先按排序字段，再按ID升序
    List<HealthPlanItemEntity> findByPlanIdOrderBySortOrderAscIdAsc(
            Long planId
    );

    // 删除指定计划下的所有任务，用于草稿重新生成
    void deleteByPlanId(Long planId);
}
