from types import SimpleNamespace
from unittest.mock import Mock

import pytest
from langchain_core.messages import HumanMessage, AIMessage

from app.middleware import safety_middleware

from app.middleware.safety_middleware import (
    SafetyMiddleware,
    SafetyGuardException,
)

from app.schemas.health_plan import WeeklyPlan
from app.middleware.safety_middleware import (
    SafetyServiceException,
)

def mock_guard_result(
    decision="PASS",
    safety="Safe",
    categories=None,
    error=None,
):
    """构造模拟的安全审查结果"""

    return SimpleNamespace(
        decision=decision,
        safety=safety,
        categories=categories or [],
        raw_output="Mock response",
        error=error,
    )


@pytest.fixture
def weekly_plan():
    """构造结构化健康计划"""

    return WeeklyPlan.model_validate({
        "weekStartDate": "2026-11-30",
        "items": [
            {
                "category": "exercise",
                "title": "每天轻松散步15分钟",
                "scheduledWeekdays": [1, 2, 3, 4, 5, 6, 7],
                "targetQuantity": 15,
                "targetUnit": "分钟",
                "sortOrder": 0,
            }
        ],
    })


def test_input_pass(monkeypatch):
    """正常输入应该放行"""

    guard = Mock(
        return_value=mock_guard_result()
    )

    monkeypatch.setattr(
        safety_middleware,
        "check_input",
        guard,
    )

    middleware = SafetyMiddleware()

    state = {
        "messages": [
            HumanMessage(content="如何改善睡眠？")
        ]
    }

    result = middleware.before_agent(state, None)

    guard.assert_called_once_with("如何改善睡眠？")

    assert result is None

    print("\n正常输入：成功放行")


def test_input_review(monkeypatch):
    """危险输入应该中止执行"""

    monkeypatch.setattr(
        safety_middleware,
        "check_input",
        Mock(
            return_value=mock_guard_result(
                decision="REVIEW",
                safety="Unsafe",
                categories=["Violent"],
            )
        ),
    )

    middleware = SafetyMiddleware()

    state = {
        "messages": [
            HumanMessage(content="模拟危险输入")
        ]
    }

    with pytest.raises(SafetyGuardException) as exc:
        middleware.before_agent(state, None)

    assert exc.value.stage == "input"
    assert exc.value.result.decision == "REVIEW"

    print("\n危险输入：成功拦截")


def test_text_output_pass(monkeypatch):
    """正常自然语言输出应该放行"""

    guard = Mock(
        return_value=mock_guard_result()
    )

    monkeypatch.setattr(
        safety_middleware,
        "check_output",
        guard,
    )

    middleware = SafetyMiddleware(
        structured_output=False
    )

    state = {
        "messages": [
            HumanMessage(content="如何改善睡眠？"),
            AIMessage(content="建议保持规律作息。"),
        ]
    }

    result = middleware.after_agent(state, None)

    guard.assert_called_once_with(
        "如何改善睡眠？",
        "建议保持规律作息。",
    )

    assert result is None

    print("\n正常自然语言输出：成功放行")


def test_text_output_reject(monkeypatch):
    """危险自然语言输出应该被拦截"""

    monkeypatch.setattr(
        safety_middleware,
        "check_output",
        Mock(
            return_value=mock_guard_result(
                decision="REJECT",
                safety="Unsafe",
                categories=["Unethical Acts"],
            )
        ),
    )

    middleware = SafetyMiddleware(
        structured_output=False
    )

    state = {
        "messages": [
            HumanMessage(content="健康咨询"),
            AIMessage(content="模拟不安全的医疗建议"),
        ]
    }

    with pytest.raises(SafetyGuardException) as exc:
        middleware.after_agent(state, None)

    assert exc.value.stage == "output"
    assert exc.value.result.decision == "REJECT"

    print("\n危险自然语言输出：成功拦截")


def test_structured_output_pass(
    monkeypatch,
    weekly_plan,
):
    """结构化输出应该被正确序列化并审查"""

    guard = Mock(
        return_value=mock_guard_result()
    )

    monkeypatch.setattr(
        safety_middleware,
        "check_output",
        guard,
    )

    middleware = SafetyMiddleware(
        structured_output=True
    )

    state = {
        "messages": [
            HumanMessage(content="生成七天健康计划")
        ],
        "structured_response": weekly_plan,
    }

    result = middleware.after_agent(state, None)

    guard.assert_called_once_with(
        "生成七天健康计划",
        weekly_plan.model_dump_json(by_alias=True),
    )

    assert result is None

    print("\n正常结构化输出：成功放行")


