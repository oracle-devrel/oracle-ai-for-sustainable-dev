#!/usr/bin/env swift

import AppKit
import AVFoundation
import CoreVideo
import Foundation

let width = 1920
let height = 1080
let fps: Int32 = 24
let root = URL(fileURLWithPath: FileManager.default.currentDirectoryPath)
let memoryRoot = root.deletingLastPathComponent()
let build = root.appendingPathComponent(".build")
let captureRoot = build.appendingPathComponent("captures-latest")
let output = root.appendingPathComponent("memory-agent-walkthrough-enhanced.mp4")
let poster = root.appendingPathComponent("memory-agent-walkthrough-enhanced-poster.png")
let srt = root.appendingPathComponent("memory-agent-walkthrough-enhanced.srt")
let vtt = root.appendingPathComponent("memory-agent-walkthrough-enhanced.vtt")
let enhancedAssets = root.appendingPathComponent("enhanced-assets")

enum Visual {
    case title
    case image(String, String)
    case code(String, String, Int, String)
    case databaseSummary([String], String)
    case recap
}

struct Scene {
    let visual: Visual
    let eyebrow: String
    let title: String
    let subtitle: String
    let caption: String
    let seconds: Double
}

struct StepInfo {
    let number: Int
    let actionTitle: String
    let story: String
    let appImage: String
    let sourcePath: String
    let sourceMarker: String
    let sourceLines: Int
    let methodLabel: String
    let codeCaption: String
    let databaseImage: String?
    let databaseObjects: [String]
    let databaseCaption: String
}

let servicePath = "python-agent/app.py"
let securityPath = "python-agent/deep_security.py"
let parkPath = "python-agent/park.py"
let arPath = "python-agent/ar.py"

