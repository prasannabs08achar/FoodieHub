import React, { createContext, useContext, useState, useEffect } from 'react';
import { checkServiceHealth } from '../services/api';

const BackendContext = createContext();

export const BackendProvider = ({ children }) => {
  const [health, setHealth] = useState({
    gateway: false,
    auth: false,
    wallet: false,
    catalog: false,
    order: false,
    dispatch: false,
  });

  const [isHealthModalOpen, setIsHealthModalOpen] = useState(false);
  const [lastChecked, setLastChecked] = useState(null);

  const probe = async () => {
    const res = await checkServiceHealth();
    setHealth(res);
    setLastChecked(new Date().toLocaleTimeString());
  };

  useEffect(() => {
    probe();
    const interval = setInterval(probe, 30000);
    return () => clearInterval(interval);
  }, []);

  const isConnected = health.gateway || health.auth;

  return (
    <BackendContext.Provider
      value={{
        health,
        isConnected,
        lastChecked,
        probe,
        isHealthModalOpen,
        setIsHealthModalOpen,
      }}
    >
      {children}
    </BackendContext.Provider>
  );
};

export const useBackend = () => useContext(BackendContext);
