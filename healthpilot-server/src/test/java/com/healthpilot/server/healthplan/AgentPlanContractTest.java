package com.healthpilot.server.healthplan;

import com.healthpilot.server.healthplan.dto.SaveHealthPlanDraftRequest;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

class AgentPlanContractTest {
    @Test
    void pythonDraftDeserializesAndPassesActualJavaConstraints() throws Exception {
        // Python 转换测试与本测试共用此契约样例，无需连接数据库。
        try (var input = getClass().getResourceAsStream("/contracts/agent-weekly-plan.json");
             var factory = Validation.buildDefaultValidatorFactory()) {
            assertNotNull(input);
            var request = JsonMapper.builder().build()
                    .readValue(input, SaveHealthPlanDraftRequest.class);
            var violations = factory.getValidator().validate(request);
            assertTrue(violations.isEmpty(), violations.toString());
            assertEquals(DayOfWeek.MONDAY, request.weekStartDate().getDayOfWeek());
            assertEquals(5, request.items().size());
            for (var item : request.items()) {
                var days = item.scheduledWeekdays().split(",");
                assertEquals(days.length, new HashSet<>(Arrays.asList(days)).size());
            }
        }
    }
}
