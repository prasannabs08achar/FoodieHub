import React, { useState } from 'react';
import { useWallet } from '../context/WalletContext';

export default function WalletPage() {
  const { balance, transactions, addFunds } = useWallet();
  const [amount, setAmount] = useState('500');
  const [loading, setLoading] = useState(false);
  const [successMsg, setSuccessMsg] = useState('');

  const handleAdd = async (e) => {
    e.preventDefault();
    if (!amount || Number(amount) <= 0) return;
    setLoading(true);
    setSuccessMsg('');
    const ok = await addFunds(Number(amount));
    setLoading(false);
    if (ok) {
      setSuccessMsg(`₹${amount} added successfully to your wallet!`);
      setTimeout(() => setSuccessMsg(''), 3000);
    }
  };

  return (
    <div className="max-w-xl mx-auto px-4 sm:px-6 py-6 pb-28">
      <div className="mb-6">
        <h1 className="text-xl font-extrabold text-slate-900 tracking-tight">FoodieHub Wallet</h1>
        <p className="text-xs text-slate-500 mt-0.5">Powered by Spring Boot wallet-service with idempotency protection</p>
      </div>

      {/* Balance Card */}
      <div className="rounded-3xl bg-slate-900 text-white p-6 sm:p-7 shadow-lg mb-6 relative overflow-hidden">
        <div className="absolute top-0 right-0 p-6 text-6xl opacity-10 pointer-events-none">
          💰
        </div>
        <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider">
          Available Digital Balance
        </span>
        <div className="text-3xl sm:text-4xl font-extrabold text-emerald-400 mt-1">
          ₹{balance.toFixed(2)}
        </div>
        <p className="text-xs text-slate-400 mt-3">
          100% safe & instant 1-click checkout for food orders.
        </p>
      </div>

      {successMsg && (
        <div className="p-3 mb-6 bg-emerald-50 border border-emerald-200 rounded-xl text-xs text-emerald-800 font-bold">
          ✓ {successMsg}
        </div>
      )}

      {/* Top up box */}
      <div className="clean-card p-5 sm:p-6 bg-white mb-6">
        <h3 className="font-extrabold text-sm text-slate-900 mb-3">Top Up Wallet</h3>
        <form onSubmit={handleAdd} className="space-y-4">
          <div className="grid grid-cols-4 gap-2">
            {[200, 500, 1000, 2000].map((v) => (
              <button
                key={v}
                type="button"
                onClick={() => setAmount(String(v))}
                className={`py-2 rounded-xl text-xs font-bold border transition-colors ${
                  amount === String(v)
                    ? 'bg-orange-50 border-orange-500 text-orange-600 shadow-sm'
                    : 'bg-white border-slate-200 hover:border-slate-300 text-slate-700'
                }`}
              >
                +₹{v}
              </button>
            ))}
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Custom Amount (₹)
            </label>
            <input
              type="number"
              min="1"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              className="clean-input"
              placeholder="Enter amount"
            />
          </div>

          <button
            type="submit"
            disabled={loading}
            className="btn-success w-full text-xs"
          >
            {loading ? 'Processing top up...' : `Add ₹${amount} to Wallet`}
          </button>
        </form>
      </div>

      {/* Transactions */}
      <div className="clean-card p-5 sm:p-6 bg-white">
        <h3 className="font-extrabold text-sm text-slate-900 mb-3">Recent Transactions</h3>
        {transactions.length === 0 ? (
          <div className="text-center py-6 text-xs text-slate-400">
            No transaction records found.
          </div>
        ) : (
          <div className="divide-y divide-slate-100">
            {transactions.map((tx) => {
              const isCredit = tx.type === 'CREDIT' || (tx.amount && tx.amount > 0);
              return (
                <div key={tx.id} className="py-3 flex items-center justify-between text-xs">
                  <div>
                    <div className="font-bold text-slate-800">{tx.description || tx.type}</div>
                    <div className="text-[10px] text-slate-400 mt-0.5">
                      {new Date(tx.createdAt).toLocaleDateString()}
                    </div>
                  </div>
                  <div className={`font-extrabold text-sm ${isCredit ? 'text-emerald-600' : 'text-rose-600'}`}>
                    {isCredit ? '+' : '-'}₹{Math.abs(tx.amount)}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}

export { WalletPage };
