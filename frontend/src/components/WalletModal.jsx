import React, { useState } from 'react';
import { useWallet } from '../context/WalletContext';

export default function WalletModal({ isOpen, onClose }) {
  const walletCtx = useWallet();
  const show = isOpen !== undefined ? isOpen : walletCtx?.isWalletModalOpen;
  const handleClose = onClose || (() => walletCtx?.setIsWalletModalOpen(false));

  const [amount, setAmount] = useState('500');
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState('');

  if (!show) return null;

  const handleAdd = async (e) => {
    e.preventDefault();
    if (!amount || Number(amount) <= 0) return;
    setLoading(true);
    setSuccess('');
    const ok = await walletCtx.addFunds(Number(amount));
    setLoading(false);
    if (ok) {
      setSuccess(`₹${amount} added successfully!`);
      setTimeout(() => {
        setSuccess('');
        handleClose();
      }, 1200);
    }
  };

  return (
    <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm z-50 flex items-center justify-center p-4 animate-fade-in">
      <div
        onClick={(e) => e.stopPropagation()}
        className="clean-card max-w-sm w-full p-6 bg-white shadow-2xl animate-scale-up"
      >
        <div className="flex items-center justify-between pb-3 border-b border-slate-100 mb-4">
          <h3 className="font-extrabold text-base text-slate-900">FoodieHub Wallet</h3>
          <button
            onClick={handleClose}
            className="w-7 h-7 rounded-full bg-slate-100 hover:bg-slate-200 text-slate-600 flex items-center justify-center text-xs font-bold transition-colors"
          >
            ✕
          </button>
        </div>

        <div className="rounded-2xl bg-slate-900 text-white p-5 text-center mb-5">
          <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider">
            Current Balance
          </span>
          <div className="text-3xl font-extrabold text-emerald-400 mt-1">
            ₹{walletCtx?.balance?.toFixed(2) || '0.00'}
          </div>
        </div>

        {success && (
          <div className="p-2.5 mb-4 bg-emerald-50 border border-emerald-200 rounded-xl text-xs text-emerald-800 font-bold text-center">
            ✓ {success}
          </div>
        )}

        <form onSubmit={handleAdd} className="space-y-4">
          <div className="grid grid-cols-4 gap-1.5">
            {[200, 500, 1000, 2000].map((v) => (
              <button
                key={v}
                type="button"
                onClick={() => setAmount(String(v))}
                className={`py-1.5 rounded-lg text-xs font-bold border transition-colors ${
                  amount === String(v)
                    ? 'bg-orange-50 border-orange-500 text-orange-600'
                    : 'bg-white border-slate-200 text-slate-700'
                }`}
              >
                +₹{v}
              </button>
            ))}
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Top up amount (₹)
            </label>
            <input
              type="number"
              min="1"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              className="clean-input"
              placeholder="Amount"
            />
          </div>

          <button
            type="submit"
            disabled={loading}
            className="btn-success w-full text-xs"
          >
            {loading ? 'Adding funds...' : `Add ₹${amount} to Wallet`}
          </button>
        </form>
      </div>
    </div>
  );
}

export { WalletModal };
