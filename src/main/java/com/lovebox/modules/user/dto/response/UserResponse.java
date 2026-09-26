package com.lovebox.modules.user.dto.response;

import com.lovebox.modules.user.entity.User;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserResponse(UUID id, String email, String name, String avatarUrl, String role,
                           String phone, String address, String bankAccount, String bankName,
                           int trustScore, Instant createdAt, boolean onboarded, StyleProfile styleProfile) {

    public record StyleProfile(Integer heightCm, Integer weightKg, Integer bust, Integer waist, Integer hip,
                               String clothingSize, Long budgetMax, List<String> favOccasions, List<String> favStyles,
                               List<String> favColors, String fitNote) {}

    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getEmail(), u.getName(), u.getAvatarUrl(), u.getRole(),
                u.getPhone(), u.getAddress(), u.getBankAccount(), u.getBankName(), u.getTrustScore(), u.getCreatedAt(),
                u.getOnboardedAt() != null,
                new StyleProfile(u.getHeightCm(), u.getWeightKg(), u.getBust(), u.getWaist(), u.getHip(), u.getClothingSize(), u.getBudgetMax(),
                        u.getFavOccasions(), u.getFavStyles(), u.getFavColors(), u.getFitNote()));
    }
}
