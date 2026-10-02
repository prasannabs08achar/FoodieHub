import React, { useState } from 'react';
import { useCart } from '../context/CartContext';
import { useWallet } from '../context/WalletContext';
import { useOrders } from '../context/OrderContext';
import { useAuth } from '../context/AuthContext';

export default function CartDrawer({ isOpen, onClose, onLogin }) {
  const cartCtx = useCart();
  const walletCtx = useWallet();
  const orderCtx = useOrders();
  const authCtx = useAuth();

  const show = isOpen !== undefined ? isOpen : cartCtx?.isCartDrawerOpen;
  const handleClose = onClose || (() => cartCtx?.setIsCartDrawerOpen(false));

  const [couponInput, setCouponInput] = useState('');
  const [couponErr, setCouponErr] = useState('');

  if (!show) return null;

  const {
    items = [], activeRestaurant, updateQuantity, clearCart,
    itemTotal = 0, deliveryFee = 0, gst = 0, platformFee = 0, discount = 0, finalTotal = 0,
    coupon, applyCoupon, removeCoupon,
    pendingItemReplace, confirmReplaceCart, cancelReplaceCart,
  } = cartCtx || {};

  const balance = walletCtx?.balance || 0;
  const placingOrder = orderCtx?.placingOrder;
  const errorMsg = orderCtx?.errorMsg;

  const handleCoupon = (code) => {
    setCouponErr('');
    const res = applyCoupon(code || couponInput);
    if (!res?.success) setCouponErr(res?.message || 'Invalid coupon');
    else setCouponInput('');
  };

  const handleCheckout = async () => {
    if (!authCtx?.user) {
      handleClose();
      if (onLogin) onLogin();
      else authCtx?.setIsAuthModalOpen(true);
      return;
    }
    const result = await orderCtx.placeOrder();
    if (result) handleClose();
  };

  const hasEnough = balance >= finalTotal;

  return (
    <>
      {/* Backdrop */}
      <div
        onClick={handleClose}
        className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm z-50 animate-fade-in"
      />

      {/* Drawer */}
      <div className="fixed right-0 top-0 bottom-0 w-full sm:w-[420px] bg-white z-50 shadow-2xl flex flex-col animate-fade-in">
        {/* Header */}
        <div className="p-4 sm:p-5 border-b border-slate-100 flex items-center justify-between">
          <div>
            <h2 className="font-extrabold text-base text-slate-900">Your Cart</h2>
            {activeRestaurant && (
              <p className="text-xs text-slate-500 truncate max-w-[260px] mt-0.5">
                {activeRestaurant.name}
              </p>
            )}
          </div>
          <button
            onClick={handleClose}
            className="w-8 h-8 rounded-full bg-slate-100 hover:bg-slate-200 text-slate-600 flex items-center justify-center text-sm font-bold transition-colors"
          >
            ✕
          </button>
        </div>

        {items.length === 0 ? (
          <div className="flex-1 flex flex-col items-center justify-center p-8 text-center">
            <div className="text-5xl mb-3">🛒</div>
            <h3 className="font-bold text-base text-slate-800">Your cart is empty</h3>
            <p className="text-xs text-slate-500 mt-1 max-w-xs">
              Explore mouth-watering dishes from top restaurants and add them here!
            </p>
          </div>
        ) : (
          <div className="flex-1 overflow-y-auto p-4 sm:p-5 space-y-5">
            {/* Items */}
            <div className="divide-y divide-slate-100 border-b border-slate-100 pb-2">
              {items.map(item => (
                <div key={item.id} className="py-3 flex items-center justify-between gap-3">
                  <div className="flex items-center gap-2.5 flex-1 min-w-0">
                    <span className={item.isVeg ? 'veg-badge' : 'nonveg-badge'}></span>
                    <div className="truncate">
                      <h4 className="font-bold text-xs sm:text-sm text-slate-900 truncate">
                        {item.name}
                      </h4>
                      <p className="text-xs text-slate-500">₹{item.price}</p>
                    </div>
                  </div>

                  <div className="flex items-center gap-3 flex-shrink-0">
                    <div className="inline-flex items-center border border-emerald-600 rounded-lg overflow-hidden bg-white shadow-sm">
                      <button
                        onClick={() => updateQuantity(item.id, item.quantity - 1)}
                        className="px-2 py-0.5 text-emerald-700 hover:bg-emerald-50 text-xs font-bold"
                      >
                        −
                      </button>
                      <span className="px-2 text-xs font-extrabold text-emerald-700 min-w-[18px] text-center">
                        {item.quantity}
                      </span>
                      <button
                        onClick={() => updateQuantity(item.id, item.quantity + 1)}
                        className="px-2 py-0.5 text-emerald-700 hover:bg-emerald-50 text-xs font-bold"
                      >
                        +
                      </button>
                    </div>
                    <div className="font-extrabold text-xs sm:text-sm text-slate-900 w-14 text-right">
                      ₹{item.price * item.quantity}
                    </div>
                  </div>
                </div>
              ))}

              <div className="pt-2 flex justify-end">
                <button
                  onClick={clearCart}
                  className="text-[11px] font-semibold text-red-600 hover:underline"
                >
                  Clear all items
                </button>
              </div>
            </div>

            {/* Coupons */}
            <div className="bg-slate-50 rounded-xl p-3 border border-slate-200/80">
              <div className="text-xs font-bold text-slate-800 mb-2">🏷 Offers & Coupons</div>
              {coupon ? (
                <div className="flex items-center justify-between bg-emerald-50 border border-emerald-200 px-3 py-2 rounded-lg">
                  <div className="text-xs text-emerald-800 font-bold">
                    ✓ "{coupon.code}" Applied (-₹{discount})
                  </div>
                  <button
                    onClick={removeCoupon}
                    className="text-xs text-red-600 font-bold hover:underline"
                  >
                    Remove
                  </button>
                </div>
              ) : (
                <div>
                  <div className="flex gap-2">
                    <input
                      value={couponInput}
                      onChange={e => setCouponInput(e.target.value)}
                      placeholder="Enter coupon (e.g. WELCOME50)"
                      className="flex-1 px-3 py-1.5 bg-white border border-slate-200 rounded-lg text-xs outline-none focus:border-orange-500"
                    />
                    <button
                      onClick={() => handleCoupon()}
                      className="px-3 py-1.5 bg-orange-500 hover:bg-orange-600 text-white text-xs font-bold rounded-lg transition-colors"
                    >
                      Apply
                    </button>
                  </div>
                  {couponErr && (
                    <div className="text-[11px] text-red-600 mt-1 font-medium">{couponErr}</div>
                  )}
                </div>
              )}
            </div>

            {/* Bill Summary */}
            <div className="bg-slate-50 rounded-xl p-3.5 border border-slate-200/80 space-y-2">
              <div className="text-xs font-extrabold text-slate-700 uppercase tracking-wider mb-1">
                Bill Summary
              </div>
              <div className="flex justify-between text-xs text-slate-600">
                <span>Item Total</span>
                <span className="font-semibold text-slate-800">₹{itemTotal}</span>
              </div>
              <div className="flex justify-between text-xs text-slate-600">
                <span>Delivery Fee</span>
                <span className="font-semibold text-slate-800">
                  {deliveryFee === 0 ? <span className="text-emerald-600 font-bold">FREE</span> : `₹${deliveryFee}`}
                </span>
              </div>
              <div className="flex justify-between text-xs text-slate-600">
                <span>Platform Fee</span>
                <span className="font-semibold text-slate-800">₹{platformFee}</span>
              </div>
              <div className="flex justify-between text-xs text-slate-600">
                <span>GST & Taxes (5%)</span>
                <span className="font-semibold text-slate-800">₹{gst}</span>
              </div>
              {discount > 0 && (
                <div className="flex justify-between text-xs text-emerald-700 font-semibold">
                  <span>Coupon Discount</span>
                  <span>-₹{discount}</span>
                </div>
              )}
              <div className="border-t border-slate-200 pt-2 flex justify-between text-sm font-extrabold text-slate-900">
                <span>To Pay</span>
                <span>₹{finalTotal}</span>
              </div>
            </div>

            {/* Wallet Info Banner */}
            <div className="bg-emerald-50/70 border border-emerald-200 rounded-xl p-3 flex items-center justify-between">
              <div>
                <div className="text-[11px] font-bold text-emerald-800">FoodieHub Wallet</div>
                <div className="text-xs font-extrabold text-emerald-900 mt-0.5">
                  Available: ₹{balance.toFixed(2)}
                </div>
              </div>
              {!hasEnough && (
                <button
                  onClick={() => walletCtx?.setIsWalletModalOpen(true)}
                  className="px-2.5 py-1 bg-emerald-700 hover:bg-emerald-800 text-white text-xs font-bold rounded-lg transition-colors"
                >
                  + Add ₹{(finalTotal - balance).toFixed(0)}
                </button>
              )}
            </div>

            {errorMsg && (
              <div className="p-3 bg-red-50 border border-red-200 rounded-xl text-xs text-red-700 font-medium">
                {errorMsg}
              </div>
            )}
          </div>
        )}

        {/* Checkout Button */}
        {items.length > 0 && (
          <div className="p-4 border-t border-slate-100 bg-white">
            <button
              onClick={handleCheckout}
              disabled={placingOrder}
              className={`w-full py-3 rounded-xl font-bold text-sm text-white shadow-sm transition-all flex items-center justify-center gap-2 ${
                hasEnough
                  ? 'bg-emerald-600 hover:bg-emerald-700 active:bg-emerald-800'
                  : 'bg-orange-500 hover:bg-orange-600 active:bg-orange-700'
              }`}
            >
              {placingOrder ? (
                <span>Placing order...</span>
              ) : hasEnough ? (
                <>
                  <span>Pay ₹{finalTotal} with Wallet</span>
                  <span>→</span>
                </>
              ) : (
                <span>Top up ₹{(finalTotal - balance).toFixed(0)} to Place Order</span>
              )}
            </button>
          </div>
        )}
      </div>

      {/* Multi-restaurant Replace Modal */}
      {pendingItemReplace && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm z-[100] flex items-center justify-center p-4">
          <div className="clean-card max-w-sm w-full p-5 bg-white space-y-3">
            <h3 className="font-extrabold text-base text-slate-900">Replace cart items?</h3>
            <p className="text-xs text-slate-600 leading-relaxed">
              Your cart has items from <strong>{activeRestaurant?.name}</strong>. Starting a new cart from{' '}
              <strong>{pendingItemReplace.restaurant.name}</strong> will discard existing items.
            </p>
            <div className="flex gap-2 pt-2">
              <button
                onClick={cancelReplaceCart}
                className="btn-secondary flex-1 text-xs"
              >
                Keep Old Cart
              </button>
              <button
                onClick={confirmReplaceCart}
                className="btn-primary flex-1 text-xs"
              >
                Start New Cart
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}

export { CartDrawer };
