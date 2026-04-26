package oracleai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class SpatialTools {

    private static final Pattern PRODUCT_ID_PATTERN = Pattern.compile("\\b([A-Z]{2,}-\\d+)\\b");
    private static final String HOTSPOT_QUERY = """
            SELECT
                summary.product_id,
                product.product_name,
                summary.quarter_label,
                summary.risk_level AS overall_risk_level,
                summary.stockout_probability,
                summary.projected_revenue_impact_usd,
                summary.primary_region,
                summary.recommendation_summary,
                snapshot.hotspot_rank,
                snapshot.hotspot_score,
                snapshot.coverage_days,
                snapshot.backlog_units,
                snapshot.service_level_pct,
                snapshot.at_risk_units,
                snapshot.revenue_impact_usd,
                snapshot.risk_level AS warehouse_risk_level,
                snapshot.recommended_role,
                warehouse.warehouse_name,
                geo.warehouse_code,
                geo.county_name,
                geo.state_code,
                geo.region_name,
                geo.latitude,
                geo.longitude
            FROM sc_inventory_risk_summary summary
            JOIN sc_products product
              ON product.product_id = summary.product_id
            JOIN sc_warehouse_risk_snapshot snapshot
              ON snapshot.product_id = summary.product_id
             AND snapshot.active_flag = 'Y'
            JOIN sc_warehouses warehouse
              ON warehouse.warehouse_id = snapshot.warehouse_id
            JOIN sc_warehouse_geo geo
              ON geo.warehouse_id = snapshot.warehouse_id
            WHERE summary.product_id = ?
              AND summary.active_flag = 'Y'
            ORDER BY snapshot.hotspot_rank
            """;

    private final Environment environment;
    private final GeometryFactory geometryFactory;
    private final BasemapLayers basemapLayers;

    public SpatialTools(Environment environment, ObjectMapper objectMapper) {
        this.environment = environment;
        this.geometryFactory = new GeometryFactory();
        this.basemapLayers = loadBasemapLayers(objectMapper);
    }

    public SpatialResponse resolveSpatialResponse(String userInput) {
        String productId = extractProductId(userInput);
        try {
            return resolveDatabaseSpatialResponse(productId);
        } catch (Exception exception) {
            return resolveSeededSpatialResponse(productId, exception.getMessage());
        }
    }

    public String renderHotspotPng(SpatialResponse response) throws Exception {
        int width = 1480;
        int height = 940;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();

        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            drawBackground(graphics, width, height);
            drawHeader(graphics, width, response);

            int mapLeft = 56;
            int mapTop = 170;
            int mapWidth = 900;
            int mapHeight = 680;
            drawMapPanel(graphics, response, mapLeft, mapTop, mapWidth, mapHeight);
            drawSidebar(graphics, response, 1000, 170, 400, 680);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "png", outputStream);
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } finally {
            graphics.dispose();
        }
    }

    public java.util.Map<String, Object> spatialEvidenceFor(String productId) {
        SpatialResponse response = resolveSpatialResponse("Show a map for " + productId);
        WarehouseHotspot destination = response.hotspots().stream()
                .filter(hotspot -> hotspot.recommendedRole().contains("DESTINATION"))
                .findFirst()
                .orElse(response.hotspots().get(0));
        WarehouseHotspot source = response.hotspots().stream()
                .filter(hotspot -> hotspot.recommendedRole().contains("SOURCE"))
                .findFirst()
                .orElse(response.hotspots().get(response.hotspots().size() - 1));

        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("status", "ok");
        result.put("productId", response.productId());
        result.put("hotspotRegion", response.summary().primaryRegion());
        result.put("hotspotSummary", response.summaryText());
        result.put("recommendedSourceWarehouse", "Warehouse: " + source.warehouseName());
        result.put("recommendedDestinationWarehouse", "Warehouse: " + destination.warehouseName());
        result.put("suggestedTransferUnits", 500);
        result.put("coverageRiskDays", destination.coverageDays());
        result.put("sourceDetail", response.sourceDetail());
        return result;
    }

    private SpatialResponse resolveDatabaseSpatialResponse(String productId) throws Exception {
        List<WarehouseHotspot> hotspots = new ArrayList<>();
        RiskSummary summary = null;

        try (
                Connection connection = OracleJdbcSupport.openConnection(environment);
                PreparedStatement statement = connection.prepareStatement(HOTSPOT_QUERY)
        ) {
            statement.setString(1, productId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    if (summary == null) {
                        summary = new RiskSummary(
                                resultSet.getString("product_id"),
                                resultSet.getString("product_name"),
                                resultSet.getString("quarter_label"),
                                resultSet.getString("overall_risk_level"),
                                resultSet.getDouble("stockout_probability"),
                                resultSet.getDouble("projected_revenue_impact_usd"),
                                resultSet.getString("primary_region"),
                                resultSet.getString("recommendation_summary")
                        );
                    }

                    hotspots.add(new WarehouseHotspot(
                            resultSet.getString("product_id"),
                            resultSet.getString("warehouse_code"),
                            resultSet.getString("warehouse_name"),
                            resultSet.getString("county_name"),
                            resultSet.getString("state_code"),
                            resultSet.getString("region_name"),
                            resultSet.getDouble("latitude"),
                            resultSet.getDouble("longitude"),
                            resultSet.getInt("hotspot_rank"),
                            resultSet.getDouble("hotspot_score"),
                            resultSet.getDouble("coverage_days"),
                            resultSet.getInt("backlog_units"),
                            resultSet.getDouble("service_level_pct"),
                            resultSet.getInt("at_risk_units"),
                            resultSet.getDouble("revenue_impact_usd"),
                            resultSet.getString("warehouse_risk_level"),
                            resultSet.getString("recommended_role")
                    ));
                }
            }
        }

        if (summary == null || hotspots.isEmpty()) {
            throw new IllegalArgumentException("No spatial hotspot rows were found for productId " + productId + ".");
        }

        List<WarehouseHotspot> orderedHotspots = hotspots.stream()
                .sorted(Comparator.comparingInt(WarehouseHotspot::hotspotRank))
                .toList();

        return new SpatialResponse(
                productId,
                summary,
                orderedHotspots,
                "database",
                "Oracle warehouse hotspot tables",
                buildSummaryText(summary, orderedHotspots)
        );
    }

    private SpatialResponse resolveSeededSpatialResponse(String productId, String databaseError) {
        DemoInventoryData.InventoryRiskSummary seededSummary = DemoInventoryData.summaryFor(productId);
        List<WarehouseHotspot> seededHotspots = DemoInventoryData.hotspotsFor(productId).stream()
                .map(hotspot -> new WarehouseHotspot(
                        hotspot.productId(),
                        hotspot.warehouseCode(),
                        hotspot.warehouseName(),
                        hotspot.countyName(),
                        hotspot.stateCode(),
                        hotspot.regionName(),
                        hotspot.latitude(),
                        hotspot.longitude(),
                        hotspot.hotspotRank(),
                        hotspot.hotspotScore(),
                        hotspot.coverageDays(),
                        hotspot.backlogUnits(),
                        hotspot.serviceLevelPct(),
                        hotspot.atRiskUnits(),
                        hotspot.revenueImpactUsd(),
                        hotspot.riskLevel(),
                        hotspot.recommendedRole()
                ))
                .toList();

        RiskSummary summary = new RiskSummary(
                seededSummary.productId(),
                seededSummary.productName(),
                seededSummary.quarterLabel(),
                seededSummary.riskLevel(),
                seededSummary.stockoutProbability(),
                seededSummary.projectedRevenueImpactUsd(),
                seededSummary.primaryRegion(),
                seededSummary.recommendationSummary()
        );

        return new SpatialResponse(
                summary.productId(),
                summary,
                seededHotspots,
                "seeded",
                "Seeded spatial hotspot fallback"
                        + (databaseError == null || databaseError.isBlank() ? "" : " (" + databaseError + ")"),
                buildSummaryText(summary, seededHotspots)
        );
    }

    private static String extractProductId(String userInput) {
        Matcher matcher = PRODUCT_ID_PATTERN.matcher(userInput == null ? "" : userInput.toUpperCase(Locale.ROOT));
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "SKU-500";
    }

    private static String buildSummaryText(RiskSummary summary, List<WarehouseHotspot> hotspots) {
        WarehouseHotspot destination = hotspots.stream()
                .filter(hotspot -> hotspot.recommendedRole().contains("DESTINATION"))
                .findFirst()
                .orElse(hotspots.get(0));
        WarehouseHotspot source = hotspots.stream()
                .filter(hotspot -> hotspot.recommendedRole().contains("SOURCE"))
                .findFirst()
                .orElse(hotspots.get(hotspots.size() - 1));

        return "Hotspot map for " + summary.productId() + ": "
                + destination.warehouseName() + " in " + destination.countyName() + ", " + destination.stateCode()
                + " is the primary pressure node (score " + formatDecimal(destination.hotspotScore())
                + ", coverage " + formatDecimal(destination.coverageDays()) + " days). "
                + source.warehouseName() + " is the best relief source.";
    }

    private void drawMapPanel(Graphics2D graphics, SpatialResponse response, int left, int top, int width, int height) {
        graphics.setColor(new Color(0xDDECF7));
        graphics.fill(new RoundRectangle2D.Double(left, top, width, height, 30, 30));
        graphics.setColor(new Color(0xAEC4D6));
        graphics.setStroke(new BasicStroke(2f));
        graphics.draw(new RoundRectangle2D.Double(left, top, width, height, 30, 30));

        List<Point> points = response.hotspots().stream()
                .map(hotspot -> geometryFactory.createPoint(new Coordinate(hotspot.longitude(), hotspot.latitude())))
                .toList();
        MultiPoint multiPoint = geometryFactory.createMultiPoint(points.toArray(Point[]::new));
        Geometry hull = multiPoint.convexHull();
        Envelope envelope = expandEnvelope(multiPoint.getEnvelopeInternal());

        drawStatePolygons(graphics, basemapLayers.statePolygons(), envelope, left, top, width, height);
        drawLakePolygons(graphics, basemapLayers.lakePolygons(), envelope, left, top, width, height);
        drawLongitudeLatitudeGrid(graphics, left, top, width, height);
        drawRiverLines(graphics, basemapLayers.riverLines(), envelope, left, top, width, height);

        if (hull instanceof Polygon polygon) {
            drawPolygon(
                    graphics,
                    polygon,
                    envelope,
                    left,
                    top,
                    width,
                    height,
                    new Color(0xBFE2D1, true),
                    new Color(0x5B8A72),
                    2.4f
            );
        }

        WarehouseHotspot source = response.hotspots().stream()
                .filter(hotspot -> hotspot.recommendedRole().contains("SOURCE"))
                .findFirst()
                .orElse(null);
        WarehouseHotspot destination = response.hotspots().stream()
                .filter(hotspot -> hotspot.recommendedRole().contains("DESTINATION"))
                .findFirst()
                .orElse(null);
        if (source != null && destination != null) {
            LineString transferLine = geometryFactory.createLineString(
                    new Coordinate[] {
                            new Coordinate(source.longitude(), source.latitude()),
                            new Coordinate(destination.longitude(), destination.latitude())
                    }
            );
            drawLineString(graphics, transferLine, envelope, left, top, width, height, new Color(0xC2410C), 4f);
        }

        for (WarehouseHotspot hotspot : response.hotspots()) {
            drawHotspot(graphics, hotspot, envelope, left, top, width, height);
        }

        graphics.setColor(new Color(0x0F172A));
        graphics.setFont(new Font("SansSerif", Font.BOLD, 22));
        graphics.drawString("Warehouse Hotspots", left + 24, top + 34);
        graphics.setFont(new Font("SansSerif", Font.PLAIN, 14));
        graphics.setColor(new Color(0x475569));
        graphics.drawString("Derived from warehouse risk coordinates and hotspot scores", left + 24, top + 58);
    }

    private void drawSidebar(Graphics2D graphics, SpatialResponse response, int left, int top, int width, int height) {
        graphics.setColor(new Color(0xFFFDF7));
        graphics.fill(new RoundRectangle2D.Double(left, top, width, height, 30, 30));
        graphics.setColor(new Color(0xE7D8BF));
        graphics.setStroke(new BasicStroke(2f));
        graphics.draw(new RoundRectangle2D.Double(left, top, width, height, 30, 30));

        graphics.setColor(new Color(0x0F172A));
        graphics.setFont(new Font("SansSerif", Font.BOLD, 24));
        graphics.drawString(response.summary().productId() + " Overview", left + 28, top + 42);

        graphics.setFont(new Font("SansSerif", Font.PLAIN, 16));
        graphics.setColor(new Color(0x334155));
        List<String> lines = List.of(
                "Quarter: " + response.summary().quarterLabel(),
                "Risk level: " + response.summary().riskLevel(),
                "Primary region: " + response.summary().primaryRegion(),
                "Stockout probability: " + formatPercent(response.summary().stockoutProbability()),
                "Revenue impact: $" + String.format("%,.0f", response.summary().projectedRevenueImpactUsd()),
                "Source: " + response.sourceDetail()
        );

        int textY = top + 86;
        for (String line : lines) {
            graphics.drawString(line, left + 28, textY);
            textY += 28;
        }

        graphics.setFont(new Font("SansSerif", Font.BOLD, 18));
        graphics.setColor(new Color(0x7C2D12));
        graphics.drawString("Recommended move", left + 28, textY + 10);

        graphics.setFont(new Font("SansSerif", Font.PLAIN, 15));
        graphics.setColor(new Color(0x475569));
        drawWrappedText(
                graphics,
                response.summaryText(),
                left + 28,
                textY + 40,
                width - 56,
                22
        );

        int hotspotY = top + 360;
        graphics.setFont(new Font("SansSerif", Font.BOLD, 18));
        graphics.setColor(new Color(0x0F172A));
        graphics.drawString("Hotspot ranks", left + 28, hotspotY);
        hotspotY += 26;

        graphics.setFont(new Font("SansSerif", Font.PLAIN, 14));
        for (WarehouseHotspot hotspot : response.hotspots()) {
            String bullet = hotspot.hotspotRank() + ". " + hotspot.warehouseName()
                    + " | " + hotspot.countyName() + ", " + hotspot.stateCode()
                    + " | score " + formatDecimal(hotspot.hotspotScore())
                    + " | coverage " + formatDecimal(hotspot.coverageDays()) + "d";
            drawWrappedText(graphics, bullet, left + 28, hotspotY, width - 56, 18);
            hotspotY += 52;
        }
    }

    private void drawHeader(Graphics2D graphics, int width, SpatialResponse response) {
        graphics.setPaint(new GradientPaint(
                0,
                0,
                new Color(0x16324F),
                width,
                120,
                new Color(0x28536B)
        ));
        graphics.fillRect(0, 0, width, 126);

        graphics.setColor(new Color(0xF8FAFC));
        graphics.setFont(new Font("SansSerif", Font.BOLD, 34));
        graphics.drawString("Oracle Spatial Hotspot Map", 56, 52);

        graphics.setFont(new Font("SansSerif", Font.PLAIN, 18));
        graphics.setColor(new Color(0xDCEAF2));
        graphics.drawString(
                response.summary().productName() + " | " + response.summary().quarterLabel() + " | "
                        + response.sourceMode().toUpperCase(Locale.ROOT),
                56,
                86
        );
    }

    private static void drawBackground(Graphics2D graphics, int width, int height) {
        graphics.setPaint(new GradientPaint(
                0,
                0,
                new Color(0xEEF3F8),
                width,
                height,
                new Color(0xF8F3EA)
        ));
        graphics.fillRect(0, 0, width, height);
    }

    private static void drawLongitudeLatitudeGrid(Graphics2D graphics, int left, int top, int width, int height) {
        graphics.setColor(new Color(0xD2DCE4));
        graphics.setStroke(new BasicStroke(1f));
        for (int x = left + 60; x < left + width; x += 120) {
            graphics.drawLine(x, top + 56, x, top + height - 34);
        }
        for (int y = top + 76; y < top + height; y += 100) {
            graphics.drawLine(left + 34, y, left + width - 34, y);
        }
    }

    private void drawStatePolygons(
            Graphics2D graphics,
            List<Geometry> polygons,
            Envelope envelope,
            int left,
            int top,
            int width,
            int height
    ) {
        for (Geometry geometry : polygons) {
            if (!geometry.getEnvelopeInternal().intersects(envelope)) {
                continue;
            }
            drawGeometry(
                    graphics,
                    geometry,
                    envelope,
                    left,
                    top,
                    width,
                    height,
                    new Color(0xF6F2E8),
                    new Color(0x9BA9B4),
                    1.2f
            );
        }
    }

    private void drawLakePolygons(
            Graphics2D graphics,
            List<Geometry> polygons,
            Envelope envelope,
            int left,
            int top,
            int width,
            int height
    ) {
        for (Geometry geometry : polygons) {
            if (!geometry.getEnvelopeInternal().intersects(envelope)) {
                continue;
            }
            drawGeometry(
                    graphics,
                    geometry,
                    envelope,
                    left,
                    top,
                    width,
                    height,
                    new Color(0xDDECF7),
                    new Color(0x7AA7C7),
                    1.0f
            );
        }
    }

    private void drawRiverLines(
            Graphics2D graphics,
            List<Geometry> lineGeometries,
            Envelope envelope,
            int left,
            int top,
            int width,
            int height
    ) {
        graphics.setColor(new Color(0x5B9BC8));
        graphics.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (Geometry geometry : lineGeometries) {
            if (!geometry.getEnvelopeInternal().intersects(envelope)) {
                continue;
            }
            drawLineGeometry(graphics, geometry, envelope, left, top, width, height);
        }
    }

    private void drawPolygon(
            Graphics2D graphics,
            Polygon polygon,
            Envelope envelope,
            int left,
            int top,
            int width,
            int height,
            Color fillColor,
            Color borderColor,
            float strokeWidth
    ) {
        Path2D path = new Path2D.Double();
        Coordinate[] coordinates = polygon.getExteriorRing().getCoordinates();
        for (int index = 0; index < coordinates.length; index++) {
            ScreenPoint screenPoint = toScreen(coordinates[index], envelope, left, top, width, height);
            if (index == 0) {
                path.moveTo(screenPoint.x(), screenPoint.y());
            } else {
                path.lineTo(screenPoint.x(), screenPoint.y());
            }
        }
        path.closePath();

        graphics.setColor(fillColor);
        graphics.fill(path);
        graphics.setColor(borderColor);
        graphics.setStroke(new BasicStroke(strokeWidth));
        graphics.draw(path);
    }

    private void drawGeometry(
            Graphics2D graphics,
            Geometry geometry,
            Envelope envelope,
            int left,
            int top,
            int width,
            int height,
            Color fillColor,
            Color borderColor,
            float strokeWidth
    ) {
        if (geometry instanceof Polygon polygon) {
            drawPolygon(graphics, polygon, envelope, left, top, width, height, fillColor, borderColor, strokeWidth);
            return;
        }
        for (int index = 0; index < geometry.getNumGeometries(); index++) {
            Geometry child = geometry.getGeometryN(index);
            if (child instanceof Polygon polygon) {
                drawPolygon(graphics, polygon, envelope, left, top, width, height, fillColor, borderColor, strokeWidth);
            }
        }
    }

    private void drawLineString(
            Graphics2D graphics,
            LineString lineString,
            Envelope envelope,
            int left,
            int top,
            int width,
            int height,
            Color color,
            float strokeWidth
    ) {
        graphics.setColor(color);
        graphics.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Coordinate[] coordinates = lineString.getCoordinates();
        for (int index = 0; index < coordinates.length - 1; index++) {
            ScreenPoint start = toScreen(coordinates[index], envelope, left, top, width, height);
            ScreenPoint end = toScreen(coordinates[index + 1], envelope, left, top, width, height);
            graphics.drawLine((int) start.x(), (int) start.y(), (int) end.x(), (int) end.y());
        }
    }

    private void drawLineGeometry(
            Graphics2D graphics,
            Geometry geometry,
            Envelope envelope,
            int left,
            int top,
            int width,
            int height
    ) {
        if (geometry instanceof LineString lineString) {
            drawLineString(graphics, lineString, envelope, left, top, width, height, new Color(0x5B9BC8), 1.6f);
            return;
        }
        for (int index = 0; index < geometry.getNumGeometries(); index++) {
            Geometry child = geometry.getGeometryN(index);
            if (child instanceof LineString lineString) {
                drawLineString(graphics, lineString, envelope, left, top, width, height, new Color(0x5B9BC8), 1.6f);
            }
        }
    }

    private void drawHotspot(
            Graphics2D graphics,
            WarehouseHotspot hotspot,
            Envelope envelope,
            int left,
            int top,
            int width,
            int height
    ) {
        ScreenPoint screenPoint = toScreen(
                new Coordinate(hotspot.longitude(), hotspot.latitude()),
                envelope,
                left,
                top,
                width,
                height
        );
        double radius = 14 + (hotspot.hotspotScore() * 16);
        Color fill = hotspot.recommendedRole().contains("DESTINATION")
                ? new Color(0xDC2626)
                : hotspot.recommendedRole().contains("SOURCE")
                ? new Color(0x0F766E)
                : new Color(0x2563EB);

        graphics.setColor(new Color(fill.getRed(), fill.getGreen(), fill.getBlue(), 70));
        graphics.fill(new Ellipse2D.Double(screenPoint.x() - radius - 8, screenPoint.y() - radius - 8, (radius + 8) * 2, (radius + 8) * 2));
        graphics.setColor(fill);
        graphics.fill(new Ellipse2D.Double(screenPoint.x() - radius, screenPoint.y() - radius, radius * 2, radius * 2));
        graphics.setColor(Color.WHITE);
        graphics.setStroke(new BasicStroke(2f));
        graphics.draw(new Ellipse2D.Double(screenPoint.x() - radius, screenPoint.y() - radius, radius * 2, radius * 2));

        String label = hotspot.warehouseCode() + " | " + hotspot.warehouseName();
        graphics.setFont(new Font("SansSerif", Font.BOLD, 13));
        FontMetrics metrics = graphics.getFontMetrics();
        int labelWidth = metrics.stringWidth(label) + 22;
        int labelX = (int) Math.min(screenPoint.x() + 18, left + width - labelWidth - 14);
        int labelY = (int) Math.max(screenPoint.y() - 32, top + 20);

        graphics.setColor(new Color(0xFFFFFF));
        graphics.fill(new RoundRectangle2D.Double(labelX, labelY, labelWidth, 26, 12, 12));
        graphics.setColor(new Color(0x94A3B8));
        graphics.draw(new RoundRectangle2D.Double(labelX, labelY, labelWidth, 26, 12, 12));
        graphics.setColor(new Color(0x0F172A));
        graphics.drawString(label, labelX + 11, labelY + 18);
    }

    private static void drawWrappedText(Graphics2D graphics, String text, int x, int y, int maxWidth, int lineHeight) {
        FontMetrics metrics = graphics.getFontMetrics();
        StringBuilder currentLine = new StringBuilder();
        int currentY = y;

        for (String word : text.split("\\s+")) {
            String candidate = currentLine.isEmpty() ? word : currentLine + " " + word;
            if (metrics.stringWidth(candidate) > maxWidth && !currentLine.isEmpty()) {
                graphics.drawString(currentLine.toString(), x, currentY);
                currentLine = new StringBuilder(word);
                currentY += lineHeight;
            } else {
                currentLine = new StringBuilder(candidate);
            }
        }

        if (!currentLine.isEmpty()) {
            graphics.drawString(currentLine.toString(), x, currentY);
        }
    }

    private static Envelope expandEnvelope(Envelope original) {
        Envelope envelope = new Envelope(original);
        double minWidth = 12.0;
        double minHeight = 8.0;
        if (envelope.getWidth() < minWidth) {
            double delta = (minWidth - envelope.getWidth()) / 2.0;
            envelope.expandBy(delta, 0);
        }
        if (envelope.getHeight() < minHeight) {
            double delta = (minHeight - envelope.getHeight()) / 2.0;
            envelope.expandBy(0, delta);
        }
        envelope.expandBy(envelope.getWidth() * 0.12, envelope.getHeight() * 0.14);
        return envelope;
    }

    private static ScreenPoint toScreen(
            Coordinate coordinate,
            Envelope envelope,
            int left,
            int top,
            int width,
            int height
    ) {
        double xRatio = (coordinate.x - envelope.getMinX()) / envelope.getWidth();
        double yRatio = (coordinate.y - envelope.getMinY()) / envelope.getHeight();
        double x = left + 40 + (xRatio * (width - 80));
        double y = top + height - 40 - (yRatio * (height - 100));
        return new ScreenPoint(x, y);
    }

    private static String formatPercent(double value) {
        return String.format("%.0f%%", value * 100.0);
    }

    private static String formatDecimal(double value) {
        return String.format("%.1f", value);
    }

    private BasemapLayers loadBasemapLayers(ObjectMapper objectMapper) {
        try {
            return new BasemapLayers(
                    loadGeometries(objectMapper, "oracleai/basemap/us-states.geojson"),
                    loadGeometries(objectMapper, "oracleai/basemap/ne_110m_rivers_lake_centerlines.geojson"),
                    loadGeometries(objectMapper, "oracleai/basemap/ne_110m_lakes.geojson")
            );
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to load bundled spatial basemap resources.", exception);
        }
    }

    private List<Geometry> loadGeometries(ObjectMapper objectMapper, String resourcePath) throws Exception {
        try (InputStream inputStream = SpatialTools.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IllegalArgumentException("Missing basemap resource: " + resourcePath);
            }

            JsonNode root = objectMapper.readTree(inputStream);
            List<Geometry> geometries = new ArrayList<>();
            for (JsonNode feature : root.path("features")) {
                JsonNode geometryNode = feature.path("geometry");
                if (geometryNode.isMissingNode() || geometryNode.isNull()) {
                    continue;
                }
                Geometry geometry = parseGeometry(geometryNode);
                if (geometry != null) {
                    geometries.add(geometry);
                }
            }
            return geometries;
        }
    }

    private Geometry parseGeometry(JsonNode geometryNode) {
        String type = geometryNode.path("type").asText("");
        JsonNode coordinates = geometryNode.path("coordinates");
        return switch (type) {
            case "Polygon" -> geometryFactory.createPolygon(parseLinearRing(coordinates.get(0)));
            case "MultiPolygon" -> geometryFactory.createMultiPolygon(
                    streamArray(coordinates)
                            .map(polygon -> geometryFactory.createPolygon(parseLinearRing(polygon.get(0))))
                            .toArray(Polygon[]::new)
            );
            case "LineString" -> geometryFactory.createLineString(parseCoordinates(coordinates));
            case "MultiLineString" -> geometryFactory.createMultiLineString(
                    streamArray(coordinates)
                            .map(this::parseCoordinates)
                            .map(geometryFactory::createLineString)
                            .toArray(LineString[]::new)
            );
            default -> null;
        };
    }

    private Coordinate[] parseLinearRing(JsonNode coordinatesNode) {
        Coordinate[] coordinates = parseCoordinates(coordinatesNode);
        if (coordinates.length == 0) {
            return coordinates;
        }
        Coordinate first = coordinates[0];
        Coordinate last = coordinates[coordinates.length - 1];
        if (Double.compare(first.x, last.x) != 0 || Double.compare(first.y, last.y) != 0) {
            Coordinate[] closed = new Coordinate[coordinates.length + 1];
            System.arraycopy(coordinates, 0, closed, 0, coordinates.length);
            closed[coordinates.length] = new Coordinate(first.x, first.y);
            return closed;
        }
        return coordinates;
    }

    private Coordinate[] parseCoordinates(JsonNode coordinatesNode) {
        List<Coordinate> coordinates = new ArrayList<>();
        for (JsonNode coordinateNode : streamArray(coordinatesNode).toList()) {
            if (coordinateNode.size() >= 2) {
                coordinates.add(new Coordinate(coordinateNode.get(0).asDouble(), coordinateNode.get(1).asDouble()));
            }
        }
        return coordinates.toArray(Coordinate[]::new);
    }

    private static java.util.stream.Stream<JsonNode> streamArray(JsonNode arrayNode) {
        List<JsonNode> nodes = new ArrayList<>();
        arrayNode.forEach(nodes::add);
        return nodes.stream();
    }

    public record SpatialResponse(
            String productId,
            RiskSummary summary,
            List<WarehouseHotspot> hotspots,
            String sourceMode,
            String sourceDetail,
            String summaryText
    ) {}

    public record RiskSummary(
            String productId,
            String productName,
            String quarterLabel,
            String riskLevel,
            double stockoutProbability,
            double projectedRevenueImpactUsd,
            String primaryRegion,
            String recommendationSummary
    ) {}

    public record WarehouseHotspot(
            String productId,
            String warehouseCode,
            String warehouseName,
            String countyName,
            String stateCode,
            String regionName,
            double latitude,
            double longitude,
            int hotspotRank,
            double hotspotScore,
            double coverageDays,
            int backlogUnits,
            double serviceLevelPct,
            int atRiskUnits,
            double revenueImpactUsd,
            String riskLevel,
            String recommendedRole
    ) {}

    private record BasemapLayers(
            List<Geometry> statePolygons,
            List<Geometry> riverLines,
            List<Geometry> lakePolygons
    ) {}

    private record ScreenPoint(double x, double y) {}
}
