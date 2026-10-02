import React, { createContext, useContext, useState } from 'react';
import { useAuth } from './AuthContext';
import { cartApi } from '../services/api';

const CartContext = createContext();

export const CartProvider = ({ children }) => {
  const { user } = useAuth();
  const [items, setItems] = useState([]);
  const [activeRestaurant, setActiveRestaurant] = useState(null);
  const [coupon, setCoupon] = useState(null);
  const [isCartDrawerOpen, setIsCartDrawerOpen] = useState(false);
  const [pendingItemReplace, setPendingItemReplace] = useState(null);

  const addItem = (item, restaurant) => {
    // If cart has items from another restaurant, prompt replacement
    if (activeRestaurant && activeRestaurant.id !== restaurant.id && items.length > 0) {
      setPendingItemReplace({ item, restaurant });
      return;
    }

    if (!activeRestaurant || items.length === 0) {
      setActiveRestaurant(restaurant);
    }

    setItems((prev) => {
      const existing = prev.find((i) => i.id === item.id);
      if (existing) {
        return prev.map((i) => (i.id === item.id ? { ...i, quantity: i.quantity + 1 } : i));
      }
      return [
        ...prev,
        {
          id: item.id,
          name: item.name,
          price: Number(item.price),
          isVeg: item.isVeg,
          image: item.imageUrl || item.image,
          restaurantId: restaurant.id,
          quantity: 1,
        },
      ];
    });

    // Sync to backend cart-service if user is authenticated
    if (user?.userId) {
      cartApi.addItem(user.userId, restaurant.id, item.id, 1).catch(() => {});
    }
  };

  const confirmReplaceCart = () => {
    if (pendingItemReplace) {
      const { item, restaurant } = pendingItemReplace;
      setActiveRestaurant(restaurant);
      setItems([
        {
          id: item.id,
          name: item.name,
          price: Number(item.price),
          isVeg: item.isVeg,
          image: item.imageUrl || item.image,
          restaurantId: restaurant.id,
          quantity: 1,
        },
      ]);
      setPendingItemReplace(null);
      if (user?.userId) {
        cartApi.clearCart(user.userId, activeRestaurant?.id).catch(() => {});
        cartApi.addItem(user.userId, restaurant.id, item.id, 1).catch(() => {});
      }
    }
  };

  const cancelReplaceCart = () => {
    setPendingItemReplace(null);
  };

  const updateQuantity = (itemId, newQty) => {
    if (newQty <= 0) {
      removeItem(itemId);
      return;
    }
    setItems((prev) =>
      prev.map((i) => (i.id === itemId ? { ...i, quantity: newQty } : i))
    );
    if (user?.userId && activeRestaurant?.id) {
      cartApi.updateItem(user.userId, activeRestaurant.id, itemId, newQty).catch(() => {});
    }
  };

  const removeItem = (itemId) => {
    setItems((prev) => {
      const next = prev.filter((i) => i.id !== itemId);
      if (next.length === 0) {
        setActiveRestaurant(null);
      }
      return next;
    });
    if (user?.userId && activeRestaurant?.id) {
      cartApi.removeItem(user.userId, activeRestaurant.id, itemId).catch(() => {});
    }
  };

  const clearCart = () => {
    const oldRestId = activeRestaurant?.id;
    setItems([]);
    setActiveRestaurant(null);
    setCoupon(null);
    if (user?.userId && oldRestId) {
      cartApi.clearCart(user.userId, oldRestId).catch(() => {});
    }
  };

  const applyCoupon = (code) => {
    const clean = code.trim().toUpperCase();
    if (clean === 'WELCOME50') {
      if (itemTotal < 199) return { success: false, message: 'Minimum order amount is ₹199' };
      setCoupon({ code: 'WELCOME50', discount: Math.min(100, itemTotal * 0.5) });
      return { success: true, message: '50% discount applied!' };
    }
    if (clean === 'SWIGGYONE') {
      setCoupon({ code: 'SWIGGYONE', freeDelivery: true });
      return { success: true, message: 'Free Delivery applied!' };
    }
    if (clean === 'FEAST20') {
      if (itemTotal < 499) return { success: false, message: 'Minimum order amount is ₹499' };
      setCoupon({ code: 'FEAST20', discount: 150 });
      return { success: true, message: '₹150 discount applied!' };
    }
    return { success: false, message: 'Invalid coupon code' };
  };

  const removeCoupon = () => setCoupon(null);

  // Bill computations
  const totalItems = items.reduce((acc, i) => acc + i.quantity, 0);
  const itemTotal = items.reduce((acc, i) => acc + i.price * i.quantity, 0);

  let deliveryFee = itemTotal > 0 ? 35 : 0;
  if (coupon?.freeDelivery || itemTotal >= 600) {
    deliveryFee = 0;
  }

  const platformFee = itemTotal > 0 ? 5 : 0;
  const gst = itemTotal > 0 ? Math.round(itemTotal * 0.05) : 0;
  const discount = coupon?.discount || 0;
  const finalTotal = Math.max(0, Math.round(itemTotal + deliveryFee + platformFee + gst - discount));

  return (
    <CartContext.Provider
      value={{
        items,
        activeRestaurant,
        addItem,
        updateQuantity,
        removeItem,
        clearCart,
        totalItems,
        itemTotal,
        deliveryFee,
        platformFee,
        gst,
        discount,
        finalTotal,
        coupon,
        applyCoupon,
        removeCoupon,
        isCartDrawerOpen,
        setIsCartDrawerOpen,
        pendingItemReplace,
        confirmReplaceCart,
        cancelReplaceCart,
      }}
    >
      {children}
    </CartContext.Provider>
  );
};

export const useCart = () => useContext(CartContext);
