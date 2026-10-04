WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

CREATE TABLE supply_locations (
    location_id       NUMBER PRIMARY KEY,
    location_code     VARCHAR2(30) NOT NULL UNIQUE,
    location_name     VARCHAR2(200) NOT NULL,
    region_name       VARCHAR2(100) NOT NULL,
    location_type     VARCHAR2(30) NOT NULL,
    CONSTRAINT ck_supply_location_type
        CHECK (location_type IN ('DISTRIBUTION_CENTER', 'STORE', 'FULFILLMENT_CENTER'))
);

CREATE TABLE supply_products (
    product_id        NUMBER PRIMARY KEY,
    sku               VARCHAR2(40) NOT NULL UNIQUE,
    product_name      VARCHAR2(200) NOT NULL,
    category_name     VARCHAR2(100) NOT NULL,
    unit_value        NUMBER(12,2) NOT NULL,
    CONSTRAINT ck_supply_product_value CHECK (unit_value >= 0)
);

CREATE TABLE inventory_positions (
    product_id        NUMBER NOT NULL,
    location_id       NUMBER NOT NULL,
    on_hand_qty       NUMBER(12) NOT NULL,
    reserved_qty      NUMBER(12) DEFAULT 0 NOT NULL,
    inbound_qty       NUMBER(12) DEFAULT 0 NOT NULL,
    forecast_7d_qty   NUMBER(12) NOT NULL,
    safety_stock_qty  NUMBER(12) NOT NULL,
    last_updated      TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT inventory_positions_pk PRIMARY KEY (product_id, location_id),
    CONSTRAINT fk_inventory_product FOREIGN KEY (product_id)
        REFERENCES supply_products(product_id),
    CONSTRAINT fk_inventory_location FOREIGN KEY (location_id)
        REFERENCES supply_locations(location_id),
    CONSTRAINT ck_inventory_quantities CHECK (
        on_hand_qty >= 0 AND reserved_qty >= 0 AND inbound_qty >= 0
        AND forecast_7d_qty >= 0 AND safety_stock_qty >= 0
        AND reserved_qty <= on_hand_qty)
);

CREATE TABLE supply_lanes (
    source_location_id NUMBER NOT NULL,
    target_location_id NUMBER NOT NULL,
    transit_days       NUMBER(3) NOT NULL,
    unit_transfer_cost NUMBER(10,2) NOT NULL,
    active_flag        CHAR(1) DEFAULT 'Y' NOT NULL,
    CONSTRAINT supply_lanes_pk PRIMARY KEY (source_location_id, target_location_id),
    CONSTRAINT fk_lane_source FOREIGN KEY (source_location_id)
        REFERENCES supply_locations(location_id),
    CONSTRAINT fk_lane_target FOREIGN KEY (target_location_id)
        REFERENCES supply_locations(location_id),
    CONSTRAINT ck_lane_locations CHECK (source_location_id <> target_location_id),
    CONSTRAINT ck_lane_values CHECK (
        transit_days BETWEEN 1 AND 90
        AND unit_transfer_cost >= 0
        AND active_flag IN ('Y', 'N'))
);

CREATE TABLE inventory_transfers (
    transfer_id        NUMBER PRIMARY KEY,
    product_id         NUMBER NOT NULL,
    source_location_id NUMBER NOT NULL,
    target_location_id NUMBER NOT NULL,
    transfer_qty       NUMBER(12) NOT NULL,
    approval_notes     VARCHAR2(2000) NOT NULL,
    requested_by       VARCHAR2(200) NOT NULL,
    transfer_status    VARCHAR2(30) NOT NULL,
    approved_at        TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    completed_at       TIMESTAMP,
    CONSTRAINT fk_transfer_product FOREIGN KEY (product_id)
        REFERENCES supply_products(product_id),
    CONSTRAINT fk_transfer_source FOREIGN KEY (source_location_id)
        REFERENCES supply_locations(location_id),
    CONSTRAINT fk_transfer_target FOREIGN KEY (target_location_id)
        REFERENCES supply_locations(location_id),
    CONSTRAINT ck_transfer_locations CHECK (source_location_id <> target_location_id),
    CONSTRAINT ck_transfer_qty CHECK (transfer_qty > 0),
    CONSTRAINT ck_transfer_status CHECK (
        transfer_status IN ('APPROVED', 'IN_TRANSIT', 'COMPLETED', 'CANCELLED'))
);

CREATE INDEX inventory_positions_ix1
    ON inventory_positions(location_id, product_id);
CREATE INDEX inventory_transfers_ix1
    ON inventory_transfers(target_location_id, approved_at DESC);
