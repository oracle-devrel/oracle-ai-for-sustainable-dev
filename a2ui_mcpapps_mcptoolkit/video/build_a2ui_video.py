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
SECONDS_PER_SCENE = 6
ROOT = Path(__file__).resolve().parent
PROJECT = ROOT.parent
IMAGE_ROOT = PROJECT / "images"
OUTPUT = ROOT / "a2ui-developer-walkthrough.mp4"
POSTER = ROOT / "a2ui-developer-walkthrough-poster.png"
SRT = ROOT / "a2ui-developer-walkthrough.srt"
VTT = ROOT / "a2ui-developer-walkthrough.vtt"


@dataclass(frozen=True)
class Scene:
    title: str
    caption: str
    source: str
    image: str | None = None
    code: str | None = None
    cursor_from: tuple[int, int] = (620, 680)
    cursor_to: tuple[int, int] = (1320, 680)


SCENES: list[Scene] = [
    Scene("A2UI starts with a contract", "A2UI lets an agent return a declarative UI recipe that the host renders with approved native components.", "images/interactive-ai-architecture.svg", "interactive-ai-architecture.svg"),
    Scene("Two A2UI runtime paths", "This project uses A2UI v0.9.1 over AG-UI locally and A2UI v0.8 DataParts over A2A in Gemini Enterprise.", "blog.html"),
    Scene("Open the standalone app", "The local browser demonstrates A2UI beside AG-UI event streaming.", "web-client/index.html", "local-end-to-end-application.png"),
    Scene("Start the Toolkit", "The Oracle Database MCP Java Toolkit runs separately and exposes governed database tools.", "oracle-db-mcp-toolkit/run.sh", code="java -jar oracle-db-mcp-toolkit.jar \\\n  --config config/tools.yaml \\\n  --transport streamable-http \\\n  --port 3000"),
    Scene("Start the service", "The Spring Boot service orchestrates UI protocols, approval state, and database tools.", "agent-service/run.sh", code="./mvnw spring-boot:run\n# or\nmvn spring-boot:run"),
    Scene("Submit a review request", "The browser asks for stockout transfer recommendations and opens a streaming run.", "web-client/app.js", code="const response = await fetch('/api/runs/stream', {\n  method: 'POST',\n  body: formData\n});"),
    Scene("Handle AG-UI events", "The browser receives AG-UI events and watches for custom A2UI messages.", "web-client/app.js", code="if (event.type === 'STATE_SNAPSHOT') {\n  updateState(event.snapshot);\n} else if (event.type === 'CUSTOM' && event.name === 'a2ui.message') {\n  renderA2ui(event.value);\n}"),
    Scene("Stream from the service", "The service streams the run from /api/runs/stream.", "AgentController.java", code="@PostMapping('/api/runs/stream')\nvoid streamRun(...) {\n  agui.stream(output,\n      minimumStockoutRisk,\n      maximumRows,\n      actor);\n}"),
    Scene("Emit run and tool events", "The service emits run, text, tool-call, state, and custom events.", "AguiRunService.java", code='send(output, Map.of("type", "TOOL_CALL_START",\n    "toolCallName", "find-stockout-transfer-recommendations"));\nsend(output, Map.of("type", "STATE_SNAPSHOT",\n    "snapshot", Map.of("status", "AWAITING_APPROVAL")));'),
    Scene("Carry A2UI in AG-UI", "A2UI messages are carried in AG-UI CUSTOM events named a2ui.message.", "AguiRunService.java", code='private void a2ui(OutputStream output, Map<String, Object> envelope)\n    throws IOException {\n  send(output, Map.of("type", "CUSTOM",\n      "name", "a2ui.message", "value", envelope));\n}'),
    Scene("Create the A2UI surface", "The standalone path creates an A2UI v0.9.1 surface with a catalog and theme.", "A2uiPayloads.java", code='return Map.of("version", VERSION, "createSurface", Map.of(\n  "surfaceId", "inventory-transfer-review",\n  "catalogId", CATALOG,\n  "theme", Map.of("primaryColor", "#c74634")));'),
    Scene("Declare approved controls", "The service declares Image, Text, List, Card, TextField, Row, and Button components.", "A2uiPayloads.java", code='Map.of("id", "recommendationCard",\n  "component", "Card",\n  "child", "recommendationText"),\nMap.of("id", "confirm",\n  "component", "Button",\n  "text", "Approve inventory transfer")'),
    Scene("Bind database values", "Recommendation values from Oracle AI Database are bound into the A2UI data model.", "A2uiPayloads.java", code='"value", Map.of(\n  "summary", recommendations.size() + " governed recommendations require review.",\n  "recommendations", rows,\n  "approvalId", approvalId,\n  "writesAllowed", writesAllowed)'),
    Scene("Call the Toolkit gateway", "The service asks the Toolkit for bounded transfer recommendations.", "McpToolkitSupplyChainGateway.java", code='mcpClient.callTool(\n  "find-stockout-transfer-recommendations",\n  Map.of("minimumStockoutRisk", minimumStockoutRisk,\n         "maximumRows", maximumRows));'),
    Scene("Map tool to SQL", "The Toolkit maps find-stockout-transfer-recommendations to bounded SQL.", "oracle-db-mcp-toolkit/config/tools.yaml", code="name: find-stockout-transfer-recommendations\nkind: sql\nsql: |\n  SELECT * FROM stockout_transfer_recommendation_v\n  WHERE stockout_risk_score >= :minimumStockoutRisk"),
    Scene("Calculate recommendations", "Oracle AI Database calculates feasible source-to-target transfer candidates.", "database/04-views.sql", code="CREATE OR REPLACE VIEW stockout_transfer_recommendation_v AS\nSELECT sku, product_name, source_location_code,\n       target_location_code, stockout_risk_score,\n       recommended_transfer_quantity\nFROM ..."),
    Scene("Render native review UI", "The host renders a native review form from the recipe rather than executing generated frontend code.", "images/local-end-to-end-application.png", "local-end-to-end-application.png"),
    Scene("Carry approval state", "The data model carries the approval ID, selected recommendation, permissions, and notes.", "A2uiPayloads.java", code='"approvalId", approvalId,\n"writesAllowed", writesAllowed,\n"form", Map.of("approvalNotes",\n  "Approve the database-recommended transfer...")'),
    Scene("Approve one transfer", "The user explicitly approves one exact transfer from the rendered controls.", "web-client/app.js", code="async function approveSelectedTransfer() {\n  await fetch('/api/approve', {\n    method: 'POST',\n    body: approvalFormData\n  });\n}"),
    Scene("Validate before writing", "The service receives approval and validates it before any database write.", "AgentController.java", code="@PostMapping('/api/approve')\nMap<String, Object> approve(...) {\n  return runtime.approveTransfer(\n    approvalId, recommendationId, notes, actor);\n}"),
    Scene("Prevent substitution", "The approval service prevents arbitrary transfer substitution after review.", "ApprovalService.java", code="TransferRecommendation recommendation =\n  approvals.requireRecommendation(approvalId, recommendationId);\nrepository.approveTransfer(recommendation, notes);"),
    Scene("Execute the write tool", "The Toolkit executes the approved write through a stored procedure.", "oracle-db-mcp-toolkit/config/tools.yaml", code="name: approve-inventory-transfer\nkind: sql\nsql: |\n  BEGIN\n    approve_inventory_transfer(:approvalId, :recommendationId, :approvalNotes);\n  END;"),
    Scene("Switch to Gemini A2A", "Gemini Enterprise receives A2UI as DataParts instead of AG-UI custom events.", "gemini-enterprise-a2a/main.py", code="parts = [\n  Part(root=TextPart(text='Showing recommendations')),\n  *(create_a2ui_part(message) for message in messages),\n]"),
    Scene("Advertise A2UI support", "The A2A agent card advertises the A2UI extension and supported catalog.", "gemini-enterprise-a2a/main.py", code="capabilities=AgentCapabilities(\n  streaming=True,\n  extensions=[get_a2ui_agent_extension(\n    VERSION_0_8, False, [STANDARD_CATALOG])])"),
    Scene("Wrap each A2UI message", "The adapter wraps each A2UI message with create_a2ui_part.", "gemini-enterprise-a2a/main.py", code="*(create_a2ui_part(message) for message in messages)"),
    Scene("Build v0.8 messages", "The Gemini path builds beginRendering, surfaceUpdate, and dataModelUpdate messages.", "gemini-enterprise-a2a/a2ui_payloads.py", code='return [\n  {"beginRendering": {"surfaceId": surface_id, "root": "root"}},\n  {"surfaceUpdate": {"surfaceId": surface_id, "components": components}},\n  {"dataModelUpdate": {"surfaceId": surface_id, "contents": [...]}}\n]'),
    Scene("Render in Gemini Enterprise", "Gemini Enterprise renders the review surface from the A2UI DataParts.", "images/gemini-enterprise-a2ui-review.png", "gemini-enterprise-a2ui-review.png"),
    Scene("Add graph A2UI", "The graph agent now returns portable A2UI cards and inspect buttons plus its deterministic PNG.", "oracle-ai-database-gcp-gemini/oracle_agent_java/.../GraphA2uiPayloads.java", code='components.add(card("node-card-" + index,\n  List.of(title, details, inspectButton)));\nbutton("inspect-" + index,\n  "Inspect this graph node", "inspectGraphNode");'),
    Scene("Keep the boundary clear", "A2UI gives controlled native UI while Oracle AI Database remains the governed data and transaction layer.", "images/a2ui-host-process-boundary.svg", "a2ui-host-process-boundary.svg"),
]


