package com.lovebox.modules.cart.service;

import com.lovebox.modules.cart.dto.request.AddCartItemRequest;
import com.lovebox.modules.cart.dto.response.CartResponse;

import java.util.UUID;

public interface CartService {
    CartResponse getCart(UUID userId);
    CartResponse addItem(UUID userId, AddCartItemRequest request);
    CartResponse updateItemQuantity(UUID userId, UUID itemId, int quantity);
    void removeItem(UUID userId, UUID itemId);
    void clearCart(UUID userId);
}
