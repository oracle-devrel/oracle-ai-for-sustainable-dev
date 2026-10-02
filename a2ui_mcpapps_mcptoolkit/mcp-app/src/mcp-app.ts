import { App } from "@modelcontextprotocol/ext-apps";

declare const maplibregl: {
  Map: new (options: Record<string, unknown>) => MapLibreMap;
};

type MapLibreMap = {
  addSource: (id: string, source: Record<string, unknown>) => void;
  addLayer: (layer: Record<string, unknown>) => void;
  fitBounds: (bounds: MapLibreBounds, options?: Record<string, unknown>) => void;
  on: (...args: unknown[]) => void;
};
type MapLibreBounds = { extend: (coordinate: [number, number]) => MapLibreBounds };
type MapLibreEvent = { features?: Array<{ properties?: Record<string, unknown> }> };

type TransferRecommendation = {
  recommendationId: string;
  sku: string;
  productName: string;
  sourceLocationCode: string;
  targetLocationCode: string;
  shortageQuantity: number;
  recommendedTransferQuantity: number;
  transitDays: number;
  unitTransferCost: number;
  stockoutRiskScore: number;
  riskLevel: string;
  rationale: string;
};

const app = new App({
  name: "Oracle Supply-Chain Inventory Exchange",
  version: "0.1.0"
});
const metrics =
  document.querySelector<HTMLDivElement>("#metrics")!;
const recommendationsElement =
  document.querySelector<HTMLDivElement>("#recommendations")!;
const sourceElement =
  document.querySelector<HTMLParagraphElement>("#source")!;
const modeElement =
  document.querySelector<HTMLParagraphElement>("#mode")!;
const decisionElement =
  document.querySelector<HTMLElement>("#decision")!;
const selectionElement =
  document.querySelector<HTMLElement>("#selection")!;
const notesElement =
  document.querySelector<HTMLTextAreaElement>("#approval-notes")!;
const approveElement =
  document.querySelector<HTMLButtonElement>("#approve")!;
const cancelElement =
  document.querySelector<HTMLButtonElement>("#cancel")!;
const statusElement =
  document.querySelector<HTMLParagraphElement>("#status")!;
const spatialView = document.querySelector<HTMLElement>("#spatial-view")!;
const spatialSource = document.querySelector<HTMLParagraphElement>("#spatial-source")!;
const spatialMap = document.querySelector<HTMLDivElement>("#spatial-map")!;

let approvalId: string | undefined;
let selectedRecommendation: TransferRecommendation | undefined;

app.ontoolresult = (result) => {
  const rawPayload = result.structuredContent as {
    view?: string;
    source?: string;
    sku?: string;
    geojson?: { type: string; features: unknown[] };
    hotspots?: unknown[];
  } | undefined;
  if (rawPayload?.view === "spatial-hotspots" && rawPayload.geojson) {
    renderSpatial({
      source: rawPayload.source,
      sku: rawPayload.sku,
      geojson: rawPayload.geojson
    });
    return;
  }
  spatialView.hidden = true;
  const payload = result.structuredContent as {
    recommendations?: TransferRecommendation[];
    source?: string;
    minimumStockoutRisk?: number;
  } | undefined;
  approvalId =
    result._meta && typeof result._meta.approvalId === "string"
      ? result._meta.approvalId
      : undefined;
  selectedRecommendation = undefined;
  sourceElement.textContent =
    payload?.source === "oracle-db-mcp-java-toolkit"
      ? "Live governed results from the Oracle Database MCP Java Toolkit"
        + ` · minimum stockout risk ${payload.minimumStockoutRisk}`
      : "Waiting for governed Toolkit results.";
  render(payload?.recommendations ?? []);
};

