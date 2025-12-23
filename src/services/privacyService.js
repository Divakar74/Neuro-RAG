import axios from 'axios';

const API_BASE = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

export const exportMyData = async () => {
  try {
    const token = localStorage.getItem('token');
    const res = await axios.get(`${API_BASE}/privacy/export`, {
      responseType: 'blob',
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });
    return res;
  } catch (error) {
    console.error('Export failed:', error);
    throw error;
  }
};















