#!/usr/bin/env swift

import AppKit
import AVFoundation
import CoreVideo
import Foundation

let width = 1920
let height = 1080
let fps: Int32 = 24
let root = URL(fileURLWithPath: FileManager.default.currentDirectoryPath)
let build = root.appendingPathComponent(".build")
let finalVideo = root.appendingPathComponent("interactive-ai-walkthrough.mp4")
let poster = root.appendingPathComponent("interactive-ai-walkthrough-poster.png")
let srtFile = root.appendingPathComponent("interactive-ai-walkthrough.srt")
let vttFile = root.appendingPathComponent("interactive-ai-walkthrough.vtt")

enum VisualKind { case title, architecture, code, application, result }

struct Scene {
    let narrationFile: String
    let eyebrow: String
    let title: String
    let subtitle: String
    let kind: VisualKind
    let fileLabel: String?
    let code: String?
}

struct TimedScene {
    let scene: Scene
    let narration: String
    let start: Double
    let duration: Double
}

struct Cue {
    let start: Double
    let end: Double
    let text: String
    let sceneIndex: Int
}

let scenes: [Scene] = [
    Scene(narrationFile: "01-intro.txt", eyebrow: "ORACLE AI DATABASE · INTERACTIVE AI", title: "From agent answer\nto governed action", subtitle: "A2UI · AG-UI · MCP Apps · Oracle Database MCP Java Toolkit", kind: .title, fileLabel: nil, code: nil),
    Scene(narrationFile: "02-architecture.txt", eyebrow: "PROTOCOL RESPONSIBILITY MAP", title: "Tools, agents, and GUI\nhave different boundaries", subtitle: "MCP · agent to tools     A2A · agent to agent     AG-UI · agent to frontend", kind: .architecture, fileLabel: nil, code: nil),
    Scene(narrationFile: "03-mcp-runtime.txt", eyebrow: "LIVE MCP RUNTIME", title: "Verify the server and\nexact tool allowlist", subtitle: "The default path launches the pinned Oracle Toolkit over MCP stdio.", kind: .code, fileLabel: "McpToolkitRiskRepository.java", code: """
client.initialize();
Set<String> available = client.listTools();

if (!available.equals(REQUIRED_TOOLS))
  throw new IllegalStateException("Tool allowlist mismatch");

client.callTool("find-at-risk-customers", Map.of(
    "minimumRisk", minimumRisk,
    "maximumRows", maximumRows));
"""),
    Scene(narrationFile: "04-mcp-tools.txt", eyebrow: "ORACLE DATABASE MCP JAVA TOOLKIT", title: "Expose governed business tools,\nnot unrestricted SQL", subtitle: "Five YAML-defined, bound operations are enabled—and no others.", kind: .code, fileLabel: "oracle-db-mcp-toolkit/config/tools.yaml", code: """
toolsets:
  account-risk:
    - find-at-risk-customers
    - get-customer-risk-details
    - reserve-customer-action-id
    - create-customer-follow-up
    - count-customer-actions

statement: >-
  BEGIN create_customer_follow_up_mcp(...); END;
"""),
    Scene(narrationFile: "05-database.txt", eyebrow: "ORACLE AI DATABASE", title: "Govern the data and\nexecute one atomic action", subtitle: "Views stabilize reads; an input-only procedure owns the approved write.", kind: .code, fileLabel: "database/06-mcp-procedure.sql", code: """
SELECT customer_id INTO v_customer_id
  FROM customer_accounts
 WHERE customer_id = p_customer_id
   FOR UPDATE;

INSERT INTO customer_actions (...)
VALUES (..., 'APPROVED');

UPDATE customer_accounts
   SET follow_up_status = 'ACTION APPROVED';
"""),
    Scene(narrationFile: "06-agui.txt", eyebrow: "AG-UI EVENT STREAM", title: "Stream the run, tool activity,\nand application state", subtitle: "Lifecycle events plus A2UI envelopes carried in a CUSTOM event.", kind: .code, fileLabel: "AguiRunService.java", code: """
send(output, Map.of("type", "RUN_STARTED", ...));
send(output, Map.of("type", "TOOL_CALL_START",
    "toolCallName", "find-at-risk-customers"));

accounts = repository.findAtRisk(minimumRisk, maximumRows);

send(output, Map.of("type", "CUSTOM",
    "name", "a2ui.message",
    "value", A2uiPayloads.updateData(...)));
"""),
    Scene(narrationFile: "07-a2ui.txt", eyebrow: "A2UI TRUSTED RENDERING", title: "Declarative controls,\nnot executable agent code", subtitle: "The client owns the catalog, validation, design system, and DOM insertion.", kind: .code, fileLabel: "web-client/app.js", code: """
const allowedComponents = new Set([
  "Column", "Row", "List", "Card", "Text",
  "Button", "TextField", "ChoicePicker"
]);

if (envelope.version !== "v0.9.1")
  throw new Error("Unsupported A2UI version");

name.textContent = account.customerName;
"""),
    Scene(narrationFile: "08-mcp-app.txt", eyebrow: "SEPARATE MCP APP DEMO", title: "Render governed results\nin the official basic host", subtitle: "The MCP App adapter calls the Java service; the Toolkit remains the database path.", kind: .code, fileLabel: "mcp-app/server.ts", code: """
registerAppTool(server, "show-account-risk-dashboard", {
  title: "Show account risk dashboard",
  inputSchema: z.object({
    minimumRisk: z.number().default(75), maximumRows: z.number().int().default(10)
  }),
  _meta: { ui: { resourceUri:
    "ui://oracle-account-risk/dashboard-v1" } }
}, async ({ minimumRisk, maximumRows }) => {
  const accounts = await loadGovernedAccounts(minimumRisk, maximumRows);
  return { structuredContent: { accounts,
    source: "oracle-db-mcp-java-toolkit" } };
});
"""),
    Scene(narrationFile: "09-application.txt", eyebrow: "LIVE APPLICATION FLOW", title: "Explore governed risk,\nthen choose an action", subtitle: "A2UI results and approval controls alongside the AG-UI event rail.", kind: .application, fileLabel: nil, code: nil),
    Scene(narrationFile: "10-approval.txt", eyebrow: "HUMAN APPROVAL BOUNDARY", title: "Commit exactly one\naudited follow-up", subtitle: "Actor-bound, result-bound, expiring, and single-use approval state.", kind: .result, fileLabel: "Approval result", code: nil),
    Scene(narrationFile: "11-recap.txt", eyebrow: "THE RESPONSIBILITY MAP", title: "Interactive AI without\nweakening governance", subtitle: "Stream · Render · Embed · Expose · Govern", kind: .architecture, fileLabel: nil, code: nil)
]

