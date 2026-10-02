import React from 'react';

const SORT_OPTIONS = [
  { value: '', label: 'Relevance' },
  { value: 'rating', label: 'Top Rated' },
  { value: 'delivery', label: 'Fastest Delivery' },
  { value: 'price_low', label: 'Cost: Low to High' },
  { value: 'price_high', label: 'Cost: High to Low' },
];

export default function FilterBar({ filters, setFilters }) {
  const toggleFilter = (key) => setFilters(prev => ({ ...prev, [key]: !prev[key] }));

  const chips = [
    { key: 'fastDelivery', label: '⚡ Fast Delivery (≤30m)' },
    { key: 'topRated', label: '★ Rating 4.0+' },
    { key: 'pureVeg', label: '🌿 Pure Veg' },
    { key: 'offers', label: '🏷 Has Offers' },
  ];

  return (
    <div className="flex items-center justify-between gap-2 flex-wrap mb-6 pb-3 border-b border-slate-200">
      <div className="flex items-center gap-2 flex-wrap">
        {chips.map(c => {
          const active = filters[c.key];
          return (
            <button
              key={c.key}
              onClick={() => toggleFilter(c.key)}
              className={`px-3 py-1.5 rounded-full text-xs font-semibold border transition-all ${
                active
                  ? 'bg-orange-50 border-orange-500 text-orange-600 shadow-sm'
                  : 'bg-white border-slate-200 hover:border-slate-300 text-slate-700'
              }`}
            >
              {c.label}
            </button>
          );
        })}
      </div>

      <div className="flex items-center gap-1.5">
        <span className="text-xs text-slate-400 font-medium">Sort:</span>
        <select
          value={filters.sort || ''}
          onChange={e => setFilters(prev => ({ ...prev, sort: e.target.value }))}
          className="bg-white border border-slate-200 hover:border-slate-300 rounded-lg text-xs font-semibold text-slate-700 px-2.5 py-1.5 focus:outline-none focus:ring-1 focus:ring-orange-500 cursor-pointer"
        >
          {SORT_OPTIONS.map(o => (
            <option key={o.value} value={o.value}>{o.label}</option>
          ))}
        </select>
      </div>
    </div>
  );
}

export { FilterBar };