let steps: [StepInfo] = [
    StepInfo(number: 1, actionTitle: "Begin Ava's visit", story: "Begin Ava's visit: Click Reset, then Retain. Ava requests a quiet, mobility-friendly visit. Agent Memory stores messages and typed memories with embeddings, showing durable semantic and episodic memory.", appImage: "step-01-app.png", sourcePath: servicePath, sourceMarker: "def retain(self)", sourceLines: 22, methodLabel: "retain()", codeCaption: "retain() creates Ava's thread, stores the conversation, extracts typed records, and adds the earlier rainy-visit episode.", databaseImage: "step-01-db.png", databaseObjects: ["MAGIC_PY_MESSAGE", "MAGIC_PY_MEMORY"], databaseCaption: "Conversation rows and durable memory rows are added. Each memory carries identity scope, metadata, and an embedding for later retrieval."),
    StepInfo(number: 2, actionTitle: "Personalize Ava's plan", story: "Personalize Ava's plan: Click Recall + Reuse. Scoped search retrieves Ava's breakfast, accessibility, entertainment, and prior-visit memories. Oracle AI Database ranks relevant records, showing scope before similarity.", appImage: "step-02-app.png", sourcePath: servicePath, sourceMarker: "def recall(self)", sourceLines: 22, methodLabel: "recall()", codeCaption: "recall() applies exact guest, agent, and thread scope before search ranking, then assembles the selected memories into reusable context.", databaseImage: "step-02-db.png", databaseObjects: ["MAGIC_PY_MEMORY"], databaseCaption: "The database reads Ava-scoped active records and ranks them by relevance. Recall is read-only, so no stored row changes."),
    StepInfo(number: 3, actionTitle: "Correct Ava's preference", story: "Correct Ava's preference: Click Refine. Ava clarifies that she prefers the lantern show, not fireworks. The memory is updated while retaining provenance, demonstrating correction rather than silently preserving misinformation.", appImage: "step-03-app.png", sourcePath: servicePath, sourceMarker: "def correct(self)", sourceLines: 22, methodLabel: "correct()", codeCaption: "correct() finds the inaccurate record and calls the memory update operation with the corrected preference and correction provenance.", databaseImage: "step-03-db.png", databaseObjects: ["MAGIC_PY_MEMORY"], databaseCaption: "The active memory content and metadata change from fireworks to the lantern show while retaining its durable record identity and provenance."),
    StepInfo(number: 4, actionTitle: "Forget tonight's closure", story: "Forget tonight's closure: Click Expire. The temporary garden-path closure becomes ineligible for future recall. TTL distinguishes short-lived operational conditions from Ava's durable accessibility and entertainment preferences.", appImage: "step-04-app.png", sourcePath: servicePath, sourceMarker: "def expire(self)", sourceLines: 24, methodLabel: "expire()", codeCaption: "expire() updates the temporary closure's expiration metadata, then verifies that normal search excludes the now-expired operational memory.", databaseImage: "step-04-db.png", databaseObjects: ["MAGIC_PY_MEMORY"], databaseCaption: "The closure row remains available for evidence, but its expiration state makes it ineligible for future active-memory searches."),
    StepInfo(number: 5, actionTitle: "Learn under privacy controls", story: "Learn from rainy visits: Click Dream. Three synthetic, unscoped outcomes pass direct-identifier and scope checks before producing a pending rerouting guideline. The checks reduce risk but do not prove anonymity.", appImage: "step-05-app.png", sourcePath: servicePath, sourceMarker: "def dream(self)", sourceLines: 34, methodLabel: "dream()", codeCaption: "dream() requires three successful traces, rejects scoped or identifier-bearing evidence, and writes one pending guideline for human review.", databaseImage: "step-05-db.png", databaseObjects: ["MAGIC_PY_MEMORY"], databaseCaption: "Three synthetic trace rows remain separate. A pending guideline records the generalized procedure, evidence count, and required privacy review."),
    StepInfo(number: 6, actionTitle: "Authorize the lesson", story: "Govern the lesson: Click Approve. Ava acts as trip organizer. Oracle Deep Data Security authorizes the approval metadata update on the shared guideline; the participant role cannot perform it.", appImage: "step-06-app.png", sourcePath: securityPath, sourceMarker: "def approve_guideline", sourceLines: 30, methodLabel: "approve_guideline()", codeCaption: "approve_guideline() executes as Ava. MEMORY_TRIP_ORGANIZER permits the metadata update on eligible shared learning rows in Oracle AI Database.", databaseImage: "step-06-db.png", databaseObjects: ["MAGIC_PY_MEMORY", "MEMORY_TRIP_ORGANIZER"], databaseCaption: "The live DDS panel proves role-specific visibility. Approval changes the guideline to approved and records Ava as approver under database enforcement."),
    StepInfo(number: 7, actionTitle: "Prove Leo's boundary", story: "Help Leo safely: Click Next guest and run the identity comparison. Leo receives the approved procedure, zero Ava-private rows, and zero raw traces. A deliberate query for Ava's rows is blocked in the database.", appImage: "step-07-app.png", sourcePath: securityPath, sourceMarker: "def _rows_for", sourceLines: 30, methodLabel: "Deep Data Security query", codeCaption: "The app runs the same table query as each end user. Data roles and CREATE DATA GRANT predicates determine the rows before results leave Oracle AI Database.", databaseImage: "step-07-db.png", databaseObjects: ["MAGIC_PY_MEMORY", "MEMORY_TRIP_PARTICIPANT"], databaseCaption: "The live proof shows Leo with one approved shared guideline, zero Ava-private rows, zero raw traces, and zero rows from the cross-user probe."),
    StepInfo(number: 8, actionTitle: "Prepare Memory Quest", story: "Prepare Memory Quest: Click Reset quest. Ava's previous progress, points, badge, and reward history disappear, while the park map, paths, clues, and quest definitions remain reusable.", appImage: "step-08-app.png", sourcePath: parkPath, sourceMarker: "def reset(self)", sourceLines: 18, methodLabel: "reset()", codeCaption: "reset() deletes only visitor-specific quest results in dependency-safe order and preserves the reusable park world and knowledge.", databaseImage: "step-08-db.png", databaseObjects: ["AIM_PARK_PROGRESS", "AIM_PARK_GUEST_BADGES", "AIM_PARK_REWARD_AUDIT"], databaseCaption: "Progress, badge, and reward-audit tables become empty. Places, paths, quests, clues, graph relationships, and knowledge remain intact."),
    StepInfo(number: 9, actionTitle: "Plan Ava's accessible route", story: "Plan Ava's accessible route: Click Plan route. Property Graph finds connected accessible paths; Oracle Spatial measures physical distance. Ava avoids Summit Steps and travels through covered, step-free locations.", appImage: "step-09-app.png", sourcePath: parkPath, sourceMarker: "def plan(self)", sourceLines: 30, methodLabel: "plan()", codeCaption: "plan() filters graph edges for open accessible paths, computes the permitted route, and asks Oracle Spatial for straight-line distance.", databaseImage: "step-09-db.png", databaseObjects: ["AIM_PARK_PATHS", "AIM_PARK_PLACES", "AIM_PARK_GRAPH"], databaseCaption: "This is a read-only planning step. Graph topology supplies traversable paths and Spatial geometry supplies physical separation; no rows change."),
    StepInfo(number: 10, actionTitle: "Form the quest party", story: "Form the quest party: Click Start quest. Ava and Leo join Covered Constellations through consent-bounded party relationships. Transactional progress and audit rows start without combining their private memories.", appImage: "step-10-app.png", sourcePath: parkPath, sourceMarker: "def start(self)", sourceLines: 28, methodLabel: "start()", codeCaption: "start() validates the consented party and inserts quest progress plus the start audit event in one transaction.", databaseImage: "step-10-db.png", databaseObjects: ["AIM_PARK_PROGRESS", "AIM_PARK_REWARD_AUDIT"], databaseCaption: "A progress row starts at step zero with zero points, and a QUEST_STARTED audit row records the same committed transaction."),
    StepInfo(number: 11, actionTitle: "Find the breakfast clue", story: "Find the breakfast clue: Click Complete checkpoint. Ava reaches Quiet Café and earns 50 points. The database atomically updates progress and audit evidence, preventing reward inconsistencies.", appImage: "step-11-app.png", sourcePath: parkPath, sourceMarker: "def complete_next_step(self)", sourceLines: 34, methodLabel: "complete_next_step()", codeCaption: "complete_next_step() locks Ava's progress, validates the next ordered checkpoint, adds 50 points, and writes the checkpoint audit atomically.", databaseImage: "step-11-db.png", databaseObjects: ["AIM_PARK_PROGRESS", "AIM_PARK_REWARD_AUDIT"], databaseCaption: "Progress advances to Quiet Cafe with 50 points. The matching CHECKPOINT_COMPLETED audit row records the same 50-point delta."),
    StepInfo(number: 12, actionTitle: "Discover the atrium mosaic", story: "Discover the atrium mosaic: Click Complete checkpoint again. Ava follows the covered connector to the constellation artwork. Another transaction validates sequence, records the checkpoint, and awards 50 points.", appImage: "step-12-app.png", sourcePath: parkPath, sourceMarker: "def complete_next_step(self)", sourceLines: 34, methodLabel: "complete_next_step()", codeCaption: "The same transaction boundary validates checkpoint two, advances progress in sequence, adds 50 points, and appends audit evidence.", databaseImage: "step-12-db.png", databaseObjects: ["AIM_PARK_PROGRESS", "AIM_PARK_REWARD_AUDIT"], databaseCaption: "Progress advances to Covered Atrium with 100 total points. A second checkpoint audit row proves the ordered state transition."),
    StepInfo(number: 13, actionTitle: "Finish at Lantern Garden", story: "Finish at Lantern Garden: Click Complete checkpoint a third time. Ava decodes the founder symbol, completes the quest, reaches 400 points, and receives the Lantern Pathfinder badge transactionally.", appImage: "step-13-app.png", sourcePath: parkPath, sourceMarker: "def complete_next_step(self)", sourceLines: 38, methodLabel: "complete_next_step()", codeCaption: "The final checkpoint transaction completes progress, awards the completion bonus, issues the badge, and records the final audit event together.", databaseImage: "step-13-db.png", databaseObjects: ["AIM_PARK_PROGRESS", "AIM_PARK_GUEST_BADGES", "AIM_PARK_REWARD_AUDIT"], databaseCaption: "Ava's progress is completed at 400 points and the Lantern Pathfinder badge row is added without any partial reward state."),
    StepInfo(number: 14, actionTitle: "Expand the park story", story: "Expand the park story: Click Retrieve + expand. Vector Search finds quiet, rainy, accessible lore; Property Graph connects it to places, paths, and quests. GraphRAG explains relationships beyond similarity.", appImage: "step-14-app.png", sourcePath: parkPath, sourceMarker: "def graph_rag(self)", sourceLines: 34, methodLabel: "graph_rag()", codeCaption: "graph_rag() embeds the question, ranks grounded knowledge by vector distance, and expands each hit through explicit graph relationships.", databaseImage: nil, databaseObjects: ["AIM_PARK_KNOWLEDGE", "AIM_PARK_GRAPH", "AIM_PARK_PATHS", "AIM_PARK_QUEST_STEPS"], databaseCaption: "This is a read-only retrieval step. Native vectors find semantic matches, while graph tables supply connected places, paths, and quest context."),
    StepInfo(number: 15, actionTitle: "Remember through AR", story: "Remember through AR: Configure consent, start an AR session, then confirm 'remember this.' The simulated glasses show route overlays while Agent Memory stores Ava's observation with guest scope, provenance, and TTL.", appImage: "step-15-app.png", sourcePath: arPath, sourceMarker: "def remember(self)", sourceLines: 30, methodLabel: "start_session() + remember()", codeCaption: "start_session() fixes privacy choices for the session. remember() stores only the confirmed observation with guest scope, AR provenance, and bounded retention.", databaseImage: "step-15-db.png", databaseObjects: ["AIM_AR_SESSIONS", "MAGIC_PY_MEMORY", "AIM_AR_AUDIT"], databaseCaption: "The session and consent state are recorded, the observation becomes expirable guest memory, and the AR audit captures the authorized action."),
    StepInfo(number: 16, actionTitle: "Search opted-in media", story: "Search opted-in media: Enable recording consent, index the atrium caption, then search for the constellation artwork. Oracle AI Database filters identity, consent, and expiration before vector ranking.", appImage: "step-16-app.png", sourcePath: arPath, sourceMarker: "def search_media(self)", sourceLines: 34, methodLabel: "remember_media() + search_media()", codeCaption: "remember_media() indexes the consented description. search_media() filters guest, agent, thread, consent, and TTL before vector-distance ranking.", databaseImage: "step-16-db.png", databaseObjects: ["AIM_AR_MEDIA", "AIM_AR_AUDIT"], databaseCaption: "The media description, native vector, consent provenance, and expiration are stored. Scoped search returns one eligible constellation-mosaic result."),
]

