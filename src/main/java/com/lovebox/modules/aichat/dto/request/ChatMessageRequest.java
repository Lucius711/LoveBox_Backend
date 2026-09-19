package com.lovebox.modules.aichat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter @Setter
public class ChatMessageRequest {
    private UUID conversationId;     // null = start new conversation
    @NotBlank @Size(max = 2000)
    private String message;
}
