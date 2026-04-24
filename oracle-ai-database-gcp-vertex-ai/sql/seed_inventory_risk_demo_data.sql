set define off

prompt [1/3] seed sc_inventory_risk_summary
merge into sc_inventory_risk_summary target
using (
    select 'SKU-500' as product_id, '2026-Q3' as quarter_label, 'HIGH' as risk_level, 0.72 as stockout_probability, 1240 as at_risk_units, 185000 as projected_revenue_impact_usd, 'Northeast corridor' as primary_region, 'Rebalance inventory into Newark before the next delayed inbound cycle.' as recommendation_summary, 'Y' as active_flag from dual
    union all
    select 'SKU-700', '2026-Q3', 'MEDIUM', 0.49, 780, 96000, 'Upper Midwest', 'Pre-stage buffer inventory in Chicago before demand spikes.', 'Y' from dual
    union all
    select 'SKU-900', '2026-Q3', 'MEDIUM', 0.41, 620, 87000, 'Southeast', 'Maintain current buffers but watch Gulf-port capacity.', 'Y' from dual
) source
on (target.product_id = source.product_id)
when matched then update set
    target.quarter_label = source.quarter_label,
    target.risk_level = source.risk_level,
    target.stockout_probability = source.stockout_probability,
    target.at_risk_units = source.at_risk_units,
    target.projected_revenue_impact_usd = source.projected_revenue_impact_usd,
    target.primary_region = source.primary_region,
    target.recommendation_summary = source.recommendation_summary,
    target.active_flag = source.active_flag
when not matched then insert (
    product_id,
    quarter_label,
    risk_level,
    stockout_probability,
    at_risk_units,
    projected_revenue_impact_usd,
    primary_region,
    recommendation_summary,
    active_flag
) values (
    source.product_id,
    source.quarter_label,
    source.risk_level,
    source.stockout_probability,
    source.at_risk_units,
    source.projected_revenue_impact_usd,
    source.primary_region,
    source.recommendation_summary,
    source.active_flag
);

prompt [2/3] seed sc_warehouse_geo
merge into sc_warehouse_geo target
using (
    select 4001 as warehouse_id, 'WH-101' as warehouse_code, 'Essex' as county_name, 'NJ' as state_code, 'Northeast corridor' as region_name, 40.735700 as latitude, -74.172400 as longitude from dual
    union all
    select 4002, 'WH-202', 'Tarrant', 'TX', 'Southern buffer', 32.899800, -97.040300 from dual
    union all
    select 4003, 'WH-303', 'Cook', 'IL', 'Midwest relay', 41.974200, -87.907300 from dual
) source
on (target.warehouse_id = source.warehouse_id)
when matched then update set
    target.warehouse_code = source.warehouse_code,
    target.county_name = source.county_name,
    target.state_code = source.state_code,
    target.region_name = source.region_name,
    target.latitude = source.latitude,
    target.longitude = source.longitude
when not matched then insert (
    warehouse_id,
    warehouse_code,
    county_name,
    state_code,
    region_name,
    latitude,
    longitude
) values (
    source.warehouse_id,
    source.warehouse_code,
    source.county_name,
    source.state_code,
    source.region_name,
    source.latitude,
    source.longitude
);

prompt [3/3] seed sc_warehouse_risk_snapshot
merge into sc_warehouse_risk_snapshot target
using (
    select 'SKU-500' as product_id, 4001 as warehouse_id, 1 as hotspot_rank, 0.86 as hotspot_score, 4.2 as coverage_days, 410 as backlog_units, 91 as service_level_pct, 620 as at_risk_units, 92000 as revenue_impact_usd, 'CRITICAL' as risk_level, 'DESTINATION_HOTSPOT' as recommended_role, 'Y' as active_flag from dual
    union all
    select 'SKU-500', 4002, 2, 0.31, 11.8, 120, 97, 180, 41000, 'BUFFER', 'SOURCE_BUFFER', 'Y' from dual
    union all
    select 'SKU-500', 4003, 3, 0.48, 8.1, 190, 94, 210, 52000, 'WATCH', 'RELAY_NODE', 'Y' from dual
    union all
    select 'SKU-700', 4003, 1, 0.74, 5.0, 360, 92, 430, 61000, 'HIGH', 'DESTINATION_HOTSPOT', 'Y' from dual
    union all
    select 'SKU-700', 4002, 2, 0.36, 10.4, 110, 98, 170, 22000, 'BUFFER', 'SOURCE_BUFFER', 'Y' from dual
    union all
    select 'SKU-700', 4001, 3, 0.29, 12.6, 90, 96, 140, 13000, 'WATCH', 'SATELLITE_NODE', 'Y' from dual
    union all
    select 'SKU-900', 4002, 1, 0.63, 6.5, 280, 93, 300, 47000, 'HIGH', 'DESTINATION_HOTSPOT', 'Y' from dual
    union all
    select 'SKU-900', 4003, 2, 0.34, 11.0, 115, 97, 150, 19000, 'BUFFER', 'SOURCE_BUFFER', 'Y' from dual
    union all
    select 'SKU-900', 4001, 3, 0.28, 13.2, 80, 97, 110, 12000, 'WATCH', 'SATELLITE_NODE', 'Y' from dual
) source
on (
    target.product_id = source.product_id
    and target.warehouse_id = source.warehouse_id
)
when matched then update set
    target.hotspot_rank = source.hotspot_rank,
    target.hotspot_score = source.hotspot_score,
    target.coverage_days = source.coverage_days,
    target.backlog_units = source.backlog_units,
    target.service_level_pct = source.service_level_pct,
    target.at_risk_units = source.at_risk_units,
    target.revenue_impact_usd = source.revenue_impact_usd,
    target.risk_level = source.risk_level,
    target.recommended_role = source.recommended_role,
    target.active_flag = source.active_flag
when not matched then insert (
    product_id,
    warehouse_id,
    hotspot_rank,
    hotspot_score,
    coverage_days,
    backlog_units,
    service_level_pct,
    at_risk_units,
    revenue_impact_usd,
    risk_level,
    recommended_role,
    active_flag
) values (
    source.product_id,
    source.warehouse_id,
    source.hotspot_rank,
    source.hotspot_score,
    source.coverage_days,
    source.backlog_units,
    source.service_level_pct,
    source.at_risk_units,
    source.revenue_impact_usd,
    source.risk_level,
    source.recommended_role,
    source.active_flag
);

commit;
