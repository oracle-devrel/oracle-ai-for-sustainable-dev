from __future__ import annotations

import os
import re
from dataclasses import dataclass, field
from datetime import date
from typing import Any, Mapping

import oracledb

try:
    from google.adk.agents import LlmAgent, ParallelAgent, SequentialAgent
    from google.adk.runners import InMemoryRunner, RunConfig
    from google.adk.tools import FunctionTool
    from google.genai import types as genai_types

    ADK_IMPORT_ERROR: Exception | None = None
except Exception as exc:  # pragma: no cover - handled by deterministic fallback
    LlmAgent = None
    ParallelAgent = None
    SequentialAgent = None
    InMemoryRunner = None
    RunConfig = None
    FunctionTool = None
    genai_types = None
    ADK_IMPORT_ERROR = exc


APP_NAME = "oracle-inventory-action"
DEFAULT_MODEL_NAME = "gemini-2.0-flash"
DEFAULT_PRODUCT_ID = "SKU-500"
PRODUCT_ID_PATTERN = re.compile(r"\b([A-Z]{2,}-\d+)\b")
TRUTHY_VALUES = {"1", "true", "yes", "on"}

DATABASE_GRAPH_QUERY = """
WITH dependency_path AS (
    SELECT
        supplier_name,
        tier_level,
        supplier_region,
        on_time_pct,
        plant_name,
        cycle_days,
        utilization_pct,
        port_id,
        port_name,
        eta_hours,
        delay_risk_score,
        warehouse_name,
        inventory_units,
        fill_rate_pct,
        product_id,
        demand_change_pct,
        margin_pct
    FROM GRAPH_TABLE (
        supply_chain_graph
        MATCH
            (s IS supplier) -[e1 IS supplies]-> (p IS plant)
                            -[e2 IS ships_via]-> (po IS port)
                            -[e3 IS routes_to]-> (w IS warehouse)
                            -[e4 IS stocks]-> (pr IS product)
        WHERE pr.product_id = :product_id
          AND s.active_flag = 'Y'
          AND p.active_flag = 'Y'
          AND po.active_flag = 'Y'
          AND w.active_flag = 'Y'
          AND pr.active_flag = 'Y'
          AND e1.is_current = 'Y'
          AND e2.is_current = 'Y'
          AND e3.is_current = 'Y'
          AND e4.is_current = 'Y'
        COLUMNS (
            s.supplier_name AS supplier_name,
            s.tier_level AS tier_level,
            s.region_code AS supplier_region,
            s.on_time_pct AS on_time_pct,
            p.plant_name AS plant_name,
            p.cycle_days AS cycle_days,
            p.utilization_pct AS utilization_pct,
            po.port_id AS port_id,
            po.port_name AS port_name,
            po.eta_hours AS eta_hours,
            po.delay_risk_score AS delay_risk_score,
            w.warehouse_name AS warehouse_name,
            w.inventory_units AS inventory_units,
            w.fill_rate_pct AS fill_rate_pct,
            pr.product_id AS product_id,
            pr.demand_change_pct AS demand_change_pct,
            pr.margin_pct AS margin_pct
        )
    )
)
SELECT
    dp.supplier_name,
    dp.tier_level,
    dp.supplier_region,
    dp.on_time_pct,
    dp.plant_name,
    dp.cycle_days,
    dp.utilization_pct,
    dp.port_name,
    dp.eta_hours,
    dp.delay_risk_score,
    dp.warehouse_name,
    dp.inventory_units,
    dp.fill_rate_pct,
    dp.product_id,
    dp.demand_change_pct,
    dp.margin_pct,
    a.alert_name,
    a.lane_name,
    a.risk_score AS alert_risk
FROM dependency_path dp
LEFT JOIN sc_alert_port ap
  ON ap.port_id = dp.port_id
 AND ap.is_current = 'Y'
LEFT JOIN sc_alerts a
  ON a.alert_id = ap.alert_id
 AND a.active_flag = 'Y'
FETCH FIRST 1 ROW ONLY
"""

HOTSPOT_QUERY = """
SELECT
    summary.product_id,
    product.product_name,
    summary.quarter_label,
    summary.risk_level AS overall_risk_level,
    summary.stockout_probability,
    summary.projected_revenue_impact_usd,
    summary.primary_region,
    summary.recommendation_summary,
    snapshot.hotspot_rank,
    snapshot.hotspot_score,
    snapshot.coverage_days,
    snapshot.backlog_units,
    snapshot.service_level_pct,
    snapshot.at_risk_units,
    snapshot.revenue_impact_usd,
    snapshot.risk_level AS warehouse_risk_level,
    snapshot.recommended_role,
    warehouse.warehouse_name,
    geo.warehouse_code,
    geo.county_name,
    geo.state_code,
    geo.region_name,
    geo.latitude,
    geo.longitude
FROM sc_inventory_risk_summary summary
JOIN sc_products product
  ON product.product_id = summary.product_id
JOIN sc_warehouse_risk_snapshot snapshot
  ON snapshot.product_id = summary.product_id
 AND snapshot.active_flag = 'Y'
JOIN sc_warehouses warehouse
  ON warehouse.warehouse_id = snapshot.warehouse_id
JOIN sc_warehouse_geo geo
  ON geo.warehouse_id = snapshot.warehouse_id
WHERE summary.product_id = :product_id
  AND summary.active_flag = 'Y'
ORDER BY snapshot.hotspot_rank
"""


@dataclass(frozen=True)
class GraphSnapshot:
    supplier_name: str
    tier_level: Any
    supplier_region: str
    on_time_pct: Any
    plant_name: str
    cycle_days: Any
    utilization_pct: Any
    port_name: str
    eta_hours: Any
    delay_risk_score: Any
    warehouse_name: str
    inventory_units: Any
    fill_rate_pct: Any
    product_id: str
    demand_change_pct: Any
    margin_pct: Any
    alert_name: str
    lane_name: str
    alert_risk: Any


