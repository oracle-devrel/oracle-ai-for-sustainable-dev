import os
from pathlib import Path

import uvicorn
from a2a.server.agent_execution import AgentExecutor
from a2a.server.apps.jsonrpc.starlette_app import A2AStarletteApplication
from a2a.server.request_handlers.default_request_handler import DefaultRequestHandler
from a2a.server.tasks.inmemory_push_notification_config_store import (
    InMemoryPushNotificationConfigStore,
)
from a2a.server.tasks.inmemory_task_store import InMemoryTaskStore
from a2a.server.tasks.task_updater import TaskUpdater
from a2a.types import (
    AgentCapabilities,
    AgentCard,
    AgentSkill,
    DataPart,
    JSONRPCError,
    Part,
    TaskNotCancelableError,
    TextPart,
)
from dotenv import load_dotenv

from inventory_action_service import (
    InventoryActionCoordinator,
    InventoryActionResult,
    build_rpc_url,
)

REPO_ROOT = Path(__file__).resolve().parent.parent
load_dotenv(REPO_ROOT / ".env")

BIND_HOST = os.environ.get("BIND_HOST") or "0.0.0.0"
PORT = int(os.environ.get("ACTION_AGENT_PORT") or os.environ.get("PORT") or "8080")
RPC_URL = build_rpc_url(os.environ)
COORDINATOR = InventoryActionCoordinator(os.environ)


def build_agent_card() -> AgentCard:
    return AgentCard(
        capabilities=AgentCapabilities(
            streaming=False,
            pushNotifications=False,
            stateTransitionHistory=False,
            extensions=[],
        ),
        defaultInputModes=["text/plain"],
        defaultOutputModes=["application/json", "text/plain"],
        description=(
            "ADK-based coordinator for final-stage inventory action planning. "
            "It gathers graph, spatial, and external signals, checks policy, "
            "and drafts a recommended inventory move that still requires approval."
        ),
        name="oracle_inventory_action_agent",
        preferredTransport="JSONRPC",
        protocolVersion="0.3.0",
        skills=[
            AgentSkill(
                description=(
                    "Coordinates the final inventory action stage by gathering evidence and "
                    "recommending a transfer, expedite, substitute, or hold action."
                ),
                examples=[],
                id="oracle_inventory_action_agent",
                inputModes=["text/plain"],
                name="inventory-action-coordinator",
                outputModes=["application/json", "text/plain"],
                tags=["llm", "orchestration"],
            ),
            AgentSkill(
                description=(
                    "Given a product or risk prompt, gather graph, spatial, and external evidence "
                    "before proposing a draft inventory move and indicating whether approval is required."
                ),
                examples=[],
                id="oracle_inventory_action_agent-recommendInventoryAction",
                inputModes=["text/plain"],
                name="recommendInventoryAction",
                outputModes=["application/json", "text/plain"],
                tags=["llm", "tools", "inventory"],
            ),
        ],
        supportsAuthenticatedExtendedCard=False,
        url=RPC_URL,
        version="0.0.2",
    )


def response_parts(result: InventoryActionResult) -> list[Part]:
    parts = [Part(root=TextPart(text=result.response_text))]
    if result.draft_action:
        payload = {"action": result.draft_action}
        if result.policy_result:
            payload["policy"] = result.policy_result
        parts.append(Part(root=DataPart(data=payload)))
    return parts


def response_metadata(result: InventoryActionResult) -> dict[str, object]:
    metadata: dict[str, object] = {
        "coordinator": result.orchestration_mode,
        "traceCount": len(result.trace),
    }
    if result.draft_action:
        metadata["actionType"] = result.draft_action.get("actionType")
        metadata["draftActionId"] = result.draft_action.get("draftActionId")
    return metadata


class InventoryActionExecutor(AgentExecutor):
    async def execute(self, context, event_queue) -> None:
        updater = TaskUpdater(event_queue, context.task_id, context.context_id)
        if not context.current_task:
            await updater.submit()

        await updater.start_work()

        try:
            result = await COORDINATOR.run(
                context.get_user_input(""),
                context.context_id,
            )
            await updater.complete(
                updater.new_agent_message(
                    response_parts(result),
                    metadata=response_metadata(result),
                )
            )
        except Exception as exc:
            await updater.failed(
                updater.new_agent_message(
                    [Part(root=TextPart(text=f"Inventory action coordinator failed: {exc}"))],
                    metadata={"error": "inventory_action_execution_failed"},
                )
            )
            raise JSONRPCError(
                code=-32603,
                message=f"Inventory action coordinator failed: {exc}",
            )

    async def cancel(self, context, event_queue) -> None:
        raise TaskNotCancelableError()


REQUEST_HANDLER = DefaultRequestHandler(
    agent_executor=InventoryActionExecutor(),
    task_store=InMemoryTaskStore(),
    push_config_store=InMemoryPushNotificationConfigStore(),
)

app = A2AStarletteApplication(
    agent_card=build_agent_card(),
    http_handler=REQUEST_HANDLER,
).build()

if __name__ == "__main__":
    uvicorn.run(app, host=BIND_HOST, port=PORT)
