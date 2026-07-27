WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

INSERT INTO supply_locations VALUES (101, 'ATL-DC', 'Atlanta Distribution Center', 'Southeast', 'DISTRIBUTION_CENTER');
INSERT INTO supply_locations VALUES (102, 'PHX-DC', 'Phoenix Distribution Center', 'Southwest', 'DISTRIBUTION_CENTER');
INSERT INTO supply_locations VALUES (103, 'CHI-FC', 'Chicago Fulfillment Center', 'Midwest', 'FULFILLMENT_CENTER');
INSERT INTO supply_locations VALUES (104, 'SEA-FC', 'Seattle Fulfillment Center', 'Northwest', 'FULFILLMENT_CENTER');

INSERT INTO supply_products VALUES (1001, 'BAT-48V', '48V Solar Battery Pack', 'Energy Storage', 1299.00);
INSERT INTO supply_products VALUES (1002, 'THERM-PRO', 'Smart Thermostat Pro', 'Building Controls', 189.00);
INSERT INTO supply_products VALUES (1003, 'EVSE-22K', '22kW EV Charger', 'EV Infrastructure', 899.00);
INSERT INTO supply_products VALUES (1004, 'WATER-SENSE', 'Connected Water Sensor', 'Environmental Monitoring', 79.00);
INSERT INTO supply_products VALUES (1005, 'GRID-CTRL', 'Edge Grid Controller', 'Grid Automation', 2499.00);

-- Each product has both constrained and surplus locations so the governed view
-- can calculate an actionable transfer rather than merely report a shortage.
INSERT INTO inventory_positions VALUES (1001, 101, 18, 4, 0, 26, 8, SYSTIMESTAMP);
INSERT INTO inventory_positions VALUES (1001, 102, 92, 7, 4, 21, 18, SYSTIMESTAMP);
INSERT INTO inventory_positions VALUES (1001, 103, 48, 6, 0, 24, 10, SYSTIMESTAMP);
INSERT INTO inventory_positions VALUES (1001, 104, 37, 5, 8, 20, 9, SYSTIMESTAMP);

INSERT INTO inventory_positions VALUES (1002, 101, 160, 20, 25, 72, 30, SYSTIMESTAMP);
INSERT INTO inventory_positions VALUES (1002, 102, 29, 6, 0, 41, 15, SYSTIMESTAMP);
INSERT INTO inventory_positions VALUES (1002, 103, 124, 17, 10, 61, 24, SYSTIMESTAMP);
INSERT INTO inventory_positions VALUES (1002, 104, 81, 8, 12, 46, 18, SYSTIMESTAMP);

INSERT INTO inventory_positions VALUES (1003, 101, 44, 5, 0, 19, 8, SYSTIMESTAMP);
INSERT INTO inventory_positions VALUES (1003, 102, 51, 7, 0, 22, 9, SYSTIMESTAMP);
INSERT INTO inventory_positions VALUES (1003, 103, 13, 3, 0, 18, 7, SYSTIMESTAMP);
INSERT INTO inventory_positions VALUES (1003, 104, 64, 8, 6, 24, 10, SYSTIMESTAMP);

INSERT INTO inventory_positions VALUES (1004, 101, 210, 26, 20, 91, 38, SYSTIMESTAMP);
INSERT INTO inventory_positions VALUES (1004, 102, 174, 18, 18, 82, 32, SYSTIMESTAMP);
INSERT INTO inventory_positions VALUES (1004, 103, 158, 22, 12, 77, 30, SYSTIMESTAMP);
INSERT INTO inventory_positions VALUES (1004, 104, 24, 5, 0, 52, 18, SYSTIMESTAMP);

INSERT INTO inventory_positions VALUES (1005, 101, 31, 4, 0, 12, 6, SYSTIMESTAMP);
INSERT INTO inventory_positions VALUES (1005, 102, 27, 3, 2, 11, 5, SYSTIMESTAMP);
INSERT INTO inventory_positions VALUES (1005, 103, 35, 5, 0, 14, 6, SYSTIMESTAMP);
INSERT INTO inventory_positions VALUES (1005, 104, 8, 2, 0, 10, 5, SYSTIMESTAMP);

INSERT INTO supply_lanes VALUES (101, 102, 3, 18.50, 'Y');
INSERT INTO supply_lanes VALUES (101, 103, 2, 13.25, 'Y');
INSERT INTO supply_lanes VALUES (101, 104, 5, 25.00, 'Y');
INSERT INTO supply_lanes VALUES (102, 101, 3, 18.50, 'Y');
INSERT INTO supply_lanes VALUES (102, 103, 3, 16.75, 'Y');
INSERT INTO supply_lanes VALUES (102, 104, 3, 17.25, 'Y');
INSERT INTO supply_lanes VALUES (103, 101, 2, 13.25, 'Y');
INSERT INTO supply_lanes VALUES (103, 102, 3, 16.75, 'Y');
INSERT INTO supply_lanes VALUES (103, 104, 4, 19.50, 'Y');
INSERT INTO supply_lanes VALUES (104, 101, 5, 25.00, 'Y');
INSERT INTO supply_lanes VALUES (104, 102, 3, 17.25, 'Y');
INSERT INTO supply_lanes VALUES (104, 103, 4, 19.50, 'Y');

COMMIT;