@dataclass(frozen=True)
class InventoryRiskSummary:
    product_id: str
    product_name: str
    quarter_label: str
    risk_level: str
    stockout_probability: float
    projected_revenue_impact_usd: float
    primary_region: str
    recommendation_summary: str


@dataclass(frozen=True)
class WarehouseHotspot:
    product_id: str
    warehouse_id: int
    warehouse_code: str
    warehouse_name: str
    county_name: str
    state_code: str
    region_name: str
    latitude: float
    longitude: float
    hotspot_rank: int
    hotspot_score: float
    coverage_days: float
    backlog_units: int
    service_level_pct: float
    at_risk_units: int
    revenue_impact_usd: float
    risk_level: str
    recommended_role: str


@dataclass(frozen=True)
class SpatialSnapshot:
    product_id: str
    summary: InventoryRiskSummary
    hotspots: list[WarehouseHotspot]
    source_mode: str
    source_detail: str
    summary_text: str


@dataclass(frozen=True)
class ExternalSignal:
    signal_name: str
    signal_summary: str
    risk_level: str
    recommended_lead_time_action: str


@dataclass
class InventoryActionResult:
    response_text: str
    trace: list[str]
    orchestration_mode: str
    draft_action: dict[str, Any] = field(default_factory=dict)
    policy_result: dict[str, Any] = field(default_factory=dict)


SEEDED_GRAPH_SNAPSHOTS: dict[str, GraphSnapshot] = {
    "SKU-500": GraphSnapshot(
        supplier_name="Vertex Plastics",
        tier_level=1,
        supplier_region="Busan",
        on_time_pct=92,
        plant_name="Columbus Final Pack",
        cycle_days=4.3,
        utilization_pct=78,
        port_name="Houston",
        eta_hours=54,
        delay_risk_score=0.31,
        warehouse_name="Newark Inventory Hub",
        inventory_units=4226,
        fill_rate_pct=95,
        product_id="SKU-500",
        demand_change_pct=8,
        margin_pct=30,
        alert_name="Weather Delay",
        lane_name="Pacific lane",
        alert_risk=0.60,
    ),
    "SKU-700": GraphSnapshot(
        supplier_name="Atlas Components",
        tier_level=1,
        supplier_region="Shenzhen",
        on_time_pct=96,
        plant_name="Austin Assembly",
        cycle_days=3.8,
        utilization_pct=84,
        port_name="Savannah",
        eta_hours=26,
        delay_risk_score=0.14,
        warehouse_name="DFW Hub",
        inventory_units=3180,
        fill_rate_pct=97,
        product_id="SKU-700",
        demand_change_pct=-3,
        margin_pct=26,
        alert_name="Customs Hold",
        lane_name="Atlantic lane",
        alert_risk=0.28,
    ),
    "SKU-900": GraphSnapshot(
        supplier_name="Meridian Metals",
        tier_level=1,
        supplier_region="Monterrey",
        on_time_pct=89,
        plant_name="Raleigh Integration",
        cycle_days=5.1,
        utilization_pct=73,
        port_name="Long Beach",
        eta_hours=62,
        delay_risk_score=0.41,
        warehouse_name="Chicago Crossdock",
        inventory_units=2875,
        fill_rate_pct=93,
        product_id="SKU-900",
        demand_change_pct=11,
        margin_pct=22,
        alert_name="Capacity Spike",
        lane_name="Gulf lane",
        alert_risk=0.44,
    ),
}

SEEDED_SUMMARIES: dict[str, InventoryRiskSummary] = {
    "SKU-500": InventoryRiskSummary(
        product_id="SKU-500",
        product_name="Sustainable Widget 500",
        quarter_label="2026-Q3",
        risk_level="HIGH",
        stockout_probability=0.72,
        projected_revenue_impact_usd=185000,
        primary_region="Northeast corridor",
        recommendation_summary="Rebalance inventory into Newark before the next delayed inbound cycle.",
    ),
    "SKU-700": InventoryRiskSummary(
        product_id="SKU-700",
        product_name="Low Carbon Kit 700",
        quarter_label="2026-Q3",
        risk_level="MEDIUM",
        stockout_probability=0.49,
        projected_revenue_impact_usd=96000,
        primary_region="Upper Midwest",
        recommendation_summary="Pre-stage buffer inventory in Chicago before demand spikes.",
    ),
    "SKU-900": InventoryRiskSummary(
        product_id="SKU-900",
        product_name="Circular Sensor 900",
        quarter_label="2026-Q3",
        risk_level="MEDIUM",
        stockout_probability=0.41,
        projected_revenue_impact_usd=87000,
        primary_region="Southeast",
        recommendation_summary="Maintain current buffers but watch Gulf-port capacity.",
    ),
}

