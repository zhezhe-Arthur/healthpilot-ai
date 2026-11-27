from app.agent.health_agent import health_agent


# def main():
#     result = health_agent.invoke({
#         "messages": [
#             {
#                 "role": "user",
#                 "content": (
#                     "请介绍你在 HealthPilot AI 中的职责，"
#                     "并说明如何为用户制定健康计划。"
#                 ),
#             }
#         ]
#     })
#
#     print("========== Agent 回复 ==========")
#     print(result["messages"][-1].content)

def main():
    result = health_agent.invoke({
        "messages": [
            {
                "role": "user",
                "content": (
                    "请使用 read_file 工具读取 "
                    "/skills/weekly_planning/SKILL.md，"
                    "然后根据文件内容说明："
                    "HealthPilot 是否需要每日打卡？"
                    "七天计划如何确认生效？"
                )
            }
        ]
    })

    print("========== 工具调用 ==========")

    for message in result["messages"]:
        for call in getattr(message, "tool_calls", []):
            print(call["name"], call["args"])

    print("\n========== Agent 回复 ==========")
    print(result["messages"][-1].content)


if __name__ == "__main__":
    main()