import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import { API_BASE_URL } from '../config/urlConfig';
import { useAuth } from '../features/auth/hooks/useAuth';
import './UserPage.css';

const UserPage = () => {
  const [profileData, setProfileData] = useState('');
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

  const handleLogout = () => {
    logout();
  };

  if (loading) return <div className="loading">Loading...</div>;
  if (error) return <div className="error">{error}</div>;

  return (
    <div className="user-container">
      <nav className="user-nav">
        <h1>User Dashboard</h1>
        <button onClick={handleLogout} className="logout-btn">
          Logout
        </button>
      </nav>
      
      <main className="user-main">
        <div className="profile-card">
          <h2>Your Profile</h2>
          <p>{profileData}</p>
        </div>

        <div className="user-stats">
          <div className="stat-card">
            <h3>Workouts Completed</h3>
            <p>0</p>
          </div>
          <div className="stat-card">
            <h3>Total Minutes</h3>
            <p>0</p>
          </div>
          <div className="stat-card">
            <h3>Current Streak</h3>
            <p>0 days</p>
          </div>
        </div>

        <div className="quick-actions">
          <h2>Quick Actions</h2>
          <div className="actions-grid">
            <button className="action-btn">Start Workout</button>
            <button className="action-btn">View History</button>
            <button className="action-btn">Update Profile</button>
          </div>
        </div>
      </main>
    </div>
  );
};

export default UserPage;