-- =====================================================================
-- LOVE BOX — DATABASE SCHEMA (PostgreSQL 16)
-- Convention: 100% align IGOGO-backend
--   • Prefix     : dtb_* cho tất cả bảng
--   • PK         : UUID gen_random_uuid()
--   • Timestamps : created_at / updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
--   • Soft-delete: deleted_at TIMESTAMPTZ (nullable) nơi cần
--   • Constraint : chk_* CHECK,  uq_* UNIQUE,  fk_* FK,  pk_* PK
--   • Index      : idx_*
--   • Trigger    : updated_at tự động qua set_updated_at()
--   • Login      : Google OAuth duy nhất — KHÔNG có password / guest_token
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Shared trigger function (giống pattern IGOGO)
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;


-- =====================================================================
-- 1. USERS  (Google OAuth only — không có password_hash)
-- =====================================================================
CREATE TABLE dtb_users (
    id              UUID            NOT NULL DEFAULT gen_random_uuid(),
    google_sub      VARCHAR(64)     NOT NULL,               -- Google "sub" claim (stable ID)
    email           VARCHAR(255)    NOT NULL,
    full_name       VARCHAR(255)    NOT NULL,
    avatar_url      TEXT,
    phone           VARCHAR(20),
    role            VARCHAR(16)     NOT NULL DEFAULT 'CUSTOMER',
    status          VARCHAR(16)     NOT NULL DEFAULT 'ACTIVE',
    last_login_at   TIMESTAMPTZ,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ,

    CONSTRAINT pk_dtb_users PRIMARY KEY (id),
    CONSTRAINT uq_dtb_users_google_sub UNIQUE (google_sub),
    CONSTRAINT uq_dtb_users_email UNIQUE (email),
    CONSTRAINT chk_dtb_users_role   CHECK (role   IN ('CUSTOMER', 'ADMIN')),
    CONSTRAINT chk_dtb_users_status CHECK (status IN ('ACTIVE', 'DISABLED'))
);

CREATE INDEX idx_dtb_users_status     ON dtb_users(status) WHERE deleted_at IS NULL;
CREATE INDEX idx_dtb_users_created_at ON dtb_users(created_at DESC);

CREATE TRIGGER trg_dtb_users_updated_at
    BEFORE UPDATE ON dtb_users
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- Refresh-token store (giống auth_sessions của IGOGO)
-- Access token (JWT) không lưu DB; chỉ lưu hash refresh token
CREATE TABLE dtb_auth_sessions (
    id                  UUID            NOT NULL DEFAULT gen_random_uuid(),
    user_id             UUID            NOT NULL,
    refresh_token_hash  VARCHAR(255)    NOT NULL,           -- bcrypt/sha256 hash, không lưu raw
    user_agent          TEXT,
    ip_address          VARCHAR(45),
    expires_at          TIMESTAMPTZ     NOT NULL,
    revoked_at          TIMESTAMPTZ,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_dtb_auth_sessions PRIMARY KEY (id),
    CONSTRAINT uq_dtb_auth_sessions_token UNIQUE (refresh_token_hash),
    CONSTRAINT fk_dtb_auth_sessions_user
        FOREIGN KEY (user_id) REFERENCES dtb_users(id) ON DELETE CASCADE
);

CREATE INDEX idx_dtb_auth_sessions_user_id   ON dtb_auth_sessions(user_id);
CREATE INDEX idx_dtb_auth_sessions_expires_at ON dtb_auth_sessions(expires_at);


-- =====================================================================
-- 2. CATALOG (danh mục + bảng giá + mẫu showcase dựng sẵn)
-- =====================================================================
CREATE TABLE dtb_product_categories (
    id              UUID            NOT NULL DEFAULT gen_random_uuid(),
    name            VARCHAR(100)    NOT NULL,               -- "Cute Baby", "Thanh lịch"
    slug            VARCHAR(100)    NOT NULL,
    description     TEXT,
    display_order   SMALLINT        NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_dtb_product_categories PRIMARY KEY (id),
    CONSTRAINT uq_dtb_product_categories_slug UNIQUE (slug)
);

CREATE TRIGGER trg_dtb_product_categories_updated_at
    BEFORE UPDATE ON dtb_product_categories
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


