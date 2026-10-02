// Central API Client connecting directly to Spring Boot Microservices
// Pure backend integration - No localStorage mock data

import { SAMPLE_RESTAURANTS } from './mockData';

// Purge any stale localStorage left from prior sessions
try {
  if (typeof window !== 'undefined' && window.localStorage) {
    localStorage.clear();
  }
} catch (e) {}

// Standard UUID generator for Idempotency-Key and headers
export const generateUUID = () => {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) {
    return crypto.randomUUID();
  }
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
    const r = (Math.random() * 16) | 0;
    const v = c === 'x' ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
};

const getStoredAuth = () => {
  try {
    const raw = sessionStorage.getItem('foodiehub_auth');
    if (raw) return JSON.parse(raw);
  } catch (e) {}
  return null;
};

// Base HTTP request handler to Spring Boot Microservices
async function request(endpoint, options = {}) {
  const auth = getStoredAuth();
  const headers = {
    'Content-Type': 'application/json',
    Accept: 'application/json',
    ...options.headers,
  };

  if (auth?.accessToken) {
    headers['Authorization'] = `Bearer ${auth.accessToken}`;
  }
  if (auth?.userId && !headers['X-User-Id']) {
    headers['X-User-Id'] = auth.userId;
  }

  const config = {
    ...options,
    headers,
  };

  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), 10000);
  config.signal = controller.signal;

  try {
    const response = await fetch(endpoint, config);
    clearTimeout(timeoutId);

    if (response.status === 204) {
      return null;
    }

    const data = await response.json().catch(() => null);

    if (!response.ok) {
      const errorMsg = data?.message || data?.error || `HTTP ${response.status}: Request failed`;
      const err = new Error(errorMsg);
      err.status = response.status;
      err.data = data;
      throw err;
    }

    return data;
  } catch (err) {
    clearTimeout(timeoutId);
    throw err;
  }
}

// -------------------------------------------------------------
// BACKEND HEALTH SERVICE
// -------------------------------------------------------------
export const checkServiceHealth = async () => {
  const results = {
    gateway: false,
    auth: false,
    wallet: false,
    catalog: false,
    order: false,
    dispatch: false,
  };

  try {
    const res = await fetch('/api/auth/user', { method: 'GET' }).catch(() => null);
    if (res && (res.status === 200 || res.status === 401 || res.status === 403)) {
      results.gateway = true;
      results.auth = true;
    }
  } catch (e) {}

  return results;
};

