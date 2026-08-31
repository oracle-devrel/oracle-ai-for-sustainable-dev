#!/usr/bin/env swift

import AppKit
import AVFoundation
import CoreVideo
import Foundation

let width = 1920
let height = 1080
let fps: Int32 = 24
let secondsPerScene = 12.0
let root = URL(fileURLWithPath: FileManager.default.currentDirectoryPath)
let imageRoot = root.deletingLastPathComponent().appendingPathComponent("images")
let output = root.appendingPathComponent("gemini-oracle-a2a-walkthrough.mp4")
let poster = root.appendingPathComponent("gemini-oracle-a2a-walkthrough-poster.png")
let srtURL = root.appendingPathComponent("gemini-oracle-a2a-walkthrough.srt")
let vttURL = root.appendingPathComponent("gemini-oracle-a2a-walkthrough.vtt")
try FileManager.default.createDirectory(at: root.appendingPathComponent(".build"), withIntermediateDirectories: true)

struct Scene {
    let eyebrow: String
    let title: String
    let caption: String
    let detail: String
    let image: String?
    let file: String?
    let code: String?
    let cursorFrom: CGPoint
    let cursorTo: CGPoint
}

let scenes = [
    Scene(eyebrow: "ORACLE AI DATABASE + GEMINI ENTERPRISE", title: "One governed data core.\nFocused A2A agents.", caption: "Follow stockout risk from relational analysis through graph, spatial, governed action, and interactive review.", detail: "Silent walkthrough · sanitized captures · animated interactions", image: nil, file: nil, code: nil, cursorFrom: CGPoint(x: 390, y: 650), cursorTo: CGPoint(x: 770, y: 650)),
    Scene(eyebrow: "STEP 0 · SELECT THE AGENT", title: "Open each specialist\nexplicitly.", caption: "Registration makes agents available, but Core Assistant does not automatically delegate to every listed agent.", detail: "Select the database, graph, spatial, action, or A2UI specialist before prompting.", image: "demo-agent-registration.png", file: nil, code: nil, cursorFrom: CGPoint(x: 1100, y: 380), cursorTo: CGPoint(x: 1110, y: 680)),
    Scene(eyebrow: "STEPS 1–2 · RELATIONAL + SELECT AI", title: "Find the risk, then\ndrill into SKU-500.", caption: "The managed database agent, or the labeled custom fallback, uses a narrow Select AI profile and SQL_TOOL over allowlisted views.", detail: "Ask for risk, revenue impact, region, and the warehouses driving the result.", image: nil, file: "sql/verify_oracle_ai_database_agent.sql", code: """
SELECT DBMS_CLOUD_AI_AGENT.RUN_TEAM(
  team_name => 'ORACLE_AI_DATABASE_AGENT',
  user_prompt => 'List top stockout risks next quarter'
);

-- The team uses its allowlisted Select AI profile
-- and SQL_TOOL. The host receives results, not credentials.
""", cursorFrom: CGPoint(x: 1040, y: 500), cursorTo: CGPoint(x: 1430, y: 690)),
    Scene(eyebrow: "STEP 3 · PROPERTY GRAPH", title: "Trace the complete\ndependency path.", caption: "The graph specialist traverses supplier, plant, port, warehouse, product, and active alert relationships stored in Oracle AI Database.", detail: "Prompt: show dependencies for SKU-500 and render the graph as an image.", image: nil, file: "sql/setup_supply_chain_graph_schema.sql", code: """
CREATE PROPERTY GRAPH supply_chain_graph
  VERTEX TABLES (
    suppliers, plants, ports, warehouses, products)
  EDGE TABLES (
    supplier_plant, plant_port,
    port_warehouse, warehouse_product);
""", cursorFrom: CGPoint(x: 1040, y: 495), cursorTo: CGPoint(x: 1450, y: 690)),
    Scene(eyebrow: "STEP 4 · SPATIAL", title: "Map pressure and\nthe safest relief route.", caption: "The spatial specialist joins warehouse coordinates to governed risk snapshots and identifies Newark pressure with DFW as the relief source.", detail: "Prompt: show SKU-500 hotspots and highlight the best relief route.", image: nil, file: "sql/setup_inventory_risk_demo_schema.sql", code: """
SELECT product_id, warehouse_name,
       latitude, longitude,
       stockout_probability, projected_shortage
  FROM inventory_risk_spatial_v
 WHERE product_id = :product_id
 ORDER BY stockout_probability DESC;
""", cursorFrom: CGPoint(x: 1060, y: 500), cursorTo: CGPoint(x: 1420, y: 710)),
    Scene(eyebrow: "STEP 5 · GOVERNED ACTION", title: "Recommend a move.\nDo not execute it.", caption: "The coordinator gathers graph, spatial, and external evidence, drafts a DFW-to-Newark transfer, and states that approval is required.", detail: "The model proposes. The database still owns the final transaction boundary.", image: nil, file: "oracle_agent_java/.../InventoryActionAgent.java", code: """
graphEvidence = getGraphEvidence(productId);
spatialEvidence = getSpatialEvidence(productId);
externalSignals = getExternalSignals(productId);

checkTransferPolicy(...);
draftInventoryTransferAction(...);
// Never claim the inventory move was executed.
""", cursorFrom: CGPoint(x: 1040, y: 495), cursorTo: CGPoint(x: 1440, y: 720)),
    Scene(eyebrow: "STEP 6 · NATIVE A2UI", title: "Render the review\nas native controls.", caption: "Select Oracle Supply-Chain A2UI, submit the bounded prompt, review the Oracle result, enter notes, and choose approve or cancel.", detail: "A2A transports A2UI DataParts. Gemini Enterprise renders its trusted catalog.", image: "demo-a2ui-review.png", file: nil, code: nil, cursorFrom: CGPoint(x: 1460, y: 360), cursorTo: CGPoint(x: 970, y: 720)),
    Scene(eyebrow: "STEP 7 · OPTIONAL MCP APP", title: "Compare a portable\nsandboxed dashboard.", caption: "Enable the private OAuth-backed MCP connector and call show-inventory-transfer-dashboard. Confirm tool discovery before presenting the widget as validated.", detail: "Same Oracle-governed result · different UI runtime contract", image: nil, file: "a2ui_mcpapps_mcptoolkit/mcp-app/server.ts", code: """
tool: show-inventory-transfer-dashboard
input: {
  minimumStockoutRisk: 70,
  maximumRows: 3
}
resource: ui://inventory-exchange/dashboard-v2
mode: read-only demonstration
""", cursorFrom: CGPoint(x: 1070, y: 510), cursorTo: CGPoint(x: 1450, y: 700)),
    Scene(eyebrow: "STEP 8 · AUTHORIZATION", title: "Filter rows before\nthe model sees them.", caption: "Authenticated database identities and Deep Data Security can restrict rows before any agent, host, UI, or model receives them.", detail: "Identity belongs in authentication and server configuration, never prompt text.", image: nil, file: "a2ui_mcpapps_mcptoolkit/database/07-deep-data-security.sql", code: """
CREATE DATA GRANT inventory_environmental_read
  AS SELECT ON stockout_transfer_recommendation_v
  WHERE category_name = 'Environmental Monitoring'
  TO inventory_environmental_role;

SET USE DATA GRANTS ONLY
  ON stockout_transfer_recommendation_v ENABLED;
""", cursorFrom: CGPoint(x: 1030, y: 500), cursorTo: CGPoint(x: 1440, y: 710)),
    Scene(eyebrow: "DEMO COMPLETE", title: "Explain every result\nand its authority.", caption: "Gemini Enterprise hosts the conversation. A2A connects specialists. Oracle AI Database governs relational, graph, spatial, AI, security, and transaction work.", detail: "No secret or private infrastructure identifier appears in this video.", image: nil, file: nil, code: nil, cursorFrom: CGPoint(x: 620, y: 650), cursorTo: CGPoint(x: 1310, y: 650))
]

