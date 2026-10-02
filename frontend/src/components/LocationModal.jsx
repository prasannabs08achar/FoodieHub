import React, { useState } from 'react';
import { useLocation } from '../context/LocationContext';

export default function LocationModal({ isOpen, onClose, selectedLocation, onSelect }) {
  const locCtx = useLocation();
  const show = isOpen !== undefined ? isOpen : locCtx?.isLocationModalOpen;
  const handleClose = onClose || (() => locCtx?.setIsLocationModalOpen(false));
  const activeLoc = selectedLocation || locCtx?.currentLocation;
  const handleSelect = onSelect || ((loc) => locCtx?.setLocation(loc));
  const [search, setSearch] = useState('');

  const LOCATIONS = locCtx?.presetLocations || [
    { label: 'Koramangala', address: 'Koramangala 4th Block, Bengaluru', lat: 12.9352, lng: 77.6245 },
    { label: 'Indiranagar', address: '100ft Road, Indiranagar, Bengaluru', lat: 12.9719, lng: 77.6412 },
    { label: 'HSR Layout', address: 'Sector 3, HSR Layout, Bengaluru', lat: 12.9116, lng: 77.6389 },
    { label: 'Whitefield', address: 'ITPL Main Road, Whitefield, Bengaluru', lat: 12.9698, lng: 77.7499 },
    { label: 'Jayanagar', address: '4th Block, Jayanagar, Bengaluru', lat: 12.9308, lng: 77.5836 },
    { label: 'MG Road', address: 'Church Street, MG Road, Bengaluru', lat: 12.9756, lng: 77.6067 },
  ];

  if (!show) return null;

  const filtered = LOCATIONS.filter(l =>
    (l.label || l.name || '').toLowerCase().includes(search.toLowerCase()) ||
    (l.address || '').toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm z-50 flex items-center justify-center p-4 animate-fade-in">
      <div
        onClick={(e) => e.stopPropagation()}
        className="clean-card max-w-sm w-full p-6 bg-white shadow-2xl animate-scale-up"
      >
        <div className="flex items-center justify-between pb-3 border-b border-slate-100 mb-4">
          <h3 className="font-extrabold text-base text-slate-900">Select Location</h3>
          <button
            onClick={handleClose}
            className="w-7 h-7 rounded-full bg-slate-100 hover:bg-slate-200 text-slate-600 flex items-center justify-center text-xs font-bold transition-colors"
          >
            ✕
          </button>
        </div>

        <div className="mb-4">
          <input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search address or area..."
            className="clean-input"
          />
        </div>

        <div className="text-[11px] font-extrabold uppercase tracking-wider text-slate-400 mb-2">
          Popular Bengaluru Hubs
        </div>

        <div className="space-y-1.5 max-h-64 overflow-y-auto pr-1">
          {filtered.map((loc) => {
            const locName = loc.label || loc.name;
            const isSelected = activeLoc?.label === locName || activeLoc?.name === locName;
            return (
              <button
                key={locName}
                onClick={() => {
                  handleSelect({ label: locName, address: loc.address || locName, lat: loc.lat, lng: loc.lng });
                  handleClose();
                }}
                className={`w-full text-left p-3 rounded-xl border flex items-center gap-3 transition-colors ${
                  isSelected
                    ? 'bg-orange-50/80 border-orange-400 text-orange-900'
                    : 'bg-white border-slate-100 hover:border-slate-200 hover:bg-slate-50 text-slate-700'
                }`}
              >
                <span className="text-base">📍</span>
                <div className="truncate">
                  <div className="font-bold text-xs truncate">{locName}</div>
                  <div className="text-[11px] text-slate-500 truncate">{loc.address}</div>
                </div>
              </button>
            );
          })}
        </div>
      </div>
    </div>
  );
}

export { LocationModal };
