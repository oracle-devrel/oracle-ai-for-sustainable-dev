# Interactive AI blog walkthrough

This directory contains the reproducible source and publication assets for the silent, captioned blog walkthrough. It deliberately has no voice track so narration can be recorded and mixed later.

```bash
cd a2ui_agui_mcpapps_mcptoolkit/video
./build-video.swift
./verify-video.swift
```

The macOS build uses Swift, AppKit, and AVFoundation. It creates:

- `interactive-ai-walkthrough.mp4` — silent 1920×1080 H.264 video with burned subtitles
- `interactive-ai-walkthrough-poster.png` — blog poster frame
- `interactive-ai-walkthrough.srt` — YouTube-ready subtitles
- `interactive-ai-walkthrough.vtt` — captions used by the HTML5 blog embed

No `.env` file, password, wallet, token, or private connection string is read by the video build.

The caption-source files and both caption formats retain the canonical names `A2UI`, `AG-UI`, `A2A`, `MCP`, and `MCP Apps`; there are no phonetic speech-synthesis substitutions.
