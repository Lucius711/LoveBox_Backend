package com.lovebox.modules.ai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter @NoArgsConstructor
public class SendMessageRequest {
    @NotBlank(message = "Tin nhắn không được để trống")
    @Size(max = 4000, message = "Tin nhắn không được vượt quá 4000 ký tự")
    private String message;
}
