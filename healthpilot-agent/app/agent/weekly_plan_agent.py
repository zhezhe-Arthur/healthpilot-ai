
from deepagents import create_deep_agent, FilesystemPermission
from langchain.agents.structured_output import ToolStrategy

from langchain_openai import ChatOpenAI

from app.config import settings
from app.agent.health_agent import backend
from app.schemas.health_plan import WeeklyPlan
from app.middleware.safety_middleware import SafetyMiddleware

PLANNING_PROMPT = """
你是 HealthPilot AI 的七天健康计划智能体。

你的职责：
根据用户已确认的健康资料、长期目标、每周反馈和上周计划，
使用专业 Skills 生成结构化的七天健康计划。

生成模式：

1. MANUAL：用户主动请求生成计划。
2. AUTO_WITH_FEEDBACK：周期到期，已有每周反馈。
3. AUTO_NO_FEEDBACK：周期到期，没有收到每周反馈。

核心规则：
- 每份计划从指定的周一开始，覆盖周一至周日。
- 必须采用调用方提供的 weekStartDate，不自行猜测日期。
- 根据健康状况安排合理的运动、饮食、睡眠与心理健康行动。
- 不要求用户每日手动打卡。
- 有反馈时，根据实际完成情况和主观感受调整下一周任务。
- 无反馈时，以最近已确认计划为依据，保守延续或适度简化。
- 不因缺少反馈就认定用户未执行计划。
- 不在缺少依据时自动提高运动强度或增加任务量。
- 不编造健康档案、病史、反馈或实际完成情况。
- 如果关键信息不足，采用保守安排，并说明需要确认的事项。
- 遵守健康安全边界，不提供疾病诊断或处方治疗。

数据规则：
- 严格按照 WeeklyPlan 结构生成结果。
- category、目标数量、单位和日期必须符合业务约束。
- 你只负责生成建议，不保存、确认或修改数据库。
- 所有结果均为待确认草稿，由 Java 服务负责保存 DRAFT。
- 用户确认后计划才正式生效。
"""

weekly_plan_model = ChatOpenAI(
    model=settings.deepseek_model,
    api_key=settings.deepseek_api_key,
    base_url="https://api.deepseek.com",
    temperature=0.3,
    timeout=60,
    max_retries=2,
    extra_body={
        "thinking": {
            "type": "disabled"
        }
    },
)

weekly_plan_agent = create_deep_agent(
    model=weekly_plan_model,
    tools=[],
    subagents=[],
    system_prompt=PLANNING_PROMPT,
    backend=backend,
    skills=["/skills/"],
    permissions=[
        FilesystemPermission(
            operations=["write"],
            paths=["/skills/**"],
            mode="deny",
        ),
    ],
    middleware=[
        SafetyMiddleware(
            structured_output=True,
        ),
    ],
    response_format=ToolStrategy(WeeklyPlan),
)
