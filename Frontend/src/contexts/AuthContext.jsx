import { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { apiClient } from '../api/apiClient';

const STORAGE_KEY_TOKEN = 'auth_token';
const STORAGE_KEY_REFRESH = 'auth_refresh_token';
const STORAGE_KEY_USER = 'auth_user';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [accessToken, setAccessToken] = useState(() => localStorage.getItem(STORAGE_KEY_TOKEN));
  const [refreshToken, setRefreshToken] = useState(() => localStorage.getItem(STORAGE_KEY_REFRESH));
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  // Rehydrate user from localStorage on mount
  useEffect(() => {
    const storedUser = localStorage.getItem(STORAGE_KEY_USER);
    if (storedUser) {
      try {
        setUser(JSON.parse(storedUser));
      } catch {
        localStorage.removeItem(STORAGE_KEY_USER);
      }
    }
  }, []);

  const login = useCallback(async (username, password) => {
    setLoading(true);
    setError(null);
    try {
      const res = await apiClient.post('/api/v1/auth/login', { username, password });

      const data = res.data?.data;
      if (!data) throw new Error('Invalid response');

      const { accessToken: at, refreshToken: rt, user: u } = data;

      setAccessToken(at);
      setRefreshToken(rt);
      setUser(u);
      localStorage.setItem(STORAGE_KEY_TOKEN, at);
      localStorage.setItem(STORAGE_KEY_REFRESH, rt);
      localStorage.setItem(STORAGE_KEY_USER, JSON.stringify(u));

      return u;
    } catch (err) {
      const message = err.response?.data?.message || err.message || 'Login failed';
      setError(message);
      throw new Error(message);
    } finally {
      setLoading(false);
    }
  }, []);

  const logout = useCallback(() => {
    setAccessToken(null);
    setRefreshToken(null);
    setUser(null);
    setError(null);
    localStorage.removeItem(STORAGE_KEY_TOKEN);
    localStorage.removeItem(STORAGE_KEY_REFRESH);
    localStorage.removeItem(STORAGE_KEY_USER);
  }, []);

  const value = {
    user,
    accessToken,
    refreshToken,
    login,
    logout,
    isAuthenticated: !!accessToken && !!user,
    role: user?.roleCode || null,
    loading,
    error,
    clearError: () => setError(null),
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
