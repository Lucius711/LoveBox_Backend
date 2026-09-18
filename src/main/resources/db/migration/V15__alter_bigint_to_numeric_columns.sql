-- =====================================================================
-- V15: Cast BIGINT money columns to NUMERIC(12,2) to match entities
-- Affected: dtb_orders.total_amount, dtb_order_items.unit_price,
--           dtb_payment_transactions.amount
-- =====================================================================

-- dtb_orders
ALTER TABLE dtb_orders
    ALTER COLUMN total_amount TYPE NUMERIC(12, 2) USING total_amount::NUMERIC(12, 2);

-- dtb_order_items
ALTER TABLE dtb_order_items
    ALTER COLUMN unit_price TYPE NUMERIC(12, 2) USING unit_price::NUMERIC(12, 2);

-- dtb_payment_transactions
ALTER TABLE dtb_payment_transactions
    ALTER COLUMN amount TYPE NUMERIC(12, 2) USING amount::NUMERIC(12, 2);
