package oracleai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.completion.chat.ChatCompletionChoice;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;
import lombok.Data;
import oracle.jdbc.OracleTypes;
import oracle.sql.json.OracleJsonObject;
import oracle.ucp.jdbc.PoolDataSource;
import oracle.ucp.jdbc.PoolDataSourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/databasejs")
public class CallAIFromOracleDatabaseUseJSONAndSQL {

    private static Logger log = LoggerFactory.getLogger(CallAIFromOracleDatabaseUseJSONAndSQL.class);
    String lastReply;

    @GetMapping("/form")
    public String form(){
        return "                <html>" +
                "<form method=\"post\" action=\"/databasejs/conversation\">" +
                "  <br> Provide a unique conversation name and dialogue/question ..\n" +
                "  <br><label for=\"conversationname\">conversation name:</label><br>" +
                "  <input type=\"text\" id=\"conversationname\" name=\"conversationname\" value=\"conversationname\"><br>" +

                "  <label for=\"dialogue\">dialogue:</label><br>" +
                "  <input type=\"text\" id=\"dialogue\" name=\"dialogue\" value=\"dialogue\" size=\"60\"><br><br>" +
                "  <input type=\"submit\" value=\"Submit\">" +
                "</form> " +
                "</html>";
    }

    @PostMapping("/conversation")
    public String conversation( @RequestParam("conversationname") String conversationName,
                                @RequestParam("dialogue") String dialogue)
            throws Exception {
        System.out.println("conversationname:" + conversationName + "dialogue:" + dialogue + " ");
        dialogue = URLEncoder.encode(dialogue, StandardCharsets.UTF_8.toString());
        Connection conn = getConnection();
        Conversation conversation = new Conversation();
        ObjectMapper objectMapper = new ObjectMapper();
        try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO conversation_dv VALUES (?)")) {
            conversation.setName(conversationName);
            // the user asking question
            Interlocutor interlocutorUser = new Interlocutor();
            interlocutorUser.setInterlocutorId(1);
            interlocutorUser.setName("Paul");
            interlocutorUser.setDialogue(dialogue);
            // the as yet unanswered repl
            Interlocutor interlocutorOpenAI = new Interlocutor();
            interlocutorOpenAI.setInterlocutorId(0);
            interlocutorOpenAI.setName("OpenAI");
            conversation.setInterlocutor(List.of(interlocutorOpenAI, interlocutorUser));
            String json = objectMapper.writeValueAsString(conversation);
            System.out.println(json);
                    stmt.setObject(1,  json, OracleTypes.JSON);
            stmt.execute();
        }
        System.out.println("CallAIFromOracleDatabaseUseJSONAndSQL. insert done");
        CallableStatement cstmt = conn.prepareCall("{call openai_call()}");
        cstmt.execute();
        System.out.println("CallAIFromOracleDatabaseUseJSONAndSQL. sproc done");
        return lastReply;
    }

    private static Connection getConnection() throws SQLException {
        PoolDataSource pool = PoolDataSourceFactory.getPoolDataSource();
        pool.setURL("jdbc:oracle:thin:@localhost:1521/FREEPDB1");
        pool.setUser("aijs");
        pool.setPassword("Welcome12345");
        pool.setConnectionFactoryClassName("oracle.jdbc.pool.OracleDataSource");
        Connection conn  = pool.getConnection();
        return conn;
    }

    @Data
    public class Conversation {
        private String name;
        private List<Interlocutor> interlocutor;
    }

    @Data
    public class Interlocutor {
        private int interlocutorId;
        private String name;
        private String dialogue;
    }

    @GetMapping("/getreply")
    String getreply( @RequestParam("textcontent") String textcontent)  {
        System.out.println("CallAIFromOracleDatabaseUseJSONAndSQL.getreply");
        OpenAiService service =
                new OpenAiService(System.getenv("OPENAI_KEY"), Duration.ofSeconds(60));
        System.out.println("Streaming chat completion... textcontent:" + textcontent);
        final List<ChatMessage> messages = new ArrayList<>();
        final ChatMessage systemMessage = new ChatMessage(ChatMessageRole.SYSTEM.value(), textcontent);
        messages.add(systemMessage);
        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest
                .builder()
                .model("gpt-3.5-turbo")
                .messages(messages)
                .n(1)
                .maxTokens(300) //was 50
                .logitBias(new HashMap<>())
                .build();
        String replyString = "";
        String content;
        for (ChatCompletionChoice choice : service.createChatCompletion(chatCompletionRequest).getChoices()) {
            content = choice.getMessage().getContent();
            replyString += (content == null?" ": content);
        }
        service.shutdownExecutor();
        System.out.println("CallAIFromOracleDatabaseUseJSONAndSQL.getreply replyString:" + replyString);
        return lastReply = replyString;
    }

    @GetMapping("/queryconversations")
    public String queryconversations() throws SQLException {
        PreparedStatement stmt = getConnection().prepareStatement("SELECT data FROM conversation_dv ");
//     conn.prepareStatement("SELECT data FROM conversation_dv t WHERE t.data.conversationId = ? ");   stmt.setInt(1, 201);
        ResultSet rs = stmt.executeQuery();
        String results = "";
        while (rs.next()) {
            OracleJsonObject race = rs.getObject(1, OracleJsonObject.class);
            System.out.println(race.toString());
            results+= race + "\n";
        }
        System.out.println("queryconversations results:" + results);
        return results;
    }
}