let fm = FileManager.default
try fm.createDirectory(at: build, withIntermediateDirectories: true)
for url in [finalVideo, poster, srtFile, vttFile] where fm.fileExists(atPath: url.path) {
    try fm.removeItem(at: url)
}

var timedScenes: [TimedScene] = []
var cursor = 0.0
for scene in scenes {
    let narrationURL = root.appendingPathComponent("narration").appendingPathComponent(scene.narrationFile)
    let narration = try String(contentsOf: narrationURL, encoding: .utf8).trimmingCharacters(in: .whitespacesAndNewlines)
    let wordCount = narration.split(whereSeparator: { $0.isWhitespace }).count
    let duration = max(9.0, Double(wordCount) / 2.8 + 3.0)
    timedScenes.append(TimedScene(scene: scene, narration: narration, start: cursor, duration: duration))
    cursor += duration
}
let totalDuration = cursor

func sentences(_ text: String) -> [String] {
    let pattern = "(?<=[.!?])\\s+"
    let regex = try! NSRegularExpression(pattern: pattern)
    let range = NSRange(text.startIndex..., in: text)
    var pieces: [String] = []
    var last = text.startIndex
    for match in regex.matches(in: text, range: range) {
        guard let swiftRange = Range(match.range, in: text) else { continue }
        let piece = text[last..<swiftRange.lowerBound].trimmingCharacters(in: .whitespacesAndNewlines)
        if !piece.isEmpty { pieces.append(piece) }
        last = swiftRange.upperBound
    }
    let tail = text[last...].trimmingCharacters(in: .whitespacesAndNewlines)
    if !tail.isEmpty { pieces.append(tail) }
    return pieces.isEmpty ? [text] : pieces
}

