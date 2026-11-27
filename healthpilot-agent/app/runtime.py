import logging
from dataclasses import dataclass, field
from typing import Any

from app.guardrails.llm_guard import (
    check_output,
    guarded_call,
)
from app.guardrails.plan_guard import (
    check_weekly_plan,
    MODES,
)
from app.schemas.health_plan import WeeklyPlan
from app.agent.health_agent import extract_health_memory

from app.middleware.safety_middleware import (
    SafetyGuardException,
    SafetyServiceException,
)
from app.guardrails.safety_response import (
    resolve_safety_response,
    resolve_service_failure,
)

logger = logging.getLogger(__name__)


@dataclass
class RuntimeResult:
    """统一运行结果。"""

    status: str
    data: Any = None
    messages: list[str] = field(default_factory=list)
    memory_candidates: list[dict] = field(
        default_factory=list
    )


def review(reason: str) -> RuntimeResult:
    """构造需要进一步处理的结果。"""

    return RuntimeResult(
        status="REVIEW",
        messages=[reason],
    )


def handle_safety_exception(
    exc: SafetyGuardException,
) -> RuntimeResult:
    """统一处理安全审查异常。"""

    logger.warning(
        "安全审查未通过：stage=%s, decision=%s",
        exc.stage,
        exc.result.decision,
    )

    response = resolve_safety_response(
        stage=exc.stage,
        categories=exc.result.categories,
    )

    return RuntimeResult(
        status=exc.result.decision,
        data=None,
        messages=[response.message],
    )

def handle_safety_service_exception(
    exc: SafetyServiceException,
) -> RuntimeResult:
    """处理安全服务重试耗尽的情况。"""

    logger.warning(
        "安全服务不可用：stage=%s",
        exc.stage,
    )

    response = resolve_service_failure()

    return RuntimeResult(
        status="REVIEW",
        data=None,
        messages=[response.message],
    )

def extract_final_answer(result: dict) -> str | None:
    """获取当前轮次最终的自然语言回复。"""

    for message in reversed(result.get("messages", [])):

        message_type = getattr(message, "type", "")

        # 不读取上一轮对话中的回复
        if message_type == "human":
            break

        if message_type != "ai":
            continue

        # 跳过工具调用消息
        if getattr(message, "tool_calls", None):
            continue

        if (
            isinstance(message.content, str)
            and message.content.strip()
        ):
            return message.content

    return None


def extract_memory_candidates(
    user_text: str,
) -> tuple[list[dict], list[str]]:
    """
    提取健康记忆候选。

    记忆提取失败不影响已经通过审查的聊天回复。
    """

    try:
        extraction = extract_health_memory(user_text)

        if not extraction.candidates:
            return [], []

        # 记忆提取独立于 Agent，因此需要单独审查
        memory_guard = guarded_call(
            check_output,
            user_text,
            extraction.model_dump_json(
                by_alias=True
            ),
        )

        if (
            memory_guard is None
            or memory_guard.decision != "PASS"
        ):
            return [], [
                "候选记忆未通过安全审查，暂不提交"
            ]

        candidates = [
            item.model_dump(
                by_alias=True,
                mode="json",
            )
            for item in extraction.candidates
        ]

        return candidates, []

    except Exception:
        logger.exception("健康记忆提取异常")

        return [], [
            "本次健康记忆提取未成功"
        ]


def run_health_chat(
    user_text: str,
    agent=None,
) -> RuntimeResult:
    """Health Agent 自然对话入口。"""

    try:
        if agent is None:
            from app.agent.health_agent import (
                health_agent,
            )

            agent = health_agent

        # 1. 执行 Agent
        # 输入、输出审查均由 SafetyMiddleware 完成
        result = agent.invoke({
            "messages": [
                {
                    "role": "user",
                    "content": user_text,
                }
            ]
        })

        # 2. 提取最终回复
        answer = extract_final_answer(result)

        if not answer:
            return review("未获得有效的对话回复")


    except SafetyGuardException as exc:
        return handle_safety_exception(exc)


    except SafetyServiceException as exc:
        return handle_safety_service_exception(exc)

    except Exception:
        logger.exception("Health Agent 执行异常")

        return review("Health Agent 执行异常")

    # 3. 提取候选健康记忆
    memory_candidates, memory_messages = (
        extract_memory_candidates(user_text)
    )

    # 4. 返回已经通过审查的回复
    return RuntimeResult(
        status="READY",
        data=answer,
        messages=memory_messages,
        memory_candidates=memory_candidates,
    )


def run_weekly_plan(
    user_text: str,
    mode: str,
    previous_plan: WeeklyPlan | None = None,
    agent=None,
) -> RuntimeResult:
    """Weekly Plan Agent 七天计划生成入口。"""

    # 1. 验证业务参数
    if mode not in MODES:
        return review("不支持的计划生成模式")

    if (
        mode == "AUTO_NO_FEEDBACK"
        and previous_plan is None
    ):
        return review(
            "无反馈自动生成缺少上一份确认计划"
        )

    try:
        if agent is None:
            from app.agent.weekly_plan_agent import (
                weekly_plan_agent,
            )

            agent = weekly_plan_agent

        # 2. 执行 Agent
        # 中间件负责输入与结构化输出安全审查
        result = agent.invoke({
            "messages": [
                {
                    "role": "user",
                    "content": user_text,
                }
            ]
        })

        # 3. 验证结构化数据
        plan = WeeklyPlan.model_validate(
            result["structured_response"]
        )
    except SafetyGuardException as exc:
        return handle_safety_exception(exc)

    except SafetyServiceException as exc:
        return handle_safety_service_exception(exc)


    except Exception:
        logger.exception("计划生成或结构化解析失败")

        return review("计划生成或结构化解析失败")

    # 4. 确定性业务规则校验
    try:
        rule_result = check_weekly_plan(
            plan,
            mode=mode,
            previous_plan=previous_plan,
        )

    except Exception:
        logger.exception("计划业务规则校验异常")

        return review("计划业务规则校验失败")

    if not rule_result.passed:
        return RuntimeResult(
            status="REVIEW",
            messages=rule_result.errors,
        )

    # 5. 返回合格的候选计划
    # Runtime 不直接写数据库，也不确认计划生效
    return RuntimeResult(
        status="READY",
        data=plan.model_dump(
            by_alias=True,
            mode="json",
        ),
        messages=rule_result.warnings,
    )