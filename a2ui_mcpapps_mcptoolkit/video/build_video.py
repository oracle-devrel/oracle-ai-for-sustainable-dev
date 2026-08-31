#!/usr/bin/env python3
from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
from textwrap import wrap

import imageio.v2 as imageio
import numpy as np
from PIL import Image, ImageDraw, ImageFont, ImageOps

WIDTH = 1920
HEIGHT = 1080
FPS = 24
SECONDS_PER_SCENE = 7
ROOT = Path(__file__).resolve().parent
IMAGE_ROOT = ROOT.parent / "images"
OUTPUT = ROOT / "mcp-app-developer-walkthrough.mp4"
POSTER = ROOT / "mcp-app-developer-walkthrough-poster.png"
SRT = ROOT / "mcp-app-developer-walkthrough.srt"
VTT = ROOT / "mcp-app-developer-walkthrough.vtt"


@dataclass(frozen=True)
class Scene:
    eyebrow: str
    title: str
    caption: str
    detail: str
    image: str | None = None
    file: str | None = None
    code: str | None = None
    cursor_from: tuple[int, int] = (600, 650)
    cursor_to: tuple[int, int] = (1320, 650)


SCENES = [
    Scene("STEP 01", "Start with the\npublished app.", "Open the Gemini Enterprise Inventory System and confirm the Oracle Supply Chain MCP App connector is active.", "Developer view: host, connector, MCP server, agent service, Toolkit, and database.", "gemini-enterprise-mcp-app-dashboard.png", cursor_from=(500, 650), cursor_to=(1120, 650)),
    Scene("STEP 02", "Connect the\ncustom MCP server.", "The connector registration points Gemini Enterprise at the MCP server endpoint running in Cloud Run.", "The connector configuration lives in Gemini Enterprise, not in the repo source.", "gemini-enterprise-mcp-app-enable-action.png", cursor_from=(720, 300), cursor_to=(980, 300)),
    Scene("STEP 03", "Expose one\nMCP App tool.", "server.ts registers show-inventory-transfer-dashboard as the model-visible tool.", "a2ui_mcpapps_mcptoolkit/mcp-app/server.ts", file="mcp-app/server.ts", code='''registerAppTool(server,\n  "show-inventory-transfer-dashboard",\n  {\n    title: "Show inventory transfer dashboard",\n    description: "Shows governed stockout exposure.",\n    annotations: { readOnlyHint: true }\n  },\n  async (args) => loadGovernedReview(...)\n);'''),
    Scene("STEP 04", "Advertise the\nUI resource.", "The tool definition includes _meta.ui.resourceUri so the host knows which MCP App iframe to load.", "a2ui_mcpapps_mcptoolkit/mcp-app/server.ts", file="mcp-app/server.ts", code='''const resourceUri =\n  "ui://oracle-supply-chain/inventory-exchange-v2";\n\n_meta: {\n  ui: {\n    resourceUri,\n    visibility: ["model", "app"]\n  }\n}'''),
    Scene("STEP 05", "Register the\napp resource.", "registerAppResource serves the compiled HTML for the ui:// resource requested by the host.", "a2ui_mcpapps_mcptoolkit/mcp-app/server.ts", file="mcp-app/server.ts", code='''registerAppResource(\n  server,\n  resourceUri,\n  resourceUri,\n  { mimeType: RESOURCE_MIME_TYPE },\n  async () => ({ contents: [{ text: html }] })\n);'''),
    Scene("STEP 06", "Set iframe\nsecurity policy.", "The MCP App resource declares CSP domains for host-enforced resource and network boundaries.", "a2ui_mcpapps_mcptoolkit/mcp-app/server.ts", file="mcp-app/server.ts", code='''_meta: {\n  ui: {\n    prefersBorder: true,\n    csp: {\n      connectDomains: [],\n      resourceDomains: ["https://www.oracle.com"]\n    }\n  }\n}'''),
    Scene("STEP 07", "Build the\niframe client.", "mcp-app.ts connects to the MCP Apps host bridge and waits for the tool result.", "a2ui_mcpapps_mcptoolkit/mcp-app/src/mcp-app.ts", file="mcp-app/src/mcp-app.ts", code='''const app = new App({\n  name: "Oracle Supply-Chain Inventory Exchange",\n  version: "0.1.0"\n});\n\napp.ontoolresult = (result) => {\n  render(result.structuredContent.recommendations);\n};'''),
    Scene("STEP 08", "Authorize the\nconnector.", "The user authorizes the MCP connector before Gemini Enterprise can call the remote MCP server.", "This is host-side authorization, separate from Oracle AI Database credentials.", "gemini-enterprise-mcp-app-authorize.png", cursor_from=(1010, 445), cursor_to=(1190, 445)),
    Scene("STEP 09", "Enable the\nconnector tool.", "After discovery, Gemini Enterprise shows the MCP connector in the prompt control menu.", "The tool list lives in Gemini Enterprise after it reads the remote MCP server capabilities.", "gemini-enterprise-mcp-app-authorize.png", cursor_from=(410, 620), cursor_to=(750, 760)),
    Scene("STEP 10", "Ask for the\ndashboard.", "The prompt mentions an inventory transfer dashboard, stockout risk, and a row limit, so the host selects the dashboard tool.", "Prompt: show the inventory transfer dashboard with risk 70, limited to 3.", "gemini-enterprise-mcp-app-dashboard.png", cursor_from=(1080, 122), cursor_to=(1380, 122)),
    Scene("STEP 11", "Host calls\ntools/call.", "Gemini Enterprise calls show-inventory-transfer-dashboard with parsed arguments.", "The MCP server receives minimumStockoutRisk and maximumRows.", file="mcp-app/server.ts", code='''async ({ minimumStockoutRisk, maximumRows }) => {\n  const review = await loadGovernedReview(\n    minimumStockoutRisk,\n    maximumRows\n  );\n  return { structuredContent: ... };\n}'''),
    Scene("STEP 12", "Call the\nagent service.", "The MCP server posts form data to the Spring Boot agent service for governed review data.", "a2ui_mcpapps_mcptoolkit/mcp-app/server.ts", file="mcp-app/server.ts", code='''await agentFormRequest("/api/reviews", {\n  minimumStockoutRisk,\n  maximumRows\n});'''),
    Scene("STEP 13", "Receive\n/api/reviews.", "AgentController accepts the request and delegates to the runtime.", "a2ui_mcpapps_mcptoolkit/agent-service/src/main/java/.../AgentController.java", file="AgentController.java", code='''@PostMapping("/api/reviews")\nMap<String, Object> createReview(...) {\n  return runtime.createReview(\n    minimumStockoutRisk,\n    maximumRows,\n    accessProfile\n  );\n}'''),
    Scene("STEP 14", "Create the\nreview handle.", "AgentRuntime creates the approval context and asks the supply-chain gateway for recommendations.", "a2ui_mcpapps_mcptoolkit/agent-service/src/main/java/.../AgentRuntime.java", file="AgentRuntime.java", code='''List<TransferRecommendation> rows =\n  repository.findRecommendations(\n    minimumStockoutRisk,\n    maximumRows,\n    accessProfile\n  );\n\nString approvalId = approvals.createReview(rows);'''),
    Scene("STEP 15", "Use the\nToolkit gateway.", "McpToolkitSupplyChainGateway calls the standalone Oracle Database MCP Java Toolkit over HTTP.", "a2ui_mcpapps_mcptoolkit/agent-service/src/main/java/.../McpToolkitSupplyChainGateway.java", file="McpToolkitSupplyChainGateway.java", code='''mcpClient.callTool(\n  "find-stockout-transfer-recommendations",\n  Map.of(\n    "minimumStockoutRisk", minimumStockoutRisk,\n    "maximumRows", maximumRows\n  )\n);'''),
    Scene("STEP 16", "Keep Toolkit\nseparate.", "The Oracle Database MCP Java Toolkit runs as its own process and owns the database tool contract.", "a2ui_mcpapps_mcptoolkit/oracle-db-mcp-toolkit/run.sh", file="oracle-db-mcp-toolkit/run.sh", code='''java -jar oracle-db-mcp-toolkit.jar \\\n  --config config/tools.yaml \\\n  --transport streamable-http \\\n  --port 3000'''),
    Scene("STEP 17", "Map tool\nto SQL.", "tools.yaml binds the tool name to a bounded SQL statement rather than arbitrary model-generated SQL.", "a2ui_mcpapps_mcptoolkit/oracle-db-mcp-toolkit/config/tools.yaml", file="oracle-db-mcp-toolkit/config/tools.yaml", code='''name: find-stockout-transfer-recommendations\nkind: sql\nsql: |\n  SELECT * FROM stockout_transfer_recommendation_v\n   WHERE stockout_risk_score >= :minimumStockoutRisk\n   FETCH FIRST :maximumRows ROWS ONLY'''),
    Scene("STEP 18", "Query Oracle\nAI Database.", "The Toolkit uses JDBC/UCP credentials server-side and returns governed rows to the agent service.", "The host and iframe never receive wallet files or database passwords.", file="database/04-recommendation-view.sql", code='''CREATE OR REPLACE VIEW\n  stockout_transfer_recommendation_v AS\nSELECT sku, product_name, source_location_code,\n       target_location_code, stockout_risk_score,\n       recommended_transfer_quantity, rationale\nFROM ...'''),
    Scene("STEP 19", "Return governed\nstructured data.", "The database rows return through Toolkit, gateway, agent service, and MCP server as structuredContent.", "The return path follows the request path back to the host.", "mcp-app-chatgpt-sequence.svg", cursor_from=(1480, 705), cursor_to=(720, 705)),
    Scene("STEP 20", "Load the\nui resource.", "The host uses the tool's ui:// association and reads the MCP App HTML resource.", "The resource is learned during tool discovery, then loaded when the tool result arrives.", "mcp-app-chatgpt-sequence.svg", cursor_from=(715, 390), cursor_to=(1010, 390)),
    Scene("STEP 21", "Render the\nsandboxed iframe.", "Gemini Enterprise renders the MCP App in a host-controlled sandbox and passes the tool result through the bridge.", "The iframe renders developer-built HTML and JavaScript under host policy.", "gemini-enterprise-mcp-app-dashboard.png", cursor_from=(930, 610), cursor_to=(1040, 780)),
    Scene("STEP 22", "Populate\nmetrics.", "mcp-app.ts computes recommendation count, critical count, and units to rebalance from structuredContent.", "a2ui_mcpapps_mcptoolkit/mcp-app/src/mcp-app.ts", file="mcp-app/src/mcp-app.ts", code='''const units = recommendations.reduce(\n  (sum, row) => sum + row.recommendedTransferQuantity,\n  0\n);\n\nmetrics.append(metric("Units to rebalance", String(units)));'''),
    Scene("STEP 23", "Render\ncards.", "The MCP App turns each governed recommendation into a review card with route, risk, rationale, and action state.", "a2ui_mcpapps_mcptoolkit/mcp-app/src/mcp-app.ts", file="mcp-app/src/mcp-app.ts", code='''name.textContent = `${recommendation.sku} · ...`;\nroute.textContent =\n  `${source} -> ${target} · ${units} units`;\nscore.textContent =\n  `${riskLevel} · ${stockoutRiskScore}`;'''),
    Scene("STEP 24", "Explain\nread-only mode.", "Without an approval handle, the Gemini Enterprise MCP App shows read-only preview and does not expose write buttons.", "MCP_WRITES_ENABLED controls whether approval tools are registered.", "gemini-enterprise-mcp-app-dashboard.png", cursor_from=(980, 705), cursor_to=(1010, 865)),
    Scene("STEP 25", "Enable writes\nwhen appropriate.", "If writes are enabled, the MCP server also registers app-only approval and rejection tools.", "a2ui_mcpapps_mcptoolkit/mcp-app/server.ts", file="mcp-app/server.ts", code='''if (writesEnabled) {\n  registerAppTool(server,\n    "approve-inventory-transfer",\n    { _meta: { ui: { visibility: ["app"] } } },\n    async (args) => approveTransfer(...)\n  );\n}'''),
    Scene("STEP 26", "Call app-only\napproval.", "The iframe uses app.callServerTool so ChatGPT, Claude, or Gemini makes the MCP call on its behalf.", "The iframe does not directly call the database or Toolkit.", file="mcp-app/src/mcp-app.ts", code='''await app.callServerTool({\n  name: "approve-inventory-transfer",\n  arguments: {\n    approvalId,\n    recommendationId,\n    approvalNotes\n  }\n});'''),
    Scene("STEP 27", "Validate exact\napproval.", "ApprovalService verifies the approval ID, recommendation ID, notes, and expiration before any write.", "a2ui_mcpapps_mcptoolkit/agent-service/src/main/java/.../ApprovalService.java", file="ApprovalService.java", code='''TransferRecommendation recommendation =\n  approvals.requireRecommendation(\n    approvalId,\n    recommendationId\n  );\n\nrepository.approveTransfer(recommendation, notes);'''),
    Scene("STEP 28", "Execute one\ngoverned write.", "The Toolkit write tool executes only the approved transfer and Oracle AI Database records the audited result.", "a2ui_mcpapps_mcptoolkit/oracle-db-mcp-toolkit/config/tools.yaml", file="oracle-db-mcp-toolkit/config/tools.yaml", code='''name: approve-inventory-transfer\nkind: sql\nsql: |\n  BEGIN\n    approve_inventory_transfer(\n      :approvalId, :recommendationId, :approvalNotes);\n  END;'''),
    Scene("STEP 29", "End with\nthe boundary.", "The host presents the experience, the MCP App renders the dashboard, and Oracle AI Database retains transaction authority.", "Same workflow, reusable UI path, governed data and execution.", "interactive-ai-architecture.svg", cursor_from=(540, 520), cursor_to=(1480, 610)),
]


