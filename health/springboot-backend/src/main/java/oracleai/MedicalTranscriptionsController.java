package oracleai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import oracleai.services.OpenAI;
import oracleai.services.OracleGenAI;
import oracleai.services.OracleObjectStore;
import oracleai.services.OracleSpeechAI;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/medicaltranscript")
public class MedicalTranscriptionsController {

        @CrossOrigin
        @PostMapping("/medicaltranscript")
        public String medicaltranscript(@RequestParam("file") MultipartFile multipartFile) throws Exception {
                try {
                        // Log detailed file information
                        String originalFileName = multipartFile.getOriginalFilename();
                        String contentType = multipartFile.getContentType();
                        long fileSize = multipartFile.getSize();

                        System.out.println("=== MEDICAL TRANSCRIPT DEBUG ===");
                        System.out.println("Received file: " + originalFileName);
                        System.out.println("Content type: " + contentType);
                        System.out.println("File size: " + fileSize + " bytes");
                        System.out.println("File is empty: " + multipartFile.isEmpty());
                        System.out.println("================================");

                        // Validate file
                        if (originalFileName == null || originalFileName.isEmpty()) {
                                System.err.println("ERROR: No filename provided");
                                return "No file selected. Please select an audio file and try again.";
                        }

                        if (multipartFile.isEmpty()) {
                                System.err.println("ERROR: File is empty");
                                return "The selected file is empty. Please select a valid audio file.";
                        }

                        // Check if this is a supported audio format for Oracle Speech AI
                        if (!isSupportedAudioFormat(originalFileName)) {
                                System.err.println("ERROR: Unsupported audio format: " + originalFileName);
                                return "ERROR: Unsupported audio format. Please use WAV, MP3, FLAC, or OGG format.";
                        }

                        // Check file size (Oracle Speech AI has size limits)
                        if (fileSize > 100 * 1024 * 1024) { // 100MB limit
                                System.err.println("ERROR: File too large: " + fileSize + " bytes");
                                return "ERROR: Audio file is too large (" + (fileSize / 1024 / 1024)
                                                + "MB). Maximum supported size is 100MB.";
                        }
                        System.out.println("Uploading to Object Storage...");
                        OracleObjectStore.sendToObjectStorage(multipartFile.getOriginalFilename(),
                                        multipartFile.getInputStream());

                        System.out.println("Starting speech transcription...");
                        String transcriptionJobId = OracleSpeechAI
                                        .getTranscriptFromOCISpeech(multipartFile.getOriginalFilename());
                        System.out.println("Transcription job created with ID: " + transcriptionJobId);

                        System.out.println("Retrieving transcription results from Object Storage...");
                        String objectPath = AIApplication.OBJECTSTORAGE_NAMESPACE + "_" +
                                        AIApplication.OBJECTSTORAGE_BUCKETNAME + "_" +
                                        multipartFile.getOriginalFilename() + ".json";
                        System.out.println("Looking for object: " + objectPath);

                        String jsonTranscriptFromObjectStorage = OracleObjectStore.getFromObjectStorage(
                                        transcriptionJobId, objectPath);

                        System.out.println("Raw response from Object Storage: " +
                                        (jsonTranscriptFromObjectStorage.length() > 200
                                                        ? jsonTranscriptFromObjectStorage.substring(0, 200) + "..."
                                                        : jsonTranscriptFromObjectStorage));

                        // Check if we got an error response from Object Storage
                        if (jsonTranscriptFromObjectStorage.contains("\"error\"")) {
                                System.err.println("Object Storage error detected: " + jsonTranscriptFromObjectStorage);
                                return "ERROR: Speech transcription service is currently unavailable. Please try again later.";
                        }

                        System.out.println("Processing transcription tokens...");
                        String textFromTranscript = getConcatenatedTokens(jsonTranscriptFromObjectStorage);
                        System.out.println("Extracted text: " + textFromTranscript);

                        // Check if transcription processing failed
                        if (textFromTranscript.startsWith("Unable to process")) {
                                System.err.println("Transcription processing failed: " + textFromTranscript);
                                return "ERROR: " + textFromTranscript;
                        }

                        System.out.println("Sending to OpenAI for analysis...");
                        String answerFromGenAI = OpenAI.chat(textFromTranscript + " in 40 words or less");
                        System.out.println("OpenAI response: " + answerFromGenAI);
                        return answerFromGenAI
                                        + " This advice is not meant as a substitute for professional healthcare guidance.";
                } catch (RuntimeException e) {
                        System.err.println("RuntimeException in medical transcript: " + e.getMessage());
                        e.printStackTrace();
                        if (e.getMessage().contains("Speech transcription job failed")) {
                                return "ERROR: Speech transcription failed. This could be due to: 1) Audio quality issues (too noisy, unclear speech), 2) Audio too short/long, 3) Service temporarily unavailable, 4) Audio encoding issues. Please try with a clear, well-recorded audio file.";
                        }
                        return "ERROR: " + e.getMessage();
                } catch (Exception e) {
                        System.err.println("Exception in medical transcript processing: " + e.getMessage());
                        e.printStackTrace();
                        return "ERROR: Technical issue with transcription service. " + e.getMessage();
                }
        }

        public String getConcatenatedTokens(String json) throws JsonProcessingException {
                try {
                        OracleSpeechAI.TranscriptionResponse response = new ObjectMapper().readValue(json,
                                        OracleSpeechAI.TranscriptionResponse.class);

                        // Check if response and transcriptions are not null
                        if (response == null || response.getTranscriptions() == null
                                        || response.getTranscriptions().isEmpty()) {
                                System.err.println("No valid transcriptions found in response");
                                return "Unable to process audio transcription. Please try again with a clear audio file.";
                        }

                        return response.getTranscriptions().stream()
                                        .flatMap(transcription -> transcription.getTokens().stream())
                                        .map(OracleSpeechAI.TranscriptionResponse.Transcription.Token::getToken)
                                        .collect(Collectors.joining(" "));
                } catch (Exception e) {
                        System.err.println("Error processing transcription JSON: " + e.getMessage());
                        System.err.println("JSON content: " + json);
                        return "Unable to process audio transcription due to technical issues. Please try again.";
                }
        }

        private boolean isAudioFile(String fileName) {
                if (fileName == null)
                        return false;
                String lowerFileName = fileName.toLowerCase();
                return lowerFileName.endsWith(".mp3") ||
                                lowerFileName.endsWith(".wav") ||
                                lowerFileName.endsWith(".m4a") ||
                                lowerFileName.endsWith(".flac") ||
                                lowerFileName.endsWith(".ogg") ||
                                lowerFileName.endsWith(".aac") ||
                                lowerFileName.endsWith(".wma") ||
                                lowerFileName.endsWith(".opus");
        }

        private boolean isSupportedAudioFormat(String fileName) {
                if (fileName == null)
                        return false;
                String lowerFileName = fileName.toLowerCase();
                // Oracle Speech AI supported formats (format may not be the issue)
                return lowerFileName.endsWith(".wav") ||
                                lowerFileName.endsWith(".mp3") ||
                                lowerFileName.endsWith(".flac") ||
                                lowerFileName.endsWith(".ogg") ||
                                lowerFileName.endsWith(".m4a"); // Re-enabling since format isn't the root issue
                // Note: Issue seems to be audio content/quality rather than format
        }
}
