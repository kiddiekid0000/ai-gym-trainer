import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import authService from '../features/auth/services/authService';
import './VerifyOtpPage.css';

const VerifyOtpPage = () => {
  const [email, setEmail] = useState('');
  const [otp, setOtp] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    // Get email from localStorage
    const pendingEmail = localStorage.getItem('pendingEmail');
    if (!pendingEmail) {
      navigate('/register');
    } else {
      setEmail(pendingEmail);
    }
  }, [navigate]);

  const handleVerify = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    setSuccess('');

    try {
      const response = await authService.verifyOtp(email, otp);

      if (response.status === 'VERIFIED') {
        setSuccess('Email verified successfully! Redirecting to login...');
        localStorage.removeItem('pendingEmail');
        
        // Redirect to login after 2 seconds
        setTimeout(() => {
          navigate('/login');
        }, 2000);
      }
    } catch (err: any) {
      setError(err.message || 'OTP verification failed. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleResendOtp = async () => {
    setLoading(true);
    setError('');
    setSuccess('');

    try {
      await authService.resendOtp(email);
      setSuccess('OTP sent successfully! Check your email.');
    } catch (err: any) {
      setError(err.message || 'Failed to resend OTP');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="verify-otp-container">
      <div className="verify-otp-box">
        <h2>Verify Email</h2>
        <p className="subtitle">Enter the 6-digit OTP sent to your email</p>

        {error && <div className="error-message">{error}</div>}
        {success && <div className="success-message">{success}</div>}

        <form onSubmit={handleVerify}>
          <div className="form-group">
            <label>Email</label>
            <input
              type="email"
              value={email}
              disabled
              className="disabled-input"
            />
          </div>

          <div className="form-group">
            <label>OTP</label>
            <input
              type="text"
              placeholder="Enter 6-digit OTP"
              value={otp}
              onChange={(e) => setOtp(e.target.value.replace(/[^0-9]/g, '').slice(0, 6))}
              maxLength={6}
              required
              disabled={loading}
            />
          </div>

          <button type="submit" disabled={loading || otp.length !== 6}>
            {loading ? 'Verifying...' : 'Verify'}
          </button>
        </form>

        <div className="resend-section">
          <p>Didn't receive OTP?</p>
          <button
            type="button"
            className="resend-button"
            onClick={handleResendOtp}
            disabled={loading}
          >
            Resend OTP
          </button>
        </div>
      </div>
    </div>
  );
};

export default VerifyOtpPage;
