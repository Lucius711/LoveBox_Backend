package com.lovebox.modules.cart.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter @NoArgsConstructor
public class AddCartItemRequest {
    @NotNull(message = "ID thiết kế hộp không được để trống") private UUID giftDesignId;
    @NotNull(message = "ID kích thước hộp không được để trống") private UUID boxSizeId;
    private UUID greetingWishId;
    @Min(value = 1, message = "Số lượng phải ít nhất 1") private int quantity = 1;
}
