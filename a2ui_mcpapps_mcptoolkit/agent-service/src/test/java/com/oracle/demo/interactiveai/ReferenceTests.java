package com.oracle.demo.interactiveai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class ReferenceTests {
    @Test
    void sqlSetupSplitterPreservesSemicolonsInsideText() {
        List<String> statements = DatabaseSetup.splitStatements(
                "INSERT INTO notes VALUES ('First; still first'); "
                        + "SELECT 1 FROM dual;");
        assertEquals(2, statements.size());
        assertTrue(statements.getFirst().contains("First; still first"));
    }

    @Test
    void ucpConfigurationUsesFinancialDatabaseDefaults() throws Exception {
        var dataSource = UcpDataSourceConfiguration.fromEnvironment(Map.of(
                "TNS_ADMIN", "/tmp/Wallet_financialdb",
                "DB_PASSWORD", "test-only-placeholder"));
        assertEquals("FINANCIAL", dataSource.getUser());
        assertEquals(
                "jdbc:oracle:thin:@financialdb_high"
                        + "?TNS_ADMIN=/tmp/Wallet_financialdb",
                dataSource.getURL());
        assertEquals(
                "InteractiveAiFinancialUcpPool",
                dataSource.getConnectionPoolName());
        assertEquals(4, dataSource.getMaxPoolSize());
    }

    @Test
    void ucpConfigurationSupportsAnotherAutonomousDatabaseService()
            throws Exception {
        var dataSource = UcpDataSourceConfiguration.fromEnvironment(Map.of(
                "TNS_ADMIN", "/var/run/oracle-wallet",
                "DB_SERVICE_NAME", "paulparkdb_tp",
                "DB_USERNAME", "FINANCIAL",
                "DB_PASSWORD", "test-only-placeholder"));
        assertEquals(
                "jdbc:oracle:thin:@paulparkdb_tp"
                        + "?TNS_ADMIN=/var/run/oracle-wallet",
                dataSource.getURL());
    }

    @Test
    void transferRecommendationsAreBoundedAndOrdered() {
        var repository = new DemoSupplyChainRepository();
        List<TransferRecommendation> recommendations =
                repository.findTransferRecommendations(85, 2);
        assertEquals(2, recommendations.size());
        assertTrue(
                recommendations.get(0).stockoutRiskScore()
                        >= recommendations.get(1).stockoutRiskScore());
    }

    @Test
    void invalidInputIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new DemoSupplyChainRepository()
                        .findTransferRecommendations(101, 10));
        assertThrows(
                IllegalArgumentException.class,
                () -> InputValidation.approval(
                        null,
                        "bad",
                        "planner@example.com"));
        assertEquals(
                "FINANCIAL",
                DatabaseUserBootstrap.validatedIdentifier("financial"));
        assertThrows(
                IllegalArgumentException.class,
                () -> DatabaseUserBootstrap.validatedIdentifier(
                        "FINANCIAL; DROP USER ADMIN"));
    }

    @Test
    void rejectedApprovalMakesNoWrite() {
        var repository = new DemoSupplyChainRepository();
        var approvals = new ApprovalService();
        String id = approvals.issue(
                "planner@example.com",
                repository.findTransferRecommendations(70, 3));
        approvals.reject(id, "planner@example.com");
        assertEquals(0, repository.transferCount());
    }

    @Test
    void approvedTransferIsBoundAndSingleUse() {
        var repository = new DemoSupplyChainRepository();
        var approvals = new ApprovalService();
        List<TransferRecommendation> recommendations =
                repository.findTransferRecommendations(70, 3);
        TransferRecommendation recommendation = recommendations.getFirst();
        String id = approvals.issue(
                "planner@example.com",
                recommendations);
        TransferRecommendation approved = approvals.consume(
                id,
                recommendation.recommendationId(),
                "planner@example.com");
        repository.approveTransfer(
                approved,
                "Rebalance inventory before forecast demand.",
                "planner@example.com");
        assertEquals(1, repository.transferCount());
        assertThrows(
                IllegalArgumentException.class,
                () -> approvals.consume(
                        id,
                        recommendation.recommendationId(),
                        "planner@example.com"));
    }

    @Test
    void unknownRecommendationCannotBeApproved() {
        var repository = new DemoSupplyChainRepository();
        var approvals = new ApprovalService();
        String id = approvals.issue(
                "planner@example.com",
                repository.findTransferRecommendations(70, 3));
        assertThrows(
                IllegalArgumentException.class,
                () -> approvals.consume(
                        id,
                        "9999:999:998",
                        "planner@example.com"));
    }

    @Test
    void streamUsesOfficialEventNamesAndA2uiEnvelope() throws Exception {
        var output = new ByteArrayOutputStream();
        new AguiRunService(
                        new DemoSupplyChainRepository(),
                        new ApprovalService())
                .stream(
                        output,
                        70,
                        3,
                        "planner@example.com");
        String stream = output.toString(StandardCharsets.UTF_8);
        assertTrue(stream.contains("\"type\":\"RUN_STARTED\""));
        assertTrue(stream.contains("\"type\":\"TOOL_CALL_RESULT\""));
        assertTrue(stream.contains("\"name\":\"a2ui.message\""));
        assertTrue(stream.contains("\"version\":\"v0.9.1\""));
        assertTrue(stream.contains("find-stockout-transfer-recommendations"));
    }

    @Test
    void a2uiHasNoArbitraryActionPicker() {
        String json = Json.value(A2uiPayloads.components());
        assertTrue(json.contains("inventory-transfer-review"));
        assertTrue(json.contains("\"component\":\"Image\""));
        assertTrue(json.contains("https://www.oracle.com/a/ocom/img/oracle-logo.svg"));
        assertTrue(json.contains("recommendationList"));
        assertTrue(json.contains("Approve inventory transfer"));
        assertFalse(json.contains("ChoicePicker"));
        assertFalse(json.contains("FREEZE_CHANGES"));
    }

    @Test
    void governedPayloadIdentifiesToolkitSource() {
        TransferRecommendation recommendation =
                new DemoSupplyChainRepository()
                        .findTransferRecommendations(90, 1)
                        .getFirst();
        String payload = Main.recommendationsJson(
                List.of(recommendation));
        assertTrue(payload.contains(
                "\"source\":\"oracle-db-mcp-java-toolkit\""));
        assertTrue(payload.contains(
                "\"sku\":\"BAT-48V\""));
    }

    @Test
    void reviewPayloadCarriesAppOnlyApprovalHandle() {
        TransferRecommendation recommendation =
                new DemoSupplyChainRepository()
                        .findTransferRecommendations(90, 1)
                        .getFirst();
        String payload = Main.reviewJson(
                List.of(recommendation),
                "test-approval-handle",
                true);
        assertTrue(payload.contains(
                "\"source\":\"oracle-db-mcp-java-toolkit\""));
        assertTrue(payload.contains(
                "\"approvalId\":\"test-approval-handle\""));
        assertTrue(payload.contains(
                "\"recommendationId\":\"1001:102:101\""));
    }
}
