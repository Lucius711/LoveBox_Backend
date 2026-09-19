package com.lovebox.modules.aichat.controller;

import com.lovebox.modules.aichat.dto.request.ChatMessageRequest;
import com.lovebox.modules.aichat.dto.request.UpdatePreferencesRequest;
import com.lovebox.modules.aichat.dto.response.ChatConversationResponse;
import com.lovebox.modules.aichat.dto.response.ChatPreferencesResponse;
import com.lovebox.modules.aichat.service.ChatService;
import com.lovebox.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * SSE streaming chat endpoint.
     * Emits: thinking → chunk (word-by-word) → done | error
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChatMessageRequest request) {
        return chatService.streamChat(principal.getId(), request.getConversationId(), request.getMessage());
    }

    /**
     * List all conversations for the current user (newest first).
     */
    @GetMapping("/conversations")
    public ResponseEntity<List<ChatConversationResponse>> listConversations(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(chatService.listConversations(principal.getId()));
    }

    /**
     * Get a single conversation with its messages.
     */
    @GetMapping("/conversations/{id}")
    public ResponseEntity<ChatConversationResponse> getConversation(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        return ResponseEntity.ok(chatService.getConversation(id, principal.getId()));
    }

    /**
     * Delete (soft-delete / mark CLOSED) a conversation.
     */
    @DeleteMapping("/conversations/{id}")
    public ResponseEntity<Void> deleteConversation(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        chatService.deleteConversation(id, principal.getId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Get user's chat preferences (language, style, budget range…).
     */
    @GetMapping("/preferences")
    public ResponseEntity<ChatPreferencesResponse> getPreferences(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(chatService.getPreferences(principal.getId()));
    }

    /**
     * Update user's chat preferences.
     */
    @PutMapping("/preferences")
    public ResponseEntity<ChatPreferencesResponse> updatePreferences(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdatePreferencesRequest request) {
        return ResponseEntity.ok(chatService.updatePreferences(principal.getId(), request));
    }
}