CREATE TABLE dtb_box_sizes (
    id              UUID            NOT NULL DEFAULT gen_random_uuid(),
    code            VARCHAR(32)     NOT NULL,               -- 'STANDARD', 'DELUXE'
    name            VARCHAR(100)    NOT NULL,
    price           NUMERIC(12, 0)  NOT NULL,               -- VND
    description     TEXT,
    is_active       SMALLINT        NOT NULL DEFAULT 1,     -- 1 / 0 (align IGOGO dùng smallint)
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_dtb_box_sizes PRIMARY KEY (id),
    CONSTRAINT uq_dtb_box_sizes_code UNIQUE (code),
    CONSTRAINT chk_dtb_box_sizes_price     CHECK (price > 0),
    CONSTRAINT chk_dtb_box_sizes_is_active CHECK (is_active IN (0, 1))
);

CREATE TRIGGER trg_dtb_box_sizes_updated_at
    BEFORE UPDATE ON dtb_box_sizes
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


CREATE TABLE dtb_showcase_designs (
    id              UUID            NOT NULL DEFAULT gen_random_uuid(),
    category_id     UUID            NOT NULL,
    box_size_id     UUID            NOT NULL,
    name            VARCHAR(255)    NOT NULL,
    description     TEXT,
    image_url       TEXT            NOT NULL,
    is_featured     SMALLINT        NOT NULL DEFAULT 0,
    display_order   SMALLINT        NOT NULL DEFAULT 0,
    status          VARCHAR(16)     NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ,

    CONSTRAINT pk_dtb_showcase_designs PRIMARY KEY (id),
    CONSTRAINT fk_dtb_showcase_designs_category
        FOREIGN KEY (category_id) REFERENCES dtb_product_categories(id) ON DELETE RESTRICT,
    CONSTRAINT fk_dtb_showcase_designs_box_size
        FOREIGN KEY (box_size_id) REFERENCES dtb_box_sizes(id) ON DELETE RESTRICT,
    CONSTRAINT chk_dtb_showcase_designs_status
        CHECK (status IN ('ACTIVE', 'HIDDEN')),
    CONSTRAINT chk_dtb_showcase_designs_is_featured
        CHECK (is_featured IN (0, 1))
);

CREATE INDEX idx_dtb_showcase_designs_category_id ON dtb_showcase_designs(category_id);
CREATE INDEX idx_dtb_showcase_designs_status       ON dtb_showcase_designs(status) WHERE deleted_at IS NULL;

CREATE TRIGGER trg_dtb_showcase_designs_updated_at
    BEFORE UPDATE ON dtb_showcase_designs
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- =====================================================================
-- 3. AI STUDIO — Chat + Image Generation
--    Bắt buộc đăng nhập Google → user_id NOT NULL (không có guest_token)
-- =====================================================================
CREATE TABLE dtb_ai_conversations (
    id              UUID            NOT NULL DEFAULT gen_random_uuid(),
    user_id         UUID            NOT NULL,               -- bắt buộc login Google
    title           VARCHAR(255),
    message_count   INTEGER         NOT NULL DEFAULT 0,
    status          VARCHAR(20)     NOT NULL DEFAULT 'active',
    -- Rolling summary để giữ context dài (giống IGOGO aichat)
    summary         TEXT,
    summarized_through_at TIMESTAMPTZ,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ,

    CONSTRAINT pk_dtb_ai_conversations PRIMARY KEY (id),
    CONSTRAINT fk_dtb_ai_conversations_user
        FOREIGN KEY (user_id) REFERENCES dtb_users(id) ON DELETE CASCADE,
    CONSTRAINT chk_dtb_ai_conversations_status
        CHECK (status IN ('active', 'completed', 'abandoned'))
);

CREATE INDEX idx_dtb_ai_conversations_user_id ON dtb_ai_conversations(user_id);
CREATE INDEX idx_dtb_ai_conversations_status  ON dtb_ai_conversations(status);

