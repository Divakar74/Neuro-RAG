import axios from 'axios';

const API_BASE = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

export const generateSuggestions = async ({ apiKey, resumeData, cognitiveBiases, sessionSummary }) => {
  try {
    const prompt = buildSuggestionsPrompt(resumeData, cognitiveBiases, sessionSummary);

    const response = await axios.post(`${API_BASE}/ai/generate-suggestions`, {
      prompt,
      apiKey
    }, {
      headers: {
        'Content-Type': 'application/json'
      }
    });

    return response.data.suggestions;
  } catch (error) {
    console.error('Error generating suggestions:', error);
    throw error;
  }
};

const buildSuggestionsPrompt = (resumeData, cognitiveBiases, sessionSummary) => {
  return `You are an AI career coach analyzing a user's skill assessment results. Based on the following information, provide personalized career development suggestions.

USER RESUME DATA:
${resumeData ? JSON.stringify(resumeData, null, 2) : 'No resume data available'}

COGNITIVE ANALYSIS:
${cognitiveBiases && cognitiveBiases.length > 0 ? cognitiveBiases.map(b => `- ${b.type}: ${b.description}`).join('\n') : 'No cognitive analysis available'}

SESSION SUMMARY:
${sessionSummary ? JSON.stringify(sessionSummary, null, 2) : 'No session summary available'}

Please provide 3-5 specific, actionable suggestions for career development. Focus on:
1. Skill gaps that need immediate attention
2. Learning resources and courses
3. Career progression opportunities
4. Networking and professional development

Format your response as a numbered list with clear, concise recommendations.`;
};
