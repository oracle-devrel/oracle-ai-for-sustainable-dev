package oracleai.aiholo;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import com.google.cloud.texttospeech.v1.AudioEncoding;
import com.google.cloud.texttospeech.v1.SsmlVoiceGender;
import com.google.cloud.texttospeech.v1.SynthesisInput;
import com.google.cloud.texttospeech.v1.SynthesizeSpeechResponse;
import com.google.cloud.texttospeech.v1.TextToSpeechClient;
import com.google.cloud.texttospeech.v1.VoiceSelectionParams;
import com.google.protobuf.ByteString;
import com.google.cloud.texttospeech.v1.AudioConfig;

import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.*;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.stereotype.Service;

@RestController
@RequestMapping("/aiholo")
// @CrossOrigin(origins = "*")
public class AIHoloController {
    private String theValue = "mirrorme";
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final String API_URL = "http://129.x.x.x/v1/chat/completions?client=server";
    private static final String AUTH_TOKEN = "Bearer asdf";
    private static final String DEFAULT_LANGUAGE_CODE = "Bearer asdf";
    private static final String DEFAULT_VOICE_NAME = "Bearer asdf";

    @Autowired
    private DataSource dataSource;

    private static final Object metahumanLock = new Object();
    private static boolean isRecentQuestionProcessed;

    public AIHoloController() {
        System.out.println("startInactivityMonitor...");
        startInactivityMonitor();
    }

    private void startInactivityMonitor() {
        scheduler.scheduleAtFixedRate(() -> {
            if (isRecentQuestionProcessed) {
                System.out.println("isRecentQuestionProcessed true so skipping the timecheck/keepalive");
                isRecentQuestionProcessed = false;
            }
            String fileName = "currenttime.wav"; //testing123-brazil.wav
            TTSAndAudio2Face.processMetahuman(
                        fileName, TimeInWords.getTimeInWords(true),
                    DEFAULT_LANGUAGE_CODE, DEFAULT_VOICE_NAME);
        }, 1, 15, TimeUnit.MINUTES);
    }


    @GetMapping("/set")
    public String setValue(@RequestParam("value") String value) {
        theValue = value;
        System.out.println("EchoController set: " + theValue);
        String filePath = "C:/Users/opc/aiholo_output.txt";
        try (FileWriter writer = new FileWriter(filePath)) {
            JSONObject json = new JSONObject();
            json.put("data", value); // Store the response inside JSON
            writer.write(json.toString());
            writer.flush();
        } catch (IOException e) {
            return "Error writing to file: " + e.getMessage();
        }

        if (value.equals("mirrorme") || value.equals("question"))
            return "「ミラーミー」モードが正常に有効化されました";
        else
            return "set successfully: " + theValue;

    }

    @GetMapping("/get")
    public String getValue() {
        System.out.println("EchoController get: " + theValue);
        return theValue;
    }

    static String sql = """
                SELECT DBMS_CLOUD_AI.GENERATE(
                    prompt       => ?,
                    profile_name => 'VIDEO_GAMES',
                    action       => ?
                ) FROM dual
            """;

