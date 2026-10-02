import React, { useState } from 'react';
import { AuthProvider, useAuth } from './context/AuthContext';
import { LocationProvider } from './context/LocationContext';
import { WalletProvider } from './context/WalletContext';
import { CartProvider } from './context/CartContext';
import { OrderProvider, useOrders } from './context/OrderContext';
import { BackendProvider } from './context/BackendContext';

import { Navbar } from './components/Navbar';
import { LocationModal } from './components/LocationModal';
import { AuthModal } from './components/AuthModal';
import { WalletModal } from './components/WalletModal';
import { BackendHealthModal } from './components/BackendHealthModal';
import { CartDrawer } from './components/CartDrawer';
import { FloatingCartBar } from './components/FloatingCartBar';

import { HomePage } from './pages/HomePage';
import { RestaurantDetailPage } from './pages/RestaurantDetailPage';
import { OrderTrackingPage } from './pages/OrderTrackingPage';
import { OrdersHistoryPage } from './pages/OrdersHistoryPage';
import { WalletPage } from './pages/WalletPage';
import { OwnerDashboard } from './pages/OwnerDashboard';
import { DeliveryAgentDashboard } from './pages/DeliveryAgentDashboard';

function MainApp() {
  const [currentView, setCurrentView] = useState('home');
  const [selectedRestaurant, setSelectedRestaurant] = useState(null);
  const { activeTrackingOrder } = useOrders();

  const handleSelectRestaurant = (restaurant) => {
    setSelectedRestaurant(restaurant);
    setCurrentView('restaurant');
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const handleTrackOrder = (order) => {
    setCurrentView('track');
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  return (
    <div className="min-h-screen flex flex-col bg-slate-50 text-slate-800">
      {/* Top Header */}
      <Navbar
        currentView={currentView}
        setCurrentView={setCurrentView}
      />

      {/* Main View Container */}
      <main className="flex-1">
        {currentView === 'home' && (
          <HomePage
            onSelectRestaurant={handleSelectRestaurant}
            setCurrentView={setCurrentView}
            setSelectedRestaurant={setSelectedRestaurant}
          />
        )}

        {currentView === 'restaurant' && selectedRestaurant && (
          <RestaurantDetailPage
            restaurant={selectedRestaurant}
            onBack={() => setCurrentView('home')}
          />
        )}

        {currentView === 'track' && (
          <OrderTrackingPage
            order={activeTrackingOrder}
            onBack={() => setCurrentView('home')}
          />
        )}

        {currentView === 'history' && (
          <OrdersHistoryPage
            onTrackOrder={handleTrackOrder}
            onBackToHome={() => setCurrentView('home')}
          />
        )}

        {currentView === 'wallet' && <WalletPage />}

        {currentView === 'owner' && <OwnerDashboard />}

        {currentView === 'agent' && <DeliveryAgentDashboard />}
      </main>

      {/* Modals & Overlays */}
      <LocationModal />
      <AuthModal />
      <WalletModal />
      <BackendHealthModal />
      <CartDrawer />
      <FloatingCartBar />

      {/* Clean Minimalist Footer */}
      <footer className="bg-white border-t border-slate-200/80 py-8 text-xs text-slate-500">
        <div className="max-w-6xl mx-auto px-4 sm:px-6 flex flex-col sm:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-2">
            <span className="text-base">🍔</span>
            <span className="font-extrabold text-sm text-slate-900 tracking-tight">Foodie<span className="text-orange-500">Hub</span></span>
            <span className="text-slate-300">|</span>
            <span>Clean React frontend for Spring Boot Microservices</span>
          </div>

          <div className="flex items-center gap-4 font-semibold text-slate-600">
            <button onClick={() => setCurrentView('home')} className="hover:text-orange-600">Restaurants</button>
            <button onClick={() => setCurrentView('history')} className="hover:text-orange-600">Orders</button>
            <button onClick={() => setCurrentView('wallet')} className="hover:text-orange-600">Wallet</button>
            <button onClick={() => setCurrentView('owner')} className="hover:text-orange-600">Partner</button>
            <button onClick={() => setCurrentView('agent')} className="hover:text-orange-600">Deliver</button>
          </div>
        </div>
      </footer>
    </div>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <LocationProvider>
        <WalletProvider>
          <CartProvider>
            <OrderProvider>
              <BackendProvider>
                <MainApp />
              </BackendProvider>
            </OrderProvider>
          </CartProvider>
        </WalletProvider>
      </LocationProvider>
    </AuthProvider>
  );
}