def font(size: int, bold: bool = False, mono: bool = False) -> ImageFont.FreeTypeFont:
    names = ["/System/Library/Fonts/Menlo.ttc"] if mono else [
        "/System/Library/Fonts/Supplemental/Arial Bold.ttf" if bold else "/System/Library/Fonts/Supplemental/Arial.ttf",
        "/System/Library/Fonts/Helvetica.ttc",
    ]
    for name in names:
        try:
            return ImageFont.truetype(name, size=size)
        except OSError:
            pass
    return ImageFont.load_default(size=size)


F_TITLE = font(56, True)
F_CAPTION = font(34, True)
F_BODY = font(26)
F_MONO = font(28, mono=True)
F_SMALL = font(20, mono=True)


def draw_wrapped(draw: ImageDraw.ImageDraw, text: str, box, fnt, fill, spacing=8, align="left"):
    x, y, w, h = box
    lines: list[str] = []
    for para in text.splitlines() or [""]:
        approx = max(12, int(w / max(10, fnt.size * 0.56)))
        lines.extend(wrap(para, approx, break_long_words=False) or [""])
    cy = y
    for line in lines:
        if cy > y + h - fnt.size:
            break
        tx = x
        if align == "center":
            tx = x + (w - draw.textlength(line, font=fnt)) / 2
        draw.text((tx, cy), line, font=fnt, fill=fill)
        cy += fnt.size + spacing


