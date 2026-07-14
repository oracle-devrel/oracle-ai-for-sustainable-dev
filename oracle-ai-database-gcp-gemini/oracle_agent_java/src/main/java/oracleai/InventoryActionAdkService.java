package oracleai;

import com.google.adk.agents.LlmAgent;
import com.google.adk.agents.RunConfig;
import com.google.adk.agents.SequentialAgent;
import com.google.adk.agents.ParallelAgent;
import com.google.adk.events.Event;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.sessions.Session;
import com.google.adk.tools.FunctionTool;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import io.reactivex.rxjava3.core.Flowable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class InventoryActionAdkService {

    private static final String APP_NAME = "oracle-inventory-action";
    private static final Pattern PRODUCT_ID_PATTERN = Pattern.compile("\\b([A-Z]{2,}-\\d+)\\b");

    private final InMemoryRunner runner;
    private final InventoryActionTools inventoryActionTools;

    public InventoryActionAdkService(Environment environment, InventoryActionTools tools) {
        this.inventoryActionTools = tools;
        String modelName = firstNonBlank(
                environment.getProperty("ACTION_COORDINATOR_MODEL"),
                environment.getProperty("MODEL_NAME"),
                "gemini-2.0-flash"
        );

        FunctionTool graphEvidenceTool = FunctionTool.create(tools, "getGraphEvidence");
        FunctionTool spatialEvidenceTool = FunctionTool.create(tools, "getSpatialEvidence");
        FunctionTool externalSignalsTool = FunctionTool.create(tools, "getExternalSignals");
        FunctionTool policyTool = FunctionTool.create(tools, "checkTransferPolicy");
        FunctionTool draftActionTool = FunctionTool.create(tools, "draftInventoryTransferAction");

        LlmAgent graphEvidenceAgent = LlmAgent.builder()
                .name("graph_evidence_specialist")
                .description("Oracle Graph specialist for supply-chain dependency evidence.")
                .model(modelName)
                .instruction("""
                        You are the graph-evidence specialist for inventory risk response.
                        Always call getGraphEvidence for the relevant productId before answering.
                        Return only a concise supply-chain evidence summary and never recommend an action.
                        """)
                .tools(graphEvidenceTool)
                .build();

        LlmAgent spatialEvidenceAgent = LlmAgent.builder()
                .name("spatial_evidence_specialist")
                .description("Spatial hotspot specialist for warehouse pressure and transfer direction.")
                .model(modelName)
                .instruction("""
                        You are the spatial-evidence specialist for inventory risk response.
                        Always call getSpatialEvidence for the relevant productId before answering.
                        Return only a concise hotspot summary with recommended source and destination warehouses.
                        Do not make a final action recommendation.
                        """)
                .tools(spatialEvidenceTool)
                .build();

        LlmAgent externalSignalsAgent = LlmAgent.builder()
                .name("external_signal_specialist")
                .description("External-risk specialist for weather and geopolitical supply-lane impacts.")
                .model(modelName)
                .instruction("""
                        You are the external-signals specialist for inventory risk response.
                        Always call getExternalSignals for the relevant productId before answering.
                        Return only a concise summary of outside factors that could change the timing or urgency of an action.
                        Do not make a final action recommendation.
                        """)
                .tools(externalSignalsTool)
                .build();

        ParallelAgent parallelEvidenceAgent = ParallelAgent.builder()
                .name("parallel_evidence_gatherer")
                .description("Runs graph, spatial, and external-signal specialists in parallel before an action recommendation is made.")
                .subAgents(graphEvidenceAgent, spatialEvidenceAgent, externalSignalsAgent)
                .build();

        LlmAgent decisionAgent = LlmAgent.builder()
                .name("inventory_action_decider")
                .description("Synthesizes evidence, checks policy, and drafts an inventory action recommendation.")
                .model(modelName)
                .instruction("""
                        You are the final inventory-action coordinator.
                        Review the graph, spatial, and external evidence already gathered in this session.
                        Your job is to recommend one next step: transfer, expedite, substitute, or hold.
                        If a transfer is the best next move, call checkTransferPolicy first and then call draftInventoryTransferAction.
                        Never claim that an inventory move has been executed.
                        Your final answer must include:
                        1. Recommended action.
                        2. Why that action is justified from the evidence.
                        3. Whether approval is required.
                        4. If you drafted a move, the draft action id and the proposed source, destination, and units.
                        If evidence is missing, say so plainly and recommend the safest next step.
                        """)
                .tools(policyTool, draftActionTool)
                .build();

        SequentialAgent rootAgent = SequentialAgent.builder()
                .name("inventory_action_orchestrator")
                .description("Coordinates final-stage inventory action planning using evidence specialists and a decision agent.")
                .subAgents(parallelEvidenceAgent, decisionAgent)
                .build();

        this.runner = new InMemoryRunner(rootAgent, APP_NAME);
    }

    public InventoryActionResult run(String userInput, String contextId) {
        String normalizedInput = userInput == null || userInput.isBlank()
                ? "Recommend an inventory action for " + DemoInventoryData.DEFAULT_PRODUCT_ID + "."
                : userInput.trim();
        if (!containsProductId(normalizedInput)) {
            normalizedInput = normalizedInput
                    + "\nUse " + DemoInventoryData.DEFAULT_PRODUCT_ID
                    + " as the default product id when the request does not specify one.";
        }
        try {
            return runAdk(normalizedInput, contextId);
        } catch (Exception exception) {
            return runDeterministicFallback(normalizedInput, exception);
        }
    }

    private InventoryActionResult runAdk(String normalizedInput, String contextId) {
        String userId = firstNonBlank(contextId, "inventory-action-user");
        String sessionId = firstNonBlank(contextId, "inventory-action-session");
        Session session = ensureSession(userId, sessionId);

        Flowable<Event> eventStream = runner.runAsync(
                userId,
                session.id(),
                Content.fromParts(Part.fromText(normalizedInput)),
                RunConfig.builder().build(),
                Map.of()
        );

        List<String> trace = new ArrayList<>();
        String[] finalText = new String[] {""};

        eventStream.blockingForEach(event -> {
            String content = event.stringifyContent();
            if (!content.isBlank()) {
                trace.add(content);
            }
            if (event.finalResponse() && !content.isBlank()) {
                finalText[0] = content;
            }
        });

        String resolvedText = !finalText[0].isBlank()
                ? finalText[0]
                : trace.stream().filter(text -> !text.isBlank()).reduce((left, right) -> right).orElse(
                        "The inventory action coordinator did not return a final recommendation."
                );

        return new InventoryActionResult(resolvedText, trace, "adk");
    }

    private InventoryActionResult runDeterministicFallback(String userInput, Exception exception) {
        String productId = extractProductId(userInput);
        Map<String, Object> graphEvidence = inventoryActionTools.getGraphEvidence(productId);
        Map<String, Object> spatialEvidence = inventoryActionTools.getSpatialEvidence(productId);
        Map<String, Object> externalSignals = inventoryActionTools.getExternalSignals(productId);

        String sourceWarehouse = stringValue(spatialEvidence.get("recommendedSourceWarehouse"));
        String destinationWarehouse = stringValue(spatialEvidence.get("recommendedDestinationWarehouse"));
        int units = intValue(spatialEvidence.get("suggestedTransferUnits"), 250);

        String activeAlert = stringValue(graphEvidence.get("activeAlert"));
        String signalSummary = stringValue(externalSignals.get("signalSummary"));
        String combinedReason = combinedReason(activeAlert, signalSummary);

        Map<String, Object> policyResult = inventoryActionTools.checkTransferPolicy(
                productId,
                sourceWarehouse,
                destinationWarehouse,
                units,
                combinedReason
        );
        Map<String, Object> draftResult = inventoryActionTools.draftInventoryTransferAction(
                productId,
                sourceWarehouse,
                destinationWarehouse,
                units,
                combinedReason
        );

        String approvalLine = Boolean.TRUE.equals(policyResult.get("requiresApproval"))
                ? "Approval is required before execution."
                : "Only standard review is required before execution.";
        String dependencyPath = stringValue(graphEvidence.get("dependencyPath"));
        String hotspotSummary = stringValue(spatialEvidence.get("hotspotSummary"));

        List<String> rationaleParts = new ArrayList<>();
        if (!dependencyPath.isBlank()) {
            rationaleParts.add(toSentence(dependencyPath));
        }
        if (!activeAlert.isBlank()) {
            rationaleParts.add(toSentence(activeAlert));
        }
        if (!hotspotSummary.isBlank()) {
            rationaleParts.add(toSentence(hotspotSummary));
        }
        if (!signalSummary.isBlank()) {
            rationaleParts.add(toSentence(signalSummary));
        }

        String responseText = "Fallback recommendation for " + productId + ": transfer "
                + units + " units from " + sourceWarehouse + " to " + destinationWarehouse + ". "
                + "Why: " + String.join(" ", rationaleParts) + " "
                + toSentence(approvalLine) + " Draft action id: "
                + stringValue(draftResult.get("draftActionId")) + ". "
                + "Policy check: " + toSentence(stringValue(policyResult.get("policySummary"))) + " "
                + "The ADK model path was unavailable, so this response used deterministic local orchestration instead ("
                + toParenthetical(exception.getMessage()) + ").";

        List<String> trace = List.of(
                "graphEvidence=" + graphEvidence,
                "spatialEvidence=" + spatialEvidence,
                "externalSignals=" + externalSignals,
                "policyResult=" + policyResult,
                "draftResult=" + draftResult
        );

        return new InventoryActionResult(responseText, trace, "deterministic-fallback", draftResult, policyResult);
    }

    private Session ensureSession(String userId, String sessionId) {
        return runner.sessionService()
                .getSession(APP_NAME, userId, sessionId, Optional.empty())
                .switchIfEmpty(
                        runner.sessionService()
                                .createSession(APP_NAME, userId, new ConcurrentHashMap<>(), sessionId)
                                .toMaybe()
                )
                .blockingGet();
    }

    private static String firstNonBlank(String... candidates) {
        if (candidates == null) {
            return "";
        }
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate.trim();
            }
        }
        return "";
    }

    private static String extractProductId(String userInput) {
        Matcher matcher = PRODUCT_ID_PATTERN.matcher(userInput == null ? "" : userInput.toUpperCase());
        if (matcher.find()) {
            return matcher.group(1);
        }
        return DemoInventoryData.DEFAULT_PRODUCT_ID;
    }

    private static boolean containsProductId(String userInput) {
        return PRODUCT_ID_PATTERN.matcher(userInput == null ? "" : userInput.toUpperCase()).find();
    }

    private static String stringValue(Object value) {
        return value == null ? "" : value.toString();
    }

    private static int intValue(Object value, int defaultValue) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(stringValue(value));
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    private static String combinedReason(String activeAlert, String signalSummary) {
        List<String> reasonParts = new ArrayList<>();
        if (!stringValue(activeAlert).isBlank()) {
            reasonParts.add("Graph: " + toParenthetical(activeAlert) + ".");
        }
        if (!stringValue(signalSummary).isBlank()) {
            reasonParts.add("External: " + toParenthetical(signalSummary) + ".");
        }
        if (reasonParts.isEmpty()) {
            return "Inventory risk evidence supports a balancing transfer.";
        }
        return String.join(" ", reasonParts);
    }

    private static String toSentence(String value) {
        String normalized = stringValue(value).trim();
        if (normalized.isBlank()) {
            return "";
        }
        while (normalized.endsWith(".") || normalized.endsWith("!") || normalized.endsWith("?")) {
            normalized = normalized.substring(0, normalized.length() - 1).trim();
        }
        return normalized + ".";
    }

    private static String toParenthetical(String value) {
        String normalized = stringValue(value).trim();
        if (normalized.isBlank()) {
            return "unknown error";
        }
        while (normalized.endsWith(".") || normalized.endsWith("!") || normalized.endsWith("?")) {
            normalized = normalized.substring(0, normalized.length() - 1).trim();
        }
        return normalized;
    }

    public record InventoryActionResult(
            String responseText,
            List<String> trace,
            String orchestrationMode,
            Map<String, Object> draftAction,
            Map<String, Object> policyResult
    ) {
        public InventoryActionResult(String responseText, List<String> trace, String orchestrationMode) {
            this(responseText, trace, orchestrationMode, Map.of(), Map.of());
        }
    }
}
