import React, { createContext, useContext, useState, useEffect } from 'react';
import { authApi } from '../services/api';

const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(() => {
    try {
      const stored = sessionStorage.getItem('foodiehub_auth');
      if (stored) return JSON.parse(stored);
    } catch (e) {
      console.error('Failed to parse auth from storage', e);
    }
    return null;
  });

  const [isAuthModalOpen, setIsAuthModalOpen] = useState(false);
  const [authModalMode, setAuthModalMode] = useState('login'); // 'login' | 'register'

  useEffect(() => {
    if (user) {
      sessionStorage.setItem('foodiehub_auth', JSON.stringify(user));
    } else {
      sessionStorage.removeItem('foodiehub_auth');
    }
  }, [user]);

  const login = async (email, password) => {
    const res = await authApi.login(email, password);
    const loggedUser = {
      userId: res.userId,
      email: email,
      fullName: res.user?.fullName || email.split('@')[0],
      role: res.role || 'CUSTOMER',
      accessToken: res.accessToken,
    };
    setUser(loggedUser);
    setIsAuthModalOpen(false);
    return loggedUser;
  };

  const register = async ({ email, password, fullName, role }) => {
    await authApi.register({ email, password, fullName, role });
    // After successful registration, log in with credentials
    const loginRes = await authApi.login(email, password);
    const loggedUser = {
      userId: loginRes.userId,
      email: email,
      fullName: fullName,
      role: loginRes.role || role || 'CUSTOMER',
      accessToken: loginRes.accessToken,
    };
    setUser(loggedUser);
    setIsAuthModalOpen(false);
    return loggedUser;
  };

  const logout = () => {
    setUser(null);
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        isAuthenticated: !!user,
        login,
        register,
        logout,
        isAuthModalOpen,
        setIsAuthModalOpen,
        authModalMode,
        setAuthModalMode,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => useContext(AuthContext);
