package com.oracle.demo.interactiveai;

import java.util.List;
import java.util.Map;

public final class A2uiPayloads {
    public static final String VERSION = "v0.9.1";
    public static final String CATALOG =
            "https://a2ui.org/specification/v0_9_1/catalogs/basic/catalog.json";

    private A2uiPayloads() {
    }

    public static Map<String, Object> createSurface() {
        return Map.of("version", VERSION, "createSurface", Map.of(
                "surfaceId", "inventory-transfer-review",
                "catalogId", CATALOG,
                "sendDataModel", true,
                "theme", Map.of("primaryColor", "#c74634")));
    }

    public static Map<String, Object> components() {
        List<Map<String, Object>> components = List.of(
                Map.of(
                        "id", "root",
                        "component", "Column",
                        "children", List.of(
                                "banner",
                                "recommendationList",
                                "notes",
                                "buttons")),
                Map.of(
                        "id", "banner",
                        "component", "Text",
                        "text", Map.of("path", "/summary")),
                Map.of(
                        "id", "recommendationList",
                        "component", "List",
                        "children", Map.of(
                                "path", "/recommendations",
                                "componentId", "recommendationCard")),
                Map.of(
                        "id", "recommendationCard",
                        "component", "Card",
                        "child", "recommendationText"),
                Map.of(
                        "id", "recommendationText",
                        "component", "Text",
                        "text", Map.of(
                                "call", "formatString",
                                "args", Map.of(
                                        "value",
                                        "${sku}: move ${recommendedTransferQuantity} units from ${sourceLocationCode} to ${targetLocationCode}"))),
                Map.of(
                        "id", "notes",
                        "component", "TextField",
                        "label", "Transfer approval notes",
                        "value", Map.of("path", "/form/approvalNotes")),
                Map.of(
                        "id", "buttons",
                        "component", "Row",
                        "children", List.of("confirm", "cancel")),
                Map.of(
                        "id", "confirm",
                        "component", "Button",
                        "text", "Approve inventory transfer",
                        "variant", "primary",
                        "action", Map.of("event", Map.of(
                                "name", "approve_inventory_transfer"))),
                Map.of(
                        "id", "cancel",
                        "component", "Button",
                        "text", "Cancel",
                        "variant", "borderless",
                        "action", Map.of("event", Map.of(
                                "name", "reject_inventory_transfer"))));
        return Map.of(
                "version", VERSION,
                "updateComponents", Map.of(
                        "surfaceId", "inventory-transfer-review",
                        "components", components));
    }

    public static Map<String, Object> data(
            List<TransferRecommendation> recommendations,
            String approvalId) {
        List<Map<String, Object>> rows =
                recommendations.stream().map(Json::recommendationMap).toList();
        return Map.of(
                "version", VERSION,
                "updateDataModel", Map.of(
                        "surfaceId", "inventory-transfer-review",
                        "path", "/",
                        "value", Map.of(
                                "summary",
                                recommendations.size()
                                        + " governed inventory transfer recommendation(s) require review.",
                                "recommendations", rows,
                                "approvalId", approvalId,
                                "form", Map.of(
                                        "approvalNotes",
                                        "Approve the database-recommended transfer to reduce stockout exposure."))));
    }
}
