from types import SimpleNamespace
from unittest.mock import Mock

import pytest

from langchain_core.messages import (
    HumanMessage,
    AIMessage,
)

from app import runtime
from tests.plan_fixtures import plan_request

from app.middleware.safety_middleware import (
    SafetyGuardException,
    SafetyServiceException,
)

from app.schemas.health_plan import WeeklyPlan


# ==========================================
# 1. 公共测试数据
# ==========================================

@pytest.fixture
def weekly_plan():
    return WeeklyPlan.model_validate({
        "weekStartDate": "2026-11-30",
        "items": [
            {
                "category": "exercise",
                "title": "每天轻松散步15分钟",
                "scheduledWeekdays": [
                    1, 2, 3, 4, 5, 6, 7
                ],
                "targetQuantity": 15,
                "targetUnit": "分钟",
                "sortOrder": 0,
            }
        ],
    })


@pytest.fixture
def chat_agent():
    """模拟已经通过安全中间件的 Health Agent。"""

    agent = Mock()

    agent.invoke.return_value = {
        "messages": [
            HumanMessage(
                content="如何改善睡眠？"
            ),
            AIMessage(
                content="建议保持规律作息。"
            ),
        ]
    }

    return agent


@pytest.fixture
def plan_agent(weekly_plan):
    """模拟已经通过安全中间件的计划 Agent。"""

    agent = Mock()

    agent.invoke.return_value = {
        "structured_response": weekly_plan,
    }

    return agent


def mock_guard_result(decision):
    return SimpleNamespace(
        decision=decision,
        safety=(
            "Safe"
            if decision == "PASS"
            else "Unsafe"
        ),
        categories=[],
        error=None,
    )


# ==========================================
# 2. Health Agent 正常业务测试
# ==========================================

def test_health_chat_ready(
    monkeypatch,
    chat_agent,
):
    """正常聊天应该返回 READY。"""

    monkeypatch.setattr(
        runtime,
        "extract_health_memory",
        Mock(
            return_value=SimpleNamespace(
                candidates=[]
            )
        ),
    )

    result = runtime.run_health_chat(
        "如何改善睡眠？",
        agent=chat_agent,
    )

    assert result.status == "READY"
    assert result.data == "建议保持规律作息。"
    assert result.memory_candidates == []

    chat_agent.invoke.assert_called_once()

    print("\n正常健康对话：READY")


def test_health_chat_empty_answer(
    monkeypatch,
):
    """未生成有效回复时不能返回 READY。"""

    agent = Mock()

    agent.invoke.return_value = {
        "messages": [
            HumanMessage(content="健康咨询"),
            AIMessage(content=""),
        ]
    }

    memory = Mock()

    monkeypatch.setattr(
        runtime,
        "extract_health_memory",
        memory,
    )

    result = runtime.run_health_chat(
        "健康咨询",
        agent=agent,
    )

    assert result.status == "REVIEW"
    assert result.data is None

    memory.assert_not_called()

    print("\n空回复：成功拦截")


def test_health_chat_agent_error():
    """Agent 执行异常时统一返回 REVIEW。"""

    agent = Mock()

    agent.invoke.side_effect = RuntimeError(
        "Model unavailable"
    )

    result = runtime.run_health_chat(
        "如何改善睡眠？",
        agent=agent,
    )

    assert result.status == "REVIEW"
    assert result.data is None

    print("\nHealth Agent 执行异常：REVIEW")


# ==========================================
# 3. 安全异常统一处理
# ==========================================

@pytest.mark.parametrize(
    "agent_type",
    ["chat", "plan"],
)
@pytest.mark.parametrize(
    "stage, decision",
    [
        ("input", "REVIEW"),
        ("output", "REJECT"),
    ],
)
def test_runtime_safety_exception(
    agent_type,
    stage,
    decision,
):
    """保留安全中间件的真实决策。"""

    agent = Mock()

    agent.invoke.side_effect = (
        SafetyGuardException(
            stage=stage,
            result=mock_guard_result(decision),
        )
    )

    if agent_type == "chat":
        result = runtime.run_health_chat(
            "模拟用户请求",
            agent=agent,
        )
    else:
        result = runtime.run_weekly_plan(
            plan_request("模拟计划请求", mode="MANUAL"),
            agent=agent,
        )

    assert result.status == decision
    assert result.data is None
    assert result.memory_candidates == []

    print(
        f"\nAgent：{agent_type}"
        f"\n安全阶段：{stage}"
        f"\n最终决策：{result.status}"
    )


@pytest.mark.parametrize(
    "agent_type",
    ["chat", "plan"],
)
def test_runtime_safety_service_error(agent_type):
    """安全服务重试耗尽后统一返回 REVIEW。"""

    agent = Mock()

    agent.invoke.side_effect = (
        SafetyServiceException(
            stage="input",
        )
    )

    if agent_type == "chat":
        result = runtime.run_health_chat(
            "健康咨询",
            agent=agent,
        )
    else:
        result = runtime.run_weekly_plan(
            plan_request("生成健康计划", mode="MANUAL"),
            agent=agent,
        )

    assert result.status == "REVIEW"
    assert result.data is None

    print(
        f"\n{agent_type} 安全服务不可用：REVIEW"
    )


# ==========================================
# 4. 健康记忆测试
# ==========================================