let bg = NSColor(calibratedRed: 0.035, green: 0.055, blue: 0.075, alpha: 1)
let panel = NSColor(calibratedRed: 0.075, green: 0.105, blue: 0.13, alpha: 1)
let cyan = NSColor(calibratedRed: 0.12, green: 0.82, blue: 0.92, alpha: 1)
let green = NSColor(calibratedRed: 0.30, green: 0.86, blue: 0.55, alpha: 1)
let orange = NSColor(calibratedRed: 0.96, green: 0.48, blue: 0.23, alpha: 1)
let muted = NSColor(calibratedWhite: 0.72, alpha: 1)

func rounded(_ rect: NSRect, _ radius: CGFloat, _ color: NSColor) {
    color.setFill(); NSBezierPath(roundedRect: rect, xRadius: radius, yRadius: radius).fill()
}

func drawText(_ value: String, _ rect: NSRect, _ font: NSFont, _ color: NSColor, _ alignment: NSTextAlignment = .left) {
    let p = NSMutableParagraphStyle(); p.alignment = alignment; p.lineSpacing = 5; p.lineBreakMode = .byWordWrapping
    value.draw(with: rect, options: [.usesLineFragmentOrigin, .usesFontLeading], attributes: [.font: font, .foregroundColor: color, .paragraphStyle: p])
}