var scenes: [Scene] = [
    Scene(visual: .title, eyebrow: "ORACLE AI DATABASE + AGENT MEMORY", title: "Memories Are\nthe Magic", subtitle: "", caption: "Follow Ava and Leo through durable memory, Deep Data Security, governed learning, accessible routing, transactional play, GraphRAG, and consented AR memory.", seconds: 8)
]

for step in steps {
    let number = String(format: "%02d", step.number)
    scenes.append(Scene(visual: .image(step.appImage, "LIVE APPLICATION"), eyebrow: "STEP \(number) | SCENARIO", title: step.actionTitle, subtitle: "Follow the highlighted application state and result.", caption: step.story, seconds: 8))
    scenes.append(Scene(visual: .code(step.sourcePath, step.sourceMarker, step.sourceLines, step.methodLabel), eyebrow: "STEP \(number) | SOURCE METHOD", title: "Code called", subtitle: step.methodLabel, caption: step.codeCaption, seconds: 7))
    if let image = step.databaseImage {
        scenes.append(Scene(visual: .image(image, "DATABASE CONTENT / CHANGE"), eyebrow: "STEP \(number) | DATABASE EVIDENCE", title: "Verify database state", subtitle: step.databaseObjects.joined(separator: " · "), caption: step.databaseCaption, seconds: 7))
    } else {
        scenes.append(Scene(visual: .databaseSummary(step.databaseObjects, "READ-ONLY"), eyebrow: "STEP \(number) | DATABASE EVIDENCE", title: "Verify database reads", subtitle: "No stored rows change in this step.", caption: step.databaseCaption, seconds: 7))
    }
}