def rounded(draw, xy, radius, fill, outline=None, width=1):
    draw.rounded_rectangle(xy, radius=radius, fill=fill, outline=outline, width=width)


def visual(name: str, size: tuple[int, int]) -> Image.Image:
    path = IMAGE_ROOT / name
    if path.exists() and path.suffix.lower() != ".svg":
        return ImageOps.contain(Image.open(path).convert("RGB"), size, Image.Resampling.LANCZOS)
    img = Image.new("RGB", size, (18, 29, 38))
    d = ImageDraw.Draw(img)
    rounded(d, (28, 28, size[0] - 28, size[1] - 28), 24, (30, 43, 55), (72, 104, 126), 2)
    draw_wrapped(d, "Diagram / source asset", (80, 110, size[0] - 160, 70), font(44, True), (255, 255, 255))
    draw_wrapped(d, name, (80, 205, size[0] - 160, 80), font(30, mono=True), (174, 224, 236))
    draw_wrapped(d, "See the linked file in the repository for the exact source or SVG.", (80, 320, size[0] - 160, 120), font(30), (220, 228, 236))
    return img


def draw_code(draw, scene: Scene, box):
    x, y, w, h = box
    rounded(draw, (x, y, x + w, y + h), 22, (10, 18, 25), (72, 97, 115), 2)
    rounded(draw, (x, y, x + w, y + 58), 22, (28, 41, 52))
    draw.text((x + 28, y + 16), scene.source, font=F_SMALL, fill=(198, 210, 220))
    code = scene.code or scene.source
    numbered = "\n".join(f"{i + 1:>2}  {line}" for i, line in enumerate(code.splitlines()))
    draw_wrapped(draw, numbered, (x + 28, y + 88, w - 56, h - 112), F_MONO, (217, 232, 237), 7)


