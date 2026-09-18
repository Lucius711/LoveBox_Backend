package com.lovebox.modules.cart.serviceimpl;

import com.lovebox.common.constant.ErrorCode;
import com.lovebox.common.exception.AppException;
import com.lovebox.modules.cart.dto.request.AddCartItemRequest;
import com.lovebox.modules.cart.dto.response.CartResponse;
import com.lovebox.modules.cart.entity.Cart;
import com.lovebox.modules.cart.entity.CartItem;
import com.lovebox.modules.cart.repository.CartItemRepository;
import com.lovebox.modules.cart.repository.CartRepository;
import com.lovebox.modules.cart.service.CartService;
import com.lovebox.modules.catalog.entity.BoxSize;
import com.lovebox.modules.catalog.repository.BoxSizeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service @RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final BoxSizeRepository boxSizeRepository;

    @Override
    public CartResponse getCart(UUID userId) {
        Cart cart = getOrCreateCart(userId);
        return buildResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse addItem(UUID userId, AddCartItemRequest request) {
        BoxSize boxSize = boxSizeRepository.findById(request.getBoxSizeId())
                .orElseThrow(() -> new AppException(ErrorCode.BOX_SIZE_NOT_FOUND));
        if (boxSize.getIsActive() != 1) {
            throw new AppException(ErrorCode.BOX_SIZE_INACTIVE);
        }

        Cart cart = getOrCreateCart(userId);
        CartItem item = CartItem.builder()
                .cartId(cart.getId())
                .giftDesignId(request.getGiftDesignId())
                .greetingWishId(request.getGreetingWishId())
                .boxSizeId(request.getBoxSizeId())
                .quantity(request.getQuantity())
                .unitPrice(boxSize.getPrice())
                .build();
        cartItemRepository.save(item);
        return buildResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse updateItemQuantity(UUID userId, UUID itemId, int quantity) {
        if (quantity < 1) throw new AppException(ErrorCode.CART_ITEM_QUANTITY_INVALID);
        Cart cart = getOrCreateCart(userId);
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new AppException(ErrorCode.CART_ITEM_NOT_FOUND));
        if (!item.getCartId().equals(cart.getId())) {
            throw new AppException(ErrorCode.CART_ITEM_NOT_FOUND);
        }
        item.setQuantity(quantity);
        cartItemRepository.save(item);
        return buildResponse(cart);
    }

    @Override
    @Transactional
    public void removeItem(UUID userId, UUID itemId) {
        Cart cart = getOrCreateCart(userId);
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new AppException(ErrorCode.CART_ITEM_NOT_FOUND));
        if (!item.getCartId().equals(cart.getId())) {
            throw new AppException(ErrorCode.CART_ITEM_NOT_FOUND);
        }
        cartItemRepository.delete(item);
    }

    @Override
    @Transactional
    public void clearCart(UUID userId) {
        cartRepository.findByUserId(userId).ifPresent(cart ->
                cartItemRepository.deleteByCartId(cart.getId()));
    }

    private Cart getOrCreateCart(UUID userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> cartRepository.save(Cart.builder().userId(userId).build()));
    }

    private CartResponse buildResponse(Cart cart) {
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
        List<CartResponse.CartItemDto> dtos = items.stream().map(i -> CartResponse.CartItemDto.builder()
                .id(i.getId()).giftDesignId(i.getGiftDesignId()).greetingWishId(i.getGreetingWishId())
                .boxSizeId(i.getBoxSizeId()).quantity(i.getQuantity()).unitPrice(i.getUnitPrice())
                .subtotal(i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity()))).build()).toList();
        BigDecimal total = dtos.stream()
                .map(CartResponse.CartItemDto::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return CartResponse.builder().cartId(cart.getId()).items(dtos).totalAmount(total).build();
    }
}
