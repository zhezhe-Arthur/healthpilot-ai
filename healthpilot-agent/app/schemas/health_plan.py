
from datetime import date
from decimal import Decimal
from enum import Enum

from pydantic import (
    BaseModel,
    ConfigDict,
    Field,
    field_validator,
    model_validator,
    JsonValue,
)
from pydantic.alias_generators import to_camel

class PlanCategory(str, Enum):
    EXERCISE = "exercise"
    MENTAL = "mental_health"
    NUTRITION = "nutrition"
    SLEEP = "sleep"
    GOAL = "goal"


JAVA_CATEGORIES = {
    PlanCategory.EXERCISE: "BODY",
    PlanCategory.MENTAL: "MENTAL",
    PlanCategory.NUTRITION: "DIET",
    PlanCategory.SLEEP: "SLEEP",
    PlanCategory.GOAL: "GOAL",
}


class PlanMode(str, Enum):
    MANUAL = "MANUAL"
    AUTO_WITH_FEEDBACK = "AUTO_WITH_FEEDBACK"
    AUTO_NO_FEEDBACK = "AUTO_NO_FEEDBACK"


class PlanItem(BaseModel):
    model_config = ConfigDict(
        alias_generator=to_camel,
        populate_by_name=True,
        extra="forbid",
        revalidate_instances="always",
    )

    category: PlanCategory
    title: str = Field(min_length=1, max_length=150)
    scheduled_weekdays: list[int] = Field(min_length=1, max_length=7)
    target_quantity: Decimal = Field(ge=Decimal("0.01"), le=Decimal("999999.99"), max_digits=8, decimal_places=2)
    target_unit: str = Field(min_length=1, max_length=20)
    sort_order: int = Field(ge=0, le=2147483647, strict=True)

    @field_validator("category", mode="before")
    @classmethod
    def normalize_category(cls, value):
        # Java 的类别别名进入内部模型时统一归一化。
        if isinstance(value, str):
            return {java: internal for internal, java in JAVA_CATEGORIES.items()}.get(value, value)
        return value

    @field_validator("title", "target_unit")
    @classmethod
    def reject_blank(cls, value: str, info) -> str:
        if not value.strip():
            raise ValueError("标题和单位不能为空白")
        limit = 150 if info.field_name == "title" else 20
        if len(value.encode("utf-16-le")) // 2 > limit:
            raise ValueError("字段长度超出 Java 保存限制")
        return value

    @field_validator("scheduled_weekdays")
    @classmethod
    def validate_weekdays(cls, value: list[int]) -> list[int]:
        if any(day < 1 or day > 7 for day in value):
            raise ValueError("星期必须在 1～7 之间")

        if len(value) != len(set(value)):
            raise ValueError("星期不能重复")

        return value

class WeeklyPlan(BaseModel):
    model_config = ConfigDict(
        alias_generator=to_camel,
        populate_by_name=True,
        extra="forbid",
        revalidate_instances="always",
    )

    week_start_date: date
    items: list[PlanItem] = Field(min_length=1, max_length=40)

    @field_validator("week_start_date")
    @classmethod
    def validate_monday(cls, value: date) -> date:
        if value.weekday() != 0:
            raise ValueError("每周计划必须从周一开始")
        return value


class WeeklyPlanRequest(BaseModel):
    """调用方负责授权并提供已确认资料；此对象不从自由文本推测业务参数。"""

    model_config = ConfigDict(
        alias_generator=to_camel, populate_by_name=True, extra="forbid",
        revalidate_instances="always",
    )

    mode: PlanMode
    week_start_date: date
    confirmed_profile: dict[str, JsonValue]
    health_goals: list[dict[str, JsonValue]]
    user_text: str = ""
    weekly_feedback: dict[str, JsonValue] | None = None
    previous_plan: WeeklyPlan | None = None

    @model_validator(mode="after")
    def validate_context(self):
        if self.week_start_date.weekday() != 0:
            raise ValueError("目标周必须从周一开始")
        if self.mode == PlanMode.AUTO_NO_FEEDBACK:
            if self.previous_plan is None:
                raise ValueError("无反馈自动生成缺少上一份确认计划")
            if self.weekly_feedback is not None:
                raise ValueError("无反馈模式不能携带每周反馈")
        if self.mode == PlanMode.AUTO_WITH_FEEDBACK and not self.weekly_feedback:
            raise ValueError("有反馈模式必须提供每周反馈")
        if self.previous_plan and self.previous_plan.week_start_date >= self.week_start_date:
            raise ValueError("上一份计划的日期必须早于目标周")
        return self


def to_java_draft(plan: WeeklyPlan) -> dict:
    """显式转换为 SaveHealthPlanDraftRequest，不负责保存或确认。"""
    plan = WeeklyPlan.model_validate(plan)
    return {
        "weekStartDate": plan.week_start_date.isoformat(),
        "items": [
            {
                "category": JAVA_CATEGORIES[item.category],
                "title": item.title,
                "scheduledWeekdays": ",".join(str(day) for day in item.scheduled_weekdays),
                "targetQuantity": float(item.target_quantity),
                "targetUnit": item.target_unit,
                "sortOrder": item.sort_order,
            }
            for item in plan.items
        ],
    }
