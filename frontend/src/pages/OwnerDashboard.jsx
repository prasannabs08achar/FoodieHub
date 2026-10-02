import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { catalogApi, orderApi } from '../services/api';

export default function OwnerDashboard() {
  const { user } = useAuth();
  const [restaurants, setRestaurants] = useState([]);
  const [selectedRestaurant, setSelectedRestaurant] = useState(null);
  const [activeTab, setActiveTab] = useState('orders'); // 'orders' | 'menu' | 'addRestaurant'
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(false);

  // New restaurant form state
  const [newRestName, setNewRestName] = useState('');
  const [newRestCuisine, setNewRestCuisine] = useState('');
  const [newRestAddress, setNewRestAddress] = useState('');
  const [newRestCity, setNewRestCity] = useState('Bengaluru');
  const [newRestDesc, setNewRestDesc] = useState('');
  const [newRestMaxOrders, setNewRestMaxOrders] = useState(30);

  // New menu item form state
  const [isAddItemModalOpen, setIsAddItemModalOpen] = useState(false);
  const [itemName, setItemName] = useState('');
  const [itemPrice, setItemPrice] = useState('');
  const [itemDesc, setItemDesc] = useState('');
  const [itemQty, setItemQty] = useState(50);
  const [itemVeg, setItemVeg] = useState(true);

  const loadOwnerData = async () => {
    if (!user?.userId) return;
    setLoading(true);
    try {
      const list = await catalogApi.getOwnerRestaurants(user.userId);
      setRestaurants(list || []);
      if (list && list.length > 0) {
        setSelectedRestaurant(list[0]);
      } else {
        setSelectedRestaurant(null);
        setActiveTab('addRestaurant');
      }
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  const loadRestaurantOrders = async () => {
    if (!selectedRestaurant) return;
    try {
      const placed = await orderApi.getRestaurantOrders(selectedRestaurant.id, 'PLACED');
      setOrders(placed || []);
    } catch (e) {
      console.error(e);
    }
  };

  useEffect(() => {
    loadOwnerData();
  }, [user?.userId]);

  useEffect(() => {
    if (selectedRestaurant) {
      loadRestaurantOrders();
    }
  }, [selectedRestaurant]);

  const handleCreateRestaurant = async (e) => {
    e.preventDefault();
    if (!newRestName || !newRestCuisine || !newRestAddress) return;
    try {
      const created = await catalogApi.createRestaurant(user.userId, {
        name: newRestName,
        cuisine: newRestCuisine,
        address: newRestAddress,
        city: newRestCity,
        description: newRestDesc,
        maxConcurrentOrders: Number(newRestMaxOrders),
      });
      setRestaurants((prev) => [created, ...prev]);
      setSelectedRestaurant(created);
      setActiveTab('menu');
      setNewRestName('');
      setNewRestCuisine('');
      setNewRestAddress('');
      alert('Restaurant created successfully in catalog-service!');
    } catch (err) {
      alert('Failed to create restaurant: ' + err.message);
    }
  };

  const handleAddDish = async (e) => {
    e.preventDefault();
    if (!itemName || !itemPrice || !selectedRestaurant) return;
    try {
      const newItem = await catalogApi.createMenuItem(selectedRestaurant.id, user.userId, {
        name: itemName,
        description: itemDesc,
        price: Number(itemPrice),
        dailyQuantity: Number(itemQty),
        isVeg: itemVeg,
      });

      setSelectedRestaurant((prev) => ({
        ...prev,
        menu: [...(prev.menu || []), newItem],
      }));

      setIsAddItemModalOpen(false);
      setItemName('');
      setItemPrice('');
      setItemDesc('');
      alert('Menu item added successfully in catalog-service!');
    } catch (err) {
      alert('Failed to add menu item: ' + err.message);
    }
  };

  const handleUpdateOrderStatus = async (orderId, nextStatus) => {
    await orderApi.updateOrderStatus(orderId, user.userId, nextStatus);
    loadRestaurantOrders();
  };

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 py-6 pb-28">
      {/* Header Card */}
      <div className="clean-card p-5 sm:p-6 bg-white mb-6 flex flex-wrap items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-xl font-extrabold text-slate-900 tracking-tight">
              Restaurant Partner Portal
            </h1>
            <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-orange-100 text-orange-800">
              OWNER
            </span>
          </div>
          <p className="text-xs text-slate-500 mt-0.5">
            Manage your restaurants, dishes & live kitchen queue
          </p>
        </div>

        {restaurants.length > 0 && (
          <div className="flex items-center gap-2">
            <span className="text-xs font-semibold text-slate-500">Selected:</span>
            <select
              value={selectedRestaurant?.id || ''}
              onChange={(e) => {
                const found = restaurants.find((r) => r.id === e.target.value);
                if (found) setSelectedRestaurant(found);
              }}
              className="clean-input !py-1.5 !text-xs cursor-pointer font-bold"
            >
              {restaurants.map((r) => (
                <option key={r.id} value={r.id}>
                  {r.name}
                </option>
              ))}
            </select>
          </div>
        )}
      </div>

      {/* Tabs */}
      <div className="flex gap-2 mb-6">
        {[
          { key: 'orders', label: '🍳 Kitchen Queue' },
          { key: 'menu', label: '🍽 Menu Inventory' },
          { key: 'addRestaurant', label: '➕ Add Restaurant' },
        ].map((tab) => (
          <button
            key={tab.key}
            onClick={() => setActiveTab(tab.key)}
            className={`px-4 py-2 rounded-xl text-xs font-bold border transition-colors ${
              activeTab === tab.key
                ? 'bg-orange-500 text-white border-orange-500 shadow-sm'
                : 'bg-white border-slate-200 hover:border-slate-300 text-slate-700'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* Orders Tab */}
      {activeTab === 'orders' && (
        <div className="clean-card p-5 sm:p-6 bg-white">
          <div className="flex items-center justify-between pb-3 border-b border-slate-100 mb-4">
            <h3 className="font-extrabold text-sm text-slate-900">
              Live Kitchen Queue ({selectedRestaurant?.name})
            </h3>
            <button onClick={loadRestaurantOrders} className="btn-secondary text-xs !py-1 !px-2.5">
              🔄 Refresh
            </button>
          </div>

          {orders.length === 0 ? (
            <div className="text-center py-10 text-xs text-slate-400">
              No pending orders for {selectedRestaurant?.name || 'this restaurant'}.
            </div>
          ) : (
            <div className="space-y-3">
              {orders.map((o) => (
                <div
                  key={o.id}
                  className="p-4 rounded-xl border border-slate-100 bg-slate-50/50 flex flex-wrap items-center justify-between gap-3"
                >
                  <div>
                    <div className="font-bold text-xs text-slate-900">Order #{o.id?.slice(0, 8)}</div>
                    <div className="text-[11px] text-slate-500 mt-0.5">
                      Status: <span className="font-bold text-orange-600">{o.status}</span> • Total: ₹{o.totalAmount || o.subtotal || 0}
                    </div>
                  </div>

                  <div className="flex gap-2">
                    {o.status === 'PLACED' && (
                      <button
                        onClick={() => handleUpdateOrderStatus(o.id, 'ACCEPTED')}
                        className="btn-primary text-xs !py-1.5"
                      >
                        Accept Order
                      </button>
                    )}
                    {o.status === 'ACCEPTED' && (
                      <button
                        onClick={() => handleUpdateOrderStatus(o.id, 'PREPARING')}
                        className="btn-primary text-xs !py-1.5"
                      >
                        Start Cooking
                      </button>
                    )}
                    {o.status === 'PREPARING' && (
                      <button
                        onClick={() => handleUpdateOrderStatus(o.id, 'READY_FOR_PICKUP')}
                        className="btn-success text-xs !py-1.5"
                      >
                        Mark Ready for Pickup
                      </button>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Menu Tab */}
      {activeTab === 'menu' && (
        <div className="clean-card p-5 sm:p-6 bg-white">
          <div className="flex items-center justify-between pb-3 border-b border-slate-100 mb-4">
            <h3 className="font-extrabold text-sm text-slate-900">
              Menu Items ({selectedRestaurant?.name})
            </h3>
            <button
              onClick={() => setIsAddItemModalOpen(true)}
              className="btn-primary text-xs !py-1.5"
            >
              + Add New Dish
            </button>
          </div>

          <div className="divide-y divide-slate-100">
            {(selectedRestaurant?.menu || []).map((item) => (
              <div key={item.id} className="py-3 flex items-center justify-between text-xs">
                <div className="flex items-center gap-2">
                  <span className={item.isVeg ? 'veg-badge' : 'nonveg-badge'}></span>
                  <div>
                    <div className="font-bold text-slate-800">{item.name}</div>
                    <div className="text-[11px] text-slate-400">{item.description || 'No description'}</div>
                  </div>
                </div>
                <div className="font-extrabold text-sm text-slate-900">₹{item.price}</div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Add Restaurant Tab */}
      {activeTab === 'addRestaurant' && (
        <div className="clean-card p-5 sm:p-6 bg-white max-w-lg">
          <h3 className="font-extrabold text-sm text-slate-900 mb-4">
            Register New Restaurant
          </h3>
          <form onSubmit={handleCreateRestaurant} className="space-y-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Restaurant Name</label>
              <input required value={newRestName} onChange={e => setNewRestName(e.target.value)} placeholder="e.g. Royal Spice Kitchen" className="clean-input" />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Cuisine Type</label>
              <input required value={newRestCuisine} onChange={e => setNewRestCuisine(e.target.value)} placeholder="e.g. North Indian, Biryani" className="clean-input" />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Address</label>
              <input required value={newRestAddress} onChange={e => setNewRestAddress(e.target.value)} placeholder="Street / Area" className="clean-input" />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">City</label>
              <input value={newRestCity} onChange={e => setNewRestCity(e.target.value)} className="clean-input" />
            </div>
            <button type="submit" className="btn-primary w-full text-xs mt-2">
              Save Restaurant
            </button>
          </form>
        </div>
      )}

      {/* Add Dish Modal */}
      {isAddItemModalOpen && (
        <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm z-50 flex items-center justify-center p-4 animate-fade-in">
          <div className="clean-card max-w-sm w-full p-6 bg-white shadow-2xl animate-scale-up">
            <div className="flex items-center justify-between pb-3 border-b border-slate-100 mb-4">
              <h3 className="font-extrabold text-base text-slate-900">Add Menu Item</h3>
              <button
                onClick={() => setIsAddItemModalOpen(false)}
                className="w-7 h-7 rounded-full bg-slate-100 hover:bg-slate-200 text-slate-600 flex items-center justify-center text-xs font-bold"
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleAddDish} className="space-y-3">
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">Dish Name</label>
                <input required value={itemName} onChange={e => setItemName(e.target.value)} placeholder="e.g. Butter Chicken" className="clean-input" />
              </div>
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">Price (₹)</label>
                <input type="number" required value={itemPrice} onChange={e => setItemPrice(e.target.value)} placeholder="260" className="clean-input" />
              </div>
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">Description</label>
                <input value={itemDesc} onChange={e => setItemDesc(e.target.value)} placeholder="Ingredients or taste notes" className="clean-input" />
              </div>
              <label className="flex items-center gap-2 text-xs font-semibold text-slate-700 cursor-pointer pt-1">
                <input type="checkbox" checked={itemVeg} onChange={e => setItemVeg(e.target.checked)} className="rounded text-orange-500" />
                Pure Vegetarian Dish
              </label>
              <button type="submit" className="btn-primary w-full text-xs mt-2">
                Add to Menu
              </button>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

export { OwnerDashboard };
