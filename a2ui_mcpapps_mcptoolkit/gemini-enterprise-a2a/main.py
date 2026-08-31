"""A2A v0.3 adapter that delivers A2UI v0.8 to Gemini Enterprise."""

from __future__ import annotations

import asyncio
import json
import os
import re
from typing import Any
from urllib.error import HTTPError
from urllib.parse import urlencode
from urllib.request import Request, urlopen

import uvicorn
from a2a.server.agent_execution import AgentExecutor
from a2a.server.apps.jsonrpc.starlette_app import A2AStarletteApplication
from a2a.server.request_handlers.default_request_handler import (
    DefaultRequestHandler,
)
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
from a2ui.a2a.extension import get_a2ui_agent_extension
from a2ui.a2a.parts import create_a2ui_part
from a2ui.schema.constants import VERSION_0_8

from a2ui_payloads import result_messages, review_messages

AGENT_SERVICE_URL = os.environ.get(
    "AGENT_SERVICE_URL", "http://127.0.0.1:8080"
).rstrip("/")
BIND_HOST = os.environ.get("BIND_HOST", "0.0.0.0")
PORT = int(
    os.environ.get(
        "GEMINI_ENTERPRISE_A2A_PORT",
        os.environ.get("PORT", "3002"),
    )
)
PUBLIC_A2A_URL = os.environ.get(
    "PUBLIC_A2A_URL", f"http://127.0.0.1:{PORT}"
).rstrip("/")
TRUSTED_ACCESS_PROFILE = os.environ.get(
    "TRUSTED_ACCESS_PROFILE", "full"
).strip().lower()
if TRUSTED_ACCESS_PROFILE not in {"full", "environmental"}:
    raise ValueError(
        "TRUSTED_ACCESS_PROFILE must be full or environmental"
    )
AGENT_DISPLAY_NAME = os.environ.get(
    "A2A_AGENT_DISPLAY_NAME",
    (
        "Oracle Environmental Inventory Planner"
        if TRUSTED_ACCESS_PROFILE == "environmental"
        else "Oracle Supply-Chain Inventory Exchange"
    ),
)
STANDARD_CATALOG = (
    "https://a2ui.org/specification/v0_8/"
    "standard_catalog_definition.json"
)


def build_agent_card() -> AgentCard:
    extension = get_a2ui_agent_extension(
        VERSION_0_8,
        False,
        [STANDARD_CATALOG],
    )
    return AgentCard(
        capabilities=AgentCapabilities(
            streaming=True,
            pushNotifications=False,
            stateTransitionHistory=False,
            extensions=[extension],
        ),
        defaultInputModes=["text/plain", "application/json+a2ui"],
        defaultOutputModes=["text/plain", "application/json+a2ui"],
        description=(
            "Shows read-only environmental inventory recommendations "
            "filtered by Oracle Deep Data Security."
            if TRUSTED_ACCESS_PROFILE == "environmental"
            else (
                "Shows Oracle-governed inventory-transfer recommendations "
                "and requires explicit A2UI approval before a database write."
            )
        ),
        name=AGENT_DISPLAY_NAME,
        preferredTransport="JSONRPC",
        protocolVersion="0.3.0",
        skills=[
            AgentSkill(
                description=(
                    "Review Deep Data Security-filtered environmental "
                    "inventory recommendations."
                    if TRUSTED_ACCESS_PROFILE == "environmental"
                    else (
                        "Review stockout-risk recommendations and approve or "
                        "cancel one exact inventory transfer."
                    )
                ),
                examples=[
                    (
                        "Show inventory transfers with a minimum stockout "
                        "risk of 70, limited to 10 recommendations."
                    )
                ],
                id="inventory-transfer-review",
                inputModes=["text/plain", "application/json+a2ui"],
                name=(
                    "Environmental inventory review"
                    if TRUSTED_ACCESS_PROFILE == "environmental"
                    else "Inventory transfer review"
                ),
                outputModes=["text/plain", "application/json+a2ui"],
                tags=(
                    ["inventory", "oracle", "a2ui", "deep-data-security"]
                    if TRUSTED_ACCESS_PROFILE == "environmental"
                    else ["inventory", "oracle", "a2ui", "approval"]
                ),
            )
        ],
        supportsAuthenticatedExtendedCard=False,
        url=PUBLIC_A2A_URL,
        version="0.1.0",
    )


