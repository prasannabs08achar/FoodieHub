import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { orderApi } from '../services/api';

export default function DeliveryAgentDashboard() {
  const { user } = useAuth();
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(false);
  const [activeDelivery, setActiveDelivery] = useState(null);
  const [earnings, setEarnings] = useState(480);
  const [completedCount, setCompletedCount] = useState(8);

  const fetchOrders = async () => {
    setLoading(true);
    try {
      const ready = await orderApi.getReadyForPickupOrders();
      setOrders(ready || []);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchOrders();
  }, []);

  const handleAcceptDelivery = (order) => {
    setActiveDelivery(order);
    setOrders((prev) => prev.filter((o) => o.id !== order.id));
  };

  const handleMarkPickedUp = async () => {
    if (!activeDelivery) return;
    await orderApi.updateOrderStatus(activeDelivery.id, user.userId, 'PICKED_UP');
    setActiveDelivery((prev) => ({ ...prev, status: 'PICKED_UP' }));
  };

  const handleMarkDelivered = async () => {
    if (!activeDelivery) return;
    await orderApi.updateOrderStatus(activeDelivery.id, user.userId, 'DELIVERED');
    setEarnings((prev) => prev + 65);
    setCompletedCount((prev) => prev + 1);
    setActiveDelivery(null);
    fetchOrders();
  };

  return (
    <div className="max-w-xl mx-auto px-4 sm:px-6 py-6 pb-28">
      {/* Overview Card */}
      <div className="clean-card p-5 sm:p-6 bg-white mb-6">
        <div className="flex items-center gap-2 mb-1">
          <h1 className="text-xl font-extrabold text-slate-900 tracking-tight">
            Delivery Partner Portal
          </h1>
          <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-teal-100 text-teal-800">
            AGENT
          </span>
        </div>
        <p className="text-xs text-slate-500">
          Accept pickups and complete deliveries with Spring Boot dispatch-service
        </p>

        <div className="grid grid-cols-2 gap-3 mt-4">
          <div className="p-3.5 bg-slate-50 rounded-2xl border border-slate-100">
            <span className="text-[10px] font-extrabold text-slate-400 uppercase tracking-wider">
              Today's Earnings
            </span>
            <div className="text-2xl font-extrabold text-emerald-600 mt-0.5">
              ₹{earnings}
            </div>
          </div>
          <div className="p-3.5 bg-slate-50 rounded-2xl border border-slate-100">
            <span className="text-[10px] font-extrabold text-slate-400 uppercase tracking-wider">
              Completed Trips
            </span>
            <div className="text-2xl font-extrabold text-slate-900 mt-0.5">
              {completedCount}
            </div>
          </div>
        </div>
      </div>

      {/* Active Run Card */}
      {activeDelivery && (
        <div className="clean-card p-5 bg-orange-50/70 border-orange-200 mb-6 space-y-3">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-orange-900">
              🛵 Active Delivery in Progress
            </span>
            <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-orange-200 text-orange-900">
              {activeDelivery.status}
            </span>
          </div>

          <div className="text-xs text-slate-700">
            <div>
              <strong>Restaurant:</strong> {activeDelivery.restaurantName || activeDelivery.restaurantId}
            </div>
            <div className="text-[11px] text-slate-500 mt-0.5">
              Order ID: #{activeDelivery.id?.slice(0, 8)}
            </div>
          </div>

          <div>
            {activeDelivery.status === 'READY_FOR_PICKUP' ? (
              <button
                onClick={handleMarkPickedUp}
                className="btn-primary w-full text-xs"
              >
                Confirm Food Picked Up from Kitchen
              </button>
            ) : (
              <button
                onClick={handleMarkDelivered}
                className="btn-success w-full text-xs"
              >
                Mark Order Delivered (+₹65)
              </button>
            )}
          </div>
        </div>
      )}

      {/* Available Pickups */}
      <div className="clean-card p-5 sm:p-6 bg-white">
        <div className="flex items-center justify-between pb-3 border-b border-slate-100 mb-4">
          <h3 className="font-extrabold text-sm text-slate-900">
            Orders Ready for Pickup
          </h3>
          <button onClick={fetchOrders} className="btn-secondary text-xs !py-1 !px-2.5">
            🔄 Refresh
          </button>
        </div>

        {loading ? (
          <div className="text-center py-6 text-xs text-slate-400">Loading orders...</div>
        ) : orders.length === 0 ? (
          <div className="text-center py-8 text-xs text-slate-400">
            No orders currently waiting for pickup.
          </div>
        ) : (
          <div className="space-y-2.5">
            {orders.map((o) => (
              <div
                key={o.id}
                className="p-3.5 rounded-xl border border-slate-100 bg-slate-50/50 flex items-center justify-between gap-3 text-xs"
              >
                <div>
                  <div className="font-bold text-slate-900">Order #{o.id?.slice(0, 8)}</div>
                  <div className="text-[11px] text-slate-500 mt-0.5">
                    Ready at kitchen • Reward: <span className="font-bold text-emerald-600">₹65</span>
                  </div>
                </div>

                <button
                  onClick={() => handleAcceptDelivery(o)}
                  disabled={!!activeDelivery}
                  className="btn-primary text-xs !py-1.5 !px-3"
                >
                  Accept Run
                </button>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

export { DeliveryAgentDashboard };
