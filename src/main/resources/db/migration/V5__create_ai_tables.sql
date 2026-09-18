-- =====================================================================
-- Love Box — Flyway Migration V5 (AI tables)
-- =====================================================================

-- 1. AI Conversations
CREATE TABLE dtb_ai_conversations (
    id                    UUID         NOT NULL DEFAULT gen_random_uuid(),
    user_id               UUID         NOT NULL,
    title                 VARCHAR(255),
    summary               TEXT,
    summarized_through_at TIMESTAMPTZ,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_dtb_ai_conversations PRIMARY KEY (id),
    CONSTRAINT fk_ai_conv_user FOREIGN KEY (user_id)
        REFERENCES dtb_users(id) ON DELETE CASCADE
);

CREATE INDEX idx_ai_conversations_user_id ON dtb_ai_conversations(user_id);
CREATE INDEX idx_ai_conversations_created  ON dtb_ai_conversations(created_at DESC);

CREATE TRIGGER trg_ai_conversations_updated_at
    BEFORE UPDATE ON dtb_ai_conversations
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- 2. AI Messages
CREATE TABLE dtb_ai_messages (
    id              UUID        NOT NULL DEFAULT gen_random_uuid(),
    conversation_id UUID        NOT NULL,
    role            VARCHAR(32) NOT NULL,
    content         TEXT        NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT pk_dtb_ai_messages PRIMARY KEY (id),
    CONSTRAINT fk_ai_msg_conv FOREIGN KEY (conversation_id)
        REFERENCES dtb_ai_conversations(id) ON DELETE CASCADE,
    CONSTRAINT chk_ai_msg_role CHECK (role IN ('user', 'assistant', 'system'))
);

CREATE INDEX idx_ai_messages_conversation_id ON dtb_ai_messages(conversation_id);
CREATE INDEX idx_ai_messages_created         ON dtb_ai_messages(created_at ASC);

CREATE TRIGGER trg_ai_messages_updated_at
    BEFORE UPDATE ON dtb_ai_messages
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- 3. AI Image Generations (request log)
CREATE TABLE dtb_ai_image_generations (
    id              UUID         NOT NULL DEFAULT gen_random_uuid(),
    conversation_id UUID,
    user_id         UUID         NOT NULL,
    prompt          TEXT         NOT NULL,
    revised_prompt  TEXT,
    model           VARCHAR(64)  NOT NULL DEFAULT 'dall-e-3',
    status          VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
    error_message   TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_dtb_ai_image_generations PRIMARY KEY (id),
    CONSTRAINT fk_ai_imggen_user FOREIGN KEY (user_id)
        REFERENCES dtb_users(id) ON DELETE CASCADE,
    CONSTRAINT fk_ai_imggen_conv FOREIGN KEY (conversation_id)
        REFERENCES dtb_ai_conversations(id) ON DELETE SET NULL,
    CONSTRAINT chk_ai_imggen_status CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED'))
);

CREATE INDEX idx_ai_image_generations_user_id ON dtb_ai_image_generations(user_id);
CREATE INDEX idx_ai_image_generations_status  ON dtb_ai_image_generations(status);
CREATE INDEX idx_ai_image_generations_created ON dtb_ai_image_generations(created_at DESC);

CREATE TRIGGER trg_ai_image_generations_updated_at
    BEFORE UPDATE ON dtb_ai_image_generations
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- 4. AI Generated Images (result store)
CREATE TABLE dtb_ai_generated_images (
    id            UUID         NOT NULL DEFAULT gen_random_uuid(),
    generation_id UUID         NOT NULL,
    image_url     VARCHAR(512) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_dtb_ai_generated_images PRIMARY KEY (id),
    CONSTRAINT fk_ai_genimg_generation FOREIGN KEY (generation_id)
        REFERENCES dtb_ai_image_generations(id) ON DELETE CASCADE
);

CREATE INDEX idx_ai_generated_images_generation_id ON dtb_ai_generated_images(generation_id);

CREATE TRIGGER trg_ai_generated_images_updated_at
    BEFORE UPDATE ON dtb_ai_generated_images
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
