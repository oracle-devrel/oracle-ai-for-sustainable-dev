package oracleai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import oracleai.services.OracleGenAI;
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
        public String getQuestion() {
            return question;
        }
        public void setQuestion(String question) {
            this.question = question;
        }
    }


    @CrossOrigin
    @PostMapping("/askai")
    public ResponseEntity<String> receiveQuestion(@RequestBody QuestionForm questionForm) throws Exception {
        System.out.println("ExplainAndAdviseOnHealthTestResults.receiveQuestion:" +
                questionForm.getQuestion());
        String answerFromGenAI = OracleGenAI.chat(questionForm.getQuestion() + " in 40 words or less");
        System.out.println("answerFromGenAI: " + answerFromGenAI);
        //todo parse string
        ResponseEntity<String> stringResponseEntity = new ResponseEntity<>(answerFromGenAI, HttpStatus.OK);
        return stringResponseEntity;
    }

}