def draw_cursor(draw, progress, start=(650, 650), end=(1370, 650)):
    q = min(1, max(0, (progress - 0.15) / 0.65))
    x = start[0] + (end[0] - start[0]) * q
    y = start[1] + (end[1] - start[1]) * q
    pts = [(x, y), (x + 14, y + 46), (x + 25, y + 27), (x + 46, y + 37), (x + 52, y + 24), (x + 32, y + 14)]
    draw.polygon(pts, fill=(255, 255, 255), outline=(0, 0, 0))
    if 0.78 < progress < 0.95:
        r = int(22 + 60 * ((progress - 0.78) / 0.17))
        draw.ellipse((x - r, y - r, x + r, y + r), outline=(28, 209, 232), width=5)


def render(scene: Scene, idx: int, progress: float) -> Image.Image:
    img = Image.new("RGB", (WIDTH, HEIGHT), (7, 13, 19))
    draw = ImageDraw.Draw(img)
    for x in range(0, WIDTH, 80):
        draw.line((x, 0, x, HEIGHT), fill=(11, 43, 53))
    for y in range(0, HEIGHT, 80):
        draw.line((0, y, WIDTH, y), fill=(11, 43, 53))
    draw.text((70, 55), f"A2UI STEP {idx + 1:02d}", font=font(24, True), fill=(35, 208, 232))
    draw_wrapped(draw, scene.title, (70, 112, 565, 170), F_TITLE, (255, 255, 255), 10)
    draw_wrapped(draw, scene.source, (74, 315, 560, 110), F_BODY, (178, 194, 205), 8)
    box = (690, 120, 1160, 700)
    if scene.image:
        v = visual(scene.image, (box[2], box[3]))
        rounded(draw, (box[0], box[1], box[0] + box[2], box[1] + box[3]), 22, (18, 28, 37), (76, 99, 118), 2)
        img.paste(v, (box[0] + (box[2] - v.width) // 2, box[1] + (box[3] - v.height) // 2))
    else:
        draw_code(draw, scene, box)
    rounded(draw, (135, 875, 1785, 1010), 22, (3, 6, 9), (38, 58, 72), 1)
    draw_wrapped(draw, scene.caption, (180, 903, 1560, 82), F_CAPTION, (255, 255, 255), 8, "center")
    draw.text((1690, 58), f"{idx + 1:02d} / {len(SCENES):02d}", font=F_SMALL, fill=(184, 197, 207))
    draw_cursor(draw, progress, scene.cursor_from, scene.cursor_to)
    return img


def ts(seconds: float, comma: bool) -> str:
    ms = round(seconds * 1000)
    sep = "," if comma else "."
    return f"{ms // 3600000:02d}:{(ms // 60000) % 60:02d}:{(ms // 1000) % 60:02d}{sep}{ms % 1000:03d}"


def captions():
    srt: list[str] = []
    vtt: list[str] = ["WEBVTT", ""]
    for i, scene in enumerate(SCENES):
        start = i * SECONDS_PER_SCENE + 0.2
        end = (i + 1) * SECONDS_PER_SCENE - 0.2
        srt.extend([str(i + 1), f"{ts(start, True)} --> {ts(end, True)}", scene.caption, ""])
        vtt.extend([f"{ts(start, False)} --> {ts(end, False)}", scene.caption, ""])
    SRT.write_text("\n".join(srt), encoding="utf-8")
    VTT.write_text("\n".join(vtt), encoding="utf-8")


def main():
    captions()
    render(SCENES[0], 0, 0.5).save(POSTER)
    with imageio.get_writer(OUTPUT, fps=FPS, codec="libx264", quality=8, macro_block_size=None, ffmpeg_params=["-pix_fmt", "yuv420p"]) as writer:
        for i, scene in enumerate(SCENES):
            for frame in range(FPS * SECONDS_PER_SCENE):
                writer.append_data(np.asarray(render(scene, i, frame / (FPS * SECONDS_PER_SCENE))))
    print(f"Created {OUTPUT.name}, poster, SRT, and VTT ({len(SCENES) * SECONDS_PER_SCENE} seconds).")


if __name__ == "__main__":
    main()
