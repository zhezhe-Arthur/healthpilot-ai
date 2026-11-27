import json
from datetime import date
from decimal import Decimal
from pathlib import Path
from unittest.mock import Mock

import pytest
from pydantic import ValidationError

from app.runtime import run_weekly_plan
from app.schemas.health_plan import WeeklyPlan, WeeklyPlanRequest, to_java_draft
from app.guardrails.plan_guard import check_weekly_plan, exercise_minutes
from tests.plan_fixtures import plan_request


def plan_data(week="2026-11-30", category="exercise", quantity=15):
    return {
        "weekStartDate": week,
        "items": [{
            "category": category, "title": "轻松散步",
            "scheduledWeekdays": [1, 2, 3, 4, 5, 6, 7],
            "targetQuantity": quantity, "targetUnit": "分钟", "sortOrder": 0,
        }],
    }


@pytest.mark.parametrize("mode", ["MANUAL", "AUTO_WITH_FEEDBACK", "AUTO_NO_FEEDBACK"])
def test_agent_receives_complete_request(mode):
    previous = plan_data("2026-11-23")
    feedback = {"completedQuantity": 60, "experience": "较轻松"} if mode == "AUTO_WITH_FEEDBACK" else None
    request = WeeklyPlanRequest.model_validate(plan_request(
        "只沿用已确认安排", mode=mode, previous_plan=previous, weeklyFeedback=feedback,
    ))
    agent = Mock()
    agent.invoke.return_value = {"structured_response": plan_data()}
    result = run_weekly_plan(request, agent=agent)
    assert result.status == "READY"
    message = agent.invoke.call_args.args[0]["messages"][0]
    assert message["role"] == "user"
    actual = json.loads(message["content"])
    assert actual == request.model_dump(by_alias=True, mode="json")
    assert actual["mode"] == mode
    assert actual["weekStartDate"] == "2026-11-30"
    assert actual["confirmedProfile"] == request.confirmed_profile
    assert actual["healthGoals"] == request.health_goals
    assert actual["weeklyFeedback"] == feedback
    assert actual["previousPlan"]["items"][0]["targetQuantity"] == "15"
    assert result.data["items"][0]["category"] == "BODY"
    assert result.data["items"][0]["scheduledWeekdays"] == "1,2,3,4,5,6,7"


@pytest.mark.parametrize("week", ["2026-11-23", "2026-12-07"])
def test_other_monday_is_rejected(week):
    agent = Mock()
    agent.invoke.return_value = {"structured_response": plan_data(week)}
    result = run_weekly_plan(plan_request(), agent=agent)
    assert result.status == "REVIEW"
    assert result.data is None
    assert any("目标周" in message for message in result.messages)


@pytest.mark.parametrize("changes", [
    {"weekStartDate": "2026-12-01"},
    {"weekStartDate": None},
    {"mode": "AUTO_WITH_FEEDBACK"},
    {"mode": "AUTO_NO_FEEDBACK", "previousPlan": plan_data("2026-11-23"), "weeklyFeedback": {}},
    {"previousPlan": plan_data("2026-11-30")},
    {"previousPlan": plan_data("2026-12-07")},
])
def test_invalid_context_does_not_call_agent(changes):
    agent = Mock()
    result = run_weekly_plan(plan_request(**changes), agent=agent)
    assert result.status == "REVIEW"
    assert result.data is None
    agent.invoke.assert_not_called()


@pytest.mark.parametrize("old_category,new_category", [("exercise", "BODY"), ("BODY", "exercise"), ("BODY", "BODY")])
def test_category_alias_cannot_bypass_no_feedback_guard(old_category, new_category):
    old = WeeklyPlan.model_validate(plan_data("2026-11-23", old_category))
    new = WeeklyPlan.model_validate(plan_data(category=new_category, quantity=120))
    assert exercise_minutes(old) == 105
    assert exercise_minutes(new) == 840
    result = check_weekly_plan(new, mode="AUTO_NO_FEEDBACK", previous_plan=old)
    assert not result.passed
    agent = Mock()
    agent.invoke.return_value = {"structured_response": new}
    result = run_weekly_plan(plan_request(mode="AUTO_NO_FEEDBACK", previous_plan=old), agent=agent)
    assert result.status == "REVIEW"
    assert result.data is None


def test_unknown_category_cannot_be_counted_as_zero():
    data = plan_data(category="unknown")
    with pytest.raises(ValidationError):
        WeeklyPlan.model_validate(data)
    agent = Mock()
    agent.invoke.return_value = {"structured_response": data}
    assert run_weekly_plan(plan_request(), agent=agent).status == "REVIEW"


def test_uncomparable_unit_requires_review():
    old = WeeklyPlan.model_validate(plan_data("2026-11-23"))
    data = plan_data()
    data["items"][0]["targetUnit"] = "组"
    result = check_weekly_plan(WeeklyPlan.model_validate(data), mode="AUTO_NO_FEEDBACK", previous_plan=old)
    assert not result.passed


@pytest.mark.parametrize("changes", [
    {"title": "x" * 151}, {"title": "😀" * 76}, {"title": "  "},
    {"targetQuantity": None, "targetUnit": None},
    {"targetQuantity": "0.001"}, {"targetQuantity": "1000000"},
    {"targetQuantity": "1.001"}, {"targetQuantity": "NaN"}, {"targetQuantity": "Infinity"},
    {"targetUnit": "x" * 21}, {"targetUnit": " "},
    {"scheduledWeekdays": [1, 1]}, {"scheduledWeekdays": [8]},
    {"sortOrder": 2147483648},
])
def test_unsavable_item_is_rejected(changes):
    data = plan_data()
    data["items"][0].update(changes)
    with pytest.raises(ValidationError):
        WeeklyPlan.model_validate(data)


def test_plan_item_limit():
    data = plan_data()
    data["items"] *= 41
    with pytest.raises(ValidationError):
        WeeklyPlan.model_validate(data)


def test_java_contract_fixture_matches_python_adapter():
    # 同一份 JSON 由 Java 的真实 DTO + Bean Validation 测试读取。
    fixture = Path(__file__).resolve().parents[2] / "healthpilot-server/src/test/resources/contracts/agent-weekly-plan.json"
    expected = json.loads(fixture.read_text(encoding="utf-8"))
    data = plan_data()
    item = data["items"][0]
    data["items"] = [dict(item, category=category, sortOrder=index, targetQuantity=quantity)
                     for index, (category, quantity) in enumerate([
                         ("exercise", "15.25"), ("mental_health", "0.01"),
                         ("nutrition", "999999.99"), ("sleep", "20"), ("goal", "1"),
                     ])]
    draft = to_java_draft(WeeklyPlan.model_validate(data))
    assert json.loads(json.dumps(draft)) == expected
    assert Decimal(str(draft["items"][2]["targetQuantity"])) == Decimal("999999.99")
