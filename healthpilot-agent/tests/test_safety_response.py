from types import SimpleNamespace
from unittest.mock import Mock

import pytest

from app import runtime
from tests.plan_fixtures import plan_request

from app.guardrails.safety_response import (
    resolve_safety_response,
    resolve_service_failure,
)

from app.middleware.safety_middleware import (
    SafetyGuardException,
    SafetyServiceException,
)


@pytest.mark.parametrize(
    "stage, categories, expected_code",
    [
        (
            "input",
            ["Suicide & Self-Harm"],
            "CRISIS_SUPPORT",
        ),
        (
            "input",
            ["Violent"],
            "VIOLENCE_RISK",
        ),
        (
            "output",
            ["Unethical Acts"],
            "UNSAFE_OUTPUT",
        ),
        (
            "output",
            ["Non-violent Illegal Acts"],
            "UNSAFE_OUTPUT",
        ),
        (
            "input",
            ["Unknown"],
            "SAFETY_REVIEW",
        ),
        (
            "input",
            [],
            "SAFETY_REVIEW",
        ),
        (
            "input",
            ["Violent", "Suicide & Self-Harm"],
            "CRISIS_SUPPORT",
        ),
        (
            "input",
            [" suicide & self-harm "],
            "CRISIS_SUPPORT",
        ),
    ],
)
def test_safety_response(
    stage,
    categories,
    expected_code,
):
    response = resolve_safety_response(
        stage,
        categories,
    )

    assert response.code == expected_code
    assert response.message

    print(
        f"\n风险类别：{categories}"
        f"\n处理类型：{response.code}"
        f"\n安全回复：{response.message}"
    )


def test_service_failure_response():
    response = resolve_service_failure()

    assert response.code == (
        "SAFETY_SERVICE_UNAVAILABLE"
    )

    assert response.message

    print(f"\n服务降级：{response.message}")


def test_runtime_crisis_support(monkeypatch):
    """自杀风险触发安全求助提示。"""

    agent = Mock()

    memory = Mock()

    monkeypatch.setattr(
        runtime,
        "extract_health_memory",
        memory,
    )

    agent.invoke.side_effect = (
        SafetyGuardException(
            stage="input",
            result=SimpleNamespace(
                decision="REVIEW",
                categories=["Suicide & Self-Harm"],
            ),
        )
    )

    result = runtime.run_health_chat(
        "模拟危机求助",
        agent=agent,
    )

    assert result.status == "REVIEW"
    assert result.data is None
    assert result.memory_candidates == []
    assert "急救" in result.messages[0]

    memory.assert_not_called()

    print(f"\n危机干预：{result.messages}")


@pytest.mark.parametrize(
    "agent_type",
    ["chat", "plan"],
)
def test_runtime_service_failure(agent_type):
    agent = Mock()

    agent.invoke.side_effect = (
        SafetyServiceException(
            stage="input"
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
    assert "暂时不可用" in result.messages[0]

    print(
        f"\n{agent_type} 服务降级："
        f"{result.messages}"
    )


def test_runtime_output_rejected():
    """危险输出不能以业务数据形式返回。"""

    agent = Mock()

    agent.invoke.side_effect = (
        SafetyGuardException(
            stage="output",
            result=SimpleNamespace(
                decision="REJECT",
                categories=["Unethical Acts"],
            ),
        )
    )

    result = runtime.run_health_chat(
        "健康咨询",
        agent=agent,
    )

    assert result.status == "REJECT"
    assert result.data is None
    assert result.memory_candidates == []
    assert "未通过安全审查" in result.messages[0]

    print(f"\n危险输出降级：{result.messages}")