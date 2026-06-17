package oracleai;

import io.a2a.spec.AgentCard;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.Message;
import io.a2a.spec.Part;
import io.a2a.spec.SendMessageRequest;
import io.a2a.spec.SendMessageResponse;
import io.a2a.spec.Task;
import io.a2a.spec.TaskNotCancelableError;
import io.a2a.spec.TaskNotFoundError;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatus;
import io.a2a.spec.TextPart;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springaicommunity.a2a.server.controller.AgentCardController;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/select-ai")
public class SelectAiA2AController {

    private final AgentCardController agentCardController;
    private final SelectAiService selectAiService;
    private final ConcurrentMap<String, Task> tasks;

    public SelectAiA2AController(Environment environment, SelectAiService selectAiService) {
        AgentCard selectAiCard = SelectAiCardFactory.buildSelectAiAgentCard(environment);
        this.agentCardController = new AgentCardController(selectAiCard);
        this.selectAiService = selectAiService;
        this.tasks = new ConcurrentHashMap<>();
    }

    @GetMapping(
            path = "/.well-known/agent-card.json",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public AgentCard getAgentCard() {
        return agentCardController.getAgentCard();
    }

    @GetMapping(
            path = "/card",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public AgentCard getAgentCardV1() {
        return agentCardController.getAgentCardV1();
    }

    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public SendMessageResponse sendMessage(@RequestBody SendMessageRequest request) throws JSONRPCError {
        String userInput = extractUserInput(request);
        SelectAiService.SelectAiResult result = selectAiService.answer(userInput);

        String taskId = UUID.randomUUID().toString();
        String contextId = UUID.randomUUID().toString();

        Message userMessage = copyMessage(
                request.getParams() == null ? null : request.getParams().message(),
                Message.Role.USER,
                contextId,
                taskId,
                Map.of()
        );
        Message agentMessage = new Message(
                Message.Role.AGENT,
                List.of(new TextPart(result.responseText())),
                UUID.randomUUID().toString(),
                contextId,
                taskId,
                List.of(),
                Map.of(
                        "executionMode", result.executionMode(),
                        "action", result.action(),
                        "sourceDetail", result.sourceDetail()
                ),
                List.of()
        );

        Task task = new Task(
                taskId,
                contextId,
                new TaskStatus(TaskState.COMPLETED, agentMessage, OffsetDateTime.now()),
                List.of(),
                userMessage == null ? List.of(agentMessage) : List.of(userMessage, agentMessage),
                Map.of(
                        "executionMode", result.executionMode(),
                        "action", result.action(),
                        "sourceDetail", result.sourceDetail()
                )
        );
        tasks.put(taskId, task);

        return new SendMessageResponse(request.getId(), task);
    }

    @GetMapping(
            path = "/tasks/{taskId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Task getTask(@PathVariable String taskId) throws JSONRPCError {
        Task task = tasks.get(taskId);
        if (task == null) {
            throw new TaskNotFoundError();
        }
        return task;
    }

    @PostMapping(
            path = "/tasks/{taskId}/cancel",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Task cancelTask(@PathVariable String taskId) throws JSONRPCError {
        throw new TaskNotCancelableError();
    }

    private static String extractUserInput(SendMessageRequest request) {
        if (request == null || request.getParams() == null || request.getParams().message() == null) {
            return "";
        }

        List<String> texts = new ArrayList<>();
        for (Part<?> part : request.getParams().message().getParts()) {
            if (part instanceof TextPart textPart && textPart.getText() != null) {
                texts.add(textPart.getText());
            }
        }
        return String.join("\n", texts).trim();
    }

    private static Message copyMessage(
            Message original,
            Message.Role fallbackRole,
            String contextId,
            String taskId,
            Map<String, Object> metadata
    ) {
        if (original == null) {
            return null;
        }
        return new Message(
                original.getRole() == null ? fallbackRole : original.getRole(),
                original.getParts() == null ? List.of() : original.getParts(),
                original.getMessageId() == null ? UUID.randomUUID().toString() : original.getMessageId(),
                contextId,
                taskId,
                original.getReferenceTaskIds() == null ? List.of() : original.getReferenceTaskIds(),
                metadata,
                original.getExtensions() == null ? List.of() : original.getExtensions()
        );
    }
}
