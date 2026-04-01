import { Link } from 'react-router-dom';
import './HomePage.css';

const HomePage = () => {
  return (
    <div className="home-container">
      <header className="header">
        <h1>AI Gym Trainer</h1>
        <nav className="nav">
          <Link to="/login" className="btn btn-outline">Sign In</Link>
          <Link to="/register" className="btn btn-primary">Register</Link>
        </nav>
      </header>
      
      <main className="main-content">
        <section className="hero">
          <h2>Your Personal AI Gym Trainer</h2>
          <p>Get personalized workout plans and track your progress with AI</p>
          <Link to="/register" className="btn btn-large btn-primary">
            Get Started
          </Link>
        </section>
      </main>
    </div>
  );
};

export default HomePage;