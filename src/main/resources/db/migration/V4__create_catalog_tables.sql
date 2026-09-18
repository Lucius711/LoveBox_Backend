-- =====================================================================
-- V4 — CATALOG: dtb_product_categories, dtb_box_sizes, dtb_showcase_designs
-- =====================================================================

-- 4.1 Danh mục hộp quà
CREATE TABLE dtb_product_categories (
    id              UUID            NOT NULL DEFAULT gen_random_uuid(),
    name            VARCHAR(100)    NOT NULL,
    slug            VARCHAR(100)    NOT NULL,
    description     TEXT,
    display_order   SMALLINT        NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_dtb_product_categories      PRIMARY KEY (id),
    CONSTRAINT uq_dtb_product_categories_slug UNIQUE (slug)
);

CREATE TRIGGER trg_dtb_product_categories_updated_at
    BEFORE UPDATE ON dtb_product_categories
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- 4.2 Size hộp (Standard 250k, Deluxe 350k…)
CREATE TABLE dtb_box_sizes (
    id              UUID            NOT NULL DEFAULT gen_random_uuid(),
    code            VARCHAR(32)     NOT NULL,
    name            VARCHAR(100)    NOT NULL,
    price           NUMERIC(12, 0)  NOT NULL,
    description     TEXT,
    is_active       SMALLINT        NOT NULL DEFAULT 1,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_dtb_box_sizes         PRIMARY KEY (id),
    CONSTRAINT uq_dtb_box_sizes_code    UNIQUE (code),
    CONSTRAINT chk_dtb_box_sizes_price  CHECK (price > 0),
    CONSTRAINT chk_dtb_box_sizes_active CHECK (is_active IN (0, 1))
);

CREATE TRIGGER trg_dtb_box_sizes_updated_at
    BEFORE UPDATE ON dtb_box_sizes
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- 4.3 Mẫu dựng sẵn trên landing page
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
    CONSTRAINT chk_dtb_showcase_designs_featured
        CHECK (is_featured IN (0, 1))
);

CREATE INDEX idx_dtb_showcase_designs_category_id
    ON dtb_showcase_designs(category_id);
CREATE INDEX idx_dtb_showcase_designs_status
    ON dtb_showcase_designs(status) WHERE deleted_at IS NULL;

CREATE TRIGGER trg_dtb_showcase_designs_updated_at
    BEFORE UPDATE ON dtb_showcase_designs
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