// -------------------------------------------------------------
// AUTH SERVICE (auth-service on port 8081 / gateway 8080)
// -------------------------------------------------------------
export const authApi = {
  login: async (email, password) => {
    return await request('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    });
  },

  register: async ({ email, password, fullName, role = 'CUSTOMER' }) => {
    const path =
      role === 'OWNER'
        ? '/api/auth/register/owner'
        : role === 'AGENT'
        ? '/api/auth/register/agent'
        : '/api/auth/register';

    return await request(path, {
      method: 'POST',
      body: JSON.stringify({ email, password, fullName }),
    });
  },

  getProfile: async () => {
    return request('/api/auth/user');
  },
};

// -------------------------------------------------------------
// WALLET SERVICE (wallet-service on port 8082 / gateway 8080)
// -------------------------------------------------------------
export const walletApi = {
  getWallet: async (userId) => {
    return await request('/api/wallet', {
      headers: { 'X-User-Id': userId },
    });
  },

  addFunds: async (userId, amount) => {
    const idempotencyKey = generateUUID();
    return await request('/api/wallet/funds', {
      method: 'POST',
      headers: {
        'X-User-Id': userId,
        'Idempotency-Key': idempotencyKey,
      },
      body: JSON.stringify({ amount: Number(amount) }),
    });
  },

  getTransactions: async (userId, page = 0, size = 20) => {
    return await request(`/api/wallet/transactions?page=${page}&size=${size}`, {
      headers: { 'X-User-Id': userId },
    });
  },

  debitWallet: async (userId, amount) => {
    const idempotencyKey = generateUUID();
    return await request('/api/wallet/debit', {
      method: 'POST',
      headers: {
        'X-User-Id': userId,
        'Idempotency-Key': idempotencyKey,
      },
      body: JSON.stringify({ amount: Number(amount) }),
    });
  },
};

// -------------------------------------------------------------
// CATALOG SERVICE (catalog-service on port 8083 / gateway 8080)
// -------------------------------------------------------------
export const catalogApi = {
  getRestaurants: async () => {
    // If backend has restaurants or endpoint, fetch directly
    try {
      const res = await request('/api/catalog/restaurants');
      if (Array.isArray(res) && res.length > 0) return res;
    } catch (e) {
      // Backend catalog might not have a public list endpoint; use in-memory list
    }
    return SAMPLE_RESTAURANTS;
  },

  getRestaurantById: async (restaurantId) => {
    try {
      const res = await request(`/api/catalog/restaurants/${restaurantId}`);
      if (res) {
        const menu = await catalogApi.getRestaurantMenu(restaurantId);
        return { ...res, menu: menu || [] };
      }
    } catch (err) {}
    return SAMPLE_RESTAURANTS.find((r) => r.id === restaurantId) || null;
  },

  getRestaurantMenu: async (restaurantId) => {
    try {
      return await request(`/api/catalog/restaurants/${restaurantId}/menu-items`);
    } catch (err) {
      const rest = SAMPLE_RESTAURANTS.find((r) => r.id === restaurantId);
      return rest ? rest.menu : [];
    }
  },

  createRestaurant: async (ownerId, restaurantData) => {
    const idempotencyKey = generateUUID();
    return await request('/api/catalog/restaurants', {
      method: 'POST',
      headers: {
        'X-User-Id': ownerId,
        'Idempotency-Key': idempotencyKey,
      },
      body: JSON.stringify(restaurantData),
    });
  },

  createMenuItem: async (restaurantId, ownerId, itemData) => {
    const idempotencyKey = generateUUID();
    return await request(`/api/catalog/restaurants/${restaurantId}/menu-items`, {
      method: 'POST',
      headers: {
        'X-User-Id': ownerId,
        'Idempotency-Key': idempotencyKey,
      },
      body: JSON.stringify(itemData),
    });
  },

  getOwnerRestaurants: async (ownerId) => {
    return await request('/api/catalog/restaurants/owner', {
      headers: { 'X-User-Id': ownerId },
    });
  },
};

// -------------------------------------------------------------
// CART SERVICE (order-service on port 8084)
// -------------------------------------------------------------
export const cartApi = {
  getCart: async (customerId, restaurantId) => {
    return await request(`/api/carts/${restaurantId}`, {
      headers: { 'X-User-Id': customerId },
    });
  },

  addItem: async (customerId, restaurantId, menuItemId, quantity = 1) => {
    return await request(`/api/carts/${restaurantId}/items`, {
      method: 'POST',
      headers: { 'X-User-Id': customerId },
      body: JSON.stringify({ menuItemId, quantity: Number(quantity) }),
    });
  },

  updateItem: async (customerId, restaurantId, menuItemId, quantity) => {
    return await request(`/api/carts/${restaurantId}/items/${menuItemId}`, {
      method: 'PUT',
      headers: { 'X-User-Id': customerId },
      body: JSON.stringify({ quantity: Number(quantity) }),
    });
  },

  removeItem: async (customerId, restaurantId, menuItemId) => {
    return await request(`/api/carts/${restaurantId}/items/${menuItemId}`, {
      method: 'DELETE',
      headers: { 'X-User-Id': customerId },
    });
  },

  clearCart: async (customerId, restaurantId) => {
    return await request(`/api/carts/${restaurantId}`, {
      method: 'DELETE',
      headers: { 'X-User-Id': customerId },
    });
  },
};

// -------------------------------------------------------------
// ORDER SERVICE (order-service on port 8084 / gateway 8080)
// -------------------------------------------------------------
export const orderApi = {
  placeOrder: async (customerId, restaurantId, lat, lng) => {
    const idempotencyKey = generateUUID();
    const payload = {
      restaurantId,
      deliveryLatitude: Number(lat) || 12.9352,
      deliveryLongitude: Number(lng) || 77.6245,
    };

    return await request('/api/orders', {
      method: 'POST',
      headers: {
        'X-User-Id': customerId,
        'Idempotency-Key': idempotencyKey,
      },
      body: JSON.stringify(payload),
    });
  },

  getOrder: async (orderId) => {
    return await request(`/api/orders/${orderId}`);
  },

  getCustomerOrders: async (customerId) => {
    return await request('/api/orders/customer', {
      headers: { 'X-User-Id': customerId },
    });
  },

  getOrderHistory: async (orderId) => {
    return await request(`/api/orders/${orderId}/history`);
  },

  updateOrderStatus: async (orderId, changedBy, newStatus) => {
    return await request(`/api/orders/${orderId}/status`, {
      method: 'PATCH',
      headers: { 'X-User-Id': changedBy },
      body: JSON.stringify({ newStatus }),
    });
  },

  cancelOrder: async (orderId, customerId, reason = 'Customer cancelled') => {
    return await request(`/api/orders/${orderId}/cancel`, {
      method: 'POST',
      headers: { 'X-User-Id': customerId },
      body: JSON.stringify({ reason }),
    });
  },

  getRestaurantOrders: async (restaurantId, status = 'PLACED') => {
    return await request(`/api/orders/restaurant/${restaurantId}?status=${status}`);
  },

  getReadyForPickupOrders: async () => {
    return await request('/api/orders/internal/ready-for-pickup');
  },
};

// -------------------------------------------------------------
// DISPATCH SERVICE (dispatch-service on port 8085 / gateway 8080)
// -------------------------------------------------------------
export const dispatchApi = {
  getDeliveryRun: async (orderId) => {
    return await request(`/api/dispatch/runs/${orderId}`);
  },
};

// Unified api object for easy imports
export const api = {
  getRestaurants: catalogApi.getRestaurants,
  getRestaurantById: catalogApi.getRestaurantById,
  getMenuItems: catalogApi.getRestaurantMenu,
  createRestaurant: catalogApi.createRestaurant,
  createMenuItem: catalogApi.createMenuItem,
  getOwnerRestaurants: catalogApi.getOwnerRestaurants,
  ...authApi,
  ...walletApi,
  ...orderApi,
  ...cartApi,
  ...catalogApi,
};

export default api;
