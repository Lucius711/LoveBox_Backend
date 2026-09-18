package com.lovebox.modules.ai.controller;

import com.lovebox.common.response.ApiResponse;
import com.lovebox.modules.ai.dto.request.GenerateImageRequest;
import com.lovebox.modules.ai.dto.request.SendMessageRequest;
import com.lovebox.modules.ai.dto.response.ConversationResponse;
import com.lovebox.modules.ai.dto.response.ImageGenerationResponse;
import com.lovebox.modules.ai.service.AiService;
import com.lovebox.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @PostMapping("/conversations")
    public ResponseEntity<ApiResponse<ConversationResponse>> startConversation(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(aiService.startConversation(principal.getId())));
    }

    @PostMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<ApiResponse<ConversationResponse>> sendMessage(
            @PathVariable UUID conversationId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SendMessageRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                aiService.sendMessage(conversationId, principal.getId(), request)));
    }

    @GetMapping("/conversations/{conversationId}")
    public ResponseEntity<ApiResponse<ConversationResponse>> getConversation(
            @PathVariable UUID conversationId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                aiService.getConversation(conversationId, principal.getId())));
    }

    @GetMapping("/conversations")
    public ResponseEntity<ApiResponse<List<ConversationResponse>>> listConversations(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(aiService.listConversations(principal.getId())));
    }

    @PostMapping("/images/generate")
    public ResponseEntity<ApiResponse<ImageGenerationResponse>> generateImage(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody GenerateImageRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                aiService.generateImage(principal.getId(), request)));
    }
}
