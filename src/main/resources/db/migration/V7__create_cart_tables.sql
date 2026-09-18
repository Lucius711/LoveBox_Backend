-- =====================================================================
-- Love Box — Flyway Migration V7 (Cart tables)
-- =====================================================================

-- 1. Carts
CREATE TABLE dtb_carts (
    id         UUID        NOT NULL DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT pk_dtb_carts PRIMARY KEY (id),
    CONSTRAINT uq_carts_user_id UNIQUE (user_id),
    CONSTRAINT fk_cart_user FOREIGN KEY (user_id)
        REFERENCES dtb_users(id) ON DELETE CASCADE
);

CREATE INDEX idx_carts_user_id ON dtb_carts(user_id);

CREATE TRIGGER trg_carts_updated_at
    BEFORE UPDATE ON dtb_carts
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- 2. Cart Items
CREATE TABLE dtb_cart_items (
    id              UUID         NOT NULL DEFAULT gen_random_uuid(),
    cart_id         UUID         NOT NULL,
    box_size_id     UUID         NOT NULL,
    gift_design_id  UUID,
    greeting_wish_id UUID,
    quantity        INT          NOT NULL DEFAULT 1,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_dtb_cart_items PRIMARY KEY (id),
    CONSTRAINT fk_cart_item_cart FOREIGN KEY (cart_id)
        REFERENCES dtb_carts(id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_item_box_size FOREIGN KEY (box_size_id)
        REFERENCES dtb_box_sizes(id),
    CONSTRAINT fk_cart_item_gift_design FOREIGN KEY (gift_design_id)
        REFERENCES dtb_gift_designs(id) ON DELETE SET NULL,
    CONSTRAINT fk_cart_item_greeting_wish FOREIGN KEY (greeting_wish_id)
        REFERENCES dtb_greeting_wishes(id) ON DELETE SET NULL,
    CONSTRAINT chk_cart_item_quantity CHECK (quantity > 0)
);

CREATE INDEX idx_cart_items_cart_id ON dtb_cart_items(cart_id);

CREATE TRIGGER trg_cart_items_updated_at
    BEFORE UPDATE ON dtb_cart_items
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
