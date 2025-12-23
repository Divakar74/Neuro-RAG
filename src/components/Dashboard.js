import React, { useState, useEffect } from 'react';
import HeroSection from './HeroSection';
import SkillRadarChart from './SkillRadarChart';
import SkillGraph from './SkillGraph';
import StrengthsCard from './StrengthsCard';
import GrowthAreasCard from './GrowthAreasCard';
import LevelBadge from './LevelBadge';
import MotivationBanner from './MotivationBanner';
import { Loader2, TrendingUp, Award, Target, Star, ShieldCheck, Download } from 'lucide-react';
import { exportMyData } from '../services/privacyService';
import { fetchAssessmentProgress } from '../services/assessmentService';
import FeedbackCard from './FeedbackCard';

const Dashboard = ({ sessionId, skillDependencies, loading = false, skillData }) => {
  const [animationDelay, setAnimationDelay] = useState(0);
  const [progress, setProgress] = useState(null);
  const [derivedSkillData, setDerivedSkillData] = useState(skillData || []);
  const [isLoading, setIsLoading] = useState(loading);

  useEffect(() => {
    // Stagger animations for a nice entrance effect
    const timer = setTimeout(() => setAnimationDelay(1), 100);
    return () => clearTimeout(timer);
  }, []);

  useEffect(() => {
    const load = async () => {
      if (!sessionId) return;
      setIsLoading(true);
      try {
        const res = await fetchAssessmentProgress(sessionId);
        setProgress(res.data);
        // Derive skills array if backend provides confidence map
        const levels = res.data?.skillConfidenceLevels || {};
        const derived = Object.entries(levels).map(([code, confidence]) => ({
          name: code.replace('_', ' '),
          level: Math.max(1, Math.min(5, Math.round((confidence || 0) * 5) || 1)),
        }));
        setDerivedSkillData(derived);
      } finally {
        setIsLoading(false);
      }
    };
    load();
  }, [sessionId]);

  if (isLoading) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-slate-50 to-blue-50 flex items-center justify-center">
        <div className="text-center">
          <Loader2 className="w-12 h-12 animate-spin text-blue-600 mx-auto mb-4" />
          <h2 className="text-xl font-semibold text-gray-700 mb-2">Loading Your Dashboard</h2>
          <p className="text-gray-500">Analyzing your skills and preparing insights...</p>
        </div>
      </div>
    );
  }

  if (!derivedSkillData || derivedSkillData.length === 0) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-slate-50 to-blue-50 flex items-center justify-center">
        <div className="text-center max-w-md">
          <Award className="w-16 h-16 text-gray-400 mx-auto mb-4" />
          <h2 className="text-2xl font-bold text-gray-700 mb-2">No Skill Data Available</h2>
          <p className="text-gray-500 mb-6">
            Complete your skill assessment to see your personalized dashboard with insights and recommendations.
          </p>
          <button
            onClick={() => window.location.href = '/assessment'}
            className="bg-blue-600 text-white px-6 py-3 rounded-lg hover:bg-blue-700 transition font-semibold"
          >
            Start Assessment
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen brand-gradient">
      <div className="max-w-7xl mx-auto px-4 py-8">
        {/* Badges/Motivation strip */}
        <div className="mb-6 flex flex-wrap items-center gap-3">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-white shadow border border-slate-200 text-sm">
            <Star className="w-4 h-4 text-amber-500" />
            <span>Nice! You’re 70% closer to your target role</span>
          </div>
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-white shadow border border-slate-200 text-sm">
            <ShieldCheck className="w-4 h-4 text-emerald-600" />
            <span>Consistency badge unlocked</span>
          </div>
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
            className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-white shadow border border-slate-200 text-sm hover:bg-slate-50"
            title="Download My Data"
          >
            <Download className="w-4 h-4 text-slate-700" />
            <span>Download My Data</span>
          </button>
        </div>
        {/* Hero Section */}
        <div className="mb-8 animate-fade-in">
          <HeroSection skillData={derivedSkillData} />
        </div>

        {/* Stats Overview */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
          <div className="bg-white rounded-xl shadow-lg p-6 animate-slide-in-left">
            <div className="flex items-center">
              <div className="bg-blue-100 p-3 rounded-lg">
                <TrendingUp className="w-6 h-6 text-blue-600" />
              </div>
              <div className="ml-4">
                <p className="text-sm font-medium text-gray-600">Average Skill Level</p>
                <p className="text-2xl font-bold text-gray-900">
                  {(derivedSkillData.reduce((sum, skill) => sum + skill.level, 0) / derivedSkillData.length).toFixed(1)}
                </p>
              </div>
            </div>
          </div>

          <div className="bg-white rounded-xl shadow-lg p-6 animate-slide-in-left" style={{ animationDelay: '0.1s' }}>
            <div className="flex items-center">
              <div className="bg-green-100 p-3 rounded-lg">
                <Award className="w-6 h-6 text-green-600" />
              </div>
              <div className="ml-4">
                <p className="text-sm font-medium text-gray-600">Top Skills</p>
                <p className="text-2xl font-bold text-gray-900">
                  {derivedSkillData.filter(skill => skill.level >= 4).length}
                </p>
              </div>
            </div>
          </div>

          <div className="bg-white rounded-xl shadow-lg p-6 animate-slide-in-left" style={{ animationDelay: '0.2s' }}>
            <div className="flex items-center">
              <div className="bg-orange-100 p-3 rounded-lg">
                <Target className="w-6 h-6 text-orange-600" />
              </div>
              <div className="ml-4">
                <p className="text-sm font-medium text-gray-600">Growth Areas</p>
                <p className="text-2xl font-bold text-gray-900">
                  {derivedSkillData.filter(skill => skill.level <= 2).length}
                </p>
              </div>
            </div>
          </div>
        </div>

        {/* Main Dashboard Grid */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 mb-8">
          {/* Left Column - Charts */}
          <div className="lg:col-span-2 space-y-8">
            {/* Skill Radar Chart */}
            <div className="bg-white rounded-xl shadow-lg p-6 hover:shadow-xl transition-all duration-300 animate-slide-in-right border border-primary-300">
              <SkillRadarChart skillData={derivedSkillData} />
            </div>

            {/* Skill Graph */}
            <div className="bg-white rounded-xl shadow-lg p-6 hover:shadow-xl transition-all duration-300 animate-slide-in-right border border-primary-300" style={{ animationDelay: '0.1s' }}>
              <SkillGraph skillData={derivedSkillData} skillDependencies={skillDependencies} />
            </div>

            {/* Assessment Timeline (simple) */}
            <div className="bg-white rounded-xl shadow-lg p-6 hover:shadow-xl transition-all duration-300 animate-slide-in-right" style={{ animationDelay: '0.2s' }}>
              <h3 className="text-lg font-semibold text-gray-800 mb-4">Assessment Timeline</h3>
              <ul className="space-y-2 text-sm text-gray-700">
                <li className="flex items-center justify-between">
                  <span>Questions answered</span>
                  <span className="font-medium">{progress?.questionsAnswered ?? '—'}</span>
                </li>
                <li className="flex items-center justify-between">
                  <span>Estimated coverage</span>
                  <span className="font-medium">{Math.round((progress?.coverage || 0) * 100)}%</span>
                </li>
                <li className="flex items-center justify-between">
                  <span>Status</span>
                  <span className="font-medium capitalize">{progress?.status || 'in_progress'}</span>
                </li>
              </ul>
            </div>
          </div>

          {/* Right Column - Analysis Cards */}
          <div className="space-y-6">
            {/* Strengths Card */}
            <div className="transform hover:scale-105 transition-all duration-300 animate-slide-in-left" style={{ animationDelay: '0.2s' }}>
              <StrengthsCard skillData={derivedSkillData} />
            </div>

            {/* Growth Areas Card */}
            <div className="transform hover:scale-105 transition-all duration-300 animate-slide-in-left" style={{ animationDelay: '0.3s' }}>
              <GrowthAreasCard skillData={derivedSkillData} />
            </div>

            {/* Feedback Card */}
            <div className="bg-transparent animate-slide-in-left" style={{ animationDelay: '0.35s' }}>
              <FeedbackCard sessionId={sessionId} />
            </div>

            {/* Cognitive Consistency Indicator */}
            <div className="bg-white rounded-xl shadow-lg p-6 hover:shadow-xl transition-all duration-300 animate-slide-in-left" style={{ animationDelay: '0.38s' }}>
              <h3 className="text-xl font-semibold mb-2 text-gray-800">Cognitive Consistency</h3>
              {(() => {
                const confidence = progress?.overallConfidence ?? 0;
                const label = confidence > 0.75 ? 'High' : confidence > 0.45 ? 'Medium' : 'Low';
                const color = label === 'High' ? 'text-emerald-600' : label === 'Medium' ? 'text-amber-600' : 'text-rose-600';
                return (
                  <div className={`text-lg font-semibold ${color}`}>{label}</div>
                );
              })()}
              <p className="text-sm text-gray-600 mt-2">Based on stability, timing, and answer patterns.</p>
            </div>

            {/* Skill Level Summary */}
            <div className="bg-white rounded-xl shadow-lg p-6 hover:shadow-xl transition-all duration-300 animate-slide-in-left" style={{ animationDelay: '0.4s' }}>
              <h3 className="text-xl font-semibold mb-4 text-gray-800">Skill Levels</h3>
              <div className="space-y-3">
                {derivedSkillData?.slice(0, 5).map((skill, index) => (
                  <div key={index} className="flex items-center justify-between animate-fade-in" style={{ animationDelay: `${0.5 + index * 0.1}s` }}>
                    <span className="text-gray-700 font-medium">{skill.name}</span>
                    <LevelBadge level={skill.level} />
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>

        {/* Motivation Banner */}
        <div className="mb-8 animate-fade-in" style={{ animationDelay: '0.6s' }}>
          <MotivationBanner />
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
