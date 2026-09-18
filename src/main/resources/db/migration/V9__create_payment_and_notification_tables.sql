-- =====================================================================
-- Love Box — Flyway Migration V9 (Payment & Notification tables)
-- =====================================================================

-- 1. Payment Transactions
CREATE TABLE dtb_payment_transactions (
    id             UUID         NOT NULL DEFAULT gen_random_uuid(),
    order_id       UUID         NOT NULL,
    amount         BIGINT       NOT NULL,
    status         VARCHAR(32)  NOT NULL DEFAULT 'AWAITING_RECEIPT',
    receipt_url    VARCHAR(512),
    verified_at    TIMESTAMPTZ,
    verified_by    UUID,
    note           TEXT,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_dtb_payment_transactions PRIMARY KEY (id),
    CONSTRAINT uq_payment_transactions_order_id UNIQUE (order_id),
    CONSTRAINT fk_payment_txn_order FOREIGN KEY (order_id)
        REFERENCES dtb_orders(id) ON DELETE RESTRICT,
    CONSTRAINT fk_payment_txn_verifier FOREIGN KEY (verified_by)
        REFERENCES dtb_users(id) ON DELETE SET NULL,
    CONSTRAINT chk_payment_txn_status CHECK (
        status IN ('AWAITING_RECEIPT','RECEIPT_UPLOADED','VERIFIED','REJECTED')
    ),
    CONSTRAINT chk_payment_txn_amount CHECK (amount > 0)
);

CREATE INDEX idx_payment_transactions_order_id ON dtb_payment_transactions(order_id);
CREATE INDEX idx_payment_transactions_status   ON dtb_payment_transactions(status);

CREATE TRIGGER trg_payment_transactions_updated_at
    BEFORE UPDATE ON dtb_payment_transactions
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- 2. Admin Notifications
CREATE TABLE dtb_admin_notifications (
    id         UUID         NOT NULL DEFAULT gen_random_uuid(),
    type       VARCHAR(64)  NOT NULL,
    payload    JSONB,
    status     VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
    sent_at    TIMESTAMPTZ,
    error      TEXT,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_dtb_admin_notifications PRIMARY KEY (id),
    CONSTRAINT chk_admin_notifications_status CHECK (
        status IN ('PENDING','SENT','FAILED')
    )
);

CREATE INDEX idx_admin_notifications_status  ON dtb_admin_notifications(status);
CREATE INDEX idx_admin_notifications_type    ON dtb_admin_notifications(type);
CREATE INDEX idx_admin_notifications_created ON dtb_admin_notifications(created_at DESC);

CREATE TRIGGER trg_admin_notifications_updated_at
    BEFORE UPDATE ON dtb_admin_notifications
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