CREATE TRIGGER trg_dtb_ai_conversations_updated_at
    BEFORE UPDATE ON dtb_ai_conversations
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- Lưu từng lượt chat (user prompt + bot reply)
CREATE TABLE dtb_ai_messages (
    id                  UUID            NOT NULL DEFAULT gen_random_uuid(),
    conversation_id     UUID            NOT NULL,
    role                VARCHAR(20)     NOT NULL,           -- 'user' | 'assistant' | 'system'
    content             TEXT            NOT NULL,
    -- Token tracking (giống IGOGO dtb_aichat_messages)
    prompt_tokens       INTEGER,
    completion_tokens   INTEGER,
    processing_time_ms  INTEGER,
    from_cache          SMALLINT        NOT NULL DEFAULT 0, -- 0 / 1 (align IGOGO)
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_dtb_ai_messages PRIMARY KEY (id),
    CONSTRAINT fk_dtb_ai_messages_conversation
        FOREIGN KEY (conversation_id) REFERENCES dtb_ai_conversations(id) ON DELETE CASCADE,
    CONSTRAINT chk_dtb_ai_messages_role
        CHECK (role IN ('user', 'assistant', 'system')),
    CONSTRAINT chk_dtb_ai_messages_from_cache
        CHECK (from_cache IN (0, 1))
);

CREATE INDEX idx_dtb_ai_messages_conversation_created
    ON dtb_ai_messages(conversation_id, created_at);


-- Mỗi lần bấm "Gửi" = 1 generation request tới DALL-E 3
CREATE TABLE dtb_ai_image_generations (
    id                  UUID            NOT NULL DEFAULT gen_random_uuid(),
    conversation_id     UUID            NOT NULL,
    message_id          UUID,                               -- message chứa prompt gốc
    prompt              TEXT            NOT NULL,
    -- DALL-E 3 tự rewrite prompt — lưu lại để debug & compliance
    revised_prompt      TEXT,
    provider            VARCHAR(32)     NOT NULL DEFAULT 'OPENAI_DALLE3',
    model               VARCHAR(32)     NOT NULL DEFAULT 'dall-e-3',
    size                VARCHAR(16)     NOT NULL DEFAULT '1024x1024',
    quality             VARCHAR(16)     NOT NULL DEFAULT 'standard',
    status              VARCHAR(16)     NOT NULL DEFAULT 'pending',
    error_message       TEXT,
    -- Token tracking (DALL-E tính theo image, nhưng vẫn log để billing)
    prompt_tokens       INTEGER,
    request_payload     TEXT,                               -- JSON params gửi API
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_dtb_ai_image_generations PRIMARY KEY (id),
    CONSTRAINT fk_dtb_ai_image_generations_conversation
        FOREIGN KEY (conversation_id) REFERENCES dtb_ai_conversations(id) ON DELETE CASCADE,
    CONSTRAINT fk_dtb_ai_image_generations_message
        FOREIGN KEY (message_id) REFERENCES dtb_ai_messages(id) ON DELETE SET NULL,
    CONSTRAINT chk_dtb_ai_image_generations_status
        CHECK (status IN ('pending', 'processing', 'success', 'failed')),
    CONSTRAINT chk_dtb_ai_image_generations_quality
        CHECK (quality IN ('standard', 'hd'))
);

CREATE INDEX idx_dtb_ai_image_generations_conversation_id
    ON dtb_ai_image_generations(conversation_id);
CREATE INDEX idx_dtb_ai_image_generations_status
    ON dtb_ai_image_generations(status);

CREATE TRIGGER trg_dtb_ai_image_generations_updated_at
    BEFORE UPDATE ON dtb_ai_image_generations
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- 1–2 ảnh kết quả cho mỗi generation
CREATE TABLE dtb_ai_generated_images (
    id              UUID            NOT NULL DEFAULT gen_random_uuid(),
    generation_id   UUID            NOT NULL,
    image_url       TEXT            NOT NULL,
    thumbnail_url   TEXT,
    width           INTEGER,
    height          INTEGER,
    display_order   SMALLINT        NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_dtb_ai_generated_images PRIMARY KEY (id),
    CONSTRAINT fk_dtb_ai_generated_images_generation
        FOREIGN KEY (generation_id) REFERENCES dtb_ai_image_generations(id) ON DELETE CASCADE
);

CREATE INDEX idx_dtb_ai_generated_images_generation_id
    ON dtb_ai_generated_images(generation_id);


