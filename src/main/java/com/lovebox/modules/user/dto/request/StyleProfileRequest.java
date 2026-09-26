package com.lovebox.modules.user.dto.request;

import jakarta.validation.constraints.*;

import java.util.List;

/** Câu trả lời onboarding — mọi trường đều tuỳ chọn (người dùng được bỏ qua). */
public record StyleProfileRequest(
        @Min(120) @Max(210) Integer heightCm,
        @Min(30) @Max(150) Integer weightKg,
        @Min(50) @Max(200) Integer bust,
        @Min(40) @Max(200) Integer waist,
        @Min(50) @Max(200) Integer hip,
        @Pattern(regexp = "S|M|L|XL") String clothingSize,
        @PositiveOrZero Long budgetMax,
        @Size(max = 10) List<String> favOccasions,
        @Size(max = 10) List<String> favStyles,
        @Size(max = 10) List<String> favColors,
        @Size(max = 500) String fitNote) {}
