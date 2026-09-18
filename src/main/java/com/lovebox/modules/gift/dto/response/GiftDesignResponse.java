package com.lovebox.modules.gift.dto.response;

import com.lovebox.modules.gift.entity.GiftDesign;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter @Builder
public class GiftDesignResponse {
    private UUID id;
    private String sourceType;
    private UUID aiGeneratedImageId;
    private UUID showcaseDesignId;
    private String customizationData;
    private String status;
    private Instant createdAt;

    public static GiftDesignResponse from(GiftDesign g) {
        return GiftDesignResponse.builder()
                .id(g.getId()).sourceType(g.getSourceType())
                .aiGeneratedImageId(g.getAiGeneratedImageId())
                .showcaseDesignId(g.getShowcaseDesignId())
                .customizationData(g.getCustomizationData())
                .status(g.getStatus()).createdAt(g.getCreatedAt()).build();
    }
}
