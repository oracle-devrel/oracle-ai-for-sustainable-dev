WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

CREATE OR REPLACE PROCEDURE approve_inventory_transfer_mcp (
    p_transfer_id        IN NUMBER,
    p_product_id         IN NUMBER,
    p_source_location_id IN NUMBER,
    p_target_location_id IN NUMBER,
    p_transfer_qty       IN NUMBER,
    p_approval_notes     IN VARCHAR2,
    p_requested_by       IN VARCHAR2
) AUTHID DEFINER AS
    v_position_count NUMBER := 0;
    v_source_surplus NUMBER := 0;
    v_target_shortage NUMBER := 0;
    v_safe_transfer_qty NUMBER := 0;
BEGIN
    IF p_transfer_id < 1 THEN
        RAISE_APPLICATION_ERROR(-20001, 'Transfer ID must be positive');
    END IF;

    IF p_product_id < 1 OR p_source_location_id < 1 OR p_target_location_id < 1
       OR p_source_location_id = p_target_location_id THEN
        RAISE_APPLICATION_ERROR(-20002, 'Invalid product or transfer locations');
    END IF;

    IF p_transfer_qty < 1 THEN
        RAISE_APPLICATION_ERROR(-20003, 'Transfer quantity must be positive');
    END IF;

    IF LENGTH(TRIM(p_approval_notes)) NOT BETWEEN 3 AND 2000 THEN
        RAISE_APPLICATION_ERROR(-20004, 'Approval notes must contain 3 to 2000 characters');
    END IF;

    IF LENGTH(TRIM(p_requested_by)) NOT BETWEEN 3 AND 200 THEN
        RAISE_APPLICATION_ERROR(-20005, 'Requested by must contain 3 to 200 characters');
    END IF;

    -- Lock both inventory positions in deterministic order to prevent races
    -- between concurrent transfer approvals.
    FOR position IN (
        SELECT location_id,
               on_hand_qty,
               reserved_qty,
               inbound_qty,
               forecast_7d_qty,
               safety_stock_qty
          FROM inventory_positions
         WHERE product_id = p_product_id
           AND location_id IN (p_source_location_id, p_target_location_id)
         ORDER BY location_id
         FOR UPDATE
    ) LOOP
        v_position_count := v_position_count + 1;
        IF position.location_id = p_source_location_id THEN
            v_source_surplus := GREATEST(
                position.on_hand_qty - position.reserved_qty
                - position.forecast_7d_qty - position.safety_stock_qty,
                0);
        ELSE
            v_target_shortage := GREATEST(
                position.forecast_7d_qty + position.safety_stock_qty
                - (position.on_hand_qty - position.reserved_qty + position.inbound_qty),
                0);
        END IF;
    END LOOP;

    IF v_position_count <> 2 THEN
        RAISE_APPLICATION_ERROR(-20006, 'Source or target inventory position was not found');
    END IF;

    v_safe_transfer_qty := LEAST(v_source_surplus, v_target_shortage);
    IF p_transfer_qty > v_safe_transfer_qty THEN
        RAISE_APPLICATION_ERROR(
            -20007,
            'Recommendation is stale; current safe transfer quantity is ' ||
            TO_CHAR(v_safe_transfer_qty));
    END IF;

    INSERT INTO inventory_transfers (
        transfer_id, product_id, source_location_id, target_location_id,
        transfer_qty, approval_notes, requested_by, transfer_status
    ) VALUES (
        p_transfer_id, p_product_id, p_source_location_id, p_target_location_id,
        p_transfer_qty, p_approval_notes, p_requested_by, 'APPROVED'
    );

    UPDATE inventory_positions
       SET reserved_qty = reserved_qty + p_transfer_qty,
           last_updated = SYSTIMESTAMP
     WHERE product_id = p_product_id
       AND location_id = p_source_location_id;

    -- The Toolkit commits only after the entire tool statement succeeds.
END;
/
