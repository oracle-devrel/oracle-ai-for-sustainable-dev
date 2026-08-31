import { ThemeParkMemoryApi } from "./ThemeParkMemoryApi";

@component
export class ThemeParkMemoryController extends BaseScriptComponent {
  @input apiBaseUrl: string = "https://REPLACE-WITH-YOUR-TUNNEL.example";
  @input statusText!: Text;
  @input resultText!: Text;
  @input recordingIndicator!: SceneObject;

  private api!: ThemeParkMemoryApi;
  private asrModule = require("LensStudio:AsrModule") as AsrModule;
  private finalTranscript = "";

  onAwake(): void {
    this.api = new ThemeParkMemoryApi(this.apiBaseUrl.replace(/\/$/, ""));
    this.recordingIndicator.enabled = false;
    this.statusText.text = "AR sensing ready · recording off";
  }

  async startPrivateSession(): Promise<void> {
    try {
      const session = await this.api.startSession(false);
      this.statusText.text = `Private session · ${session.guestId} · recording off`;
      this.resultText.text = session.overlay;
    } catch (error) {
      this.showError(error);
    }
  }

  startRememberPhrase(): void {
    const options = AsrModule.AsrTranscriptionOptions.create();
    options.mode = AsrModule.AsrMode.HighAccuracy;
    options.silenceUntilTerminationMs = 1200;
    options.onTranscriptionUpdateEvent.add((event) => {
      this.finalTranscript = event.text;
      this.resultText.text = event.isFinal
        ? `Confirm memory: ${event.text}`
        : `Listening: ${event.text}`;
    });
    options.onTranscriptionErrorEvent.add((event) => {
      this.resultText.text = `Speech recognition unavailable: ${event}`;
    });
    this.asrModule.startTranscribing(options);
  }

  async confirmRemember(): Promise<void> {
    if (!this.finalTranscript.trim()) {
      this.resultText.text = "Nothing to remember yet.";
      return;
    }
    try {
      const result = await this.api.remember(this.finalTranscript.trim());
      this.resultText.text = String(result.overlay || result.message);
      this.finalTranscript = "";
    } catch (error) {
      this.showError(error);
    }
  }

  async startRecordingSessionAfterConsent(): Promise<void> {
    try {
      const session = await this.api.startSession(true);
      this.recordingIndicator.enabled = true;
      this.statusText.text = "Recording consent on · 7-day retention";
      this.resultText.text = session.overlay;
    } catch (error) {
      this.showError(error);
    }
  }

  async indexConfirmedTranscript(transcript: string): Promise<void> {
    try {
      const result = await this.api.indexMediaTranscript(transcript);
      this.resultText.text = String(result.overlay || result.message);
    } catch (error) {
      this.showError(error);
    }
  }

  private showError(error: unknown): void {
    this.resultText.text = `Request blocked: ${String(error)}`;
  }
}
