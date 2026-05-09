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
  TYPES: '/exercises/types',
  ALL: '/exercises',
  BY_TYPE: '/exercises/type',
} as const;

export const WORKOUT_ENDPOINTS = {
  START: '/workouts/start',
  ADD_SET: '/workouts',
  END: '/workouts',
  HISTORY: '/workouts',
  RECORDS: '/records',
} as const;

export const LEADERBOARD_ENDPOINTS = {
  GET: '/leaderboard',
} as const;

export const WEBSOCKET_URL = '/ws';