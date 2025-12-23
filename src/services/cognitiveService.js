import axios from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

// Fetch cognitive bias analysis for a session
export const fetchCognitiveAnalysis = async (sessionToken) => {
  const response = await axios.get(`${API_BASE_URL}/cognitive/bias-analysis/${sessionToken}`);
  return response.data;
};

// Fetch cognitive bias analysis by session ID
export const fetchCognitiveAnalysisBySessionId = async (sessionId) => {
  try {
    // First try the existing endpoint
    const response = await axios.get(`${API_BASE_URL}/cognitive/bias-analysis/session/${sessionId}`);
    return response.data;
  } catch (error) {
    // Fallback to user data persistence endpoint for historical data
    try {
      const fallbackResponse = await axios.get(`${API_BASE_URL}/user-data/session/${sessionId}/cognitive-analysis`);
      return fallbackResponse.data;
    } catch (fallbackError) {
      console.warn('Both cognitive analysis endpoints failed:', fallbackError);
      throw error; // Throw original error
    }
  }
};
