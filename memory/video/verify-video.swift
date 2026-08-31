#!/usr/bin/env swift

import AppKit
import AVFoundation
import Foundation

let root = URL(fileURLWithPath: FileManager.default.currentDirectoryPath)
let videoName = CommandLine.arguments.dropFirst().first ?? "memory-agent-walkthrough.mp4"
let video = root.appendingPathComponent(videoName)
let sheetName = video.deletingPathExtension().lastPathComponent + "-contact-sheet.png"
let sheetURL = root.appendingPathComponent(".build").appendingPathComponent(sheetName)
let asset = AVURLAsset(url: video)
let duration = CMTimeGetSeconds(asset.duration)
let videoTracks = asset.tracks(withMediaType: .video)
let audioTracks = asset.tracks(withMediaType: .audio)

guard let track = videoTracks.first, audioTracks.isEmpty else {
    fputs("Expected one silent video track and no audio track.\n", stderr)
    exit(1)
}

let size = track.naturalSize.applying(track.preferredTransform)
let pixelWidth = Int(abs(size.width))
let pixelHeight = Int(abs(size.height))
let minimumDuration = videoName.contains("loop") ? 7.5 : 80
guard pixelWidth == 1920, pixelHeight == 1080, duration >= minimumDuration else {
    fputs("Unexpected media dimensions or duration.\n", stderr)
    exit(1)
}

let reader = try AVAssetReader(asset: asset)
let readerOutput = AVAssetReaderTrackOutput(track: track, outputSettings: nil)
guard reader.canAdd(readerOutput) else {
    fputs("Unable to read the complete video track.\n", stderr)
    exit(1)
}
reader.add(readerOutput)
reader.startReading()
var decodedSamples = 0
while readerOutput.copyNextSampleBuffer() != nil {
    decodedSamples += 1
}
guard reader.status == .completed, decodedSamples >= Int(duration * 20) else {
    fputs("The complete video track did not decode successfully.\n", stderr)
    exit(1)
}

let generator = AVAssetImageGenerator(asset: asset)
generator.appliesPreferredTrackTransform = true
generator.maximumSize = NSSize(width: 640, height: 360)
let fractions = [0.03, 0.18, 0.34, 0.50, 0.68, 0.84, 0.96]
let sheet = NSImage(size: NSSize(width: 1920, height: 1080))
sheet.lockFocusFlipped(true)
NSColor.black.setFill()
NSBezierPath(rect: NSRect(x: 0, y: 0, width: 1920, height: 1080)).fill()
for (index, fraction) in fractions.enumerated() {
    let time = CMTime(seconds: duration * fraction, preferredTimescale: 600)
    let frame = try generator.copyCGImage(at: time, actualTime: nil)
    let row = index / 3
    let column = index % 3
    NSImage(cgImage: frame, size: NSSize(width: 640, height: 360)).draw(
        in: NSRect(x: CGFloat(column) * 640, y: CGFloat(row) * 360, width: 640, height: 360)
    )
}
sheet.unlockFocus()
var rect = NSRect(origin: .zero, size: sheet.size)
let cg = sheet.cgImage(forProposedRect: &rect, context: nil, hints: nil)!
let rep = NSBitmapImageRep(cgImage: cg)
try rep.representation(using: .png, properties: [:])!.write(to: sheetURL)

print(String(format: "Verified silent %dx%d, %.1f seconds, %d video track, %d audio tracks", pixelWidth, pixelHeight, duration, videoTracks.count, audioTracks.count))
print("Decoded complete track: \(decodedSamples) video samples")
print("Contact sheet: \(sheetURL.path)")
