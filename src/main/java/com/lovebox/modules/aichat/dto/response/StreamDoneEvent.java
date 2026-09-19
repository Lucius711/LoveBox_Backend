package com.lovebox.modules.aichat.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter @Builder
public class StreamDoneEvent {
    private String type;           // "done"
    private UUID messageId;
    private UUID conversationId;
    private String conversationTitle;
    private Object card;
    private Object quickReplies;
    private Object actions;
}
