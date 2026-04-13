import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import { API_BASE_URL } from '../config/urlConfig';
import { useAuth } from '../features/auth/hooks/useAuth';
import './UserPage.css';

interface UserProfile {
  id: number;
  email: string;
  role: 'USER' | 'ADMIN';
  verified: boolean;
  status: string;
}

const UserPage = () => {
  const [profileData, setProfileData] = useState<UserProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const navigate = useNavigate();
  const { logout } = useAuth();

  useEffect(() => {
    fetchProfile();
  }, []);

  const fetchProfile = async () => {
    try {
      const response = await axios.get(`${API_BASE_URL}/users/profile`, {
        withCredentials: true
      });
      setProfileData(response.data);
    } catch (err: any) {
      if (err.response?.status === 403 || err.response?.status === 401) {
        // unauthorized or not logged in
        navigate('/login');
      } else {
        setError(err.response?.data?.message || 'Failed to load profile');
      }
    } finally {
      setLoading(false);
    }
  };

  if (loading) return <div>Loading...</div>;
  if (error) return <div>Error: {error}</div>;
  if (!profileData) return <div>No profile data</div>;

  return (
    <div style={{ padding: '20px' }}>
      <h1>User Profile</h1>
      
      <div style={{ marginTop: '20px', border: '1px solid #ccc', padding: '15px' }}>
        <p><strong>ID:</strong> {profileData.id}</p>
        <p><strong>Email:</strong> {profileData.email}</p>
        <p><strong>Role:</strong> {profileData.role}</p>
        <p><strong>Verified:</strong> {profileData.verified ? '✓ Yes' : '✗ No'}</p>
        <p><strong>Status:</strong> {profileData.status}</p>
      </div>

      <button 
        onClick={logout}
        style={{ marginTop: '20px', padding: '10px 20px' }}
      >
        Logout
      </button>
    </div>
  );
};

export default UserPage;