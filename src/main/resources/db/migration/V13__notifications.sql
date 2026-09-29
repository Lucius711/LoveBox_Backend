-- Thông báo trong tài khoản (chuông trên header): có người thuê, đồ được duyệt, đơn Chủ đồ được duyệt...
CREATE TABLE dtb_notifications (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID         NOT NULL REFERENCES dtb_users(id) ON DELETE CASCADE,
    title      VARCHAR(255) NOT NULL,
    link       VARCHAR(255),                      -- đường dẫn frontend, vd /account?tab=owner
    read_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_notifications_user ON dtb_notifications(user_id, created_at DESC);
