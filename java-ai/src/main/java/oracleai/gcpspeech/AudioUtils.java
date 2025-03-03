package oracleai.gcpspeech;

import javax.sound.sampled.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class AudioUtils {
    private static final int SAMPLE_RATE = 16000; // 16kHz
    private static final int NUM_CHANNELS = 1; // Mono
    private static final int BITS_PER_SAMPLE = 16; // 16-bit PCM
    private static final int BYTE_RATE = SAMPLE_RATE * NUM_CHANNELS * (BITS_PER_SAMPLE / 8);
    private static final int CHUNK_SIZE = 3 * SAMPLE_RATE * (BITS_PER_SAMPLE / 8); // 3 seconds of audio

    private static List<byte[]> audioChunks = new ArrayList<>();

    public static void saveAudioChunk(byte[] audioData) throws IOException {
        audioChunks.add(audioData);

        // If accumulated length >= 3 seconds, save as WAV
        int totalBytes = audioChunks.stream().mapToInt(a -> a.length).sum();
        if (totalBytes >= CHUNK_SIZE) {
            saveAsWav("C:/Users/opc/Downloads/audio_logs/audio_" + System.currentTimeMillis() + ".wav", audioChunks);
            audioChunks.clear(); // Clear buffer after saving
        }
    }

    public static void saveAsWav(String filename, List<byte[]> audioDataList) throws IOException {
        File file = new File(filename);
        Files.createDirectories(Paths.get(file.getParent()));

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (byte[] audioData : audioDataList) {
                out.write(audioData);
            }
            byte[] fullAudioData = out.toByteArray();

            try (ByteArrayOutputStream wavOut = new ByteArrayOutputStream()) {
                writeWavHeader(wavOut, fullAudioData.length);
                wavOut.write(fullAudioData);
                Files.write(file.toPath(), wavOut.toByteArray());
            }
        }
        System.out.println("💾 Saved WAV file: " + filename);
    }

    private static void writeWavHeader(ByteArrayOutputStream out, int audioDataLength) throws IOException {
        int totalDataLen = 36 + audioDataLength;
        int subchunk2Size = audioDataLength;

        out.write("RIFF".getBytes()); // ChunkID
        out.write(intToByteArray(totalDataLen)); // ChunkSize
        out.write("WAVE".getBytes()); // Format

        // Subchunk1 (format details)
        out.write("fmt ".getBytes()); // Subchunk1ID
        out.write(intToByteArray(16)); // Subchunk1Size
        out.write(shortToByteArray((short) 1)); // AudioFormat (PCM)
        out.write(shortToByteArray((short) NUM_CHANNELS)); // NumChannels
        out.write(intToByteArray(SAMPLE_RATE)); // SampleRate
        out.write(intToByteArray(BYTE_RATE)); // ByteRate
        out.write(shortToByteArray((short) (NUM_CHANNELS * (BITS_PER_SAMPLE / 8)))); // BlockAlign
        out.write(shortToByteArray((short) BITS_PER_SAMPLE)); // BitsPerSample

        // Subchunk2 (audio data)
        out.write("data".getBytes()); // Subchunk2ID
        out.write(intToByteArray(subchunk2Size)); // Subchunk2Size
    }

    private static byte[] intToByteArray(int value) {
        return ByteBuffer.allocate(4).putInt(value).array();
    }

    private static byte[] shortToByteArray(short value) {
        return ByteBuffer.allocate(2).putShort(value).array();
    }
}
