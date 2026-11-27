
from dataclasses import dataclass
from datetime import date
from decimal import Decimal

from app.schemas.health_plan import WeeklyPlan, PlanCategory, PlanMode


MODES = {mode.value for mode in PlanMode}

# 目前只比较可统一换算为分钟的运动任务
MINUTE_UNITS = {
    "分钟",
    "分钟/次",
    "分钟/天",
    "min",
    "minute",
    "minutes",
}


@dataclass
class PlanGuardResult:
    errors: list[str]
    warnings: list[str]

    @property
    def passed(self) -> bool:
        return len(self.errors) == 0


def exercise_minutes(plan: WeeklyPlan) -> Decimal | None:
    """
    计算一周量化运动的总分钟数。

    无法确定运动时长时返回 None，
    不猜测单位、不假设任务已经完成。
    """
    total = Decimal(0)

    for item in plan.items:
        if not isinstance(item.category, PlanCategory):
            return None
        if item.category != PlanCategory.EXERCISE:
            continue

        # 不通过标题猜测休息或允许缺失时长。
        if item.target_quantity is None:
            return None

        unit = (item.target_unit or "").strip().lower()

        if unit not in MINUTE_UNITS:
            return None

        total += item.target_quantity * len(item.scheduled_weekdays)

    return total


def check_weekly_plan(
    plan: WeeklyPlan,
    *,
    mode: str,
    previous_plan: WeeklyPlan | None = None,
    expected_week_start_date: date | None = None,
) -> PlanGuardResult:
    errors = []
    warnings = []

    if expected_week_start_date is not None and plan.week_start_date != expected_week_start_date:
        errors.append("计划起始日期与请求的目标周不一致")

    # 1. 生成模式
    if mode not in MODES:
        errors.append(f"未知计划生成模式：{mode}")

    # 2. 必须覆盖一周七天
    covered_days = set()

    for item in plan.items:
        covered_days.update(item.scheduled_weekdays)

    missing_days = set(range(1, 8)) - covered_days

    if missing_days:
        errors.append(
            f"计划未覆盖星期：{sorted(missing_days)}"
        )

    # 3. 排序编号不能重复
    sort_orders = [item.sort_order for item in plan.items]

    if len(sort_orders) != len(set(sort_orders)):
        errors.append("计划项目 sortOrder 不能重复")

    # 4. 无反馈时必须保守更新
    if mode == "AUTO_NO_FEEDBACK":
        if previous_plan is None:
            errors.append(
                "无反馈自动生成必须提供最近已确认的计划"
            )
        else:
            if previous_plan.week_start_date >= plan.week_start_date:
                errors.append("上一份计划的日期必须早于新计划")

            old_minutes = exercise_minutes(previous_plan)
            new_minutes = exercise_minutes(plan)

            if old_minutes is None or new_minutes is None:
                errors.append(
                    "无法比较新旧运动负荷，需要人工审核"
                )
            elif new_minutes > old_minutes:
                errors.append(
                    "无反馈时禁止自动增加每周运动时长："
                    f"原计划 {old_minutes:g} 分钟，"
                    f"新计划 {new_minutes:g} 分钟"
                )

        warnings.append(
            "运动时长未增加不代表运动强度安全，"
            "仍需检查运动类型及用户健康限制"
        )

    # 5. 可选任务目前没有独立字段
    if any("可选" in item.title for item in plan.items):
        warnings.append(
            "计划存在可选任务，当前 Schema 尚未区分必做与可选"
        )

    return PlanGuardResult(
        errors=errors,
        warnings=warnings,
    )
