import React, { useEffect } from 'react';
import { useOrders } from '../context/OrderContext';
import { useAuth } from '../context/AuthContext';

export default function OrdersHistoryPage({ onTrackOrder, onBackToHome }) {
  const { orders, fetchOrders, setActiveTrackingOrder } = useOrders();
  const { user } = useAuth();

  useEffect(() => {
    fetchOrders();
  }, [user?.userId]);

  const handleTrack = (order) => {
    if (setActiveTrackingOrder) setActiveTrackingOrder(order);
    if (onTrackOrder) onTrackOrder(order);
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case 'DELIVERED':
        return <span className="px-2 py-0.5 rounded text-[11px] font-bold bg-emerald-100 text-emerald-800">Delivered</span>;
      case 'CANCELLED':
        return <span className="px-2 py-0.5 rounded text-[11px] font-bold bg-rose-100 text-rose-800">Cancelled</span>;
      case 'PLACED':
        return <span className="px-2 py-0.5 rounded text-[11px] font-bold bg-orange-100 text-orange-800">Placed</span>;
      case 'ACCEPTED':
        return <span className="px-2 py-0.5 rounded text-[11px] font-bold bg-blue-100 text-blue-800">Accepted</span>;
      case 'PREPARING':
        return <span className="px-2 py-0.5 rounded text-[11px] font-bold bg-amber-100 text-amber-800">Cooking</span>;
      case 'READY_FOR_PICKUP':
        return <span className="px-2 py-0.5 rounded text-[11px] font-bold bg-indigo-100 text-indigo-800">Ready</span>;
      case 'PICKED_UP':
        return <span className="px-2 py-0.5 rounded text-[11px] font-bold bg-teal-100 text-teal-800">Out for Delivery</span>;
      default:
        return <span className="px-2 py-0.5 rounded text-[11px] font-bold bg-slate-100 text-slate-700">{status}</span>;
    }
  };

  return (
    <div className="max-w-3xl mx-auto px-4 sm:px-6 py-6 pb-28">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-xl font-extrabold text-slate-900 tracking-tight">Your Past Orders</h1>
          <p className="text-xs text-slate-500 mt-0.5">Managed through Spring Boot order-service microservice</p>
        </div>
        <button
          onClick={onBackToHome}
          className="btn-secondary text-xs"
        >
          Explore Restaurants
        </button>
      </div>

      {orders.length === 0 ? (
        <div className="clean-card p-12 text-center bg-white">
          <div className="text-4xl mb-2">📦</div>
          <h3 className="font-bold text-base text-slate-800">No orders yet</h3>
          <p className="text-xs text-slate-500 mt-1 max-w-xs mx-auto">
            Looks like you haven't placed any orders yet. Treat yourself to delicious food!
          </p>
          <button onClick={onBackToHome} className="btn-primary mt-4 text-xs">
            Start Ordering
          </button>
        </div>
      ) : (
        <div className="space-y-4">
          {orders.map((order) => {
            const dateStr = order.createdAt ? new Date(order.createdAt).toLocaleString() : 'Recent';
            return (
              <div
                key={order.id}
                className="clean-card p-4 sm:p-5 bg-white space-y-3"
              >
                <div className="flex items-start justify-between gap-3 pb-3 border-b border-slate-100">
                  <div>
                    <h3 className="font-extrabold text-sm text-slate-900">
                      {order.restaurantName || `Restaurant #${order.restaurantId?.slice(0, 8)}`}
                    </h3>
                    <p className="text-[11px] text-slate-500 mt-0.5">
                      {dateStr} • Order #{order.id?.slice(0, 8)}
                    </p>
                  </div>
                  <div>{getStatusBadge(order.status)}</div>
                </div>

                {order.items && order.items.length > 0 && (
                  <div className="text-xs text-slate-600 bg-slate-50 p-2.5 rounded-lg border border-slate-100">
                    {order.items.map((i) => `${i.name || 'Dish'} × ${i.quantity || 1}`).join(', ')}
                  </div>
                )}

                <div className="flex items-center justify-between pt-1">
                  <div className="text-sm font-extrabold text-slate-900">
                    Total: ₹{order.totalAmount || order.subtotal || 0}
                  </div>
                  <button
                    onClick={() => handleTrack(order)}
                    className="px-3.5 py-1.5 rounded-lg border border-orange-500 text-orange-600 hover:bg-orange-50 text-xs font-bold transition-colors"
                  >
                    Track Live Status →
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

export { OrdersHistoryPage };
