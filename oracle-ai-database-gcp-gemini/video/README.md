# Gemini Enterprise A2A blog walkthrough

This directory contains the reproducible source and publication assets embedded
in `../blog.html`. The video is silent so live narration can be added later.

```bash
cd oracle-ai-database-gcp-gemini/video
swift build-video.swift
swift verify-video.swift
/Users/pparkins/.codex/skills/video-blog-walkthrough/scripts/audit_video.sh \
  gemini-oracle-a2a-walkthrough.mp4 \
  gemini-oracle-a2a-walkthrough.srt \
  ../blog.html
```

The build creates a 1920x1080 H.264 MP4, poster, SRT, and WebVTT caption file.
Authentic Gemini Enterprise captures are cropped to remove browser chrome and
rendered with scripted cursor motion and click pulses. This controlled method
shows interaction without publishing secrets or private infrastructure values.
