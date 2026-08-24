import axios from 'axios';

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8083';
const STORAGE_KEY_TOKEN = 'auth_token';
const STORAGE_KEY_REFRESH = 'auth_refresh_token';

// Refresh coordination
let isRefreshing = false;
let refreshSubscribers = [];

function subscribeTokenRefresh(cb) {
  refreshSubscribers.push(cb);
}

function onTokenRefreshed(token) {
  refreshSubscribers.forEach(cb => cb(token));
  refreshSubscribers = [];
}

export const apiClient = axios.create({
  baseURL: API_BASE,
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' },
});

// Request interceptor: attach Authorization header
apiClient.interceptors.request.use(
  config => {
    const token = localStorage.getItem(STORAGE_KEY_TOKEN);
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  error => Promise.reject(error)
);

// Response interceptor: handle 401 → token refresh
apiClient.interceptors.response.use(
  response => response,
  async error => {
    const originalRequest = error.config;

    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      const rt = localStorage.getItem(STORAGE_KEY_REFRESH);

      if (rt) {
        if (!isRefreshing) {
          isRefreshing = true;

          try {
            const res = await axios.post(
              `${API_BASE}/api/v1/auth/refresh`,
              { refreshToken: rt },
              { headers: { 'Content-Type': 'application/json' } }
            );

            const newToken = res.data?.data?.accessToken;
            const newRefresh = res.data?.data?.refreshToken;

            if (newToken) {
              localStorage.setItem(STORAGE_KEY_TOKEN, newToken);
              if (newRefresh) {
                localStorage.setItem(STORAGE_KEY_REFRESH, newRefresh);
              }
            }

            isRefreshing = false;
            onTokenRefreshed(newToken);
          } catch (refreshError) {
            isRefreshing = false;
            localStorage.removeItem(STORAGE_KEY_TOKEN);
            localStorage.removeItem(STORAGE_KEY_REFRESH);
            localStorage.removeItem('auth_user');
            window.location.href = '/login';
            return Promise.reject(refreshError);
          }
        } else {
          return new Promise(resolve => {
            subscribeTokenRefresh(token => {
              originalRequest.headers.Authorization = `Bearer ${token}`;
              resolve(apiClient(originalRequest));
            });
          });
        }
      }
    }

    return Promise.reject(error);
  }
);
