package oracleai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import oracleai.services.OracleGenAI;
import oracleai.services.OpenAI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/askai")

public class AskAIController {

    private static Logger log = LoggerFactory.getLogger(AskAIController.class);

    public static class QuestionForm {
        private String question;
        private String provider; // Optional: "oracle" or "openai" (default: "openai")

        public String getQuestion() {
            return question;
        }

        public void setQuestion(String question) {
            this.question = question;
        }

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }
    }

    @CrossOrigin
    @PostMapping("/askai")
    public ResponseEntity<String> receiveQuestion(@RequestBody QuestionForm questionForm) throws Exception {
        System.out.println("ExplainAndAdviseOnHealthTestResults.receiveQuestion:" +
                questionForm.getQuestion());

        String answerFromGenAI;
        String provider = questionForm.getProvider();

        // Default to OpenAI if no provider specified
        if (provider == null || provider.isEmpty()) {
            provider = "openai";
        }

        // Route to appropriate AI service based on provider
        if ("oracle".equalsIgnoreCase(provider)) {
            System.out.println("Using Oracle GenAI service");
            answerFromGenAI = OracleGenAI.chat(questionForm.getQuestion() + " in 40 words or less");
        } else {
            System.out.println("Using OpenAI service");
            answerFromGenAI = OpenAI.chat(questionForm.getQuestion() + " in 40 words or less");
        }

        System.out.println("answerFromGenAI: " + answerFromGenAI);
        ResponseEntity<String> stringResponseEntity = new ResponseEntity<>(answerFromGenAI, HttpStatus.OK);
        return stringResponseEntity;
    }

    @CrossOrigin
    @PostMapping("/askai/oracle")
    public ResponseEntity<String> receiveQuestionOracle(@RequestBody QuestionForm questionForm) throws Exception {
        System.out.println("ExplainAndAdviseOnHealthTestResults.receiveQuestionOracle:" +
                questionForm.getQuestion());
        String answerFromGenAI = OracleGenAI.chat(questionForm.getQuestion() + " in 40 words or less");
        System.out.println("answerFromOracleGenAI: " + answerFromGenAI);
        ResponseEntity<String> stringResponseEntity = new ResponseEntity<>(answerFromGenAI, HttpStatus.OK);
        return stringResponseEntity;
    }

    @CrossOrigin
    @PostMapping("/askai/openai")
    public ResponseEntity<String> receiveQuestionOpenAI(@RequestBody QuestionForm questionForm) throws Exception {
        System.out.println("ExplainAndAdviseOnHealthTestResults.receiveQuestionOpenAI:" +
                questionForm.getQuestion());
        String answerFromOpenAI = OpenAI.chat(questionForm.getQuestion() + " in 40 words or less");
        System.out.println("answerFromOpenAI: " + answerFromOpenAI);
        ResponseEntity<String> stringResponseEntity = new ResponseEntity<>(answerFromOpenAI, HttpStatus.OK);
        return stringResponseEntity;
    }

}
