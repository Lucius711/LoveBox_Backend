-- =====================================================================
-- V17: Cast verified_by from UUID to VARCHAR(255)
-- Must drop FK first (references dtb_users.id which is UUID)
-- Entity stores verified_by as String (admin email/name, not a FK)
-- =====================================================================

-- 1. Drop the FK constraint that blocks the type change
ALTER TABLE dtb_payment_transactions
    DROP CONSTRAINT IF EXISTS fk_payment_txn_verifier;

-- 2. Change the column type
ALTER TABLE dtb_payment_transactions
    ALTER COLUMN verified_by TYPE VARCHAR(255) USING verified_by::TEXT;
