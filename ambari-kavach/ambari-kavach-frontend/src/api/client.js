/**
 * API client for Ambari Kavach backend.
 * Uses relative URLs so Vue dev proxy forwards /api, /auth, /create_user, /manager to backend.
 * In production, serve frontend and backend from same origin or set VUE_APP_API_BASE.
 */
import axios from 'axios';
import { useAuthStore } from '../stores/auth';

const API_BASE = process.env.VUE_APP_API_BASE || '';

/**
 * Get headers for authenticated requests (X-Email from logged-in user).
 */
function getAuthHeaders() {
  const authStore = useAuthStore();
  const headers = { 'Content-Type': 'application/json' };
  if (authStore.userEmail) {
    headers['X-Email'] = authStore.userEmail;
  }
  if (authStore.jwtToken) {
    headers['Authorization'] = `Bearer ${authStore.jwtToken}`;
  }
  return headers;
}

// Handle 401 Unauthorized - redirect to login (session expired or invalid token)
axios.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      const authStore = useAuthStore();
      authStore.logout();
      const path = window.location.pathname;
      if (path !== '/login') {
        window.location.href = `/login?redirect=${encodeURIComponent(path)}`;
      }
    }
    return Promise.reject(err);
  }
);

/**
 * GET request to API.
 */
export function apiGet(path, config = {}) {
  return axios.get(`${API_BASE}${path}`, {
    ...config,
    headers: { ...getAuthHeaders(), ...config.headers }
  });
}

/**
 * POST request to API.
 */
export function apiPost(path, data, config = {}) {
  return axios.post(`${API_BASE}${path}`, data, {
    ...config,
    headers: { ...getAuthHeaders(), ...config.headers }
  });
}
