import React, { useState, useEffect } from 'react';
import { Brain, Sparkles, Loader2, RefreshCw, MapPin, BookOpen, Target, TrendingUp, Award, Clock } from 'lucide-react';
import { fetchFeedback } from '../services/assessmentService';
import { generateSuggestions } from '../services/openaiService';
import SkillGraph from './SkillGraph';

const AISuggestionsCard = ({ session, resumeData, cognitiveBiases, sessionSummary, roadmapData, skillGaps, onStartAssessment }) => {
  const [suggestions, setSuggestions] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [activeTab, setActiveTab] = useState('insights');

  const loadSuggestions = async () => {
    if (!session || !session.id) return;

    setLoading(true);
    setError(null);
    try {
      // Check if we have cached suggestions first to avoid token waste
      const cacheKey = `ai_suggestions_${session.id}`;
      const cached = localStorage.getItem(cacheKey);
      if (cached) {
        const cacheData = JSON.parse(cached);
        // Check if cache is still valid (24 hours)
        if (Date.now() - cacheData.timestamp < 24 * 60 * 60 * 1000) {
          setSuggestions(cacheData.suggestions);
          setLoading(false);
          return;
        }
      }

      // For user data persistence, try to fetch historical AI feedback first
      try {
        // Check if session has AI feedback from backend
        const feedbackResponse = await fetch(`/api/user-data/session/${session.id}/feedback`);
        if (feedbackResponse.ok) {
          const feedbackData = await feedbackResponse.json();
          if (feedbackData && feedbackData.length > 0) {
            // Use the most recent AI feedback
            const aiFeedback = feedbackData.find(f => f.feedbackType === 'AI_SUGGESTIONS');
            if (aiFeedback) {
              setSuggestions(aiFeedback.content);
              localStorage.setItem(cacheKey, JSON.stringify({
                suggestions: aiFeedback.content,
                timestamp: Date.now()
              }));
              setLoading(false);
              return;
            }
          }
        }
      } catch (feedbackError) {
        console.warn('Could not fetch historical AI feedback:', feedbackError);
      }

      // Try backend Neuro-RAG feedback first (cached on backend)
      try {
        const response = await fetchFeedback(session.id);
        setSuggestions(response.data);
        // Cache locally for future use
        localStorage.setItem(cacheKey, JSON.stringify({
          suggestions: response.data,
          timestamp: Date.now()
        }));
      } catch (e) {
        // Fallback to client-side OpenAI only if backend fails
        const apiKey = localStorage.getItem('openai_api_key') || process.env.REACT_APP_OPENAI_API_KEY;
        if (apiKey) {
          const text = await generateSuggestions({ apiKey, resumeData, cognitiveBiases, sessionSummary });
          setSuggestions(text);
          // Cache locally
          localStorage.setItem(cacheKey, JSON.stringify({
            suggestions: text,
            timestamp: Date.now()
          }));
        } else {
          throw e;
        }
      }
    } catch (err) {
      console.error('Error fetching AI suggestions:', err);
      setError('Unable to load AI suggestions at this time. Please try again later.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadSuggestions();
  }, [session]);

  const formatSuggestions = (text) => {
    if (!text) return [];

    // Split by numbered points or bullet points
    const points = text.split(/\d+\.\s+|•\s+/).filter(point => point.trim());

    return points.map((point, index) => ({
      id: index,
      text: point.trim()
    }));
  };

  const getRoadmapInsights = () => {
    if (!roadmapData || !roadmapData.phases) return null;

    const totalWeeks = roadmapData.totalDuration || 0;
    const totalMilestones = roadmapData.totalMilestones || 0;
    const nextPhase = roadmapData.phases[0];

    return {
      totalWeeks,
      totalMilestones,
      nextPhase: nextPhase?.title || 'Next Phase',
      nextMilestone: nextPhase?.milestones?.[0]?.title || 'First Milestone'
    };
  };

  const getSkillGapInsights = () => {
    if (!skillGaps || skillGaps.length === 0) return null;

    const criticalGaps = skillGaps.filter(gap => gap.priority > 0.7);
    const topGap = skillGaps[0];

    return {
      criticalCount: criticalGaps.length,
      topGap: topGap?.skill || 'Top Skill Gap',
      averageGap: skillGaps.reduce((sum, gap) => sum + gap.gap, 0) / skillGaps.length
    };
  };

  const buildSkillDependenciesFromRoadmap = () => {
    if (!roadmapData || !roadmapData.phases) return {};

    const dependencies = {};
    const skillLevels = {};

    // Extract skills from roadmap phases and assign levels based on phase order
    roadmapData.phases.forEach((phase, phaseIndex) => {
      const phaseLevel = phaseIndex + 1; // Phase 1 = level 1, Phase 2 = level 2, etc.

      phase.milestones?.forEach(milestone => {
        const skillName = milestone.title;
        skillLevels[skillName] = phaseLevel;

        // Create dependencies: later phases depend on earlier phases
        if (phaseIndex > 0) {
          const prevPhase = roadmapData.phases[phaseIndex - 1];
          prevPhase.milestones?.forEach(prevMilestone => {
            const prevSkill = prevMilestone.title;
            if (!dependencies[prevSkill]) dependencies[prevSkill] = [];
            if (!dependencies[prevSkill].includes(skillName)) {
              dependencies[prevSkill].push(skillName);
            }
          });
        }
      });
    });

    return dependencies;
  };

  const buildSkillDataFromRoadmap = () => {
    if (!roadmapData || !roadmapData.phases) return [];

    const skills = [];
    const skillSet = new Set();

    roadmapData.phases.forEach((phase, phaseIndex) => {
      const phaseLevel = phaseIndex + 1;

      phase.milestones?.forEach(milestone => {
        const skillName = milestone.title;
        if (!skillSet.has(skillName)) {
          skillSet.add(skillName);
          skills.push({
            name: skillName,
            level: Math.min(5, phaseLevel), // Cap at level 5
            confidence: 0.8 // Default confidence
          });
        }
      });
    });

    return skills;
  };

  const roadmapInsights = getRoadmapInsights();
  const skillGapInsights = getSkillGapInsights();

  return (
    <div className="bg-white/95 backdrop-blur rounded-xl shadow-lg p-6 hover:shadow-xl transition-all duration-300 ring-1 ring-slate-200">
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-3">
          <div className="bg-indigo-100 p-2 rounded-lg">
            <Brain className="w-5 h-5 text-indigo-600" />
          </div>
          <div>
            <h3 className="text-lg font-semibold text-slate-900 tracking-tight">AI Career Insights</h3>
            <p className="text-sm text-slate-600">Personalized suggestions powered by AI</p>
          </div>
        </div>
        <button
          onClick={loadSuggestions}
          disabled={loading}
          className="p-2 text-gray-400 hover:text-purple-600 transition-colors disabled:opacity-50"
          title="Refresh suggestions"
        >
          <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
        </button>
      </div>

      {/* Tab Navigation */}
      <div className="flex space-x-1 mb-4 bg-gray-100 p-1 rounded-lg">
        <button
          onClick={() => setActiveTab('insights')}
          className={`flex-1 py-2 px-3 text-sm font-medium rounded-md transition-colors ${
            activeTab === 'insights'
              ? 'bg-white text-indigo-700 shadow-sm'
              : 'text-gray-600 hover:text-gray-900'
          }`}
        >
          Career Insights
        </button>
        <button
          onClick={() => setActiveTab('roadmap')}
          className={`flex-1 py-2 px-3 text-sm font-medium rounded-md transition-colors ${
            activeTab === 'roadmap'
              ? 'bg-white text-indigo-700 shadow-sm'
              : 'text-gray-600 hover:text-gray-900'
          }`}
        >
          Visual Roadmap
        </button>
      </div>

      {loading && (
        <div className="flex items-center justify-center py-8">
          <Loader2 className="w-6 h-6 animate-spin text-purple-600 mr-2" />
          <span className="text-gray-600">Generating personalized insights...</span>
        </div>
      )}

      {error && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4">
          <p className="text-red-700 text-sm">{error}</p>
        </div>
      )}

      {!loading && !error && (
        <>
          {/* Career Insights Tab */}
          {activeTab === 'insights' && (
            <div className="space-y-3">
              <div className="flex items-center gap-2 mb-3">
                <Sparkles className="w-4 h-4 text-purple-600" />
                <span className="text-sm font-medium text-purple-700">Your Personalized Recommendations</span>
              </div>

              {formatSuggestions(suggestions).map((suggestion) => (
                <div
                  key={suggestion.id}
                  className="flex items-start gap-3 p-3 bg-purple-50 rounded-lg border border-purple-100 hover:bg-purple-100 transition-colors"
                >
                  <div className="flex-shrink-0 w-6 h-6 bg-purple-600 text-white rounded-full flex items-center justify-center text-xs font-semibold mt-0.5">
                    {suggestion.id + 1}
                  </div>
                  <p className="text-sm text-gray-700 leading-relaxed">{suggestion.text}</p>
                </div>
              ))}

              {formatSuggestions(suggestions).length === 0 && (
                <p className="text-gray-600 text-sm italic">{suggestions}</p>
              )}
            </div>
          )}

          {/* Visual Roadmap Tab */}
          {activeTab === 'roadmap' && (
            <div className="space-y-4">
              {roadmapData && roadmapData.phases ? (
                <div className="space-y-6">
                  {/* Skill Dependency Graph */}
                  <div className="bg-gradient-to-br from-indigo-50 to-purple-50 p-4 rounded-lg">
                    <h4 className="font-medium text-indigo-900 mb-4 flex items-center">
                      <Target className="w-5 h-5 mr-2" />
                      Skill Dependency Graph
                    </h4>
                    <div className="bg-white rounded-lg p-2" style={{ height: '400px' }}>
                      <SkillGraph
                        skillData={buildSkillDataFromRoadmap()}
                        skillDependencies={buildSkillDependenciesFromRoadmap()}
                      />
                    </div>
                    <p className="text-xs text-indigo-600 mt-2">
                      Interactive graph showing skill relationships and learning progression
                    </p>
                  </div>

                  {/* Traditional Timeline View */}
                  <div className="bg-gradient-to-br from-purple-50 to-pink-50 p-4 rounded-lg">
                    <h4 className="font-medium text-purple-900 mb-4 flex items-center">
                      <MapPin className="w-5 h-5 mr-2" />
                      Learning Timeline
                    </h4>

                    {/* Visual Timeline */}
                    <div className="space-y-4">
                      {roadmapData.phases.map((phase, phaseIndex) => (
                        <div key={phaseIndex} className="relative">
                          {/* Phase Header */}
                          <div className="flex items-center mb-3">
                            <div className="flex items-center justify-center w-8 h-8 bg-gradient-to-br from-purple-600 to-pink-600 text-white rounded-full font-bold shadow-lg mr-3">
                              {phaseIndex + 1}
                            </div>
                            <div className="flex-1">
                              <h5 className="font-semibold text-gray-800">{phase.title}</h5>
                              <p className="text-sm text-gray-600">{phase.description}</p>
                              <div className="flex items-center mt-1 text-xs text-gray-500">
                                <Clock className="w-3 h-3 mr-1" />
                                <span>{phase.duration} weeks</span>
                              </div>
                            </div>
                          </div>

                          {/* Milestones */}
                          <div className="ml-11 space-y-2">
                            {phase.milestones?.slice(0, 2).map((milestone, milestoneIndex) => (
                              <div key={milestoneIndex} className="flex items-start">
                                <div className="flex items-center justify-center w-5 h-5 bg-emerald-500 text-white rounded-full text-xs font-bold mr-3 mt-0.5 shadow">
                                  ✓
                                </div>
                                <div className="flex-1">
                                  <p className="text-sm font-medium text-gray-800">{milestone.title}</p>
                                  <p className="text-xs text-gray-600">{milestone.description}</p>
                                </div>
                              </div>
                            ))}
                            {phase.milestones?.length > 2 && (
                              <div className="ml-8 text-xs text-gray-500">
                                +{phase.milestones.length - 2} more milestones
                              </div>
                            )}
                          </div>

                          {/* Phase separator */}
                          {phaseIndex < roadmapData.phases.length - 1 && (
                            <div className="ml-4 w-0.5 h-6 bg-gradient-to-b from-purple-200 to-pink-200"></div>
                          )}
                        </div>
                      ))}
                    </div>

                    {/* Completion Summary */}
                    <div className="mt-6 p-3 bg-white/70 rounded-lg border border-purple-200">
                      <div className="flex items-center justify-between">
                        <div className="flex items-center">
                          <Award className="w-4 h-4 text-yellow-500 mr-2" />
                          <span className="text-sm font-medium text-gray-800">Expected Completion</span>
                        </div>
                        <span className="text-sm font-bold text-purple-700">
                          {roadmapData.totalDuration} weeks • {roadmapData.totalMilestones} milestones
                        </span>
                      </div>
                    </div>
                  </div>
                </div>
              ) : (
                <div className="text-center py-8">
                  <MapPin className="w-12 h-12 text-gray-300 mx-auto mb-3" />
                  <p className="text-gray-500 text-sm">Generate a learning roadmap to see your personalized visual plan</p>
                </div>
              )}
            </div>
          )}
        </>
      )}

      {!loading && !error && !suggestions && (
        <div className="text-center py-8">
          <Brain className="w-12 h-12 text-gray-300 mx-auto mb-3" />
          <p className="text-gray-500 text-sm mb-4">Complete your assessment to receive AI-powered career insights</p>
          <button
            onClick={onStartAssessment}
            className="bg-indigo-600 hover:bg-indigo-700 text-white px-6 py-3 rounded-lg transition-colors font-semibold"
          >
            Start My Assessment
          </button>
        </div>
      )}
    </div>
  );
};

export default AISuggestionsCard;
