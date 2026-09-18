-- =====================================================================
-- V3 — AUTH: dtb_auth_sessions (refresh token store)
-- =====================================================================
CREATE TABLE dtb_auth_sessions (
    id                  UUID            NOT NULL DEFAULT gen_random_uuid(),
    user_id             UUID            NOT NULL,
    refresh_token_hash  VARCHAR(255)    NOT NULL,
    user_agent          TEXT,
    ip_address          VARCHAR(45),
    expires_at          TIMESTAMPTZ     NOT NULL,
    revoked_at          TIMESTAMPTZ,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_dtb_auth_sessions       PRIMARY KEY (id),
    CONSTRAINT uq_dtb_auth_sessions_token UNIQUE (refresh_token_hash),
    CONSTRAINT fk_dtb_auth_sessions_user
        FOREIGN KEY (user_id) REFERENCES dtb_users(id) ON DELETE CASCADE
);

CREATE INDEX idx_dtb_auth_sessions_user_id    ON dtb_auth_sessions(user_id);
CREATE INDEX idx_dtb_auth_sessions_expires_at ON dtb_auth_sessions(expires_at);
