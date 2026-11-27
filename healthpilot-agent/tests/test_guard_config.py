from app.config import (
    GUARD_BASE_URL,
    GUARD_MODEL,
    GUARD_TIMEOUT,
    GUARD_MAX_ATTEMPTS,
    GUARD_RETRY_DELAY,
)

from app.guardrails import llm_guard


def test_guard_base_url():
    """验证安全服务地址。"""

    assert GUARD_BASE_URL.startswith(
        ("http://", "https://")
    )

    assert not GUARD_BASE_URL.endswith("/")

    print(f"\n安全服务地址：{GUARD_BASE_URL}")


def test_guard_model():
    """验证安全模型名称。"""

    assert GUARD_MODEL.strip()

    print(f"\n安全模型：{GUARD_MODEL}")


def test_guard_parameters():
    """验证超时及重试配置。"""

    assert GUARD_TIMEOUT > 0
    assert GUARD_MAX_ATTEMPTS >= 1
    assert GUARD_RETRY_DELAY >= 0

    print(f"\n请求超时：{GUARD_TIMEOUT}")
    print(f"最大尝试次数：{GUARD_MAX_ATTEMPTS}")
    print(f"重试间隔：{GUARD_RETRY_DELAY}")


def test_guard_headers_without_key(monkeypatch):
    """本地 Ollama 无需认证。"""

    monkeypatch.setattr(
        llm_guard,
        "GUARD_API_KEY",
        "",
    )

    headers = llm_guard.get_guard_headers()

    assert "Authorization" not in headers


def test_guard_headers_with_key(monkeypatch):
    """远程服务支持认证请求头。"""

    monkeypatch.setattr(
        llm_guard,
        "GUARD_API_KEY",
        "test-token",
    )

    headers = llm_guard.get_guard_headers()

    assert headers["Authorization"] == (
        "Bearer test-token"
    )


def test_guard_config_binding():
    """验证安全模块使用统一配置。"""

    assert llm_guard.GUARD_BASE_URL == GUARD_BASE_URL

    assert llm_guard.GUARD_MODEL == GUARD_MODEL

    assert llm_guard.GUARD_TIMEOUT == GUARD_TIMEOUT