func captionSegments(_ text: String, lineLimit: Int = 42) -> [String] {
    var segments: [String] = []
    for sentence in sentences(text) {
        var lines: [String] = []
        var line = ""
        for word in sentence.split(separator: " ").map(String.init) {
            let candidate = line.isEmpty ? word : line + " " + word
            if candidate.count <= lineLimit || line.isEmpty {
                line = candidate
            } else if lines.isEmpty {
                lines.append(line)
                line = word
            } else {
                segments.append((lines + [line]).joined(separator: "\n"))
                lines.removeAll(keepingCapacity: true)
                line = word
            }
        }
        if !line.isEmpty { lines.append(line) }
        if !lines.isEmpty { segments.append(lines.joined(separator: "\n")) }
    }
    return segments.isEmpty ? [text] : segments
}

var cues: [Cue] = []
for (sceneIndex, timed) in timedScenes.enumerated() {
    let pieces = captionSegments(timed.narration)
    let weights = pieces.map { max(1, $0.split(separator: " ").count) }
    let totalWeight = weights.reduce(0, +)
    var cueStart = timed.start + 0.2
    let cueWindow = timed.duration - 0.4
    for (index, piece) in pieces.enumerated() {
        let fraction = Double(weights[index]) / Double(totalWeight)
        let cueEnd = index == pieces.count - 1 ? timed.start + timed.duration - 0.2 : cueStart + cueWindow * fraction
        cues.append(Cue(start: cueStart, end: cueEnd, text: piece, sceneIndex: sceneIndex))
        cueStart = cueEnd
    }
}

func timestamp(_ seconds: Double, separator: String) -> String {
    let milliseconds = Int((seconds * 1000).rounded())
    let hours = milliseconds / 3_600_000
    let minutes = (milliseconds / 60_000) % 60
    let secs = (milliseconds / 1000) % 60
    let millis = milliseconds % 1000
    return String(format: "%02d:%02d:%02d%@%03d", hours, minutes, secs, separator, millis)
}

var srt = ""
var vtt = "WEBVTT\n\n"
for (index, cue) in cues.enumerated() {
    srt += "\(index + 1)\n\(timestamp(cue.start, separator: ",")) --> \(timestamp(cue.end, separator: ","))\n\(cue.text)\n\n"
    vtt += "\(timestamp(cue.start, separator: ".")) --> \(timestamp(cue.end, separator: "."))\n\(cue.text)\n\n"
}
try srt.write(to: srtFile, atomically: true, encoding: .utf8)
try vtt.write(to: vttFile, atomically: true, encoding: .utf8)

let oracleRed = NSColor(calibratedRed: 0.78, green: 0.16, blue: 0.10, alpha: 1)
let ink = NSColor(calibratedWhite: 0.08, alpha: 1)
let muted = NSColor(calibratedWhite: 0.65, alpha: 1)
let cream = NSColor(calibratedRed: 0.98, green: 0.96, blue: 0.92, alpha: 1)
let panel = NSColor(calibratedWhite: 0.13, alpha: 1)
let blue = NSColor(calibratedRed: 0.20, green: 0.55, blue: 0.78, alpha: 1)
let green = NSColor(calibratedRed: 0.23, green: 0.68, blue: 0.48, alpha: 1)

func rounded(_ rect: NSRect, radius: CGFloat, color: NSColor) {
    color.setFill()
    NSBezierPath(roundedRect: rect, xRadius: radius, yRadius: radius).fill()
}

func text(_ value: String, _ rect: NSRect, font: NSFont, color: NSColor, alignment: NSTextAlignment = .left, lineSpacing: CGFloat = 4) {
    let paragraph = NSMutableParagraphStyle()
    paragraph.alignment = alignment
    paragraph.lineSpacing = lineSpacing
    paragraph.lineBreakMode = .byWordWrapping
    value.draw(with: rect, options: [.usesLineFragmentOrigin, .usesFontLeading], attributes: [
        .font: font, .foregroundColor: color, .paragraphStyle: paragraph
    ])
}