def test_memory_extraction_success(
    monkeypatch,
    chat_agent,
):
    """候选记忆通过安全审查后正常返回。"""

    candidate_data = {
        "content": "平时晚上11点睡觉",
    }

    candidate = Mock()

    candidate.model_dump.return_value = (
        candidate_data
    )

    extraction = Mock()

    extraction.candidates = [candidate]

    extraction.model_dump_json.return_value = (
        '{"candidates": []}'
    )

    monkeypatch.setattr(
        runtime,
        "extract_health_memory",
        Mock(return_value=extraction),
    )

    guard = Mock(
        return_value=mock_guard_result("PASS")
    )

    monkeypatch.setattr(
        runtime,
        "guarded_call",
        guard,
    )

    user_text = "平时晚上11点睡觉"

    result = runtime.run_health_chat(
        user_text,
        agent=chat_agent,
    )

    assert result.status == "READY"

    assert result.memory_candidates == [
        candidate_data
    ]

    guard.assert_called_once_with(
        runtime.check_output,
        user_text,
        extraction.model_dump_json.return_value,
    )

    print("\n健康记忆候选：成功返回")


def test_memory_extraction_failure(
    monkeypatch,
    chat_agent,
):
    """记忆提取失败不影响正常聊天。"""

    monkeypatch.setattr(
        runtime,
        "extract_health_memory",
        Mock(
            side_effect=RuntimeError(
                "Memory model unavailable"
            )
        ),
    )

    result = runtime.run_health_chat(
        "如何改善睡眠？",
        agent=chat_agent,
    )

    assert result.status == "READY"
    assert result.data == "建议保持规律作息。"
    assert result.memory_candidates == []
    assert result.messages

    print("\n记忆提取失败：聊天正常返回")


@pytest.mark.parametrize(
    "guard_result",
    [
        None,
        mock_guard_result("REVIEW"),
        mock_guard_result("REJECT"),
    ],
)
def test_memory_guard_failure(
    monkeypatch,
    chat_agent,
    guard_result,
):
    """候选记忆审查失败时丢弃记忆。"""

    extraction = Mock()
    extraction.candidates = [Mock()]

    extraction.model_dump_json.return_value = (
        '{"candidates": []}'
    )

    monkeypatch.setattr(
        runtime,
        "extract_health_memory",
        Mock(return_value=extraction),
    )

    monkeypatch.setattr(
        runtime,
        "guarded_call",
        Mock(return_value=guard_result),
    )

    result = runtime.run_health_chat(
        "如何改善睡眠？",
        agent=chat_agent,
    )

    assert result.status == "READY"
    assert result.data == "建议保持规律作息。"
    assert result.memory_candidates == []
    assert result.messages

    print("\n记忆安全审查失败：仅丢弃候选记忆")


# ==========================================
# 5. Weekly Plan Agent 测试
# ==========================================

def test_weekly_plan_ready(plan_agent):
    """合法计划应通过真实 Plan Guard 校验。"""

    result = runtime.run_weekly_plan(
        plan_request("生成七天健康计划", mode="MANUAL"),
        agent=plan_agent,
    )

    assert result.status == "READY"

    assert result.data["weekStartDate"] == (
        "2026-11-30"
    )

    assert len(result.data["items"]) == 1

    plan_agent.invoke.assert_called_once()

    print("\n合法七天计划：READY")


def test_invalid_plan_mode(plan_agent):
    """不支持的生成模式不能执行 Agent。"""

    result = runtime.run_weekly_plan(
        plan_request("生成健康计划", mode="INVALID_MODE"),
        agent=plan_agent,
    )

    assert result.status == "REVIEW"
    assert result.data is None

    plan_agent.invoke.assert_not_called()

    print("\n无效生成模式：成功拦截")


def test_missing_previous_plan(plan_agent):
    """无反馈自动生成必须提供上一周计划。"""

    result = runtime.run_weekly_plan(
        plan_request("自动生成下一周健康计划", mode="AUTO_NO_FEEDBACK", previous_plan=None),
        agent=plan_agent,
    )

    assert result.status == "REVIEW"
    assert result.data is None

    plan_agent.invoke.assert_not_called()

    print("\n缺少上一周计划：成功拦截")


def test_invalid_structured_response():
    """结构化数据不合法时禁止返回。"""

    agent = Mock()

    agent.invoke.return_value = {
        "structured_response": {
            "invalid": True
        }
    }

    result = runtime.run_weekly_plan(
        plan_request("生成健康计划", mode="MANUAL"),
        agent=agent,
    )

    assert result.status == "REVIEW"
    assert result.data is None

    print("\n无效结构化数据：成功拦截")


def test_plan_guard_reject(plan_agent, weekly_plan):
    """无反馈情况下禁止擅自增加运动量。"""

    previous_data = weekly_plan.model_dump(
        by_alias=True,
        mode="json",
    )

    previous_data["weekStartDate"] = "2026-11-23"

    previous_data["items"][0][
        "targetQuantity"
    ] = 5

    previous_data["items"][0]["title"] = (
        "每天轻松散步5分钟"
    )

    previous_plan = WeeklyPlan.model_validate(
        previous_data
    )

    result = runtime.run_weekly_plan(
        plan_request("自动生成下一周计划", mode="AUTO_NO_FEEDBACK", previous_plan=previous_plan),
        agent=plan_agent,
    )

    assert result.status == "REVIEW"
    assert result.data is None
    assert result.messages

    print(
        "\nPlan Guard 校验失败："
        f"{result.messages}"
    )


def test_plan_agent_error():
    """计划 Agent 执行失败时返回 REVIEW。"""

    agent = Mock()

    agent.invoke.side_effect = RuntimeError(
        "Model unavailable"
    )

    result = runtime.run_weekly_plan(
        plan_request("生成健康计划", mode="MANUAL"),
        agent=agent,
    )

    assert result.status == "REVIEW"
    assert result.data is None

    print("\n计划 Agent 执行异常：REVIEW")