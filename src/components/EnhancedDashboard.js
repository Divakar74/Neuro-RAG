import React, { useState, useEffect } from 'react';
import { useUser } from '../contexts/UserContext';
import { 
  Brain, 
  FileText, 
  TrendingUp, 
  Award, 
  Target, 
  Star, 
  ShieldCheck, 
  Download, 
  LogOut,
  Eye,
  AlertTriangle,
  CheckCircle,
  Clock,
  BarChart3,
  Lightbulb,
  User,
  Briefcase,
  GraduationCap
} from 'lucide-react';
import { exportMyData } from '../services/privacyService';
import { fetchAssessmentProgress, getUserSessions, fetchSessionResponsesByToken, getUserQuestionsHistory } from '../services/assessmentService';
import { fetchResumeData, getUserResumeData } from '../services/resumeService';
import { fetchCognitiveAnalysis } from '../services/cognitiveService';
import SkillRadarChart from './SkillRadarChart';
import SkillGraph from './SkillGraph';
import LevelBadge from './LevelBadge';
import FeedbackCard from './FeedbackCard';
import CognitiveBiasCard from './CognitiveBiasCard';

import SkillGapAnalysis from './SkillGapAnalysis';
import RoadmapTimeline from './RoadmapTimeline';
import FreeResourcesCard from './FreeResourcesCard';

import { useRoadmap } from '../hooks/useRoadmap';
import { getUserSkillGaps } from '../services/assessmentService';

