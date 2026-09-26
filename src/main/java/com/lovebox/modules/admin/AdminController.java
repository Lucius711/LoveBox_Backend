package com.lovebox.modules.admin;

import com.lovebox.common.response.ApiResponse;
import com.lovebox.modules.booking.BookingDtos.*;
import com.lovebox.modules.booking.BookingService;
import com.lovebox.modules.product.ProductDtos.Detail;
import com.lovebox.modules.product.ProductDtos.ReviewDecision;
import com.lovebox.modules.product.ProductService;
import com.lovebox.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Chỉ ROLE_ADMIN (SecurityConfig): kiểm duyệt đồ, xử lý đơn/tranh chấp, dòng tiền. */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final ProductService productService;
    private final BookingService bookingService;

    @GetMapping("/products")
    public ApiResponse<List<Detail>> products(@RequestParam(required = false) String status) {
        return ApiResponse.success(productService.byStatus(status));
    }

    @PostMapping("/products/{id}/review")
    public ApiResponse<Detail> reviewProduct(@PathVariable UUID id, @RequestBody ReviewDecision decision) {
        return ApiResponse.success(productService.review(id, decision));
    }

    @GetMapping("/bookings")
    public ApiResponse<List<View>> bookings(@RequestParam(required = false) String status) {
        return ApiResponse.success(bookingService.all(status));
    }

    @PatchMapping("/bookings/{id}/status")
    public ApiResponse<View> bookingStatus(@AuthenticationPrincipal UserPrincipal p, @PathVariable UUID id,
                                           @Valid @RequestBody StatusChange req) {
        return ApiResponse.success(bookingService.changeStatus(p, id, req));
    }

    /** Admin xác nhận đã chuyển khoản hoàn tiền thủ công. */
    @PostMapping("/bookings/{id}/refund/confirm")
    public ApiResponse<View> confirmRefund(@PathVariable UUID id, @RequestBody(required = false) Map<String, String> body) {
        return ApiResponse.success("Đã ghi nhận hoàn tiền", bookingService.confirmRefund(id, body == null ? null : body.get("ref")));
    }

    @GetMapping("/stats")
    public ApiResponse<AdminStats> stats() {
        return ApiResponse.success(bookingService.adminStats());
    }
}
