import React from 'react';

export default function RestaurantCard({ restaurant, onClick }) {
  const imgSrc = restaurant.imageUrl || restaurant.image || 'https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=500&auto=format&fit=crop&q=80';
  const offer = restaurant.offerText || restaurant.discount;

  return (
    <div
      onClick={onClick}
      className="group bg-white rounded-2xl border border-slate-200/80 overflow-hidden shadow-sm hover:shadow-md hover:border-slate-300 transition-all duration-200 cursor-pointer flex flex-col"
    >
      {/* Image container */}
      <div className="relative aspect-[16/10] w-full overflow-hidden bg-slate-100">
        <img
          src={imgSrc}
          alt={restaurant.name}
          className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
          onError={(e) => { e.target.style.display = 'none'; }}
        />
        {offer && (
          <div className="absolute bottom-2 left-2 px-2.5 py-1 rounded-md bg-slate-900/80 backdrop-blur-sm text-white text-[11px] font-extrabold uppercase tracking-wide">
            🏷 {offer}
          </div>
        )}
      </div>

      {/* Info container */}
      <div className="p-3.5 flex-1 flex flex-col justify-between">
        <div>
          <h3 className="font-bold text-sm text-slate-900 group-hover:text-orange-600 transition-colors truncate">
            {restaurant.name}
          </h3>
          <p className="text-xs text-slate-500 truncate mt-0.5">
            {restaurant.cuisineType || restaurant.cuisine}
          </p>
        </div>

        <div className="flex items-center justify-between text-xs text-slate-600 pt-3 mt-2 border-t border-slate-100">
          <span className="inline-flex items-center gap-1 font-bold px-1.5 py-0.5 rounded bg-emerald-50 text-emerald-700">
            ★ {restaurant.rating || 4.2}
          </span>
          <span className="text-slate-500 font-medium">
            🕐 {restaurant.deliveryTime || '25-35 min'}
          </span>
          <span className="text-slate-500 font-medium">
            ₹{restaurant.avgCostForTwo || restaurant.costForTwo || 300} for two
          </span>
        </div>
      </div>
    </div>
  );
}

export { RestaurantCard };