func badge(_ value: String, x: CGFloat, y: CGFloat, color: NSColor) {
    let size = (value as NSString).size(withAttributes: [.font: NSFont.boldSystemFont(ofSize: 22)])
    rounded(NSRect(x: x, y: y, width: size.width + 34, height: 43), radius: 21, color: color.withAlphaComponent(0.22))
    text(value, NSRect(x: x + 17, y: y + 8, width: size.width, height: 28), font: .boldSystemFont(ofSize: 22), color: color)
}

func drawArchitecture(y: CGFloat = 405) {
    rounded(NSRect(x: 555, y: y, width: 810, height: 112), radius: 22, color: panel)
    text("USER-FACING AGENTIC APPLICATION", NSRect(x: 580, y: y + 18, width: 760, height: 25), font: .boldSystemFont(ofSize: 18), color: muted, alignment: .center)
    badge("A2UI · native controls", x: 640, y: y + 55, color: blue)
    badge("MCP Apps · rich iframe", x: 1000, y: y + 55, color: NSColor.systemPurple)

    rounded(NSRect(x: 720, y: y + 205, width: 480, height: 132), radius: 24, color: oracleRed)
    text("Java agent backend", NSRect(x: 750, y: y + 232, width: 420, height: 40), font: .boldSystemFont(ofSize: 32), color: .white, alignment: .center)
    text("orchestration · policy · approval", NSRect(x: 750, y: y + 278, width: 420, height: 30), font: .systemFont(ofSize: 21), color: .white, alignment: .center)

    rounded(NSRect(x: 75, y: y + 214, width: 300, height: 115), radius: 20, color: panel)
    text("TOOLS", NSRect(x: 100, y: y + 238, width: 250, height: 34), font: .boldSystemFont(ofSize: 28), color: .white, alignment: .center)
    text("Toolkit · APIs · services", NSRect(x: 95, y: y + 281, width: 260, height: 28), font: .systemFont(ofSize: 19), color: muted, alignment: .center)

    rounded(NSRect(x: 1545, y: y + 214, width: 300, height: 115), radius: 20, color: panel)
    text("OTHER AGENTS", NSRect(x: 1570, y: y + 238, width: 250, height: 34), font: .boldSystemFont(ofSize: 28), color: .white, alignment: .center)
    text("conceptual · not implemented", NSRect(x: 1565, y: y + 281, width: 260, height: 28), font: .systemFont(ofSize: 18), color: muted, alignment: .center)

    text("AG-UI", NSRect(x: 910, y: y + 138, width: 100, height: 32), font: .boldSystemFont(ofSize: 24), color: oracleRed, alignment: .center)
    text("↕", NSRect(x: 935, y: y + 164, width: 50, height: 40), font: .boldSystemFont(ofSize: 34), color: oracleRed, alignment: .center)
    text("MCP  ·  agent ↔ tools", NSRect(x: 405, y: y + 248, width: 285, height: 34), font: .boldSystemFont(ofSize: 22), color: blue, alignment: .center)
    text("→", NSRect(x: 660, y: y + 246, width: 50, height: 40), font: .boldSystemFont(ofSize: 34), color: blue, alignment: .center)
    text("A2A  ·  agent ↔ agent", NSRect(x: 1230, y: y + 248, width: 285, height: 34), font: .boldSystemFont(ofSize: 22), color: NSColor.systemPurple, alignment: .center)
    text("→", NSRect(x: 1510, y: y + 246, width: 50, height: 40), font: .boldSystemFont(ofSize: 34), color: NSColor.systemPurple, alignment: .center)

    rounded(NSRect(x: 720, y: y + 375, width: 480, height: 78), radius: 20, color: NSColor(calibratedWhite: 0.17, alpha: 1))
    text("Oracle AI Database", NSRect(x: 750, y: y + 390, width: 420, height: 35), font: .boldSystemFont(ofSize: 29), color: .white, alignment: .center)
    text("Toolkit JDBC/UCP · governed results", NSRect(x: 750, y: y + 425, width: 420, height: 24), font: .systemFont(ofSize: 17), color: green, alignment: .center)
    text("↓", NSRect(x: 935, y: y + 335, width: 50, height: 40), font: .boldSystemFont(ofSize: 32), color: green, alignment: .center)
}