SEEDED_HOTSPOTS: dict[str, list[WarehouseHotspot]] = {
    "SKU-500": [
        WarehouseHotspot(
            product_id="SKU-500",
            warehouse_id=4001,
            warehouse_code="WH-101",
            warehouse_name="Newark Inventory Hub",
            county_name="Essex",
            state_code="NJ",
            region_name="Northeast corridor",
            latitude=40.7357,
            longitude=-74.1724,
            hotspot_rank=1,
            hotspot_score=0.86,
            coverage_days=4.2,
            backlog_units=410,
            service_level_pct=91.0,
            at_risk_units=620,
            revenue_impact_usd=92000,
            risk_level="CRITICAL",
            recommended_role="DESTINATION_HOTSPOT",
        ),
        WarehouseHotspot(
            product_id="SKU-500",
            warehouse_id=4002,
            warehouse_code="WH-202",
            warehouse_name="DFW Hub",
            county_name="Tarrant",
            state_code="TX",
            region_name="Southern buffer",
            latitude=32.8998,
            longitude=-97.0403,
            hotspot_rank=2,
            hotspot_score=0.31,
            coverage_days=11.8,
            backlog_units=120,
            service_level_pct=97.0,
            at_risk_units=180,
            revenue_impact_usd=41000,
            risk_level="BUFFER",
            recommended_role="SOURCE_BUFFER",
        ),
        WarehouseHotspot(
            product_id="SKU-500",
            warehouse_id=4003,
            warehouse_code="WH-303",
            warehouse_name="Chicago Crossdock",
            county_name="Cook",
            state_code="IL",
            region_name="Midwest relay",
            latitude=41.9742,
            longitude=-87.9073,
            hotspot_rank=3,
            hotspot_score=0.48,
            coverage_days=8.1,
            backlog_units=190,
            service_level_pct=94.0,
            at_risk_units=210,
            revenue_impact_usd=52000,
            risk_level="WATCH",
            recommended_role="RELAY_NODE",
        ),
    ],
    "SKU-700": [
        WarehouseHotspot(
            product_id="SKU-700",
            warehouse_id=4003,
            warehouse_code="WH-303",
            warehouse_name="Chicago Crossdock",
            county_name="Cook",
            state_code="IL",
            region_name="Upper Midwest",
            latitude=41.9742,
            longitude=-87.9073,
            hotspot_rank=1,
            hotspot_score=0.74,
            coverage_days=5.0,
            backlog_units=360,
            service_level_pct=92.0,
            at_risk_units=430,
            revenue_impact_usd=61000,
            risk_level="HIGH",
            recommended_role="DESTINATION_HOTSPOT",
        ),
        WarehouseHotspot(
            product_id="SKU-700",
            warehouse_id=4002,
            warehouse_code="WH-202",
            warehouse_name="DFW Hub",
            county_name="Tarrant",
            state_code="TX",
            region_name="Southern buffer",
            latitude=32.8998,
            longitude=-97.0403,
            hotspot_rank=2,
            hotspot_score=0.36,
            coverage_days=10.4,
            backlog_units=110,
            service_level_pct=98.0,
            at_risk_units=170,
            revenue_impact_usd=22000,
            risk_level="BUFFER",
            recommended_role="SOURCE_BUFFER",
        ),
        WarehouseHotspot(
            product_id="SKU-700",
            warehouse_id=4001,
            warehouse_code="WH-101",
            warehouse_name="Newark Inventory Hub",
            county_name="Essex",
            state_code="NJ",
            region_name="Northeast",
            latitude=40.7357,
            longitude=-74.1724,
            hotspot_rank=3,
            hotspot_score=0.29,
            coverage_days=12.6,
            backlog_units=90,
            service_level_pct=96.0,
            at_risk_units=140,
            revenue_impact_usd=13000,
            risk_level="WATCH",
            recommended_role="SATELLITE_NODE",
        ),
    ],
    "SKU-900": [
        WarehouseHotspot(
            product_id="SKU-900",
            warehouse_id=4002,
            warehouse_code="WH-202",
            warehouse_name="DFW Hub",
            county_name="Tarrant",
            state_code="TX",
            region_name="Southeast feeder",
            latitude=32.8998,
            longitude=-97.0403,
            hotspot_rank=1,
            hotspot_score=0.63,
            coverage_days=6.5,
            backlog_units=280,
            service_level_pct=93.0,
            at_risk_units=300,
            revenue_impact_usd=47000,
            risk_level="HIGH",
            recommended_role="DESTINATION_HOTSPOT",
        ),
        WarehouseHotspot(
            product_id="SKU-900",
            warehouse_id=4003,
            warehouse_code="WH-303",
            warehouse_name="Chicago Crossdock",
            county_name="Cook",
            state_code="IL",
            region_name="Midwest relay",
            latitude=41.9742,
            longitude=-87.9073,
            hotspot_rank=2,
            hotspot_score=0.34,
            coverage_days=11.0,
            backlog_units=115,
            service_level_pct=97.0,
            at_risk_units=150,
            revenue_impact_usd=19000,
            risk_level="BUFFER",
            recommended_role="SOURCE_BUFFER",
        ),
        WarehouseHotspot(
            product_id="SKU-900",
            warehouse_id=4001,
            warehouse_code="WH-101",
            warehouse_name="Newark Inventory Hub",
            county_name="Essex",
            state_code="NJ",
            region_name="Northeast",
            latitude=40.7357,
            longitude=-74.1724,
            hotspot_rank=3,
            hotspot_score=0.28,
            coverage_days=13.2,
            backlog_units=80,
            service_level_pct=97.0,
            at_risk_units=110,
            revenue_impact_usd=12000,
            risk_level="WATCH",
            recommended_role="SATELLITE_NODE",
        ),
    ],
}


def first_non_blank(*candidates: str | None) -> str:
    for candidate in candidates:
        if candidate is not None and str(candidate).strip():
            return str(candidate).strip()
    return ""


def truthy(value: str | None) -> bool:
    return str(value or "").strip().lower() in TRUTHY_VALUES


def normalize_product_id(product_id: str | None) -> str:
    normalized = first_non_blank(product_id, DEFAULT_PRODUCT_ID).upper()
    return normalized or DEFAULT_PRODUCT_ID


def contains_product_id(user_input: str | None) -> bool:
    return PRODUCT_ID_PATTERN.search((user_input or "").upper()) is not None


def extract_product_id(user_input: str | None) -> str:
    matcher = PRODUCT_ID_PATTERN.search((user_input or "").upper())
    if matcher:
        return matcher.group(1)
    return DEFAULT_PRODUCT_ID


def string_value(value: Any) -> str:
    return "" if value is None else str(value)


