import React, { createContext, useContext, useState, useEffect } from 'react';
import axios from 'axios';

const UserContext = createContext();

export const useUser = () => {
  const context = useContext(UserContext);
  if (!context) {
    throw new Error('useUser must be used within a UserProvider');
  }
  return context;
};

export const UserProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Check for stored JWT token and user data
    const checkAuthStatus = async () => {
      const storedToken = localStorage.getItem('token');
      const storedUser = localStorage.getItem('user');

      if (storedToken && storedUser) {
        try {
          // Validate token with backend
          const response = await axios.get('/api/auth/validate', {
            headers: { 'Authorization': `Bearer ${storedToken}` }
          });

          if (response.data.success) {
            setUser(JSON.parse(storedUser));
            setToken(storedToken);
            // Set default Authorization header for all future requests
            axios.defaults.headers.common['Authorization'] = `Bearer ${storedToken}`;
          } else {
            // Token invalid, clear storage
            localStorage.removeItem('token');
            localStorage.removeItem('user');
            delete axios.defaults.headers.common['Authorization'];
          }
        } catch (error) {
          console.error('Token validation failed:', error);
          // Clear invalid tokens
          localStorage.removeItem('token');
          localStorage.removeItem('user');
          delete axios.defaults.headers.common['Authorization'];
        }
      }
      setLoading(false);
    };

    checkAuthStatus();
  }, []);

  const login = async (credentials) => {
    try {
      const response = await axios.post('/api/auth/login', credentials);
      const data = response.data;

      if (!data.success) {
        throw new Error(data.message || 'Login failed');
      }

      const userData = data.user;
      const jwtToken = data.token;

      // Store user data and token
      setUser(userData);
      setToken(jwtToken);

      localStorage.setItem('user', JSON.stringify(userData));
      localStorage.setItem('token', jwtToken);

      // Set default Authorization header for all future requests
      axios.defaults.headers.common['Authorization'] = `Bearer ${jwtToken}`;

    } catch (error) {
      throw new Error(error.response?.data?.message || 'Login failed');
    }
  };

  const register = async (userData) => {
    try {
      const response = await axios.post('/api/auth/register', userData);
      const data = response.data;

      if (!data.success) {
        throw new Error(data.message || 'Registration failed');
      }

      const newUserData = data.user;
      setUser(newUserData);
      localStorage.setItem('user', JSON.stringify(newUserData));

    } catch (error) {
      throw new Error(error.response?.data?.message || 'Registration failed');
    }
  };

  const logout = async () => {
    try {
      // Call logout endpoint to invalidate server-side if needed
      if (token) {
        await axios.post('/api/auth/logout');
      }
    } catch (error) {
      console.error('Logout error:', error);
    } finally {
      // Clear client-side state regardless of server response
      setUser(null);
      setToken(null);
      localStorage.removeItem('user');
      localStorage.removeItem('token');
      delete axios.defaults.headers.common['Authorization'];
    }
  };

  const value = {
    user,
    token,
    loading,
    login,
    register,
    logout,
  };

  return (
    <UserContext.Provider value={value}>
      {children}
    </UserContext.Provider>
  );
};