const EnhancedDashboard = ({ userId, sessionId, skillDependencies, loading = false, skillData, onStartNewAssessment, onLogout, onBackToLanding }) => {
  const { user, logout } = useUser();
  const [animationDelay, setAnimationDelay] = useState(0);
  const [progress, setProgress] = useState(null);
  const [derivedSkillData, setDerivedSkillData] = useState(skillData || []);
  const [isLoading, setIsLoading] = useState(loading);
  const [resumeData, setResumeData] = useState(null);
  const [cognitiveBiasData, setCognitiveBiasData] = useState([]);
  const [sessions, setSessions] = useState([]);
  const [selectedSession, setSelectedSession] = useState(null);
  const [selectedSessionDbId, setSelectedSessionDbId] = useState(null);
  const [userQuestionsHistory, setUserQuestionsHistory] = useState([]);
  const [userSkillGaps, setUserSkillGaps] = useState([]);

  useEffect(() => {
    const timer = setTimeout(() => setAnimationDelay(1), 100);
    return () => clearTimeout(timer);
  }, []);

  useEffect(() => {
    const loadData = async () => {
      if (!sessionId && !userId && !user) return;
      setIsLoading(true);
      try {
        // Load user resume data if userId is provided (moved up to load first)
        if (userId) {
          try {
            const resumeRes = await getUserResumeData(userId);
            if (resumeRes && resumeRes.length > 0) {
              // Use the most recent resume data
              setResumeData(resumeRes[0]);
            } else {
              console.log('No resume data available for user');
            }
          } catch (error) {
            console.log('No resume data available for user');
          }

          // Load user sessions
          const sessionsRes = await getUserSessions(userId);
          const list = sessionsRes.data || [];
          setSessions(list);
          if (sessionId) {
            setSelectedSession(sessionId);
            const matched = list.find(s => s.sessionToken === sessionId);
            setSelectedSessionDbId(matched?.id || null);
          }

          // Load user questions history across all sessions
          try {
            const historyRes = await getUserQuestionsHistory(userId);
            setUserQuestionsHistory(historyRes.data || []);
          } catch (error) {
            console.log('No questions history available for user');
            setUserQuestionsHistory([]);
          }

          // Load user skill gaps for personalized recommendations
          try {
            const skillGapsRes = await getUserSkillGaps(userId);
            setUserSkillGaps(skillGapsRes.data || []);
          } catch (error) {
            console.log('No skill gaps data available for user');
            setUserSkillGaps([]);
          }
        }

        // Load assessment progress if sessionId exists
        if (sessionId) {
          const progressRes = await fetchAssessmentProgress(sessionId);
          setProgress(progressRes.data);

          // Derive skills array from confidence levels
          const levels = progressRes.data?.skillConfidenceLevels || {};
          const derived = Object.entries(levels).map(([code, confidence]) => ({
            name: code.replace('_', ' '),
            level: Math.max(1, Math.min(5, Math.round((confidence || 0) * 5) || 1)),
            confidence: confidence
          }));
          setDerivedSkillData(derived);

          // Load cognitive bias data
          try {
            const cognitiveRes = await fetchCognitiveAnalysis(sessionId);
            setCognitiveBiasData(cognitiveRes);
          } catch (error) {
            console.log('No cognitive analysis data available');
            setCognitiveBiasData([]);
          }
        }

      } finally {
        setIsLoading(false);
      }
    };
    loadData();
  }, [sessionId, userId, user]);

  // Roadmap hook uses numeric session DB id
  const { roadmap, loading: roadmapLoading, generateRoadmap } = useRoadmap(selectedSessionDbId);

  if (isLoading) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-slate-50 via-blue-50 to-indigo-50 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-16 w-16 border-b-2 border-indigo-600 mx-auto mb-4"></div>
          <h2 className="text-2xl font-semibold text-gray-700 mb-2">Loading Your Dashboard</h2>
          <p className="text-gray-500">Analyzing your skills and preparing insights...</p>
        </div>
      </div>
    );
  }

  if (!derivedSkillData || derivedSkillData.length === 0) {
    // Show welcome message for new users or users without current session data
    // But if they have historical data, show a different message
    const hasHistory = userQuestionsHistory.length > 0 || sessions.length > 0;

    return (
      <div className="min-h-screen brand-gradient flex items-center justify-center">
        <div className="text-center max-w-md">
          <Award className="w-20 h-20 mx-auto mb-6" style={{color:'var(--brand-700)'}} />
          <h2 className="text-3xl font-bold text-gray-700 mb-4">
            {hasHistory ? 'Welcome Back to Your Skill Journey' : 'Welcome to Your Skill Journey'}
          </h2>
          <p className="text-gray-500 mb-8 text-lg">
            {hasHistory
              ? 'Continue building on your assessment history. Start a new session to further develop your skills.'
              : 'Upload your resume and complete your skill assessment to unlock personalized insights, cognitive analysis, and tailored recommendations.'
            }
          </p>
          {hasHistory && (
            <div className="mb-6 p-4 bg-white rounded-lg shadow-sm">
              <p className="text-sm text-gray-600 mb-2">Your Progress:</p>
              <div className="flex justify-between text-sm">
                <span>Sessions: {sessions.length}</span>
                <span>Questions: {userQuestionsHistory.length}</span>
              </div>
            </div>
          )}
          <div className="space-y-4">
            <button
              onClick={() => onStartNewAssessment && onStartNewAssessment(progress?.targetRole)}
              className="w-full text-white px-8 py-4 rounded-xl transition-all duration-300 font-semibold text-lg shadow-lg hover:shadow-xl brand-cta-gradient"
            >
              {hasHistory ? 'Continue Assessment' : 'Upload Resume & Start Assessment'}
            </button>
            <button
              onClick={() => onStartNewAssessment && onStartNewAssessment(progress?.targetRole)}
              className="w-full bg-white text-indigo-600 border border-indigo-600 px-8 py-4 rounded-xl transition-all duration-300 font-semibold text-lg shadow-lg hover:bg-indigo-50"
            >
              Start Assessment Only
            </button>
          </div>
        </div>
      </div>
    );
  }

  const averageLevel = derivedSkillData.reduce((sum, skill) => sum + skill.level, 0) / derivedSkillData.length;
  const topSkills = derivedSkillData.filter(skill => skill.level >= 3);
  const growthAreas = derivedSkillData.filter(skill => skill.level <= 2);

  return (
    <div className="min-h-screen brand-gradient">
      {/* Header */}
      <div className="bg-white shadow-sm border-b border-gray-200">
        <div className="max-w-7xl mx-auto px-6 py-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-4">
              <div className="bg-gradient-to-r from-indigo-600 to-purple-600 p-2 rounded-lg">
                <Brain className="w-6 h-6 text-white" />
              </div>
              <div>
                <h1 className="text-2xl font-bold text-gray-900">Skill Assessment Dashboard</h1>
                <p className="text-gray-600">Welcome back, {user?.email?.split('@')[0] || 'User'}!</p>
              </div>
            </div>
            <div className="flex items-center space-x-3">
              {/* AI settings hidden per request */}
              <button
                onClick={() => {
                  if (onLogout) {
                    onLogout();
                  } else {
                    logout();
                    if (onBackToLanding) onBackToLanding();
                  }
                }}
                className="bg-red-100 hover:bg-red-200 text-red-700 px-4 py-2 rounded-lg transition flex items-center space-x-2"
              >
                <LogOut className="w-4 h-4" />
                <span>Logout</span>
              </button>
            </div>
          </div>
        </div>
      </div>

      <div className="max-w-7xl mx-auto px-6 py-8">
        {/* Quick Stats */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
          <div className="bg-white rounded-xl shadow-lg p-6 hover:shadow-xl transition-all duration-300">
            <div className="flex items-center">
              <div className="bg-blue-100 p-3 rounded-lg">
                <TrendingUp className="w-6 h-6 text-blue-600" />
              </div>
              <div className="ml-4">
                <p className="text-sm font-medium text-gray-600">Average Skill Level</p>
                <p className="text-2xl font-bold text-gray-900">{averageLevel.toFixed(1)}/5</p>
              </div>
            </div>
          </div>

          <div className="bg-white rounded-xl shadow-lg p-6 hover:shadow-xl transition-all duration-300">
            <div className="flex items-center">
              <div className="bg-green-100 p-3 rounded-lg">
                <Award className="w-6 h-6 text-green-600" />
              </div>
              <div className="ml-4">
                <p className="text-sm font-medium text-gray-600">Strengths</p>
                <p className="text-2xl font-bold text-gray-900">{topSkills.length}</p>
              </div>
            </div>
          </div>

          <div className="bg-white rounded-xl shadow-lg p-6 hover:shadow-xl transition-all duration-300">
            <div className="flex items-center">
              <div className="bg-orange-100 p-3 rounded-lg">
                <Target className="w-6 h-6 text-orange-600" />
              </div>
              <div className="ml-4">
                <p className="text-sm font-medium text-gray-600">Growth Areas</p>
                <p className="text-2xl font-bold text-gray-900">{growthAreas.length}</p>
              </div>
            </div>
          </div>

          <div className="bg-white rounded-xl shadow-lg p-6 hover:shadow-xl transition-all duration-300">
            <div className="flex items-center">
          <div className="bg-purple-100 p-3 rounded-lg">
                <BarChart3 className="w-6 h-6 text-purple-600" />
              </div>
              <div className="ml-4">
                <p className="text-sm font-medium text-gray-600">Total Questions Answered</p>
                <p className="text-2xl font-bold text-gray-900">{userQuestionsHistory.length}</p>
              </div>
            </div>
          </div>
        </div>

        {/* Main Content Grid */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 mb-8">
          {/* Left Column - Charts and Analysis */}
          <div className="lg:col-span-2 space-y-8">


            {/* Skill Radar Chart */}
            <div className="bg-white rounded-xl shadow-lg p-6 hover:shadow-xl transition-all duration-300">
              <SkillRadarChart skillData={derivedSkillData} />
            </div>

            {/* Skill Graph */}
            <div className="bg-white rounded-xl shadow-lg p-6 hover:shadow-xl transition-all duration-300">
              <SkillGraph skillData={derivedSkillData} skillDependencies={skillDependencies} />
            </div>

            {/* Skill Gap Analysis */}
            <SkillGapAnalysis 
              skillData={derivedSkillData} 
              targetRole={progress?.targetRole}
              sessionId={sessionId}
              resumeData={resumeData}
            />
          </div>

          {/* Right Column - Insights and Actions */}
          <div className="space-y-6">
            {/* Cognitive Bias Analysis */}
            {cognitiveBiasData.length > 0 && (
              <div className="space-y-4">
                <h3 className="text-xl font-semibold text-gray-800 flex items-center">
                  <Brain className="w-5 h-5 mr-2 text-indigo-600" />
                  Cognitive Analysis
                </h3>
                {cognitiveBiasData.map((bias, index) => (
                  <CognitiveBiasCard key={index} bias={bias} />
                ))}
              </div>
            )}



            {/* AI Feedback */}
            <div className="bg-transparent">
              <FeedbackCard sessionId={selectedSessionDbId} />
            </div>

            {/* Free Learning Resources */}
            <FreeResourcesCard skillGaps={userSkillGaps} userId={userId} />

            {/* Quick Actions */}
            <div className="bg-white rounded-xl shadow-lg p-6">
              <h3 className="text-lg font-semibold text-gray-800 mb-4">Quick Actions</h3>
              <div className="space-y-3">
                <button
                  onClick={() => onStartNewAssessment && onStartNewAssessment(progress?.targetRole)}
                  className="w-full bg-indigo-600 hover:bg-indigo-700 text-white px-4 py-3 rounded-lg transition flex items-center justify-center space-x-2"
                >
                  <Target className="w-4 h-4" />
                  <span>Start New Assessment</span>
                </button>
                <button
                  onClick={async () => {
                    try {
                      const res = await exportMyData();
                      const url = window.URL.createObjectURL(new Blob([res.data], { type: 'application/json' }));
                      const link = document.createElement('a');
                      link.href = url;
                      link.setAttribute('download', 'neuro-rag-export.json');
                      document.body.appendChild(link);
                      link.click();
                      link.remove();
                      window.URL.revokeObjectURL(url);
                    } catch {}
                  }}
                  className="w-full bg-gray-50 hover:bg-gray-100 text-gray-700 px-4 py-3 rounded-lg transition flex items-center justify-center space-x-2"
                >
                  <Download className="w-4 h-4" />
                  <span>Export My Data</span>
                </button>
              </div>
            </div>

            {/* Roadmap */}
            <div className="bg-white rounded-xl shadow-lg p-6">
              <h3 className="text-lg font-semibold text-gray-800 mb-4">Your Roadmap</h3>
              {roadmap ? (
                <RoadmapTimeline roadmapData={roadmap} />
              ) : (
                <div>
                  <p className="text-gray-600 mb-4">Generate a personalized learning roadmap based on this session.</p>
                  <button
                    disabled={roadmapLoading || !selectedSessionDbId}
                    onClick={() => generateRoadmap([])}
                    className="w-full bg-gray-50 hover:bg-gray-100 text-gray-700 px-4 py-3 rounded-lg transition disabled:opacity-50"
                  >
                    {roadmapLoading ? 'Generating...' : 'Generate Roadmap'}
                  </button>
                </div>
              )}
            </div>

            {/* Session History */}
            {sessions.length > 0 && (
              <div className="bg-white rounded-xl shadow-lg p-6">
                <h3 className="text-lg font-semibold text-gray-800 mb-4">Assessment History</h3>
                <div className="space-y-2">
                  {sessions.slice(0, 3).map((session) => (
                    <div
                      key={session.id}
                      className={`p-3 rounded-lg cursor-pointer transition ${
                        selectedSession === session.sessionToken
                          ? 'bg-indigo-50 border border-indigo-200'
                          : 'bg-gray-50 hover:bg-gray-100'
                      }`}
                      onClick={() => setSelectedSession(session.sessionToken)}
                    >
                      <div className="flex items-center justify-between">
                        <span className="text-sm font-medium text-gray-700">
                          {session.targetRole || 'General Assessment'}
                        </span>
                        <span className="text-xs text-gray-500">
                          {new Date(session.startedAt).toLocaleDateString()}
                        </span>
                      </div>
                      <div className="text-xs text-gray-500 mt-1">
                        {session.questionsAsked} questions • {session.status}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Historical Questions Summary */}
            {userQuestionsHistory.length > 0 && (
              <div className="bg-white rounded-xl shadow-lg p-6">
                <h3 className="text-lg font-semibold text-gray-800 mb-4">Your Assessment Journey</h3>
                <div className="space-y-3">
                  <div className="flex items-center justify-between text-sm">
                    <span className="text-gray-600">Total Sessions Completed</span>
                    <span className="font-semibold text-gray-900">{sessions.filter(s => s.status === 'completed').length}</span>
                  </div>
                  <div className="flex items-center justify-between text-sm">
                    <span className="text-gray-600">Questions Across All Sessions</span>
                    <span className="font-semibold text-gray-900">{userQuestionsHistory.length}</span>
                  </div>
                  <div className="flex items-center justify-between text-sm">
                    <span className="text-gray-600">Average Questions per Session</span>
                    <span className="font-semibold text-gray-900">
                      {sessions.length > 0 ? Math.round(userQuestionsHistory.length / sessions.length) : 0}
                    </span>
                  </div>
                  <div className="mt-4 p-3 bg-indigo-50 rounded-lg">
                    <p className="text-sm text-indigo-700">
                      You've been consistently working on your skills! Your historical data shows progress across multiple assessment sessions.
                    </p>
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default EnhancedDashboard;