def int_value(value: Any, default_value: int) -> int:
    if isinstance(value, bool):
        return int(value)
    if isinstance(value, (int, float)):
        return int(value)
    try:
        return int(string_value(value))
    except (TypeError, ValueError):
        return default_value


def value_or_default(value: Any, default_value: str) -> str:
    normalized = string_value(value).strip()
    return normalized or default_value


def to_sentence(value: Any) -> str:
    normalized = string_value(value).strip()
    if not normalized:
        return ""
    while normalized.endswith((".", "!", "?")):
        normalized = normalized[:-1].strip()
    return f"{normalized}."


def to_parenthetical(value: Any) -> str:
    normalized = string_value(value).strip()
    if not normalized:
        return "unknown error"
    while normalized.endswith((".", "!", "?")):
        normalized = normalized[:-1].strip()
    return normalized


def format_percent(value: Any) -> str:
    return f"{value_or_default(value, 'n/a')}%"


def format_signed_percent(value: Any) -> str:
    normalized = string_value(value).strip()
    if not normalized:
        return "n/a"
    return f"{normalized if normalized.startswith('-') else '+' + normalized}%"


def format_decimal(value: float) -> str:
    return f"{value:.2f}"


def combined_reason(active_alert: Any, signal_summary: Any) -> str:
    reason_parts: list[str] = []
    if string_value(active_alert).strip():
        reason_parts.append(f"Graph: {to_parenthetical(active_alert)}.")
    if string_value(signal_summary).strip():
        reason_parts.append(f"External: {to_parenthetical(signal_summary)}.")
    if not reason_parts:
        return "Inventory risk evidence supports a balancing transfer."
    return " ".join(reason_parts)


def dependency_path(nodes: list[dict[str, str]]) -> str:
    labels = [node.get("label", "").strip() for node in nodes if node.get("label", "").strip()]
    return " -> ".join(labels) if labels else "No dependency path available"


def first_node_of_type(nodes: list[dict[str, str]], node_type: str) -> dict[str, str]:
    for node in nodes:
        if node.get("type", "").upper() == node_type.upper():
            return node
    return {}


def pick_destination_hotspot(hotspots: list[WarehouseHotspot]) -> WarehouseHotspot:
    for hotspot in hotspots:
        if "DESTINATION" in hotspot.recommended_role:
            return hotspot
    return hotspots[0]


def pick_source_hotspot(hotspots: list[WarehouseHotspot]) -> WarehouseHotspot:
    for hotspot in hotspots:
        if "SOURCE" in hotspot.recommended_role:
            return hotspot
    return hotspots[-1]


def build_summary_text(summary: InventoryRiskSummary, hotspots: list[WarehouseHotspot]) -> str:
    destination = pick_destination_hotspot(hotspots)
    source = pick_source_hotspot(hotspots)
    return (
        f"Hotspot map for {summary.product_id}: "
        f"{destination.warehouse_name} in {destination.county_name}, {destination.state_code} "
        f"is the primary pressure node (score {format_decimal(destination.hotspot_score)}, "
        f"coverage {format_decimal(destination.coverage_days)} days). "
        f"{source.warehouse_name} is the best relief source."
    )


def external_signal_for(product_id: str) -> ExternalSignal:
    if product_id == "SKU-600":
        return ExternalSignal(
            signal_name="Rail congestion watch",
            signal_summary="Midwest rail congestion is extending inbound replenishment by roughly 18 hours.",
            risk_level="medium",
            recommended_lead_time_action="Shift transfer timing forward by one day.",
        )
    if product_id == "SKU-700":
        return ExternalSignal(
            signal_name="Port labor caution",
            signal_summary="Northeast port labor uncertainty could delay replenishment windows next week.",
            risk_level="medium",
            recommended_lead_time_action="Pre-stage transfer inventory before the next inbound delay window.",
        )
    return ExternalSignal(
        signal_name="Weather delay",
        signal_summary="Pacific lane weather is raising delay risk on the inbound route that feeds Newark.",
        risk_level="high",
        recommended_lead_time_action="Prioritize an internal transfer before the next external replenishment cycle.",
    )


