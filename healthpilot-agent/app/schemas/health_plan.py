
from datetime import date

from pydantic import (
    BaseModel,
    ConfigDict,
    Field,
    field_validator,
    model_validator,
)
from pydantic.alias_generators import to_camel

class PlanItem(BaseModel):
    model_config = ConfigDict(
        alias_generator=to_camel,
        populate_by_name=True,
        extra="forbid",
    )

    category: str = Field(min_length=1, max_length=30)
    title: str = Field(min_length=1, max_length=200)
    scheduled_weekdays: list[int] = Field(min_length=1, max_length=7)
    target_quantity: float | None = Field(default=None, gt=0)
    target_unit: str | None = Field(default=None, min_length=1)
    sort_order: int = Field(ge=0)

    @field_validator("scheduled_weekdays")
    @classmethod
    def validate_weekdays(cls, value: list[int]) -> list[int]:
        if any(day < 1 or day > 7 for day in value):
            raise ValueError("星期必须在 1～7 之间")

        if len(value) != len(set(value)):
            raise ValueError("星期不能重复")

        return value

    @model_validator(mode="after")
    def validate_target(self):
        if (self.target_quantity is None) != (self.target_unit is None):
            raise ValueError("目标数量和单位必须同时存在或同时缺失")

        return self


class WeeklyPlan(BaseModel):
    model_config = ConfigDict(
        alias_generator=to_camel,
        populate_by_name=True,
        extra="forbid",
    )

    week_start_date: date
    items: list[PlanItem] = Field(min_length=1)

    @field_validator("week_start_date")
    @classmethod
    def validate_monday(cls, value: date) -> date:
        if value.weekday() != 0:
            raise ValueError("每周计划必须从周一开始")
        return value
