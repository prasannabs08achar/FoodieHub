package com.foodiehub.order_service.service;

import com.foodiehub.order_service.dao.CartDao;
import com.foodiehub.order_service.dao.CartItemDao;
import com.foodiehub.order_service.dto.AddCartItemRequest;
import com.foodiehub.order_service.dto.CartItemResponse;
import com.foodiehub.order_service.dto.CartResponse;
import com.foodiehub.order_service.dto.UpdateCartItemRequest;
import com.foodiehub.order_service.exception.CartItemNotFoundException;
import com.foodiehub.order_service.exception.CartNotFoundException;
import com.foodiehub.order_service.model.Cart;
import com.foodiehub.order_service.model.CartItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartDao cartDao;
    private final CartItemDao cartItemDao;

    @Transactional
    public CartResponse addItem(
            UUID customerId,
            UUID restaurantId,
            AddCartItemRequest request
    ) {

        Cart cart = cartDao
                .findByCustomerIdAndRestaurantId(
                        customerId,
                        restaurantId
                )
                .orElseGet(() ->
                        createCart(customerId, restaurantId)
                );

        CartItem cartItem = cartItemDao
                .findByCartIdAndMenuItemId(
                        cart.getId(),
                        request.menuItemId()
                )
                .orElse(null);

        if (cartItem == null) {

            cartItem = CartItem.builder()
                    .cartId(cart.getId())
                    .menuItemId(request.menuItemId())
                    .quantity(request.quantity())

                    // Temporary until Catalog integration
                    .unitPrice(BigDecimal.ZERO)

                    .build();

        } else {

            cartItem.setQuantity(
                    cartItem.getQuantity() + request.quantity()
            );
        }

        cartItemDao.save(cartItem);

        cart.setUpdatedAt(Instant.now());
        cartDao.save(cart);

        return mapToResponse(cart);
    }

    @Transactional(readOnly = true)
    public CartResponse getCart(
            UUID customerId,
            UUID restaurantId
    ) {

        Cart cart = cartDao
                .findByCustomerIdAndRestaurantId(
                        customerId,
                        restaurantId
                )
                .orElseThrow(() ->
                        new CartNotFoundException(
                                "Cart not found for customer and restaurant"
                        )
                );

        return mapToResponse(cart);
    }

    @Transactional
    public CartResponse updateItem(
            UUID customerId,
            UUID restaurantId,
            UUID menuItemId,
            UpdateCartItemRequest request
    ) {

        Cart cart = cartDao
                .findByCustomerIdAndRestaurantId(
                        customerId,
                        restaurantId
                )
                .orElseThrow(() ->
                        new CartNotFoundException(
                                "Cart not found for customer and restaurant"
                        )
                );

        CartItem cartItem = cartItemDao
                .findByCartIdAndMenuItemId(
                        cart.getId(),
                        menuItemId
                )
                .orElseThrow(() ->
                        new CartItemNotFoundException(
                                "Menu item is not present in the cart"
                        )
                );

        cartItem.setQuantity(request.quantity());

        cartItemDao.save(cartItem);

        cart.setUpdatedAt(Instant.now());
        cartDao.save(cart);

        return mapToResponse(cart);
    }

    @Transactional
    public void removeItem(
            UUID customerId,
            UUID restaurantId,
            UUID menuItemId
    ) {

        Cart cart = cartDao
                .findByCustomerIdAndRestaurantId(
                        customerId,
                        restaurantId
                )
                .orElseThrow(() ->
                        new CartNotFoundException(
                                "Cart not found for customer and restaurant"
                        )
                );

        CartItem cartItem = cartItemDao
                .findByCartIdAndMenuItemId(
                        cart.getId(),
                        menuItemId
                )
                .orElseThrow(() ->
                        new CartItemNotFoundException(
                                "Menu item is not present in the cart"
                        )
                );

        cartItemDao.delete(cartItem);

        cart.setUpdatedAt(Instant.now());
        cartDao.save(cart);
    }

    @Transactional
    public void clearCart(
            UUID customerId,
            UUID restaurantId
    ) {

        Cart cart = cartDao
                .findByCustomerIdAndRestaurantId(
                        customerId,
                        restaurantId
                )
                .orElseThrow(() ->
                        new CartNotFoundException(
                                "Cart not found for customer and restaurant"
                        )
                );

        cartItemDao.deleteByCartId(cart.getId());

        cart.setUpdatedAt(Instant.now());
        cartDao.save(cart);
    }

    private Cart createCart(
            UUID customerId,
            UUID restaurantId
    ) {

        Cart cart = Cart.builder()
                .customerId(customerId)
                .restaurantId(restaurantId)
                .build();

        return cartDao.save(cart);
    }

    private CartResponse mapToResponse(Cart cart) {

        List<CartItem> cartItems =
                cartItemDao.findByCartId(cart.getId());

        List<CartItemResponse> itemResponses =
                cartItems.stream()
                        .map(this::mapItemToResponse)
                        .toList();

        BigDecimal totalAmount =
                itemResponses.stream()
                        .map(CartItemResponse::totalPrice)
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        return new CartResponse(
                cart.getId(),
                cart.getCustomerId(),
                cart.getRestaurantId(),
                itemResponses,
                totalAmount,
                cart.getCreatedAt(),
                cart.getUpdatedAt()
        );
    }

    private CartItemResponse mapItemToResponse(
            CartItem cartItem
    ) {

        BigDecimal totalPrice =
                cartItem.getUnitPrice()
                        .multiply(
                                BigDecimal.valueOf(
                                        cartItem.getQuantity()
                                )
                        );

        return new CartItemResponse(
                cartItem.getId(),
                cartItem.getMenuItemId(),
                cartItem.getQuantity(),
                cartItem.getUnitPrice(),
                totalPrice
        );
    }
}