import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../features/auth/hooks/useAuth';
import './LoginPage.css';

const LoginPage = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const { login, loading, error } = useAuth();

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    login(email, password);
  };

  return (
    <div className="login-container">
      <div className="login-box">
        <h2>Sign In</h2>
        
        {error && <div className="error">{error}</div>}
        
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <input
              type="email"
              placeholder="Email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              disabled={loading}
            />
          </div>
          
          <div className="form-group">
            <input
              type="password"
              placeholder="Password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              disabled={loading}
            />
          </div>
          
          <button type="submit" disabled={loading}>
            {loading ? 'Loading...' : 'Sign In'}
          </button>
        </form>
        
        <p>
          Don't have an account? <Link to="/register">Register</Link>
        </p>
        <Link to="/">Back to Home</Link>
      </div>
    </div>
  );
};

export default LoginPage;