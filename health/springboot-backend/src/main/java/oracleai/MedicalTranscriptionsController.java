package oracleai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import oracleai.services.OracleGenAI;
import oracleai.services.OracleObjectStore;
import oracleai.services.OracleSpeechAI;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/medicaltranscript")
public class MedicalTranscriptionsController {

    @PostMapping("/medicaltranscript")
    public String medicaltranscript(@RequestParam("file") MultipartFile multipartFile) throws Exception {
        OracleObjectStore.sendToObjectStorage(multipartFile.getOriginalFilename(), multipartFile.getInputStream());
        String transcriptionJobId = OracleSpeechAI.getTranscriptFromOCISpeech(multipartFile.getOriginalFilename());
        System.out.println("transcriptionJobId: " + transcriptionJobId);
        String jsonTranscriptFromObjectStorage =
                OracleObjectStore.getFromObjectStorage(transcriptionJobId,
                        AIApplication.OBJECTSTORAGE_NAMESPACE + "_" +
                                AIApplication.OBJECTSTORAGE_BUCKETNAME + "_" +
                                multipartFile.getOriginalFilename() + ".json");
//        System.out.println("jsonTranscriptFromObjectStorage: " + jsonTranscriptFromObjectStorage);
        String textFromTranscript  = getConcatenatedTokens(jsonTranscriptFromObjectStorage);
        System.out.println("textFromTranscript: " + textFromTranscript);
        String answerFromGenAI = OracleGenAI.chat(textFromTranscript + " in 40 words or less");
        return answerFromGenAI + " This advice is not meant as a substitute for professional healthcare guidance.";
    }


    public String getConcatenatedTokens(String json) throws JsonProcessingException{
            OracleSpeechAI.TranscriptionResponse response =
                    new ObjectMapper().readValue(json, OracleSpeechAI.TranscriptionResponse.class);
            return response.getTranscriptions().stream()
                    .flatMap(transcription -> transcription.getTokens().stream())
                    .map(OracleSpeechAI.TranscriptionResponse.Transcription.Token::getToken)
                    .collect(Collectors.joining(" "));
    }
}
