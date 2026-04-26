package oracleai;

import io.a2a.spec.AgentCard;
import io.a2a.spec.DataPart;
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
import java.util.LinkedHashMap;
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
@RequestMapping("/inventory-action")
public class InventoryActionA2AController {

    private final AgentCardController agentCardController;
    private final InventoryActionAdkService inventoryActionAdkService;
    private final ConcurrentMap<String, Task> tasks;

    public InventoryActionA2AController(
            Environment environment,
            InventoryActionAdkService inventoryActionAdkService
    ) {
        AgentCard inventoryActionCard = InventoryActionCardFactory.buildInventoryActionAgentCard(environment);
        this.agentCardController = new AgentCardController(inventoryActionCard);
        this.inventoryActionAdkService = inventoryActionAdkService;
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
        String taskId = UUID.randomUUID().toString();
        String contextId = UUID.randomUUID().toString();

        Message userMessage = copyMessage(
                request.getParams() == null ? null : request.getParams().message(),
                Message.Role.USER,
                contextId,
                taskId,
                Map.of()
        );

        try {
            InventoryActionAdkService.InventoryActionResult result =
                    inventoryActionAdkService.run(userInput, contextId);

            Message agentMessage = new Message(
                    Message.Role.AGENT,
                    responseParts(result),
                    UUID.randomUUID().toString(),
                    contextId,
                    taskId,
                    List.of(),
                    responseMetadata(result),
                    List.of()
            );

            Task completedTask = new Task(
                    taskId,
                    contextId,
                    new TaskStatus(TaskState.COMPLETED, agentMessage, OffsetDateTime.now()),
                    List.of(),
                    userMessage == null ? List.of(agentMessage) : List.of(userMessage, agentMessage),
                    responseMetadata(result)
            );
            tasks.put(taskId, completedTask);
            return new SendMessageResponse(request.getId(), completedTask);
        } catch (Exception exception) {
            Message agentMessage = new Message(
                    Message.Role.AGENT,
                    List.of(new TextPart("Inventory action coordinator failed: " + exception.getMessage())),
                    UUID.randomUUID().toString(),
                    contextId,
                    taskId,
                    List.of(),
                    Map.of("error", "inventory_action_execution_failed"),
                    List.of()
            );

            Task failedTask = new Task(
                    taskId,
                    contextId,
                    new TaskStatus(TaskState.FAILED, agentMessage, OffsetDateTime.now()),
                    List.of(),
                    userMessage == null ? List.of(agentMessage) : List.of(userMessage, agentMessage),
                    Map.of("error", "inventory_action_execution_failed")
            );
            tasks.put(taskId, failedTask);
            return new SendMessageResponse(request.getId(), failedTask);
        }
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

    private static List<Part<?>> responseParts(InventoryActionAdkService.InventoryActionResult result) {
        List<Part<?>> parts = new ArrayList<>();
        parts.add(new TextPart(result.responseText()));
        if (result.draftAction() != null && !result.draftAction().isEmpty()) {
            Map<String, Object> actionData = new LinkedHashMap<>();
            actionData.put("action", result.draftAction());
            if (result.policyResult() != null && !result.policyResult().isEmpty()) {
                actionData.put("policy", result.policyResult());
            }
            parts.add(new DataPart(actionData));
        }
        return parts;
    }

    private static Map<String, Object> responseMetadata(InventoryActionAdkService.InventoryActionResult result) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("coordinator", result.orchestrationMode());
        metadata.put("traceCount", result.trace().size());
        if (result.draftAction() != null && !result.draftAction().isEmpty()) {
            metadata.put("actionType", result.draftAction().get("actionType"));
            metadata.put("draftActionId", result.draftAction().get("draftActionId"));
        }
        return metadata;
    }
}
