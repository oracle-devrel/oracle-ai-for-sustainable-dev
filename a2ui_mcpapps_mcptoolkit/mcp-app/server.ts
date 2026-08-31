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

const resourceUri = "ui://oracle-supply-chain/inventory-exchange-v2";
const agentServiceUrl =
  process.env.AGENT_SERVICE_URL ?? "http://127.0.0.1:8080";
const agentServiceTimeoutMs =
  Number(process.env.AGENT_SERVICE_TIMEOUT_MS ?? "30000");
const bindHost = process.env.MCP_BIND_HOST ?? "127.0.0.1";
const port = Number(process.env.PORT ?? "3001");
const writesEnabled = process.env.MCP_WRITES_ENABLED === "true";

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
const GovernedReviewSchema = GovernedRecommendationsSchema.extend({
  approvalId: z.string().uuid()
});
const TransferResultSchema = z.object({
  transferId: z.number().int().positive(),
  recommendationId: z.string(),
  transferQuantity: z.number().positive(),
  status: z.literal("APPROVED")
});
const RejectionResultSchema = z.object({
  status: z.literal("REJECTED")
});

async function agentFormRequest(
  pathName: string,
  values: Record<string, string | number>
) {
  const endpoint = new URL(pathName, agentServiceUrl);
  const body = new URLSearchParams();
  Object.entries(values).forEach(
    ([name, value]) => body.set(name, String(value))
  );
  const response = await fetch(endpoint, {
    method: "POST",
    headers: {
      "Content-Type": "application/x-www-form-urlencoded"
    },
    body,
    signal: AbortSignal.timeout(agentServiceTimeoutMs)
  });
  const payload: unknown = await response.json();
  if (!response.ok) {
    const message =
      typeof payload === "object"
        && payload !== null
        && "error" in payload
        && typeof payload.error === "string"
        ? payload.error
        : `Governed request failed with HTTP ${response.status}`;
    throw new Error(message);
  }
  return payload;
}

async function loadGovernedReview(
  minimumStockoutRisk: number,
  maximumRows: number
) {
  return GovernedReviewSchema.parse(
    await agentFormRequest("/api/reviews", {
      minimumStockoutRisk,
      maximumRows
    })
  );
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
  },
  annotations: {
    readOnlyHint: true,
    openWorldHint: false
  }
}, async ({ minimumStockoutRisk, maximumRows }) => {
  const review = await loadGovernedReview(
    minimumStockoutRisk,
    maximumRows
  );
  return {
    content: [{
      type: "text",
      text:
        "Oracle Database MCP Java Toolkit returned "
        + `${review.recommendations.length} governed inventory-transfer recommendations `
        + "for explicit user review."
    }],
    structuredContent: {
      recommendations: review.recommendations,
      source: "oracle-db-mcp-java-toolkit",
      minimumStockoutRisk,
      maximumRows
    },
    _meta: writesEnabled ? { approvalId: review.approvalId } : {}
  };
});

if (writesEnabled) {
  registerAppTool(server, "approve-inventory-transfer", {
    title: "Approve selected inventory transfer",
    description:
      "App-only action that executes one exact, previously reviewed Oracle-governed inventory transfer.",
    inputSchema: {
      approvalId: z.string().uuid(),
      recommendationId: z.string().min(1).max(100),
      approvalNotes: z.string().min(10).max(500)
    },
    _meta: {
      ui: {
        visibility: ["app"]
      }
    },
    annotations: {
      destructiveHint: true,
      idempotentHint: false,
      openWorldHint: false
    }
  }, async ({ approvalId, recommendationId, approvalNotes }) => {
    const result = TransferResultSchema.parse(
      await agentFormRequest("/api/approve", {
        approvalId,
        recommendationId,
        approvalNotes
      })
    );
    return {
      content: [{
        type: "text",
        text:
          `Approved governed inventory transfer ${result.transferId} `
          + `for recommendation ${result.recommendationId}.`
      }],
      structuredContent: result
    };
  });

  registerAppTool(server, "reject-inventory-transfer-review", {
    title: "Cancel inventory transfer review",
    description:
      "App-only action that invalidates the current approval handle without writing an inventory transfer.",
    inputSchema: {
      approvalId: z.string().uuid()
    },
    _meta: {
      ui: {
        visibility: ["app"]
      }
    },
    annotations: {
      destructiveHint: false,
      idempotentHint: false,
      openWorldHint: false
    }
  }, async ({ approvalId }) => {
    const result = RejectionResultSchema.parse(
      await agentFormRequest("/api/reject", { approvalId })
    );
    return {
      content: [{
        type: "text",
        text: "Cancelled the inventory-transfer review; no database write ran."
      }],
      structuredContent: result
    };
  });
}

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
      ),
      _meta: {
        ui: {
          prefersBorder: true,
          csp: {
            connectDomains: [],
            resourceDomains: ["https://www.oracle.com"]
          }
        }
      }
    }]
  })
);

const app = express();
app.use(cors({ origin: false }));
app.use(express.json({ limit: "256kb" }));
app.get(
  "/health",
  (_request, response) =>
    response.json({
      status: "UP",
      dataSource: "oracle-db-mcp-java-toolkit",
      writeActionsEnabled: writesEnabled
    })
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
  port,
  bindHost,
  () => console.log(
    "Supply-chain MCP App server listening on "
      + `http://${bindHost}:${port}/mcp`
  )
);