    @GetMapping("/play")
    public String play(@RequestParam("question") String question, 
        @RequestParam("selectedMode") String selectedMode,
        @RequestParam("languagecode") String languagecode,
         @RequestParam("voicename") String voicename) throws Exception {
        System.out.println(
                "play question: " + question + " selectedMode: " + selectedMode +
                        " languagecode:"+ languagecode+ " voicename:"+ voicename);
        question = "qual é o videogame mais popular";
        System.out.println(
                "modified question: " + question );
        theValue = "question";
        String filePath = "C:/Users/opc/aiholo_output.txt";
        try (FileWriter writer = new FileWriter(filePath)) {
            JSONObject json = new JSONObject();
            json.put("data", theValue); // Store the response inside JSON
            writer.write(json.toString());
            writer.flush();
        } catch (IOException e) {
            return "Error writing to file: " + e.getMessage();
        }
        String answer = "I'm sorry. I couldn't find an answer", action = "chat"; //TODO, this should be in correct language
        if (question.contains("use vectorrag")) {
            action = "vectorrag";
            question = question.replace("use vectorrag", "").trim();
            answer = executeSandbox(question);
        } else {
            if (selectedMode.contains("use narrate")) {
                action = "narrate";
//                question = question.replace("use narrate", "").trim();
            } else {
                question = question.replace("use chat", "").trim();
            }
            try (Connection connection = dataSource.getConnection();
                    PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                System.out.println("Database Connection : " + connection);
                String response = null;
                preparedStatement.setString(1, question);
                preparedStatement.setString(2, action);
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    if (resultSet.next()) {
                        response = resultSet.getString(1); // Retrieve AI response from the first column
                    }
                }
                answer = response;
            } catch (SQLException e) {
                System.err.println("Failed to connect to the database: " + e.getMessage());
                return "Database Connection Failed!";
            }
        }
        String fileName = "output.wav";
        System.out.println("about to TTS and sendAudioToAudio2Face for answer: " + answer);
        TTSAndAudio2Face.processMetahuman(fileName, answer, languagecode, voicename);
        return answer;
    }












    public String executeSandbox(String cummulativeResult) {
        System.out.println("isRag is true, using AI sandbox: " + cummulativeResult);

        // Remove "use RAG" references
    //    cummulativeResult = cummulativeResult.replace("use RAG", "").replace("use rag", "").trim();
      //  cummulativeResult += " . Make answer one sentence that is shorter than 50 words";

        // Prepare request body
        Map<String, Object> payload = new HashMap<>();
        Map<String, String> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", cummulativeResult);
        payload.put("messages", new Object[] { message });

        // Convert payload to JSON
        JSONObject jsonPayload = new JSONObject(payload);

        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", AUTH_TOKEN);
        headers.set("Accept", "application/json");

        HttpEntity<String> request = new HttpEntity<>(jsonPayload.toString(), headers);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.exchange(API_URL, HttpMethod.POST, request, String.class);

        String latestAnswer;
        if (response.getStatusCode() == HttpStatus.OK) {
            JSONObject responseData = new JSONObject(response.getBody());
            latestAnswer = responseData.getJSONArray("choices").getJSONObject(0).getJSONObject("message")
                    .getString("content");

            System.out.println("RAG Full Response latest_answer: " + latestAnswer);
            return latestAnswer;
        } else {
            System.out.println("Failed to fetch data: " + response.getStatusCode() + " " + response.getBody());
            return " I'm sorry, I couldn't find an answer";
        }
    }
    // `https://141.148.204.74:8444/aiholo/tts?textToConvert=${encodeURIComponent(textToConvert)}&languageCode=${encodeURIComponent(languageCode)}&ssmlGender=${encodeURIComponent(ssmlGender)}&voiceName=${encodeURIComponent(voiceName)}`;
            


   // `https://host:port/aiholo/tts?textToConvert=${encodeURIComponent(textToConvert)}&languageCode=${encodeURIComponent(languageCode)}&ssmlGender=${encodeURIComponent(ssmlGender)}&voiceName=${encodeURIComponent(voiceName)}`;
   @GetMapping("/tts")
   public ResponseEntity<byte[]>  tts(@RequestParam("textToConvert") String textToConvert, 
       @RequestParam("languageCode") String languageCode, 
       @RequestParam("ssmlGender") String ssmlGender, 
       @RequestParam("voiceName") String voiceName) throws Exception {
        String info= "tts for textToConvert " + textToConvert;
        System.out.println("in TTS GCP info:" + info);
        try (TextToSpeechClient textToSpeechClient = TextToSpeechClient.create()) {
         System.out.println("in TTS GCP textToSpeechClient:" + textToSpeechClient + " languagecode:" + languageCode);
          SynthesisInput input = SynthesisInput.newBuilder().setText(textToConvert).build();
          VoiceSelectionParams voice =
              VoiceSelectionParams.newBuilder()
                  .setLanguageCode(languageCode)
                //   .setSsmlGender(SsmlVoiceGender.NEUTRAL)
                  .setSsmlGender(SsmlVoiceGender.FEMALE)
                  .setName(voiceName)
                //   .setName("pt-BR-Wavenet-A") 
                  .build();
          AudioConfig audioConfig =
              AudioConfig.newBuilder()
                      .setAudioEncoding(AudioEncoding.LINEAR16) // wav
    //                  .setAudioEncoding(AudioEncoding.MP3)
                      .build();
          SynthesizeSpeechResponse response =
              textToSpeechClient.synthesizeSpeech(input, voice, audioConfig);
          ByteString audioContents = response.getAudioContent();
          byte[] audioData = audioContents.toByteArray();

          // Set response headers
          HttpHeaders headers = new HttpHeaders();
          headers.set(HttpHeaders.CONTENT_TYPE, "audio/mpeg"); 
          headers.set(HttpHeaders.CONTENT_DISPOSITION, 
          "attachment; filename=\"tts-" + languageCode + "" + ssmlGender+ "" + voiceName + "_" +
                  getFirst10Chars(textToConvert) + ".mp3\"");

          return new ResponseEntity<>(audioData, headers, HttpStatus.OK);
    
        //   try (OutputStream out = new FileOutputStream("output.wav")) {
        //     out.write(audioContents.toByteArray());
        //     System.out.println("Audio content written to file \"output.wav\"");
        //   }
        }

        // return "succesful " + info;
   }

   public static String getFirst10Chars(String textToConvert) {
    if (textToConvert == null || textToConvert.isEmpty()) {
        return "";
    }
    return textToConvert.length() > 10 ? textToConvert.substring(0, 10) : textToConvert;
}
}
