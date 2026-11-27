"""使用虚构资料验证两个 Runtime 入口，不写 Java 数据库。"""
import os

import pytest

from app.runtime import run_health_chat, run_weekly_plan
from tests.plan_fixtures import plan_request
from tests.test_plan_contract import plan_data

pytestmark = pytest.mark.skipif(os.getenv("RUN_LLM_TESTS") != "1", reason="需要主动开启真实模型调用")


class RecordingAgent:
    def __init__(self, agent):
        self.agent = agent
        self.result = None

    def invoke(self, state):
        self.result = self.agent.invoke(state)
        return self.result

    def assert_skill_read(self):
        messages = self.result["messages"]
        calls = {
            call["id"]
            for message in messages
            for call in getattr(message, "tool_calls", [])
            if call["name"] == "read_file" and "/skills/" in str(call["args"])
        }
        assert any(
            getattr(message, "tool_call_id", None) in calls
            and getattr(message, "status", "success") == "success"
            and "name: " in str(message.content)
            for message in messages
        ), "未观察到成功读取 Skill 入口文件"


def test_live_chat_runtime():
    from app.agent.health_agent import health_agent
    agent = RecordingAgent(health_agent)
    result = run_health_chat(
        "这是虚构资料的集成测试，无需记录个人信息。请先读取 /skills/weekly-planning/SKILL.md，"
        "简短解释 HealthPilot 是否要求每日手动打卡，以及计划如何确认。",
        agent=agent,
    )
    assert result.status == "READY", result.messages
    assert result.data
    assert not result.messages, "候选记忆提取或审查未完成"
    agent.assert_skill_read()


def test_live_plan_runtime():
    from app.agent.weekly_plan_agent import weekly_plan_agent
    agent = RecordingAgent(weekly_plan_agent)
    result = run_weekly_plan(plan_request(
        "这是虚构资料的集成测试。先读取 /skills/weekly-planning/SKILL.md 和共享缺失反馈规则。"
        "仅原样延续 previousPlan 中的一项轻松散步安排到指定目标周，保持标题、数量、单位和执行日不变。",
        mode="AUTO_NO_FEEDBACK", previous_plan=plan_data("2026-11-23"),
        confirmedProfile={"age": 36, "activityLevel": "LOW", "restrictions": [],
                          "confirmedRoutine": "已确认可沿用每天轻松散步15分钟"},
    ), agent=agent)
    assert result.status == "READY", result.messages
    assert result.data["weekStartDate"] == "2026-11-30"
    assert result.data["items"][0]["category"] == "BODY"
    assert isinstance(result.data["items"][0]["scheduledWeekdays"], str)
    agent.assert_skill_read()
