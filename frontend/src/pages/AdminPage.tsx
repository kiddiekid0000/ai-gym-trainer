import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import { API_BASE_URL } from '../config/urlConfig';
import './AdminPage.css';

const AdminPage = () => {
  const [adminData, setAdminData] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    fetchAdminData();
  }, []);

  const fetchAdminData = async () => {
    try {
      const response = await axios.get(`${API_BASE_URL}/users/admin`, {
        withCredentials: true
      });
      setAdminData(response.data);
    } catch (err: any) {
      if (err.response?.status === 403 || err.response?.status === 401) {
        // Không có quyền hoặc chưa đăng nhập
        navigate('/login');
      } else {
        setError(err.response?.data?.message || 'Failed to load admin data');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = () => {
    document.cookie = 'accessToken=; Max-Age=0; path=/';
    document.cookie = 'refreshToken=; Max-Age=0; path=/';
    navigate('/');
  };

  if (loading) return <div className="loading">Loading...</div>;
  if (error) return <div className="error">{error}</div>;

  return (
    <div className="admin-container">
      <nav className="admin-nav">
        <h1>Admin Dashboard</h1>
        <button onClick={handleLogout} className="logout-btn">
          Logout
        </button>
      </nav>
      
      <main className="admin-main">
        <div className="admin-card">
          <h2>Admin Access</h2>
          <p>{adminData}</p>
        </div>

        <div className="admin-stats">
          <div className="stat-card">
            <h3>Total Users</h3>
            <p>0</p>
          </div>
          <div className="stat-card">
            <h3>Active Sessions</h3>
            <p>0</p>
          </div>
          <div className="stat-card">
            <h3>System Status</h3>
            <p>Online</p>
          </div>
        </div>
      </main>
    </div>
  );
};

export default AdminPage;