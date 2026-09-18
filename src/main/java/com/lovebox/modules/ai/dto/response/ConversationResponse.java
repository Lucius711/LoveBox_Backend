package com.lovebox.modules.ai.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter @Builder
public class ConversationResponse {
    private UUID id;
    private String status;
    private List<MessageDto> messages;
    private Instant createdAt;

    @Getter @Builder
    public static class MessageDto {
        private UUID id;
        private String role;
        private String content;
        private Short fromCache;
        private Instant createdAt;
    }
}
