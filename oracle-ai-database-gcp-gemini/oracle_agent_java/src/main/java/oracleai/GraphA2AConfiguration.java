package oracleai;

import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.events.EventQueue;
import io.a2a.server.tasks.TaskUpdater;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentSkill;
import io.a2a.spec.FilePart;
import io.a2a.spec.FileWithBytes;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.Part;
import io.a2a.spec.TaskNotCancelableError;
import io.a2a.spec.TextPart;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javax.imageio.ImageIO;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class GraphA2AConfiguration {

    @Bean
    AgentCard agentCard(Environment environment) {
        return buildGraphAgentCard(environment);
    }

    static AgentCard buildGraphAgentCard(Environment environment) {
        return buildAgentCard(
                environment,
                "Oracle Database Property Graph Agent",
                "Oracle Database property graph specialist for product SKUs such as SKU-500. Generates Oracle-backed supply chain dependency graphs, supplier-to-warehouse paths, upstream and downstream relationship analysis, and active disruption alerts as a text summary plus PNG image."
        );
    }

    static AgentCard buildSpatialAliasCard(Environment environment) {
        return buildAgentCard(
                environment,
                "oracle_spatial_agent",
                "Spatial import alias served by the shared Oracle Java agent runtime. Current behavior still routes to the graph-oriented implementation."
        );
    }

    private static AgentCard buildAgentCard(
            Environment environment,
            String agentName,
            String description
    ) {
        String publicProtocol = valueOrDefault(environment, "PUBLIC_PROTOCOL", "http");
        String publicHost = valueOrDefault(environment, "PUBLIC_HOST", "localhost");
        String graphPort = valueOrDefault(
                environment,
                "GRAPH_AGENT_PORT",
                valueOrDefault(environment, "PORT", "8081")
        );
        String baseUrl = firstNonBlank(
                environment.getProperty("GRAPH_AGENT_URL"),
                environment.getProperty("A2A_URL"),
                String.format("%s://%s:%s", publicProtocol, publicHost, graphPort)
        );
        String graphUrl = appendPath(baseUrl, "/graph");

        return new AgentCard(
                agentName,
                description,
                graphUrl,
                null,
                "0.1.0",
                null,
                new AgentCapabilities(false, false, false, List.of()),
                List.of("text/plain"),
                List.of("image/png", "text/plain"),
                List.of(
                        new AgentSkill(
                                "oracle_graph_agent",
                                "supply-chain-dependency-graph-specialist",
                                "Specialist in Oracle Graph dependency analysis for product SKUs such as SKU-500. Use this when a user asks for a supply chain dependency graph, supplier-to-warehouse path, upstream or downstream relationships, or active disruption alerts for a product.",
                                List.of(
                                        "llm",
                                        "oracle",
                                        "database",
                                        "property-graph",
                                        "supply-chain",
                                        "dependencies",
                                        "sku",
                                        "image"
                                ),
                                List.of(
                                        "Show the supply chain dependency graph for SKU-500 and explain the active alert.",
                                        "Use the Oracle Database property graph to show supply chain dependencies for SKU-500 and render the graph as an image.",
                                        "For SKU-500, show the supplier-to-warehouse dependency graph and explain the current disruption.",
                                        "Visualize the upstream supplier path for SKU-500 and highlight the risky node.",
                                        "For SKU-500, map the dependency relationships from supplier to warehouse to retailer."
                                ),
                                List.of("text/plain"),
                                List.of("image/png", "text/plain"),
                                null
                        ),
                        new AgentSkill(
                                "oracle_graph_agent-getSupplyChainDependencies",
                                "getSupplyChainDependencies",
                                "Fetches Oracle Property Graph supply chain dependencies and active alert context for a specific product ID such as SKU-500.",
                                List.of(
                                        "llm",
                                        "tools",
                                        "oracle",
                                        "database",
                                        "property-graph",
                                        "sku",
                                        "dependencies"
                                ),
                                List.of(
                                        "Get the dependency graph for SKU-500.",
                                        "Use Oracle Database property graph data for SKU-500.",
                                        "Show the Oracle graph path for SKU-500.",
                                        "Find the supply chain dependencies for SKU-500."
                                ),
                                List.of("text/plain"),
                                List.of("image/png"),
                                null
                        )
                ),
                false,
                null,
                null,
                null,
                List.of(),
                "JSONRPC",
                "0.3.0",
                null
        );
    }

    @Bean
    AgentExecutor agentExecutor(
            Function<GraphTools.GraphRequest, GraphTools.GraphResponse> getSupplyChainDependencies,
            GraphRequestParser graphRequestParser
    ) {
        return buildAgentExecutor(getSupplyChainDependencies, graphRequestParser);
    }

    static AgentExecutor buildAgentExecutor(
            Function<GraphTools.GraphRequest, GraphTools.GraphResponse> getSupplyChainDependencies,
            GraphRequestParser graphRequestParser
    ) {
        return new AgentExecutor() {
            @Override
            public void execute(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
                TaskUpdater updater = new TaskUpdater(context, eventQueue);
                if (context.getTask() == null) {
                    updater.submit();
                }

                updater.startWork();

                try {
                    String userInput = context.getUserInput("");
                    GraphTools.GraphRequest graphRequest = graphRequestParser.parse(userInput);
                    GraphTools.GraphResponse graphResponse =
                            getSupplyChainDependencies.apply(graphRequest);
                    String responseText = formatResponse(graphResponse);
                    Part<?> imagePart = new FilePart(
                            new FileWithBytes(
                                    "image/png",
                                    "supply-chain-graph.png",
                                    renderGraphPng(graphResponse.productId(), graphResponse)
                            )
                    );

                    updater.addArtifact(
                            List.of(imagePart),
                            null,
                            "supply_chain_graph_png",
                            Map.of(
                                    "productId", graphResponse.productId(),
                                    "contentType", "image/png",
                                    "sourceMode", graphResponse.sourceMode()
                            )
                    );
                    updater.complete(
                            updater.newAgentMessage(
                                    List.of(new TextPart(responseText)),
                                    Map.of(
                                            "tool", "getSupplyChainDependencies",
                                            "artifactName", "supply-chain-graph.png",
                                            "sourceMode", graphResponse.sourceMode()
                                    )
                            )
                    );
                } catch (Exception e) {
                    updater.fail(
                            updater.newAgentMessage(
                                    List.of(new TextPart("Graph agent failed: " + e.getMessage())),
                                    Map.of("error", "graph_agent_execution_failed")
                            )
                    );
                    throw new JSONRPCError(-32603, "Graph agent execution failed: " + e.getMessage(), null);
                }
            }

            @Override
            public void cancel(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
                if (context.getTask() != null && context.getTask().getStatus() != null) {
                    throw new TaskNotCancelableError();
                }
                throw new TaskNotCancelableError();
            }
        };
    }

    static String formatResponse(GraphTools.GraphResponse graphResponse) {
        String productId = graphResponse.productId();
        List<String> route = new ArrayList<>();
        addRouteLabel(route, graphResponse, "SUPPLIER");
        addRouteLabel(route, graphResponse, "PLANT");
        addRouteLabel(route, graphResponse, "PORT");
        addRouteLabel(route, graphResponse, "WAREHOUSE");
        addRouteLabel(route, graphResponse, "PRODUCT");

        String routeSummary = route.isEmpty()
                ? "No dependency path found"
                : String.join(" -> ", route);

        Map<String, String> alertNode = findNodeByType(graphResponse, "ALERT");
        if (!alertNode.isEmpty()) {
            return "Dependencies for " + productId + ": " + routeSummary
                    + ". Active signal: " + alertNode.getOrDefault("label", "Alert")
                    + " | " + alertNode.getOrDefault("metric", "Risk unavailable")
                    + ". Data source: " + graphResponse.sourceDetail() + ".";
        }

        return "Dependencies for " + productId + ": " + routeSummary
                + ". Data source: " + graphResponse.sourceDetail() + ".";
    }

    static String renderGraphPng(String productId, GraphTools.GraphResponse graphResponse) throws Exception {
        int width = 1480;
        int height = 940;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();

        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            drawBackground(graphics, width, height);
            drawHeader(graphics, width, productId, graphResponse);

            List<NodeCard> nodeCards = layoutNodeCards(graphResponse);
            drawEdges(graphics, nodeCards, graphResponse.edges());
            for (NodeCard nodeCard : nodeCards) {
                drawNodeCard(graphics, nodeCard);
            }

            drawSummaryPanel(graphics, productId, graphResponse, width);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "png", outputStream);
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } finally {
            graphics.dispose();
        }
    }

    private static void drawBackground(Graphics2D graphics, int width, int height) {
        graphics.setPaint(new GradientPaint(
                0,
                0,
                new Color(0xF8F4EC),
                width,
                height,
                new Color(0xE2E8F0)
        ));
        graphics.fillRect(0, 0, width, height);

        graphics.setColor(new Color(0xD9E2EC));
        for (int x = 80; x < width; x += 120) {
            graphics.drawLine(x, 140, x, height - 40);
        }
        for (int y = 160; y < height; y += 100) {
            graphics.drawLine(40, y, width - 40, y);
        }
    }

    private static void drawHeader(
            Graphics2D graphics,
            int width,
            String productId,
            GraphTools.GraphResponse graphResponse
    ) {
        graphics.setColor(new Color(0x102A43));
        graphics.fill(new RoundRectangle2D.Double(32, 28, width - 64, 112, 36, 36));

        graphics.setColor(new Color(0xF8FAFC));
        graphics.setFont(new Font("SansSerif", Font.BOLD, 34));
        graphics.drawString("Oracle Graph Dependency View", 62, 78);

        graphics.setColor(new Color(0xCBD5E1));
        graphics.setFont(new Font("SansSerif", Font.PLAIN, 18));
        graphics.drawString(headerSubtitle(productId, graphResponse), 62, 112);

        drawHeaderChip(graphics, width - 360, 56, "Nodes " + graphResponse.nodes().size());
        drawHeaderChip(graphics, width - 228, 56, "Edges " + graphResponse.edges().size());
        drawHeaderChip(graphics, width - 96, 56, "PNG");
    }

    private static void drawHeaderChip(Graphics2D graphics, int centerX, int centerY, String label) {
        Font chipFont = new Font("SansSerif", Font.BOLD, 15);
        graphics.setFont(chipFont);
        FontMetrics metrics = graphics.getFontMetrics();
        int width = metrics.stringWidth(label) + 28;
        int height = 30;
        int x = centerX - (width / 2);
        int y = centerY - (height / 2);

        graphics.setColor(new Color(0x173F5F));
        graphics.fill(new RoundRectangle2D.Double(x, y, width, height, 18, 18));
        graphics.setColor(new Color(0xE2E8F0));
        graphics.drawString(label, x + 14, y + 20);
    }

    private static List<NodeCard> layoutNodeCards(GraphTools.GraphResponse graphResponse) {
        List<NodeCard> cards = new ArrayList<>();
        Map<String, int[]> positions = new LinkedHashMap<>();
        positions.put("SUPPLIER", new int[]{90, 250});
        positions.put("PLANT", new int[]{370, 130});
        positions.put("PORT", new int[]{680, 130});
        positions.put("WAREHOUSE", new int[]{1000, 250});
        positions.put("PRODUCT", new int[]{1010, 510});
        positions.put("ALERT", new int[]{690, 510});

        int fallbackIndex = 0;
        for (Map<String, String> node : graphResponse.nodes()) {
            String type = node.getOrDefault("type", "NODE");
            int[] position = positions.get(type);
            if (position == null) {
                position = new int[]{90 + ((fallbackIndex % 4) * 300), 250 + ((fallbackIndex / 4) * 180)};
                fallbackIndex++;
            }
            cards.add(
                    new NodeCard(
                            node.getOrDefault("id", "node-" + cards.size()),
                            type,
                            node.getOrDefault("label", type),
                            node.getOrDefault("detail", ""),
                            node.getOrDefault("metric", ""),
                            position[0],
                            position[1],
                            260,
                            118,
                            fillColorForType(type),
                            accentColorForType(type)
                    )
            );
        }
        return cards;
    }

    private static void drawEdges(
            Graphics2D graphics,
            List<NodeCard> nodeCards,
            List<Map<String, String>> edges
    ) {
        Map<String, NodeCard> nodesById = new LinkedHashMap<>();
        for (NodeCard nodeCard : nodeCards) {
            nodesById.put(nodeCard.id(), nodeCard);
        }

        for (Map<String, String> edge : edges) {
            NodeCard from = nodesById.get(edge.get("from"));
            NodeCard to = nodesById.get(edge.get("to"));
            if (from == null || to == null) {
                continue;
            }
            drawEdge(graphics, from, to, edge.getOrDefault("label", "RELATED_TO"));
        }
    }

    private static void drawEdge(
            Graphics2D graphics,
            NodeCard from,
            NodeCard to,
            String label
    ) {
        int fromCenterX = from.x() + (from.width() / 2);
        int fromCenterY = from.y() + (from.height() / 2);
        int toCenterX = to.x() + (to.width() / 2);
        int toCenterY = to.y() + (to.height() / 2);

        int deltaX = toCenterX - fromCenterX;
        int deltaY = toCenterY - fromCenterY;

        int startX;
        int startY;
        int endX;
        int endY;

        if (Math.abs(deltaX) >= Math.abs(deltaY)) {
            startX = deltaX >= 0 ? from.x() + from.width() : from.x();
            startY = fromCenterY;
            endX = deltaX >= 0 ? to.x() : to.x() + to.width();
            endY = toCenterY;
        } else {
            startX = fromCenterX;
            startY = deltaY >= 0 ? from.y() + from.height() : from.y();
            endX = toCenterX;
            endY = deltaY >= 0 ? to.y() : to.y() + to.height();
        }

        graphics.setColor(new Color(0xC2410C));
        graphics.setStroke(new BasicStroke(4.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.drawLine(startX, startY, endX, endY);
        drawArrowHead(graphics, startX, startY, endX, endY);
        drawEdgeLabel(graphics, label, (startX + endX) / 2, (startY + endY) / 2);
    }

    private static void drawArrowHead(
            Graphics2D graphics,
            int startX,
            int startY,
            int endX,
            int endY
    ) {
        double angle = Math.atan2(endY - startY, endX - startX);
        int arrowLength = 16;
        int arrowWidth = 7;

        int x1 = endX - (int) (arrowLength * Math.cos(angle - (Math.PI / 6)));
        int y1 = endY - (int) (arrowLength * Math.sin(angle - (Math.PI / 6)));
        int x2 = endX - (int) (arrowLength * Math.cos(angle + (Math.PI / 6)));
        int y2 = endY - (int) (arrowLength * Math.sin(angle + (Math.PI / 6)));

        Polygon arrow = new Polygon();
        arrow.addPoint(endX, endY);
        arrow.addPoint(x1, y1);
        arrow.addPoint(x2, y2);

        graphics.fillPolygon(arrow);
        graphics.fillOval(endX - arrowWidth, endY - arrowWidth, arrowWidth * 2, arrowWidth * 2);
    }

    private static void drawEdgeLabel(Graphics2D graphics, String label, int x, int y) {
        Font font = new Font("SansSerif", Font.BOLD, 14);
        graphics.setFont(font);
        FontMetrics metrics = graphics.getFontMetrics();
        int width = metrics.stringWidth(label) + 22;
        int height = 28;
        int left = x - (width / 2);
        int top = y - (height / 2);

        graphics.setColor(new Color(0xFFF7ED));
        graphics.fill(new RoundRectangle2D.Double(left, top, width, height, 16, 16));
        graphics.setColor(new Color(0xC2410C));
        graphics.setStroke(new BasicStroke(1.5f));
        graphics.draw(new RoundRectangle2D.Double(left, top, width, height, 16, 16));
        graphics.drawString(label, left + 11, top + 19);
    }

    private static void drawNodeCard(Graphics2D graphics, NodeCard nodeCard) {
        RoundRectangle2D.Double shape = new RoundRectangle2D.Double(
                nodeCard.x(),
                nodeCard.y(),
                nodeCard.width(),
                nodeCard.height(),
                28,
                28
        );

        graphics.setColor(nodeCard.fillColor());
        graphics.fill(shape);
        graphics.setColor(nodeCard.accentColor());
        graphics.setStroke(new BasicStroke(3f));
        graphics.draw(shape);

        drawNodeTypeChip(graphics, nodeCard);

        graphics.setColor(new Color(0x0F172A));
        graphics.setFont(new Font("SansSerif", Font.BOLD, 20));
        graphics.drawString(
                truncate(graphics, nodeCard.label(), nodeCard.width() - 34),
                nodeCard.x() + 18,
                nodeCard.y() + 62
        );

        graphics.setColor(new Color(0x475569));
        graphics.setFont(new Font("SansSerif", Font.PLAIN, 15));
        graphics.drawString(
                truncate(graphics, nodeCard.detail(), nodeCard.width() - 34),
                nodeCard.x() + 18,
                nodeCard.y() + 88
        );

        graphics.setColor(nodeCard.accentColor().darker());
        graphics.setFont(new Font("SansSerif", Font.BOLD, 15));
        graphics.drawString(
                truncate(graphics, nodeCard.metric(), nodeCard.width() - 34),
                nodeCard.x() + 18,
                nodeCard.y() + 108
        );
    }

    private static void drawNodeTypeChip(Graphics2D graphics, NodeCard nodeCard) {
        Font font = new Font("SansSerif", Font.BOLD, 13);
        graphics.setFont(font);
        FontMetrics metrics = graphics.getFontMetrics();
        String chipText = nodeCard.type();
        int width = metrics.stringWidth(chipText) + 20;

        graphics.setColor(withAlpha(nodeCard.accentColor(), 230));
        graphics.fill(new RoundRectangle2D.Double(
                nodeCard.x() + 16,
                nodeCard.y() + 14,
                width,
                24,
                14,
                14
        ));
        graphics.setColor(Color.WHITE);
        graphics.drawString(chipText, nodeCard.x() + 26, nodeCard.y() + 31);
    }

    private static void drawSummaryPanel(
            Graphics2D graphics,
            String productId,
            GraphTools.GraphResponse graphResponse,
            int canvasWidth
    ) {
        int x = 70;
        int y = 690;
        int width = canvasWidth - 140;
        int height = 190;

        graphics.setColor(new Color(0xFFFFFF));
        graphics.fill(new RoundRectangle2D.Double(x, y, width, height, 30, 30));
        graphics.setColor(new Color(0xCBD5E1));
        graphics.setStroke(new BasicStroke(2f));
        graphics.draw(new RoundRectangle2D.Double(x, y, width, height, 30, 30));

        graphics.setColor(new Color(0x0F172A));
        graphics.setFont(new Font("SansSerif", Font.BOLD, 26));
        graphics.drawString("Traversal Summary", x + 28, y + 42);

        graphics.setColor(new Color(0x475569));
        graphics.setFont(new Font("SansSerif", Font.PLAIN, 17));
        graphics.drawString(buildPathSummary(graphResponse), x + 28, y + 76);
        graphics.drawString("Requested product: " + productId, x + 28, y + 106);
        graphics.drawString(
                "Resolved source: " + graphResponse.sourceDetail() + ".",
                x + 28,
                y + 136
        );
        graphics.drawString(
                "Modes supported: database, payload, and auto validation/fallback.",
                x + 28,
                y + 164
        );

        drawMetricBadge(
                graphics,
                x + 880,
                y + 34,
                findNodeMetric(graphResponse, "PORT", "Delay risk n/a"),
                new Color(0xF59E0B)
        );
        drawMetricBadge(
                graphics,
                x + 880,
                y + 84,
                findNodeMetric(graphResponse, "WAREHOUSE", "Fill rate n/a"),
                new Color(0x0F766E)
        );
        drawMetricBadge(
                graphics,
                x + 880,
                y + 134,
                findNodeMetric(graphResponse, "ALERT", "Risk n/a"),
                new Color(0xC2410C)
        );
    }

    private static void drawMetricBadge(
            Graphics2D graphics,
            int x,
            int y,
            String text,
            Color accent
    ) {
        graphics.setColor(withAlpha(accent, 40));
        graphics.fill(new RoundRectangle2D.Double(x, y, 430, 34, 18, 18));
        graphics.setColor(accent);
        graphics.setFont(new Font("SansSerif", Font.BOLD, 15));
        graphics.drawString(text, x + 16, y + 22);
    }

    private static String headerSubtitle(String productId, GraphTools.GraphResponse graphResponse) {
        return switch (graphResponse.sourceMode()) {
            case "database" -> "Oracle Database property graph result for product request: " + productId;
            case "payload" -> "Validated upstream graph payload for product request: " + productId;
            default -> "Graph result for product request: " + productId;
        };
    }

    private static void addRouteLabel(
            List<String> route,
            GraphTools.GraphResponse graphResponse,
            String type
    ) {
        Map<String, String> node = findNodeByType(graphResponse, type);
        if (!node.isEmpty()) {
            route.add(node.getOrDefault("label", type));
        }
    }

    private static String buildPathSummary(GraphTools.GraphResponse graphResponse) {
        List<String> route = new ArrayList<>();
        addRouteLabel(route, graphResponse, "SUPPLIER");
        addRouteLabel(route, graphResponse, "PLANT");
        addRouteLabel(route, graphResponse, "PORT");
        addRouteLabel(route, graphResponse, "WAREHOUSE");
        addRouteLabel(route, graphResponse, "PRODUCT");
        return route.isEmpty() ? "No path available" : String.join(" -> ", route);
    }

    private static String findNodeMetric(
            GraphTools.GraphResponse graphResponse,
            String type,
            String fallback
    ) {
        Map<String, String> node = findNodeByType(graphResponse, type);
        return node.getOrDefault("metric", fallback);
    }

    private static Map<String, String> findNodeByType(
            GraphTools.GraphResponse graphResponse,
            String type
    ) {
        for (Map<String, String> node : graphResponse.nodes()) {
            if (type.equals(node.get("type"))) {
                return node;
            }
        }
        return Map.of();
    }

    private static Color fillColorForType(String type) {
        return switch (type) {
            case "SUPPLIER" -> new Color(0xDBEAFE);
            case "PLANT" -> new Color(0xE0F2FE);
            case "PORT" -> new Color(0xFEF3C7);
            case "WAREHOUSE" -> new Color(0xDCFCE7);
            case "PRODUCT" -> new Color(0xFCE7F3);
            case "ALERT" -> new Color(0xFEE2E2);
            default -> new Color(0xE2E8F0);
        };
    }

    private static Color accentColorForType(String type) {
        return switch (type) {
            case "SUPPLIER" -> new Color(0x2563EB);
            case "PLANT" -> new Color(0x0284C7);
            case "PORT" -> new Color(0xD97706);
            case "WAREHOUSE" -> new Color(0x15803D);
            case "PRODUCT" -> new Color(0xBE185D);
            case "ALERT" -> new Color(0xDC2626);
            default -> new Color(0x475569);
        };
    }

    private static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    private static String truncate(Graphics2D graphics, String text, int maxWidth) {
        if (text == null || text.isBlank()) {
            return "";
        }
        FontMetrics metrics = graphics.getFontMetrics();
        if (metrics.stringWidth(text) <= maxWidth) {
            return text;
        }

        String ellipsis = "...";
        int targetWidth = maxWidth - metrics.stringWidth(ellipsis);
        StringBuilder builder = new StringBuilder();
        for (char character : text.toCharArray()) {
            if (metrics.stringWidth(builder.toString() + character) > targetWidth) {
                break;
            }
            builder.append(character);
        }
        return builder + ellipsis;
    }

    private static String valueOrDefault(Environment environment, String key, String defaultValue) {
        return firstNonBlank(environment.getProperty(key), defaultValue);
    }

    private static String appendPath(String baseUrl, String path) {
        String trimmedBase = baseUrl == null ? "" : baseUrl.trim();
        if (trimmedBase.endsWith("/")) {
            trimmedBase = trimmedBase.substring(0, trimmedBase.length() - 1);
        }
        return trimmedBase + path;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private record NodeCard(
            String id,
            String type,
            String label,
            String detail,
            String metric,
            int x,
            int y,
            int width,
            int height,
            Color fillColor,
            Color accentColor
    ) {}
}
