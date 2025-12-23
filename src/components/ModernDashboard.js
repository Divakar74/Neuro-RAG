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
  GraduationCap,
  Zap,
  Activity,
  BookOpen,
  ChevronRight,
  Sparkles
} from 'lucide-react';
import { exportMyData } from '../services/privacyService';
import { fetchAssessmentProgress, getUserSessions } from '../services/assessmentService';
import { fetchResumeData } from '../services/resumeService';
import { fetchCognitiveAnalysis } from '../services/cognitiveService';
import SkillRadarChart from './SkillRadarChart';
import SkillGraph from './SkillGraph';
import LevelBadge from './LevelBadge';
import FeedbackCard from './FeedbackCard';
import CognitiveBiasCard from './CognitiveBiasCard';
import ResumeAnalysisCard from './ResumeAnalysisCard';
import SkillGapAnalysis from './SkillGapAnalysis';

const ModernDashboard = ({ sessionId, skillDependencies, loading = false, skillData }) => {
  const { user, logout } = useUser();
  const [animationDelay, setAnimationDelay] = useState(0);
  const [progress, setProgress] = useState(null);
  const [derivedSkillData, setDerivedSkillData] = useState(skillData || []);
  const [isLoading, setIsLoading] = useState(loading);
  const [resumeData, setResumeData] = useState(null);
  const [cognitiveBiasData, setCognitiveBiasData] = useState([]);
  const [sessions, setSessions] = useState([]);
  const [selectedSession, setSelectedSession] = useState(null);
  const [activeTab, setActiveTab] = useState('overview');

  useEffect(() => {
    const timer = setTimeout(() => setAnimationDelay(1), 100);
    return () => clearTimeout(timer);
  }, []);

  useEffect(() => {
    const loadData = async () => {
      if (!sessionId) return;
      setIsLoading(true);
      try {
        // Load assessment progress
        const progressRes = await fetchAssessmentProgress(sessionId);
        setProgress(progressRes.data);
        
        // Derive skills array from confidence levels
        const levels = progressRes.data?.skillConfidenceLevels || {};
        const derived = Object.entries(levels).map(([code, confidence]) => ({
          name: code.replace('_', ' '),
          level: Math.max(1, Math.min(5, Math.round((confidence || 0) * 5) || 1)),
          confidence: confidence || 0
        }));
        setDerivedSkillData(derived);

        // Load resume data
        try {
          const resumeRes = await fetchResumeData(sessionId);
          setResumeData(resumeRes?.data || resumeRes);
        } catch (error) {
          console.log('No resume data available');
        }

        // Load user sessions
        const sessionsRes = await getUserSessions();
        setSessions(sessionsRes.data || []);
        setSelectedSession(sessionId);

        // Load cognitive bias data
        try {
          const cognitiveRes = await fetchCognitiveAnalysis(sessionId);
          setCognitiveBiasData(cognitiveRes);
        } catch (error) {
          console.log('No cognitive analysis data available');
          setCognitiveBiasData([]);
        }

      } finally {
        setIsLoading(false);
      }
    };
    loadData();
  }, [sessionId]);

  if (isLoading) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-slate-900 via-purple-900 to-slate-900 flex items-center justify-center">
        <div className="text-center">
          <div className="relative">
            <div className="w-20 h-20 border-4 border-purple-200 border-t-purple-600 rounded-full animate-spin mx-auto mb-6"></div>
            <Sparkles className="w-8 h-8 text-purple-400 absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 animate-pulse" />
          </div>
          <h2 className="text-2xl font-bold text-white mb-2">Loading Your Dashboard</h2>
          <p className="text-purple-200">Analyzing your skills and preparing insights...</p>
        </div>
      </div>
    );
  }

  if (!derivedSkillData || derivedSkillData.length === 0) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-slate-900 via-purple-900 to-slate-900 flex items-center justify-center">
        <div className="text-center max-w-md">
          <div className="bg-gradient-to-r from-purple-600 to-pink-600 p-4 rounded-2xl w-24 h-24 mx-auto mb-6 flex items-center justify-center">
            <Award className="w-12 h-12 text-white" />
          </div>
          <h2 className="text-3xl font-bold text-white mb-4">Welcome to Your Skill Journey</h2>
          <p className="text-purple-200 mb-8 text-lg">
            Complete your skill assessment to unlock personalized insights, cognitive analysis, and tailored recommendations.
          </p>
          <button
            onClick={() => window.location.href = '/assessment'}
            className="bg-gradient-to-r from-purple-600 to-pink-600 text-white px-8 py-4 rounded-xl hover:from-purple-700 hover:to-pink-700 transition-all duration-300 font-semibold text-lg shadow-2xl hover:shadow-purple-500/25"
          >
            Start Assessment
          </button>
        </div>
      </div>
    );
  }

  const averageLevel = derivedSkillData.reduce((sum, skill) => sum + skill.level, 0) / derivedSkillData.length;
  const topSkills = derivedSkillData.filter(skill => skill.level >= 4);
  const growthAreas = derivedSkillData.filter(skill => skill.level <= 2);

  const tabs = [
    { id: 'overview', label: 'Overview', icon: BarChart3 },
    { id: 'skills', label: 'Skills', icon: Brain },
    { id: 'analysis', label: 'Analysis', icon: Activity },
    { id: 'resume', label: 'Resume', icon: FileText }
  ];

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-purple-900 to-slate-900">
      {/* Header */}
      <div className="bg-slate-800/50 backdrop-blur-sm border-b border-purple-500/20">
        <div className="max-w-7xl mx-auto px-6 py-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-4">
              <div className="bg-gradient-to-r from-purple-600 to-pink-600 p-3 rounded-xl">
                <Brain className="w-6 h-6 text-white" />
              </div>
              <div>
                <h1 className="text-2xl font-bold text-white">Skill Assessment Dashboard</h1>
                <p className="text-purple-200">Welcome back, {user?.email?.split('@')[0] || 'User'}!</p>
              </div>
            </div>
            <div className="flex items-center space-x-3">
              <button
                onClick={logout}
                className="bg-red-500/20 hover:bg-red-500/30 text-red-300 px-4 py-2 rounded-lg transition flex items-center space-x-2 backdrop-blur-sm"
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
          <div className="bg-gradient-to-r from-blue-600/20 to-cyan-600/20 backdrop-blur-sm rounded-2xl p-6 border border-blue-500/20 hover:border-blue-400/40 transition-all duration-300">
            <div className="flex items-center">
              <div className="bg-blue-500/20 p-3 rounded-xl">
                <TrendingUp className="w-6 h-6 text-blue-400" />
              </div>
              <div className="ml-4">
                <p className="text-sm font-medium text-blue-200">Average Skill Level</p>
                <p className="text-2xl font-bold text-white">{averageLevel.toFixed(1)}/5</p>
              </div>
            </div>
          </div>

          <div className="bg-gradient-to-r from-green-600/20 to-emerald-600/20 backdrop-blur-sm rounded-2xl p-6 border border-green-500/20 hover:border-green-400/40 transition-all duration-300">
            <div className="flex items-center">
              <div className="bg-green-500/20 p-3 rounded-xl">
                <Award className="w-6 h-6 text-green-400" />
              </div>
              <div className="ml-4">
                <p className="text-sm font-medium text-green-200">Strengths</p>
                <p className="text-2xl font-bold text-white">{topSkills.length}</p>
              </div>
            </div>
          </div>

          <div className="bg-gradient-to-r from-orange-600/20 to-red-600/20 backdrop-blur-sm rounded-2xl p-6 border border-orange-500/20 hover:border-orange-400/40 transition-all duration-300">
            <div className="flex items-center">
              <div className="bg-orange-500/20 p-3 rounded-xl">
                <Target className="w-6 h-6 text-orange-400" />
              </div>
              <div className="ml-4">
                <p className="text-sm font-medium text-orange-200">Growth Areas</p>
                <p className="text-2xl font-bold text-white">{growthAreas.length}</p>
              </div>
            </div>
          </div>

          <div className="bg-gradient-to-r from-purple-600/20 to-pink-600/20 backdrop-blur-sm rounded-2xl p-6 border border-purple-500/20 hover:border-purple-400/40 transition-all duration-300">
            <div className="flex items-center">
              <div className="bg-purple-500/20 p-3 rounded-xl">
                <BarChart3 className="w-6 h-6 text-purple-400" />
              </div>
              <div className="ml-4">
                <p className="text-sm font-medium text-purple-200">Questions Answered</p>
                <p className="text-2xl font-bold text-white">{progress?.questionsAnswered || 0}</p>
              </div>
            </div>
          </div>
        </div>

        {/* Tab Navigation */}
        <div className="mb-8">
          <div className="flex space-x-1 bg-slate-800/50 backdrop-blur-sm rounded-xl p-1 border border-purple-500/20">
            {tabs.map((tab) => {
              const Icon = tab.icon;
              return (
                <button
                  key={tab.id}
                  onClick={() => setActiveTab(tab.id)}
                  className={`flex-1 flex items-center justify-center space-x-2 px-4 py-3 rounded-lg transition-all duration-300 ${
                    activeTab === tab.id
                      ? 'bg-gradient-to-r from-purple-600 to-pink-600 text-white shadow-lg'
                      : 'text-purple-200 hover:text-white hover:bg-slate-700/50'
                  }`}
                >
                  <Icon className="w-4 h-4" />
                  <span className="font-medium">{tab.label}</span>
                </button>
              );
            })}
          </div>
        </div>

        {/* Tab Content */}
        {activeTab === 'overview' && (
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
            {/* Left Column - Charts */}
            <div className="lg:col-span-2 space-y-8">
              {/* Skill Radar Chart */}
              <div className="bg-slate-800/50 backdrop-blur-sm rounded-2xl p-6 border border-purple-500/20 hover:border-purple-400/40 transition-all duration-300">
                <SkillRadarChart skillData={derivedSkillData} />
              </div>

              {/* Skill Graph */}
              <div className="bg-slate-800/50 backdrop-blur-sm rounded-2xl p-6 border border-purple-500/20 hover:border-purple-400/40 transition-all duration-300">
                <SkillGraph skillData={derivedSkillData} skillDependencies={skillDependencies} />
              </div>
            </div>

            {/* Right Column - Insights */}
            <div className="space-y-6">
              {/* AI Feedback */}
              <div className="bg-slate-800/50 backdrop-blur-sm rounded-2xl p-6 border border-purple-500/20">
                <FeedbackCard sessionId={sessionId} />
              </div>

              {/* Quick Actions */}
              <div className="bg-slate-800/50 backdrop-blur-sm rounded-2xl p-6 border border-purple-500/20">
                <h3 className="text-lg font-semibold text-white mb-4 flex items-center">
                  <Zap className="w-5 h-5 mr-2 text-yellow-400" />
                  Quick Actions
                </h3>
                <div className="space-y-3">
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
                    className="w-full bg-slate-700/50 hover:bg-slate-600/50 text-slate-200 px-4 py-3 rounded-lg transition flex items-center justify-center space-x-2"
                  >
                    <Download className="w-4 h-4" />
                    <span>Export My Data</span>
                  </button>
                </div>
              </div>
            </div>
          </div>
        )}

        {activeTab === 'skills' && (
          <div className="space-y-8">
            <SkillGapAnalysis 
              skillData={derivedSkillData} 
              targetRole={progress?.targetRole}
              sessionId={sessionId}
              resumeData={resumeData}
            />
          </div>
        )}

        {activeTab === 'analysis' && (
          <div className="space-y-8">
            {/* Cognitive Bias Analysis */}
            {cognitiveBiasData.length > 0 && (
              <div className="bg-slate-800/50 backdrop-blur-sm rounded-2xl p-6 border border-purple-500/20">
                <h3 className="text-xl font-semibold text-white mb-6 flex items-center">
                  <Brain className="w-6 h-6 mr-3 text-purple-400" />
                  Cognitive Analysis
                </h3>
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                  {cognitiveBiasData.map((bias, index) => (
                    <CognitiveBiasCard key={index} bias={bias} />
                  ))}
                </div>
              </div>
            )}
          </div>
        )}

        {activeTab === 'resume' && resumeData && (
          <div className="space-y-8">
            <ResumeAnalysisCard resumeData={resumeData} />
          </div>
        )}
      </div>
    </div>
  );
};

export default ModernDashboard;



