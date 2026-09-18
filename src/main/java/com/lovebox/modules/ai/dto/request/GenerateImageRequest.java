package com.lovebox.modules.ai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter @NoArgsConstructor
public class GenerateImageRequest {
    @NotBlank(message = "Prompt không được để trống")
    @Size(max = 1000, message = "Prompt không được vượt quá 1000 ký tự")
    private String prompt;

    private String conversationId;
}
