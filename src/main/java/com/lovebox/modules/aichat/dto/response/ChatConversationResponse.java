package com.lovebox.modules.aichat.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter @Builder
public class ChatConversationResponse {
    private UUID id;
    private String title;
    private String status;
    private List<ChatMessageResponse> messages;
    private Instant createdAt;
    private Instant updatedAt;
}
