package com.lovebox.modules.aichat.service;

import com.lovebox.modules.aichat.dto.request.UpdatePreferencesRequest;
import com.lovebox.modules.aichat.dto.response.ChatConversationResponse;
import com.lovebox.modules.aichat.dto.response.ChatPreferencesResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;

public interface ChatService {
    SseEmitter streamChat(UUID userId, UUID conversationId, String message);
    List<ChatConversationResponse> listConversations(UUID userId);
    ChatConversationResponse getConversation(UUID conversationId, UUID userId);
    void deleteConversation(UUID conversationId, UUID userId);
    ChatPreferencesResponse getPreferences(UUID userId);
    ChatPreferencesResponse updatePreferences(UUID userId, UpdatePreferencesRequest request);
}
