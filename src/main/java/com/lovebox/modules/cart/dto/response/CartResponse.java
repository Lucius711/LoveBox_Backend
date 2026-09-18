package com.lovebox.modules.cart.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter @Builder
public class CartResponse {
    private UUID cartId;
    private List<CartItemDto> items;
    private BigDecimal totalAmount;

    @Getter @Builder
    public static class CartItemDto {
        private UUID id;
        private UUID giftDesignId;
        private UUID greetingWishId;
        private UUID boxSizeId;
        private int quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;
    }
}
