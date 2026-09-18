package com.lovebox.modules.gift.service;

import com.lovebox.modules.gift.dto.request.CreateGiftDesignRequest;
import com.lovebox.modules.gift.dto.request.CreateGreetingWishRequest;
import com.lovebox.modules.gift.dto.response.GiftDesignResponse;
import com.lovebox.modules.gift.dto.response.GreetingWishResponse;

import java.util.List;
import java.util.UUID;

public interface GiftService {
    GiftDesignResponse createGiftDesign(UUID userId, CreateGiftDesignRequest request);
    List<GiftDesignResponse> listGiftDesigns(UUID userId);
    GiftDesignResponse getGiftDesign(UUID id, UUID userId);
    GreetingWishResponse createGreetingWish(UUID userId, CreateGreetingWishRequest request);
    GreetingWishResponse getGreetingWishByQrToken(UUID qrToken);
}
