import React, { useState, useEffect } from 'react';
import { useUser } from '../contexts/UserContext';
import { Calendar, Clock, TrendingUp, Award, Target, Plus, LogOut } from 'lucide-react';
import { fetchAssessmentProgress, getUserSessions } from '../services/assessmentService';
import { fetchCognitiveAnalysisBySessionId } from '../services/cognitiveService';
import ResumeUploader from './ResumeUploader';
import ResumeContentViewer from './ResumeContentViewer';
import CognitiveBiasCard from './CognitiveBiasCard';
import AISuggestionsCard from './AISuggestionsCard';
import SkillGapAnalysis from './SkillGapAnalysis';
import { uploadResume, fetchResumeData } from '../services/resumeService';
import { useRoadmap } from '../hooks/useRoadmap';
import RoadmapTimeline from './RoadmapTimeline';
import { parseResumeSections } from '../utils/parseResumeSections';

const UserDashboard = ({ onStartNewAssessment, onViewSession, performanceData }) => {
  const { user, logout } = useUser();
  const [sessions, setSessions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [cognitiveBiases, setCognitiveBiases] = useState([]);
  const [stats, setStats] = useState({
    totalSessions: 0,
    averageScore: 0,
    topSkills: 0,
    recentActivity: null
  });
  const [resumeData, setResumeData] = useState(null);
  const [showUploader, setShowUploader] = useState(false);
  const [latestSessionToken, setLatestSessionToken] = useState(null);
  const { roadmap, loading: roadmapLoading, generateRoadmap } = useRoadmap(latestSessionToken);

  useEffect(() => {
    loadUserSessions();
  }, []);

  const loadUserSessions = async () => {
    setLoading(true);
    try {
      const response = await getUserSessions(user?.id);
      const sessionsData = response.data;

      // Map all sessions to expected format for display
      console.log('Raw session data:', sessionsData);
      const mappedSessions = sessionsData.map(session => ({
        id: session.sessionToken,
        sessionDbId: session.id,
        createdAt: new Date(session.startedAt),
        targetRole: session.targetRole,
        status: session.status,
        skillCount: session.skillAssessments ? session.skillAssessments.length : 0,
        averageLevel: (() => {
          if (!session.skillAssessments || session.skillAssessments.length === 0) {
            console.log(`Session ${session.id}: No skill assessments found, setting level to 0 (Novice)`);
            return 0; // No assessments = Novice level
          }
          const validAssessments = session.skillAssessments.filter(sa => sa.assessedLevel != null);
          if (validAssessments.length === 0) {
            console.log(`Session ${session.id}: No valid skill assessments found, setting level to 0 (Novice)`);
            return 0; // No valid assessments = Novice level
          }
          const avg = validAssessments.reduce((sum, sa) => sum + sa.assessedLevel, 0) / validAssessments.length;
          console.log(`Session ${session.id}: ${validAssessments.length} assessments, average level: ${avg}`);
          return avg;
        })(),
        resumeUploaded: session.resumeUploaded,
        // Add persistence indicators for user data restoration
        hasAIFeedback: session.hasAIFeedback || false,
        hasRoadmap: session.hasRoadmap || false,
        responseCount: session.responseCount || 0
      }));

      setSessions(mappedSessions);
      // Roadmap APIs expect numeric DB id; store that as hook key
      setLatestSessionToken(mappedSessions[0]?.sessionDbId || null);
      setStats({
        totalSessions: mappedSessions.length,
        averageScore: mappedSessions.length > 0 ? mappedSessions.reduce((sum, s) => sum + s.averageLevel, 0) / mappedSessions.length : 0,
        topSkills: mappedSessions.reduce((sum, s) => sum + s.skillCount, 0),
        recentActivity: mappedSessions[0]?.createdAt
      });

      // Load resume data for the logged-in user
      if (user?.id) {
        try {
          const userResumeData = await getUserResumeData(user.id);
          if (userResumeData && userResumeData.length > 0) {
            // Use the most recent resume data
            setResumeData(userResumeData[0]);
          }
        } catch (e) {
          console.warn('Unable to fetch user resume data', e);
          // Fallback to session-based resume loading for backward compatibility
          const resumeSessions = sessionsData.filter(session => session.resumeUploaded);
          if (resumeSessions.length > 0) {
            const latestResumeSession = resumeSessions[0];
            try {
              const resume = await fetchResumeData(latestResumeSession.sessionToken);
              if (resume) setResumeData(resume?.data || resume);
            } catch (fallbackError) {
              console.warn('Fallback resume fetch also failed', fallbackError);
            }
          }
        }
      } else {
        // For anonymous users, use session-based resume loading
        const resumeSessions = sessionsData.filter(session => session.resumeUploaded);
        if (resumeSessions.length > 0) {

  const loadCognitiveBiases = async (sessionId) => {
    try {
      const cognitiveResponse = await fetchCognitiveAnalysisBySessionId(sessionId);
      if (cognitiveResponse) {
        setCognitiveBiases(cognitiveResponse);
      }
    } catch (error) {
      console.error('Failed to load cognitive biases:', error);
      setCognitiveBiases([]);
    }
  };

  const formatDate = (date) => {
    return new Intl.DateTimeFormat('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric'
    }).format(new Date(date));
  };

  const getStatusColor = (status) => {
    switch (status) {
      case 'completed': return 'bg-green-100 text-green-800';
      case 'in_progress': return 'bg-blue-100 text-blue-800';
      case 'draft': return 'bg-gray-100 text-gray-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  };

  const getProficiencyLevel = (score) => {
    if (score >= 0.8) return 'Expert';
    if (score >= 0.6) return 'Advanced';
    if (score >= 0.4) return 'Intermediate';
    if (score >= 0.2) return 'Beginner';
    return 'Novice';
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-primary-100 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-700 mx-auto"></div>
          <p className="mt-4 text-primary-700">Loading your dashboard...</p>
        </div>
      </div>
    );
  }

  if (showUploader) {
    return (
      <div className="min-h-screen bg-primary-100 flex items-center justify-center">
        <div className="max-w-lg w-full p-6 bg-white rounded-lg shadow-lg">
          <h2 className="text-2xl font-bold mb-4 text-primary-900">Upload Your Resume</h2>
          <ResumeUploader
            onUpload={async (file, role) => {
              setLoading(true);
              try {
                await uploadResume(file, role, user?.id);
                setShowUploader(false);
                await loadUserSessions();
              } catch (error) {
                console.error('Failed to upload resume:', error);
              } finally {
                setLoading(false);
              }
            }}
          />
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-primary-100">
      <div className="max-w-7xl mx-auto px-6 py-10">
        {/* Header */}
        <div className="mb-8 flex items-center justify-between">
          <div>
            <h1 className="text-4xl font-bold text-primary-900 mb-2">
              Welcome back, {user?.email?.split('@')[0]}!
            </h1>
            <p className="text-primary-700 text-lg">
              Track your skill development journey and continue growing
            </p>
          </div>
          <div className="flex items-center space-x-4">
            <button
              onClick={onStartNewAssessment}
              className="bg-primary-700 text-white px-6 py-3 rounded-lg hover:bg-primary-900 transition flex items-center space-x-2 font-semibold"
            >
              <Target className="w-5 h-5" />
              <span>Take Assessment</span>
            </button>
            <button
              onClick={() => {
                logout();
                // Navigate to landing page after logout
                window.location.href = '/';
              }}
              className="bg-red-100 text-red-700 px-4 py-2 rounded-lg hover:bg-red-200 transition flex items-center space-x-2"
            >
              <LogOut className="w-4 h-4" />
              <span>Logout</span>
            </button>
          </div>
        </div>

        {/* Take Assessment Hero Section */}
        <div className="bg-gradient-to-r from-primary-700 to-primary-900 rounded-3xl p-8 mb-8 text-white">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-3xl font-bold mb-2">Ready to Test Your Skills?</h2>
              <p className="text-primary-100 text-lg mb-4">
                {sessions.length === 0 
                  ? "Start your first assessment to discover your strengths and areas for growth."
                  : "Take another assessment to track your progress and identify new learning opportunities."
                }
              </p>
              <div className="flex items-center space-x-6 text-primary-100">
                <div>
                  <span className="text-2xl font-bold text-white">{stats.totalSessions}</span>
                  <span className="ml-2">Assessments Completed</span>
                </div>
                {stats.totalSessions > 0 && (
                  <div>
                    <span className="text-2xl font-bold text-white">{getProficiencyLevel(stats.averageScore)}</span>
                    <span className="ml-2">Current Level</span>
                  </div>
                )}
              </div>
            </div>
            <div className="flex flex-col space-y-3">
              <button
                onClick={onStartNewAssessment}
                className="bg-white text-primary-700 px-8 py-4 rounded-lg hover:bg-primary-50 transition font-semibold text-lg flex items-center space-x-2"
              >
                <Target className="w-6 h-6" />
                <span>{sessions.length === 0 ? 'Start First Assessment' : 'Take New Assessment'}</span>
              </button>
              {sessions.length > 0 && (
                <p className="text-primary-100 text-sm text-center">
                  Track your skill development journey
                </p>
              )}
            </div>
          </div>
        </div>

        {/* Cognitive Bias Analysis */}
        {cognitiveBiases.length > 0 && (
          <div className="mb-8">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-2xl font-bold text-primary-900">Cognitive Bias Analysis</h2>
              <button
                onClick={onStartNewAssessment}
                className="bg-primary-100 text-primary-700 px-4 py-2 rounded-lg hover:bg-primary-200 transition flex items-center space-x-2 text-sm font-medium"
              >
                <Target className="w-4 h-4" />
                <span>Test Again</span>
              </button>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mb-6">
              {cognitiveBiases.map((bias, index) => (
                <CognitiveBiasCard
                  key={index}
                  bias={bias}
                />
              ))}
            </div>
            <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 text-center">
              <p className="text-blue-800 text-sm mb-2">
                Want to see how your cognitive patterns evolve? Take another assessment to track changes.
              </p>
              <button
                onClick={onStartNewAssessment}
                className="text-blue-700 hover:text-blue-900 font-medium text-sm"
              >
                Take New Assessment →
              </button>
            </div>
          </div>
        )}

        {/* AI Suggestions */}
        {sessions.length > 0 && sessions[0].status === 'completed' && (
          <div className="mb-8">
            <AISuggestionsCard 
              session={sessions[0]}
              resumeData={resumeData}
              cognitiveBiases={cognitiveBiases}
              sessionSummary={{
                averageLevel: stats.averageScore,
                skillsAssessed: stats.topSkills,
                lastActivity: stats.recentActivity
              }}
            />
          </div>
        )}

        {/* Resume Summary Display - With Extracted Content */}
        {resumeData && (
          <div className="bg-white rounded-2xl p-6 shadow-md mb-8">
            <div className="flex items-center justify-between mb-6">
              <h2 className="text-2xl font-bold text-primary-900">Resume Profile</h2>
              <div className="text-sm text-gray-500">
                Uploaded: {resumeData.createdAt ? new Date(resumeData.createdAt).toLocaleDateString() : 'Recently'}
              </div>
            </div>

            {/* Quick Stats */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
              <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
                <div className="text-sm font-medium text-blue-900">Total Experience</div>
                <div className="text-2xl font-bold text-blue-700">{resumeData.totalYearsExperience || 0} years</div>
              </div>
              <div className="bg-green-50 border border-green-200 rounded-lg p-4">
                <div className="text-sm font-medium text-green-900">Skills Identified</div>
                <div className="text-2xl font-bold text-green-700">
                  {(() => {
                    try {
                      const skills = resumeData.extractedSkills ? JSON.parse(resumeData.extractedSkills) : [];
                      return Array.isArray(skills) ? skills.length : 0;
                    } catch {
                      return 0;
                    }
                  })()}
                </div>
              </div>
              <div className="bg-purple-50 border border-purple-200 rounded-lg p-4">
                <div className="text-sm font-medium text-purple-900">Education Items</div>
                <div className="text-2xl font-bold text-purple-700">
                  {(() => {
                    try {
                      const education = resumeData.extractedEducation ? JSON.parse(resumeData.extractedEducation) : [];
                      return Array.isArray(education) ? education.length : 0;
                    } catch {
                      return 0;
                    }
                  })()}
                </div>
              </div>
            </div>

            {/* Structured Sections - With Extracted Content */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
              <div>
                <h3 className="text-lg font-semibold text-primary-900 mb-3">Key Skills</h3>
                <div className="flex flex-wrap gap-2">
                  {(() => {
                    try {
                      const skills = resumeData.extractedSkills ? JSON.parse(resumeData.extractedSkills) : [];
                      if (Array.isArray(skills) && skills.length > 0) {
                        return skills.slice(0, 8).map((skill, i) => (
                          <span key={i} className="bg-blue-50 text-blue-800 px-3 py-1 rounded-full text-sm">
                            {typeof skill === 'string' ? skill : skill.skillName || skill}
                          </span>
                        ));
                      }
                      return <div className="text-gray-500 text-sm">No skills detected</div>;
                    } catch {
                      return <div className="text-gray-500 text-sm">No skills detected</div>;
                    }
                  })()}
                  {(() => {
                    try {
                      const skills = resumeData.extractedSkills ? JSON.parse(resumeData.extractedSkills) : [];
                      return Array.isArray(skills) && skills.length > 8 ? (
                        <span className="text-gray-500 text-sm">+{skills.length - 8} more</span>
                      ) : null;
                    } catch {
                      return null;
                    }
                  })()}
                </div>
              </div>
              <div>
                <h3 className="text-lg font-semibold text-primary-900 mb-3">Education</h3>
                <div className="space-y-2">
                  {(() => {
                    try {
                      const education = resumeData.extractedEducation ? JSON.parse(resumeData.extractedEducation) : [];
                      if (Array.isArray(education) && education.length > 0) {
                        return education.slice(0, 3).map((edu, i) => (
                          <div key={i} className="text-sm text-gray-800 bg-green-50 px-3 py-2 rounded">
                            {typeof edu === 'string' ? edu : `${edu.degree || ''} - ${edu.institution || ''} (${edu.year || ''})`}
                          </div>
                        ));
                      }
                      return <div className="text-gray-500 text-sm">No education detected</div>;
                    } catch {
                      return <div className="text-gray-500 text-sm">No education detected</div>;
                    }
                  })()}
                  {(() => {
                    try {
                      const education = resumeData.extractedEducation ? JSON.parse(resumeData.extractedEducation) : [];
                      return Array.isArray(education) && education.length > 3 ? (
                        <div className="text-gray-500 text-sm">+{education.length - 3} more</div>
                      ) : null;
                    } catch {
                      return null;
                    }
                  })()}
                </div>
              </div>
            </div>

            {/* Experience Section */}
            <div className="mb-6">
              <h3 className="text-lg font-semibold text-primary-900 mb-3">Experience</h3>
              <div className="space-y-2">
                {(() => {
                  try {
                    const experience = resumeData.extractedExperience ? JSON.parse(resumeData.extractedExperience) : [];
                    if (Array.isArray(experience) && experience.length > 0) {
                      return experience.slice(0, 3).map((exp, i) => (
                        <div key={i} className="text-sm text-gray-800 bg-purple-50 px-3 py-2 rounded">
                          <div className="font-medium">
                            {exp.title || exp.position || 'Position'} at {exp.company || exp.organization || 'Company'}
                          </div>
                          <div className="text-xs text-gray-600 mt-1">
                            {exp.start_year || exp.startYear || ''} - {exp.end_year_or_present || exp.endYear || 'Present'}
                          </div>
                        </div>
                      ));
                    }
                    return <div className="text-gray-500 text-sm">No experience detected</div>;
                  } catch {
                    return <div className="text-gray-500 text-sm">No experience detected</div>;
                  }
                })()}
                {(() => {
                  try {
                    const experience = resumeData.extractedExperience ? JSON.parse(resumeData.extractedExperience) : [];
                    return Array.isArray(experience) && experience.length > 3 ? (
                      <div className="text-gray-500 text-sm">+{experience.length - 3} more</div>
                    ) : null;
                  } catch {
                    return null;
                  }
                })()}
              </div>
            </div>

            {/* Personal Information */}
            {(() => {
              try {
                const entities = resumeData.rawEntities ? JSON.parse(resumeData.rawEntities) : {};
                const personalInfo = entities.personalInfo || {};
                if (personalInfo.name || personalInfo.email || personalInfo.phone) {
                  return (
                    <div className="mb-6">
                      <h3 className="text-lg font-semibold text-primary-900 mb-3">Personal Information</h3>
                      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                        {personalInfo.name && (
                          <div className="bg-orange-50 border border-orange-200 rounded-lg p-3">
                            <div className="text-xs font-medium text-orange-900">Name</div>
                            <div className="text-sm font-semibold text-orange-800">{personalInfo.name}</div>
                          </div>
                        )}
                        {personalInfo.email && (
                          <div className="bg-orange-50 border border-orange-200 rounded-lg p-3">
                            <div className="text-xs font-medium text-orange-900">Email</div>
                            <div className="text-sm font-semibold text-orange-800">{personalInfo.email}</div>
                          </div>
                        )}
                        {personalInfo.phone && (
                          <div className="bg-orange-50 border border-orange-200 rounded-lg p-3">
                            <div className="text-xs font-medium text-orange-900">Phone</div>
                            <div className="text-sm font-semibold text-orange-800">{personalInfo.phone}</div>
                          </div>
                        )}
                      </div>
                    </div>
                  );
                }
                return null;
              } catch {
                return null;
              }
            })()}
          </div>
        )}

        {/* Performance Data Display */}
        {performanceData && (
          <div className="bg-white rounded-2xl p-6 shadow-md mb-8">
            <h2 className="text-2xl font-bold text-primary-900 mb-4">Latest Assessment Performance</h2>

            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
              <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
                <div className="text-sm font-medium text-blue-900">Total Questions</div>
                <div className="text-2xl font-bold text-blue-700">{performanceData.totalQuestions || 0}</div>
              </div>
              <div className="bg-green-50 border border-green-200 rounded-lg p-4">
                <div className="text-sm font-medium text-green-900">MCQ Accuracy</div>
                <div className="text-2xl font-bold text-green-700">{((performanceData.mcqAccuracy || 0) * 100).toFixed(1)}%</div>
              </div>
              <div className="bg-purple-50 border border-purple-200 rounded-lg p-4">
                <div className="text-sm font-medium text-purple-900">Avg Confidence</div>
                <div className="text-2xl font-bold text-purple-700">{(performanceData.averageConfidence || 0).toFixed(2)}</div>
              </div>
              <div className="bg-orange-50 border border-orange-200 rounded-lg p-4">
                <div className="text-sm font-medium text-orange-900">Avg Consistency</div>
                <div className="text-2xl font-bold text-orange-700">{(performanceData.averageConsistency || 0).toFixed(2)}</div>
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div className="bg-red-50 border border-red-200 rounded-lg p-4">
                <div className="text-sm font-medium text-red-900">Cognitive Bias</div>
                <div className="text-2xl font-bold text-red-700">{(performanceData.averageCognitiveBias || 0).toFixed(2)}</div>
              </div>
              <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
                <div className="text-sm font-medium text-yellow-900">Time Pressure</div>
                <div className="text-2xl font-bold text-yellow-700">{(performanceData.averageTimePressure || 0).toFixed(2)}</div>
              </div>
              <div className="bg-indigo-50 border border-indigo-200 rounded-lg p-4">
                <div className="text-sm font-medium text-indigo-900">Answer Stability</div>
                <div className="text-2xl font-bold text-indigo-700">{(performanceData.averageAnswerStability || 0).toFixed(2)}</div>
              </div>
            </div>
          </div>
        )}

        {/* Stats Overview */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-10">
          <div className="bg-white rounded-2xl p-6 shadow-md hover:shadow-lg transition group cursor-pointer" onClick={onStartNewAssessment}>
            <div className="flex items-center space-x-3 mb-3">
              <div className="bg-primary-100 p-2 rounded-lg group-hover:bg-primary-200 transition">
                <Target className="w-5 h-5 text-primary-700" />
              </div>
              <div className="flex-1">
                <p className="text-sm font-medium text-gray-600">Total Assessments</p>
                <p className="text-2xl font-bold text-primary-900">{stats.totalSessions}</p>
              </div>
            </div>
            <div className="mt-3 text-xs text-primary-600 group-hover:text-primary-800 transition font-medium">
              Click to take another assessment →
            </div>
          </div>

          <div className="bg-white rounded-2xl p-6 shadow-md">
            <div className="flex items-center space-x-3 mb-3">
              <div className="bg-green-100 p-2 rounded-lg">
                <TrendingUp className="w-5 h-5 text-green-700" />
              </div>
              <div>
                <p className="text-sm font-medium text-gray-600">Average Level</p>
                <p className="text-2xl font-bold text-primary-900">{getProficiencyLevel(stats.averageScore)}</p>
                <p className="text-sm text-gray-500">{stats.averageScore.toFixed(1)}/1.0</p>
              </div>
            </div>
            {stats.totalSessions > 0 && (
              <div className="mt-3">
                <button 
                  onClick={onStartNewAssessment}
                  className="text-xs text-green-600 hover:text-green-800 font-medium"
                >
                  Improve your level →
                </button>
              </div>
            )}
          </div>

          <div className="bg-white rounded-2xl p-6 shadow-md">
            <div className="flex items-center space-x-3 mb-3">
              <div className="bg-blue-100 p-2 rounded-lg">
                <Award className="w-5 h-5 text-blue-700" />
              </div>
              <div>
                <p className="text-sm font-medium text-gray-600">Skills Assessed</p>
                <p className="text-2xl font-bold text-primary-900">{stats.topSkills}</p>
              </div>
            </div>
          </div>

          <div className="bg-white rounded-2xl p-6 shadow-md">
            <div className="flex items-center space-x-3 mb-3">
              <div className="bg-primary-100 p-2 rounded-lg">
                <Calendar className="w-5 h-5 text-primary-700" />
              </div>
              <div>
                <p className="text-sm font-medium text-gray-600">Last Activity</p>
                <p className="text-sm font-bold text-primary-900">
                  {stats.recentActivity ? formatDate(stats.recentActivity) : 'No activity'}
                </p>
              </div>
            </div>
          </div>
        </div>

        {/* Assessment Progress Tracking */}
        {sessions.length > 1 && (
          <div className="bg-white rounded-2xl p-6 shadow-md mb-8">
            <h2 className="text-2xl font-bold text-primary-900 mb-4">Your Progress Over Time</h2>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              {/* Progress Indicators */}
              <div className="bg-gradient-to-br from-green-50 to-green-100 border border-green-200 rounded-lg p-4">
                <div className="flex items-center justify-between mb-2">
                  <h3 className="text-lg font-semibold text-green-900">Skill Level Trend</h3>
                  <TrendingUp className="w-5 h-5 text-green-700" />
                </div>
                <div className="text-3xl font-bold text-green-700 mb-1">
                  {(() => {
                    const latest = sessions[0]?.averageLevel || 0;
                    const previous = sessions[1]?.averageLevel || 0;
                    const change = latest - previous;
                    return change >= 0 ? `+${change.toFixed(2)}` : change.toFixed(2);
                  })()}
                </div>
                <div className="text-sm text-green-600">
                  Since last assessment
                </div>
              </div>

              <div className="bg-gradient-to-br from-blue-50 to-blue-100 border border-blue-200 rounded-lg p-4">
                <div className="flex items-center justify-between mb-2">
                  <h3 className="text-lg font-semibold text-blue-900">Skills Coverage</h3>
                  <Award className="w-5 h-5 text-blue-700" />
                </div>
                <div className="text-3xl font-bold text-blue-700 mb-1">
                  {(() => {
                    const totalUniqueSkills = new Set(
                      sessions.flatMap(s => 
                        // This would need to be implemented based on actual skill data structure
                        Array.from({length: s.skillCount}, (_, i) => `skill_${i}`)
                      )
                    ).size;
                    return totalUniqueSkills;
                  })()}
                </div>
                <div className="text-sm text-blue-600">
                  Unique skills assessed
                </div>
              </div>

              <div className="bg-gradient-to-br from-purple-50 to-purple-100 border border-purple-200 rounded-lg p-4">
                <div className="flex items-center justify-between mb-2">
                  <h3 className="text-lg font-semibold text-purple-900">Assessment Streak</h3>
                  <Calendar className="w-5 h-5 text-purple-700" />
                </div>
                <div className="text-3xl font-bold text-purple-700 mb-1">
                  {sessions.length}
                </div>
                <div className="text-sm text-purple-600">
                  Total assessments
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Past Sessions Module */}
        <div className="bg-white rounded-3xl p-8 shadow-lg mb-8">
          <div className="flex items-center justify-between mb-6">
            <h2 className="text-2xl font-bold text-primary-900">Past Sessions</h2>
            <button
              onClick={onStartNewAssessment}
              className="bg-primary-700 text-white px-6 py-3 rounded-lg hover:bg-primary-900 transition flex items-center space-x-2"
            >
              <Plus className="w-5 h-5" />
              <span>Take Assessment</span>
            </button>
          </div>

          {sessions.length === 0 ? (
            <div className="text-center py-12">
              <Target className="w-16 h-16 text-gray-300 mx-auto mb-4" />
              <h3 className="text-xl font-semibold text-gray-600 mb-2">No assessments yet</h3>
              <p className="text-gray-500 mb-6">Start your first skill assessment to track your progress</p>
              <button
                onClick={onStartNewAssessment}
                className="bg-primary-700 text-white px-8 py-3 rounded-lg hover:bg-primary-900 transition"
              >
                Start Your First Assessment
              </button>
            </div>
          ) : (
            <div className="space-y-4">
              {sessions.map((session) => (
                <div
                  key={session.id}
                  className="border border-gray-200 rounded-xl p-6 hover:shadow-md transition cursor-pointer"
                  onClick={() => onViewSession(session)}
                >
                  <div className="flex items-center justify-between">
                    <div className="flex-1">
                      <div className="flex items-center space-x-3 mb-2">
                        <h3 className="text-lg font-semibold text-primary-900">
                          {session.targetRole}
                        </h3>
                        <span className={`px-3 py-1 rounded-full text-xs font-medium ${getStatusColor(session.status)}`}>
                          {session.status.replace('_', ' ')}
                        </span>
                      </div>
                      <div className="flex items-center space-x-6 text-sm text-gray-600">
                        <div className="flex items-center space-x-1">
                          <Calendar className="w-4 h-4" />
                          <span>{formatDate(session.createdAt)}</span>
                        </div>
                        <div className="flex items-center space-x-1">
                          <Award className="w-4 h-4" />
                          <span>{session.skillCount} skills assessed</span>
                        </div>
                        <div className="flex items-center space-x-1">
                          <TrendingUp className="w-4 h-4" />
                          <span>Avg. level: {getProficiencyLevel(session.averageLevel)}</span>
                        </div>
                      </div>
                    </div>
                    <div className="text-right">
                      <button className="text-primary-700 hover:text-primary-900 font-medium">
                        View Details →
                      </button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Skill Gap Analysis */}
        {sessions.length > 0 && sessions[0].status === 'completed' && (
          <div className="mb-8">
            <SkillGapAnalysis
              skillData={sessions[0].skillAssessments || []}
              targetRole={sessions[0].targetRole}
              sessionId={sessions[0].sessionDbId}
              resumeData={resumeData}
              userResponses={[]} // TODO: Pass actual user responses if available
            />
          </div>
        )}

        {/* Quick Actions & Roadmap */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div className="bg-white rounded-2xl p-6 shadow-md">
            <h3 className="text-lg font-semibold text-primary-900 mb-4">Ready for Your Next Assessment?</h3>
            <div className="mb-4">
              {sessions.length === 0 ? (
                <p className="text-gray-600">
                  Start your first skill assessment to discover your strengths and identify areas for growth.
                </p>
              ) : (
                <div>
                  <p className="text-gray-600 mb-2">
                    Continue tracking your skill development progress with another assessment.
                  </p>
                  <div className="text-sm text-gray-500">
                    Last assessment: {stats.recentActivity ? formatDate(stats.recentActivity) : 'None'}
                  </div>
                </div>
              )}
            </div>
            <button
              onClick={onStartNewAssessment}
              className="w-full bg-primary-700 text-white py-3 rounded-lg hover:bg-primary-900 transition font-semibold flex items-center justify-center space-x-2"
            >
              <Target className="w-5 h-5" />
              <span>{sessions.length === 0 ? 'Start First Assessment' : 'Take New Assessment'}</span>
            </button>
          </div>

          <div className="bg-white rounded-2xl p-6 shadow-md">
            <h3 className="text-lg font-semibold text-primary-900 mb-4">Your Roadmap</h3>
            {roadmap ? (
              <RoadmapTimeline roadmapData={roadmap} />
            ) : (
              <div>
                <p className="text-gray-600 mb-4">
                  Generate a personalized learning roadmap based on your latest assessment.
                </p>
                <button
                  disabled={roadmapLoading || sessions.length === 0}
                  onClick={() => {
                    const latest = sessions[0];
                    // Roadmap endpoints expect numeric session DB id
                    setLatestSessionToken(latest?.sessionDbId);
                    generateRoadmap();
                  }}
                  className="w-full bg-gray-100 text-gray-700 py-3 rounded-lg hover:bg-gray-200 transition disabled:opacity-50"
                >
                  {roadmapLoading ? 'Generating...' : 'Generate Roadmap'}
                </button>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default UserDashboard;