func drawBadge(_ value: String, x: CGFloat, y: CGFloat, color: NSColor) {
    let size = (value as NSString).size(withAttributes: [.font: NSFont.boldSystemFont(ofSize: 21)])
    rounded(NSRect(x: x, y: y, width: size.width + 34, height: 42), 21, color.withAlphaComponent(0.20))
    drawText(value, NSRect(x: x + 17, y: y + 8, width: size.width, height: 26), .boldSystemFont(ofSize: 21), color)
}

func drawScreenshot(_ name: String, in rect: NSRect) {
    guard let image = NSImage(contentsOf: imageRoot.appendingPathComponent(name)) else { return }
    NSGraphicsContext.saveGraphicsState()
    NSBezierPath(roundedRect: rect, xRadius: 20, yRadius: 20).addClip()
    let source = NSRect(x: 0, y: 0, width: image.size.width, height: image.size.height * 0.94)
    image.draw(in: rect, from: source, operation: .sourceOver, fraction: 1, respectFlipped: true, hints: [.interpolation: NSImageInterpolation.high])
    NSGraphicsContext.restoreGraphicsState()
    NSColor(calibratedWhite: 0.4, alpha: 0.7).setStroke(); NSBezierPath(roundedRect: rect, xRadius: 20, yRadius: 20).stroke()
}

func drawCode(_ file: String, _ code: String, in rect: NSRect) {
    rounded(rect, 20, NSColor(calibratedWhite: 0.055, alpha: 1))
    rounded(NSRect(x: rect.minX, y: rect.minY, width: rect.width, height: 58), 20, panel)
    drawText(file, NSRect(x: rect.minX + 30, y: rect.minY + 15, width: rect.width - 60, height: 30), .monospacedSystemFont(ofSize: 21, weight: .semibold), muted)
    let numbered = code.split(separator: "\n", omittingEmptySubsequences: false).enumerated().map { String(format: "%2d  %@", $0.offset + 1, String($0.element)) }.joined(separator: "\n")
    drawText(numbered, NSRect(x: rect.minX + 28, y: rect.minY + 82, width: rect.width - 56, height: rect.height - 105), .monospacedSystemFont(ofSize: 24, weight: .regular), NSColor(calibratedRed: 0.82, green: 0.89, blue: 0.93, alpha: 1))
}

