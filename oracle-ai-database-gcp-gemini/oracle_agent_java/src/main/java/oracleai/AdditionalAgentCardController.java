package oracleai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.a2a.spec.AgentCard;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdditionalAgentCardController {

    private final Environment environment;
    private final ObjectMapper objectMapper;

    public AdditionalAgentCardController(Environment environment, ObjectMapper objectMapper) {
        this.environment = environment;
        this.objectMapper = objectMapper;
    }

    @GetMapping(
            value = {"/agent-card-graph.json", "/graph/.well-known/agent-card.json"},
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    AgentCard graphAgentCard() {
        return GraphA2AConfiguration.buildGraphAgentCard(environment);
    }

    @GetMapping(
            value = {
                    "/agent-card-spatial.json",
                    "/spatial-agent-card.json",
                    "/spatial/.well-known/agent-card.json"
            },
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    AgentCard spatialAgentCard() {
        return SpatialAgentCardFactory.buildSpatialAgentCard(environment);
    }

    @GetMapping(
            value = {
                    "/agent-card-action.json",
                    "/inventory-action-agent-card.json"
            },
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    AgentCard actionAgentCard() {
        return InventoryActionCardFactory.buildInventoryActionAgentCard(environment);
    }

    @GetMapping(
            value = {
                    "/agent-card-select-ai.json",
                    "/select-ai-agent-card.json"
            },
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    AgentCard selectAiAgentCard() {
        return SelectAiCardFactory.buildSelectAiAgentCard(environment);
    }

    @GetMapping(
            value = {
                    "/oracle-ai-database-agent-card.json"
            },
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    ObjectNode oracleAiDatabaseAgentCard() {
        return OracleAiDatabaseAgentCardFactory.buildOracleAiDatabaseAgentCard(environment, objectMapper);
    }

    @GetMapping(
            value = {
                    "/agent-card-inventory-system.json",
                    "/inventory-system-agent-card.json"
            },
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    AgentCard inventorySystemAgentCard() {
        return InventorySystemCardFactory.buildInventorySystemAgentCard(environment);
    }
}