def test_structured_output_reject(
    monkeypatch,
    weekly_plan,
):
    """安全审查拒绝时，结构化计划不能返回"""

    guard = Mock(
        return_value=mock_guard_result(
            decision="REJECT",
            safety="Unsafe",
        )
    )

    monkeypatch.setattr(
        safety_middleware,
        "check_output",
        guard,
    )

    middleware = SafetyMiddleware(
        structured_output=True
    )

    state = {
        "messages": [
            HumanMessage(content="生成七天健康计划")
        ],
        "structured_response": weekly_plan,
    }

    with pytest.raises(SafetyGuardException) as exc:
        middleware.after_agent(state, None)

    guard.assert_called_once_with(
        "生成七天健康计划",
        weekly_plan.model_dump_json(by_alias=True),
    )

    assert exc.value.stage == "output"
    assert exc.value.result.decision == "REJECT"

    print("\n结构化输出被判定为危险：成功拦截")


def test_missing_structured_output(monkeypatch):
    """结构化输出缺失时不能放行"""

    guard = Mock(
        return_value=mock_guard_result(
            decision="REVIEW",
            safety="UNKNOWN",
            error="Empty output",
        )
    )

    monkeypatch.setattr(
        safety_middleware,
        "check_output",
        guard,
    )

    middleware = SafetyMiddleware(
        structured_output=True
    )

    state = {
        "messages": [
            HumanMessage(content="生成七天健康计划")
        ]
    }

    with pytest.raises(SafetyGuardException) as exc:
        middleware.after_agent(state, None)

    guard.assert_called_once_with(
        "生成七天健康计划",
        "",
    )

    assert exc.value.stage == "output"
    assert exc.value.result.decision == "REVIEW"

    print("\n结构化输出缺失：成功拦截")


@pytest.mark.parametrize("stage", ["input", "output"])
def test_guard_service_unavailable(
    monkeypatch,
    stage,
):
    """验证安全服务连续三次失败。"""

    guard = Mock(
        return_value=mock_guard_result(
            decision="REVIEW",
            safety="UNKNOWN",
            error="Connection refused",
        )
    )

    monkeypatch.setattr(
        safety_middleware,
        f"check_{stage}",
        guard,
    )

    middleware = SafetyMiddleware()

    state = {
        "messages": [
            HumanMessage(content="健康咨询"),
            AIMessage(content="建议保持规律作息。"),
        ]
    }

    with pytest.raises(SafetyServiceException) as exc:

        if stage == "input":
            middleware.before_agent(state, None)
        else:
            middleware.after_agent(state, None)

    assert exc.value.stage == stage

    # 验证确实执行了三次尝试
    assert guard.call_count == 3

    print(
        f"\n安全服务故障：{stage}"
        f"\n实际尝试次数：{guard.call_count}"
        f"\n最终结果：成功拦截"
    )

@pytest.mark.parametrize("stage", ["input", "output"])
def test_guard_retry_success(monkeypatch, stage):
    """前两次调用失败，第三次成功。"""

    failure = mock_guard_result(
        decision="REVIEW",
        safety="UNKNOWN",
        error="Connection refused",
    )

    success = mock_guard_result(
        decision="PASS",
        safety="Safe",
    )

    guard = Mock(
        side_effect=[
            failure,
            failure,
            success,
        ]
    )

    monkeypatch.setattr(
        safety_middleware,
        f"check_{stage}",
        guard,
    )

    middleware = SafetyMiddleware()

    state = {
        "messages": [
            HumanMessage(content="如何改善睡眠？"),
            AIMessage(content="建议保持规律作息。"),
        ]
    }

    if stage == "input":
        result = middleware.before_agent(state, None)
    else:
        result = middleware.after_agent(state, None)

    assert guard.call_count == 3
    assert result is None

    print(
        f"\n安全服务恢复：{stage}"
        f"\n实际尝试次数：{guard.call_count}"
        f"\n最终结果：成功放行"
    )