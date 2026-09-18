package com.lovebox.modules.gift.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter @NoArgsConstructor
public class CreateGiftDesignRequest {
    @NotBlank(message = "Loại nguồn thiết kế không được để trống")
    private String sourceType; // AI_GENERATED | SHOWCASE
    private String aiGeneratedImageId;
    private String showcaseDesignId;
    private String customizationData;
}
