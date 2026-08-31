#!/usr/bin/env swift

import AppKit
import AVFoundation
import CoreVideo
import Foundation
import ImageIO
import UniformTypeIdentifiers

let width = 1920
let height = 1080
let fps: Int32 = 24
let seconds = 8
let frameCount = Int(fps) * seconds
let root = URL(fileURLWithPath: FileManager.default.currentDirectoryPath)
let presentation = root.appendingPathComponent("presentation")
let sourceURL = presentation.appendingPathComponent("guest-journey-command-center-source.png")
let mp4URL = presentation.appendingPathComponent("guest-journey-command-center-loop.mp4")
let gifURL = presentation.appendingPathComponent("guest-journey-command-center-loop.gif")

guard let sourceImage = NSImage(contentsOf: sourceURL),
      let source = sourceImage.cgImage(forProposedRect: nil, context: nil, hints: nil) else {
    fatalError("Missing presentation source image")
}

for url in [mp4URL, gifURL] where FileManager.default.fileExists(atPath: url.path) {
    try FileManager.default.removeItem(at: url)
}

let colorSpace = CGColorSpaceCreateDeviceRGB()

func renderFrame(_ frame: Int) -> CGImage {
    let progress = Double(frame) / Double(frameCount)
    let phase = progress * .pi * 2
    let buffer = CGContext(
        data: nil,
        width: width,
        height: height,
        bitsPerComponent: 8,
        bytesPerRow: width * 4,
        space: colorSpace,
        bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue
    )!

    let scale = CGFloat(1.03 + 0.018 * sin(phase))
    let imageAspect = CGFloat(source.width) / CGFloat(source.height)
    let canvasAspect = CGFloat(width) / CGFloat(height)
    var base = CGRect(x: 0, y: 0, width: width, height: height)
    if imageAspect > canvasAspect {
        base.size.width = CGFloat(height) * imageAspect
        base.origin.x = (CGFloat(width) - base.width) / 2
    } else {
        base.size.height = CGFloat(width) / imageAspect
        base.origin.y = (CGFloat(height) - base.height) / 2
    }
    let driftX = CGFloat(10 * sin(phase))
    let driftY = CGFloat(6 * cos(phase))
    let target = base.insetBy(dx: -base.width * (scale - 1) / 2, dy: -base.height * (scale - 1) / 2)
        .offsetBy(dx: driftX, dy: driftY)
    buffer.draw(source, in: target)

    let center = CGPoint(x: 985, y: 560)
    for ring in 0..<4 {
        let offset = (progress + Double(ring) / 4).truncatingRemainder(dividingBy: 1)
        let radius = CGFloat(95 + offset * 285)
        let alpha = CGFloat((1 - offset) * 0.28)
        buffer.setStrokeColor(NSColor(calibratedRed: 0, green: 0.86, blue: 1, alpha: alpha).cgColor)
        buffer.setLineWidth(3)
        buffer.strokeEllipse(in: CGRect(x: center.x - radius, y: center.y - radius * 0.24, width: radius * 2, height: radius * 0.48))
    }

    for particle in 0..<28 {
        let seed = Double(particle) * 0.61803398875
        let orbit = phase + seed * .pi * 2
        let radius = CGFloat(120 + (particle % 7) * 34)
        let x = center.x + cos(orbit) * radius
        let y = center.y + sin(orbit * 0.72) * radius * 0.35 + CGFloat((particle % 5) * 9)
        let size = CGFloat(2 + particle % 4)
        buffer.setFillColor(NSColor(calibratedRed: 0.15, green: 0.86, blue: 1, alpha: 0.35).cgColor)
        buffer.fillEllipse(in: CGRect(x: x, y: y, width: size, height: size))
    }

    return buffer.makeImage()!
}

let writer = try AVAssetWriter(outputURL: mp4URL, fileType: .mp4)
let settings: [String: Any] = [
    AVVideoCodecKey: AVVideoCodecType.h264,
    AVVideoWidthKey: width,
    AVVideoHeightKey: height,
    AVVideoCompressionPropertiesKey: [
        AVVideoAverageBitRateKey: 5_500_000,
        AVVideoProfileLevelKey: AVVideoProfileLevelH264HighAutoLevel
    ]
]
let input = AVAssetWriterInput(mediaType: .video, outputSettings: settings)
let attributes: [String: Any] = [
    kCVPixelBufferPixelFormatTypeKey as String: kCVPixelFormatType_32ARGB,
    kCVPixelBufferWidthKey as String: width,
    kCVPixelBufferHeightKey as String: height
]
let adaptor = AVAssetWriterInputPixelBufferAdaptor(assetWriterInput: input, sourcePixelBufferAttributes: attributes)
writer.add(input)
writer.startWriting()
writer.startSession(atSourceTime: .zero)

let gifFrameCount = frameCount / 2
let gif = CGImageDestinationCreateWithURL(gifURL as CFURL, UTType.gif.identifier as CFString, gifFrameCount, nil)!
CGImageDestinationSetProperties(gif, [kCGImagePropertyGIFDictionary: [kCGImagePropertyGIFLoopCount: 0]] as CFDictionary)
let gifFrameProperties = [kCGImagePropertyGIFDictionary: [kCGImagePropertyGIFDelayTime: 2.0 / Double(fps)]] as CFDictionary

func gifFrame(_ source: CGImage) -> CGImage {
    let targetWidth = 960
    let targetHeight = 540
    let context = CGContext(
        data: nil,
        width: targetWidth,
        height: targetHeight,
        bitsPerComponent: 8,
        bytesPerRow: targetWidth * 4,
        space: colorSpace,
        bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue
    )!
    context.interpolationQuality = .high
    context.draw(source, in: CGRect(x: 0, y: 0, width: targetWidth, height: targetHeight))
    return context.makeImage()!
}

for frame in 0..<frameCount {
    let rendered = renderFrame(frame)
    if frame % 2 == 0 {
        CGImageDestinationAddImage(gif, gifFrame(rendered), gifFrameProperties)
    }
    while !input.isReadyForMoreMediaData { Thread.sleep(forTimeInterval: 0.002) }
    var pixelBuffer: CVPixelBuffer?
    CVPixelBufferCreate(kCFAllocatorDefault, width, height, kCVPixelFormatType_32ARGB, attributes as CFDictionary, &pixelBuffer)
    let buffer = pixelBuffer!
    CVPixelBufferLockBaseAddress(buffer, [])
    let context = CGContext(
        data: CVPixelBufferGetBaseAddress(buffer),
        width: width,
        height: height,
        bitsPerComponent: 8,
        bytesPerRow: CVPixelBufferGetBytesPerRow(buffer),
        space: colorSpace,
        bitmapInfo: CGImageAlphaInfo.noneSkipFirst.rawValue
    )!
    context.draw(rendered, in: CGRect(x: 0, y: 0, width: width, height: height))
    CVPixelBufferUnlockBaseAddress(buffer, [])
    adaptor.append(buffer, withPresentationTime: CMTime(value: Int64(frame), timescale: fps))
}

input.markAsFinished()
let finished = DispatchSemaphore(value: 0)
writer.finishWriting { finished.signal() }
finished.wait()
guard writer.status == .completed else { throw writer.error ?? NSError(domain: "PresentationLoop", code: 1) }
guard CGImageDestinationFinalize(gif) else { fatalError("Could not finalize GIF") }

print("Created PowerPoint loop: \(mp4URL.lastPathComponent)")
print("Created auto-looping fallback: \(gifURL.lastPathComponent)")
