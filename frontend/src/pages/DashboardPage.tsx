import { useNavigate } from 'react-router-dom';
import { useAuth } from '../features/auth/hooks/useAuth';
import './DashboardPage.css';

const DashboardPage = () => {
  const navigate = useNavigate();
  const { logout } = useAuth();

  return (
    <div className="dashboard-container">
      <div className="dashboard-header">
        <h1>Gym Trainer Dashboard</h1>
        <button onClick={logout} className="logout-btn">
          Logout
        </button>
      </div>

      <div className="menu-grid">
        <div 
          className="menu-card exercises-card"
          onClick={() => navigate('/exercises')}
        >
          <div className="menu-icon">💪</div>
          <h2>Exercises</h2>
          <p>Choose an exercise to do</p>
        </div>

        <div 
          className="menu-card records-card"
          onClick={() => navigate('/leaderboard')}
        >
          <div className="menu-icon">🏆</div>
          <h2>Best Records</h2>
          <p>View leaderboard</p>
        </div>

        <div 
          className="menu-card my-records-card"
          onClick={() => navigate('/workouts')}
        >
          <div className="menu-icon">📊</div>
          <h2>Your Records</h2>
          <p>View your performance</p>
        </div>

        <div 
          className="menu-card profile-card"
          onClick={() => navigate('/user')}
        >
          <div className="menu-icon">👤</div>
          <h2>Profile</h2>
          <p>View profile info</p>
        </div>
      </div>
    </div>
  );
};

export default DashboardPage;