scenes.append(Scene(visual: .recap, eyebrow: "GOVERNED CONTINUAL LEARNING", title: "Retain · Recall\nReuse · Refine", subtitle: "One connected guest journey, with every memory and database effect inspectable.", caption: "The experience combines scoped memory, Deep Data Security, human governance, graph, Spatial, vectors, transactions, gamification, and privacy-first AR without retraining model weights.", seconds: 9))

let fm = FileManager.default
try fm.createDirectory(at: build, withIntermediateDirectories: true)
for file in [output, poster, srt, vtt] where fm.fileExists(atPath: file.path) {
    try fm.removeItem(at: file)
}

let red = NSColor(calibratedRed: 1.0, green: 0.31, blue: 0.47, alpha: 1)
let gold = NSColor(calibratedRed: 1.0, green: 0.82, blue: 0.40, alpha: 1)
let teal = NSColor(calibratedRed: 0.0, green: 0.90, blue: 1.0, alpha: 1)
let violet = NSColor(calibratedRed: 0.48, green: 0.19, blue: 1.0, alpha: 1)
let ink = NSColor(calibratedRed: 0.008, green: 0.027, blue: 0.055, alpha: 1)
let muted = NSColor(calibratedRed: 0.54, green: 0.71, blue: 0.76, alpha: 1)
let panel = NSColor(calibratedRed: 0.02, green: 0.075, blue: 0.12, alpha: 1)

func rounded(_ rect: NSRect, radius: CGFloat, color: NSColor) {
    color.setFill()
    NSBezierPath(roundedRect: rect, xRadius: radius, yRadius: radius).fill()
}

func outlined(_ rect: NSRect, radius: CGFloat, color: NSColor, width: CGFloat = 1.5) {
    color.setStroke()
    let path = NSBezierPath(roundedRect: rect, xRadius: radius, yRadius: radius)
    path.lineWidth = width
    path.stroke()
}

func drawCircuitGrid() {
    let grid = teal.withAlphaComponent(0.055)
    grid.setStroke()
    for x in stride(from: CGFloat(0), through: CGFloat(width), by: 72) {
        let path = NSBezierPath()
        path.move(to: NSPoint(x: x, y: 0))
        path.line(to: NSPoint(x: x, y: CGFloat(height)))
        path.lineWidth = 1
        path.stroke()
    }
    for y in stride(from: CGFloat(0), through: CGFloat(height), by: 72) {
        let path = NSBezierPath()
        path.move(to: NSPoint(x: 0, y: y))
        path.line(to: NSPoint(x: CGFloat(width), y: y))
        path.lineWidth = 1
        path.stroke()
    }
    violet.withAlphaComponent(0.08).setFill()
    NSBezierPath(ovalIn: NSRect(x: 1460, y: -160, width: 620, height: 620)).fill()
}

