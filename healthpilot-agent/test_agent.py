from app.agent.health_agent import health_agent


def main():
    result = health_agent.invoke({
        "messages": [
            {
                "role": "user",
                "content": (
                    "请介绍你在 HealthPilot AI 中的职责，"
                    "并说明如何为用户制定健康计划。"
                ),
            }
        ]
    })

    print("========== Agent 回复 ==========")
    print(result["messages"][-1].content)


if __name__ == "__main__":
    main()