import React, { useState, useMemo, useEffect } from 'react';
import { useCart } from '../context/CartContext';
import { api } from '../services/api';

export default function RestaurantDetailPage({ restaurant, onBack }) {
  const { items, addItem, updateQuantity } = useCart();
  const [dishSearch, setDishSearch] = useState('');
  const [vegOnly, setVegOnly] = useState(false);
  const [menu, setMenu] = useState(restaurant.menu || []);

  useEffect(() => {
    if (!restaurant.menu?.length) {
      api.getMenuItems(restaurant.id)
        .then(data => { if (data?.length) setMenu(data); })
        .catch(() => {});
    }
  }, [restaurant.id]);

  const categories = useMemo(() => {
    const map = {};
    menu.forEach(item => {
      if (dishSearch && !item.name.toLowerCase().includes(dishSearch.toLowerCase())) return;
      if (vegOnly && !item.isVeg) return;
      const cat = item.category || 'Specialties';
      if (!map[cat]) map[cat] = [];
      map[cat].push(item);
    });
    return map;
  }, [menu, dishSearch, vegOnly]);

  const getQty = (itemId) => items.find(i => i.id === itemId)?.quantity || 0;

  const imgSrc = restaurant.imageUrl || restaurant.image || 'https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=800&auto=format&fit=crop&q=80';

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 py-6 pb-28">
      {/* Back button */}
      <button
        onClick={onBack}
        className="inline-flex items-center gap-1.5 text-xs font-semibold text-slate-600 hover:text-slate-900 mb-4 transition-colors group"
      >
        <span className="group-hover:-translate-x-0.5 transition-transform">←</span>
        <span>Back to Restaurants</span>
      </button>

      {/* Restaurant Overview Card */}
      <div className="clean-card overflow-hidden mb-6 bg-white">
        <div className="relative h-44 sm:h-56 w-full bg-slate-100 overflow-hidden">
          <img
            src={imgSrc}
            alt={restaurant.name}
            className="w-full h-full object-cover"
            onError={e => { e.target.style.display = 'none'; }}
          />
          <div className="absolute inset-0 bg-gradient-to-t from-slate-950/80 via-slate-950/30 to-transparent flex flex-col justify-end p-5 sm:p-6 text-white">
            <h1 className="text-xl sm:text-2xl font-extrabold tracking-tight">
              {restaurant.name}
            </h1>
            <p className="text-xs sm:text-sm text-slate-200 mt-1">
              {restaurant.cuisineType || restaurant.cuisine} • {restaurant.address || restaurant.city || 'Bangalore'}
            </p>
          </div>
        </div>

        <div className="p-4 sm:p-5 flex flex-wrap items-center justify-between gap-4 border-t border-slate-100">
          <div className="flex items-center gap-4 text-xs sm:text-sm font-semibold text-slate-700">
            <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg bg-emerald-50 text-emerald-700 font-bold">
              ★ {restaurant.rating || 4.2}
            </span>
            <span className="text-slate-300">|</span>
            <span>🕐 {restaurant.deliveryTime || '30-40 min'}</span>
            <span className="text-slate-300">|</span>
            <span>₹{restaurant.avgCostForTwo || restaurant.costForTwo || 300} for two</span>
          </div>

          {(restaurant.offerText || restaurant.discount) && (
            <span className="text-xs font-extrabold px-2.5 py-1 rounded-lg bg-orange-100 text-orange-800">
              🏷 {restaurant.offerText || restaurant.discount}
            </span>
          )}
        </div>
      </div>

      {/* Dish Search & Veg Filter */}
      <div className="flex items-center justify-between gap-3 mb-6">
        <div className="relative flex-1 max-w-sm">
          <span className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-400 text-xs">
            🔍
          </span>
          <input
            value={dishSearch}
            onChange={e => setDishSearch(e.target.value)}
            placeholder="Search in menu..."
            className="w-full pl-9 pr-3 py-2 bg-white border border-slate-200 hover:border-slate-300 focus:border-orange-500 focus:ring-1 focus:ring-orange-500 rounded-xl text-xs text-slate-800 placeholder-slate-400 outline-none transition-all"
          />
        </div>

        <button
          onClick={() => setVegOnly(v => !v)}
          className={`inline-flex items-center gap-1.5 px-3 py-2 rounded-xl text-xs font-bold border transition-colors ${
            vegOnly
              ? 'bg-emerald-50 border-emerald-500 text-emerald-700'
              : 'bg-white border-slate-200 hover:border-slate-300 text-slate-700'
          }`}
        >
          <span className="veg-badge"></span>
          <span>Veg Only</span>
        </button>
      </div>

      {/* Menu Sections */}
      {Object.keys(categories).length === 0 ? (
        <div className="clean-card p-10 text-center bg-white">
          <div className="text-3xl mb-2">🍽</div>
          <h3 className="font-bold text-sm text-slate-800">No dishes found</h3>
          <p className="text-xs text-slate-500 mt-1">Try modifying your search or toggling off veg filter.</p>
        </div>
      ) : (
        <div className="space-y-6">
          {Object.entries(categories).map(([catName, catItems]) => (
            <div key={catName} className="clean-card p-5 bg-white">
              <h2 className="text-sm sm:text-base font-extrabold text-slate-900 pb-3 border-b border-slate-100 flex items-center justify-between">
                <span>{catName}</span>
                <span className="text-xs text-slate-400 font-normal">({catItems.length})</span>
              </h2>

              <div className="divide-y divide-slate-100">
                {catItems.map(item => {
                  const qty = getQty(item.id);
                  const itemImg = item.imageUrl || item.image;
                  return (
                    <div
                      key={item.id}
                      className="py-4 flex items-center justify-between gap-4"
                    >
                      <div className="flex-1 pr-2">
                        <div className="flex items-center gap-2 mb-1">
                          <span className={item.isVeg ? 'veg-badge' : 'nonveg-badge'}></span>
                          {item.isBestseller && (
                            <span className="text-[10px] font-extrabold uppercase px-1.5 py-0.5 rounded bg-amber-100 text-amber-800">
                              ★ Bestseller
                            </span>
                          )}
                        </div>
                        <h3 className="font-bold text-sm text-slate-900">{item.name}</h3>
                        <p className="font-extrabold text-sm text-slate-800 mt-0.5">₹{item.price}</p>
                        {item.description && (
                          <p className="text-xs text-slate-500 line-clamp-2 mt-1 leading-relaxed">
                            {item.description}
                          </p>
                        )}
                      </div>

                      <div className="flex flex-col items-center gap-2 flex-shrink-0">
                        {itemImg && (
                          <img
                            src={itemImg}
                            alt={item.name}
                            className="w-20 h-16 sm:w-24 sm:h-20 object-cover rounded-xl border border-slate-100"
                            onError={e => { e.target.style.display = 'none'; }}
                          />
                        )}

                        {qty === 0 ? (
                          <button
                            onClick={() => addItem(item, restaurant)}
                            className="px-5 py-1.5 rounded-lg border-2 border-slate-200 bg-white hover:border-emerald-600 hover:text-emerald-700 text-emerald-600 text-xs font-bold transition-all shadow-sm active:scale-95"
                          >
                            ADD
                          </button>
                        ) : (
                          <div className="inline-flex items-center border-2 border-emerald-600 rounded-lg overflow-hidden bg-white shadow-sm">
                            <button
                              onClick={() => updateQuantity(item.id, qty - 1)}
                              className="px-2.5 py-1 text-emerald-700 hover:bg-emerald-50 text-xs font-bold transition-colors"
                            >
                              −
                            </button>
                            <span className="px-2 text-xs font-extrabold text-emerald-700 min-w-[20px] text-center">
                              {qty}
                            </span>
                            <button
                              onClick={() => updateQuantity(item.id, qty + 1)}
                              className="px-2.5 py-1 text-emerald-700 hover:bg-emerald-50 text-xs font-bold transition-colors"
                            >
                              +
                            </button>
                          </div>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

export { RestaurantDetailPage };
