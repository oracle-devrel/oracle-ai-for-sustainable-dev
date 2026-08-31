# Memory blog walkthrough video

This directory builds the latest silent, subtitle-led 1920x1080 MP4 for the
memory article. A dark neon circuit visual system matches the browser app and
blog while preserving accessible caption contrast. Its 16 numbered steps follow
the complete Ava and Leo theme-park story: governed memory, Oracle Deep Data
Security, accessible route planning, consent-bounded quest play, GraphRAG, and
privacy-first AR memory.

Every step contains three verified views:

1. the relevant live application state;
2. the source method called by that action; and
3. the database content or change, including an explicit read-only explanation
   when the step does not write rows.

```bash
cd memory/video
./build-video.swift
./verify-video.swift
```

Generated publication assets:

- `memory-agent-walkthrough.mp4`
- `memory-agent-walkthrough-poster.png`
- `memory-agent-walkthrough.srt`
- `memory-agent-walkthrough.vtt`

An optional enhanced edition adds small cinematic picture-in-picture windows
to the accessibility, quest, and AR application scenes without covering the
source or database evidence:

```bash
cd memory/video
./build-video-enhanced.swift
```

Enhanced assets:

- `memory-agent-walkthrough-enhanced.mp4`
- `memory-agent-walkthrough-enhanced-poster.png`
- `memory-agent-walkthrough-enhanced.srt`
- `memory-agent-walkthrough-enhanced.vtt`

The generated inserts live under `enhanced-assets/`. They are illustrative
fictional scenes, while the underlying application and database captures remain
the verified evidence.

The presentation loop is available as both an MP4 and an auto-looping GIF:

```bash
cd memory/video
./build-presentation-loop.swift
```

- `presentation/guest-journey-command-center-loop.mp4`
- `presentation/guest-journey-command-center-loop.gif`

For PowerPoint, the MP4 provides the best image quality. Enable **Loop until
stopped** on the Playback tab. The GIF is convenient when automatic looping is
more important than file size or image quality.

The MP4 intentionally contains no audio track. Captions are burned into the
video and also published as SRT and WebVTT sidecars. Current Chrome captures are
kept as build inputs under `.build/captures-latest/` and are not published as
standalone article assets.
