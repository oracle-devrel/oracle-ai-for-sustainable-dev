package oracleai;

import com.google.api.gax.rpc.ApiStreamObserver;
import com.google.api.gax.rpc.BidiStreamingCallable;
import com.google.cloud.speech.v1.*;
import com.google.protobuf.ByteString;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;
import org.springframework.web.socket.TextMessage;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CustomWebSocketHandler extends BinaryWebSocketHandler {
    private static final ConcurrentHashMap<String, ApiStreamObserver<StreamingRecognizeRequest>> activeSessions = new ConcurrentHashMap<>();
    private static SpeechClient speechClient;
    private static final int MIN_AUDIO_BUFFER_SIZE = 48000; // 🔥 Buffer at least 3 seconds before sending
    private static final double SILENCE_THRESHOLD = 0.01; // 🔥 Adjust silence detection (RMS method)

    static {
        try {
            speechClient = SpeechClient.create();
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize SpeechClient", e);
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        System.out.println("✅ WebSocket Connected: " + session.getId());

        ApiStreamObserver<StreamingRecognizeResponse> responseObserver = new ApiStreamObserver<>() {
            @Override
            public void onNext(StreamingRecognizeResponse response) {
                for (StreamingRecognitionResult result : response.getResultsList()) {
                    if (result.getAlternativesCount() > 0) {
                        String transcript = result.getAlternatives(0).getTranscript().trim();
                        if (!transcript.isEmpty()) {
                            System.out.println("📝 Full API Response: " + response.toString());
                            System.out.println("📝 Transcription: " + transcript);

                            try {
                                session.sendMessage(new TextMessage(transcript));
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

        // ✅ Configure Google Speech API for better accuracy
        BidiStreamingCallable<StreamingRecognizeRequest, StreamingRecognizeResponse> callable =
                speechClient.streamingRecognizeCallable();
        ApiStreamObserver<StreamingRecognizeRequest> requestObserver = callable.bidiStreamingCall(responseObserver);
        activeSessions.put(session.getId(), requestObserver);

        RecognitionConfig config = RecognitionConfig.newBuilder()
                .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                .setSampleRateHertz(16000)
                .setLanguageCode("en-US")
                .setEnableAutomaticPunctuation(true)
                .setModel("latest_long") // ✅ Best for longer speech
                .setAudioChannelCount(1)
                .setEnableWordTimeOffsets(true)
                .build();

        StreamingRecognitionConfig streamingConfig = StreamingRecognitionConfig.newBuilder()
                .setConfig(config)
                .setInterimResults(true)
                .setSingleUtterance(false) // ✅ Allows continuous speech
                .build();

        requestObserver.onNext(StreamingRecognizeRequest.newBuilder()
                .setStreamingConfig(streamingConfig)
                .build());
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        ByteBuffer payload = message.getPayload();
        byte[] audioBytes = new byte[payload.remaining()];
        payload.get(audioBytes);

        // ✅ Verify PCM Format
        if (!isValidPCMFormat(audioBytes)) {
            System.out.println("⚠️ Invalid PCM format. Skipping...");
            return;
        }

        // ✅ Check silence with RMS (Root Mean Square)
        if (isSilent(audioBytes)) {
            System.out.println("🔇 Skipping silent audio.");
            return;
        }

        // ✅ Save to WAV file
        try {
            saveAudioToWAV(audioBytes, "audio_" + System.currentTimeMillis() + ".wav");
        } catch (IOException e) {
            e.printStackTrace();
        }

        // ✅ Buffering audio before sending
        if (audioBytes.length < MIN_AUDIO_BUFFER_SIZE) {
            System.out.println("⏳ Accumulating audio, not sending yet...");
            return; // Don't send yet, wait for more audio
        }

        // ✅ Send to Google API
        if (activeSessions.containsKey(session.getId())) {
            ApiStreamObserver<StreamingRecognizeRequest> requestObserver = activeSessions.get(session.getId());
            requestObserver.onNext(StreamingRecognizeRequest.newBuilder()
                    .setAudioContent(ByteString.copyFrom(audioBytes))
                    .build());
        }
    }

    // ✅ Validate PCM Format
    private boolean isValidPCMFormat(byte[] audioData) {
        if (audioData.length < 2) return false;

        for (int i = 0; i < audioData.length; i += 2) {
            short sample = (short) ((audioData[i + 1] << 8) | (audioData[i] & 0xFF));
            if (sample < -32768 || sample > 32767) {
                return false; // Not valid 16-bit PCM range
            }
        }
        return true;
    }

    // ✅ Improved Silence Detection with RMS
    private boolean isSilent(byte[] audioData) {
        double sum = 0.0;
        for (int i = 0; i < audioData.length; i += 2) {
            short sample = (short) ((audioData[i + 1] << 8) | (audioData[i] & 0xFF));
            sum += sample * sample;
        }
        double rms = Math.sqrt(sum / (audioData.length / 2));

        System.out.println("📊 RMS Value: " + rms); // Debugging

        return rms < SILENCE_THRESHOLD;
    }

    // ✅ Save Audio to WAV File
    private void saveAudioToWAV(byte[] audioData, String filename) throws IOException {
        String filePath = "C:/Users/opc/Downloads/audio_logs/" + filename;
        Files.createDirectories(Paths.get("C:/Users/opc/Downloads/audio_logs/"));

        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            fos.write(generateWAVHeader(audioData.length, 16000, 1)); // ✅ Proper WAV header
            fos.write(audioData);
        }

        System.out.println("💾 Saved WAV: " + filePath);
    }

    // ✅ Generate Correct WAV Header
    private byte[] generateWAVHeader(int dataSize, int sampleRate, int channels) {
        int totalDataLen = dataSize + 36;
        int byteRate = sampleRate * channels * 2;

        return new byte[]{
            'R', 'I', 'F', 'F', (byte) (totalDataLen & 0xff), (byte) ((totalDataLen >> 8) & 0xff),
            (byte) ((totalDataLen >> 16) & 0xff), (byte) ((totalDataLen >> 24) & 0xff),
            'W', 'A', 'V', 'E', 'f', 'm', 't', ' ',
            16, 0, 0, 0, 1, 0, (byte) channels, 0,
            (byte) (sampleRate & 0xff), (byte) ((sampleRate >> 8) & 0xff),
            (byte) ((sampleRate >> 16) & 0xff), (byte) ((sampleRate >> 24) & 0xff),
            (byte) (byteRate & 0xff), (byte) ((byteRate >> 8) & 0xff),
            (byte) ((byteRate >> 16) & 0xff), (byte) ((byteRate >> 24) & 0xff),
            (byte) (channels * 2), 0, 16, 0,
            'd', 'a', 't', 'a', (byte) (dataSize & 0xff), (byte) ((dataSize >> 8) & 0xff),
            (byte) ((dataSize >> 16) & 0xff), (byte) ((dataSize >> 24) & 0xff)
        };
    }
}
