import { createContext, useContext } from 'react';
import { apiClient } from '../api/apiClient';

const ApiContext = createContext(null);

export function ApiProvider({ children }) {
  return (
    <ApiContext.Provider value={{ apiClient }}>
      {children}
    </ApiContext.Provider>
  );
}

export function useApi() {
  const ctx = useContext(ApiContext);
  if (!ctx) throw new Error('useApi must be used within ApiProvider');
  return ctx.apiClient;
}
