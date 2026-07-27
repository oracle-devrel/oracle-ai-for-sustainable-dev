#!/usr/bin/env swift

import AppKit
import AVFoundation
import Foundation

let root = URL(fileURLWithPath: FileManager.default.currentDirectoryPath)
let videoURL = root.appendingPathComponent("interactive-ai-walkthrough.mp4")
let outputURL = root.appendingPathComponent(".build/contact-sheet.png")
let asset = AVURLAsset(url: videoURL)
let duration = CMTimeGetSeconds(asset.duration)
let videoTracks = asset.tracks(withMediaType: .video)
let audioTracks = asset.tracks(withMediaType: .audio)

guard let videoTrack = videoTracks.first, audioTracks.isEmpty else {
    fputs("Expected one silent video track and no audio track.\n", stderr)
    exit(1)
}

let transformedSize = videoTrack.naturalSize.applying(videoTrack.preferredTransform)
let pixelWidth = Int(abs(transformedSize.width))
let pixelHeight = Int(abs(transformedSize.height))
guard pixelWidth == 1920, pixelHeight == 1080, duration > 60 else {
    fputs("Unexpected media dimensions or duration.\n", stderr)
    exit(1)
}

let generator = AVAssetImageGenerator(asset: asset)
generator.appliesPreferredTrackTransform = true
generator.maximumSize = NSSize(width: 640, height: 360)
let fractions = [0.03, 0.24, 0.45, 0.63, 0.80, 0.94]
let sheet = NSImage(size: NSSize(width: 1920, height: 720))
sheet.lockFocusFlipped(true)
NSColor.black.setFill()
NSBezierPath(rect: NSRect(x: 0, y: 0, width: 1920, height: 720)).fill()
for (index, fraction) in fractions.enumerated() {
    let time = CMTime(seconds: duration * fraction, preferredTimescale: 600)
    let image = try generator.copyCGImage(at: time, actualTime: nil)
    NSImage(cgImage: image, size: NSSize(width: 640, height: 360)).draw(
        in: NSRect(x: CGFloat(index % 3) * 640, y: CGFloat(index / 3) * 360, width: 640, height: 360))
}
sheet.unlockFocus()
var rect = NSRect(origin: .zero, size: sheet.size)
let sheetCG = sheet.cgImage(forProposedRect: &rect, context: nil, hints: nil)!
let representation = NSBitmapImageRep(cgImage: sheetCG)
try representation.representation(using: .png, properties: [:])!.write(to: outputURL)

print(String(format: "Verified silent %dx%d, %.1f seconds, %d video track, %d audio tracks", pixelWidth, pixelHeight, duration, videoTracks.count, audioTracks.count))
print("Contact sheet: \(outputURL.path)")
