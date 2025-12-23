import axios from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

// Get user ID from localStorage for session tracking
const getUserId = () => {
  const user = localStorage.getItem('user');
  if (user) {
    try {
      const userData = JSON.parse(user);
      return userData.id;
    } catch (error) {
      console.error('Invalid user data in localStorage:', error);
    }
  }
  return null;
};

// Include user ID and JWT token in headers for session tracking
const authHeaders = () => {
  const userId = getUserId();
  const token = localStorage.getItem('token');
  const headers = {};
  if (userId) headers['X-User-Id'] = userId;
  if (token) headers['Authorization'] = `Bearer ${token}`;
  return headers;
};

// Get user session ID from localStorage
const getUserSessionId = () => {
  const user = localStorage.getItem('user');
  if (user) {
    try {
      const userData = JSON.parse(user);
      return userData.sessionId;
    } catch (error) {
      console.error('Invalid user data in localStorage:', error);
    }
  }
  return null;
};



// Upload resume file
export const uploadResume = async (file, role, userId) => {
  const formData = new FormData();
  formData.append('file', file);
  if (role) {
    formData.append('role', role);
  }
  if (userId != null) {
    formData.append('userId', userId);
  }

  const response = await axios.post(`${API_BASE_URL}/resume/upload`, formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
      ...authHeaders()
    }
  });

  return response;
};

// Update resume data
export const updateResume = async (arg1, arg2) => {
  let id;
  let payload;
  if (typeof arg1 === 'object' && arg1 !== null && 'id' in arg1) {
    id = arg1.id;
    payload = arg1;
  } else {
    id = arg1;
    payload = arg2 || {};
  }
  const response = await axios.put(`${API_BASE_URL}/resume/${id}`, payload);
  return response.data;
};

// Fetch resume data for a session
export const fetchResumeData = async (sessionToken) => {
  const response = await axios.get(`${API_BASE_URL}/resume/session/token/${sessionToken}`, {
    headers: authHeaders()
  });
  return response.data;
};

// Get all resume data for user
export const getUserResumeData = async (userId) => {
  const response = await axios.get(`${API_BASE_URL}/resume/user/${userId}`, {
    headers: authHeaders()
  });
  return response.data;
};

// Delete resume data
export const deleteResumeData = async (resumeId) => {
  const response = await axios.delete(`${API_BASE_URL}/resume/${resumeId}`);
  return response.data;
};