func drawText(_ value: String, _ rect: NSRect, font: NSFont, color: NSColor, alignment: NSTextAlignment = .left, spacing: CGFloat = 4) {
    let paragraph = NSMutableParagraphStyle()
    paragraph.alignment = alignment
    paragraph.lineSpacing = spacing
    paragraph.lineBreakMode = .byWordWrapping
    value.draw(with: rect, options: [.usesLineFragmentOrigin, .usesFontLeading], attributes: [.font: font, .foregroundColor: color, .paragraphStyle: paragraph])
}

func badge(_ label: String, x: CGFloat, y: CGFloat, color: NSColor) {
    let size = (label as NSString).size(withAttributes: [.font: NSFont.boldSystemFont(ofSize: 21)])
    rounded(NSRect(x: x, y: y, width: size.width + 32, height: 42), radius: 21, color: color.withAlphaComponent(0.22))
    drawText(label, NSRect(x: x + 16, y: y + 8, width: size.width, height: 28), font: .boldSystemFont(ofSize: 21), color: color)
}

func sourceExcerpt(path: URL, marker: String, lines: Int) -> String {
    let text = (try? String(contentsOf: path, encoding: .utf8)) ?? "Source file unavailable"
    let sourceLines = text.components(separatedBy: .newlines)
    let start = sourceLines.firstIndex(where: { $0.contains(marker) }) ?? 0
    let lower = max(0, start - 2)
    let upper = min(sourceLines.count, lower + lines)
    return sourceLines[lower..<upper].enumerated().map { String(format: "%3d  %@", lower + $0.offset + 1, $0.element) }.joined(separator: "\n")
}

func drawCode(path: String, marker: String, lines: Int, label: String) {
    let rect = NSRect(x: 650, y: 140, width: 1190, height: 680)
    rounded(rect, radius: 22, color: NSColor(calibratedWhite: 0.095, alpha: 1))
    outlined(rect, radius: 22, color: teal.withAlphaComponent(0.45))
    rounded(NSRect(x: rect.minX, y: rect.minY, width: rect.width, height: 60), radius: 22, color: NSColor(calibratedWhite: 0.16, alpha: 1))
    for (index, color) in [NSColor.systemRed, .systemYellow, .systemGreen].enumerated() {
        color.setFill()
        NSBezierPath(ovalIn: NSRect(x: rect.minX + 24 + CGFloat(index) * 31, y: rect.minY + 21, width: 16, height: 16)).fill()
    }
    drawText("AGENT SERVICE  /  \(label)", NSRect(x: rect.minX + 125, y: rect.minY + 15, width: rect.width - 150, height: 34), font: .monospacedSystemFont(ofSize: 20, weight: .semibold), color: muted)
    drawText(sourceExcerpt(path: memoryRoot.appendingPathComponent(path), marker: marker, lines: lines), NSRect(x: rect.minX + 30, y: rect.minY + 82, width: rect.width - 60, height: rect.height - 104), font: .monospacedSystemFont(ofSize: lines > 31 ? 16 : 18, weight: .regular), color: NSColor(calibratedRed: 0.84, green: 0.89, blue: 0.91, alpha: 1), spacing: lines > 31 ? 3 : 5)
}

