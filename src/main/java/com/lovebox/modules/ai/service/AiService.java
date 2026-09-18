package com.lovebox.modules.ai.service;

import com.lovebox.modules.ai.dto.request.GenerateImageRequest;
import com.lovebox.modules.ai.dto.request.SendMessageRequest;
import com.lovebox.modules.ai.dto.response.ConversationResponse;
import com.lovebox.modules.ai.dto.response.ImageGenerationResponse;

import java.util.List;
import java.util.UUID;

public interface AiService {
    ConversationResponse startConversation(UUID userId);
    ConversationResponse sendMessage(UUID conversationId, UUID userId, SendMessageRequest request);
    ConversationResponse getConversation(UUID conversationId, UUID userId);
    List<ConversationResponse> listConversations(UUID userId);
    ImageGenerationResponse generateImage(UUID userId, GenerateImageRequest request);
}