-- =====================================================================
-- 4. GIFT DESIGN đã chốt (source: AI render HOẶC chọn từ showcase)
-- =====================================================================
CREATE TABLE dtb_gift_designs (
    id                      UUID            NOT NULL DEFAULT gen_random_uuid(),
    user_id                 UUID            NOT NULL,
    source_type             VARCHAR(16)     NOT NULL,       -- 'AI_GENERATED' | 'SHOWCASE'
    ai_generated_image_id   UUID,
    showcase_design_id      UUID,
    conversation_id         UUID,
    box_size_id             UUID            NOT NULL,
    final_image_url         TEXT            NOT NULL,       -- snapshot ảnh đã chốt
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_dtb_gift_designs PRIMARY KEY (id),
    CONSTRAINT fk_dtb_gift_designs_user
        FOREIGN KEY (user_id) REFERENCES dtb_users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_dtb_gift_designs_ai_image
        FOREIGN KEY (ai_generated_image_id) REFERENCES dtb_ai_generated_images(id) ON DELETE RESTRICT,
    CONSTRAINT fk_dtb_gift_designs_showcase
        FOREIGN KEY (showcase_design_id) REFERENCES dtb_showcase_designs(id) ON DELETE RESTRICT,
    CONSTRAINT fk_dtb_gift_designs_conversation
        FOREIGN KEY (conversation_id) REFERENCES dtb_ai_conversations(id) ON DELETE SET NULL,
    CONSTRAINT fk_dtb_gift_designs_box_size
        FOREIGN KEY (box_size_id) REFERENCES dtb_box_sizes(id) ON DELETE RESTRICT,
    CONSTRAINT chk_dtb_gift_designs_source_type
        CHECK (source_type IN ('AI_GENERATED', 'SHOWCASE')),
    -- Bảo đảm tính loại trừ: AI → phải có ai_image, SHOWCASE → phải có showcase
    CONSTRAINT chk_dtb_gift_designs_source_exclusive CHECK (
        (source_type = 'AI_GENERATED'
            AND ai_generated_image_id IS NOT NULL
            AND showcase_design_id   IS NULL)
        OR
        (source_type = 'SHOWCASE'
            AND showcase_design_id   IS NOT NULL
            AND ai_generated_image_id IS NULL)
    )
);

CREATE INDEX idx_dtb_gift_designs_user_id     ON dtb_gift_designs(user_id);
CREATE INDEX idx_dtb_gift_designs_box_size_id ON dtb_gift_designs(box_size_id);

CREATE TRIGGER trg_dtb_gift_designs_updated_at
    BEFORE UPDATE ON dtb_gift_designs
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- Lời chúc → mã hóa thành QR in trên thiệp vật lý
CREATE TABLE dtb_greeting_wishes (
    id              UUID            NOT NULL DEFAULT gen_random_uuid(),
    gift_design_id  UUID            NOT NULL,
    user_id         UUID            NOT NULL,
    content         TEXT            NOT NULL,
    qr_token        UUID            NOT NULL DEFAULT gen_random_uuid(), -- payload encode vào QR
    qr_image_url    TEXT,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_dtb_greeting_wishes PRIMARY KEY (id),
    CONSTRAINT fk_dtb_greeting_wishes_gift_design
        FOREIGN KEY (gift_design_id) REFERENCES dtb_gift_designs(id) ON DELETE CASCADE,
    CONSTRAINT fk_dtb_greeting_wishes_user
        FOREIGN KEY (user_id) REFERENCES dtb_users(id) ON DELETE RESTRICT,
    CONSTRAINT uq_dtb_greeting_wishes_qr_token UNIQUE (qr_token),
    CONSTRAINT chk_dtb_greeting_wishes_content_len
        CHECK (char_length(content) BETWEEN 1 AND 2000)
);

CREATE INDEX idx_dtb_greeting_wishes_gift_design_id ON dtb_greeting_wishes(gift_design_id);
CREATE INDEX idx_dtb_greeting_wishes_user_id        ON dtb_greeting_wishes(user_id);

