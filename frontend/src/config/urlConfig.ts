// src/config/urlConfig.ts
// Use /api for Nginx proxy routing - Nginx forwards this to gym-trainer-backend:8080
export const API_BASE_URL = '/api';

export const AUTH_ENDPOINTS = {
  LOGIN: '/auth/login',
  REGISTER: '/auth/register',
  REFRESH: '/auth/refresh',
  LOGOUT: '/auth/logout',
  VERIFY_OTP: '/auth/verify-otp',
  SEND_OTP: '/auth/send-otp',
} as const;

export const EXERCISE_ENDPOINTS = {
  TYPES: '/api/exercises/types',
  ALL: '/api/exercises',
  BY_TYPE: '/api/exercises/type',
} as const;

export const WORKOUT_ENDPOINTS = {
  START: '/api/workouts/start',
  ADD_SET: '/api/workouts',
  END: '/api/workouts',
  HISTORY: '/api/workouts',
  RECORDS: '/api/records',
} as const;

export const LEADERBOARD_ENDPOINTS = {
  GET: '/api/leaderboard',
} as const;

export const WEBSOCKET_URL = '/ws';