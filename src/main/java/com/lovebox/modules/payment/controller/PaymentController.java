package com.lovebox.modules.payment.controller;

import com.lovebox.common.response.ApiResponse;
import com.lovebox.modules.payment.dto.request.UploadReceiptRequest;
import com.lovebox.modules.payment.dto.response.PaymentResponse;
import com.lovebox.modules.payment.service.PaymentService;
import com.lovebox.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/orders/{orderId}/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/init")
    public ResponseEntity<ApiResponse<PaymentResponse>> initPayment(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                paymentService.initPayment(orderId, principal.getId())));
    }

    @PostMapping("/receipt")
    public ResponseEntity<ApiResponse<PaymentResponse>> uploadReceipt(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UploadReceiptRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                paymentService.uploadReceipt(orderId, principal.getId(), request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PaymentResponse>> getPayment(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                paymentService.getPaymentByOrder(orderId, principal.getId())));
    }
}
