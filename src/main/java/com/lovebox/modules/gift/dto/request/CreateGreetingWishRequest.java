package com.lovebox.modules.gift.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter @NoArgsConstructor
public class CreateGreetingWishRequest {
    @NotBlank(message = "Tên người nhận không được để trống")
    @Size(max = 255) private String recipientName;

    @NotBlank(message = "Lời chúc không được để trống")
    @Size(max = 2000) private String message;

    @Size(max = 255) private String senderName;
}
