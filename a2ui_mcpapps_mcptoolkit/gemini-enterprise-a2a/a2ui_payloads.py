"""Deterministic A2UI v0.8 payloads for Gemini Enterprise."""

from __future__ import annotations

from typing import Any
from uuid import uuid4


def review_messages(
    recommendations: list[dict[str, Any]],
    approval_id: str,
    access_profile: str = "full",
    writes_allowed: bool = True,
) -> list[dict[str, Any]]:
    surface_id = f"inventory-transfer-review-{uuid4()}"
    children = ["title", "source"]
    components: list[dict[str, Any]] = [
        _component(
            "root",
            "Column",
            {"children": {"explicitList": children}},
        ),
        _text(
            "title",
            f"{len(recommendations)} governed inventory transfer recommendation(s)",
            "h2",
        ),
        _text(
            "source",
            (
                "Live results from the Oracle Database MCP Java Toolkit. "
                + (
                    "Deep Data Security: Environmental Monitoring only, read-only."
                    if access_profile == "environmental"
                    else "Supply-chain planner: all authorized categories."
                )
            ),
            "caption",
        ),
    ]

    for index, recommendation in enumerate(recommendations, start=1):
        card_id = f"recommendation-card-{index}"
        content_id = f"recommendation-content-{index}"
        approve_id = f"approve-{index}"
        approve_text_id = f"approve-text-{index}"
        children.append(card_id)
        card_children = [
            f"name-{index}",
            f"route-{index}",
            f"risk-{index}",
            f"rationale-{index}",
        ]
        if writes_allowed:
            card_children.append(approve_id)
        components.extend(
            [
                _component(card_id, "Card", {"child": content_id}),
                _component(
                    content_id,
                    "Column",
                    {
                        "children": {
                            "explicitList": card_children
                        }
                    },
                ),
                _text(
                    f"name-{index}",
                    f"{recommendation['sku']} - {recommendation['productName']}",
                    "h3",
                ),
                _text(
                    f"route-{index}",
                    (
                        f"{recommendation['sourceLocationCode']} to "
                        f"{recommendation['targetLocationCode']}: "
                        f"{recommendation['recommendedTransferQuantity']} units"
                    ),
                ),
                _text(
                    f"risk-{index}",
                    (
                        f"{recommendation['riskLevel']} stockout risk "
                        f"{recommendation['stockoutRiskScore']}"
                    ),
                ),
                _text(f"rationale-{index}", recommendation["rationale"]),
                *([_component(
                    approve_id,
                    "Button",
                    {
                        "child": approve_text_id,
                        "primary": True,
                        "action": {
                            "name": "approveInventoryTransfer",
                            "context": [
                                {
                                    "key": "approvalId",
                                    "value": {"literalString": approval_id},
                                },
                                {
                                    "key": "recommendationId",
                                    "value": {
                                        "literalString": recommendation[
                                            "recommendationId"
                                        ]
                                    },
                                },
                                {
                                    "key": "approvalNotes",
                                    "value": {"path": "/approvalNotes"},
                                },
                                {
                                    "key": "accessProfile",
                                    "value": {
                                        "literalString": access_profile
                                    },
                                },
                            ],
                        },
                    },
                ),
                _text(approve_text_id, "Approve this exact transfer")]
                  if writes_allowed else []),
            ]
        )

    if writes_allowed:
        children.extend(["approval-notes", "cancel-review", "cancel-text"])
        components.extend(
        [
            _component(
                "approval-notes",
                "TextField",
                {
                    "label": {"literalString": "Approval notes"},
                    "text": {"path": "/approvalNotes"},
                    "textFieldType": "longText",
                },
            ),
            _component(
                "cancel-review",
                "Button",
                {
                    "child": "cancel-text",
                    "primary": False,
                    "action": {
                        "name": "rejectInventoryTransferReview",
                        "context": [
                            {
                                "key": "approvalId",
                                "value": {"literalString": approval_id},
                            },
                            {
                                "key": "accessProfile",
                                "value": {"literalString": access_profile},
                            },
                        ],
                    },
                },
            ),
            _text("cancel-text", "Cancel review without writing"),
        ]
        )
    return [
        {
            "beginRendering": {
                "surfaceId": surface_id,
                "root": "root",
            }
        },
        {
            "surfaceUpdate": {
                "surfaceId": surface_id,
                "components": components,
            }
        },
        {
            "dataModelUpdate": {
                "surfaceId": surface_id,
                "contents": [
                    {
                        "key": "approvalNotes",
                        "valueString": (
                            "Approve the database-recommended transfer "
                            "to reduce stockout exposure."
                        ),
                    }
                ],
            }
        },
    ]


def result_messages(title: str, detail: str) -> list[dict[str, Any]]:
    surface_id = f"inventory-transfer-result-{uuid4()}"
    return [
        {
            "beginRendering": {
                "surfaceId": surface_id,
                "root": "root",
            }
        },
        {
            "surfaceUpdate": {
                "surfaceId": surface_id,
                "components": [
                    _component(
                        "root",
                        "Card",
                        {"child": "content"},
                    ),
                    _component(
                        "content",
                        "Column",
                        {"children": {"explicitList": ["title", "detail"]}},
                    ),
                    _text("title", title, "h2"),
                    _text("detail", detail),
                ],
            }
        },
    ]


def _component(
    component_id: str,
    component_type: str,
    properties: dict[str, Any],
) -> dict[str, Any]:
    return {
        "id": component_id,
        "component": {component_type: properties},
    }


def _text(
    component_id: str,
    value: str,
    usage_hint: str | None = None,
) -> dict[str, Any]:
    properties: dict[str, Any] = {
        "text": {"literalString": str(value)}
    }
    if usage_hint:
        properties["usageHint"] = usage_hint
    return _component(component_id, "Text", properties)
