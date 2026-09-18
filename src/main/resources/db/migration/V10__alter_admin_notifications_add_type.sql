-- =====================================================================
-- Love Box — V10: thêm cột type vào dtb_admin_notifications
-- =====================================================================
ALTER TABLE dtb_admin_notifications
    ADD COLUMN IF NOT EXISTS type VARCHAR(64) NOT NULL DEFAULT 'GENERAL';

CREATE INDEX IF NOT EXISTS idx_admin_notifications_type ON dtb_admin_notifications(type);
