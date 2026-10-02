import React, { createContext, useContext, useState, useEffect } from 'react';
import confetti from 'canvas-confetti';
import { useAuth } from './AuthContext';
import { useLocation } from './LocationContext';
import { useCart } from './CartContext';
import { useWallet } from './WalletContext';
import { orderApi } from '../services/api';

const OrderContext = createContext();

export const OrderProvider = ({ children }) => {
  const { user, setIsAuthModalOpen } = useAuth();
  const { currentLocation } = useLocation();
  const { items, activeRestaurant, clearCart, finalTotal, setIsCartDrawerOpen } = useCart();
  const { balance, fetchWallet, setIsWalletModalOpen } = useWallet();

  const [orders, setOrders] = useState([]);
  const [activeTrackingOrder, setActiveTrackingOrder] = useState(null);
  const [placingOrder, setPlacingOrder] = useState(false);
  const [errorMsg, setErrorMsg] = useState(null);

  const fetchOrders = async () => {
    if (!user?.userId) return;
    try {
      const data = await orderApi.getCustomerOrders(user.userId);
      setOrders(data || []);
      // If we don't have an active tracking order but have a recently placed one, track it
      if (!activeTrackingOrder && data && data.length > 0) {
        const active = data.find(
          (o) => o.status !== 'DELIVERED' && o.status !== 'CANCELLED'
        );
        if (active) setActiveTrackingOrder(active);
      }
    } catch (e) {
      console.error('Fetch orders error:', e);
    }
  };

  useEffect(() => {
    fetchOrders();
  }, [user?.userId]);

  const placeOrder = async () => {
    setErrorMsg(null);
    if (!user) {
      setIsAuthModalOpen(true);
      return null;
    }
    if (items.length === 0 || !activeRestaurant) {
      setErrorMsg('Your cart is empty');
      return null;
    }

    // Check wallet balance
    if (balance < finalTotal) {
      setErrorMsg(`Insufficient wallet balance. You need ₹${finalTotal - balance} more.`);
      setIsWalletModalOpen(true);
      return null;
    }

    setPlacingOrder(true);
    try {
      const placedOrder = await orderApi.placeOrder(
        user.userId,
        activeRestaurant.id,
        currentLocation.lat,
        currentLocation.lng,
        items,
        finalTotal
      );

      // Trigger celebration confetti
      try {
        confetti({
          particleCount: 100,
          spread: 70,
          origin: { y: 0.6 },
          colors: ['#fc8019', '#e23744', '#10b981', '#3b82f6'],
        });
      } catch (err) {}

      // Refresh wallet & orders
      await fetchWallet();
      await fetchOrders();

      // Clear the cart & close cart drawer
      clearCart();
      setIsCartDrawerOpen(false);

      // Set as active tracking order
      setActiveTrackingOrder({
        ...placedOrder,
        restaurantName: activeRestaurant.name,
        restaurantAddress: activeRestaurant.address,
        restaurantImage: activeRestaurant.image,
      });

      return placedOrder;
    } catch (err) {
      console.error('Failed to place order:', err);
      setErrorMsg(err.message || 'Failed to place order. Please try again.');
      return null;
    } finally {
      setPlacingOrder(false);
    }
  };

  const cancelOrder = async (orderId, reason = 'Customer cancelled') => {
    if (!user?.userId) return;
    try {
      await orderApi.cancelOrder(orderId, user.userId, reason);
      await fetchOrders();
      await fetchWallet();
      if (activeTrackingOrder?.id === orderId) {
        setActiveTrackingOrder((prev) => (prev ? { ...prev, status: 'CANCELLED' } : null));
      }
      return true;
    } catch (err) {
      console.error('Cancel order error:', err);
      return false;
    }
  };

  const advanceOrderStatus = async (orderId, nextStatus) => {
    if (!user?.userId) return;
    try {
      const updated = await orderApi.updateOrderStatus(orderId, user.userId, nextStatus);
      await fetchOrders();
      if (activeTrackingOrder?.id === orderId) {
        setActiveTrackingOrder((prev) => (prev ? { ...prev, status: nextStatus } : null));
      }
      return updated;
    } catch (e) {
      console.error(e);
    }
  };

  return (
    <OrderContext.Provider
      value={{
        orders,
        activeTrackingOrder,
        setActiveTrackingOrder,
        placeOrder,
        cancelOrder,
        advanceOrderStatus,
        placingOrder,
        errorMsg,
        fetchOrders,
      }}
    >
      {children}
    </OrderContext.Provider>
  );
};

export const useOrders = () => useContext(OrderContext);