CREATE TRIGGER trg_dtb_greeting_wishes_updated_at
    BEFORE UPDATE ON dtb_greeting_wishes
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- =====================================================================
-- 5. CART (bắt buộc login — user_id NOT NULL)
-- =====================================================================
CREATE TABLE dtb_carts (
    id              UUID            NOT NULL DEFAULT gen_random_uuid(),
    user_id         UUID            NOT NULL,               -- bắt buộc login Google
    status          VARCHAR(16)     NOT NULL DEFAULT 'active',
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_dtb_carts PRIMARY KEY (id),
    CONSTRAINT fk_dtb_carts_user
        FOREIGN KEY (user_id) REFERENCES dtb_users(id) ON DELETE CASCADE,
    CONSTRAINT chk_dtb_carts_status
        CHECK (status IN ('active', 'converted', 'abandoned'))
);

CREATE INDEX idx_dtb_carts_user_id ON dtb_carts(user_id);
CREATE INDEX idx_dtb_carts_status  ON dtb_carts(status);

CREATE TRIGGER trg_dtb_carts_updated_at
    BEFORE UPDATE ON dtb_carts
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


CREATE TABLE dtb_cart_items (
    id              UUID            NOT NULL DEFAULT gen_random_uuid(),
    cart_id         UUID            NOT NULL,
    gift_design_id  UUID            NOT NULL,
    wish_id         UUID,
    box_size_id     UUID            NOT NULL,
    quantity        SMALLINT        NOT NULL DEFAULT 1,
    unit_price      NUMERIC(12, 0)  NOT NULL,               -- snapshot giá tại thời điểm thêm
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_dtb_cart_items PRIMARY KEY (id),
    CONSTRAINT fk_dtb_cart_items_cart
        FOREIGN KEY (cart_id) REFERENCES dtb_carts(id) ON DELETE CASCADE,
    CONSTRAINT fk_dtb_cart_items_gift_design
        FOREIGN KEY (gift_design_id) REFERENCES dtb_gift_designs(id) ON DELETE RESTRICT,
    CONSTRAINT fk_dtb_cart_items_wish
        FOREIGN KEY (wish_id) REFERENCES dtb_greeting_wishes(id) ON DELETE SET NULL,
    CONSTRAINT fk_dtb_cart_items_box_size
        FOREIGN KEY (box_size_id) REFERENCES dtb_box_sizes(id) ON DELETE RESTRICT,
    CONSTRAINT uq_dtb_cart_items_cart_design UNIQUE (cart_id, gift_design_id),
    CONSTRAINT chk_dtb_cart_items_quantity   CHECK (quantity > 0),
    CONSTRAINT chk_dtb_cart_items_unit_price CHECK (unit_price > 0)
);

CREATE INDEX idx_dtb_cart_items_cart_id ON dtb_cart_items(cart_id);

CREATE TRIGGER trg_dtb_cart_items_updated_at
    BEFORE UPDATE ON dtb_cart_items
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- =====================================================================
-- 6. ORDERS
-- =====================================================================
CREATE TABLE dtb_orders (
    id                  UUID            NOT NULL DEFAULT gen_random_uuid(),
    order_code          VARCHAR(20)     NOT NULL,           -- LB261018-0001 (display code)
    user_id             UUID            NOT NULL,
    cart_id             UUID,
    status              VARCHAR(24)     NOT NULL DEFAULT 'pending_payment',
    subtotal_amount     NUMERIC(12, 0)  NOT NULL,
    total_amount        NUMERIC(12, 0)  NOT NULL,
    currency            CHAR(3)         NOT NULL DEFAULT 'VND',
    customer_note       TEXT,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted_at          TIMESTAMPTZ,

    CONSTRAINT pk_dtb_orders PRIMARY KEY (id),
    CONSTRAINT uq_dtb_orders_order_code UNIQUE (order_code),
    CONSTRAINT fk_dtb_orders_user
        FOREIGN KEY (user_id) REFERENCES dtb_users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_dtb_orders_cart
        FOREIGN KEY (cart_id) REFERENCES dtb_carts(id) ON DELETE SET NULL,
    CONSTRAINT chk_dtb_orders_amounts CHECK (subtotal_amount >= 0 AND total_amount >= 0),
    CONSTRAINT chk_dtb_orders_status  CHECK (status IN (
        'pending_payment',    -- vừa tạo, chờ khách chuyển khoản
        'payment_submitted',  -- khách đã upload bill
        'confirmed',          -- admin xác nhận nhận tiền
        'processing',         -- team đang làm hộp quà
        'shipped',
        'completed',
        'cancelled'
    ))
);

