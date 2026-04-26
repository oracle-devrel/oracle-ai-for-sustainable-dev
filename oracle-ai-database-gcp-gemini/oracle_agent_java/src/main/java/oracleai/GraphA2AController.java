package oracleai;

import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.events.QueueManager;
import io.a2a.server.requesthandlers.DefaultRequestHandler;
import io.a2a.server.requesthandlers.RequestHandler;
import io.a2a.server.tasks.PushNotificationConfigStore;
import io.a2a.server.tasks.PushNotificationSender;
import io.a2a.server.tasks.TaskStore;
import io.a2a.spec.AgentCard;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.SendMessageRequest;
import io.a2a.spec.SendMessageResponse;
import io.a2a.spec.Task;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;
import org.springaicommunity.a2a.server.controller.AgentCardController;
import org.springaicommunity.a2a.server.controller.MessageController;
import org.springaicommunity.a2a.server.controller.TaskController;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/graph")
public class GraphA2AController {

    private final AgentCardController agentCardController;
    private final MessageController messageController;
    private final TaskController taskController;

    public GraphA2AController(
            Environment environment,
            Function<GraphTools.GraphRequest, GraphTools.GraphResponse> getSupplyChainDependencies,
            GraphRequestParser graphRequestParser,
            TaskStore taskStore,
            QueueManager queueManager,
            PushNotificationConfigStore pushNotificationConfigStore,
            PushNotificationSender pushNotificationSender
    ) {
        AgentCard graphCard = GraphA2AConfiguration.buildGraphAgentCard(environment);
        AgentExecutor agentExecutor = GraphA2AConfiguration.buildAgentExecutor(
                getSupplyChainDependencies,
                graphRequestParser
        );
        RequestHandler requestHandler = DefaultRequestHandler.create(
                agentExecutor,
                taskStore,
                queueManager,
                pushNotificationConfigStore,
                pushNotificationSender,
                ForkJoinPool.commonPool()
        );

        this.agentCardController = new AgentCardController(graphCard);
        this.messageController = new MessageController(requestHandler);
        this.taskController = new TaskController(requestHandler);
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
        return messageController.sendMessage(request);
    }

    @GetMapping(
            path = "/tasks/{taskId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Task getTask(@PathVariable String taskId) throws JSONRPCError {
        return taskController.getTask(taskId);
    }

    @PostMapping(
            path = "/tasks/{taskId}/cancel",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Task cancelTask(@PathVariable String taskId) throws JSONRPCError {
        return taskController.cancelTask(taskId);
    }
}
