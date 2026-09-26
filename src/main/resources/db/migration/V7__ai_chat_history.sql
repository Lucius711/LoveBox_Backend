-- Lịch sử chat với trợ lý AI: mỗi dòng là một cuộc chat, tin nhắn lưu dạng JSON (vài lượt/cuộc)
CREATE TABLE dtb_chat_sessions (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID         NOT NULL REFERENCES dtb_users(id) ON DELETE CASCADE,
    title      VARCHAR(120) NOT NULL,
    prompt     TEXT         NOT NULL DEFAULT '',
    messages   JSONB        NOT NULL DEFAULT '[]',
    archived   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_chat_sessions_user ON dtb_chat_sessions(user_id, archived, updated_at DESC);