class InventoryActionTools:
    def __init__(self, environment: Mapping[str, str] | None = None):
        self.environment = dict(environment or os.environ)

    def getGraphEvidence(self, productId: str) -> dict[str, Any]:
        """Look up supply-chain dependency evidence for a product and summarize the path and alert."""
        normalized_product_id = normalize_product_id(productId)
        try:
            snapshot, source_mode, source_detail = self._resolve_graph_snapshot(normalized_product_id)
            nodes = self._graph_nodes_for(snapshot, normalized_product_id)
            alert_node = first_node_of_type(nodes, "ALERT")
            warehouse_node = first_node_of_type(nodes, "WAREHOUSE")
            return {
                "status": "ok",
                "productId": normalized_product_id,
                "dependencyPath": dependency_path(nodes),
                "warehouse": warehouse_node.get("label", "Warehouse unavailable"),
                "warehouseMetric": warehouse_node.get("metric", ""),
                "activeAlert": alert_node.get("label", "Alert unavailable"),
                "alertDetail": alert_node.get("detail", ""),
                "alertMetric": alert_node.get("metric", ""),
                "sourceMode": source_mode,
                "sourceDetail": source_detail,
            }
        except Exception as exc:
            return {
                "status": "unavailable",
                "productId": normalized_product_id,
                "message": str(exc),
            }

    def getSpatialEvidence(self, productId: str) -> dict[str, Any]:
        """Return hotspot evidence and a suggested source and destination warehouse."""
        normalized_product_id = normalize_product_id(productId)
        snapshot = self._resolve_spatial_snapshot(normalized_product_id)
        destination = pick_destination_hotspot(snapshot.hotspots)
        source = pick_source_hotspot(snapshot.hotspots)
        return {
            "status": "ok",
            "productId": snapshot.product_id,
            "hotspotRegion": snapshot.summary.primary_region,
            "hotspotSummary": snapshot.summary_text,
            "recommendedSourceWarehouse": f"Warehouse: {source.warehouse_name}",
            "recommendedDestinationWarehouse": f"Warehouse: {destination.warehouse_name}",
            "suggestedTransferUnits": 500,
            "coverageRiskDays": destination.coverage_days,
            "sourceDetail": snapshot.source_detail,
        }

    def getExternalSignals(self, productId: str) -> dict[str, Any]:
        """Return seeded outside-risk context that can change the timing or urgency of an action."""
        normalized_product_id = normalize_product_id(productId)
        signal = external_signal_for(normalized_product_id)
        return {
            "status": "ok",
            "productId": normalized_product_id,
            "signalName": signal.signal_name,
            "signalSummary": signal.signal_summary,
            "riskLevel": signal.risk_level,
            "recommendedLeadTimeAction": signal.recommended_lead_time_action,
            "observedDate": date.today().isoformat(),
            "sourceDetail": "Seeded external signal summary",
        }

    def checkTransferPolicy(
        self,
        productId: str,
        sourceWarehouse: str,
        destinationWarehouse: str,
        units: int,
        reason: str,
    ) -> dict[str, Any]:
        """Check whether a proposed transfer is allowed immediately or requires approval."""
        normalized_product_id = normalize_product_id(productId)
        normalized_units = 0 if units is None else int(units)
        same_warehouse = sourceWarehouse.strip().lower() == destinationWarehouse.strip().lower()
        requires_approval = normalized_units >= 400 or same_warehouse
        allowed = not same_warehouse and normalized_units > 0
        return {
            "status": "ok" if allowed else "blocked",
            "productId": normalized_product_id,
            "allowed": allowed,
            "requiresApproval": requires_approval,
            "policySummary": (
                "Transfer can be drafted but requires approval before execution."
                if allowed and requires_approval
                else "Transfer can be drafted immediately with standard review."
                if allowed
                else "Transfer is blocked because source and destination warehouses are the same or units are missing."
            ),
            "unitsThresholdForApproval": 400,
            "reason": value_or_default(reason, "No reason supplied"),
        }

    def draftInventoryTransferAction(
        self,
        productId: str,
        sourceWarehouse: str,
        destinationWarehouse: str,
        units: int,
        reason: str,
    ) -> dict[str, Any]:
        """Create a draft inventory-transfer action without executing the move."""
        normalized_product_id = normalize_product_id(productId)
        normalized_units = 0 if units is None else int(units)
        return {
            "status": "drafted",
            "actionType": "INVENTORY_TRANSFER",
            "draftActionId": f"draft-transfer-{normalized_product_id.lower()}",
            "productId": normalized_product_id,
            "sourceWarehouse": value_or_default(sourceWarehouse, "Unknown source"),
            "destinationWarehouse": value_or_default(destinationWarehouse, "Unknown destination"),
            "units": normalized_units,
            "reason": value_or_default(reason, "No reason supplied"),
            "executionState": "NOT_EXECUTED",
            "approvalState": "PENDING_APPROVAL" if normalized_units >= 400 else "STANDARD_REVIEW",
        }

    def _resolve_graph_snapshot(self, product_id: str) -> tuple[GraphSnapshot, str, str]:
        database_error = ""
        if self._database_is_configured():
            try:
                return (
                    self._database_graph_snapshot(product_id),
                    "database",
                    "Oracle Database property graph",
                )
            except Exception as exc:
                database_error = str(exc)

        seeded = SEEDED_GRAPH_SNAPSHOTS.get(product_id) or SEEDED_GRAPH_SNAPSHOTS.get(DEFAULT_PRODUCT_ID)
        if seeded is None:
            raise ValueError(f"No supply-chain evidence is available for productId {product_id}.")
        return (
            GraphSnapshot(
                supplier_name=seeded.supplier_name,
                tier_level=seeded.tier_level,
                supplier_region=seeded.supplier_region,
                on_time_pct=seeded.on_time_pct,
                plant_name=seeded.plant_name,
                cycle_days=seeded.cycle_days,
                utilization_pct=seeded.utilization_pct,
                port_name=seeded.port_name,
                eta_hours=seeded.eta_hours,
                delay_risk_score=seeded.delay_risk_score,
                warehouse_name=seeded.warehouse_name,
                inventory_units=seeded.inventory_units,
                fill_rate_pct=seeded.fill_rate_pct,
                product_id=product_id,
                demand_change_pct=seeded.demand_change_pct,
                margin_pct=seeded.margin_pct,
                alert_name=seeded.alert_name,
                lane_name=seeded.lane_name,
                alert_risk=seeded.alert_risk,
            ),
            "seeded",
            "Seeded supply-chain dependency fallback"
            + (f" ({database_error})" if database_error else ""),
        )

    def _graph_nodes_for(self, snapshot: GraphSnapshot, product_id: str) -> list[dict[str, str]]:
        return [
            {
                "id": "supplier",
                "label": f"Supplier: {snapshot.supplier_name}",
                "type": "SUPPLIER",
                "detail": f"Tier {value_or_default(snapshot.tier_level, '?')} | "
                f"{value_or_default(snapshot.supplier_region, 'Unknown')}",
                "metric": f"On-time {format_percent(snapshot.on_time_pct)}",
            },
            {
                "id": "plant",
                "label": f"Plant: {snapshot.plant_name}",
                "type": "PLANT",
                "detail": f"Cycle {value_or_default(snapshot.cycle_days, 'n/a')} days",
                "metric": f"Utilization {format_percent(snapshot.utilization_pct)}",
            },
            {
                "id": "port",
                "label": f"Port: {snapshot.port_name}",
                "type": "PORT",
                "detail": f"ETA {value_or_default(snapshot.eta_hours, 'n/a')} hrs",
                "metric": f"Delay risk {value_or_default(snapshot.delay_risk_score, 'n/a')}",
            },
            {
                "id": "warehouse",
                "label": f"Warehouse: {snapshot.warehouse_name}",
                "type": "WAREHOUSE",
                "detail": f"Inventory {value_or_default(snapshot.inventory_units, 'n/a')} units",
                "metric": f"Fill rate {format_percent(snapshot.fill_rate_pct)}",
            },
            {
                "id": "product",
                "label": f"Product: {product_id}",
                "type": "PRODUCT",
                "detail": f"Demand {format_signed_percent(snapshot.demand_change_pct)}",
                "metric": f"Margin {format_percent(snapshot.margin_pct)}",
            },
            {
                "id": "alert",
                "label": f"Alert: {value_or_default(snapshot.alert_name, 'No Active Alert')}",
                "type": "ALERT",
                "detail": value_or_default(snapshot.lane_name, "No active lane"),
                "metric": f"Risk {value_or_default(snapshot.alert_risk, 'n/a')}",
            },
        ]

    def _resolve_spatial_snapshot(self, product_id: str) -> SpatialSnapshot:
        database_error = ""
        if self._database_is_configured():
            try:
                return self._database_spatial_snapshot(product_id)
            except Exception as exc:
                database_error = str(exc)

        summary = SEEDED_SUMMARIES.get(product_id) or SEEDED_SUMMARIES[DEFAULT_PRODUCT_ID]
        hotspots = list(SEEDED_HOTSPOTS.get(product_id) or SEEDED_HOTSPOTS[DEFAULT_PRODUCT_ID])
        return SpatialSnapshot(
            product_id=summary.product_id,
            summary=summary,
            hotspots=hotspots,
            source_mode="seeded",
            source_detail="Seeded spatial hotspot fallback"
            + (f" ({database_error})" if database_error else ""),
            summary_text=build_summary_text(summary, hotspots),
        )

    def _database_is_configured(self) -> bool:
        if truthy(self.environment.get("ACTION_FORCE_SEEDED")):
            return False
        required = (
            self.environment.get("DB_USERNAME"),
            self.environment.get("DB_PASSWORD"),
            self.environment.get("DB_DSN"),
        )
        tns_admin = first_non_blank(
            self.environment.get("TNS_ADMIN"),
            self.environment.get("DB_WALLET_DIR"),
        )
        return all(value is not None and str(value).strip() for value in required) and bool(tns_admin)

    def _open_connection(self) -> oracledb.Connection:
        username = first_non_blank(self.environment.get("DB_USERNAME"))
        password = first_non_blank(self.environment.get("DB_PASSWORD"))
        dsn = first_non_blank(self.environment.get("DB_DSN"))
        wallet_password = first_non_blank(self.environment.get("DB_WALLET_PASSWORD"))
        tns_admin = first_non_blank(
            self.environment.get("TNS_ADMIN"),
            self.environment.get("DB_WALLET_DIR"),
        )
        if not username or not password or not dsn:
            raise ValueError("DB_USERNAME, DB_PASSWORD, and DB_DSN must be configured for database access.")
        if not tns_admin:
            raise ValueError("TNS_ADMIN or DB_WALLET_DIR must be configured for database access.")

        return oracledb.connect(
            user=username,
            password=password,
            dsn=dsn,
            config_dir=tns_admin,
            wallet_location=tns_admin,
            wallet_password=wallet_password or None,
            ssl_server_dn_match=False,
        )

    def _database_graph_snapshot(self, product_id: str) -> GraphSnapshot:
        with self._open_connection() as connection:
            with connection.cursor() as cursor:
                cursor.execute(DATABASE_GRAPH_QUERY, product_id=product_id)
                row = cursor.fetchone()
                if row is None:
                    raise ValueError(f"No Oracle graph path was found for productId {product_id}.")
                columns = [description[0].lower() for description in cursor.description or []]
        record = dict(zip(columns, row))
        return GraphSnapshot(
            supplier_name=string_value(record.get("supplier_name")),
            tier_level=record.get("tier_level"),
            supplier_region=string_value(record.get("supplier_region")),
            on_time_pct=record.get("on_time_pct"),
            plant_name=string_value(record.get("plant_name")),
            cycle_days=record.get("cycle_days"),
            utilization_pct=record.get("utilization_pct"),
            port_name=string_value(record.get("port_name")),
            eta_hours=record.get("eta_hours"),
            delay_risk_score=record.get("delay_risk_score"),
            warehouse_name=string_value(record.get("warehouse_name")),
            inventory_units=record.get("inventory_units"),
            fill_rate_pct=record.get("fill_rate_pct"),
            product_id=product_id,
            demand_change_pct=record.get("demand_change_pct"),
            margin_pct=record.get("margin_pct"),
            alert_name=string_value(record.get("alert_name")),
            lane_name=string_value(record.get("lane_name")),
            alert_risk=record.get("alert_risk"),
        )

    def _database_spatial_snapshot(self, product_id: str) -> SpatialSnapshot:
        with self._open_connection() as connection:
            with connection.cursor() as cursor:
                cursor.execute(HOTSPOT_QUERY, product_id=product_id)
                rows = cursor.fetchall()
                if not rows:
                    raise ValueError(f"No spatial hotspot rows were found for productId {product_id}.")
                columns = [description[0].lower() for description in cursor.description or []]
        records = [dict(zip(columns, row)) for row in rows]
        first = records[0]
        summary = InventoryRiskSummary(
            product_id=string_value(first.get("product_id")),
            product_name=string_value(first.get("product_name")),
            quarter_label=string_value(first.get("quarter_label")),
            risk_level=string_value(first.get("overall_risk_level")),
            stockout_probability=float(first.get("stockout_probability") or 0),
            projected_revenue_impact_usd=float(first.get("projected_revenue_impact_usd") or 0),
            primary_region=string_value(first.get("primary_region")),
            recommendation_summary=string_value(first.get("recommendation_summary")),
        )
        hotspots = [
            WarehouseHotspot(
                product_id=string_value(record.get("product_id")),
                warehouse_id=int_value(record.get("warehouse_id"), 0),
                warehouse_code=string_value(record.get("warehouse_code")),
                warehouse_name=string_value(record.get("warehouse_name")),
                county_name=string_value(record.get("county_name")),
                state_code=string_value(record.get("state_code")),
                region_name=string_value(record.get("region_name")),
                latitude=float(record.get("latitude") or 0),
                longitude=float(record.get("longitude") or 0),
                hotspot_rank=int_value(record.get("hotspot_rank"), 0),
                hotspot_score=float(record.get("hotspot_score") or 0),
                coverage_days=float(record.get("coverage_days") or 0),
                backlog_units=int_value(record.get("backlog_units"), 0),
                service_level_pct=float(record.get("service_level_pct") or 0),
                at_risk_units=int_value(record.get("at_risk_units"), 0),
                revenue_impact_usd=float(record.get("revenue_impact_usd") or 0),
                risk_level=string_value(record.get("warehouse_risk_level")),
                recommended_role=string_value(record.get("recommended_role")),
            )
            for record in records
        ]
        hotspots.sort(key=lambda hotspot: hotspot.hotspot_rank)
        return SpatialSnapshot(
            product_id=product_id,
            summary=summary,
            hotspots=hotspots,
            source_mode="database",
            source_detail="Oracle warehouse hotspot tables",
            summary_text=build_summary_text(summary, hotspots),
        )


