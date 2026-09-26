-- Hồ sơ phong cách (hỏi khi đăng nhập lần đầu) — dùng để cá nhân hoá gợi ý đồ.
-- Danh sách lưu dạng chuỗi cách nhau dấu phẩy (giá trị lấy từ bộ nhãn chuẩn, không chứa dấu phẩy).
ALTER TABLE dtb_users
    ADD COLUMN height_cm     INT,
    ADD COLUMN weight_kg     INT,
    ADD COLUMN bust          INT,
    ADD COLUMN waist         INT,
    ADD COLUMN hip           INT,
    ADD COLUMN budget_max    BIGINT,
    ADD COLUMN fav_occasions TEXT NOT NULL DEFAULT '',
    ADD COLUMN fav_styles    TEXT NOT NULL DEFAULT '',
    ADD COLUMN fav_colors    TEXT NOT NULL DEFAULT '',
    ADD COLUMN fit_note      TEXT,
    ADD COLUMN onboarded_at  TIMESTAMPTZ;

-- Hoàn cọc tự động qua PayOS Payout
ALTER TABLE dtb_bookings
    ADD COLUMN refund_amount BIGINT,
    ADD COLUMN refund_ref    VARCHAR(64),
    ADD COLUMN refunded_at   TIMESTAMPTZ;
