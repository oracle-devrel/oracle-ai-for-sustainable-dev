package com.oracle.demo.interactiveai;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
final class AgentController {
    private final AgentRuntime runtime;

    AgentController(AgentRuntime runtime) {
        this.runtime = runtime;
    }

    @GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    String health() {
        return Json.value(Map.of(
                "status", "UP",
                "mode", "mcp",
                "backend", "oracle-db-mcp-java-toolkit",
                "useCase", "supply-chain-inventory-exchange"));
    }

    @GetMapping(value = "/recommendations", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<String> recommendations(
            @RequestParam(defaultValue = "70") double minimumStockoutRisk,
            @RequestParam(defaultValue = "10") int maximumRows,
            @RequestParam(defaultValue = "full") String accessProfile) {
        validate(minimumStockoutRisk, maximumRows);
        String profile = runtime.accessProfile(Map.of("accessProfile", accessProfile));
        List<TransferRecommendation> recommendations = runtime.repository(profile)
                .findTransferRecommendations(minimumStockoutRisk, maximumRows);
        return noStore(Main.recommendationsJson(recommendations));
    }

    @PostMapping(value = "/runs", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    void run(
            @RequestParam MultiValueMap<String, String> form,
            HttpServletResponse response) throws IOException {
        double risk = doubleValue(form, "minimumStockoutRisk", "70");
        int rows = intValue(form, "maximumRows", "10");
        validate(risk, rows);
        String profile = runtime.accessProfile(values(form));
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("text/event-stream;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        new AguiRunService(runtime.repository(profile), runtime.approvals()).stream(
                response.getOutputStream(), risk, rows, runtime.scopedActor(profile));
    }

    @PostMapping(
            value = "/reviews",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<String> review(@RequestParam MultiValueMap<String, String> form) {
        double risk = doubleValue(form, "minimumStockoutRisk", "70");
        int rows = intValue(form, "maximumRows", "10");
        validate(risk, rows);
        String profile = runtime.accessProfile(values(form));
        List<TransferRecommendation> recommendations = runtime.repository(profile)
                .findTransferRecommendations(risk, rows);
        String approvalId = runtime.approvals().issue(
                runtime.scopedActor(profile), recommendations);
        return noStore(Main.reviewJson(
                recommendations,
                approvalId,
                runtime.repository(profile).writesAllowed()));
    }

    @PostMapping(
            value = "/approve",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    String approve(@RequestParam MultiValueMap<String, String> form) {
        Map<String, String> values = values(form);
        String profile = runtime.accessProfile(values);
        String recommendationId = required(values, "recommendationId");
        String approvalId = required(values, "approvalId");
        String notes = required(values, "approvalNotes");
        String actor = runtime.scopedActor(profile);
        TransferRecommendation recommendation = runtime.approvals().consume(
                approvalId, recommendationId, actor);
        InputValidation.approval(recommendation, notes, actor);
        TransferResult result = runtime.repository(profile).approveTransfer(
                recommendation, notes, actor);
        System.out.printf(
                "audit tool=approve-inventory-transfer actor=%s "
                        + "recommendationId=%s result=APPROVED transferId=%d%n",
                actor, recommendationId, result.transferId());
        return Json.value(Map.of(
                "transferId", result.transferId(),
                "recommendationId", result.recommendationId(),
                "transferQuantity", result.transferQuantity(),
                "status", result.status()));
    }

    @PostMapping(
            value = "/reject",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    String reject(@RequestParam MultiValueMap<String, String> form) {
        Map<String, String> values = values(form);
        String profile = runtime.accessProfile(values);
        String actor = runtime.scopedActor(profile);
        runtime.approvals().reject(required(values, "approvalId"), actor);
        System.out.printf(
                "audit tool=approve-inventory-transfer actor=%s result=REJECTED%n",
                actor);
        return "{\"status\":\"REJECTED\"}";
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<String> badRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(Json.value(Map.of("error", exception.getMessage())));
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<String> serverError(IllegalStateException exception) {
        return ResponseEntity.internalServerError()
                .contentType(MediaType.APPLICATION_JSON)
                .body(Json.value(Map.of("error", exception.getMessage())));
    }

    private static ResponseEntity<String> noStore(String body) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    private static void validate(double risk, int rows) {
        InputValidation.minimumStockoutRisk(risk);
        InputValidation.maximumRows(rows);
    }

    private static double doubleValue(
            MultiValueMap<String, String> form, String name, String fallback) {
        String value = form.getFirst(name);
        return Double.parseDouble(value == null ? fallback : value);
    }

    private static int intValue(
            MultiValueMap<String, String> form, String name, String fallback) {
        String value = form.getFirst(name);
        return Integer.parseInt(value == null ? fallback : value);
    }

    private static Map<String, String> values(MultiValueMap<String, String> form) {
        return form.toSingleValueMap();
    }

    private static String required(Map<String, String> values, String name) {
        String value = values.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }
}
