export type ArSession = {
  sessionId: string;
  sessionToken: string;
  guestId: string;
  overlay: string;
};

export class ThemeParkMemoryApi {
  private session: ArSession | null = null;

  constructor(private readonly baseUrl: string) {}

  async startSession(mediaRecording: boolean): Promise<ArSession> {
    this.session = await this.post("session/start", {
      guestId: "AVA",
      cameraSensing: true,
      mediaRecording,
      locationSharing: false,
      retentionDays: 7,
    }, false) as ArSession;
    return this.session;
  }

  async remember(text: string): Promise<Record<string, unknown>> {
    return this.post("remember", { text, source: "spectacles" });
  }

  async indexMediaTranscript(transcript: string): Promise<Record<string, unknown>> {
    return this.post("media", {
      transcript,
      mediaType: "video-transcript",
    });
  }

  async searchMedia(query: string): Promise<Record<string, unknown>> {
    return this.post("search", { query });
  }

  private async post(
    path: string,
    payload: Record<string, unknown>,
    includeSession = true,
  ): Promise<Record<string, unknown>> {
    if (includeSession) {
      if (!this.session) throw new Error("Start the consent session first.");
      payload = {
        ...payload,
        sessionId: this.session.sessionId,
        sessionToken: this.session.sessionToken,
      };
    }
    const internetModule = require("LensStudio:InternetModule") as InternetModule;
    const response = await internetModule.fetch(
      new Request(`${this.baseUrl}/api/ar/${path}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      }),
    );
    const body = await response.json();
    if (!response.ok) throw new Error(body.error || `HTTP ${response.status}`);
    return body;
  }
}
