WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

CREATE OR REPLACE VIEW stockout_transfer_recommendation_v AS
WITH position_metrics AS (
    SELECT p.product_id,
           p.location_id,
           p.on_hand_qty,
           p.reserved_qty,
           p.inbound_qty,
           p.forecast_7d_qty,
           p.safety_stock_qty,
           (p.on_hand_qty - p.reserved_qty + p.inbound_qty) AS projected_available_qty,
           GREATEST(
               p.forecast_7d_qty + p.safety_stock_qty
               - (p.on_hand_qty - p.reserved_qty + p.inbound_qty),
               0) AS shortage_qty,
           GREATEST(
               p.on_hand_qty - p.reserved_qty
               - p.forecast_7d_qty - p.safety_stock_qty,
               0) AS transferable_surplus_qty
      FROM inventory_positions p
),
ranked_candidates AS (
    SELECT t.product_id,
           s.location_id AS source_location_id,
           t.location_id AS target_location_id,
           s.projected_available_qty AS source_available_qty,
           t.projected_available_qty AS target_available_qty,
           t.forecast_7d_qty,
           t.safety_stock_qty,
           t.shortage_qty,
           LEAST(t.shortage_qty, s.transferable_surplus_qty) AS recommended_transfer_qty,
           lane.transit_days,
           lane.unit_transfer_cost,
           LEAST(
               100,
               ROUND(
                   (t.shortage_qty / NULLIF(t.forecast_7d_qty + t.safety_stock_qty, 0)) * 75
                   + lane.transit_days * 4
                   + CASE WHEN t.inbound_qty = 0 THEN 8 ELSE 0 END,
                   1)) AS stockout_risk_score,
           ROW_NUMBER() OVER (
               PARTITION BY t.product_id, t.location_id
               ORDER BY lane.transit_days,
                        s.transferable_surplus_qty DESC,
                        lane.unit_transfer_cost) AS source_rank
      FROM position_metrics t
      JOIN position_metrics s
        ON s.product_id = t.product_id
       AND s.location_id <> t.location_id
       AND s.transferable_surplus_qty > 0
      JOIN supply_lanes lane
        ON lane.source_location_id = s.location_id
       AND lane.target_location_id = t.location_id
       AND lane.active_flag = 'Y'
     WHERE t.shortage_qty > 0
)
SELECT TO_CHAR(c.product_id) || ':' || TO_CHAR(c.source_location_id) || ':' ||
       TO_CHAR(c.target_location_id) AS recommendation_id,
       c.product_id,
       product.sku,
       product.product_name,
       product.category_name,
       c.source_location_id,
       source.location_code AS source_location_code,
       source.location_name AS source_location_name,
       c.target_location_id,
       target.location_code AS target_location_code,
       target.location_name AS target_location_name,
       c.source_available_qty,
       c.target_available_qty,
       c.forecast_7d_qty,
       c.safety_stock_qty,
       c.shortage_qty,
       c.recommended_transfer_qty,
       c.transit_days,
       c.unit_transfer_cost,
       c.stockout_risk_score,
       CASE
           WHEN c.stockout_risk_score >= 85 THEN 'CRITICAL'
           WHEN c.stockout_risk_score >= 70 THEN 'HIGH'
           WHEN c.stockout_risk_score >= 50 THEN 'MEDIUM'
           ELSE 'LOW'
       END AS risk_level,
       'Move ' || TO_CHAR(c.recommended_transfer_qty) || ' units of ' || product.sku ||
       ' from ' || source.location_code || ' to ' || target.location_code ||
       '; target projected shortage is ' || TO_CHAR(c.shortage_qty) ||
       ' units and lane transit is ' || TO_CHAR(c.transit_days) || ' days.' AS rationale
  FROM ranked_candidates c
  JOIN supply_products product ON product.product_id = c.product_id
  JOIN supply_locations source ON source.location_id = c.source_location_id
  JOIN supply_locations target ON target.location_id = c.target_location_id
 WHERE c.source_rank = 1
   AND c.recommended_transfer_qty > 0;
