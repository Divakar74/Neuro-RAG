import React from 'react';
import Hero from './Hero';

const LandingPage = ({ onGetStarted, onSignInClick }) => {
  return (
    <div className="min-h-screen bg-primary-100">
      <Hero onSignIn={onSignInClick} />
    </div>
  );
};

export default LandingPage;
