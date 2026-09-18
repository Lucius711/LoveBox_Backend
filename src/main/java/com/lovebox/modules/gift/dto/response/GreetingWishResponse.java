package com.lovebox.modules.gift.dto.response;

import com.lovebox.modules.gift.entity.GreetingWish;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter @Builder
public class GreetingWishResponse {
    private UUID id;
    private String recipientName;
    private String message;
    private String senderName;
    private UUID qrToken;
    private Instant createdAt;

    public static GreetingWishResponse from(GreetingWish w) {
        return GreetingWishResponse.builder()
                .id(w.getId()).recipientName(w.getRecipientName())
                .message(w.getMessage()).senderName(w.getSenderName())
                .qrToken(w.getQrToken()).createdAt(w.getCreatedAt()).build();
    }
}