def font(size: int, bold: bool = False, mono: bool = False) -> ImageFont.FreeTypeFont:
    names = (
        ["/System/Library/Fonts/Menlo.ttc"]
        if mono
        else [
            "/System/Library/Fonts/Supplemental/Arial Bold.ttf" if bold else "/System/Library/Fonts/Supplemental/Arial.ttf",
            "/System/Library/Fonts/Helvetica.ttc",
        ]
    )
    for name in names:
        try:
            return ImageFont.truetype(name, size=size)
        except OSError:
            pass
    return ImageFont.load_default(size=size)


F_EYEBROW = font(24, bold=True)
F_TITLE = font(60, bold=True)
F_DETAIL = font(26)
F_CAPTION = font(34, bold=True)
F_CODE = font(28, mono=True)
F_FILE = font(24, bold=True, mono=True)
F_SMALL = font(20, mono=True)


def draw_wrapped(draw: ImageDraw.ImageDraw, text: str, box: tuple[int, int, int, int], fnt, fill, spacing=8, align="left"):
    x, y, w, h = box
    lines: list[str] = []
    for paragraph in text.splitlines() or [""]:
        if not paragraph:
            lines.append("")
            continue
        approx = max(10, int(w / max(10, fnt.size * 0.55)))
        lines.extend(wrap(paragraph, approx, break_long_words=False))
    current = y
    for line in lines:
        if current > y + h - fnt.size:
            break
        if align == "center":
            tw = draw.textlength(line, font=fnt)
            tx = x + (w - tw) / 2
        else:
            tx = x
        draw.text((tx, current), line, font=fnt, fill=fill)
        current += fnt.size + spacing