func baseImage(_ scene: Scene, index: Int) -> NSImage {
    let image = NSImage(size: NSSize(width: width, height: height)); image.lockFocusFlipped(true)
    bg.setFill(); NSBezierPath(rect: NSRect(x: 0, y: 0, width: width, height: height)).fill()
    cyan.withAlphaComponent(0.08).setStroke()
    for x in stride(from: 0, through: width, by: 80) { let p = NSBezierPath(); p.move(to: NSPoint(x: x, y: 0)); p.line(to: NSPoint(x: x, y: height)); p.stroke() }
    for y in stride(from: 0, through: height, by: 80) { let p = NSBezierPath(); p.move(to: NSPoint(x: 0, y: y)); p.line(to: NSPoint(x: width, y: y)); p.stroke() }
    drawText(scene.eyebrow, NSRect(x: 70, y: 58, width: 800, height: 32), .boldSystemFont(ofSize: 22), cyan)
    drawText(scene.title, NSRect(x: 70, y: 112, width: 580, height: 190), .boldSystemFont(ofSize: index == 0 ? 68 : 52), .white)
    drawText(scene.detail, NSRect(x: 74, y: 325, width: 555, height: 105), .systemFont(ofSize: 24), muted)
    let visual = NSRect(x: 690, y: 120, width: 1160, height: 700)
    if let name = scene.image { drawScreenshot(name, in: visual) }
    else if let file = scene.file, let code = scene.code { drawCode(file, code, in: visual) }
    else {
        rounded(visual, 24, panel)
        drawBadge("A2A", x: 790, y: 360, color: cyan)
        drawBadge("A2UI", x: 990, y: 360, color: green)
        drawBadge("MCP Apps", x: 1210, y: 360, color: orange)
        drawBadge("Oracle AI Database", x: 1490, y: 360, color: cyan)
        drawText("discover  →  render  →  interact  →  govern", NSRect(x: 790, y: 485, width: 930, height: 55), .boldSystemFont(ofSize: 33), .white, .center)
    }
    rounded(NSRect(x: 135, y: 875, width: 1650, height: 135), 20, NSColor(calibratedWhite: 0.015, alpha: 0.94))
    drawText(scene.caption, NSRect(x: 180, y: 905, width: 1560, height: 78), .boldSystemFont(ofSize: 30), .white, .center)
    drawText(String(format: "%02d / %02d", index + 1, scenes.count), NSRect(x: 1690, y: 58, width: 160, height: 30), .monospacedSystemFont(ofSize: 18, weight: .medium), muted, .right)
    image.unlockFocus(); return image
}

func cgImage(_ image: NSImage) -> CGImage { var r = NSRect(origin: .zero, size: image.size); return image.cgImage(forProposedRect: &r, context: nil, hints: nil)! }

func stamp(_ seconds: Double, comma: Bool) -> String {
    let ms = Int((seconds * 1000).rounded()); return String(format: "%02d:%02d:%02d%@%03d", ms / 3_600_000, (ms / 60_000) % 60, (ms / 1000) % 60, comma ? "," : ".", ms % 1000)
}

var srt = ""; var vtt = "WEBVTT\n\n"
for (i, scene) in scenes.enumerated() {
    let start = Double(i) * secondsPerScene + 0.2; let end = Double(i + 1) * secondsPerScene - 0.2
    srt += "\(i + 1)\n\(stamp(start, comma: true)) --> \(stamp(end, comma: true))\n\(scene.caption)\n\n"
    vtt += "\(stamp(start, comma: false)) --> \(stamp(end, comma: false))\n\(scene.caption)\n\n"
}
srt = srt.trimmingCharacters(in: .whitespacesAndNewlines) + "\n"
vtt = vtt.trimmingCharacters(in: .whitespacesAndNewlines) + "\n"
try srt.write(to: srtURL, atomically: true, encoding: .utf8); try vtt.write(to: vttURL, atomically: true, encoding: .utf8)

