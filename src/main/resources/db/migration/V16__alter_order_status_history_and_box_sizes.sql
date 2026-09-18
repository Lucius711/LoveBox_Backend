-- =====================================================================
-- V16: Fix dtb_order_status_history and dtb_box_sizes type mismatches
-- =====================================================================

-- ─────────────────────────────────────────────
-- 1. dtb_order_status_history
--    V8 created: status VARCHAR(32)
--    Entity expects: from_status, to_status, changed_by (no 'status')
-- ─────────────────────────────────────────────
ALTER TABLE dtb_order_status_history
    ADD COLUMN IF NOT EXISTS from_status VARCHAR(32),
    ADD COLUMN IF NOT EXISTS to_status   VARCHAR(32),
    ADD COLUMN IF NOT EXISTS changed_by  VARCHAR(255);

-- Back-fill to_status from old status column so data is not lost
UPDATE dtb_order_status_history
SET to_status = status
WHERE to_status IS NULL AND status IS NOT NULL;

-- Make to_status NOT NULL after back-fill
ALTER TABLE dtb_order_status_history
    ALTER COLUMN to_status SET NOT NULL;

-- ─────────────────────────────────────────────
-- 2. dtb_box_sizes
--    V4 created: price NUMERIC(12,0)
--    Entity expects: precision=12, scale=2 → NUMERIC(12,2)
-- ─────────────────────────────────────────────
ALTER TABLE dtb_box_sizes
    ALTER COLUMN price TYPE NUMERIC(12, 2) USING price::NUMERIC(12, 2);