def rounded(draw: ImageDraw.ImageDraw, xy, radius, fill, outline=None, width=1):
    draw.rounded_rectangle(xy, radius=radius, fill=fill, outline=outline, width=width)


def load_visual(name: str, size: tuple[int, int]) -> Image.Image:
    path = IMAGE_ROOT / name
    if path.suffix.lower() == ".svg":
        img = Image.new("RGB", size, (18, 28, 37))
        d = ImageDraw.Draw(img)
        rounded(d, (30, 30, size[0] - 30, size[1] - 30), 24, (33, 45, 58), (82, 110, 135), 2)
        draw_wrapped(d, "Diagram asset", (80, 110, size[0] - 160, 70), font(44, bold=True), (255, 255, 255))
        draw_wrapped(d, name, (80, 210, size[0] - 160, 80), font(30, mono=True), (180, 214, 225))
        draw_wrapped(d, "See the SVG in the images folder for the full sequence diagram.", (80, 330, size[0] - 160, 130), font(30), (220, 226, 232))
        return img
    img = Image.open(path).convert("RGB")
    return ImageOps.contain(img, size, method=Image.Resampling.LANCZOS)


def draw_code(draw: ImageDraw.ImageDraw, box, file: str, code: str):
    x, y, w, h = box
    rounded(draw, (x, y, x + w, y + h), 22, (12, 20, 27), (79, 96, 112), 2)
    rounded(draw, (x, y, x + w, y + 58), 22, (29, 42, 54))
    draw.text((x + 28, y + 16), file, font=F_FILE, fill=(194, 203, 214))
    numbered = "\n".join(f"{i + 1:>2}  {line}" for i, line in enumerate(code.splitlines()))
    draw_wrapped(draw, numbered, (x + 28, y + 86, w - 56, h - 106), F_CODE, (213, 226, 233), spacing=7)


