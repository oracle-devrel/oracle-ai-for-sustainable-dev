import { readFile } from "node:fs/promises";
import path from "node:path";
import cors from "cors";
import express from "express";
import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StreamableHTTPServerTransport } from "@modelcontextprotocol/sdk/server/streamableHttp.js";
import {
  registerAppResource,
  registerAppTool,
  RESOURCE_MIME_TYPE
} from "@modelcontextprotocol/ext-apps/server";
import { z } from "zod";

const resourceUri = "ui://oracle-supply-chain/inventory-exchange-v1";
const agentServiceUrl =
  process.env.AGENT_SERVICE_URL ?? "http://127.0.0.1:8080";
const agentServiceTimeoutMs =
  Number(process.env.AGENT_SERVICE_TIMEOUT_MS ?? "30000");

const TransferRecommendationSchema = z.object({
  recommendationId: z.string(),
  productId: z.number().int().positive(),
  sku: z.string(),
  productName: z.string(),
  categoryName: z.string(),
  sourceLocationId: z.number().int().positive(),
  sourceLocationCode: z.string(),
  sourceLocationName: z.string(),
  targetLocationId: z.number().int().positive(),
  targetLocationCode: z.string(),
  targetLocationName: z.string(),
  sourceAvailableQuantity: z.number(),
  targetAvailableQuantity: z.number(),
  forecast7dQuantity: z.number(),
  safetyStockQuantity: z.number(),
  shortageQuantity: z.number().positive(),
  recommendedTransferQuantity: z.number().positive(),
  transitDays: z.number().int().positive(),
  unitTransferCost: z.number().nonnegative(),
  stockoutRiskScore: z.number().min(0).max(100),
  riskLevel: z.string(),
  rationale: z.string()
});
const GovernedRecommendationsSchema = z.object({
  source: z.literal("oracle-db-mcp-java-toolkit"),
  recommendations: z.array(TransferRecommendationSchema)
});

async function loadGovernedRecommendations(
  minimumStockoutRisk: number,
  maximumRows: number
) {
  const endpoint = new URL("/api/recommendations", agentServiceUrl);
  endpoint.searchParams.set(
    "minimumStockoutRisk",
    String(minimumStockoutRisk)
  );
  endpoint.searchParams.set("maximumRows", String(maximumRows));
  const response = await fetch(endpoint, {
    signal: AbortSignal.timeout(agentServiceTimeoutMs)
  });
  if (!response.ok) {
    throw new Error(
      `Governed recommendation request failed with HTTP ${response.status}`
    );
  }
  return GovernedRecommendationsSchema.parse(await response.json())
    .recommendations;
}

const server = new McpServer({
  name: "Oracle Supply-Chain Inventory Exchange MCP App",
  version: "0.1.0"
});

registerAppTool(server, "show-inventory-transfer-dashboard", {
  title: "Show inventory transfer dashboard",
  description:
    "Shows Oracle Database MCP Java Toolkit-governed stockout exposure and inventory-transfer recommendations.",
  inputSchema: {
    minimumStockoutRisk: z.number()
      .min(0)
      .max(100)
      .default(70)
      .describe("Minimum governed stockout-risk score"),
    maximumRows: z.number()
      .int()
      .min(1)
      .max(50)
      .default(10)
      .describe("Maximum governed transfer recommendations to display")
  },
  _meta: {
    ui: {
      resourceUri,
      visibility: ["model", "app"]
    }
  }
}, async ({ minimumStockoutRisk, maximumRows }) => {
  const recommendations = await loadGovernedRecommendations(
    minimumStockoutRisk,
    maximumRows
  );
  return {
    content: [{
      type: "text",
      text:
        "Oracle Database MCP Java Toolkit returned "
        + `${recommendations.length} governed inventory-transfer recommendations.`
    }],
    structuredContent: {
      recommendations,
      source: "oracle-db-mcp-java-toolkit",
      minimumStockoutRisk,
      maximumRows
    }
  };
});

registerAppResource(
  server,
  resourceUri,
  resourceUri,
  { mimeType: RESOURCE_MIME_TYPE },
  async () => ({
    contents: [{
      uri: resourceUri,
      mimeType: RESOURCE_MIME_TYPE,
      text: await readFile(
        path.join(import.meta.dirname, "dist", "mcp-app.html"),
        "utf8"
      )
    }]
  })
);

const app = express();
app.use(cors({ origin: false }));
app.use(express.json({ limit: "256kb" }));
app.get(
  "/health",
  (_request, response) =>
    response.json({ status: "UP", dataSource: agentServiceUrl })
);
app.post("/mcp", async (request, response) => {
  const transport = new StreamableHTTPServerTransport({
    sessionIdGenerator: undefined,
    enableJsonResponse: true
  });
  response.on("close", () => transport.close());
  await server.connect(transport);
  await transport.handleRequest(request, response, request.body);
});
app.listen(
  3001,
  "127.0.0.1",
  () => console.log(
    "Supply-chain MCP App server listening on "
      + "http://127.0.0.1:3001/mcp"
  )
);
