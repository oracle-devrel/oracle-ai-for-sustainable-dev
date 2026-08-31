# Theme Park Memory Lens for Snap Spectacles

This Lens Studio 5.15.4 source kit connects a Spectacles Lens to the Python
Oracle Agent Memory theme park demo. It is deliberately split into:

- editor-previewable AR overlays and HTTPS API calls;
- Spectacles-only speech recognition through `AsrModule`; and
- a browser simulator at `http://127.0.0.1:8092` for the complete backend flow.

## What is implemented

- explicit session consent for camera sensing, media recording, precise
  location, and retention;
- an always-visible recording/sensing status overlay;
- “remember this” voice transcription sent to Oracle Agent Memory;
- optional consented media transcript/caption indexing;
- semantic media search through Oracle AI Database `ALLMINILM` vectors;
- exact guest scope, short-lived session tokens, TTL, and database audit; and
- accessible route, clue, confirmation, and error overlays.

Raw video upload is not enabled by default. The backend can retain an optional
HTTPS object URI only after recording consent is enabled; the searchable data
is the transcript or caption embedded in Oracle AI Database.

## Create the Lens Studio project

1. Install Lens Studio 5.15.4 and create a project marked **Made for Spectacles**.
2. Add `InternetModule` and `AsrModule` to the project.
3. Import the TypeScript files in `Assets/Scripts`.
4. Create text components for status, route, clue, and result overlays and bind
   them to `ThemeParkMemoryController` in the Inspector.
5. Change `apiBaseUrl` to a public HTTPS tunnel that reaches the Python app.
   Plain HTTP is suitable only for experimental local work and cannot be
   published as a normal Lens.
6. In Preview, set **Device Type Override** to Spectacles so `fetch` works.
7. Use the browser simulator for the full flow. ASR and camera frames are
   Spectacles-only and require device testing.

## Backend

```bash
cd memory/python-agent
./run.sh
```

For a device test, expose port 8092 through an approved HTTPS tunnel, restrict
`AR_ALLOWED_ORIGIN`, and replace the demo session token with production
authentication before distributing the Lens.

## Privacy and safety boundary

- Camera sensing is not presented as “camera off.”
- Recording defaults off and requires a new opted-in session.
- No face recognition or biometric identity matching is implemented.
- Precise location defaults off.
- AR memories expire after the selected 1, 7, or 30 day retention period.
- Private guest memory uses exact guest and concierge-agent scope.
- Consequential transactions require a separate confirmation flow.
- The ordinary browser map remains the non-AR and reduced-distraction fallback.

The browser and API flows are locally testable. Outdoor visibility, tracking,
comfort, battery, permission UI, ASR quality, camera behavior, and park/ride
policy remain hardware and venue validation items.