def draw_cursor(draw: ImageDraw.ImageDraw, scene: Scene, progress: float):
    q = min(1, max(0, (progress - 0.16) / 0.64))
    x = scene.cursor_from[0] + (scene.cursor_to[0] - scene.cursor_from[0]) * q
    y = scene.cursor_from[1] + (scene.cursor_to[1] - scene.cursor_from[1]) * q
    points = [(x, y), (x + 13, y + 44), (x + 24, y + 26), (x + 43, y + 35), (x + 50, y + 23), (x + 31, y + 14)]
    draw.polygon(points, fill=(255, 255, 255), outline=(0, 0, 0))
    if 0.78 < progress < 0.94:
        radius = int(22 + 55 * ((progress - 0.78) / 0.16))
        draw.ellipse((x - radius, y - radius, x + radius, y + radius), outline=(25, 210, 235), width=6)


def render_scene(scene: Scene, index: int, progress: float = 0.5) -> Image.Image:
    img = Image.new("RGB", (WIDTH, HEIGHT), (9, 15, 21))
    draw = ImageDraw.Draw(img)
    for gx in range(0, WIDTH, 80):
        draw.line((gx, 0, gx, HEIGHT), fill=(13, 49, 59), width=1)
    for gy in range(0, HEIGHT, 80):
        draw.line((0, gy, WIDTH, gy), fill=(13, 49, 59), width=1)

    draw.text((70, 58), scene.eyebrow, font=F_EYEBROW, fill=(37, 208, 232))
    draw_wrapped(draw, scene.title, (70, 110, 580, 200), F_TITLE, (255, 255, 255), spacing=10)
    draw_wrapped(draw, scene.detail, (74, 325, 555, 120), F_DETAIL, (184, 193, 203), spacing=8)
    visual_box = (690, 120, 1160, 700)
    if scene.image:
        visual = load_visual(scene.image, (visual_box[2], visual_box[3]))
        vx = visual_box[0] + (visual_box[2] - visual.width) // 2
        vy = visual_box[1] + (visual_box[3] - visual.height) // 2
        rounded(draw, (visual_box[0], visual_box[1], visual_box[0] + visual_box[2], visual_box[1] + visual_box[3]), 22, (18, 28, 37), (79, 96, 112), 2)
        img.paste(visual, (vx, vy))
    elif scene.file and scene.code:
        draw_code(draw, visual_box, scene.file, scene.code)
    else:
        rounded(draw, (visual_box[0], visual_box[1], visual_box[0] + visual_box[2], visual_box[1] + visual_box[3]), 22, (18, 28, 37), (79, 96, 112), 2)
    rounded(draw, (135, 875, 1785, 1010), 22, (3, 6, 9), (40, 58, 72), 1)
    draw_wrapped(draw, scene.caption, (180, 903, 1560, 82), F_CAPTION, (255, 255, 255), spacing=8, align="center")
    draw.text((1690, 58), f"{index + 1:02d} / {len(SCENES):02d}", font=F_SMALL, fill=(184, 193, 203))
    draw_cursor(draw, scene, progress)
    return img


