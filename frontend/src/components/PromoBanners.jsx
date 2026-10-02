import React from 'react';
import { useCart } from '../context/CartContext';

const OFFERS = [
  { code: 'WELCOME50', label: 'Welcome 50%', desc: '50% off up to ₹100 on your meal', tag: 'Special', bg: 'from-orange-500 to-amber-500' },
  { code: 'SWIGGYONE', label: 'Flat ₹150 Off', desc: 'Orders above ₹299 + free delivery', tag: 'Popular', bg: 'from-emerald-600 to-teal-600' },
  { code: 'FEAST20', label: 'Party Feast 20%', desc: 'Flat 20% off on all party orders', tag: 'Trending', bg: 'from-blue-600 to-indigo-600' },
];

export default function PromoBanners() {
  const { applyCoupon } = useCart();

  return (
    <div className="mb-6">
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
        {OFFERS.map((o) => (
          <div
            key={o.code}
            className="clean-card p-3.5 flex items-center justify-between gap-3 bg-white"
          >
            <div>
              <div className="flex items-center gap-1.5">
                <span className="font-bold text-xs text-slate-900">{o.label}</span>
                <span className="text-[10px] font-bold px-1.5 py-0.2 rounded bg-orange-100 text-orange-800">
                  {o.tag}
                </span>
              </div>
              <p className="text-[11px] text-slate-500 mt-0.5 line-clamp-1">{o.desc}</p>
              <div className="font-mono text-[10px] font-bold text-slate-600 tracking-wider mt-1.5">
                CODE: {o.code}
              </div>
            </div>

            <button
              onClick={() => applyCoupon(o.code)}
              className="px-3 py-1.5 rounded-lg border border-orange-500 text-orange-600 hover:bg-orange-50 text-xs font-bold transition-colors flex-shrink-0"
            >
              Apply
            </button>
          </div>
        ))}
      </div>
    </div>
  );
}

export { PromoBanners };
