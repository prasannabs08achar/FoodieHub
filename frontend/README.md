# FoodieHub Frontend 

A modern, high-performance React & JavaScript frontend replicating the **Swiggy** and **Zomato** experience, integrated with the Java Spring Boot microservices backend ecosystem.

> **Note:** As per instructions, zero modifications have been made to any Java Spring Boot microservices code or configuration.

---

## 🚀 Quick Start

### 1. Start the Frontend
From the `frontend` folder:
```bash
npm run dev
```
Open **[http://localhost:3000](http://localhost:3000)** in your browser.

### 2. Build for Production
```bash
npm run build
```

---

## 🍽️ Key Features (Swiggy & Zomato Experience)

1. **Brand Aesthetics & UI Design**:
   - Signature Swiggy Orange (`#fc8019`) and Zomato Crimson accents.
   - Typography powered by *Plus Jakarta Sans* and *Outfit*.
   - Veg (green square & dot) / Non-Veg (red square & triangle) indicators on every dish.
   - Animated dish counters (`- [qty] +`), rating badges (`★ 4.5`), and delivery ETA pills.

2. **Customer Food Discovery**:
   - **"What's on your mind?"** category avatar carousel (Biryani, Pizzas, Burgers, North Indian, South Indian, Chinese, Desserts, Rolls).
   - **Curated Promotional Banners** (`WELCOME50`, `SWIGGYONE`, `FEAST20`).
   - **Filter & Sort Bar**: Fast delivery (<25 mins), Rating 4.0+, Pure Veg toggle, Active offers, Price low-to-high/high-to-low.
   - **Real-time Global Search**: Search across restaurant names, cuisines, or individual dishes.

3. **Location & GPS Selector**:
   - Interactive modal with preset Bangalore delivery hubs (Koramangala, Indiranagar, HSR Layout, Whitefield, JP Nagar, MG Road) or custom address.
   - Computes delivery GPS coordinates (`lat`, `lng`) required by `order-service`'s `PlaceOrderRequest`.

4. **Cart & Checkout Drawer**:
   - **Swiggy Multi-Restaurant Rule**: If adding an item from a different restaurant, prompts "Replace cart items?" modal.
   - Interactive coupon codes with instant bill discounts.
   - Itemized bill breakdown: Item Total, Delivery Partner Fee, Platform Fee (₹5), GST & Taxes (5%), Coupon Savings, and Final Total.
   - **FoodieHub Digital Wallet Checkout**: Real-time balance check with instant 1-click order payment. If balance is insufficient, provides a seamless top-up modal.

5. **Live Order Tracking**:
   - Visual timeline state machine matching backend `OrderStatus`:
     `PLACED` ➔ `ACCEPTED` ➔ `PREPARING` ➔ `READY_FOR_PICKUP` ➔ `PICKED_UP` ➔ `DELIVERED`.
   - Countdown timer with live delivery status updates.
   - Delivery partner details card (Ramesh Kumar, Safety Verified, direct call action).
   - Interactive simulator buttons to advance or cancel order with automatic wallet refund.

6. **Wallet Management (`wallet-service`)**:
   - Live balance display.
   - Instant top-up (+₹200, +₹500, +₹1,000, +₹2,000, or custom) using `POST /api/wallet/funds` with Idempotency Key.
   - Transaction ledger history (Credit deposits/refunds and Debit food order payments).

7. **Multi-Role Experience**:
   - **Customer View**: Browse, search, add to cart, wallet payment, order tracking.
   - **Restaurant Partner View (`Role.OWNER`)**: Register new restaurants (`catalog-service`), add dishes with daily inventory quotas and price, and manage live incoming kitchen orders.
   - **Delivery Partner View (`Role.AGENT`)**: View runs ready for pickup (`dispatch-service`), accept runs, mark picked up, mark delivered, and track daily earnings.
   - Quick 1-click demo role switcher in the user profile menu.

8. **Architecture Health Indicator**:
   - Top navbar displays an active status badge showing connection to Spring Boot API Gateway (`http://localhost:8080`).
   - Click the indicator to open the **Microservices Architecture & Port Registry Modal**.
   - **Graceful Fallback**: If backend services are starting or offline, full interactive demo mocking activates automatically so every feature can be previewed without crashing.

---

## 🔌 Microservices Integration & Ports

The frontend interacts with the Spring Boot microservices through Vite's reverse proxy:

| Microservice | Default Port | Frontend Proxy Path | Primary Responsibilities |
|---|---|---|---|
| **api-gateway** | `8080` | `/api/**` | Central routing and entry point for WebFlux microservices |
| **auth-service** | `8081` | `/api/auth/**` | JWT login, registration for CUSTOMER, OWNER, AGENT |
| **wallet-service** | `8082` | `/api/wallet/**` | Wallet balance, deposits, debits, idempotency keys |
| **catalog-service** | `8083` | `/api/catalog/**` | Restaurants, menus, dishes, daily inventory stock |
| **order-service** | `8084` | `/api/orders/**`, `/api/carts/**` | Carts, placing orders, kitchen capacity, status lifecycle |
| **dispatch-service** | `8085` | `/api/dispatch/**` | Delivery agent dispatch and assignment |
| **eureka-server** | `8761` | `http://localhost:8761` | Service discovery registry |

### Key HTTP Headers Passed:
- `Authorization: Bearer <accessToken>`: Transmitted for authenticated requests.
- `X-User-Id: <UUID>`: Transmitted to identify the current user across microservices (`catalog`, `order`, `wallet`, `auth`).
- `Idempotency-Key: <UUID>`: Automatically generated for mutating operations (`POST /api/orders`, `POST /api/wallet/funds`, `POST /api/catalog/restaurants`).