func drawCode(label: String, code: String) {
    let rect = NSRect(x: 735, y: 228, width: 1085, height: 610)
    rounded(rect, radius: 22, color: NSColor(calibratedWhite: 0.105, alpha: 1))
    rounded(NSRect(x: rect.minX, y: rect.minY, width: rect.width, height: 64), radius: 22, color: NSColor(calibratedWhite: 0.16, alpha: 1))
    for (index, color) in [NSColor.systemRed, .systemYellow, .systemGreen].enumerated() {
        color.setFill(); NSBezierPath(ovalIn: NSRect(x: rect.minX + 24 + CGFloat(index) * 31, y: rect.minY + 23, width: 16, height: 16)).fill()
    }
    text(label, NSRect(x: rect.minX + 125, y: rect.minY + 18, width: rect.width - 155, height: 34), font: .monospacedSystemFont(ofSize: 23, weight: .semibold), color: muted)
    let numbered = code.split(separator: "\n", omittingEmptySubsequences: false).enumerated().map { String(format: "%2d  %@", $0.offset + 1, String($0.element)) }.joined(separator: "\n")
    text(numbered, NSRect(x: rect.minX + 34, y: rect.minY + 92, width: rect.width - 68, height: rect.height - 120), font: .monospacedSystemFont(ofSize: 25, weight: .regular), color: NSColor(calibratedRed: 0.84, green: 0.88, blue: 0.92, alpha: 1), lineSpacing: 9)
}

func drawApplication() {
    let appRect = NSRect(x: 650, y: 170, width: 1170, height: 690)
    rounded(appRect, radius: 24, color: cream)
    text("Oracle Account-Risk Assistant", NSRect(x: 690, y: 202, width: 720, height: 48), font: .boldSystemFont(ofSize: 34), color: ink)
    rounded(NSRect(x: 690, y: 275, width: 760, height: 80), radius: 16, color: .white)
    text("Minimum risk  90       Maximum rows  3", NSRect(x: 720, y: 297, width: 540, height: 32), font: .systemFont(ofSize: 23), color: ink)
    rounded(NSRect(x: 1265, y: 289, width: 155, height: 48), radius: 10, color: oracleRed)
    text("Run review", NSRect(x: 1280, y: 301, width: 125, height: 28), font: .boldSystemFont(ofSize: 20), color: .white, alignment: .center)
    let accounts = [("Apex Freight Systems", "CRITICAL · 96", "$4.2M"), ("Blue Mesa Energy", "CRITICAL · 93", "$8.1M"), ("Cobalt Health Partners", "CRITICAL · 91", "$3.65M")]
    for (index, account) in accounts.enumerated() {
        let y = 385 + CGFloat(index) * 115
        rounded(NSRect(x: 690, y: y, width: 760, height: 96), radius: 14, color: .white)
        text(index == 0 ? "◉  \(account.0)" : "○  \(account.0)", NSRect(x: 715, y: y + 18, width: 430, height: 32), font: .boldSystemFont(ofSize: 23), color: ink)
        badge(account.1, x: 1160, y: y + 15, color: oracleRed)
        text("Value \(account.2)  ·  Governed risk evidence", NSRect(x: 755, y: y + 57, width: 520, height: 25), font: .systemFont(ofSize: 18), color: NSColor.darkGray)
    }
    rounded(NSRect(x: 1480, y: 275, width: 300, height: 455), radius: 16, color: panel)
    text("AG-UI stream", NSRect(x: 1510, y: 305, width: 240, height: 35), font: .boldSystemFont(ofSize: 25), color: .white)
    let events = ["RUN_STARTED", "STEP_STARTED", "TOOL_CALL_START", "TOOL_CALL_RESULT", "STATE_SNAPSHOT", "RUN_FINISHED"]
    for (index, event) in events.enumerated() {
        let color = event.contains("TOOL") ? blue : green
        color.setFill(); NSBezierPath(ovalIn: NSRect(x: 1512, y: 370 + CGFloat(index) * 54, width: 12, height: 12)).fill()
        text(event, NSRect(x: 1538, y: 361 + CGFloat(index) * 54, width: 215, height: 29), font: .monospacedSystemFont(ofSize: 17, weight: .medium), color: muted)
    }
    rounded(NSRect(x: 690, y: 748, width: 760, height: 78), radius: 14, color: NSColor(calibratedRed: 1, green: 0.94, blue: 0.90, alpha: 1))
    text("Follow-up: Review     Notes: Review current risk evidence", NSRect(x: 715, y: 771, width: 705, height: 30), font: .systemFont(ofSize: 20), color: ink)
}

