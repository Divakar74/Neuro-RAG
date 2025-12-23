
import axios from 'axios';

const API_BASE = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

// Authentication headers with JWT token
const authHeaders = () => {
  const token = localStorage.getItem('token');
  if (token) {
    return { 'Authorization': `Bearer ${token}` };
  }
  return {};
};

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

export const fetchAllQuestions = () => {
  return axios.get(`${API_BASE}/questions`, { headers: { ...authHeaders() } });
};

export const fetchQuestionById = (id) => {
  return axios.get(`${API_BASE}/questions/${id}`, { headers: { ...authHeaders() } });
};

export const fetchNextQuestion = (sessionToken) => {
  return axios.get(`${API_BASE}/questions/next/${sessionToken}`, { headers: { ...authHeaders() } });
};

export const fetchAssessmentProgress = (sessionToken) => {
  return axios.get(`${API_BASE}/questions/progress/${sessionToken}`, { headers: { ...authHeaders() } });
};

export const fetchRecommendedQuestions = (sessionToken, count = 5) => {
  return axios.get(`${API_BASE}/questions/recommended/${sessionToken}?count=${count}`, { headers: { ...authHeaders() } });
};

export const fetchStoppingCriteriaStatus = (sessionToken) => {
  return axios.get(`${API_BASE}/questions/stopping-criteria/${sessionToken}`, { headers: { ...authHeaders() } });
};

export const submitResponse = (response, sessionToken = null) => {
  const params = sessionToken ? `?sessionToken=${sessionToken}` : '';
  const userId = getUserId();
  return axios.post(`${API_BASE}/responses${params}`, response, {
    headers: { 'Content-Type': 'application/json', 'X-User-Id': userId, ...authHeaders() }
  });
};

export const startAssessment = (data) => {
  const params = new URLSearchParams();
  if (data.role) params.append('targetRole', data.role);
  return axios.post(`${API_BASE}/assessment/start?${params.toString()}`, null, { headers: { ...authHeaders() } });
};

export const getSession = (token) => {
  return axios.get(`${API_BASE}/assessment/session/${token}`, { headers: { ...authHeaders() } });
};

export const completeAssessment = (token) => {
  return axios.post(`${API_BASE}/assessment/complete/${token}`, null, { headers: { ...authHeaders() } });
};

export const fetchAISuggestions = (sessionId) => {
  return axios.get(`${API_BASE}/ai/suggestions/${sessionId}`, { headers: { ...authHeaders() } });
};

export const fetchFeedback = (sessionId) => {
  return axios.get(`${API_BASE}/feedback/${sessionId}`, { headers: { ...authHeaders() } });
};

export const fetchSessionResponses = (sessionId) => {
  return axios.get(`${API_BASE}/responses/session/${sessionId}`, { headers: { ...authHeaders() } });
};

export const fetchSessionResponsesByToken = (sessionToken) => {
  return axios.get(`${API_BASE}/responses/session/token/${sessionToken}`, { headers: { ...authHeaders() } });
};

export const getUserSessions = () => {
  // User ID is now handled by JWT authentication on the backend
  return axios.get(`${API_BASE}/assessment/user/sessions`, { headers: { ...authHeaders() } });
};

export const getUserQuestionsHistory = (userId) => {
  return axios.get(`${API_BASE}/questions/user/${userId}`, { headers: { ...authHeaders() } });
};

export const getUserAssessmentProgress = (userId) => {
  return axios.get(`${API_BASE}/assessment/user/progress/${userId}`, { headers: { ...authHeaders() } });
};

export const getUserSkillGaps = (userId) => {
  return axios.get(`${API_BASE}/assessment/user/skill-gaps/${userId}`, { headers: { ...authHeaders() } });
};
