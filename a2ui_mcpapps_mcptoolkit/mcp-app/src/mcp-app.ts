import { App } from "@modelcontextprotocol/ext-apps";

declare const maplibregl: {
  Map: new (options: Record<string, unknown>) => MapLibreMap;
};

type MapLibreMap = {
  addSource: (id: string, source: Record<string, unknown>) => void;
  addLayer: (layer: Record<string, unknown>) => void;
  fitBounds: (bounds: [[number, number], [number, number]], options?: Record<string, unknown>) => void;
  project: (coordinate: [number, number]) => { x: number; y: number };
  resize: () => void;
  on: (...args: unknown[]) => void;
};
type MapLibreEvent = { features?: Array<{ properties?: Record<string, unknown> }> };

type SpatialFeature = {
  geometry?: {
    type?: string;
    coordinates?: unknown;
  };
  properties?: Record<string, unknown>;
};

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
    sourceElement.textContent =
      `Oracle Database spatial evidence received from ${rawPayload.source ?? "the connected MCP tool"}.`;
    modeElement.textContent =
      "Connected to the MCP host; rendering Oracle spatial evidence.";
    statusElement.textContent =
      "Connected to the MCP host; rendering Oracle spatial evidence.";
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
      sources: {
        // OpenStreetMap raster tiles are intentionally explicit here:
        // Gemini Enterprise's MCP App sandbox only permits network calls
        // declared by the resource CSP in server.ts.
        openstreetmap: {
          type: "raster",
          tiles: ["https://tile.openstreetmap.org/{z}/{x}/{y}.png"],
          tileSize: 256,
          attribution: "© OpenStreetMap contributors"
        }
      },
      layers: [
        { id: "background", type: "background", paint: { "background-color": "#eef3f6" } },
        { id: "openstreetmap-tiles", type: "raster", source: "openstreetmap" }
      ]
    },
    center: [-96, 38],
    zoom: 3
  });
  // MCP App hosts can reveal a previously hidden iframe after the map is
  // constructed. Resize once immediately and again after layout settles.
  map.resize();
  window.setTimeout(() => map.resize(), 0);
  map.on("error", (event: { error?: { message?: string } }) => {
    console.error("MapLibre spatial map error", event.error);
    if (event.error?.message) {
      spatialMap.dataset.error = event.error.message;
      modeElement.textContent = `MapLibre error: ${event.error.message}`;
    }
  });
  map.on("load", () => {
    const features = payload.geojson.features as SpatialFeature[];
    const pointFeatures = features.filter(
      feature => feature.geometry?.type === "Point"
    );
    const sourceFeatures = pointFeatures.filter(
      feature => String(feature.properties?.recommendedRole ?? "")
        .toUpperCase().includes("SOURCE")
    );
    const destinationFeatures = pointFeatures.filter(
      feature => feature.properties?.recommendedRole !== "SOURCE"
    );
    const returnedRouteFeatures = features.filter(
      feature => feature.geometry?.type === "LineString"
    );
    const routeFeatures = returnedRouteFeatures.length > 0
      ? returnedRouteFeatures
      : buildReliefRoute(pointFeatures);
    map.addSource("inventory-spatial-source", {
      type: "geojson",
      data: { type: "FeatureCollection", features: sourceFeatures }
    });
    map.addSource("inventory-spatial-destination", {
      type: "geojson",
      data: { type: "FeatureCollection", features: destinationFeatures }
    });
    map.addSource("inventory-spatial-route", {
      type: "geojson",
      data: { type: "FeatureCollection", features: routeFeatures }
    });
    map.addLayer({
      id: "relief-route",
      type: "line",
      source: "inventory-spatial-route",
      paint: { "line-color": "#1769aa", "line-width": 4, "line-dasharray": [2, 1] }
    });
    map.addLayer({
      id: "source-hotspot",
      type: "circle",
      source: "inventory-spatial-source",
      paint: {
        "circle-color": "#2f7d32",
        "circle-radius": 14,
        "circle-stroke-color": "#ffffff",
        "circle-stroke-width": 2,
        "circle-opacity": 0.9
      }
    });
    map.addLayer({
      id: "destination-hotspot",
      type: "circle",
      source: "inventory-spatial-destination",
      paint: {
        "circle-color": "#c74634",
        "circle-radius": 14,
        "circle-stroke-color": "#ffffff",
        "circle-stroke-width": 2,
        "circle-opacity": 0.9
      }
    });
    const coordinates = collectCoordinates(features);
    if (coordinates.length > 0) {
      const longitudes = coordinates.map(([longitude]) => longitude);
      const latitudes = coordinates.map(([, latitude]) => latitude);
      map.fitBounds(
        [[Math.min(...longitudes), Math.min(...latitudes)], [Math.max(...longitudes), Math.max(...latitudes)]],
        { padding: 48, maxZoom: 7, duration: 0 }
      );
      installSpatialOverlay(map, pointFeatures, routeFeatures);
      modeElement.textContent =
        `Map rendered ${pointFeatures.length} Oracle hotspot points and ${routeFeatures.length} route. `
        + "Click a point for warehouse details.";
    } else {
      statusElement.textContent =
        "The Oracle spatial tool returned no drawable coordinates.";
    }
    const showWarehouseDetails = (event: MapLibreEvent) => {
      const properties = event.features?.[0]?.properties ?? {};
      spatialSource.textContent =
        `${properties.locationCode ?? "Warehouse"} · ${properties.locationName ?? ""} · `
        + `${properties.recommendedRole ?? ""} · risk ${properties.stockoutRiskScore ?? "n/a"}`;
    };
    map.on("click", "source-hotspot", showWarehouseDetails);
    map.on("click", "destination-hotspot", showWarehouseDetails);
  });
}

