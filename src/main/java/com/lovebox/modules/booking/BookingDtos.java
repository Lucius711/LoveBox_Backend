package com.lovebox.modules.booking;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class BookingDtos {
    private BookingDtos() {}

    public record CartItem(@NotNull UUID productId, @NotNull LocalDate startDate, @NotNull LocalDate endDate) {}

    public record CheckoutRequest(
            @NotEmpty @Size(max = 10) List<@Valid CartItem> items,
            @NotBlank @Size(max = 255) String recipientName,
            @NotBlank @Pattern(regexp = "^0\\d{9,10}$", message = "Số điện thoại không hợp lệ") String phone,
            @NotBlank @Size(max = 500) String address,
            @NotBlank @Pattern(regexp = "PICKUP|EXPRESS") String deliveryMethod,
            @NotBlank @Pattern(regexp = "COD|PAYOS") String paymentMethod,
            @Size(max = 1000) String note) {}

    public record View(UUID id, String code, long checkoutCode, UUID productId, String productName, String productImage,
                       String renterName, LocalDate startDate, LocalDate endDate, int days, long rentAmount,
                       long depositAmount, long shippingFee, long totalAmount, String status, String paymentMethod,
                       String paymentStatus, String depositStatus, long deductionAmount, String adminNote,
                       String recipientName, String phone, String address, String deliveryMethod,
                       String refundBankAccount, String refundBankName, String note, Long refundAmount,
                       Instant refundedAt, String refundStatus, boolean reviewed,
                       Instant createdAt) {}

    public record CheckoutResponse(long checkoutCode, long totalAmount, String paymentMethod, String paymentStatus,
                                   String qrCode, String checkoutUrl, List<View> bookings) {}

    /** deductionAmount + note chỉ dùng khi admin chuyển sang DISPUTED/COMPLETED. */
    public record StatusChange(@NotBlank String status, @PositiveOrZero Long deductionAmount, String note) {}

    public record ReviewRequest(@Min(1) @Max(5) int rating, @Size(max = 1000) String comment) {}

    public record OwnerStats(long totalBookings, long pending, long active, long revenue, long productCount) {}

    public record AdminStats(long depositsHeld, long depositsRefunded, long depositsForfeited, long rentRevenue,
                             long unpaid, long pendingProducts, long refundsPending, long disputes, long totalBookings) {}
}
