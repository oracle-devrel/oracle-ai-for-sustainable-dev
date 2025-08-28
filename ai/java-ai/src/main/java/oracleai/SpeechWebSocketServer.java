package oracleai;

import com.google.api.gax.rpc.BidiStreamingCallable;
import com.google.api.gax.rpc.ApiStreamObserver;
import com.google.cloud.speech.v1.*;
import com.google.protobuf.ByteString;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import javax.sound.sampled.*;

import org.springframework.stereotype.Component;

@ServerEndpoint(value = "/speech", configurator = SpeechWebSocketConfigurator.class)
@Component
public class SpeechWebSocketServer {
    private static final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    private static SpeechClient speechClient;
    private ApiStreamObserver<StreamingRecognizeRequest> requestObserver;

    static {
        try {
            speechClient = SpeechClient.create();
        } catch (IOException e) {
            throw new RuntimeException("❌ Failed to initialize SpeechClient", e);
        }
    }

    @OnOpen
    public void onOpen(Session session) {
        System.out.println("✅ WebSocket Connected: " + session.getId());
        session.setMaxBinaryMessageBufferSize(1024 * 1024); // Allow large audio messages

        ApiStreamObserver<StreamingRecognizeResponse> responseObserver = new ApiStreamObserver<>() {
            @Override
            public void onNext(StreamingRecognizeResponse response) {
                for (StreamingRecognitionResult result : response.getResultsList()) {
                    if (result.getAlternativesCount() > 0) {
                        String transcript = result.getAlternatives(0).getTranscript().trim();
                        if (!transcript.isEmpty()) {
                            System.out.println("📝 Transcription: " + transcript);
                            try {
                                session.getBasicRemote().sendText(transcript);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("❌ Google API Error: " + t.getMessage());
            }

            @Override
            public void onCompleted() {
                System.out.println("✅ Streaming completed.");
            }
        };

        // Initialize Streaming to Google Speech API
        BidiStreamingCallable<StreamingRecognizeRequest, StreamingRecognizeResponse> callable =
                speechClient.streamingRecognizeCallable();
        requestObserver = callable.bidiStreamingCall(responseObserver);

        requestObserver.onNext(StreamingRecognizeRequest.newBuilder()
                .setStreamingConfig(StreamingRecognitionConfig.newBuilder()
                        .setConfig(RecognitionConfig.newBuilder()
                                .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                                .setSampleRateHertz(16000)
                                .setLanguageCode("en-US")
                                .setAudioChannelCount(1)
                                .setEnableAutomaticPunctuation(true)
                                .build())
                        .setInterimResults(true)
                        .setSingleUtterance(false)
                        .build())
                .build());
    }

    /**
     * 🔹 **Handles Incoming Binary Audio Data (From WebSocket)**
     * This method now **reads a WAV file** instead of processing real-time audio streaming.
     */
    @OnMessage
    public void onMessage(Session session) {
        String filePath = "C:/Users/opc/Downloads/audio_logs/sample.wav"; // Change to your WAV file path
        byte[] audioBytes;

        try {
            audioBytes = readWavFile(filePath);
            if (audioBytes == null || audioBytes.length == 0) {
                System.out.println("⚠️ WAV file is empty or could not be read.");
                return;
            }
        } catch (IOException | UnsupportedAudioFileException e) {
            System.err.println("❌ Error reading WAV file: " + e.getMessage());
            return;
        }

        System.out.println("✅ Sending Audio Data from WAV file: " + audioBytes.length + " bytes");

        if (requestObserver != null) {
            requestObserver.onNext(StreamingRecognizeRequest.newBuilder()
                    .setAudioContent(ByteString.copyFrom(audioBytes))
                    .build());
        }
    }

    @OnClose
    public void onClose(Session session) {
        System.out.println("🔴 WebSocket Closed: " + session.getId());
        if (requestObserver != null) {
            requestObserver.onCompleted();
        }
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        System.err.println("⚠️ WebSocket error: " + throwable.getMessage());
    }

    /**
     * **🔹 Reads WAV File and Extracts PCM Data**
     * - Converts **WAV file** to **raw PCM data**.
     * - Ensures it is in the **correct format** (16-bit mono PCM).
     */
    private byte[] readWavFile(String filePath) throws IOException, UnsupportedAudioFileException {
        File file = new File(filePath);
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(file);
        AudioFormat format = audioInputStream.getFormat();

        System.out.println("🎵 WAV File Format: " + format);

        // Convert to PCM Signed if necessary
        if (format.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
            AudioFormat pcmFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    format.getSampleRate(),
                    16, // Force 16-bit audio
                    format.getChannels(),
                    format.getChannels() * 2,
                    format.getSampleRate(),
                    false // Little-endian
            );
            audioInputStream = AudioSystem.getAudioInputStream(pcmFormat, audioInputStream);
        }

        // Read raw audio data
        byte[] audioBytes = audioInputStream.readAllBytes();
        audioInputStream.close();
        return audioBytes;
    }
}
