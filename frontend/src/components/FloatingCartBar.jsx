import React from 'react';
import { useCart } from '../context/CartContext';

export default function FloatingCartBar({ onClick }) {
  const { totalItems, finalTotal, activeRestaurant, setIsCartDrawerOpen } = useCart();

  if (!totalItems) return null;

  const handleClick = onClick || (() => setIsCartDrawerOpen(true));

  return (
    <div className="fixed bottom-5 inset-x-0 mx-auto max-w-lg px-4 z-40">
      <button
        onClick={handleClick}
        className="w-full bg-emerald-600 hover:bg-emerald-700 text-white rounded-2xl p-3.5 px-5 shadow-xl flex items-center justify-between transition-all duration-200 transform hover:-translate-y-0.5"
      >
        <div className="flex items-center gap-2.5">
          <span className="w-6 h-6 rounded-full bg-white/20 text-white font-extrabold text-xs flex items-center justify-center">
            {totalItems}
          </span>
          <div className="text-left">
            <div className="text-xs font-bold leading-tight">
              {totalItems === 1 ? '1 item' : `${totalItems} items`} added
            </div>
            {activeRestaurant && (
              <div className="text-[10px] text-emerald-100 truncate max-w-[200px]">
                {activeRestaurant.name}
              </div>
            )}
          </div>
        </div>

        <div className="flex items-center gap-2 font-extrabold text-sm">
          <span>View Cart • ₹{finalTotal}</span>
          <span className="text-base">→</span>
        </div>
      </button>
    </div>
  );
}

export { FloatingCartBar };
