import json
import re
import logging
import time

from dataclasses import dataclass
from typing import Literal
from urllib.request import Request, ProxyHandler, build_opener

from app.config import (
    GUARD_BASE_URL,
    GUARD_MODEL,
    GUARD_API_KEY,
    GUARD_TIMEOUT,
    GUARD_MAX_ATTEMPTS,
    GUARD_RETRY_DELAY,
)

logger = logging.getLogger(__name__)


@dataclass
class GuardResult:
    decision: Literal["PASS", "REVIEW", "REJECT"]
    safety: str
    categories: list[str]
    raw_output: str = ""
    error: str | None = None


def _needs_review(error: str) -> GuardResult:
    return GuardResult(
        decision="REVIEW",
        safety="UNKNOWN",
        categories=[],
        error=error,
    )


def _call_guard(
    messages: list[dict],
    *,
    direction: Literal["input", "output"],
) -> GuardResult:

    # 长文本不截断后直接放行，后续由分段审查处理
    if any(
        not m["content"].strip() or len(m["content"]) > 12000
        for m in messages
    ):
        return _needs_review("内容为空或过长，需要进一步检查")


    if direction == "output":
        # 输出审查：绕开 GGUF 可能不兼容的聊天模板
        prompt = _build_response_guard_prompt(
            messages[0]["content"],
            messages[1]["content"],
        )

        endpoint = "/api/generate"

        payload = {
            "model": GUARD_MODEL,
            "prompt": prompt,
            "raw": True,
            "stream": False,
            "options": {
                "temperature": 0,
                "num_predict": 128,
            },
        }

    else:
        # 输入审查已验证成功，暂时保持不变
        endpoint = "/api/chat"

        payload = {
            "model": GUARD_MODEL,
            "messages": messages,
            "stream": False,
            "options": {
                "temperature": 0,
                "num_predict": 128,
            },
        }

    req = Request(
        url=f"{GUARD_BASE_URL}{endpoint}",
        data=json.dumps(payload).encode("utf-8"),
        headers=get_guard_headers(),
        method="POST",
    )

    try:
        # 本地服务不经过系统代理
        # 远程服务使用系统默认代理配置
        if GUARD_BASE_URL.startswith((
                "http://127.0.0.1:",
                "http://localhost:",
        )):
            opener = build_opener(ProxyHandler({}))
        else:
            opener = build_opener()

        with opener.open(req, timeout=GUARD_TIMEOUT) as response:
            data = json.loads(response.read().decode("utf-8"))

        if direction == "output":
            raw = data["response"].strip()
        else:
            raw = data["message"]["content"].strip()

    except Exception as exc:
        return _needs_review(f"安全模型调用失败：{exc}")

    lines = [
        line.strip()
        for line in raw.splitlines()
        if line.strip()
    ]

    if len(lines) < 2:
        result = _needs_review("安全模型输出格式不正确")
        result.raw_output = raw
        return result

    match = re.fullmatch(
        r"Safety:\s*(Safe|Controversial|Unsafe)",
        lines[0],
        re.IGNORECASE,
    )

    if not match or not lines[1].lower().startswith("categories:"):
        result = _needs_review("无法解析安全分类")
        result.raw_output = raw
        return result

    safety = match.group(1).capitalize()

    category_text = lines[1].split(":", 1)[1].strip()
    categories = (
        []
        if category_text.lower() in ("none", "")
        else [c.strip() for c in category_text.split(",")]
    )

    if safety == "Safe":
        decision = "PASS"
    elif safety == "Controversial":
        decision = "REVIEW"
    elif direction == "input":
        # 医疗求助、敏感健康信息不能简单拒绝
        decision = "REVIEW"
    else:
        # 不安全的模型回复不允许直接发出
        decision = "REJECT"

    return GuardResult(
        decision=decision,
        safety=safety,
        categories=categories,
        raw_output=raw,
    )