function renderSpatial(payload: {
  source?: string;
  sku?: string;
  geojson: { type: string; features: unknown[] };
}) {
  document.querySelector<HTMLElement>("#metrics")!.replaceChildren();
  document.querySelector<HTMLElement>("#recommendations")!.replaceChildren();
  document.querySelector<HTMLElement>("#decision")!.hidden = true;
  spatialView.hidden = false;
  spatialSource.textContent =
    `${payload.source ?? "Oracle Database"} · ${payload.sku ?? "inventory"} · read-only spatial evidence`;
  spatialMap.replaceChildren();
  if (typeof maplibregl === "undefined") {
    spatialMap.textContent = "MapLibre GL JS could not be loaded by this host.";
    return;
  }
  const map = new maplibregl.Map({
    container: spatialMap,
    attributionControl: true,
    style: {
      version: 8,
      sources: {},
      layers: [{ id: "background", type: "background", paint: { "background-color": "#eef3f6" } }]
    },
    center: [-96, 38],
    zoom: 3
  });
  map.on("load", () => {
    map.addSource("inventory-spatial", { type: "geojson", data: payload.geojson });
    map.addLayer({
      id: "relief-route",
      type: "line",
      source: "inventory-spatial",
      filter: ["==", ["get", "kind"], "relief-route"],
      paint: { "line-color": "#1769aa", "line-width": 4, "line-dasharray": [2, 1] }
    });
    map.addLayer({
      id: "hotspots",
      type: "circle",
      source: "inventory-spatial",
      filter: ["has", "locationCode"],
      paint: {
        "circle-color": ["match", ["get", "recommendedRole"], "SOURCE", "#2f7d32", "#c74634"],
        "circle-radius": ["interpolate", ["linear"], ["get", "stockoutRiskScore"], 0, 8, 100, 24],
        "circle-stroke-color": "#ffffff",
        "circle-stroke-width": 2,
        "circle-opacity": 0.85
      }
    });
    map.on("click", "hotspots", (event: MapLibreEvent) => {
      const properties = event.features?.[0]?.properties ?? {};
      spatialSource.textContent =
        `${properties.locationCode ?? "Warehouse"} · ${properties.locationName ?? ""} · `
        + `${properties.recommendedRole ?? ""} · risk ${properties.stockoutRiskScore ?? "n/a"}`;
    });
  });
}

async function connectApp() {
  try {
    await app.connect();
  } catch (error) {
    statusElement.textContent =
      "The MCP Apps host bridge could not initialize.";
    console.error("MCP App bridge initialization failed", error);
  }
}

void connectApp();

function render(recommendations: TransferRecommendation[]) {
  metrics.replaceChildren();
  recommendationsElement.replaceChildren();
  decisionElement.hidden = recommendations.length === 0 || !approvalId;
  modeElement.textContent = approvalId
    ? "Authenticated action mode: select a recommendation for explicit review."
    : "Read-only validation mode: recommendations can be inspected, but no approval or database write is available.";
  selectionElement.textContent = "Select a recommendation to review.";
  statusElement.textContent = approvalId
    ? "No database write occurs until you select a recommendation and approve it."
    : "This host did not provide an approval handle; the dashboard is read-only.";
  approveElement.disabled = true;
  if (recommendations.length === 0) {
    const empty = document.createElement("p");
    empty.textContent =
      "No inventory positions matched the governed stockout-risk threshold.";
    recommendationsElement.append(empty);
    return;
  }
  const units = recommendations.reduce(
    (sum, recommendation) =>
      sum + recommendation.recommendedTransferQuantity,
    0
  );
  const critical = recommendations.filter(
    recommendation => recommendation.riskLevel === "CRITICAL"
  ).length;
  [
    ["Recommendations", String(recommendations.length)],
    ["Critical", String(critical)],
    ["Units to rebalance", String(units)]
  ].forEach(
    ([label, value]) => metrics.append(metric(label, value))
  );
  recommendations.forEach(
    recommendation =>
      recommendationsElement.append(
        recommendationCard(recommendation)
      )
  );
}

function metric(label: string, value: string) {
  const box = document.createElement("div");
  box.className = "metric";
  const strong = document.createElement("strong");
  strong.textContent = value;
  const span = document.createElement("span");
  span.textContent = label;
  box.append(strong, span);
  return box;
}

