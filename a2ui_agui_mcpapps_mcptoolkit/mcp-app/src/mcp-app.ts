import { App } from "@modelcontextprotocol/ext-apps";

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

app.ontoolresult = (result) => {
  const payload = result.structuredContent as {
    recommendations?: TransferRecommendation[];
    source?: string;
    minimumStockoutRisk?: number;
  } | undefined;
  sourceElement.textContent =
    payload?.source === "oracle-db-mcp-java-toolkit"
      ? "Live governed results from the Oracle Database MCP Java Toolkit"
        + ` · minimum stockout risk ${payload.minimumStockoutRisk}`
      : "Waiting for governed Toolkit results.";
  render(payload?.recommendations ?? []);
};
app.connect();

function render(recommendations: TransferRecommendation[]) {
  metrics.replaceChildren();
  recommendationsElement.replaceChildren();
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
  button.textContent = "Send recommendation to conversation";
  button.addEventListener("click", () =>
    app.updateModelContext({
      content: [{
        type: "text",
        text:
          `Selected inventory transfer ${recommendation.recommendationId}: `
            + `move ${recommendation.recommendedTransferQuantity} units of `
            + `${recommendation.sku} from `
            + `${recommendation.sourceLocationCode} to `
            + `${recommendation.targetLocationCode}.`
      }]
    })
  );
  box.append(name, route, score, bar, summary, button);
  return box;
}