func drawImage(filename: String, label: String) {
    let path = captureRoot.appendingPathComponent(filename)
    guard let image = NSImage(contentsOf: path) else {
        drawText("Verified capture unavailable", NSRect(x: 650, y: 360, width: 1190, height: 80), font: .boldSystemFont(ofSize: 34), color: .white, alignment: .center)
        return
    }
    let frame = NSRect(x: 635, y: 135, width: 1215, height: 690)
    rounded(frame, radius: 22, color: panel)
    outlined(frame, radius: 22, color: teal.withAlphaComponent(0.42))
    badge(label, x: frame.minX + 24, y: frame.minY + 22, color: label.contains("DATABASE") ? gold : teal)
    let content = NSRect(x: frame.minX + 14, y: frame.minY + 76, width: frame.width - 28, height: frame.height - 90)
    if label == "LIVE APPLICATION", let full = image.cgImage(forProposedRect: nil, context: nil, hints: nil) {
        let overlap = Int(Double(full.height) * 0.08)
        let half = full.height / 2
        let crops = [
            full.cropping(to: CGRect(x: 0, y: 0, width: full.width, height: half + overlap)),
            full.cropping(to: CGRect(x: 0, y: max(0, half - overlap), width: full.width, height: full.height - half + overlap))
        ]
        let labels = ["INTERACTION + VISUAL", "RESULT + EVIDENCE"]
        for index in 0..<2 {
            let pane = NSRect(x: content.minX + CGFloat(index) * (content.width / 2 + 8), y: content.minY, width: content.width / 2 - 8, height: content.height)
            rounded(pane, radius: 16, color: NSColor(calibratedWhite: 0.08, alpha: 1))
            outlined(pane, radius: 16, color: teal.withAlphaComponent(0.22))
            drawText(labels[index], NSRect(x: pane.minX + 14, y: pane.minY + 12, width: pane.width - 28, height: 28), font: .boldSystemFont(ofSize: 17), color: muted, alignment: .center)
            if let crop = crops[index] {
                let piece = NSImage(cgImage: crop, size: NSSize(width: crop.width, height: crop.height))
                let targetArea = NSRect(x: pane.minX + 10, y: pane.minY + 48, width: pane.width - 20, height: pane.height - 58)
                let scale = min(targetArea.width / piece.size.width, targetArea.height / piece.size.height)
                let size = NSSize(width: piece.size.width * scale, height: piece.size.height * scale)
                let target = NSRect(x: targetArea.midX - size.width / 2, y: targetArea.midY - size.height / 2, width: size.width, height: size.height)
                piece.draw(in: target, from: NSRect(origin: .zero, size: piece.size), operation: .sourceOver, fraction: 1, respectFlipped: true, hints: [.interpolation: NSImageInterpolation.high])
            }
        }
    } else {
        let scale = min(content.width / image.size.width, content.height / image.size.height)
        let size = NSSize(width: image.size.width * scale, height: image.size.height * scale)
        let target = NSRect(x: content.midX - size.width / 2, y: content.midY - size.height / 2, width: size.width, height: size.height)
        image.draw(in: target, from: NSRect(origin: .zero, size: image.size), operation: .sourceOver, fraction: 1, respectFlipped: true, hints: [.interpolation: NSImageInterpolation.high])
    }
}

func drawDatabaseSummary(_ objects: [String], _ state: String) {
    rounded(NSRect(x: 670, y: 170, width: 1120, height: 590), radius: 28, color: panel)
    outlined(NSRect(x: 670, y: 170, width: 1120, height: 590), radius: 28, color: teal.withAlphaComponent(0.45))
    badge(state, x: 715, y: 215, color: teal)
    drawText("Database objects consulted", NSRect(x: 715, y: 300, width: 1025, height: 60), font: .boldSystemFont(ofSize: 38), color: .white)
    for (index, object) in objects.enumerated() {
        let y = 400 + CGFloat(index) * 72
        rounded(NSRect(x: 715, y: y, width: 1025, height: 54), radius: 12, color: NSColor(calibratedWhite: 0.18, alpha: 1))
        drawText(object, NSRect(x: 740, y: y + 12, width: 970, height: 32), font: .monospacedSystemFont(ofSize: 23, weight: .semibold), color: gold)
    }
}

func drawTitleVisual() {
    rounded(NSRect(x: 675, y: 180, width: 1110, height: 600), radius: 34, color: panel)
    outlined(NSRect(x: 675, y: 180, width: 1110, height: 600), radius: 34, color: teal.withAlphaComponent(0.5), width: 2)
    let items = [("MEMORY", teal), ("GRAPH + SPATIAL", gold), ("TRANSACTIONS", red), ("CONSENTED AR", teal)]
    for (index, item) in items.enumerated() {
        let x = 735 + CGFloat(index % 2) * 510
        let y = 260 + CGFloat(index / 2) * 200
        rounded(NSRect(x: x, y: y, width: 450, height: 145), radius: 24, color: item.1.withAlphaComponent(0.25))
        drawText(item.0, NSRect(x: x + 25, y: y + 48, width: 400, height: 50), font: .boldSystemFont(ofSize: 29), color: .white, alignment: .center)
    }
    drawText("ORACLE AI DATABASE", NSRect(x: 810, y: 675, width: 840, height: 50), font: .boldSystemFont(ofSize: 33), color: gold, alignment: .center)
}

func drawRecap() {
    for (index, label) in ["RETAIN", "RECALL", "REUSE", "REFINE"].enumerated() {
        let x = 640 + CGFloat(index) * 290
        rounded(NSRect(x: x, y: 305, width: 240, height: 145), radius: 72, color: index % 2 == 0 ? red : teal)
        drawText(label, NSRect(x: x + 20, y: 355, width: 200, height: 45), font: .boldSystemFont(ofSize: 26), color: .white, alignment: .center)
    }
    drawText("SCOPED · VERSIONED · EXPIRABLE · AUDITABLE", NSRect(x: 590, y: 535, width: 1280, height: 60), font: .boldSystemFont(ofSize: 26), color: gold, alignment: .center)
    drawText("GRAPH · SPATIAL · VECTOR · TRANSACTIONS · AR", NSRect(x: 560, y: 610, width: 1340, height: 60), font: .boldSystemFont(ofSize: 26), color: teal, alignment: .center)
}