CREATE INDEX idx_dtb_orders_user_id    ON dtb_orders(user_id);
CREATE INDEX idx_dtb_orders_status     ON dtb_orders(status) WHERE deleted_at IS NULL;
CREATE INDEX idx_dtb_orders_created_at ON dtb_orders(created_at DESC);

CREATE TRIGGER trg_dtb_orders_updated_at
    BEFORE UPDATE ON dtb_orders
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


CREATE TABLE dtb_order_items (
    id                      UUID            NOT NULL DEFAULT gen_random_uuid(),
    order_id                UUID            NOT NULL,
    gift_design_id          UUID            NOT NULL,
    wish_id                 UUID,
    box_size_id             UUID            NOT NULL,
    product_name_snapshot   VARCHAR(255)    NOT NULL,       -- snapshot lúc đặt
    image_url_snapshot      TEXT            NOT NULL,
    unit_price              NUMERIC(12, 0)  NOT NULL,
    quantity                SMALLINT        NOT NULL DEFAULT 1,
    line_total              NUMERIC(12, 0)  NOT NULL,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_dtb_order_items PRIMARY KEY (id),
    CONSTRAINT fk_dtb_order_items_order
        FOREIGN KEY (order_id) REFERENCES dtb_orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_dtb_order_items_gift_design
        FOREIGN KEY (gift_design_id) REFERENCES dtb_gift_designs(id) ON DELETE RESTRICT,
    CONSTRAINT fk_dtb_order_items_wish
        FOREIGN KEY (wish_id) REFERENCES dtb_greeting_wishes(id) ON DELETE SET NULL,
    CONSTRAINT fk_dtb_order_items_box_size
        FOREIGN KEY (box_size_id) REFERENCES dtb_box_sizes(id) ON DELETE RESTRICT,
    CONSTRAINT chk_dtb_order_items_quantity   CHECK (quantity > 0),
    CONSTRAINT chk_dtb_order_items_line_total CHECK (line_total = unit_price * quantity)
);

CREATE INDEX idx_dtb_order_items_order_id ON dtb_order_items(order_id);


-- Thông tin giao hàng — 1 đơn : 1 địa chỉ giao
CREATE TABLE dtb_shipping_info (
    id                      UUID            NOT NULL DEFAULT gen_random_uuid(),
    order_id                UUID            NOT NULL,
    sender_name             VARCHAR(255)    NOT NULL,
    sender_phone            VARCHAR(20)     NOT NULL,
    recipient_name          VARCHAR(255)    NOT NULL,
    recipient_phone         VARCHAR(20)     NOT NULL,
    address_line            TEXT            NOT NULL,
    ward                    VARCHAR(100),
    district                VARCHAR(100),
    province                VARCHAR(100)    NOT NULL,
    requested_delivery_date DATE            NOT NULL,
    delivery_note           TEXT,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_dtb_shipping_info PRIMARY KEY (id),
    CONSTRAINT uq_dtb_shipping_info_order UNIQUE (order_id),
    CONSTRAINT fk_dtb_shipping_info_order
        FOREIGN KEY (order_id) REFERENCES dtb_orders(id) ON DELETE CASCADE
);

