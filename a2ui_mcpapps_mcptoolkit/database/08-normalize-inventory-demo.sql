WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

-- Align the governed A2UI/MCP App recommendation surface with the broader
-- Gemini Enterprise supply-chain story. These inserts are intentionally
-- idempotent and do not reset inventory quantities after a transfer has been
-- approved.

MERGE INTO supply_locations target
USING (
    SELECT 105 AS location_id,
           'DFW-HUB' AS location_code,
           'DFW Hub' AS location_name,
           'South Central' AS region_name,
           'DISTRIBUTION_CENTER' AS location_type
      FROM dual
) source
ON (target.location_id = source.location_id)
WHEN NOT MATCHED THEN INSERT (
    location_id, location_code, location_name, region_name, location_type
) VALUES (
    source.location_id, source.location_code, source.location_name,
    source.region_name, source.location_type
);

MERGE INTO supply_locations target
USING (
    SELECT 106 AS location_id,
           'EWR-HUB' AS location_code,
           'Newark Inventory Hub' AS location_name,
           'Northeast' AS region_name,
           'FULFILLMENT_CENTER' AS location_type
      FROM dual
) source
ON (target.location_id = source.location_id)
WHEN NOT MATCHED THEN INSERT (
    location_id, location_code, location_name, region_name, location_type
) VALUES (
    source.location_id, source.location_code, source.location_name,
    source.region_name, source.location_type
);

MERGE INTO supply_products target
USING (
    SELECT 1500 AS product_id,
           'SKU-500' AS sku,
           'Sustainable Widget 500' AS product_name,
           'Sustainable Components' AS category_name,
           250.00 AS unit_value
      FROM dual
) source
ON (target.product_id = source.product_id)
WHEN NOT MATCHED THEN INSERT (
    product_id, sku, product_name, category_name, unit_value
) VALUES (
    source.product_id, source.sku, source.product_name,
    source.category_name, source.unit_value
);

-- DFW retains 500 safely transferable units after demand and safety stock.
MERGE INTO inventory_positions target
USING (
    SELECT 1500 AS product_id, 105 AS location_id,
           1200 AS on_hand_qty, 0 AS reserved_qty, 0 AS inbound_qty,
           500 AS forecast_7d_qty, 200 AS safety_stock_qty
      FROM dual
) source
ON (target.product_id = source.product_id AND target.location_id = source.location_id)
WHEN NOT MATCHED THEN INSERT (
    product_id, location_id, on_hand_qty, reserved_qty, inbound_qty,
    forecast_7d_qty, safety_stock_qty, last_updated
) VALUES (
    source.product_id, source.location_id, source.on_hand_qty,
    source.reserved_qty, source.inbound_qty, source.forecast_7d_qty,
    source.safety_stock_qty, SYSTIMESTAMP
);

-- Newark has an 880-unit projected shortage; the bounded recommendation is
-- therefore the 500 units that DFW can safely release.
MERGE INTO inventory_positions target
USING (
    SELECT 1500 AS product_id, 106 AS location_id,
           120 AS on_hand_qty, 0 AS reserved_qty, 0 AS inbound_qty,
           800 AS forecast_7d_qty, 200 AS safety_stock_qty
      FROM dual
) source
ON (target.product_id = source.product_id AND target.location_id = source.location_id)
WHEN NOT MATCHED THEN INSERT (
    product_id, location_id, on_hand_qty, reserved_qty, inbound_qty,
    forecast_7d_qty, safety_stock_qty, last_updated
) VALUES (
    source.product_id, source.location_id, source.on_hand_qty,
    source.reserved_qty, source.inbound_qty, source.forecast_7d_qty,
    source.safety_stock_qty, SYSTIMESTAMP
);

MERGE INTO supply_lanes target
USING (
    SELECT 105 AS source_location_id, 106 AS target_location_id,
           3 AS transit_days, 17.25 AS unit_transfer_cost, 'Y' AS active_flag
      FROM dual
) source
ON (
    target.source_location_id = source.source_location_id
    AND target.target_location_id = source.target_location_id
)
WHEN NOT MATCHED THEN INSERT (
    source_location_id, target_location_id, transit_days,
    unit_transfer_cost, active_flag
) VALUES (
    source.source_location_id, source.target_location_id,
    source.transit_days, source.unit_transfer_cost, source.active_flag
);

MERGE INTO supply_lanes target
USING (
    SELECT 106 AS source_location_id, 105 AS target_location_id,
           3 AS transit_days, 17.25 AS unit_transfer_cost, 'Y' AS active_flag
      FROM dual
) source
ON (
    target.source_location_id = source.source_location_id
    AND target.target_location_id = source.target_location_id
)
WHEN NOT MATCHED THEN INSERT (
    source_location_id, target_location_id, transit_days,
    unit_transfer_cost, active_flag
) VALUES (
    source.source_location_id, source.target_location_id,
    source.transit_days, source.unit_transfer_cost, source.active_flag
);

COMMIT;

PROMPT Normalized SKU-500 recommendation
SELECT recommendation_id,
       sku,
       source_location_code,
       target_location_code,
       recommended_transfer_qty,
       stockout_risk_score
  FROM stockout_transfer_recommendation_v
 WHERE sku = 'SKU-500';
