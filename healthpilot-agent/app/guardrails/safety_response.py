from dataclasses import dataclass


@dataclass(frozen=True)
class SafetyResponse:
    """安全降级处理结果。"""

    code: str
    message: str


CRISIS_RESPONSE = SafetyResponse(
    code="CRISIS_SUPPORT",
    message=(
        "听起来你可能正在经历非常难受的事情。"
        "如果你现在有伤害自己的想法，"
        "请尽量联系可信任的亲友，让他们陪伴你，"
        "并远离可能造成伤害的物品。"
        "如果你已经采取了伤害行为，"
        "或者担心自己无法保证安全，"
        "请立即联系当地急救服务或前往急诊。"
        "如果你在中国大陆，可以拨打120。"
    ),
)


VIOLENCE_RESPONSE = SafetyResponse(
    code="VIOLENCE_RISK",
    message=(
        "我无法提供伤害他人的方法。"
        "如果你或其他人正面临危险，"
        "请尽量远离危险环境，"
        "并联系可信任的人或当地紧急救援服务。"
    ),
)


MEDICAL_OUTPUT_RESPONSE = SafetyResponse(
    code="UNSAFE_OUTPUT",
    message=(
        "本次生成的内容未通过安全审查，"
        "因此暂时无法提供相关建议。"
        "请不要自行调整药物剂量或治疗方案。"
        "如果出现严重或突然发生的身体不适，"
        "请及时寻求专业医疗帮助。"
    ),
)


GENERAL_REVIEW_RESPONSE = SafetyResponse(
    code="SAFETY_REVIEW",
    message=(
        "本次请求需要进一步安全评估。"
        "你可以补充具体情况或重新描述问题。"
        "如果涉及紧急健康风险，请及时就医。"
    ),
)


SERVICE_UNAVAILABLE_RESPONSE = SafetyResponse(
    code="SAFETY_SERVICE_UNAVAILABLE",
    message=(
        "安全审查服务暂时不可用，"
        "目前无法提供经过安全审查的健康建议。"
        "请稍后重试。"
        "如果你的情况紧急，请直接寻求医疗帮助，"
        "不要等待系统恢复。"
    ),
)


def resolve_safety_response(
    stage: str,
    categories: list[str] | None = None,
) -> SafetyResponse:
    """根据安全审查阶段和风险类别选择固定回复。"""

    categories = {
        category.strip().casefold()
        for category in (categories or [])
    }

    # 自杀、自残风险优先处理
    if "suicide & self-harm" in categories:
        return CRISIS_RESPONSE

    # 输出审查失败，拦截原始模型内容
    if stage == "output":
        return MEDICAL_OUTPUT_RESPONSE

    # 暴力风险
    if "violent" in categories:
        return VIOLENCE_RESPONSE

    # 其他风险
    return GENERAL_REVIEW_RESPONSE


def resolve_service_failure() -> SafetyResponse:
    """安全服务故障时返回固定提示。"""

    return SERVICE_UNAVAILABLE_RESPONSE