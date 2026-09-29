-- Đơn đăng ký làm Chủ đồ: khách thuê gửi đơn → admin duyệt → role OWNER (thay cho nút "Trở thành Chủ đồ").
CREATE TABLE dtb_owner_applications (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID         NOT NULL REFERENCES dtb_users(id) ON DELETE CASCADE,
    phone         VARCHAR(16)  NOT NULL,
    address       VARCHAR(500) NOT NULL,              -- nơi lấy / nhận lại đồ
    bank_account  VARCHAR(32)  NOT NULL,              -- nhận tiền thuê
    bank_name     VARCHAR(64)  NOT NULL,
    intro         TEXT         NOT NULL,              -- định cho thuê loại đồ gì, bao nhiêu món, tình trạng
    status        VARCHAR(16)  NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    reject_reason TEXT,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    reviewed_at   TIMESTAMPTZ
);
CREATE INDEX idx_owner_apps_status ON dtb_owner_applications(status, created_at);
CREATE UNIQUE INDEX uq_owner_apps_pending ON dtb_owner_applications(user_id) WHERE status = 'PENDING';   -- mỗi người 1 đơn đang chờ
