package com.lovebox.modules.cart.controller;

import com.lovebox.common.response.ApiResponse;
import com.lovebox.modules.cart.dto.request.AddCartItemRequest;
import com.lovebox.modules.cart.dto.response.CartResponse;
import com.lovebox.modules.cart.service.CartService;
import com.lovebox.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(cartService.getCart(principal.getId())));
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartResponse>> addItem(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AddCartItemRequest request) {
        return ResponseEntity.ok(ApiResponse.success(cartService.addItem(principal.getId(), request)));
    }

    @PatchMapping("/items/{itemId}/quantity")
    public ResponseEntity<ApiResponse<CartResponse>> updateQuantity(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID itemId,
            @RequestParam int quantity) {
        return ResponseEntity.ok(ApiResponse.success(
                cartService.updateItemQuantity(principal.getId(), itemId, quantity)));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<Void>> removeItem(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID itemId) {
        cartService.removeItem(principal.getId(), itemId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> clearCart(
            @AuthenticationPrincipal UserPrincipal principal) {
        cartService.clearCart(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
