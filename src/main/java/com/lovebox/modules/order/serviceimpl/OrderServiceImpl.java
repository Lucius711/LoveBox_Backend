package com.lovebox.modules.order.serviceimpl;

import com.lovebox.common.constant.ErrorCode;
import com.lovebox.common.exception.AppException;
import com.lovebox.common.util.OrderCodeGenerator;
import com.lovebox.modules.cart.entity.CartItem;
import com.lovebox.modules.cart.repository.CartItemRepository;
import com.lovebox.modules.cart.repository.CartRepository;
import com.lovebox.modules.catalog.entity.BoxSize;
import com.lovebox.modules.catalog.repository.BoxSizeRepository;
import com.lovebox.modules.order.dto.request.CreateOrderRequest;
import com.lovebox.modules.order.dto.response.OrderResponse;
import com.lovebox.modules.order.entity.*;
import com.lovebox.modules.order.repository.*;
import com.lovebox.modules.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ShippingInfoRepository shippingInfoRepository;
    private final OrderStatusHistoryRepository statusHistoryRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final BoxSizeRepository boxSizeRepository;
    private final OrderCodeGenerator orderCodeGenerator;

    @Override
    @Transactional
    public OrderResponse createOrder(UUID userId, CreateOrderRequest request) {
        var cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.CART_NOT_FOUND));
        List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getId());
        if (cartItems.isEmpty()) throw new AppException(ErrorCode.CART_EMPTY);

        Map<UUID, BoxSize> boxSizeMap = boxSizeRepository.findAllById(
                cartItems.stream().map(CartItem::getBoxSizeId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(BoxSize::getId, b -> b));

        BigDecimal total = cartItems.stream()
                .map(i -> boxSizeMap.get(i.getBoxSizeId()).getPrice()
                        .multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long orderCount = orderRepository.countByUserId(userId);
        String orderCode = orderCodeGenerator.generate(orderCount);

        Order order = orderRepository.save(Order.builder()
                .userId(userId).orderCode(orderCode)
                .status("PENDING_PAYMENT").totalAmount(total)
                .note(request.getNote()).build());

        cartItems.forEach(ci -> {
            BoxSize bs = boxSizeMap.get(ci.getBoxSizeId());
            orderItemRepository.save(OrderItem.builder()
                    .orderId(order.getId()).giftDesignId(ci.getGiftDesignId())
                    .greetingWishId(ci.getGreetingWishId()).boxSizeId(ci.getBoxSizeId())
                    .boxSizeCode(bs.getCode()).boxSizeName(bs.getName())
                    .quantity(ci.getQuantity()).unitPrice(bs.getPrice())
                    .lineTotal(bs.getPrice().multiply(BigDecimal.valueOf(ci.getQuantity()))).build());
        });

        shippingInfoRepository.save(ShippingInfo.builder()
                .orderId(order.getId()).recipientName(request.getRecipientName())
                .phone(request.getPhone()).addressLine1(request.getAddressLine1())
                .addressLine2(request.getAddressLine2()).city(request.getCity())
                .district(request.getDistrict()).ward(request.getWard())
                .deliveryNote(request.getDeliveryNote()).build());

        statusHistoryRepository.save(OrderStatusHistory.builder()
                .orderId(order.getId()).fromStatus(null)
                .toStatus("PENDING_PAYMENT").changedBy(userId.toString()).build());

        cartItemRepository.deleteByCartId(cart.getId());

        return buildResponse(order);
    }

    @Override
    public List<OrderResponse> listOrders(UUID userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::buildResponse).toList();
    }

    @Override
    public OrderResponse getOrder(UUID orderId, UUID userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        if (!order.getUserId().equals(userId)) throw new AppException(ErrorCode.ORDER_FORBIDDEN);
        return buildResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(UUID orderId, UUID userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        if (!order.getUserId().equals(userId)) throw new AppException(ErrorCode.ORDER_FORBIDDEN);
        if ("CANCELLED".equals(order.getStatus())) throw new AppException(ErrorCode.ORDER_ALREADY_CANCELLED);
        if (!List.of("PENDING_PAYMENT", "PAID").contains(order.getStatus())) {
            throw new AppException(ErrorCode.ORDER_CANNOT_CANCEL);
        }
        String prev = order.getStatus();
        order.setStatus("CANCELLED");
        orderRepository.save(order);
        statusHistoryRepository.save(OrderStatusHistory.builder()
                .orderId(orderId).fromStatus(prev).toStatus("CANCELLED")
                .changedBy(userId.toString()).build());
        return buildResponse(order);
    }

    private OrderResponse buildResponse(Order order) {
        List<OrderResponse.OrderItemDto> items = orderItemRepository.findByOrderId(order.getId())
                .stream().map(OrderResponse.OrderItemDto::from).toList();
        ShippingInfo shipping = shippingInfoRepository.findByOrderId(order.getId()).orElse(null);
        return OrderResponse.builder()
                .id(order.getId()).orderCode(order.getOrderCode()).status(order.getStatus())
                .totalAmount(order.getTotalAmount()).note(order.getNote())
                .shipping(shipping != null ? OrderResponse.ShippingDto.from(shipping) : null)
                .items(items).createdAt(order.getCreatedAt()).build();
    }
}
