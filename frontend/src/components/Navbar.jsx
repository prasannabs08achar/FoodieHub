import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { useCart } from '../context/CartContext';
import { useWallet } from '../context/WalletContext';
import { useLocation } from '../context/LocationContext';
import { useBackend } from '../context/BackendContext';

export default function Navbar({ currentView, setCurrentView }) {
  const { user, logout, setIsAuthModalOpen, setAuthModalMode } = useAuth();
  const { totalItems, finalTotal, setIsCartDrawerOpen } = useCart();
  const { balance } = useWallet();
  const { currentLocation, setIsLocationModalOpen } = useLocation();
  const { isConnected, setIsHealthModalOpen } = useBackend();
  const [showMenu, setShowMenu] = useState(false);

  return (
    <header className="sticky top-0 z-40 bg-white/95 backdrop-blur-md border-b border-slate-200 shadow-[0_1px_3px_rgba(0,0,0,0.03)]">
      <div className="max-w-6xl mx-auto px-4 sm:px-6 h-16 flex items-center justify-between gap-4">
        {/* Logo & Location */}
        <div className="flex items-center gap-3 sm:gap-6">
          <div
            onClick={() => setCurrentView('home')}
            className="flex items-center gap-2.5 cursor-pointer group"
          >
            <div className="w-9 h-9 rounded-xl bg-orange-500 flex items-center justify-center text-white text-lg shadow-sm group-hover:scale-105 transition-transform">
              🍔
            </div>
            <div>
              <span className="font-extrabold text-lg text-slate-900 tracking-tight">
                Foodie<span className="text-orange-500">Hub</span>
              </span>
            </div>
          </div>

          {/* Location Selector */}
          <button
            onClick={() => setIsLocationModalOpen(true)}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg border border-slate-200 bg-slate-50 hover:bg-slate-100 hover:border-slate-300 text-xs font-semibold text-slate-700 transition-all max-w-[150px] sm:max-w-xs truncate"
          >
            <span className="text-orange-500">📍</span>
            <span className="truncate">{currentLocation?.label || currentLocation?.name || 'Select Location'}</span>
            <span className="text-[10px] text-slate-400">▼</span>
          </button>
        </div>

        {/* Right Nav Actions */}
        <div className="flex items-center gap-2 sm:gap-3">
          {/* Backend Status indicator */}
          <button
            onClick={() => setIsHealthModalOpen(true)}
            className="hidden md:inline-flex items-center gap-1.5 px-2.5 py-1.5 rounded-lg border border-slate-200 bg-slate-50 hover:bg-slate-100 text-xs font-medium text-slate-600 transition-colors"
            title="Microservices Registry Status"
          >
            <span className={`w-2 h-2 rounded-full ${isConnected ? 'bg-emerald-500 animate-pulse' : 'bg-slate-400'}`}></span>
            <span>Services</span>
          </button>

          {/* Wallet Balance Pill */}
          <button
            onClick={() => setCurrentView('wallet')}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg border border-slate-200 bg-white hover:bg-slate-50 text-xs font-semibold text-slate-700 shadow-sm transition-all"
          >
            <span>💰</span>
            <span>₹{balance.toFixed(0)}</span>
          </button>

          {/* Cart Button */}
          <button
            onClick={() => setIsCartDrawerOpen(true)}
            className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-lg bg-orange-500 hover:bg-orange-600 active:bg-orange-700 text-white font-semibold text-xs shadow-sm transition-all"
          >
            <span>🛒</span>
            <span className="hidden sm:inline">Cart</span>
            {totalItems > 0 && (
              <span className="px-1.5 py-0.2 bg-white text-orange-600 text-[11px] font-bold rounded-full">
                {totalItems}
              </span>
            )}
            {totalItems > 0 && <span className="font-bold border-l border-orange-400 pl-1.5">₹{finalTotal}</span>}
          </button>

          {/* User Account / Sign In */}
          {user ? (
            <div className="relative">
              <button
                onClick={() => setShowMenu(m => !m)}
                className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg border border-slate-200 bg-white hover:bg-slate-50 text-xs font-semibold text-slate-700 shadow-sm transition-all"
              >
                <span>👤</span>
                <span className="hidden sm:inline truncate max-w-[90px]">{user?.fullName?.split(' ')[0] || 'Account'}</span>
                <span className="text-[9px] text-slate-400">▼</span>
              </button>

              {showMenu && (
                <div
                  onMouseLeave={() => setShowMenu(false)}
                  className="absolute right-0 mt-2 w-56 bg-white rounded-xl border border-slate-200 shadow-lg py-1 z-50 animate-scale-up"
                >
                  <div className="px-4 py-2.5 border-b border-slate-100">
                    <div className="font-bold text-xs text-slate-800 truncate">{user?.fullName}</div>
                    <div className="text-[11px] text-slate-500 truncate">{user?.email}</div>
                    <span className="inline-block mt-1 px-2 py-0.5 rounded text-[10px] font-bold bg-orange-100 text-orange-800">
                      {user?.role}
                    </span>
                  </div>

                  <div className="py-1">
                    <button
                      onClick={() => { setCurrentView('history'); setShowMenu(false); }}
                      className="w-full text-left px-4 py-2 text-xs font-medium text-slate-700 hover:bg-slate-50 transition-colors"
                    >
                      📦 Past Orders
                    </button>
                    <button
                      onClick={() => { setCurrentView('wallet'); setShowMenu(false); }}
                      className="w-full text-left px-4 py-2 text-xs font-medium text-slate-700 hover:bg-slate-50 transition-colors"
                    >
                      💰 FoodieHub Wallet
                    </button>

                    {user?.role === 'OWNER' && (
                      <button
                        onClick={() => { setCurrentView('owner'); setShowMenu(false); }}
                        className="w-full text-left px-4 py-2 text-xs font-bold text-orange-600 hover:bg-orange-50 transition-colors"
                      >
                        🏪 Restaurant Partner
                      </button>
                    )}

                    {user?.role === 'AGENT' && (
                      <button
                        onClick={() => { setCurrentView('agent'); setShowMenu(false); }}
                        className="w-full text-left px-4 py-2 text-xs font-bold text-teal-600 hover:bg-teal-50 transition-colors"
                      >
                        🛵 Delivery Partner
                      </button>
                    )}
                  </div>

                  <div className="border-t border-slate-100 py-1">
                    <button
                      onClick={() => { logout(); setShowMenu(false); }}
                      className="w-full text-left px-4 py-1.5 text-xs font-medium text-red-600 hover:bg-red-50 transition-colors"
                    >
                      🚪 Sign Out
                    </button>
                  </div>
                </div>
              )}
            </div>
          ) : (
            <div className="flex items-center gap-1.5">
              <button
                onClick={() => { setAuthModalMode('login'); setIsAuthModalOpen(true); }}
                className="px-3 py-1.5 rounded-lg border border-slate-200 bg-white hover:bg-slate-50 text-xs font-semibold text-slate-700 transition-colors"
              >
                Sign In
              </button>
              <button
                onClick={() => { setAuthModalMode('register'); setIsAuthModalOpen(true); }}
                className="px-3 py-1.5 rounded-lg bg-orange-500 hover:bg-orange-600 text-white text-xs font-semibold shadow-sm transition-colors"
              >
                Sign Up
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}

export { Navbar };
