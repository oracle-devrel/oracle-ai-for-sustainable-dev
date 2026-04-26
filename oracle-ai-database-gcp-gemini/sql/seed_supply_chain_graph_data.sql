set define off

prompt [1/11] seed sc_suppliers
merge into sc_suppliers target
using (
    select 1001 as supplier_id, 'Vertex Plastics' as supplier_name, 1 as tier_level, 'Busan' as region_code, 92 as on_time_pct, 'Y' as active_flag from dual
    union all
    select 1002, 'Atlas Components', 1, 'Shenzhen', 96, 'Y' from dual
    union all
    select 1003, 'Meridian Metals', 1, 'Monterrey', 89, 'Y' from dual
) source
on (target.supplier_id = source.supplier_id)
when matched then update set
    target.supplier_name = source.supplier_name,
    target.tier_level = source.tier_level,
    target.region_code = source.region_code,
    target.on_time_pct = source.on_time_pct,
    target.active_flag = source.active_flag
when not matched then insert (
    supplier_id,
    supplier_name,
    tier_level,
    region_code,
    on_time_pct,
    active_flag
) values (
    source.supplier_id,
    source.supplier_name,
    source.tier_level,
    source.region_code,
    source.on_time_pct,
    source.active_flag
);

prompt [2/11] seed sc_plants
merge into sc_plants target
using (
    select 2001 as plant_id, 'Columbus Final Pack' as plant_name, 4.3 as cycle_days, 78 as utilization_pct, 'Y' as active_flag from dual
    union all
    select 2002, 'Austin Assembly', 3.8, 84, 'Y' from dual
    union all
    select 2003, 'Raleigh Integration', 5.1, 73, 'Y' from dual
) source
on (target.plant_id = source.plant_id)
when matched then update set
    target.plant_name = source.plant_name,
    target.cycle_days = source.cycle_days,
    target.utilization_pct = source.utilization_pct,
    target.active_flag = source.active_flag
when not matched then insert (
    plant_id,
    plant_name,
    cycle_days,
    utilization_pct,
    active_flag
) values (
    source.plant_id,
    source.plant_name,
    source.cycle_days,
    source.utilization_pct,
    source.active_flag
);

prompt [3/11] seed sc_ports
merge into sc_ports target
using (
    select 3001 as port_id, 'Houston' as port_name, 54 as eta_hours, 0.31 as delay_risk_score, 'Y' as active_flag from dual
    union all
    select 3002, 'Savannah', 26, 0.14, 'Y' from dual
    union all
    select 3003, 'Long Beach', 62, 0.41, 'Y' from dual
) source
on (target.port_id = source.port_id)
when matched then update set
    target.port_name = source.port_name,
    target.eta_hours = source.eta_hours,
    target.delay_risk_score = source.delay_risk_score,
    target.active_flag = source.active_flag
when not matched then insert (
    port_id,
    port_name,
    eta_hours,
    delay_risk_score,
    active_flag
) values (
    source.port_id,
    source.port_name,
    source.eta_hours,
    source.delay_risk_score,
    source.active_flag
);

prompt [4/11] seed sc_warehouses
merge into sc_warehouses target
using (
    select 4001 as warehouse_id, 'Newark Inventory Hub' as warehouse_name, 4226 as inventory_units, 95 as fill_rate_pct, 'Y' as active_flag from dual
    union all
    select 4002, 'DFW Hub', 3180, 97, 'Y' from dual
    union all
    select 4003, 'Chicago Crossdock', 2875, 93, 'Y' from dual
) source
on (target.warehouse_id = source.warehouse_id)
when matched then update set
    target.warehouse_name = source.warehouse_name,
    target.inventory_units = source.inventory_units,
    target.fill_rate_pct = source.fill_rate_pct,
    target.active_flag = source.active_flag
when not matched then insert (
    warehouse_id,
    warehouse_name,
    inventory_units,
    fill_rate_pct,
    active_flag
) values (
    source.warehouse_id,
    source.warehouse_name,
    source.inventory_units,
    source.fill_rate_pct,
    source.active_flag
);

