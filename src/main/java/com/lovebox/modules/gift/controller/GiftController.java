package com.lovebox.modules.gift.controller;

import com.lovebox.common.response.ApiResponse;
import com.lovebox.modules.gift.dto.request.CreateGiftDesignRequest;
import com.lovebox.modules.gift.dto.request.CreateGreetingWishRequest;
import com.lovebox.modules.gift.dto.response.GiftDesignResponse;
import com.lovebox.modules.gift.dto.response.GreetingWishResponse;
import com.lovebox.modules.gift.service.GiftService;
import com.lovebox.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class GiftController {

    private final GiftService giftService;

    @PostMapping("/gift-designs")
    public ResponseEntity<ApiResponse<GiftDesignResponse>> createGiftDesign(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateGiftDesignRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                giftService.createGiftDesign(principal.getId(), request)));
    }

    @GetMapping("/gift-designs")
    public ResponseEntity<ApiResponse<List<GiftDesignResponse>>> listGiftDesigns(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(giftService.listGiftDesigns(principal.getId())));
    }

    @GetMapping("/gift-designs/{id}")
    public ResponseEntity<ApiResponse<GiftDesignResponse>> getGiftDesign(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(giftService.getGiftDesign(id, principal.getId())));
    }

    @PostMapping("/greeting-wishes")
    public ResponseEntity<ApiResponse<GreetingWishResponse>> createGreetingWish(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateGreetingWishRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                giftService.createGreetingWish(principal.getId(), request)));
    }

    @GetMapping("/greeting-wishes/qr/{qrToken}")
    public ResponseEntity<ApiResponse<GreetingWishResponse>> getByQrToken(
            @PathVariable UUID qrToken) {
        return ResponseEntity.ok(ApiResponse.success(giftService.getGreetingWishByQrToken(qrToken)));
    }
}