func drawResult() {
    rounded(NSRect(x: 770, y: 285, width: 950, height: 450), radius: 28, color: panel)
    green.setFill(); NSBezierPath(ovalIn: NSRect(x: 855, y: 390, width: 150, height: 150)).fill()
    text("✓", NSRect(x: 880, y: 410, width: 100, height: 100), font: .boldSystemFont(ofSize: 86), color: .white, alignment: .center)
    text("Action committed", NSRect(x: 1060, y: 365, width: 560, height: 65), font: .boldSystemFont(ofSize: 46), color: .white)
    text("MCP tool returned action ID  ·  APPROVED", NSRect(x: 1063, y: 447, width: 560, height: 38), font: .systemFont(ofSize: 25), color: muted)
    text("Atomic procedure  ·  audited actor  ·  single-use approval", NSRect(x: 1063, y: 507, width: 560, height: 34), font: .monospacedSystemFont(ofSize: 20, weight: .medium), color: green)
    rounded(NSRect(x: 1060, y: 585, width: 285, height: 62), radius: 12, color: oracleRed)
    text("APPROVED", NSRect(x: 1085, y: 600, width: 235, height: 36), font: .boldSystemFont(ofSize: 26), color: .white, alignment: .center)
}

func imageFor(sceneIndex: Int, caption: String) -> NSImage {
    let timed = timedScenes[sceneIndex]
    let scene = timed.scene
    let image = NSImage(size: NSSize(width: width, height: height))
    image.lockFocusFlipped(true)
    NSColor(calibratedWhite: 0.055, alpha: 1).setFill()
    NSBezierPath(rect: NSRect(x: 0, y: 0, width: width, height: height)).fill()
    rounded(NSRect(x: 0, y: 0, width: 18, height: height), radius: 0, color: oracleRed)
    text(scene.eyebrow, NSRect(x: 85, y: 62, width: 1100, height: 34), font: .boldSystemFont(ofSize: 22), color: oracleRed)
    text(scene.title, NSRect(x: 85, y: 117, width: scene.kind == .code || scene.kind == .application ? 570 : 1450, height: 180), font: .boldSystemFont(ofSize: scene.kind == .title ? 75 : 59), color: .white, lineSpacing: 3)
    text(scene.subtitle, NSRect(x: 90, y: scene.kind == .title ? 350 : 305, width: scene.kind == .code || scene.kind == .application ? 540 : 1540, height: 92), font: .systemFont(ofSize: 27), color: muted, lineSpacing: 6)

    switch scene.kind {
    case .title:
        badge("AG-UI", x: 90, y: 505, color: blue)
        badge("A2UI", x: 230, y: 505, color: oracleRed)
        badge("MCP Apps", x: 365, y: 505, color: NSColor.systemPurple)
        badge("Oracle AI Database", x: 570, y: 505, color: green)
        rounded(NSRect(x: 90, y: 620, width: 1180, height: 125), radius: 24, color: panel)
        text("Show high-risk accounts · explain the evidence · approve one follow-up", NSRect(x: 132, y: 655, width: 1100, height: 55), font: .boldSystemFont(ofSize: 33), color: .white)
    case .architecture:
        drawArchitecture()
    case .code:
        drawCode(label: scene.fileLabel ?? "Source", code: scene.code ?? "")
        badge(sceneIndex == 2 ? "CONNECT" : sceneIndex == 3 ? "EXPOSE" : sceneIndex == 4 ? "TRANSACT" : sceneIndex == 5 ? "STREAM" : sceneIndex == 6 ? "RENDER" : "EMBED", x: 92, y: 525, color: sceneIndex % 2 == 0 ? blue : green)
    case .application:
        drawApplication()
    case .result:
        drawResult()
    }

    let captionRect = NSRect(x: 170, y: 914, width: 1580, height: 112)
    rounded(captionRect, radius: 18, color: NSColor(calibratedWhite: 0.02, alpha: 0.93))
    text(caption, NSRect(x: 205, y: 934, width: 1510, height: 78), font: .boldSystemFont(ofSize: 28), color: .white, alignment: .center, lineSpacing: 5)
    text(String(format: "%02d / %02d", sceneIndex + 1, timedScenes.count), NSRect(x: 1710, y: 67, width: 130, height: 28), font: .monospacedSystemFont(ofSize: 18, weight: .medium), color: muted, alignment: .right)
    image.unlockFocus()
    return image
}

