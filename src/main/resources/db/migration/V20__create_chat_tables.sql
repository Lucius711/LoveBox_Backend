-- ─────────────────────────────────────────────────────────────────
--  V20 – AI Chat Assistant tables (separate from AI Studio)
-- ─────────────────────────────────────────────────────────────────

CREATE TABLE dtb_chat_conversations (
    id         UUID        NOT NULL DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL,
    title      VARCHAR(255),
    status     VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_chat_conversations PRIMARY KEY (id),
    CONSTRAINT fk_chat_conv_user FOREIGN KEY (user_id)
        REFERENCES dtb_users(id) ON DELETE CASCADE
);

CREATE INDEX idx_chat_conv_user_id ON dtb_chat_conversations(user_id);
CREATE INDEX idx_chat_conv_created ON dtb_chat_conversations(created_at DESC);

CREATE TABLE dtb_chat_messages (
    id              UUID        NOT NULL DEFAULT gen_random_uuid(),
    conversation_id UUID        NOT NULL,
    role            VARCHAR(16) NOT NULL,
    content         TEXT        NOT NULL,
    card_json       JSONB,
    quick_replies   JSONB,
    actions_json    JSONB,
    cached          BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_chat_messages PRIMARY KEY (id),
    CONSTRAINT fk_chat_msg_conv FOREIGN KEY (conversation_id)
        REFERENCES dtb_chat_conversations(id) ON DELETE CASCADE
);

CREATE INDEX idx_chat_msg_conv_id ON dtb_chat_messages(conversation_id, created_at ASC);

CREATE TABLE dtb_chat_preferences (
    id                 UUID         NOT NULL DEFAULT gen_random_uuid(),
    user_id            UUID         NOT NULL,
    preferred_language VARCHAR(8)   NOT NULL DEFAULT 'vi',
    style_preferences  JSONB,
    occasion_interests JSONB,
    budget_range       VARCHAR(50),
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_chat_preferences PRIMARY KEY (id),
    CONSTRAINT uq_chat_pref_user UNIQUE (user_id),
    CONSTRAINT fk_chat_pref_user FOREIGN KEY (user_id)
        REFERENCES dtb_users(id) ON DELETE CASCADE
);