def timestamp(seconds: float, comma: bool) -> str:
    ms = round(seconds * 1000)
    sep = "," if comma else "."
    return f"{ms // 3600000:02d}:{(ms // 60000) % 60:02d}:{(ms // 1000) % 60:02d}{sep}{ms % 1000:03d}"


def write_captions():
    srt = []
    vtt = ["WEBVTT", ""]
    for i, scene in enumerate(SCENES):
        start = i * SECONDS_PER_SCENE + 0.2
        end = (i + 1) * SECONDS_PER_SCENE - 0.2
        srt.extend([str(i + 1), f"{timestamp(start, True)} --> {timestamp(end, True)}", scene.caption, ""])
        vtt.extend([f"{timestamp(start, False)} --> {timestamp(end, False)}", scene.caption, ""])
    SRT.write_text("\n".join(srt), encoding="utf-8")
    VTT.write_text("\n".join(vtt), encoding="utf-8")


def main():
    write_captions()
    poster_image = render_scene(SCENES[0], 0, 0.5)
    poster_image.save(POSTER)
    with imageio.get_writer(
        OUTPUT,
        fps=FPS,
        codec="libx264",
        quality=8,
        macro_block_size=None,
        ffmpeg_params=["-pix_fmt", "yuv420p"],
    ) as writer:
        for index, scene in enumerate(SCENES):
            for frame in range(FPS * SECONDS_PER_SCENE):
                progress = frame / (FPS * SECONDS_PER_SCENE)
                writer.append_data(np.asarray(render_scene(scene, index, progress)))
    print(f"Created {OUTPUT.name}, poster, SRT, and VTT ({len(SCENES) * SECONDS_PER_SCENE} seconds).")


if __name__ == "__main__":
    main()
