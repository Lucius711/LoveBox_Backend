-- =====================================================================
-- Love Box — V11: sync dtb_admin_notifications với entity
-- =====================================================================
ALTER TABLE dtb_admin_notifications
    ADD COLUMN IF NOT EXISTS order_id      UUID,
    ADD COLUMN IF NOT EXISTS channel       VARCHAR(32) NOT NULL DEFAULT 'EMAIL',
    ADD COLUMN IF NOT EXISTS message       TEXT        NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS error_message TEXT;

CREATE INDEX IF NOT EXISTS idx_admin_notifications_order_id ON dtb_admin_notifications(order_id);
