import { apiClient } from './apiClient';

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8083';

/**
 * Login user.
 * @param {string} username
 * @param {string} password
 * @returns {Promise<{ accessToken, refreshToken, expiresIn, refreshExpiresIn, user }>}
 */
export async function login(username, password) {
  const res = await apiClient.post('/api/v1/auth/login', { username, password });

  if (!res.data?.success) {
    throw new Error(res.data?.message || 'Login failed');
  }

  return res.data.data;
}

/**
 * Logout user.
 * @param {string} refreshToken
 */
export async function logout(refreshToken) {
  try {
    await apiClient.post('/api/v1/auth/logout', { refreshToken });
  } catch {
    // Ignore logout errors — local cleanup still proceeds
  }
}

/**
 * Refresh access token.
 * @param {string} refreshToken
 * @returns {Promise<{ accessToken, refreshToken, expiresIn }>}
 */
export async function refreshToken(refreshToken) {
  const res = await apiClient.post('/api/v1/auth/refresh', { refreshToken });

  if (!res.data?.success) {
    throw new Error(res.data?.message || 'Token refresh failed');
  }

  return res.data.data;
}
