-- =====================================================================
-- Lentique — nền tảng cho thuê đồ tích hợp AI. Schema khởi tạo (DB mới).
-- Tiền tệ lưu BIGINT (VND, không có phần lẻ).
-- =====================================================================
CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS btree_gist;   -- cho EXCLUDE chống trùng lịch (uuid WITH =)

CREATE OR REPLACE FUNCTION set_updated_at() RETURNS TRIGGER AS $$
BEGIN NEW.updated_at = NOW(); RETURN NEW; END;
$$ LANGUAGE plpgsql;

-- ── Users ──────────────────────────────────────────────────────────────
CREATE TABLE dtb_users (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    google_sub    VARCHAR(255) NOT NULL UNIQUE,
    email         VARCHAR(255) NOT NULL UNIQUE,
    name          VARCHAR(255) NOT NULL,
    avatar_url    TEXT,
    role          VARCHAR(16)  NOT NULL DEFAULT 'RENTER' CHECK (role IN ('RENTER', 'OWNER', 'ADMIN')),
    phone         VARCHAR(20),
    address       TEXT,
    bank_account  VARCHAR(50),              -- STK nhận lại cọc
    bank_name     VARCHAR(100),
    trust_score   INT          NOT NULL DEFAULT 100,  -- bùng đồ / làm hỏng → bị trừ
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted_at    TIMESTAMPTZ
);
CREATE TRIGGER trg_users_updated_at BEFORE UPDATE ON dtb_users FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE dtb_auth_sessions (
    id                 UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id            UUID         NOT NULL REFERENCES dtb_users(id) ON DELETE CASCADE,
    refresh_token_hash VARCHAR(255) NOT NULL,
    expires_at         TIMESTAMPTZ  NOT NULL,
    user_agent         VARCHAR(500),
    ip_address         VARCHAR(45),
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    revoked_at         TIMESTAMPTZ
);
CREATE INDEX idx_auth_sessions_hash ON dtb_auth_sessions(refresh_token_hash) WHERE revoked_at IS NULL;

-- ── Products ───────────────────────────────────────────────────────────
CREATE TABLE dtb_products (
    id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id            UUID         NOT NULL REFERENCES dtb_users(id),
    name                VARCHAR(255) NOT NULL,
    description         TEXT         NOT NULL,
    category            VARCHAR(64)  NOT NULL,              -- Loại đồ: Áo dài, Đầm dạ hội, Veston...
    size                VARCHAR(4)   NOT NULL CHECK (size IN ('S', 'M', 'L', 'XL')),
    bust_max            INT          NOT NULL,              -- cm, số đo tối đa mặc vừa
    waist_max           INT          NOT NULL,
    hip_max             INT          NOT NULL,
    item_condition      VARCHAR(64)  NOT NULL,              -- Độ mới
    retail_price        BIGINT       NOT NULL CHECK (retail_price > 0),        -- giá niêm yết, để tính cọc
    rent_price_per_day  BIGINT       NOT NULL CHECK (rent_price_per_day > 0),
    deposit_percent     INT          NOT NULL DEFAULT 100 CHECK (deposit_percent BETWEEN 50 AND 100),
    status              VARCHAR(16)  NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'HIDDEN')),
    reject_reason       TEXT,
    rent_count          INT          NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_products_status ON dtb_products(status, created_at DESC);
CREATE INDEX idx_products_owner  ON dtb_products(owner_id);
CREATE TRIGGER trg_products_updated_at BEFORE UPDATE ON dtb_products FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- Nhãn cho AI: COLOR (màu), STYLE (phong cách), OCCASION (dịp), FEATURE (kiểu dáng/tính năng: trễ vai, che bắp tay...)
CREATE TABLE dtb_product_tags (
    product_id UUID        NOT NULL REFERENCES dtb_products(id) ON DELETE CASCADE,
    tag_type   VARCHAR(16) NOT NULL CHECK (tag_type IN ('COLOR', 'STYLE', 'OCCASION', 'FEATURE')),
    tag_value  VARCHAR(64) NOT NULL,
    PRIMARY KEY (product_id, tag_type, tag_value)
);
CREATE INDEX idx_product_tags_lookup ON dtb_product_tags(tag_type, tag_value);

