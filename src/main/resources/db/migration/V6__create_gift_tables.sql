-- =====================================================================
-- Love Box — Flyway Migration V6 (Gift tables)
-- =====================================================================

-- 1. Gift Designs
CREATE TABLE dtb_gift_designs (
    id            UUID         NOT NULL DEFAULT gen_random_uuid(),
    user_id       UUID         NOT NULL,
    source_type   VARCHAR(32)  NOT NULL,
    image_url     VARCHAR(512) NOT NULL,
    generation_id UUID,
    showcase_id   UUID,
    title         VARCHAR(255),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_dtb_gift_designs PRIMARY KEY (id),
    CONSTRAINT fk_gift_design_user FOREIGN KEY (user_id)
        REFERENCES dtb_users(id) ON DELETE CASCADE,
    CONSTRAINT fk_gift_design_generation FOREIGN KEY (generation_id)
        REFERENCES dtb_ai_image_generations(id) ON DELETE SET NULL,
    CONSTRAINT fk_gift_design_showcase FOREIGN KEY (showcase_id)
        REFERENCES dtb_showcase_designs(id) ON DELETE SET NULL,
    CONSTRAINT chk_gift_design_source_type CHECK (source_type IN ('AI_GENERATED', 'SHOWCASE')),
    -- XOR: exactly one of generation_id / showcase_id must be set
    CONSTRAINT chk_gift_design_source_xor CHECK (
        (source_type = 'AI_GENERATED' AND generation_id IS NOT NULL AND showcase_id IS NULL)
        OR
        (source_type = 'SHOWCASE' AND showcase_id IS NOT NULL AND generation_id IS NULL)
    )
);

CREATE INDEX idx_gift_designs_user_id ON dtb_gift_designs(user_id);
CREATE INDEX idx_gift_designs_source  ON dtb_gift_designs(source_type);
CREATE INDEX idx_gift_designs_created ON dtb_gift_designs(created_at DESC);

CREATE TRIGGER trg_gift_designs_updated_at
    BEFORE UPDATE ON dtb_gift_designs
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- 2. Greeting Wishes
CREATE TABLE dtb_greeting_wishes (
    id         UUID         NOT NULL DEFAULT gen_random_uuid(),
    user_id    UUID         NOT NULL,
    message    TEXT         NOT NULL,
    qr_token   UUID         NOT NULL DEFAULT gen_random_uuid(),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_dtb_greeting_wishes PRIMARY KEY (id),
    CONSTRAINT uq_greeting_wishes_qr_token UNIQUE (qr_token),
    CONSTRAINT fk_greeting_wish_user FOREIGN KEY (user_id)
        REFERENCES dtb_users(id) ON DELETE CASCADE
);

CREATE INDEX idx_greeting_wishes_user_id  ON dtb_greeting_wishes(user_id);
CREATE INDEX idx_greeting_wishes_qr_token ON dtb_greeting_wishes(qr_token);

CREATE TRIGGER trg_greeting_wishes_updated_at
    BEFORE UPDATE ON dtb_greeting_wishes
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
