package com.lovebox.modules.order.service;

import com.lovebox.modules.order.dto.request.CreateOrderRequest;
import com.lovebox.modules.order.dto.response.OrderResponse;

import java.util.List;
import java.util.UUID;

public interface OrderService {
    OrderResponse createOrder(UUID userId, CreateOrderRequest request);
    List<OrderResponse> listOrders(UUID userId);
    OrderResponse getOrder(UUID orderId, UUID userId);
    OrderResponse cancelOrder(UUID orderId, UUID userId);
}
