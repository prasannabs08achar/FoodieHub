import React, { useState, useEffect } from 'react';
import { api } from '../services/api';
import { MOCK_RESTAURANTS } from '../services/mockData';
import RestaurantCard from '../components/RestaurantCard';
import CategoryPills from '../components/CategoryPills';
import PromoBanners from '../components/PromoBanners';
import FilterBar from '../components/FilterBar';
import { useLocation } from '../context/LocationContext';

export default function HomePage({ onSelectRestaurant, setSelectedRestaurant, setCurrentView, selectedLocation }) {
  const locCtx = useLocation();
  const activeLocation = selectedLocation || locCtx?.currentLocation;

  const [restaurants, setRestaurants] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [category, setCategory] = useState(null);
  const [filters, setFilters] = useState({ fastDelivery: false, topRated: false, pureVeg: false, offers: false, sort: '' });

  useEffect(() => {
    api.getRestaurants()
      .then(data => setRestaurants(data?.length ? data : MOCK_RESTAURANTS))
      .catch(() => setRestaurants(MOCK_RESTAURANTS))
      .finally(() => setLoading(false));
  }, []);

  const handleSelect = (r) => {
    if (onSelectRestaurant) onSelectRestaurant(r);
    else if (setSelectedRestaurant) {
      setSelectedRestaurant(r);
      if (setCurrentView) setCurrentView('restaurant');
    }
  };

  let list = restaurants;
  if (search) list = list.filter(r => r.name?.toLowerCase().includes(search.toLowerCase()) || (r.cuisineType || r.cuisine)?.toLowerCase().includes(search.toLowerCase()));
  if (category) list = list.filter(r => (r.cuisineType || r.cuisine)?.toLowerCase().includes(category.toLowerCase()) || r.category?.toLowerCase().includes(category.toLowerCase()));
  if (filters.topRated) list = list.filter(r => (r.rating || 0) >= 4);
  if (filters.fastDelivery) list = list.filter(r => (parseInt(r.deliveryTime) || 35) <= 30);
  if (filters.pureVeg) list = list.filter(r => r.isVeg || r.vegOnly);
  if (filters.offers) list = list.filter(r => r.offerText || r.discount);
  if (filters.sort === 'rating') list = [...list].sort((a, b) => (b.rating || 0) - (a.rating || 0));
  if (filters.sort === 'delivery') list = [...list].sort((a, b) => (parseInt(a.deliveryTime) || 30) - (parseInt(b.deliveryTime) || 30));
  if (filters.sort === 'price_low') list = [...list].sort((a, b) => (a.avgCostForTwo || a.costForTwo || 300) - (b.avgCostForTwo || b.costForTwo || 300));
  if (filters.sort === 'price_high') list = [...list].sort((a, b) => (b.avgCostForTwo || b.costForTwo || 300) - (a.avgCostForTwo || a.costForTwo || 300));

  return (
    <div className="max-w-6xl mx-auto px-4 sm:px-6 py-6 pb-28">
      {/* Hero Search Section */}
      <div className="mb-8 bg-gradient-to-r from-orange-50 via-amber-50/50 to-white rounded-3xl p-6 sm:p-8 border border-orange-100/70">
        <div className="max-w-2xl">
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-orange-100 text-orange-800 text-xs font-bold mb-3">
            <span>✨</span> Fresh & Delicious Meals Delivered
          </div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight leading-tight">
            Order food online in{' '}
            <span className="text-orange-500 underline decoration-orange-300 decoration-2 underline-offset-4">
              {activeLocation?.label || activeLocation?.name || 'Bengaluru'}
            </span>
          </h1>
          <p className="text-xs sm:text-sm text-slate-600 mt-2">
            Top restaurants, live kitchen tracking, and instant 1-click digital wallet payments.
          </p>

          <div className="mt-5 relative max-w-md">
            <span className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
              🔍
            </span>
            <input
              value={search}
              onChange={e => setSearch(e.target.value)}
              placeholder="Search for restaurants, biryani, pizza, burgers..."
              className="w-full pl-10 pr-4 py-3 bg-white border border-slate-200 hover:border-slate-300 focus:border-orange-500 focus:ring-2 focus:ring-orange-500/20 rounded-2xl text-xs sm:text-sm shadow-sm transition-all outline-none"
            />
          </div>
        </div>
      </div>

      <PromoBanners />
      <CategoryPills selected={category} onSelect={setCategory} />
      <FilterBar filters={filters} setFilters={setFilters} />

      {/* Grid header */}
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-lg font-bold text-slate-900">
          Restaurants near you
          <span className="ml-2 text-xs font-semibold px-2 py-0.5 rounded-full bg-slate-100 text-slate-600">
            {list.length}
          </span>
        </h2>
        {category && (
          <button
            onClick={() => setCategory(null)}
            className="text-xs text-orange-600 font-semibold hover:underline"
          >
            Clear "{category}" filter ✕
          </button>
        )}
      </div>

      {/* Restaurant Grid */}
      {loading ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5">
          {[1, 2, 3, 4, 5, 6, 7, 8].map(i => (
            <div key={i} className="clean-card h-64 bg-slate-100 animate-pulse rounded-2xl" />
          ))}
        </div>
      ) : list.length === 0 ? (
        <div className="clean-card p-12 text-center bg-white">
          <div className="text-4xl mb-2">🍽</div>
          <h3 className="text-base font-bold text-slate-800">No restaurants found</h3>
          <p className="text-xs text-slate-500 mt-1 max-w-sm mx-auto">
            Try adjusting your search query or reset your active filters.
          </p>
          <button
            onClick={() => {
              setSearch('');
              setCategory(null);
              setFilters({ fastDelivery: false, topRated: false, pureVeg: false, offers: false, sort: '' });
            }}
            className="btn-primary mt-4 text-xs"
          >
            Reset All Filters
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5">
          {list.map(r => (
            <RestaurantCard
              key={r.id}
              restaurant={r}
              onClick={() => handleSelect(r)}
            />
          ))}
        </div>
      )}
    </div>
  );
}

export { HomePage };
