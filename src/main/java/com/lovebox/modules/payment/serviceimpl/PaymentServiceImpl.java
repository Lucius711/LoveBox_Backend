package com.lovebox.modules.payment.serviceimpl;

import com.lovebox.common.constant.ErrorCode;
import com.lovebox.common.exception.AppException;
import com.lovebox.modules.order.entity.Order;
import com.lovebox.modules.order.repository.OrderRepository;
import com.lovebox.modules.payment.dto.request.UploadReceiptRequest;
import com.lovebox.modules.payment.dto.response.PaymentResponse;
import com.lovebox.modules.payment.entity.PaymentTransaction;
import com.lovebox.modules.payment.repository.PaymentTransactionRepository;
import com.lovebox.modules.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

@Service @RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentTransactionRepository transactionRepository;
    private final OrderRepository orderRepository;

    @Value("${app.payment.bank-account}") private String bankAccount;
    @Value("${app.payment.bank-name}") private String bankName;
    @Value("${app.payment.account-name}") private String accountName;

    @Override
    @Transactional
    public PaymentResponse initPayment(UUID orderId, UUID userId) {
        Order order = getAndValidateOrder(orderId, userId);

        return transactionRepository.findByOrderId(orderId)
                .map(PaymentResponse::from)
                .orElseGet(() -> {
                    String memo = URLEncoder.encode(order.getOrderCode(), StandardCharsets.UTF_8);
                    String vietqrPayload = String.format(
                            "https://img.vietqr.io/image/%s-%s-compact2.png?amount=%s&addInfo=%s&accountName=%s",
                            bankName, bankAccount, order.getTotalAmount().toPlainString(), memo,
                            URLEncoder.encode(accountName, StandardCharsets.UTF_8));

                    PaymentTransaction tx = PaymentTransaction.builder()
                            .orderId(orderId).amount(order.getTotalAmount())
                            .currency("VND").paymentMethod("VIETQR")
                            .status("AWAITING_RECEIPT").vietqrPayload(vietqrPayload).build();

                    return PaymentResponse.from(transactionRepository.save(tx));
                });
    }

    @Override
    @Transactional
    public PaymentResponse uploadReceipt(UUID orderId, UUID userId, UploadReceiptRequest request) {
        getAndValidateOrder(orderId, userId);

        PaymentTransaction tx = transactionRepository.findByOrderId(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_TRANSACTION_NOT_FOUND));

        if ("RECEIPT_UPLOADED".equals(tx.getStatus()) || "VERIFIED".equals(tx.getStatus())) {
            throw new AppException(ErrorCode.PAYMENT_RECEIPT_ALREADY_UPLOADED);
        }

        tx.setReceiptImageUrl(request.getReceiptImageUrl());
        tx.setReceiptUploadedAt(Instant.now());
        tx.setStatus("RECEIPT_UPLOADED");

        return PaymentResponse.from(transactionRepository.save(tx));
    }

    @Override
    public PaymentResponse getPaymentByOrder(UUID orderId, UUID userId) {
        getAndValidateOrder(orderId, userId);
        return PaymentResponse.from(
                transactionRepository.findByOrderId(orderId)
                        .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_TRANSACTION_NOT_FOUND)));
    }

    private Order getAndValidateOrder(UUID orderId, UUID userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        if (!order.getUserId().equals(userId)) throw new AppException(ErrorCode.ORDER_FORBIDDEN);
        return order;
    }
}
