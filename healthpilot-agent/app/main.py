from http import server

from fastapi import FastAPI
from app.config import settings

app = FastAPI(
    title="HealthPilot project AI Agent",
    description="健康计划智能体服务",
    version="1.0.0",
)

@app.get("/health")
def health():
    """健康检查接口"""
    return {
        "status": "ok",
        "service": "health-pilot-agent",
        "environment": settings.app_env,
    }