func cgImage(_ image: NSImage) -> CGImage {
    var rect = NSRect(origin: .zero, size: image.size)
    return image.cgImage(forProposedRect: &rect, context: nil, hints: nil)!
}

let posterImage = imageFor(sceneIndex: 0, caption: "A governed account-risk workflow from request to audited action.")
let posterRep = NSBitmapImageRep(cgImage: cgImage(posterImage))
try posterRep.representation(using: .png, properties: [:])!.write(to: poster)

let writer = try AVAssetWriter(outputURL: finalVideo, fileType: .mp4)
let videoSettings: [String: Any] = [
    AVVideoCodecKey: AVVideoCodecType.h264,
    AVVideoWidthKey: width,
    AVVideoHeightKey: height,
    AVVideoCompressionPropertiesKey: [AVVideoAverageBitRateKey: 8_000_000, AVVideoProfileLevelKey: AVVideoProfileLevelH264HighAutoLevel]
]
let writerInput = AVAssetWriterInput(mediaType: .video, outputSettings: videoSettings)
writerInput.expectsMediaDataInRealTime = false
let attributes: [String: Any] = [
    kCVPixelBufferPixelFormatTypeKey as String: kCVPixelFormatType_32ARGB,
    kCVPixelBufferWidthKey as String: width,
    kCVPixelBufferHeightKey as String: height
]
let adaptor = AVAssetWriterInputPixelBufferAdaptor(assetWriterInput: writerInput, sourcePixelBufferAttributes: attributes)
guard writer.canAdd(writerInput) else { fatalError("Cannot add video input") }
writer.add(writerInput)
writer.startWriting()
writer.startSession(atSourceTime: CMTime.zero)

let colorSpace = CGColorSpaceCreateDeviceRGB()
var renderedImages: [Int: CGImage] = [:]
for (index, cue) in cues.enumerated() { renderedImages[index] = cgImage(imageFor(sceneIndex: cue.sceneIndex, caption: cue.text)) }
let frameCount = Int(ceil(totalDuration * Double(fps)))
var cueIndex = 0
for frame in 0..<frameCount {
    let seconds = Double(frame) / Double(fps)
    while cueIndex + 1 < cues.count && seconds >= cues[cueIndex].end { cueIndex += 1 }
    while !writerInput.isReadyForMoreMediaData { Thread.sleep(forTimeInterval: 0.002) }
    var buffer: CVPixelBuffer?
    CVPixelBufferCreate(kCFAllocatorDefault, width, height, kCVPixelFormatType_32ARGB, attributes as CFDictionary, &buffer)
    guard let pixelBuffer = buffer else { fatalError("Cannot allocate pixel buffer") }
    CVPixelBufferLockBaseAddress(pixelBuffer, [])
    let context = CGContext(data: CVPixelBufferGetBaseAddress(pixelBuffer), width: width, height: height, bitsPerComponent: 8, bytesPerRow: CVPixelBufferGetBytesPerRow(pixelBuffer), space: colorSpace, bitmapInfo: CGImageAlphaInfo.noneSkipFirst.rawValue)!
    context.draw(renderedImages[cueIndex]!, in: CGRect(x: 0, y: 0, width: width, height: height))
    CVPixelBufferUnlockBaseAddress(pixelBuffer, [])
    adaptor.append(pixelBuffer, withPresentationTime: CMTime(value: Int64(frame), timescale: fps))
}
writerInput.markAsFinished()
let writerSemaphore = DispatchSemaphore(value: 0)
writer.finishWriting { writerSemaphore.signal() }
writerSemaphore.wait()
guard writer.status == AVAssetWriter.Status.completed else { throw writer.error ?? NSError(domain: "VideoBuild", code: 2) }

print(String(format: "Created %@ (%.1f seconds)", finalVideo.lastPathComponent, totalDuration))
print("Created \(poster.lastPathComponent), \(srtFile.lastPathComponent), and \(vttFile.lastPathComponent)")
