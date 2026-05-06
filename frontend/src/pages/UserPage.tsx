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
    <div className="user-dashboard">
      <header className="dashboard-header">
        <h1>AI Gym Trainer Dashboard</h1>
        <button onClick={handleLogout} className="logout-btn">
          Logout
        </button>
      </header>

      <div className="dashboard-content">
        <aside className="dashboard-sidebar">
          <div className="menu-section">
            <h3>Menu</h3>
            <div className="menu-items">
              <button onClick={() => navigate('/exercises')} className="menu-btn">
                <span className="menu-icon">🏋️</span>
                <span>Exercises</span>
              </button>
              <button onClick={() => navigate('/workouts')} className="menu-btn">
                <span className="menu-icon">💪</span>
                <span>Workouts</span>
              </button>
              <button onClick={() => navigate('/leaderboard')} className="menu-btn">
                <span className="menu-icon">🏆</span>
                <span>Leaderboards</span>
              </button>
            </div>
          </div>
        </aside>

        <main className="dashboard-main">
          <section className="profile-section">
            <h2>Your Profile</h2>
            <div className="profile-card">
              <div className="profile-info">
                <p><strong>User ID:</strong> {profileData.id}</p>
                <p><strong>Email:</strong> {profileData.email}</p>
                <p><strong>Role:</strong> {profileData.role}</p>
                <p><strong>Verified:</strong> {profileData.verified ? '✓ Yes' : '✗ No'}</p>
                <p><strong>Status:</strong> {profileData.status}</p>
              </div>
            </div>
          </section>

          <section className="quick-actions">
            <h2>Quick Actions</h2>
            <div className="action-cards">
              <div className="action-card" onClick={() => navigate('/exercises')}>
                <h3>View Exercises</h3>
                <p>Browse available exercises by type</p>
              </div>
              <div className="action-card" onClick={() => navigate('/workouts')}>
                <h3>Start Workout</h3>
                <p>Begin a new workout session</p>
              </div>
              <div className="action-card" onClick={() => navigate('/leaderboard')}>
                <h3>Check Rankings</h3>
                <p>See how you rank against others</p>
              </div>
            </div>
          </section>
        </main>
      </div>
    </div>
  );
};

export default UserPage;