func imageFor(index: Int) -> NSImage {
    let scene = scenes[index]
    let image = NSImage(size: NSSize(width: width, height: height))
    image.lockFocusFlipped(true)
    ink.setFill()
    NSBezierPath(rect: NSRect(x: 0, y: 0, width: width, height: height)).fill()
    drawCircuitGrid()
    rounded(NSRect(x: 0, y: 0, width: 18, height: height), radius: 0, color: teal)
    drawText(scene.eyebrow, NSRect(x: 70, y: 52, width: 1160, height: 34), font: .boldSystemFont(ofSize: 21), color: red)
    drawText(scene.title, NSRect(x: 70, y: 110, width: 520, height: 235), font: .boldSystemFont(ofSize: index == 0 ? 70 : 48), color: .white, spacing: 3)
    drawText(scene.subtitle, NSRect(x: 75, y: 370, width: 500, height: 235), font: .systemFont(ofSize: 25), color: muted, spacing: 7)

    switch scene.visual {
    case .title:
        drawTitleVisual()
    case .image(let filename, let label):
        drawImage(filename: filename, label: label)
    case .code(let path, let marker, let lines, let label):
        drawCode(path: path, marker: marker, lines: lines, label: label)
    case .databaseSummary(let objects, let state):
        drawDatabaseSummary(objects, state)
    case .recap:
        drawRecap()
    }

    rounded(NSRect(x: 125, y: 855, width: 1670, height: 170), radius: 22, color: NSColor(calibratedWhite: 0.02, alpha: 0.95))
    outlined(NSRect(x: 125, y: 855, width: 1670, height: 170), radius: 22, color: teal.withAlphaComponent(0.4))
    drawText(scene.caption, NSRect(x: 165, y: 878, width: 1590, height: 128), font: .boldSystemFont(ofSize: scene.caption.count > 220 ? 23 : 25), color: .white, alignment: .center, spacing: 6)
    drawText(String(format: "%02d / %02d", index + 1, scenes.count), NSRect(x: 1690, y: 52, width: 150, height: 28), font: .monospacedSystemFont(ofSize: 18, weight: .medium), color: muted, alignment: .right)
    image.unlockFocus()
    return image
}

func cgImage(_ image: NSImage) -> CGImage {
    var rect = NSRect(origin: .zero, size: image.size)
    return image.cgImage(forProposedRect: &rect, context: nil, hints: nil)!
}

func timestamp(_ seconds: Double, separator: String) -> String {
    let milliseconds = Int((seconds * 1000).rounded())
    return String(format: "%02d:%02d:%02d%@%03d", milliseconds / 3_600_000, (milliseconds / 60_000) % 60, (milliseconds / 1000) % 60, separator, milliseconds % 1000)
}

func captionLines(_ text: String, limit: Int = 62) -> String {
    var lines: [String] = []
    var current = ""
    for word in text.split(separator: " ").map(String.init) {
        if !current.isEmpty && current.count + word.count + 1 > limit {
            lines.append(current)
            current = word
        } else {
            current += (current.isEmpty ? "" : " ") + word
        }
    }
    if !current.isEmpty { lines.append(current) }
    return lines.joined(separator: "\n")
}

var cursor = 0.0
var srtText = ""
var vttText = "WEBVTT\n\n"
for (index, scene) in scenes.enumerated() {
    let end = cursor + scene.seconds
    let caption = captionLines(scene.caption)
    srtText += "\(index + 1)\n\(timestamp(cursor, separator: ",")) --> \(timestamp(end, separator: ","))\n\(caption)\n\n"
    vttText += "\(timestamp(cursor, separator: ".")) --> \(timestamp(end, separator: "."))\n\(caption)\n\n"
    cursor = end
}
try (srtText.trimmingCharacters(in: .newlines) + "\n").write(to: srt, atomically: true, encoding: .utf8)
try (vttText.trimmingCharacters(in: .newlines) + "\n").write(to: vtt, atomically: true, encoding: .utf8)

let posterImage = imageFor(index: 0)
let posterRep = NSBitmapImageRep(cgImage: cgImage(posterImage))
try posterRep.representation(using: .png, properties: [:])!.write(to: poster)

let writer = try AVAssetWriter(outputURL: output, fileType: .mp4)
let settings: [String: Any] = [AVVideoCodecKey: AVVideoCodecType.h264, AVVideoWidthKey: width, AVVideoHeightKey: height, AVVideoCompressionPropertiesKey: [AVVideoAverageBitRateKey: 7_000_000, AVVideoProfileLevelKey: AVVideoProfileLevelH264HighAutoLevel]]
let input = AVAssetWriterInput(mediaType: .video, outputSettings: settings)
let attributes: [String: Any] = [kCVPixelBufferPixelFormatTypeKey as String: kCVPixelFormatType_32ARGB, kCVPixelBufferWidthKey as String: width, kCVPixelBufferHeightKey as String: height]
let adaptor = AVAssetWriterInputPixelBufferAdaptor(assetWriterInput: input, sourcePixelBufferAttributes: attributes)
guard writer.canAdd(input) else { fatalError("Cannot add video input") }
writer.add(input)
writer.startWriting()
writer.startSession(atSourceTime: .zero)

