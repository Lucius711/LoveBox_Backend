-- =====================================================================
-- V12: Sync AI tables with entity definitions
-- =====================================================================

-- dtb_ai_conversations: add status, total tokens
ALTER TABLE dtb_ai_conversations
    ADD COLUMN IF NOT EXISTS status                   VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN IF NOT EXISTS total_prompt_tokens      BIGINT      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS total_completion_tokens  BIGINT      NOT NULL DEFAULT 0;

-- dtb_ai_messages: add token/timing/cache/model columns
ALTER TABLE dtb_ai_messages
    ADD COLUMN IF NOT EXISTS prompt_tokens       INTEGER,
    ADD COLUMN IF NOT EXISTS completion_tokens   INTEGER,
    ADD COLUMN IF NOT EXISTS processing_time_ms  INTEGER,
    ADD COLUMN IF NOT EXISTS from_cache          SMALLINT    NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS model               VARCHAR(100);

-- dtb_ai_image_generations: add provider, size, quality, prompt_tokens
ALTER TABLE dtb_ai_image_generations
    ADD COLUMN IF NOT EXISTS provider      VARCHAR(50) NOT NULL DEFAULT 'GEMINI',
    ADD COLUMN IF NOT EXISTS size          VARCHAR(20),
    ADD COLUMN IF NOT EXISTS quality       VARCHAR(20),
    ADD COLUMN IF NOT EXISTS prompt_tokens INTEGER;

-- also widen model column (was VARCHAR(64), entity expects VARCHAR(100))
ALTER TABLE dtb_ai_image_generations
    ALTER COLUMN model TYPE VARCHAR(100);

-- dtb_ai_generated_images: add storage_path
ALTER TABLE dtb_ai_generated_images
    ADD COLUMN IF NOT EXISTS storage_path VARCHAR(500);