prompt [5/11] seed sc_products
merge into sc_products target
using (
    select 'SKU-500' as product_id, 'Sustainable Widget 500' as product_name, 8 as demand_change_pct, 30 as margin_pct, 'Y' as active_flag from dual
    union all
    select 'SKU-700', 'Low Carbon Kit 700', -3, 26, 'Y' from dual
    union all
    select 'SKU-900', 'Circular Sensor 900', 11, 22, 'Y' from dual
) source
on (target.product_id = source.product_id)
when matched then update set
    target.product_name = source.product_name,
    target.demand_change_pct = source.demand_change_pct,
    target.margin_pct = source.margin_pct,
    target.active_flag = source.active_flag
when not matched then insert (
    product_id,
    product_name,
    demand_change_pct,
    margin_pct,
    active_flag
) values (
    source.product_id,
    source.product_name,
    source.demand_change_pct,
    source.margin_pct,
    source.active_flag
);

prompt [6/11] seed sc_alerts
merge into sc_alerts target
using (
    select 6001 as alert_id, 'Weather Delay' as alert_name, 'Pacific lane' as lane_name, 0.60 as risk_score, 'Y' as active_flag from dual
    union all
    select 6002, 'Customs Hold', 'Atlantic lane', 0.28, 'Y' from dual
    union all
    select 6003, 'Capacity Spike', 'Gulf lane', 0.44, 'Y' from dual
) source
on (target.alert_id = source.alert_id)
when matched then update set
    target.alert_name = source.alert_name,
    target.lane_name = source.lane_name,
    target.risk_score = source.risk_score,
    target.active_flag = source.active_flag
when not matched then insert (
    alert_id,
    alert_name,
    lane_name,
    risk_score,
    active_flag
) values (
    source.alert_id,
    source.alert_name,
    source.lane_name,
    source.risk_score,
    source.active_flag
);

prompt [7/11] seed sc_supplier_plant
merge into sc_supplier_plant target
using (
    select 5001 as supplier_plant_id, 1001 as supplier_id, 2001 as plant_id, 'SUPPLIES' as relationship_label, 'Y' as is_current, date '2026-01-01' as effective_start_dt, cast(null as date) as effective_end_dt from dual
    union all
    select 5002, 1002, 2002, 'SUPPLIES', 'Y', date '2026-01-01', cast(null as date) from dual
    union all
    select 5003, 1003, 2003, 'SUPPLIES', 'Y', date '2026-01-01', cast(null as date) from dual
) source
on (target.supplier_plant_id = source.supplier_plant_id)
when matched then update set
    target.supplier_id = source.supplier_id,
    target.plant_id = source.plant_id,
    target.relationship_label = source.relationship_label,
    target.is_current = source.is_current,
    target.effective_start_dt = source.effective_start_dt,
    target.effective_end_dt = source.effective_end_dt
when not matched then insert (
    supplier_plant_id,
    supplier_id,
    plant_id,
    relationship_label,
    is_current,
    effective_start_dt,
    effective_end_dt
) values (
    source.supplier_plant_id,
    source.supplier_id,
    source.plant_id,
    source.relationship_label,
    source.is_current,
    source.effective_start_dt,
    source.effective_end_dt
);

prompt [8/11] seed sc_plant_port
merge into sc_plant_port target
using (
    select 5101 as plant_port_id, 2001 as plant_id, 3001 as port_id, 'SHIPS_VIA' as relationship_label, 'Y' as is_current, date '2026-01-01' as effective_start_dt, cast(null as date) as effective_end_dt from dual
    union all
    select 5102, 2002, 3002, 'SHIPS_VIA', 'Y', date '2026-01-01', cast(null as date) from dual
    union all
    select 5103, 2003, 3003, 'SHIPS_VIA', 'Y', date '2026-01-01', cast(null as date) from dual
) source
on (target.plant_port_id = source.plant_port_id)
when matched then update set
    target.plant_id = source.plant_id,
    target.port_id = source.port_id,
    target.relationship_label = source.relationship_label,
    target.is_current = source.is_current,
    target.effective_start_dt = source.effective_start_dt,
    target.effective_end_dt = source.effective_end_dt
