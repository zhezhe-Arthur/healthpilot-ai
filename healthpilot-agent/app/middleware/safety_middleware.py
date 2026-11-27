import json
from typing import Any

from langchain.agents.middleware import (
    AgentMiddleware,
    AgentState,
)
from langchain_core.messages import HumanMessage, AIMessage
from langgraph.runtime import Runtime
from pydantic import BaseModel

from app.guardrails.llm_guard import (
    GuardResult,
    check_input,
    check_output,
    guarded_call,
)


class SafetyGuardException(Exception):
    """内容未通过安全审查。"""

    def __init__(
        self,
        stage: str,
        result: GuardResult,
    ):
        self.stage = stage
        self.result = result

        super().__init__(
            f"Safety guard: {stage}, {result.decision}"
        )


class SafetyServiceException(Exception):
    """安全服务重试后仍然不可用。"""

    def __init__(self, stage: str):
        self.stage = stage

        super().__init__(
            f"Safety service unavailable: {stage}"
        )


class SafetyMiddleware(AgentMiddleware):
    """公共安全护栏中间件。"""

    def __init__(
        self,
        structured_output: bool = False,
    ):
        super().__init__()

        self.structured_output = structured_output

    @staticmethod
    def _get_user_text(state: AgentState) -> str:

        for message in reversed(
            state.get("messages", [])
        ):
            if isinstance(message, HumanMessage):

                if isinstance(message.content, str):
                    return message.content

                return json.dumps(
                    message.content,
                    ensure_ascii=False,
                )

        return ""

    def _get_output_text(
        self,
        state: AgentState,
    ) -> str:

        # 结构化输出
        if self.structured_output:

            response = state.get(
                "structured_response"
            )

            if response is None:
                return ""

            if isinstance(response, BaseModel):
                return response.model_dump_json(
                    by_alias=True
                )

            return json.dumps(
                response,
                ensure_ascii=False,
            )

        # 自然语言输出
        for message in reversed(
            state.get("messages", [])
        ):
            if isinstance(message, HumanMessage):
                break

            if isinstance(message, AIMessage):

                if message.tool_calls:
                    continue

                if isinstance(message.content, str):
                    return message.content

                return json.dumps(
                    message.content,
                    ensure_ascii=False,
                )

        return ""

    def before_agent(
        self,
        state: AgentState,
        runtime: Runtime,
    ) -> dict[str, Any] | None:

        user_text = self._get_user_text(state)

        result = guarded_call(
            check_input,
            user_text,
        )

        if result is None:
            raise SafetyServiceException(
                stage="input"
            )

        if result.decision != "PASS":
            raise SafetyGuardException(
                stage="input",
                result=result,
            )

        return None

    def after_agent(
        self,
        state: AgentState,
        runtime: Runtime,
    ) -> dict[str, Any] | None:

        user_text = self._get_user_text(state)

        assistant_text = self._get_output_text(
            state
        )

        result = guarded_call(
            check_output,
            user_text,
            assistant_text,
        )

        if result is None:
            raise SafetyServiceException(
                stage="output"
            )

        if result.decision != "PASS":
            raise SafetyGuardException(
                stage="output",
                result=result,
            )

        return None