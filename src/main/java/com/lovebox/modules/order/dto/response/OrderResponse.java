package com.lovebox.modules.order.dto.response;

import com.lovebox.modules.order.entity.Order;
import com.lovebox.modules.order.entity.OrderItem;
import com.lovebox.modules.order.entity.ShippingInfo;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter @Builder
public class OrderResponse {
    private UUID id;
    private String orderCode;
    private String status;
    private BigDecimal totalAmount;
    private String note;
    private ShippingDto shipping;
    private List<OrderItemDto> items;
    private Instant createdAt;

    @Getter @Builder
    public static class ShippingDto {
        private String recipientName;
        private String phone;
        private String addressLine1;
        private String addressLine2;
        private String city;
        private String district;
        private String ward;
        private String deliveryNote;

        public static ShippingDto from(ShippingInfo s) {
            return ShippingDto.builder()
                    .recipientName(s.getRecipientName()).phone(s.getPhone())
                    .addressLine1(s.getAddressLine1()).addressLine2(s.getAddressLine2())
                    .city(s.getCity()).district(s.getDistrict()).ward(s.getWard())
                    .deliveryNote(s.getDeliveryNote()).build();
        }
    }

    @Getter @Builder
    public static class OrderItemDto {
        private UUID id;
        private UUID giftDesignId;
        private UUID greetingWishId;
        private String boxSizeCode;
        private String boxSizeName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal lineTotal;

        public static OrderItemDto from(OrderItem i) {
            return OrderItemDto.builder()
                    .id(i.getId()).giftDesignId(i.getGiftDesignId())
                    .greetingWishId(i.getGreetingWishId())
                    .boxSizeCode(i.getBoxSizeCode()).boxSizeName(i.getBoxSizeName())
                    .quantity(i.getQuantity()).unitPrice(i.getUnitPrice())
                    .lineTotal(i.getLineTotal()).build();
        }
    }
}
