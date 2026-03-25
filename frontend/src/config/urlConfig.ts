// src/config/urlConfig.ts
export const API_BASE_URL = 'http://localhost:8080';

export const AUTH_ENDPOINTS = {
  LOGIN: '/auth/login',
  REGISTER: '/auth/register',
  REFRESH: '/auth/refresh',
  LOGOUT: '/auth/logout',
} as const;