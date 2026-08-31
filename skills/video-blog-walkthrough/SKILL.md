---
name: video-blog-walkthrough
description: Create, revise, verify, and embed narrated technical walkthrough videos for developer blogs. Use when a request asks for a blog video, product walkthrough, source-code-and-application demo, subtitles/captions, narration, MP4/poster/SRT assets, or a local video embed that will later be replaced by YouTube.
---

# Video Blog Walkthrough

Produce a concise, reproducible video that demonstrates the real application and the source/configuration behind every material component.

## Workflow

1. Audit the blog and repository before scripting.
   - Extract the core message, component list, runnable path, and limitations.
   - Verify claims against current source and configuration.
   - Define a scene for the app plus one for each important component.
2. Create a scene plan with exact duration, visual source, narration, and subtitle text.
   - Prefer 90 seconds to 4 minutes unless the user specifies otherwise.
   - Start with outcome and architecture, then move through code/config, live app, data write, and recap.
   - Keep subtitles to two lines, about 42 characters per line when practical.
3. Capture authentic visuals.
   - Use the appropriate browser skill for the live application.
   - Show actual repository files for Java, JavaScript/TypeScript, YAML, SQL, or configuration.
   - Use stable zoom and large text; crop out unrelated tabs, notifications, and personal information.
   - Never show secrets, `.env` values, wallets, tokens, passwords, private URLs, or browser account data.
4. Build deterministic assets inside the project.
   - Store the build source, narration text, subtitle file, poster, and final MP4 together under a clearly named `video/` directory.
   - Prefer a repository-native build script so scenes can be regenerated when the blog changes.
   - Use available local media tooling. On macOS, AVFoundation/AVKit plus `say` is an acceptable dependency-light path.
5. Add narration and captions.
   - Narrate only what the current scene demonstrates.
   - Produce an SRT or WebVTT sidecar even if subtitles are burned into the video.
   - Use accessible contrast and keep burned captions inside title-safe margins.
6. Verify before embedding.
   - Play or inspect the complete video, not only individual frames.
   - Confirm the MP4 has video and audio, expected dimensions/duration, readable code, synchronized narration, and no secret exposure.
   - Run `scripts/audit_video.sh VIDEO SRT BLOG_HTML` when working on macOS.
7. Embed locally and preserve a YouTube migration path.
   - Use responsive HTML5 `<video controls preload="metadata" poster="...">`.
   - Include an MP4 `<source>` and caption `<track kind="captions" ... default>`.
   - Add a concise fallback link to the MP4.
   - Place the walkthrough near the article introduction and update nearby copy to match the demonstrated implementation.
8. Re-run the blog’s publishing audit and project tests, then commit only scoped assets.

## Quality Gates

- The live application visibly performs its central workflow.
- Every architecture component appears as application behavior, source, or configuration.
- Database writes shown in the video use test/demo data and are explicitly approved.
- Code is legible at normal playback size.
- Narration, burned subtitles, and sidecar captions convey the same meaning.
- No credential value or sensitive local path appears in frames, narration, captions, metadata, or committed files.
- Blog claims, commands, screenshots, and video all describe the same revision.

## Reusable Script

Run `scripts/audit_video.sh` after rendering. It checks file presence, macOS media metadata, caption numbering/timestamps, blog embedding, and obvious secret-like text in captions.