let colorSpace = CGColorSpaceCreateDeviceRGB()
let rendered = scenes.indices.map { cgImage(imageFor(index: $0)) }
let enhancementSources: [Int: CGImage] = [
    1: "accessible-covered-path.png",
    2: "accessible-covered-path.png",
    9: "accessible-covered-path.png",
    10: "transactional-quest-reward.png",
    11: "transactional-quest-reward.png",
    12: "transactional-quest-reward.png",
    13: "transactional-quest-reward.png",
    15: "consented-ar-route.png",
    16: "consented-ar-route.png"
].compactMapValues { filename in
    NSImage(contentsOf: enhancedAssets.appendingPathComponent(filename)).flatMap { cgImage($0) }
}

func appStep(for sceneIndex: Int) -> Int? {
    guard sceneIndex > 0, (sceneIndex - 1) % 3 == 0 else { return nil }
    return ((sceneIndex - 1) / 3) + 1
}

func drawEnhancement(_ image: CGImage, in context: CGContext, progress: Double) {
    let box = CGRect(x: 1325, y: 418, width: 500, height: 281)
    let pulse = CGFloat(0.5 + 0.5 * sin(progress * .pi * 2))
    context.saveGState()
    context.setShadow(offset: .zero, blur: 28, color: NSColor(calibratedRed: 0, green: 0.9, blue: 1, alpha: 0.5).cgColor)
    context.setFillColor(NSColor(calibratedWhite: 0.01, alpha: 0.94).cgColor)
    context.fill(box.insetBy(dx: -10, dy: -10))
    context.restoreGState()

    let zoom = CGFloat(1.03 + 0.035 * sin(progress * .pi))
    let drawRect = box.insetBy(dx: -box.width * (zoom - 1) / 2, dy: -box.height * (zoom - 1) / 2)
    context.saveGState()
    context.clip(to: box)
    context.setAlpha(0.88)
    context.draw(image, in: drawRect)
    context.restoreGState()
    context.setStrokeColor(NSColor(calibratedRed: 0, green: 0.9, blue: 1, alpha: 0.65 + 0.25 * pulse).cgColor)
    context.setLineWidth(5)
    context.stroke(box)
}

let totalFrames = Int(ceil(cursor * Double(fps)))
var sceneIndex = 0
var sceneEnd = scenes[0].seconds
var sceneStart = 0.0
for frame in 0..<totalFrames {
    let seconds = Double(frame) / Double(fps)
    while seconds >= sceneEnd && sceneIndex < scenes.count - 1 {
        sceneStart = sceneEnd
        sceneIndex += 1
        sceneEnd += scenes[sceneIndex].seconds
    }
    while !input.isReadyForMoreMediaData { Thread.sleep(forTimeInterval: 0.002) }
    var buffer: CVPixelBuffer?
    CVPixelBufferCreate(kCFAllocatorDefault, width, height, kCVPixelFormatType_32ARGB, attributes as CFDictionary, &buffer)
    guard let pixelBuffer = buffer else { fatalError("Cannot allocate pixel buffer") }
    CVPixelBufferLockBaseAddress(pixelBuffer, [])
    let context = CGContext(data: CVPixelBufferGetBaseAddress(pixelBuffer), width: width, height: height, bitsPerComponent: 8, bytesPerRow: CVPixelBufferGetBytesPerRow(pixelBuffer), space: colorSpace, bitmapInfo: CGImageAlphaInfo.noneSkipFirst.rawValue)!
    context.draw(rendered[sceneIndex], in: CGRect(x: 0, y: 0, width: width, height: height))
    if let step = appStep(for: sceneIndex), let enhancement = enhancementSources[step] {
        let progress = max(0, min(1, (seconds - sceneStart) / scenes[sceneIndex].seconds))
        drawEnhancement(enhancement, in: context, progress: progress)
    }
    CVPixelBufferUnlockBaseAddress(pixelBuffer, [])
    adaptor.append(pixelBuffer, withPresentationTime: CMTime(value: Int64(frame), timescale: fps))
}
input.markAsFinished()
let semaphore = DispatchSemaphore(value: 0)
writer.finishWriting { semaphore.signal() }
semaphore.wait()
guard writer.status == .completed else { throw writer.error ?? NSError(domain: "MemoryVideo", code: 1) }

print(String(format: "Created %@ (%.1f seconds, %d scenes)", output.lastPathComponent, cursor, scenes.count))
print("Created poster, SRT, and WebVTT caption assets")
