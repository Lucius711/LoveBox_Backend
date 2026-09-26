package com.lovebox.modules.product;

import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ProductDtos {
    private ProductDtos() {}

    /** Form đăng đồ: ép nhập đủ trường để AI làm việc. */
    public record ProductRequest(
            @NotBlank @Size(max = 255) String name,
            @NotBlank @Size(min = 20, max = 3000, message = "Mô tả tối thiểu 20 ký tự") String description,
            @NotBlank String category,
            @NotBlank @Pattern(regexp = "S|M|L|XL") String size,
            @Min(50) @Max(200) int bustMax,
            @Min(40) @Max(200) int waistMax,
            @Min(50) @Max(200) int hipMax,
            @NotBlank String itemCondition,
            @Min(10_000) long retailPrice,
            @Min(10_000) long rentPricePerDay,
            @Min(50) @Max(100) int depositPercent,
            @NotEmpty(message = "Chọn ít nhất 1 màu") List<String> colors,
            @NotEmpty(message = "Chọn ít nhất 1 phong cách") List<String> styles,
            @NotEmpty(message = "Chọn ít nhất 1 dịp phù hợp") List<String> occasions,
            List<String> features,
            @NotNull @Size(min = 3, max = 10, message = "Cần 3-10 ảnh (mặt trước, mặt sau, cận chất vải)") List<String> images) {}

    public record Summary(UUID id, String name, String category, String size, long rentPricePerDay, long deposit,
                          String image, List<String> colors, List<String> styles, int rentCount, String status,
                          String rejectReason, Instant createdAt) {
        public static Summary from(Product p) {
            return new Summary(p.getId(), p.getName(), p.getCategory(), p.getSize(), p.getRentPricePerDay(),
                    p.deposit(), p.cover(), p.tagValues(Product.COLOR), p.tagValues(Product.STYLE), p.getRentCount(),
                    p.getStatus(), p.getRejectReason(), p.getCreatedAt());
        }
    }

    public record Owner(UUID id, String name, String avatarUrl, int trustScore) {}

    public record ReviewView(String userName, int rating, String comment, Instant createdAt) {}

    public record Detail(UUID id, String name, String description, String category, String size,
                         int bustMax, int waistMax, int hipMax, String itemCondition,
                         long retailPrice, long rentPricePerDay, int depositPercent, long deposit,
                         List<String> colors, List<String> styles, List<String> occasions, List<String> features,
                         List<String> images, String status, String rejectReason, int rentCount, Owner owner,
                         double avgRating, List<ReviewView> reviews) {}

    public record ReviewDecision(boolean approve, String reason) {}

    public record PageResult<T>(List<T> items, int page, int size, long totalItems, int totalPages) {}
}