function installSpatialOverlay(
  map: MapLibreMap,
  pointFeatures: SpatialFeature[],
  routeFeatures: SpatialFeature[]
) {
  // Some embedded enterprise browser hosts expose MapLibre's camera and DOM
  // but suppress WebGL feature painting. Keep MapLibre as the map/camera and
  // mirror the returned GeoJSON in a lightweight SVG overlay for that case.
  const namespace = "http://www.w3.org/2000/svg";
  const overlay = document.createElementNS(namespace, "svg");
  overlay.setAttribute("aria-hidden", "true");
  overlay.style.position = "absolute";
  overlay.style.inset = "0";
  overlay.style.width = "100%";
  overlay.style.height = "100%";
  overlay.style.pointerEvents = "none";
  overlay.style.zIndex = "2";
  spatialMap.append(overlay);
  const toViewport = ([longitude, latitude]: [number, number]) =>
    map.project([longitude, latitude]);

  const redraw = () => {
    const width = spatialMap.clientWidth;
    const height = spatialMap.clientHeight;
    overlay.setAttribute("viewBox", `0 0 ${width} ${height}`);
    overlay.replaceChildren();
    for (const feature of routeFeatures) {
      const coordinates = feature.geometry?.coordinates;
      if (!Array.isArray(coordinates)) continue;
      const points = coordinates
        .filter((coordinate): coordinate is [number, number] =>
          Array.isArray(coordinate)
          && typeof coordinate[0] === "number"
          && typeof coordinate[1] === "number"
        )
        .map(([longitude, latitude]) => {
          const point = toViewport([longitude, latitude]);
          return `${point.x},${point.y}`;
        })
        .join(" ");
      const line = document.createElementNS(namespace, "polyline");
      line.setAttribute("points", points);
      line.setAttribute("fill", "none");
      line.setAttribute("stroke", "#1769aa");
      line.setAttribute("stroke-width", "5");
      line.setAttribute("stroke-linecap", "round");
      overlay.append(line);
    }
    for (const feature of pointFeatures) {
      const coordinate = feature.geometry?.coordinates;
      if (!Array.isArray(coordinate) || typeof coordinate[0] !== "number" || typeof coordinate[1] !== "number") continue;
      const point = toViewport([coordinate[0], coordinate[1]]);
      const circle = document.createElementNS(namespace, "circle");
      circle.setAttribute("cx", String(point.x));
      circle.setAttribute("cy", String(point.y));
      circle.setAttribute("r", "11");
      circle.setAttribute("fill", feature.properties?.recommendedRole === "SOURCE" ? "#2f7d32" : "#c74634");
      circle.setAttribute("stroke", "#ffffff");
      circle.setAttribute("stroke-width", "3");
      circle.style.pointerEvents = "all";
      circle.style.cursor = "pointer";
      circle.addEventListener("click", (event) => {
        event.stopPropagation();
        const properties = feature.properties ?? {};
        spatialSource.textContent =
          `${properties.locationCode ?? "Warehouse"} · ${properties.locationName ?? ""} · `
          + `${properties.recommendedRole ?? ""} · risk ${properties.stockoutRiskScore ?? "n/a"}`;
      });
      overlay.append(circle);
    }
  };
  map.on("move", redraw);
  map.on("resize", redraw);
  redraw();
}

function buildReliefRoute(pointFeatures: SpatialFeature[]): SpatialFeature[] {
  const source = pointFeatures.find(feature =>
    String(feature.properties?.recommendedRole ?? "").toUpperCase().includes("SOURCE")
  );
  const destination = pointFeatures.find(feature =>
    !String(feature.properties?.recommendedRole ?? "").toUpperCase().includes("SOURCE")
  );
  const sourceCoordinates = source?.geometry?.coordinates;
  const destinationCoordinates = destination?.geometry?.coordinates;
  if (
    !Array.isArray(sourceCoordinates)
    || !Array.isArray(destinationCoordinates)
    || typeof sourceCoordinates[0] !== "number"
    || typeof sourceCoordinates[1] !== "number"
    || typeof destinationCoordinates[0] !== "number"
    || typeof destinationCoordinates[1] !== "number"
  ) {
    return [];
  }
  return [{
    geometry: {
      type: "LineString",
      coordinates: [sourceCoordinates, destinationCoordinates]
    },
    properties: {
      kind: "relief-route",
      sourceLocationCode: source?.properties?.locationCode,
      targetLocationCode: destination?.properties?.locationCode
    }
  }];
}

function collectCoordinates(features: SpatialFeature[]): Array<[number, number]> {
  const coordinates: Array<[number, number]> = [];
  for (const feature of features) {
    const geometry = feature.geometry;
    if (!geometry?.coordinates) continue;
    collectCoordinatePairs(geometry.coordinates, coordinates);
  }
  return coordinates;
}

function collectCoordinatePairs(value: unknown, output: Array<[number, number]>): void {
  if (
    Array.isArray(value) &&
    value.length >= 2 &&
    typeof value[0] === "number" &&
    typeof value[1] === "number"
  ) {
    output.push([value[0], value[1]]);
    return;
  }
  if (Array.isArray(value)) {
    for (const child of value) collectCoordinatePairs(child, output);
  }
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
