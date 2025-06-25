package oracleai.aiholo;

import com.google.cloud.texttospeech.v1.*;
import com.google.protobuf.ByteString;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TTSAndAudio2Face {
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static void processMetahuman(String fileName, String textToSay, String languageCode, String voiceName) {
        executor.submit(() -> {
            try {
                TTS(fileName, textToSay, languageCode, voiceName);
                sendToAudio2Face(fileName);
            } catch (Exception e) {
                System.out.println("processMetahuman exception during TTS:" + e);
                //com.google.api.gax.rpc.UnavailableException: io.grpc.StatusRuntimeException:
                // UNAVAILABLE: Credentials failed to obtain metadata
                // will occur if token expired
                //TODO might be funny and helpful to do this, ie have the system gives its status and ask for help ...
                // sendToAudio2Face("uhoh-lookslikeIneedanewTTStoken.wav");
                sendToAudio2Face("../audio-aiholo/explainer.wav");
//                sendToAudio2Face("hello-brazil.wav");
            }

        });
    }


    public static void TTS(String fileName, String text, String languageCode, String voicename) throws Exception {
        try (TextToSpeechClient textToSpeechClient = TextToSpeechClient.create()) {
            System.out.println("in TTS  languagecode:" + languageCode + " voicename:" + voicename + " text:" + text);
            SynthesisInput input = SynthesisInput.newBuilder().setText(text).build();
                    //              "最受欢迎的游戏是Pods Of Kon。").build();
                    //  "最も人気のあるビデオゲームは「Pods Of Kon」です。").build();
            VoiceSelectionParams voice =
                    VoiceSelectionParams.newBuilder()
                            .setLanguageCode(languageCode) //ja-JP, en-US, ...
                            .setSsmlGender(SsmlVoiceGender.FEMALE) // NEUTRAL, MALE
                            .setName(voicename)  // "Kore" pt-BR-Wavenet-D
                            .build();
            AudioConfig audioConfig =
                    AudioConfig.newBuilder()
                            .setAudioEncoding(AudioEncoding.LINEAR16) // wav AudioEncoding.MP3
                            .build();
            SynthesizeSpeechResponse response =
                    textToSpeechClient.synthesizeSpeech(input, voice, audioConfig);
            ByteString audioContents = response.getAudioContent();
            try (OutputStream out = new FileOutputStream(fileName)) {
                out.write(audioContents.toByteArray());
//                System.out.println("Audio content written to file:" + fileName);
            }
        }
    }





    public static void sendToAudio2Face(String fileName) {
        System.out.print("sendToAudio2Face for fileName:" + fileName + " ...");
        RestTemplate restTemplate = new RestTemplate();
        String baseUrl = "http://localhost:8011/A2F/Player/";

        String setRootPathUrl = baseUrl + "SetRootPath";
        Map<String, Object> rootPathPayload = new HashMap<>();
        rootPathPayload.put("a2f_player", "/World/audio2face/Player");
        rootPathPayload.put("dir_path", AIHoloController.AUDIO_DIR_PATH);
        sendPostRequest(restTemplate, setRootPathUrl, rootPathPayload);

        String setTrackUrl = baseUrl + "SetTrack";
        Map<String, Object> trackPayload = new HashMap<>();
        trackPayload.put("a2f_player", "/World/audio2face/Player");
        trackPayload.put("file_name", fileName);
        trackPayload.put("time_range", new int[] { 0, -1 });
        sendPostRequest(restTemplate, setTrackUrl, trackPayload);

        String playTrackUrl = baseUrl + "Play";
        Map<String, Object> playPayload = new HashMap<>();
        playPayload.put("a2f_player", "/World/audio2face/Player");
        sendPostRequest(restTemplate, playTrackUrl, playPayload);
        System.out.println(" ...sendToAudio2Face complete");
    }

    private static void sendPostRequest(RestTemplate restTemplate, String url, Map<String, Object> payload) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
        if (response.getStatusCode().is2xxSuccessful()) {
//            System.out.println("Successfully sent request to: " + url);
        } else {
            System.err.println("Failed to send request to " + url + ". Response: " + response.getBody());
        }
    }





}

