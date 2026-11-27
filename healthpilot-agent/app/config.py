from pydantic_settings import BaseSettings, SettingsConfigDict

import os

from dotenv import load_dotenv

load_dotenv()

class Settings(BaseSettings):
    # 运行环境
    app_env: str = "development"

    # DeepSeek 模型配置
    deepseek_api_key: str = ""
    deepseek_model: str = "deepseek-flash"

    # Java 后端地址
    java_server_url: str = "http://localhost:8080"

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )


settings = Settings()

# ==========================================
# Qwen3Guard 安全模型配置
# ==========================================

GUARD_BASE_URL = os.getenv(
    "GUARD_BASE_URL",
    "http://127.0.0.1:11434",
).rstrip("/")


GUARD_MODEL = os.getenv(
    "GUARD_MODEL",
    "hf.co/ShahzebKhoso/Qwen3Guard-Gen-8B-GGUF:Q4_K_M",
)


GUARD_API_KEY = os.getenv(
    "GUARD_API_KEY",
    "",
)


GUARD_TIMEOUT = float(
    os.getenv("GUARD_TIMEOUT", "30")
)


GUARD_MAX_ATTEMPTS = int(
    os.getenv("GUARD_MAX_ATTEMPTS", "3")
)


GUARD_RETRY_DELAY = float(
    os.getenv("GUARD_RETRY_DELAY", "0.5")
)


if not GUARD_BASE_URL:
    raise ValueError("GUARD_BASE_URL 不能为空")

if not GUARD_MODEL.strip():
    raise ValueError("GUARD_MODEL 不能为空")

if GUARD_TIMEOUT <= 0:
    raise ValueError("GUARD_TIMEOUT 必须大于 0")

if GUARD_MAX_ATTEMPTS < 1:
    raise ValueError("GUARD_MAX_ATTEMPTS 必须大于等于 1")

if GUARD_RETRY_DELAY < 0:
    raise ValueError("GUARD_RETRY_DELAY 不能小于 0")