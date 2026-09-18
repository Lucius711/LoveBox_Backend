package com.lovebox.modules.payment.service;

import com.lovebox.modules.payment.dto.request.UploadReceiptRequest;
import com.lovebox.modules.payment.dto.response.PaymentResponse;

import java.util.UUID;

public interface PaymentService {
    PaymentResponse initPayment(UUID orderId, UUID userId);
    PaymentResponse uploadReceipt(UUID orderId, UUID userId, UploadReceiptRequest request);
    PaymentResponse getPaymentByOrder(UUID orderId, UUID userId);
}