class SupplyChainExecutor(AgentExecutor):
    async def execute(self, context, event_queue) -> None:
        updater = TaskUpdater(event_queue, context.task_id, context.context_id)
        if not context.current_task:
            await updater.submit()
        await updater.start_work()
        try:
            access_profile = TRUSTED_ACCESS_PROFILE
            action = _extract_user_action(context)
            if action:
                parts = await _handle_action(action, access_profile)
            else:
                prompt = context.get_user_input("")
                minimum_risk, maximum_rows = _query_parameters(prompt)
                review = await asyncio.to_thread(
                    _post_agent,
                    "/api/reviews",
                    {
                        "minimumStockoutRisk": minimum_risk,
                        "maximumRows": maximum_rows,
                        "accessProfile": access_profile,
                    },
                )
                messages = review_messages(
                    review["recommendations"],
                    review["approvalId"],
                    access_profile,
                    bool(review["writesAllowed"]),
                )
                parts = [
                    Part(
                        root=TextPart(
                            text=(
                                f"Showing {len(review['recommendations'])} "
                                "Oracle-governed inventory-transfer "
                                "recommendations for explicit review."
                            )
                        )
                    ),
                    *(create_a2ui_part(message) for message in messages),
                ]
            await updater.complete(updater.new_agent_message(parts))
        except Exception as exc:
            await updater.failed(
                updater.new_agent_message(
                    [
                        Part(
                            root=TextPart(
                                text=f"Inventory-transfer workflow failed: {exc}"
                            )
                        )
                    ],
                    metadata={"error": "inventory_transfer_execution_failed"},
                )
            )
            raise JSONRPCError(code=-32603, message=str(exc))

    async def cancel(self, context, event_queue) -> None:
        raise TaskNotCancelableError()


async def _handle_action(
    action: dict[str, Any], access_profile: str
) -> list[Part]:
    name = action.get("name")
    values = action.get("context")
    if not isinstance(values, dict):
        raise ValueError("A2UI action context is required")
    if name == "approveInventoryTransfer":
        notes = str(values.get("approvalNotes", "")).strip()
        if len(notes) < 10:
            raise ValueError("Approval notes must contain at least 10 characters")
        result = await asyncio.to_thread(
            _post_agent,
            "/api/approve",
            {
                "approvalId": _required(values, "approvalId"),
                "recommendationId": _required(
                    values, "recommendationId"
                ),
                "approvalNotes": notes,
                "accessProfile": access_profile,
            },
        )
        detail = (
            f"Transfer {result['transferId']} moved "
            f"{result['transferQuantity']} units and was recorded as "
            f"{result['status']}."
        )
        return [
            Part(root=TextPart(text=detail)),
            *(
                create_a2ui_part(message)
                for message in result_messages(
                    "Inventory transfer approved",
                    detail,
                )
            ),
        ]
    if name == "rejectInventoryTransferReview":
        await asyncio.to_thread(
            _post_agent,
            "/api/reject",
            {
                "approvalId": _required(values, "approvalId"),
                "accessProfile": access_profile,
            },
        )
        detail = "The review was cancelled. No database write was executed."
        return [
            Part(root=TextPart(text=detail)),
            *(
                create_a2ui_part(message)
                for message in result_messages("Review cancelled", detail)
            ),
        ]
    raise ValueError(f"Unsupported A2UI action: {name}")


def _extract_user_action(context) -> dict[str, Any] | None:
    message = getattr(context, "message", None)
    for part in getattr(message, "parts", None) or []:
        root = getattr(part, "root", None)
        if isinstance(root, DataPart) and isinstance(root.data, dict):
            action = root.data.get("userAction")
            if isinstance(action, dict):
                return action
    prompt = context.get_user_input("").strip()
    if prompt.startswith("{"):
        try:
            payload = json.loads(prompt)
            action = payload.get("userAction")
            if isinstance(action, dict):
                return action
        except json.JSONDecodeError:
            pass
    return None


def _query_parameters(prompt: str) -> tuple[float, int]:
    risk_match = re.search(
        r"(?:risk(?:\s+of)?|minimum)\D{0,20}(\d{1,3}(?:\.\d+)?)",
        prompt,
        re.IGNORECASE,
    )
    rows_match = re.search(
        r"(?:limit(?:ed)?\s+to|maximum|max)\D{0,10}(\d{1,2})",
        prompt,
        re.IGNORECASE,
    )
    minimum_risk = float(risk_match.group(1)) if risk_match else 70.0
    maximum_rows = int(rows_match.group(1)) if rows_match else 10
    if not 0 <= minimum_risk <= 100:
        raise ValueError("Minimum stockout risk must be between 0 and 100")
    if not 1 <= maximum_rows <= 50:
        raise ValueError("Maximum rows must be between 1 and 50")
    return minimum_risk, maximum_rows


def _post_agent(path: str, values: dict[str, Any]) -> dict[str, Any]:
    request = Request(
        f"{AGENT_SERVICE_URL}{path}",
        data=urlencode(values).encode("utf-8"),
        headers={"Content-Type": "application/x-www-form-urlencoded"},
        method="POST",
    )
    try:
        with urlopen(request, timeout=30) as response:
            return json.load(response)
    except HTTPError as exc:
        try:
            payload = json.load(exc)
            message = payload.get("error", str(exc))
        except Exception:
            message = str(exc)
        raise ValueError(message) from exc


def _required(values: dict[str, Any], name: str) -> str:
    value = str(values.get(name, "")).strip()
    if not value:
        raise ValueError(f"{name} is required")
    return value


handler = DefaultRequestHandler(
    agent_executor=SupplyChainExecutor(),
    task_store=InMemoryTaskStore(),
    push_config_store=InMemoryPushNotificationConfigStore(),
)
app = A2AStarletteApplication(
    agent_card=build_agent_card(),
    http_handler=handler,
).build()

if __name__ == "__main__":
    uvicorn.run(app, host=BIND_HOST, port=PORT)
