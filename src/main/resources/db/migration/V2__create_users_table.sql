-- =====================================================================
-- V2 — AUTH: dtb_users
-- =====================================================================
CREATE TABLE dtb_users (
    id              UUID            NOT NULL DEFAULT gen_random_uuid(),
    google_sub      VARCHAR(64)     NOT NULL,
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

    CONSTRAINT pk_dtb_users         PRIMARY KEY (id),
    CONSTRAINT uq_dtb_users_google  UNIQUE (google_sub),
    CONSTRAINT uq_dtb_users_email   UNIQUE (email),
    CONSTRAINT chk_dtb_users_role   CHECK (role   IN ('CUSTOMER', 'ADMIN')),
    CONSTRAINT chk_dtb_users_status CHECK (status IN ('ACTIVE', 'DISABLED'))
);

CREATE INDEX idx_dtb_users_status     ON dtb_users(status) WHERE deleted_at IS NULL;
CREATE INDEX idx_dtb_users_created_at ON dtb_users(created_at DESC);

CREATE TRIGGER trg_dtb_users_updated_at
    BEFORE UPDATE ON dtb_users
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
