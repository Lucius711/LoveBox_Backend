-- V13: Add columns missing from V12's original run (ai_messages, ai_image_generations, ai_generated_images)

-- dtb_ai_messages
ALTER TABLE dtb_ai_messages
    ADD COLUMN IF NOT EXISTS prompt_tokens       INTEGER,
    ADD COLUMN IF NOT EXISTS completion_tokens   INTEGER,
    ADD COLUMN IF NOT EXISTS processing_time_ms  INTEGER,
    ADD COLUMN IF NOT EXISTS from_cache          SMALLINT    NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS model               VARCHAR(100);

-- dtb_ai_image_generations
ALTER TABLE dtb_ai_image_generations
    ADD COLUMN IF NOT EXISTS provider      VARCHAR(50) NOT NULL DEFAULT 'GEMINI',
    ADD COLUMN IF NOT EXISTS size          VARCHAR(20),
    ADD COLUMN IF NOT EXISTS quality       VARCHAR(20),
    ADD COLUMN IF NOT EXISTS prompt_tokens INTEGER;

ALTER TABLE dtb_ai_image_generations
    ALTER COLUMN model TYPE VARCHAR(100);

-- dtb_ai_generated_images
ALTER TABLE dtb_ai_generated_images
    ADD COLUMN IF NOT EXISTS storage_path VARCHAR(500);
