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

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  if (loading) return <div className="loading">Loading...</div>;
  if (error) return <div className="error">Error: {error}</div>;
  if (!profileData) return <div className="error">No profile data</div>;

  return (
    <div className="user-page">
      <header className="page-header">
        <h1>👤 Your Profile</h1>
        <div className="header-buttons">
          <button className="back-btn" onClick={() => navigate('/dashboard')}>
            ← Back to Dashboard
          </button>
          <button className="logout-btn" onClick={handleLogout}>
            🚪 Logout
          </button>
        </div>
      </header>

      <main className="profile-main">
        <div className="profile-card">
          <div className="profile-header">
            <h2>Account Information</h2>
          </div>
          <div className="profile-info">
            <div className="info-row">
              <span className="info-label">User ID:</span>
              <span className="info-value">{profileData.id}</span>
            </div>
            <div className="info-row">
              <span className="info-label">Email:</span>
              <span className="info-value">{profileData.email}</span>
            </div>
            <div className="info-row">
              <span className="info-label">Role:</span>
              <span className="info-value">
                <span className={`role-badge role-${profileData.role.toLowerCase()}`}>
                  {profileData.role}
                </span>
              </span>
            </div>
            <div className="info-row">
              <span className="info-label">Verified:</span>
              <span className="info-value">
                {profileData.verified ? (
                  <span className="verified-badge">✓ Verified</span>
                ) : (
                  <span className="unverified-badge">✗ Not Verified</span>
                )}
              </span>
            </div>
            <div className="info-row">
              <span className="info-label">Account Status:</span>
              <span className="info-value">
                <span className={`status-badge status-${profileData.status.toLowerCase()}`}>
                  {profileData.status}
                </span>
              </span>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
};

export default UserPage;