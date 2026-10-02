import React from 'react';

const CATEGORIES = [
  { emoji: '🍕', label: 'Pizza' },
  { emoji: '🍔', label: 'Burgers' },
  { emoji: '🍜', label: 'Noodles' },
  { emoji: '🌮', label: 'Wraps' },
  { emoji: '🍱', label: 'Biryani' },
  { emoji: '🥗', label: 'Salads' },
  { emoji: '🧁', label: 'Desserts' },
  { emoji: '🥤', label: 'Drinks' },
  { emoji: '🍲', label: 'Curries' },
  { emoji: '🥞', label: 'Breakfast' },
];

export default function CategoryPills({ selected, onSelect }) {
  return (
    <div className="mb-6">
      <h3 className="text-base font-bold text-slate-900 mb-3">
        What's on your mind?
      </h3>
      <div className="flex gap-2.5 overflow-x-auto pb-2 scrollbar-none -mx-4 px-4 sm:mx-0 sm:px-0">
        {CATEGORIES.map((cat) => {
          const isSelected = selected === cat.label;
          return (
            <button
              key={cat.label}
              onClick={() => onSelect(isSelected ? null : cat.label)}
              className={`flex flex-col items-center justify-center min-w-[70px] sm:min-w-[78px] py-2.5 px-2 rounded-2xl border transition-all duration-150 flex-shrink-0 ${
                isSelected
                  ? 'bg-orange-50 border-orange-500 text-orange-600 shadow-sm'
                  : 'bg-white border-slate-200/80 hover:border-slate-300 text-slate-700 shadow-sm'
              }`}
            >
              <span className="text-2xl mb-1 transform hover:scale-110 transition-transform">
                {cat.emoji}
              </span>
              <span className="text-[11px] font-semibold tracking-tight">
                {cat.label}
              </span>
            </button>
          );
        })}
      </div>
    </div>
  );
}

export { CategoryPills };
