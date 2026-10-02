import React from 'react';
import { useBackend } from '../context/BackendContext';

const SERVICES = [
  { name: 'API Gateway', port: 8080, key: 'gateway', desc: 'Spring Cloud Gateway router & load balancer' },
  { name: 'Auth Service', port: 8081, key: 'auth', desc: 'JWT user registration, roles & login' },
  { name: 'Wallet Service', port: 8082, key: 'wallet', desc: 'Digital ledger with idempotency key debit' },
  { name: 'Catalog Service', port: 8083, key: 'catalog', desc: 'Restaurant details & menu items' },
  { name: 'Order Service', port: 8084, key: 'order', desc: 'Active cart, checkout & state machine' },
  { name: 'Dispatch Service', port: 8085, key: 'dispatch', desc: 'Delivery partner matching & tracking' },
];

export default function BackendHealthModal({ isOpen, onClose }) {
  const backendCtx = useBackend();
  const show = isOpen !== undefined ? isOpen : backendCtx?.isHealthModalOpen;
  const handleClose = onClose || (() => backendCtx?.setIsHealthModalOpen(false));
  const health = backendCtx?.health;
  const probe = backendCtx?.probe;

  if (!show) return null;

  return (
    <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm z-50 flex items-center justify-center p-4 animate-fade-in">
      <div
        onClick={(e) => e.stopPropagation()}
        className="clean-card max-w-md w-full p-6 bg-white shadow-2xl animate-scale-up"
      >
        <div className="flex items-center justify-between pb-3 border-b border-slate-100 mb-4">
          <div className="flex items-center gap-2">
            <span className="text-lg">⚙️</span>
            <div>
              <h3 className="font-extrabold text-base text-slate-900">
                Spring Boot Microservices
              </h3>
              <p className="text-[11px] text-slate-500">Live connection probe status</p>
            </div>
          </div>
          <button
            onClick={handleClose}
            className="w-7 h-7 rounded-full bg-slate-100 hover:bg-slate-200 text-slate-600 flex items-center justify-center text-xs font-bold transition-colors"
          >
            ✕
          </button>
        </div>

        <div className="space-y-2 mb-4">
          {SERVICES.map((s) => {
            const up = health?.[s.key];
            return (
              <div
                key={s.key}
                className={`p-3 rounded-xl border flex items-center justify-between gap-3 transition-colors ${
                  up
                    ? 'bg-emerald-50/60 border-emerald-200'
                    : 'bg-slate-50/70 border-slate-200/80'
                }`}
              >
                <div>
                  <div className="flex items-center gap-2">
                    <span className="font-bold text-xs text-slate-900">{s.name}</span>
                    <span className="font-mono text-[10px] text-slate-500 bg-white px-1.5 py-0.5 rounded border border-slate-200">
                      port {s.port}
                    </span>
                  </div>
                  <p className="text-[11px] text-slate-500 mt-0.5">{s.desc}</p>
                </div>

                <span
                  className={`px-2 py-0.5 rounded-full text-[10px] font-extrabold uppercase flex-shrink-0 ${
                    up
                      ? 'bg-emerald-100 text-emerald-800'
                      : 'bg-slate-200 text-slate-600'
                  }`}
                >
                  {up ? '● Online' : '○ Standby'}
                </span>
              </div>
            );
          })}
        </div>

        <div className="p-3 bg-slate-50 rounded-xl text-xs text-slate-600 mb-4 leading-relaxed">
          💡 When your Spring Boot microservices are running on ports 8080-8085, the app communicates with the live backend endpoints. When offline, all features run seamlessly in local client mock mode.
        </div>

        <button
          onClick={() => {
            if (probe) probe();
          }}
          className="btn-secondary w-full text-xs"
        >
          🔄 Re-test Backend Connection
        </button>
      </div>
    </div>
  );
}

export { BackendHealthModal };
