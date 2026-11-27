def plan_request(user_text="生成下一周计划", mode="MANUAL", previous_plan=None, **overrides):
    request = {
        "userText": user_text,
        "mode": mode,
        "weekStartDate": "2026-11-30",
        "confirmedProfile": {"activityLevel": "LOW", "restrictions": ["沿用已确认安排"]},
        "healthGoals": [{"description": "保持日常活动"}],
        "weeklyFeedback": None,
        "previousPlan": previous_plan,
    }
    request.update(overrides)
    return request
