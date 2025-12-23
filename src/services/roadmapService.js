import axios from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

export const getRoadmap = async (sessionId) => {
  try {
    // First try the existing endpoint
    const response = await axios.get(`${API_BASE_URL}/roadmap/session/${sessionId}`);
    return response;
  } catch (error) {
    // Fallback to user data persistence - check if session has roadmap data
    try {
      const userDataResponse = await axios.get(`${API_BASE_URL}/user-data/session/${sessionId}/cognitive-analysis`);
      if (userDataResponse.data && userDataResponse.data.roadmapData) {
        return { data: userDataResponse.data.roadmapData };
      }
      throw error; // No roadmap data in user data
    } catch (fallbackError) {
      console.warn('Both roadmap endpoints failed:', fallbackError);
      throw error; // Throw original error
    }
  }
};

export const generateRoadmap = async (sessionId, skillGaps) => {
  const response = await axios.post(`${API_BASE_URL}/roadmap/generate-analysis`, {
    sessionId,
  });
  return response;
};

export const updateMilestoneProgress = async (sessionId, milestoneId, completed) => {
  const response = await axios.put(`${API_BASE_URL}/roadmap/${sessionId}/milestone/${milestoneId}`, {
    completed,
  });
  return response;
};

export const getRecommendedResources = async (skillId) => {
  const response = await axios.get(`${API_BASE_URL}/resources/skill/${skillId}`);
  return response;
};

export const getProjects = async (skillId) => {
  const response = await axios.get(`${API_BASE_URL}/projects/skill/${skillId}`);
  return response;
};
