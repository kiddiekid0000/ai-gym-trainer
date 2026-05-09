// src/features/auth/hooks/useAuth.ts
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import authService from '../services/authService';

export const useAuth = () => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const login = async (email: string, password: string) => {
    setLoading(true);
    setError('');
    
    try {
      const response = await authService.login({ email, password });
      
      if (response.status === 'AUTHENTICATED') {
        // User is logged in, redirect based on role
        if (response.role === 'ADMIN') {
          navigate('/admin');
        } else {
          navigate('/dashboard');
        }
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Login failed');
    } finally {
      setLoading(false);
    }
  };

  const register = async (email: string, password: string) => {
    setLoading(true);
    setError('');
    
    try {
      const response = await authService.register({ email, password });
      
      // Check if OTP verification is required
      if (response.status === 'PENDING_OTP_VERIFICATION') {
        // Store email for OTP verification page
        localStorage.setItem('pendingEmail', response.email);
        navigate('/verify-otp');
      } else {
        // Unexpected status
        console.error('Unexpected registration response');
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Registration failed');
    } finally {
      setLoading(false);
    }
  };

  const logout = async () => {
    try {
      await authService.logout();
    } catch (err: any) {
      console.error('Logout error:', err);
    } finally {
      navigate('/');
    }
  };

  return { login, register, logout, loading, error };
};