CREATE TRIGGER trg_dtb_shipping_info_updated_at
    BEFORE UPDATE ON dtb_shipping_info
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- =====================================================================
-- 7. PAYMENT  (VietQR chuyển khoản + upload bill thủ công)
-- =====================================================================
CREATE TABLE dtb_payment_transactions (
    id                  UUID            NOT NULL DEFAULT gen_random_uuid(),
    order_id            UUID            NOT NULL,
    method              VARCHAR(24)     NOT NULL DEFAULT 'BANK_TRANSFER_QR',
    bank_name           VARCHAR(100)    NOT NULL,
    bank_account_no     VARCHAR(50)     NOT NULL,
    bank_account_name   VARCHAR(255)    NOT NULL,
    amount              NUMERIC(12, 0)  NOT NULL,
    vietqr_content      TEXT            NOT NULL,           -- nội dung chuyển khoản / QR payload
    receipt_image_url   TEXT,
    receipt_uploaded_at TIMESTAMPTZ,
    status              VARCHAR(24)     NOT NULL DEFAULT 'awaiting_receipt',
    verified_by_user_id UUID,                               -- admin user xác nhận
    verified_at         TIMESTAMPTZ,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_dtb_payment_transactions PRIMARY KEY (id),
    CONSTRAINT fk_dtb_payment_transactions_order
        FOREIGN KEY (order_id) REFERENCES dtb_orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_dtb_payment_transactions_verifier
        FOREIGN KEY (verified_by_user_id) REFERENCES dtb_users(id) ON DELETE SET NULL,
    CONSTRAINT chk_dtb_payment_transactions_amount CHECK (amount > 0),
    CONSTRAINT chk_dtb_payment_transactions_status CHECK (status IN (
        'awaiting_receipt',
        'receipt_uploaded',
        'verified',
        'rejected'
    ))
);

CREATE INDEX idx_dtb_payment_transactions_order_id ON dtb_payment_transactions(order_id);
CREATE INDEX idx_dtb_payment_transactions_status   ON dtb_payment_transactions(status);

CREATE TRIGGER trg_dtb_payment_transactions_updated_at
    BEFORE UPDATE ON dtb_payment_transactions
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- =====================================================================
-- 8. AUDIT & NOTIFY
-- =====================================================================
CREATE TABLE dtb_order_status_history (
    id                  UUID            NOT NULL DEFAULT gen_random_uuid(),
    order_id            UUID            NOT NULL,
    from_status         VARCHAR(24),
    to_status           VARCHAR(24)     NOT NULL,
    changed_by          VARCHAR(16)     NOT NULL DEFAULT 'SYSTEM',  -- SYSTEM | ADMIN | CUSTOMER
    changed_by_user_id  UUID,
    note                TEXT,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_dtb_order_status_history PRIMARY KEY (id),
    CONSTRAINT fk_dtb_order_status_history_order
        FOREIGN KEY (order_id) REFERENCES dtb_orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_dtb_order_status_history_user
        FOREIGN KEY (changed_by_user_id) REFERENCES dtb_users(id) ON DELETE SET NULL,
    CONSTRAINT chk_dtb_order_status_history_changed_by
        CHECK (changed_by IN ('SYSTEM', 'ADMIN', 'CUSTOMER'))
);

CREATE INDEX idx_dtb_order_status_history_order_id ON dtb_order_status_history(order_id);


-- Bắn thông báo đơn mới về Telegram / Google Sheet
-- (align dtb_admin_notifications của IGOGO)
CREATE TABLE dtb_admin_notifications (
    id              UUID            NOT NULL DEFAULT gen_random_uuid(),
    order_id        UUID            NOT NULL,
    channel         VARCHAR(16)     NOT NULL,               -- 'TELEGRAM' | 'GOOGLE_SHEET' | 'EMAIL'
    status          VARCHAR(16)     NOT NULL DEFAULT 'pending',
    payload         TEXT            NOT NULL,               -- JSON snapshot đơn hàng
    response_message TEXT,
    retry_count     SMALLINT        NOT NULL DEFAULT 0,
    sent_at         TIMESTAMPTZ,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_dtb_admin_notifications PRIMARY KEY (id),
    CONSTRAINT fk_dtb_admin_notifications_order
        FOREIGN KEY (order_id) REFERENCES dtb_orders(id) ON DELETE CASCADE,
    CONSTRAINT chk_dtb_admin_notifications_channel
        CHECK (channel IN ('TELEGRAM', 'GOOGLE_SHEET', 'EMAIL')),
    CONSTRAINT chk_dtb_admin_notifications_status
        CHECK (status IN ('pending', 'sent', 'failed'))
);

CREATE INDEX idx_dtb_admin_notifications_order_id ON dtb_admin_notifications(order_id);
CREATE INDEX idx_dtb_admin_notifications_status   ON dtb_admin_notifications(status);

CREATE TRIGGER trg_dtb_admin_notifications_updated_at
    BEFORE UPDATE ON dtb_admin_notifications
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