when not matched then insert (
    plant_port_id,
    plant_id,
    port_id,
    relationship_label,
    is_current,
    effective_start_dt,
    effective_end_dt
) values (
    source.plant_port_id,
    source.plant_id,
    source.port_id,
    source.relationship_label,
    source.is_current,
    source.effective_start_dt,
    source.effective_end_dt
);

prompt [9/11] seed sc_port_warehouse
merge into sc_port_warehouse target
using (
    select 5201 as port_warehouse_id, 3001 as port_id, 4001 as warehouse_id, 'ROUTES_TO' as relationship_label, 'Y' as is_current, date '2026-01-01' as effective_start_dt, cast(null as date) as effective_end_dt from dual
    union all
    select 5202, 3002, 4002, 'ROUTES_TO', 'Y', date '2026-01-01', cast(null as date) from dual
    union all
    select 5203, 3003, 4003, 'ROUTES_TO', 'Y', date '2026-01-01', cast(null as date) from dual
) source
on (target.port_warehouse_id = source.port_warehouse_id)
when matched then update set
    target.port_id = source.port_id,
    target.warehouse_id = source.warehouse_id,
    target.relationship_label = source.relationship_label,
    target.is_current = source.is_current,
    target.effective_start_dt = source.effective_start_dt,
    target.effective_end_dt = source.effective_end_dt
when not matched then insert (
    port_warehouse_id,
    port_id,
    warehouse_id,
    relationship_label,
    is_current,
    effective_start_dt,
    effective_end_dt
) values (
    source.port_warehouse_id,
    source.port_id,
    source.warehouse_id,
    source.relationship_label,
    source.is_current,
    source.effective_start_dt,
    source.effective_end_dt
);

prompt [10/11] seed sc_warehouse_product
merge into sc_warehouse_product target
using (
    select 5301 as warehouse_product_id, 4001 as warehouse_id, 'SKU-500' as product_id, 'STOCKS' as relationship_label, 'Y' as is_current, date '2026-01-01' as effective_start_dt, cast(null as date) as effective_end_dt from dual
    union all
    select 5302, 4002, 'SKU-700', 'STOCKS', 'Y', date '2026-01-01', cast(null as date) from dual
    union all
    select 5303, 4003, 'SKU-900', 'STOCKS', 'Y', date '2026-01-01', cast(null as date) from dual
) source
on (target.warehouse_product_id = source.warehouse_product_id)
when matched then update set
    target.warehouse_id = source.warehouse_id,
    target.product_id = source.product_id,
    target.relationship_label = source.relationship_label,
    target.is_current = source.is_current,
    target.effective_start_dt = source.effective_start_dt,
    target.effective_end_dt = source.effective_end_dt
when not matched then insert (
    warehouse_product_id,
    warehouse_id,
    product_id,
    relationship_label,
    is_current,
    effective_start_dt,
    effective_end_dt
) values (
    source.warehouse_product_id,
    source.warehouse_id,
    source.product_id,
    source.relationship_label,
    source.is_current,
    source.effective_start_dt,
    source.effective_end_dt
);

prompt [11/11] seed sc_alert_port
merge into sc_alert_port target
using (
    select 5401 as alert_port_id, 6001 as alert_id, 3001 as port_id, 'AFFECTS' as relationship_label, 'Y' as is_current, date '2026-01-01' as effective_start_dt, cast(null as date) as effective_end_dt from dual
    union all
    select 5402, 6002, 3002, 'AFFECTS', 'Y', date '2026-01-01', cast(null as date) from dual
    union all
    select 5403, 6003, 3003, 'AFFECTS', 'Y', date '2026-01-01', cast(null as date) from dual
) source
on (target.alert_port_id = source.alert_port_id)
when matched then update set
    target.alert_id = source.alert_id,
    target.port_id = source.port_id,
    target.relationship_label = source.relationship_label,
    target.is_current = source.is_current,
    target.effective_start_dt = source.effective_start_dt,
    target.effective_end_dt = source.effective_end_dt
when not matched then insert (
    alert_port_id,
    alert_id,
    port_id,
    relationship_label,
    is_current,
    effective_start_dt,
    effective_end_dt
) values (
    source.alert_port_id,
    source.alert_id,
    source.port_id,
    source.relationship_label,
    source.is_current,
    source.effective_start_dt,
    source.effective_end_dt
);

commit;