function recommendationCard(
  recommendation: TransferRecommendation
) {
  const box = document.createElement("article");
  box.className = "recommendation";
  const name = document.createElement("strong");
  name.textContent =
    `${recommendation.sku} · ${recommendation.productName}`;
  const route = document.createElement("div");
  route.textContent =
    `${recommendation.sourceLocationCode} → `
      + `${recommendation.targetLocationCode} · `
      + `${recommendation.recommendedTransferQuantity} units`;
  const score = document.createElement("div");
  score.textContent =
    `${recommendation.riskLevel} · `
      + `${recommendation.stockoutRiskScore}`;
  const bar = document.createElement("div");
  bar.className = "bar";
  const fill = document.createElement("span");
  fill.style.width =
    `${Math.min(100, Math.max(0, recommendation.stockoutRiskScore))}%`;
  bar.append(fill);
  const summary = document.createElement("p");
  summary.textContent = recommendation.rationale;
  const button = document.createElement("button");
  button.textContent = approvalId
    ? "Review this transfer"
    : "Read-only preview";
  button.disabled = !approvalId;
  if (!approvalId) {
    button.title =
      "Approval requires an authenticated deployment with write actions enabled.";
  }
  button.addEventListener("click", () => {
    selectedRecommendation = recommendation;
    approveElement.disabled = false;
    selectionElement.textContent =
      `Selected ${recommendation.sku}: `
      + `${recommendation.sourceLocationCode} to `
      + `${recommendation.targetLocationCode}, `
      + `${recommendation.recommendedTransferQuantity} units.`;
    statusElement.textContent =
      "Review the exact route, quantity, rationale, and notes before approval.";
    for (const card of recommendationsElement.children) {
      card.classList.remove("selected");
    }
    box.classList.add("selected");
    void app.updateModelContext({
      content: [{
        type: "text",
        text:
          `Selected inventory transfer ${recommendation.recommendationId}: `
            + `move ${recommendation.recommendedTransferQuantity} units of `
            + `${recommendation.sku} from `
            + `${recommendation.sourceLocationCode} to `
            + `${recommendation.targetLocationCode}.`
      }]
    });
  });
  box.append(name, route, score, bar, summary, button);
  return box;
}

approveElement.addEventListener("click", async () => {
  if (!approvalId || !selectedRecommendation) return;
  const notes = notesElement.value.trim();
  if (notes.length < 10) {
    statusElement.textContent =
      "Approval notes must contain at least 10 characters.";
    return;
  }
  setBusy(true, "Executing the governed transfer...");
  try {
    const result = await app.callServerTool({
      name: "approve-inventory-transfer",
      arguments: {
        approvalId,
        recommendationId: selectedRecommendation.recommendationId,
        approvalNotes: notes
      }
    });
    const payload = result.structuredContent as {
      transferId?: number;
      recommendationId?: string;
      transferQuantity?: number;
      status?: string;
    } | undefined;
    if (payload?.status !== "APPROVED") {
      throw new Error("The approval tool returned an unexpected result.");
    }
    statusElement.textContent =
      `Transfer ${payload.transferId} approved and audited for `
      + `${payload.transferQuantity} units.`;
    approvalId = undefined;
    approveElement.disabled = true;
    cancelElement.disabled = true;
    void app.updateModelContext({
      content: [{
        type: "text",
        text:
          `The user explicitly approved audited inventory transfer `
          + `${payload.transferId} for recommendation `
          + `${payload.recommendationId}.`
      }]
    });
  } catch (error) {
    setBusy(
      false,
      error instanceof Error ? error.message : "Transfer approval failed."
    );
  }
});

cancelElement.addEventListener("click", async () => {
  if (!approvalId) return;
  setBusy(true, "Cancelling this review...");
  try {
    await app.callServerTool({
      name: "reject-inventory-transfer-review",
      arguments: { approvalId }
    });
    statusElement.textContent =
      "Review cancelled. No inventory-transfer write was executed.";
    approvalId = undefined;
    selectedRecommendation = undefined;
    approveElement.disabled = true;
    cancelElement.disabled = true;
    for (const card of recommendationsElement.children) {
      card.classList.remove("selected");
    }
    void app.updateModelContext({
      content: [{
        type: "text",
        text:
          "The user cancelled the inventory-transfer review. "
          + "No database write was executed."
      }]
    });
  } catch (error) {
    setBusy(
      false,
      error instanceof Error ? error.message : "Review cancellation failed."
    );
  }
});

function setBusy(busy: boolean, message: string) {
  statusElement.textContent = message;
  approveElement.disabled =
    busy || !approvalId || !selectedRecommendation;
  cancelElement.disabled = busy || !approvalId;
  for (const button of recommendationsElement.querySelectorAll("button")) {
    button.disabled = busy;
  }
}