let bases = scenes.enumerated().map { cgImage(baseImage($0.element, index: $0.offset)) }
let first = NSBitmapImageRep(cgImage: bases[0]); try first.representation(using: .png, properties: [:])!.write(to: poster)
if FileManager.default.fileExists(atPath: output.path) { try FileManager.default.removeItem(at: output) }
let writer = try AVAssetWriter(outputURL: output, fileType: .mp4)
let settings: [String: Any] = [AVVideoCodecKey: AVVideoCodecType.h264, AVVideoWidthKey: width, AVVideoHeightKey: height, AVVideoCompressionPropertiesKey: [AVVideoAverageBitRateKey: 7_000_000]]
let input = AVAssetWriterInput(mediaType: .video, outputSettings: settings); input.expectsMediaDataInRealTime = false
let attrs: [String: Any] = [kCVPixelBufferPixelFormatTypeKey as String: kCVPixelFormatType_32ARGB, kCVPixelBufferWidthKey as String: width, kCVPixelBufferHeightKey as String: height]
let adaptor = AVAssetWriterInputPixelBufferAdaptor(assetWriterInput: input, sourcePixelBufferAttributes: attrs); writer.add(input); writer.startWriting(); writer.startSession(atSourceTime: .zero)
let colorSpace = CGColorSpaceCreateDeviceRGB(); let totalFrames = Int(Double(scenes.count) * secondsPerScene * Double(fps))
for frame in 0..<totalFrames {
    while !input.isReadyForMoreMediaData { Thread.sleep(forTimeInterval: 0.001) }
    let t = Double(frame) / Double(fps); let sceneIndex = min(scenes.count - 1, Int(t / secondsPerScene)); let p = (t.truncatingRemainder(dividingBy: secondsPerScene)) / secondsPerScene
    var buffer: CVPixelBuffer?; CVPixelBufferCreate(kCFAllocatorDefault, width, height, kCVPixelFormatType_32ARGB, attrs as CFDictionary, &buffer)
    let pixel = buffer!; CVPixelBufferLockBaseAddress(pixel, [])
    let c = CGContext(data: CVPixelBufferGetBaseAddress(pixel), width: width, height: height, bitsPerComponent: 8, bytesPerRow: CVPixelBufferGetBytesPerRow(pixel), space: colorSpace, bitmapInfo: CGImageAlphaInfo.noneSkipFirst.rawValue)!
    c.draw(bases[sceneIndex], in: CGRect(x: 0, y: 0, width: width, height: height))
    let a = scenes[sceneIndex].cursorFrom, b = scenes[sceneIndex].cursorTo; let q = min(1, max(0, (p - 0.15) / 0.65)); let x = a.x + (b.x - a.x) * q; let yTop = a.y + (b.y - a.y) * q; let y = CGFloat(height) - yTop
    c.setFillColor(NSColor.white.cgColor); c.setStrokeColor(NSColor.black.withAlphaComponent(0.75).cgColor); c.setLineWidth(4)
    let cursor = CGMutablePath(); cursor.move(to: CGPoint(x: x, y: y)); cursor.addLine(to: CGPoint(x: x + 13, y: y - 42)); cursor.addLine(to: CGPoint(x: x + 24, y: y - 25)); cursor.addLine(to: CGPoint(x: x + 42, y: y - 34)); cursor.addLine(to: CGPoint(x: x + 49, y: y - 22)); cursor.addLine(to: CGPoint(x: x + 31, y: y - 13)); cursor.closeSubpath(); c.addPath(cursor); c.drawPath(using: .fillStroke)
    if p > 0.78 && p < 0.94 { let radius = CGFloat(22 + 55 * ((p - 0.78) / 0.16)); c.setStrokeColor(cyan.withAlphaComponent(0.9).cgColor); c.setLineWidth(6); c.strokeEllipse(in: CGRect(x: x - radius, y: y - radius, width: radius * 2, height: radius * 2)) }
    CVPixelBufferUnlockBaseAddress(pixel, []); adaptor.append(pixel, withPresentationTime: CMTime(value: Int64(frame), timescale: fps))
}
input.markAsFinished(); let sem = DispatchSemaphore(value: 0); writer.finishWriting { sem.signal() }; sem.wait()
guard writer.status == .completed else { throw writer.error ?? NSError(domain: "VideoBuild", code: 1) }
print("Created \(output.lastPathComponent), poster, SRT, and VTT (\(Int(Double(scenes.count) * secondsPerScene)) seconds).")
