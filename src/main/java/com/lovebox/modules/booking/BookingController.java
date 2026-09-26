package com.lovebox.modules.booking;

import com.lovebox.common.response.ApiResponse;
import com.lovebox.modules.booking.BookingDtos.*;
import com.lovebox.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping("/bookings/checkout")
    public ApiResponse<CheckoutResponse> checkout(@AuthenticationPrincipal UserPrincipal p,
                                                  @Valid @RequestBody CheckoutRequest req) {
        return ApiResponse.success("Đặt thuê thành công", bookingService.checkout(p.getId(), req));
    }

    @GetMapping("/bookings/checkout/{code}")
    public ApiResponse<CheckoutResponse> checkoutStatus(@AuthenticationPrincipal UserPrincipal p, @PathVariable long code) {
        return ApiResponse.success(bookingService.checkoutStatus(p.getId(), code));
    }

    @GetMapping("/bookings/mine")
    public ApiResponse<List<View>> mine(@AuthenticationPrincipal UserPrincipal p) {
        return ApiResponse.success(bookingService.mine(p.getId()));
    }

    @PostMapping("/bookings/{id}/cancel")
    public ApiResponse<View> cancel(@AuthenticationPrincipal UserPrincipal p, @PathVariable UUID id) {
        return ApiResponse.success(bookingService.cancelByRenter(p.getId(), id));
    }

    @PostMapping("/bookings/{id}/review")
    public ApiResponse<Void> review(@AuthenticationPrincipal UserPrincipal p, @PathVariable UUID id,
                                    @Valid @RequestBody ReviewRequest req) {
        bookingService.review(p.getId(), id, req);
        return ApiResponse.success("Cảm ơn bạn đã đánh giá", null);
    }

    // ── Chủ đồ ────────────────────────────────────────────────────────────
    @GetMapping("/owner/bookings")
    public ApiResponse<List<View>> ownerBookings(@AuthenticationPrincipal UserPrincipal p) {
        return ApiResponse.success(bookingService.forOwner(p.getId()));
    }

    @PatchMapping("/owner/bookings/{id}/status")
    public ApiResponse<View> ownerStatus(@AuthenticationPrincipal UserPrincipal p, @PathVariable UUID id,
                                         @Valid @RequestBody StatusChange req) {
        return ApiResponse.success(bookingService.changeStatus(p, id, req));
    }

    @GetMapping("/owner/stats")
    public ApiResponse<OwnerStats> ownerStats(@AuthenticationPrincipal UserPrincipal p) {
        return ApiResponse.success(bookingService.ownerStats(p.getId()));
    }
}