def check_input(user_text: str) -> GuardResult:
    """检查用户输入"""
    return _call_guard(
        [{"role": "user", "content": user_text}],
        direction="input",
    )


def check_output(
    user_text: str,
    assistant_text: str,
) -> GuardResult:
    """结合用户请求审查 Agent 输出"""
    return _call_guard(
        [
            {"role": "user", "content": user_text},
            {"role": "assistant", "content": assistant_text},
        ],
        direction="output",
    )

def _build_response_guard_prompt(
    user_text: str,
    assistant_text: str,
) -> str:
    """
    按 Qwen3Guard 的专用格式构造回复审查请求。
    用于 Ollama /api/generate 的 raw 模式。
    """
    return (
        "<|im_start|>user\n"
        "# Task:\n"
        "1) Evaluate whether THE LAST ASSISTANT's response "
        "is safe or unsafe.\n"
        "2) Identify any unsafe content categories.\n"
        "3) Determine whether the assistant refused the request.\n\n"

        "<BEGIN SAFETY POLICY>\n"
        "Safety: Safe means no prohibited content.\n"
        "Safety: Unsafe means prohibited or harmful content.\n"
        "Safety: Controversial means potentially risky content.\n"
        "<END SAFETY POLICY>\n\n"

        "<BEGIN UNSAFE CONTENT CATEGORIES>\n"
        "Violent; Non-violent Illegal Acts; "
        "Sexual Content or Sexual Acts; PII; "
        "Suicide & Self-Harm; Unethical Acts; "
        "Politically Sensitive Topics; Copyright Violation.\n"
        "<END UNSAFE CONTENT CATEGORIES>\n\n"

        "<BEGIN CONVERSATION>\n\n"
        f"USER: {user_text}\n\n"
        f"ASSISTANT: {assistant_text}\n\n"
        "<END CONVERSATION>\n\n"

        "Provide your safety assessment for ONLY "
        "THE LAST ASSISTANT's response.\n"
        "The first line must be: "
        "Safety: Safe, Unsafe, or Controversial.\n"
        "The second line must be: Categories: ...\n"
        "The third line must be: Refusal: Yes or No.\n"
        "Do not answer the user's question.\n"
        "Do not repeat the assistant response.\n"
        "<|im_end|>\n"
        "<|im_start|>assistant\n"
        "<think>\n\n</think>\n\n"
    )

def guarded_call(
    func,
    *args,
    max_attempts: int = GUARD_MAX_ATTEMPTS,
    retry_delay: float = GUARD_RETRY_DELAY,
):
    """
    安全服务调用失败时有限重试。

    正常的 PASS、REVIEW、REJECT 不重试。
    全部尝试失败后返回 None。
    """

    if max_attempts < 1:
        raise ValueError(
            "max_attempts 必须大于等于 1"
        )

    # 无效参数无需重复调用安全模型
    for arg in args:
        if isinstance(arg, str) and not arg.strip():
            return func(*args)

    for attempt in range(1, max_attempts + 1):
        try:
            result = func(*args)

            if (
                result is not None
                and not result.error
                and result.safety != "UNKNOWN"
                and result.decision in {
                    "PASS",
                    "REVIEW",
                    "REJECT",
                }
            ):
                return result

            logger.warning(
                "安全审查结果无效，第 %d/%d 次",
                attempt,
                max_attempts,
            )

        except Exception as exc:
            logger.warning(
                "安全服务调用异常，第 %d/%d 次：%s",
                attempt,
                max_attempts,
                type(exc).__name__,
            )

        if attempt < max_attempts:
            time.sleep(retry_delay)

    return None
def get_guard_headers() -> dict[str, str]:
    """构造安全模型请求头。"""

    headers = {
        "Content-Type": "application/json",
    }

    if GUARD_API_KEY:
        headers["Authorization"] = (
            f"Bearer {GUARD_API_KEY}"
        )

    return headers