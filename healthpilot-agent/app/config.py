from pydantic_settings import BaseSettings, SettingsConfigDict


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