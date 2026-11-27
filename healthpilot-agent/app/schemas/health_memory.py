
from enum import Enum
from datetime import date

from pydantic import BaseModel, Field, ConfigDict
from pydantic.alias_generators import to_camel


class MemoryType(str, Enum):
    LONG_TERM = "LONG_TERM"
    SHORT_TERM = "SHORT_TERM"


class InformationSource(str, Enum):
    USER_STATEMENT = "USER_STATEMENT"
    MEDICAL_DOCUMENT = "MEDICAL_DOCUMENT"
    WEEKLY_FEEDBACK = "WEEKLY_FEEDBACK"
    AI_INFERENCE = "AI_INFERENCE"


class HealthMemoryCandidate(BaseModel):
    """AI 提取的一条待处理健康信息"""

    model_config = ConfigDict(
        alias_generator=to_camel,
        populate_by_name=True,
        extra="forbid",
    )

    memory_type: MemoryType

    # 提取出的健康信息
    content: str = Field(min_length=1, max_length=1000)

    # 信息来源类别
    source: InformationSource

    # 支持这条信息的原文片段
    evidence: str = Field(min_length=1, max_length=2000)

    # 事实对应的日期，未知时为 None
    observed_date: date | None = None


class MemoryExtraction(BaseModel):
    """一次对话或文件分析产生的候选记忆"""

    model_config = ConfigDict(
        alias_generator=to_camel,
        populate_by_name=True,
        extra="forbid",
    )

    candidates: list[HealthMemoryCandidate] = Field(
        default_factory=list,
        max_length=20,
    )
