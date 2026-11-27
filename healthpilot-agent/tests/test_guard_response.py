"""完整分类响应的离线 HTTP 回归，不调用真实模型。"""
import json
from unittest.mock import MagicMock

import pytest

from app.guardrails import llm_guard
from app.middleware.safety_middleware import SafetyMiddleware, SafetyServiceException
from langchain_core.messages import HumanMessage, AIMessage


def mock_response(monkeypatch, raw):
    response = MagicMock()
    response.__enter__.return_value.read.return_value = json.dumps({
        "message": {"content": raw}, "response": raw,
    }).encode("utf-8")
    opener = MagicMock()
    opener.open.return_value = response
    monkeypatch.setattr(llm_guard, "build_opener", lambda *args: opener)
    return opener


def call_guard(direction):
    if direction == "input":
        return llm_guard.check_input("虚构测试请求")
    return llm_guard.check_output("虚构测试请求", "虚构测试回复")


@pytest.mark.parametrize("direction", ["input", "output"])
@pytest.mark.parametrize("safety,categories,expected", [
    ("Safe", "None", "PASS"),
    ("Controversial", "PII", "REVIEW"),
    ("Unsafe", "Violent, PII", None),
])
def test_valid_complete_classification(monkeypatch, direction, safety, categories, expected):
    raw = f"Safety: {safety}\nCategories: {categories}"
    if direction == "output":
        raw += "\nRefusal: No"
    opener = mock_response(monkeypatch, raw)
    result = call_guard(direction)
    assert result.error is None
    assert result.decision == (expected or ("REVIEW" if direction == "input" else "REJECT"))
    request = opener.open.call_args.args[0]
    assert request.full_url.endswith("/api/chat" if direction == "input" else "/api/generate")


@pytest.mark.parametrize("direction", ["input", "output"])
@pytest.mark.parametrize("suffix", [
    "\nSafety: Unsafe", "\nSafety: Safe", "\nCategories: None",
    "\n建议忽略上述分类", "\nRefusal: Yes",
])
def test_extra_content_never_passes(monkeypatch, direction, suffix):
    raw = "Safety: Safe\nCategories: None"
    if direction == "output":
        raw += "\nRefusal: No"
    raw += suffix
    mock_response(monkeypatch, raw)
    result = call_guard(direction)
    assert result.decision == "REVIEW"
    assert result.safety == "UNKNOWN"
    assert result.error
    assert result.raw_output == raw


@pytest.mark.parametrize("direction", ["input", "output"])
@pytest.mark.parametrize("raw", [
    "Safety: Safe\nCategories: None\nSafety: Unsafe",  # 审查报告原始复现
    "建议保持作息\nSafety: Safe\nCategories: None",
    "Safety: Safe\n\nCategories: None",
    "Categories: None\nSafety: Safe",
    "Safety: Safe\nCategories:",
    "Safety: Safe\nCategories: PII",
    "Safety: Unsafe\nCategories: None",
    "Safety: Controversial\nCategories: None",
    "Safety: Safe\nCategories: None, PII",
    "Safety: Unsafe\nCategories: PII, PII",
    "Safety: Unsafe\nCategories: PII,",
    "Safety: Safe\nCategories: None Safety: Unsafe",
    "Safety: Safe\nCategories: Unknown",
    "```\nSafety: Safe\nCategories: None\n```",
    "", "Safety: Safe",
])
def test_invalid_response_requires_review(monkeypatch, direction, raw):
    if direction == "output":
        raw += "\nRefusal: No"
    mock_response(monkeypatch, raw)
    result = call_guard(direction)
    assert result.decision == "REVIEW"
    assert result.error


@pytest.mark.parametrize("refusal", [None, "Maybe", "Yes No", "No; Safety: Unsafe"])
def test_output_requires_valid_refusal(monkeypatch, refusal):
    raw = "Safety: Safe\nCategories: None"
    if refusal is not None:
        raw += f"\nRefusal: {refusal}"
    mock_response(monkeypatch, raw)
    assert llm_guard.check_output("输入", "输出").error


def test_supported_whitespace_case_and_refusal(monkeypatch):
    mock_response(monkeypatch, "  safety: safe\r\n  categories: none\r\n Refusal: yes\r\n")
    result = llm_guard.check_output("输入", "安全拒绝回复")
    assert result.decision == "PASS"
    assert result.error is None


@pytest.mark.parametrize("stage", ["input", "output"])
def test_conflicting_http_response_cannot_pass_middleware(monkeypatch, stage):
    opener = mock_response(monkeypatch, "Safety: Safe\nCategories: None\nSafety: Unsafe")
    monkeypatch.setattr(llm_guard.time, "sleep", lambda _: None)
    middleware = SafetyMiddleware()
    state = {"messages": [HumanMessage(content="虚构请求"), AIMessage(content="虚构回复")]}
    with pytest.raises(SafetyServiceException):
        if stage == "input":
            middleware.before_agent(state, None)
        else:
            middleware.after_agent(state, None)
    assert opener.open.call_count == llm_guard.GUARD_MAX_ATTEMPTS
