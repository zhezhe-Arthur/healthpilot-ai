import os

import pytest

from app.agent.weekly_plan_agent import weekly_plan_agent
from app.schemas.health_plan import WeeklyPlan
from app.guardrails.plan_guard import check_weekly_plan


@pytest.mark.skipif(
    os.getenv("RUN_LLM_TESTS") != "1",
    reason="需要主动开启真实模型调用",
)
def test_weekly_plan_agent_generation():
    result = weekly_plan_agent.invoke({
        "messages": [
            {
                "role": "user",
                "content": """
生成模式：AUTO_NO_FEEDBACK
weekStartDate：2026-11-30

模拟用户资料：
- 成年人
- 久坐，日常活动量较低
- 希望改善睡眠和增加日常活动
- 上周没有提交反馈
- 上周计划包含每周三次轻松步行，每次15分钟

请参考相关 Skills，保守生成下一周计划。
""",
            }
        ]
    })

    plan = result["structured_response"]

    assert isinstance(plan, WeeklyPlan)
    assert plan.week_start_date.isoformat() == "2026-11-30"
    assert len(plan.items) > 0

    print(plan.model_dump_json(by_alias=True, indent=2))



def make_guard_plan(week_start, extra_walk=False):
    items = [
        {
            "category": "exercise",
            "title": "轻松步行15分钟",
            "scheduledWeekdays": [1, 3, 5],
            "targetQuantity": 15,
            "targetUnit": "分钟/次",
            "sortOrder": 0,
        },
        {
            "category": "sleep",
            "title": "睡前放松",
            "scheduledWeekdays": [1, 2, 3, 4, 5, 6, 7],
            "targetQuantity": 20,
            "targetUnit": "分钟/天",
            "sortOrder": 1,
        },
    ]

    if extra_walk:
        items.append({
            "category": "exercise",
            "title": "可选：额外步行10分钟",
            "scheduledWeekdays": [2, 4, 6],
            "targetQuantity": 10,
            "targetUnit": "分钟/次",
            "sortOrder": 2,
        })

    return WeeklyPlan.model_validate({
        "weekStartDate": week_start,
        "items": items,
    })


def test_plan_guard_conservative_update():
    """无反馈，保持运动负荷，应当通过"""
    previous = make_guard_plan("2026-11-23")
    current = make_guard_plan("2026-11-30")

    result = check_weekly_plan(
        current,
        mode="AUTO_NO_FEEDBACK",
        previous_plan=previous,
    )

    assert result.passed


def test_plan_guard_rejects_increased_workload():
    """无反馈，擅自增加运动时长，应当拦截"""
    previous = make_guard_plan("2026-11-23")
    current = make_guard_plan(
        "2026-11-30",
        extra_walk=True,
    )

    result = check_weekly_plan(
        current,
        mode="AUTO_NO_FEEDBACK",
        previous_plan=previous,
    )

    assert not result.passed
    assert any("运动时长" in e for e in result.errors)


def test_plan_guard_requires_previous_plan():
    """无反馈时，不能缺少上一份确认计划"""
    current = make_guard_plan("2026-11-30")

    result = check_weekly_plan(
        current,
        mode="AUTO_NO_FEEDBACK",
    )

    assert not result.passed
