package com.lovebox.modules.aichat.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter @Builder
public class ChatMessageResponse {
    private UUID id;
    private String role;
    private String content;
    private Object card;          // parsed JSON object
    private Object quickReplies;  // parsed JSON array
    private Object actions;       // parsed JSON array
    private Boolean cached;
    private Instant createdAt;
}
