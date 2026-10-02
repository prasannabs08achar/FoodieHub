import React, { createContext, useContext, useState, useEffect } from 'react';
import { useAuth } from './AuthContext';
import { walletApi } from '../services/api';

const WalletContext = createContext();

export const WalletProvider = ({ children }) => {
  const { user } = useAuth();
  const [wallet, setWallet] = useState(null);
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(false);
  const [isWalletModalOpen, setIsWalletModalOpen] = useState(false);

  const fetchWallet = async () => {
    if (!user?.userId) return;
    setLoading(true);
    try {
      const data = await walletApi.getWallet(user.userId);
      setWallet(data);
      const txData = await walletApi.getTransactions(user.userId);
      setTransactions(txData?.content || []);
    } catch (e) {
      console.error('Wallet load error:', e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchWallet();
  }, [user?.userId]);

  const addFunds = async (amount) => {
    if (!user?.userId) return;
    setLoading(true);
    try {
      await walletApi.addFunds(user.userId, amount);
      await fetchWallet();
      return true;
    } catch (e) {
      console.error('Add funds error:', e);
      return false;
    } finally {
      setLoading(false);
    }
  };

  return (
    <WalletContext.Provider
      value={{
        wallet,
        balance: wallet?.balance != null ? Number(wallet.balance) : 0,
        transactions,
        loading,
        fetchWallet,
        addFunds,
        isWalletModalOpen,
        setIsWalletModalOpen,
      }}
    >
      {children}
    </WalletContext.Provider>
  );
};

export const useWallet = () => useContext(WalletContext);
