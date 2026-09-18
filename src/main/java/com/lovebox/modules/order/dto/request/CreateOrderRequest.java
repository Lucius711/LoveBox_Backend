package com.lovebox.modules.order.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter @NoArgsConstructor
public class CreateOrderRequest {
    @NotBlank @jakarta.validation.constraints.Size(max = 255)
    private String recipientName;

    @NotBlank @Pattern(regexp = "^[0-9]{9,11}$", message = "Số điện thoại không hợp lệ")
    private String phone;

    @NotBlank @jakarta.validation.constraints.Size(max = 500)
    private String addressLine1;

    @jakarta.validation.constraints.Size(max = 500) private String addressLine2;

    @NotBlank @jakarta.validation.constraints.Size(max = 100) private String city;

    @jakarta.validation.constraints.Size(max = 100) private String district;
    @jakarta.validation.constraints.Size(max = 100) private String ward;
    @jakarta.validation.constraints.Size(max = 500) private String deliveryNote;
    @jakarta.validation.constraints.Size(max = 1000) private String note;
}