CREATE TABLE dtb_product_images (
    product_id UUID NOT NULL REFERENCES dtb_products(id) ON DELETE CASCADE,
    sort_order INT  NOT NULL,
    url        TEXT NOT NULL,
    PRIMARY KEY (product_id, sort_order)
);

-- ── Bookings ───────────────────────────────────────────────────────────
CREATE TABLE dtb_bookings (
    id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    code                VARCHAR(20)  NOT NULL UNIQUE,
    checkout_code       BIGINT       NOT NULL,              -- gom các đơn cùng 1 lần thanh toán (= PayOS orderCode)
    product_id          UUID         NOT NULL REFERENCES dtb_products(id),
    renter_id           UUID         NOT NULL REFERENCES dtb_users(id),
    start_date          DATE         NOT NULL,
    end_date            DATE         NOT NULL,
    days                INT          NOT NULL,
    rent_amount         BIGINT       NOT NULL,
    deposit_amount      BIGINT       NOT NULL,
    shipping_fee        BIGINT       NOT NULL DEFAULT 0,
    total_amount        BIGINT       NOT NULL,
    status              VARCHAR(16)  NOT NULL DEFAULT 'PENDING' CHECK (status IN
                        ('PENDING', 'CONFIRMED', 'SHIPPING', 'RENTED', 'RETURNED', 'COMPLETED', 'DISPUTED', 'CANCELLED')),
    payment_method      VARCHAR(8)   NOT NULL CHECK (payment_method IN ('COD', 'PAYOS')),
    payment_status      VARCHAR(16)  NOT NULL DEFAULT 'UNPAID' CHECK (payment_status IN ('UNPAID', 'PAID', 'REFUNDED')),
    deposit_status      VARCHAR(16)  NOT NULL DEFAULT 'PENDING' CHECK (deposit_status IN ('PENDING', 'HELD', 'REFUNDED', 'FORFEITED')),
    deduction_amount    BIGINT       NOT NULL DEFAULT 0,    -- trừ cọc khi hư hỏng
    admin_note          TEXT,
    recipient_name      VARCHAR(255) NOT NULL,
    phone               VARCHAR(20)  NOT NULL,
    address             TEXT         NOT NULL,
    delivery_method     VARCHAR(16)  NOT NULL CHECK (delivery_method IN ('PICKUP', 'EXPRESS')),
    refund_bank_account VARCHAR(50)  NOT NULL,
    refund_bank_name    VARCHAR(100) NOT NULL,
    note                TEXT,
    payos_checkout_url  TEXT,
    payos_qr_code       TEXT,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CHECK (end_date >= start_date),
    -- Khoá lịch: [start, end + 1 ngày đệm giặt ủi]. daterange mặc định [) nên cận trên = end + 2.
    CONSTRAINT excl_bookings_no_overlap EXCLUDE USING gist (
        product_id WITH =, daterange(start_date, end_date + 2) WITH &&
    ) WHERE (status <> 'CANCELLED')
);
CREATE INDEX idx_bookings_renter   ON dtb_bookings(renter_id, created_at DESC);
CREATE INDEX idx_bookings_product  ON dtb_bookings(product_id, start_date);
CREATE INDEX idx_bookings_checkout ON dtb_bookings(checkout_code);
CREATE TRIGGER trg_bookings_updated_at BEFORE UPDATE ON dtb_bookings FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ── Reviews ────────────────────────────────────────────────────────────
CREATE TABLE dtb_reviews (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID        NOT NULL UNIQUE REFERENCES dtb_bookings(id),
    product_id UUID        NOT NULL REFERENCES dtb_products(id) ON DELETE CASCADE,
    user_id    UUID        NOT NULL REFERENCES dtb_users(id),
    rating     INT         NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment    TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_reviews_product ON dtb_reviews(product_id, created_at DESC);
