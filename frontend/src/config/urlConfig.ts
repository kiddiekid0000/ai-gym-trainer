// src/config/urlConfig.ts
// Use /api for Nginx proxy routing - Nginx forwards this to gym-trainer-backend:8080
export const API_BASE_URL = '/api';

export const AUTH_ENDPOINTS = {
  LOGIN: '/auth/login',
  REGISTER: '/auth/register',
  REFRESH: '/auth/refresh',
  LOGOUT: '/auth/logout',
} as const;