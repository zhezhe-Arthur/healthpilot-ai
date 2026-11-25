from deepagents import (
    create_deep_agent,
    GeneralPurposeSubagentProfile,
    HarnessProfile,
    register_harness_profile,
)
from langchain_openai import ChatOpenAI

from app.config import settings


# 1. 初始化 DeepSeek 模型
model = ChatOpenAI(
    model=settings.deepseek_model,
    api_key=settings.deepseek_api_key,
    base_url="https://api.deepseek.com",
    temperature=0.3,
    timeout=60,
    max_retries=2,
)


# 2. 禁用 DeepAgents 默认的通用子 Agent
register_harness_profile(
    "openai",
    HarnessProfile(
        general_purpose_subagent=GeneralPurposeSubagentProfile(
            enabled=False
        )
    ),
)


# 3. 定义健康智能体角色
SYSTEM_PROMPT = """
你是 HealthPilot AI 的健康计划智能体。

你的职责：
1. 根据用户的健康档案和健康目标制定合理的健康计划。
2. 结合用户每周反馈调整下一周计划。
3. 关注运动、营养、睡眠和心理健康。
4. 只提供一般健康管理建议，不进行疾病诊断或处方治疗。
5. 健康信息不足时，不要编造用户数据。
6. 始终使用中文回答。

业务规则：
- 每份计划覆盖连续七天。
- 用户只需要每周反馈，不要求每日打卡。
- 所有生成的计划都必须等待用户确认才能生效。
"""


# 4. 创建单 Agent
health_agent = create_deep_agent(
    model=model,
    tools=[],
    subagents=[],
    system_prompt=SYSTEM_PROMPT,
)