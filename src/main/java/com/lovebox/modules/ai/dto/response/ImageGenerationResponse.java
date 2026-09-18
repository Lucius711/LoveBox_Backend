package com.lovebox.modules.ai.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter @Builder
public class ImageGenerationResponse {
    private UUID generationId;
    private UUID imageId;
    private String imageUrl;
    private String revisedPrompt;
    private String status;
}