class InventoryActionCoordinator:
    def __init__(self, environment: Mapping[str, str] | None = None):
        self.environment = dict(environment or os.environ)
        self.tools = InventoryActionTools(self.environment)
        self._runner: InMemoryRunner | None = None

    async def run(self, user_input: str | None, context_id: str | None) -> InventoryActionResult:
        normalized_input = (
            "Recommend an inventory action for SKU-500."
            if user_input is None or not user_input.strip()
            else user_input.strip()
        )
        if not contains_product_id(normalized_input):
            normalized_input = (
                normalized_input
                + "\nUse SKU-500 as the default product id when the request does not specify one."
            )
        try:
            return await self._run_adk(normalized_input, context_id)
        except Exception as exc:
            return self._run_deterministic_fallback(normalized_input, exc)

    async def _run_adk(self, normalized_input: str, context_id: str | None) -> InventoryActionResult:
        runner = self._ensure_runner()
        user_id = first_non_blank(context_id, "inventory-action-user")
        session_id = first_non_blank(context_id, "inventory-action-session")
        session = await runner.session_service.get_session(
            app_name=APP_NAME,
            user_id=user_id,
            session_id=session_id,
        )
        if session is None:
            session = await runner.session_service.create_session(
                app_name=APP_NAME,
                user_id=user_id,
                state={},
                session_id=session_id,
            )

        content = genai_types.Content(
            role="user",
            parts=[genai_types.Part(text=normalized_input)],
        )

        trace: list[str] = []
        final_text = ""
        async for event in runner.run_async(
            user_id=user_id,
            session_id=session.id,
            new_message=content,
            run_config=RunConfig(),
        ):
            event_text = self._event_text(event)
            if event_text:
                author = string_value(getattr(event, "author", "")).strip()
                trace.append(f"{author}: {event_text}" if author else event_text)
            if getattr(event, "content", None) and getattr(event.content, "role", "") == "model":
                if event_text and event.is_final_response():
                    final_text = event_text

        resolved_text = final_text or next(
            (entry.split(": ", 1)[1] if ": " in entry else entry for entry in reversed(trace) if entry.strip()),
            "The inventory action coordinator did not return a final recommendation.",
        )
        return InventoryActionResult(
            response_text=resolved_text,
            trace=trace,
            orchestration_mode="adk",
        )

    def _run_deterministic_fallback(
        self,
        user_input: str,
        exception: Exception,
    ) -> InventoryActionResult:
        product_id = extract_product_id(user_input)
        graph_evidence = self.tools.getGraphEvidence(product_id)
        spatial_evidence = self.tools.getSpatialEvidence(product_id)
        external_signals = self.tools.getExternalSignals(product_id)

        source_warehouse = string_value(spatial_evidence.get("recommendedSourceWarehouse"))
        destination_warehouse = string_value(spatial_evidence.get("recommendedDestinationWarehouse"))
        units = int_value(spatial_evidence.get("suggestedTransferUnits"), 250)

        active_alert = string_value(graph_evidence.get("activeAlert"))
        signal_summary = string_value(external_signals.get("signalSummary"))
        reason = combined_reason(active_alert, signal_summary)

        policy_result = self.tools.checkTransferPolicy(
            product_id,
            source_warehouse,
            destination_warehouse,
            units,
            reason,
        )
        draft_result = self.tools.draftInventoryTransferAction(
            product_id,
            source_warehouse,
            destination_warehouse,
            units,
            reason,
        )

        approval_line = (
            "Approval is required before execution."
            if policy_result.get("requiresApproval") is True
            else "Only standard review is required before execution."
        )
        dependency = string_value(graph_evidence.get("dependencyPath"))
        hotspot_summary = string_value(spatial_evidence.get("hotspotSummary"))

        rationale_parts: list[str] = []
        if dependency:
            rationale_parts.append(to_sentence(dependency))
        if active_alert:
            rationale_parts.append(to_sentence(active_alert))
        if hotspot_summary:
            rationale_parts.append(to_sentence(hotspot_summary))
        if signal_summary:
            rationale_parts.append(to_sentence(signal_summary))

        response_text = (
            f"Fallback recommendation for {product_id}: transfer {units} units from "
            f"{source_warehouse} to {destination_warehouse}. "
            f"Why: {' '.join(rationale_parts)} "
            f"{to_sentence(approval_line)} Draft action id: "
            f"{string_value(draft_result.get('draftActionId'))}. "
            f"Policy check: {to_sentence(string_value(policy_result.get('policySummary')))} "
            f"The ADK model path was unavailable, so this response used deterministic local "
            f"orchestration instead ({to_parenthetical(str(exception))})."
        )

        return InventoryActionResult(
            response_text=response_text,
            trace=[
                f"graphEvidence={graph_evidence}",
                f"spatialEvidence={spatial_evidence}",
                f"externalSignals={external_signals}",
                f"policyResult={policy_result}",
                f"draftResult={draft_result}",
            ],
            orchestration_mode="deterministic-fallback",
            draft_action=draft_result,
            policy_result=policy_result,
        )

    def _ensure_runner(self) -> InMemoryRunner:
        if truthy(self.environment.get("ACTION_DISABLE_ADK")):
            raise RuntimeError("ACTION_DISABLE_ADK is enabled.")
        if ADK_IMPORT_ERROR is not None:
            raise RuntimeError(f"Google ADK is unavailable: {ADK_IMPORT_ERROR}")
        if self._runner is None:
            self._runner = InMemoryRunner(
                agent=self._build_root_agent(),
                app_name=APP_NAME,
            )
        return self._runner

    def _build_root_agent(self) -> SequentialAgent:
        model_name = first_non_blank(
            self.environment.get("ACTION_COORDINATOR_MODEL"),
            self.environment.get("MODEL_NAME"),
            DEFAULT_MODEL_NAME,
        )
        graph_tool = FunctionTool(self.tools.getGraphEvidence)
        spatial_tool = FunctionTool(self.tools.getSpatialEvidence)
        external_tool = FunctionTool(self.tools.getExternalSignals)
        policy_tool = FunctionTool(self.tools.checkTransferPolicy)
        draft_tool = FunctionTool(self.tools.draftInventoryTransferAction)

        graph_agent = LlmAgent(
            name="graph_evidence_specialist",
            description="Oracle Graph specialist for supply-chain dependency evidence.",
            model=model_name,
            instruction=(
                "You are the graph-evidence specialist for inventory risk response.\n"
                "Always call getGraphEvidence for the relevant productId before answering.\n"
                "Return only a concise supply-chain evidence summary and never recommend an action."
            ),
            tools=[graph_tool],
        )
        spatial_agent = LlmAgent(
            name="spatial_evidence_specialist",
            description="Spatial hotspot specialist for warehouse pressure and transfer direction.",
            model=model_name,
            instruction=(
                "You are the spatial-evidence specialist for inventory risk response.\n"
                "Always call getSpatialEvidence for the relevant productId before answering.\n"
                "Return only a concise hotspot summary with recommended source and destination warehouses.\n"
                "Do not make a final action recommendation."
            ),
            tools=[spatial_tool],
        )
        external_agent = LlmAgent(
            name="external_signal_specialist",
            description="External-risk specialist for weather and geopolitical supply-lane impacts.",
            model=model_name,
            instruction=(
                "You are the external-signals specialist for inventory risk response.\n"
                "Always call getExternalSignals for the relevant productId before answering.\n"
                "Return only a concise summary of outside factors that could change the timing or urgency of an action.\n"
                "Do not make a final action recommendation."
            ),
            tools=[external_tool],
        )
        parallel_agent = ParallelAgent(
            name="parallel_evidence_gatherer",
            description=(
                "Runs graph, spatial, and external-signal specialists in parallel before an "
                "action recommendation is made."
            ),
            sub_agents=[graph_agent, spatial_agent, external_agent],
        )
        decision_agent = LlmAgent(
            name="inventory_action_decider",
            description="Synthesizes evidence, checks policy, and drafts an inventory action recommendation.",
            model=model_name,
            instruction=(
                "You are the final inventory-action coordinator.\n"
                "Review the graph, spatial, and external evidence already gathered in this session.\n"
                "Your job is to recommend one next step: transfer, expedite, substitute, or hold.\n"
                "If a transfer is the best next move, call checkTransferPolicy first and then call draftInventoryTransferAction.\n"
                "Never claim that an inventory move has been executed.\n"
                "Your final answer must include:\n"
                "1. Recommended action.\n"
                "2. Why that action is justified from the evidence.\n"
                "3. Whether approval is required.\n"
                "4. If you drafted a move, the draft action id and the proposed source, destination, and units.\n"
                "If evidence is missing, say so plainly and recommend the safest next step."
            ),
            tools=[policy_tool, draft_tool],
        )
        return SequentialAgent(
            name="inventory_action_orchestrator",
            description=(
                "Coordinates final-stage inventory action planning using evidence specialists "
                "and a decision agent."
            ),
            sub_agents=[parallel_agent, decision_agent],
        )

    @staticmethod
    def _event_text(event: Any) -> str:
        content = getattr(event, "content", None)
        if content is None or not getattr(content, "parts", None):
            return ""
        parts = [string_value(getattr(part, "text", "")).strip() for part in content.parts]
        return "\n".join(text for text in parts if text).strip()


def build_rpc_url(environment: Mapping[str, str] | None = None) -> str:
    env = dict(environment or os.environ)
    explicit = first_non_blank(env.get("ACTION_AGENT_URL"), env.get("A2A_URL"))
    if explicit:
        return explicit.rstrip("/")
    public_protocol = first_non_blank(env.get("PUBLIC_PROTOCOL"), "http")
    public_host = first_non_blank(env.get("PUBLIC_HOST"), "localhost")
    port = first_non_blank(env.get("ACTION_AGENT_PORT"), env.get("PORT"), "8080")
    return f"{public_protocol}://{public_host}:{port}"
