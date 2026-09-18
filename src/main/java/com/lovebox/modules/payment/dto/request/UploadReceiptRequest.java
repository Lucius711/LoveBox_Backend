package com.lovebox.modules.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter @NoArgsConstructor
public class UploadReceiptRequest {
    @NotBlank(message = "URL biên lai không được để trống")
    private String receiptImageUrl;
}
