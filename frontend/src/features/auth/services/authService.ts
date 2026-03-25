// src/features/auth/services/authService.ts
import axios from 'axios';
import { API_BASE_URL, AUTH_ENDPOINTS } from '../../../config/urlConfig';

interface LoginCredentials {
  email: string;
  password: string;
}

interface RegisterData {
  email: string;
  password: string;
}

interface AuthResponse {
  id: number;
  email: string;
  role: 'USER' | 'ADMIN';
}

class AuthService {
  async login(credentials: LoginCredentials): Promise<AuthResponse> {
    const response = await axios.post(
      `${API_BASE_URL}${AUTH_ENDPOINTS.LOGIN}`, 
      credentials,
      { withCredentials: true }
    );
    return response.data;
  }

  async register(userData: RegisterData): Promise<AuthResponse> {
    const response = await axios.post(
      `${API_BASE_URL}${AUTH_ENDPOINTS.REGISTER}`, 
      userData,
      { withCredentials: true }
    );
    return response.data;
  }


  //force to call log out api sucessfully, if not cannot log out. 
  // Because if let user log out without successfully calling logout api, 
  // token will not be deleted and attacker can use token
  async logout(): Promise<void> {
    await axios.post(
      `${API_BASE_URL}${AUTH_ENDPOINTS.LOGOUT}`,
      {},
      { withCredentials: true }
    );
    
    document.cookie = 'accessToken=; Max-Age=0; path=/';
    document.cookie = 'refreshToken=; Max-Age=0; path=/';
  }
}

export default new AuthService();