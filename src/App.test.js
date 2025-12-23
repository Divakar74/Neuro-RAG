jest.mock('axios');

import React from 'react';
import { render, screen } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import App from './App';

// Mock the useNavigate hook
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => jest.fn(),
}));

// Helper function to render App with Router
const renderWithRouter = (component) => {
  return render(
    <BrowserRouter>
      {component}
    </BrowserRouter>
  );
};

test('renders login page heading', () => {
  renderWithRouter(<App />);
  const headingElement = screen.getByText(/Sign in to your account/i);
  expect(headingElement).toBeInTheDocument();
});

test('renders login form inputs', () => {
  renderWithRouter(<App />);
  const emailInput = screen.getByPlaceholderText(/Email address/i);
  const passwordInput = screen.getByPlaceholderText(/Password/i);
  expect(emailInput).toBeInTheDocument();
  expect(passwordInput).toBeInTheDocument();
});

test('renders sign in button', () => {
  renderWithRouter(<App />);
  const signInButton = screen.getByRole('button', { name: /Sign in/i });
  expect(signInButton).toBeInTheDocument();
});
