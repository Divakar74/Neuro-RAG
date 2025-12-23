import axios from 'axios';

const API_URL = '/api/auth';

export const loginUser = async (email, password) => {
  try {
    const response = await axios.post(\`\${API_URL}/login\`, { email, password });
    return response.data;
  } catch (error) {
    throw new Error('Invalid email or password');
  }
};

export const registerUser = async (userData) => {
  try {
    const response = await axios.post(\`\${API_URL}/register\`, userData);
    return response.data;
  } catch (error) {
    throw new Error('Registration failed');
  }
};
