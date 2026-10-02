import React, { useState, useEffect } from 'react';
import { useOrders } from '../context/OrderContext';

export default function OrderTrackingPage({ order, onBack }) {
  const { advanceOrderStatus, cancelOrder, activeTrackingOrder } = useOrders();

  const currentOrder = activeTrackingOrder || order;

  const STAGES = [
    { key: 'PLACED', title: 'Order Placed', desc: 'Sent to restaurant kitchen', icon: '📝' },
    { key: 'ACCEPTED', title: 'Kitchen Accepted', desc: 'Restaurant confirmed your order', icon: '👨‍🍳' },
    { key: 'PREPARING', title: 'Cooking Food', desc: 'Chef is crafting fresh dishes', icon: '🍳' },
    { key: 'READY_FOR_PICKUP', title: 'Ready for Pickup', desc: 'Waiting for delivery partner', icon: '📦' },
    { key: 'PICKED_UP', title: 'Out for Delivery', desc: 'Delivery partner on the way', icon: '🛵' },
    { key: 'DELIVERED', title: 'Delivered', desc: 'Delivered successfully. Enjoy!', icon: '🎉' },
  ];

  const currentStageIndex = STAGES.findIndex((s) => s.key === currentOrder?.status);
  const isCancelled = currentOrder?.status === 'CANCELLED';

  const [etaMinutes, setEtaMinutes] = useState(25);

  useEffect(() => {
    if (currentOrder?.status === 'PLACED') setEtaMinutes(30);
    else if (currentOrder?.status === 'ACCEPTED') setEtaMinutes(25);
    else if (currentOrder?.status === 'PREPARING') setEtaMinutes(18);
    else if (currentOrder?.status === 'READY_FOR_PICKUP') setEtaMinutes(12);
    else if (currentOrder?.status === 'PICKED_UP') setEtaMinutes(7);
    else if (currentOrder?.status === 'DELIVERED') setEtaMinutes(0);
  }, [currentOrder?.status]);

  if (!currentOrder) {
    return (
      <div className="max-w-xl mx-auto px-4 py-16 text-center">
        <div className="clean-card p-10 bg-white">
          <div className="text-4xl mb-3">📍</div>
          <h3 className="text-base font-bold text-slate-800">No active order to track</h3>
          <p className="text-xs text-slate-500 mt-1">Place an order to see real-time updates.</p>
          <button className="btn-primary mt-4 text-xs" onClick={onBack}>
            Browse Restaurants
          </button>
        </div>
      </div>
    );
  }

  const handleNextStatus = () => {
    if (currentStageIndex >= 0 && currentStageIndex < STAGES.length - 1) {
      const next = STAGES[currentStageIndex + 1].key;
      advanceOrderStatus(currentOrder.id, next);
    }
  };

  const handleCancel = () => {
    if (confirm('Cancel this order? Refund will be credited back to your FoodieHub wallet.')) {
      cancelOrder(currentOrder.id, 'Customer cancelled');
    }
  };

  return (
    <div className="max-w-xl mx-auto px-4 sm:px-6 py-6 pb-28">
      {/* Back button */}
      <button
        onClick={onBack}
        className="inline-flex items-center gap-1.5 text-xs font-semibold text-slate-600 hover:text-slate-900 mb-4 transition-colors"
      >
        <span>←</span>
        <span>Back to Restaurants</span>
      </button>

      {/* Top Banner Card */}
      <div className={`p-6 rounded-2xl text-white shadow-md mb-6 ${
        isCancelled
          ? 'bg-rose-600'
          : currentOrder.status === 'DELIVERED'
          ? 'bg-emerald-600'
          : 'bg-gradient-to-r from-orange-500 to-amber-500'
      }`}>
        <div className="flex items-center justify-between">
          <span className="text-[11px] font-extrabold uppercase px-2.5 py-0.5 rounded-full bg-white/20">
            Order #{currentOrder.id?.slice(0, 8)}
          </span>
          <span className="text-2xl">
            {isCancelled ? '❌' : currentOrder.status === 'DELIVERED' ? '🎉' : '🛵'}
          </span>
        </div>

        <h1 className="text-xl sm:text-2xl font-extrabold mt-3 tracking-tight">
          {isCancelled
            ? 'Order Cancelled'
            : currentOrder.status === 'DELIVERED'
            ? 'Order Delivered!'
            : `Arriving in ~${etaMinutes} mins`}
        </h1>
        <p className="text-xs text-white/90 mt-1">
          {isCancelled
            ? 'Refund credited back to your FoodieHub wallet.'
            : currentOrder.status === 'DELIVERED'
            ? 'Thank you for ordering with FoodieHub!'
            : `Live tracking from ${currentOrder.restaurantName || 'Restaurant'}`}
        </p>
      </div>

      {/* Timeline Card */}
      {!isCancelled && (
        <div className="clean-card p-5 sm:p-6 bg-white mb-6">
          <h2 className="text-sm font-extrabold text-slate-900 mb-4">
            Live Order Progression
          </h2>

          <div className="space-y-5">
            {STAGES.map((s, idx) => {
              const isPast = idx < currentStageIndex;
              const isCurrent = idx === currentStageIndex;
              return (
                <div key={s.key} className="flex items-start gap-3.5">
                  <div className={`w-7 h-7 rounded-full flex items-center justify-center text-xs font-bold flex-shrink-0 transition-colors ${
                    isPast
                      ? 'bg-emerald-600 text-white'
                      : isCurrent
                      ? 'bg-orange-500 text-white ring-4 ring-orange-100'
                      : 'bg-slate-100 text-slate-400'
                  }`}>
                    {isPast ? '✓' : idx + 1}
                  </div>

                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-1.5">
                      <span className={`text-xs font-bold ${isCurrent ? 'text-orange-600' : 'text-slate-800'}`}>
                        {s.title}
                      </span>
                      <span>{s.icon}</span>
                    </div>
                    <p className="text-[11px] text-slate-500 mt-0.5">{s.desc}</p>
                  </div>
                </div>
              );
            })}
          </div>

          {currentOrder.status !== 'DELIVERED' && (
            <div className="pt-4 mt-4 border-t border-slate-100 flex justify-end">
              <button
                onClick={handleCancel}
                className="px-3.5 py-2 rounded-xl border border-red-200 text-red-600 hover:bg-red-50 text-xs font-semibold transition-colors"
              >
                Cancel Order
              </button>
            </div>
          )}
        </div>
      )}

      {/* Order Summary Details */}
      <div className="clean-card p-5 bg-white space-y-3">
        <h3 className="font-extrabold text-sm text-slate-900">Order Summary</h3>
        <div className="flex justify-between text-xs text-slate-600">
          <span>Restaurant</span>
          <span className="font-semibold text-slate-800">
            {currentOrder.restaurantName || currentOrder.restaurantId}
          </span>
        </div>
        <div className="flex justify-between text-xs text-slate-600">
          <span>Full Order ID</span>
          <span className="font-mono text-[11px] text-slate-500">
            {currentOrder.id}
          </span>
        </div>
        <div className="flex justify-between text-xs text-slate-600">
          <span>Status</span>
          <span className="font-bold text-orange-600 bg-orange-50 px-2 py-0.5 rounded text-[11px]">
            {currentOrder.status}
          </span>
        </div>
        <div className="pt-2 border-t border-slate-100 flex justify-between text-sm font-extrabold text-slate-900">
          <span>Total Paid</span>
          <span>₹{currentOrder.totalAmount || currentOrder.subtotal || 0}</span>
        </div>
      </div>
    </div>
  );
}

export { OrderTrackingPage };
