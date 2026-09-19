package com.lovebox.modules.ai.serviceimpl;

import com.lovebox.common.constant.ErrorCode;
import com.lovebox.common.exception.AppException;
import com.lovebox.modules.ai.config.AiProperties;
import com.lovebox.modules.ai.dto.request.GenerateImageRequest;
import com.lovebox.modules.ai.dto.request.SendMessageRequest;
import com.lovebox.modules.ai.dto.response.ConversationResponse;
import com.lovebox.modules.ai.dto.response.ImageGenerationResponse;
import com.lovebox.modules.ai.entity.*;
import com.lovebox.modules.ai.repository.*;
import com.lovebox.modules.ai.service.AiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final AiConversationRepository conversationRepository;
    private final AiMessageRepository      messageRepository;
    private final AiImageGenerationRepository imageGenerationRepository;
    private final AiGeneratedImageRepository  generatedImageRepository;
    private final AiProperties aiProperties;

    @Qualifier("geminiChatClient")  private final RestClient geminiChatClient;
    @Qualifier("geminiImageClient") private final RestClient geminiImageClient;

    // ------------------------------------------------------------------ //
    //  Conversation
    // ------------------------------------------------------------------ //

    @Override
    @Transactional
    public ConversationResponse startConversation(UUID userId) {
        AiConversation conv = conversationRepository.save(AiConversation.builder()
                .userId(userId).status("ACTIVE")
                .totalPromptTokens(0L).totalCompletionTokens(0L)
                .build());
        return mapToResponse(conv, List.of());
    }

    @Override
    @Transactional
    public ConversationResponse sendMessage(UUID conversationId, UUID userId, SendMessageRequest request) {
        AiConversation conv = getAndValidate(conversationId, userId);

        messageRepository.save(AiMessage.builder()
                .conversationId(conversationId).role("user")
                .content(request.getMessage()).fromCache((short) 0)
                .build());

        List<AiMessage> history = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);

        // Build messages payload (OpenAI-compatible format)
        List<Map<String, String>> messages = history.stream()
                .map(m -> Map.of("role", m.getRole(), "content", m.getContent()))
                .toList();

        long t0 = System.currentTimeMillis();
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = geminiChatClient.post()
                    .uri("/chat/completions")
                    .body(Map.of("model", aiProperties.getChatModel(), "messages", messages))
                    .retrieve()
                    .body(Map.class);

            @SuppressWarnings("unchecked")
            Map<String, Object> usage = (Map<String, Object>) result.get("usage");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) result.get("choices");
            @SuppressWarnings("unchecked")
            String content = (String) ((Map<String, Object>) choices.get(0).get("message")).get("content");

            int promptTokens     = ((Number) usage.get("prompt_tokens")).intValue();
            int completionTokens = ((Number) usage.get("completion_tokens")).intValue();

            messageRepository.save(AiMessage.builder()
                    .conversationId(conversationId).role("assistant")
                    .content(content)
                    .promptTokens(promptTokens).completionTokens(completionTokens)
                    .processingTimeMs((int)(System.currentTimeMillis() - t0))
                    .fromCache((short) 0).model(aiProperties.getChatModel())
                    .build());

            conv.setTotalPromptTokens(conv.getTotalPromptTokens() + promptTokens);
            conv.setTotalCompletionTokens(conv.getTotalCompletionTokens() + completionTokens);
            conversationRepository.save(conv);

        } catch (Exception e) {
            log.error("[Gemini] chat completion failed", e);
            throw new AppException(ErrorCode.AI_SERVICE_UNAVAILABLE);
        }

        return mapToResponse(conv,
                messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId));
    }

    @Override
    public ConversationResponse getConversation(UUID conversationId, UUID userId) {
        AiConversation conv = getAndValidate(conversationId, userId);
        return mapToResponse(conv,
                messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId));
    }

    @Override
    public List<ConversationResponse> listConversations(UUID userId) {
        return conversationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(c -> mapToResponse(c, List.of())).toList();
    }

    // ------------------------------------------------------------------ //
    //  Image generation — Imagen 3 (native Gemini API)
    // ------------------------------------------------------------------ //

    @Override
    @Transactional
    @SuppressWarnings("unchecked")
    public ImageGenerationResponse generateImage(UUID userId, GenerateImageRequest request) {
        UUID conversationId = request.getConversationId() != null
                ? UUID.fromString(request.getConversationId()) : null;

        AiImageGeneration generation = imageGenerationRepository.save(
                AiImageGeneration.builder()
                        .conversationId(conversationId).userId(userId)
                        .prompt(request.getPrompt())
                        .provider("gemini").model(aiProperties.getImageModel())
                        .size("1024x1024").quality("standard")
                        .status("PROCESSING")
                        .build());

        try {
            String uri = String.format("/v1/models/%s:predict?key=%s",
                    aiProperties.getImageModel(), aiProperties.getGeminiApiKey());

            Map<String, Object> result = geminiImageClient.post()
                    .uri(uri)
                    .body(Map.of(
                            "instances",  List.of(Map.of("prompt", request.getPrompt())),
                            "parameters", Map.of("sampleCount", 1)
                    ))
                    .retrieve()
                    .body(Map.class);

            // Imagen trả về base64 — lưu dạng data URI
            List<Map<String, Object>> predictions = (List<Map<String, Object>>) result.get("predictions");
            String b64 = (String) predictions.get(0).get("bytesBase64Encoded");
            String mimeType = (String) ((Map<String, Object>) predictions.get(0)).getOrDefault("mimeType", "image/png");
            String imageUrl = "data:" + mimeType + ";base64," + b64;

            generation.setStatus("COMPLETED");
            imageGenerationRepository.save(generation);

            AiGeneratedImage saved = generatedImageRepository.save(
                    AiGeneratedImage.builder()
                            .generationId(generation.getId())
                            .imageUrl(imageUrl)
                            .build());

            return ImageGenerationResponse.builder()
                    .generationId(generation.getId())
                    .imageId(saved.getId())
                    .imageUrl(imageUrl)
                    .status("COMPLETED")
                    .build();

        } catch (Exception e) {
            log.error("[Imagen3] image generation failed", e);
            generation.setStatus("FAILED");
            generation.setErrorMessage(e.getMessage());
            imageGenerationRepository.save(generation);
            throw new AppException(ErrorCode.AI_IMAGE_GENERATION_FAILED);
        }
    }

    // ------------------------------------------------------------------ //
    //  Helpers
    // ------------------------------------------------------------------ //

    private AiConversation getAndValidate(UUID conversationId, UUID userId) {
        AiConversation c = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new AppException(ErrorCode.AI_CONVERSATION_NOT_FOUND));
        if (!c.getUserId().equals(userId)) throw new AppException(ErrorCode.AI_CONVERSATION_FORBIDDEN);
        return c;
    }

    private ConversationResponse mapToResponse(AiConversation c, List<AiMessage> messages) {
        return ConversationResponse.builder()
                .id(c.getId()).status(c.getStatus()).createdAt(c.getCreatedAt())
                .messages(messages.stream().map(m -> ConversationResponse.MessageDto.builder()
                        .id(m.getId()).role(m.getRole()).content(m.getContent())
                        .fromCache(m.getFromCache()).createdAt(m.getCreatedAt()).build()).toList())
                .build();
    }
}
