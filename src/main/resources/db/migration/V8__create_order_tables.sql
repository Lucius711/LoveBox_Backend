-- =====================================================================
-- Love Box — Flyway Migration V8 (Order tables)
-- =====================================================================

-- 1. Orders
CREATE TABLE dtb_orders (
    id           UUID         NOT NULL DEFAULT gen_random_uuid(),
    user_id      UUID         NOT NULL,
    order_code   VARCHAR(32)  NOT NULL,
    status       VARCHAR(32)  NOT NULL DEFAULT 'PENDING_PAYMENT',
    total_amount BIGINT       NOT NULL,
    note         TEXT,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_dtb_orders PRIMARY KEY (id),
    CONSTRAINT uq_orders_order_code UNIQUE (order_code),
    CONSTRAINT fk_order_user FOREIGN KEY (user_id)
        REFERENCES dtb_users(id) ON DELETE RESTRICT,
    CONSTRAINT chk_orders_status CHECK (
        status IN ('PENDING_PAYMENT','PAID','PROCESSING','SHIPPED','DELIVERED','CANCELLED')
    ),
    CONSTRAINT chk_orders_total_amount CHECK (total_amount >= 0)
);

CREATE INDEX idx_orders_user_id    ON dtb_orders(user_id);
CREATE INDEX idx_orders_status     ON dtb_orders(status);
CREATE INDEX idx_orders_order_code ON dtb_orders(order_code);
CREATE INDEX idx_orders_created    ON dtb_orders(created_at DESC);

CREATE TRIGGER trg_orders_updated_at
    BEFORE UPDATE ON dtb_orders
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- 2. Order Items
CREATE TABLE dtb_order_items (
    id                UUID         NOT NULL DEFAULT gen_random_uuid(),
    order_id          UUID         NOT NULL,
    box_size_code     VARCHAR(32)  NOT NULL,
    box_size_name     VARCHAR(128) NOT NULL,
    unit_price        BIGINT       NOT NULL,
    quantity          INT          NOT NULL DEFAULT 1,
    gift_design_id    UUID,
    greeting_wish_id  UUID,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_dtb_order_items PRIMARY KEY (id),
    CONSTRAINT fk_order_item_order FOREIGN KEY (order_id)
        REFERENCES dtb_orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_order_item_gift_design FOREIGN KEY (gift_design_id)
        REFERENCES dtb_gift_designs(id) ON DELETE SET NULL,
    CONSTRAINT fk_order_item_greeting_wish FOREIGN KEY (greeting_wish_id)
        REFERENCES dtb_greeting_wishes(id) ON DELETE SET NULL,
    CONSTRAINT chk_order_items_quantity   CHECK (quantity > 0),
    CONSTRAINT chk_order_items_unit_price CHECK (unit_price >= 0)
);

CREATE INDEX idx_order_items_order_id ON dtb_order_items(order_id);

CREATE TRIGGER trg_order_items_updated_at
    BEFORE UPDATE ON dtb_order_items
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- 3. Shipping Info
CREATE TABLE dtb_shipping_info (
    id             UUID         NOT NULL DEFAULT gen_random_uuid(),
    order_id       UUID         NOT NULL,
    recipient_name VARCHAR(255) NOT NULL,
    phone          VARCHAR(20)  NOT NULL,
    address        TEXT         NOT NULL,
    province       VARCHAR(128),
    district       VARCHAR(128),
    ward           VARCHAR(128),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_dtb_shipping_info PRIMARY KEY (id),
    CONSTRAINT uq_shipping_info_order_id UNIQUE (order_id),
    CONSTRAINT fk_shipping_info_order FOREIGN KEY (order_id)
        REFERENCES dtb_orders(id) ON DELETE CASCADE
);

CREATE TRIGGER trg_shipping_info_updated_at
    BEFORE UPDATE ON dtb_shipping_info
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- 4. Order Status History
CREATE TABLE dtb_order_status_history (
    id         UUID        NOT NULL DEFAULT gen_random_uuid(),
    order_id   UUID        NOT NULL,
    status     VARCHAR(32) NOT NULL,
    note       TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT pk_dtb_order_status_history PRIMARY KEY (id),
    CONSTRAINT fk_order_status_history_order FOREIGN KEY (order_id)
        REFERENCES dtb_orders(id) ON DELETE CASCADE,
    CONSTRAINT chk_order_status_history_status CHECK (
        status IN ('PENDING_PAYMENT','PAID','PROCESSING','SHIPPED','DELIVERED','CANCELLED')
    )
);

CREATE INDEX idx_order_status_history_order_id ON dtb_order_status_history(order_id);
CREATE INDEX idx_order_status_history_created  ON dtb_order_status_history(created_at ASC);

CREATE TRIGGER trg_order_status_history_updated_at
    BEFORE UPDATE ON dtb_order_status_history
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
