import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../features/auth/hooks/useAuth';
import './RegisterPage.css';

const RegisterPage = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [localError, setLocalError] = useState('');
  const { register, loading, error } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (password !== confirmPassword) {
      setLocalError('Passwords do not match');
      return;
    }

    setLocalError('');
    await register(email, password);
  };

  // if register successfully, navigate to user page, logic in useAuth.ts
  return (
    <div className="register-container">
      <div className="register-box">
        <h2>Create Account</h2>
        <p className="subtitle">Join AI Gym Trainer today</p>
        
        {(localError || error) && (
          <div className="error-message">{localError || error}</div>
        )}
        
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
              minLength={6}
            />
          </div>

          <div className="form-group">
            <input
              type="password"
              placeholder="Confirm Password"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              required
              disabled={loading}
              minLength={6}
            />
          </div>
          
          <button type="submit" disabled={loading}>
            {loading ? 'Creating account...' : 'Register'}
          </button>
        </form>
        
        <p className="login-link">
          Already have an account? <Link to="/login">Sign In</Link>
        </p>
        <Link to="/" className="back-link">← Back to Home</Link>
      </div>
    </div>
  );
};

export default RegisterPage;