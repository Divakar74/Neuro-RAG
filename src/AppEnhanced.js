import React, { useState, useEffect } from 'react';
import { UserProvider, useUser } from './contexts/UserContext';
import Hero from './components/Hero';
import ResumeUploader from './components/ResumeUploader';
import RoleSelector from './components/RoleSelector';
import EnhancedAssessmentFlow from './components/EnhancedAssessmentFlow';
import EnhancedDashboard from './components/EnhancedDashboard';
import RoadmapPage from './components/RoadmapPage';
import LoginModal from './components/LoginModal';
import RegisterModal from './components/RegisterModal';
import { uploadResume, updateResume } from './services/resumeService';
import ResumeVerificationModal from './components/ResumeVerificationModal';
import { startAssessment, getUserSessions } from './services/assessmentService';
import { Loader2, ArrowLeft, Home } from 'lucide-react';
import './App.css';

const AppContent = () => {
  const { user, loading: userLoading, logout } = useUser();
  const [uploadedResume, setUploadedResume] = useState(null);
  const [selectedRole, setSelectedRole] = useState(null);
  const [assessmentSession, setAssessmentSession] = useState(null);
  const [currentView, setCurrentView] = useState('landing');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [onboarding, setOnboarding] = useState({ hasRegistered: false });
  const [showVerify, setShowVerify] = useState(false);
  const [resumeRecord, setResumeRecord] = useState(null);
  const [userSessions, setUserSessions] = useState([]);
  const [selectedSession, setSelectedSession] = useState(null);
  const [showLoginModal, setShowLoginModal] = useState(false);
  const [showRegisterModal, setShowRegisterModal] = useState(false);

  // Load user sessions on app start
  useEffect(() => {
    if (user) {
      loadUserSessions();
    }
  }, [user]);

  // Redirect to dashboard if logged in on landing
  useEffect(() => {
    if (user && currentView === 'landing') {
      // Check if user has past sessions
      const checkUserHistory = async () => {
        try {
          const response = await getUserSessions(user.id);
          const sessionsList = response.data || [];
        if (sessionsList.length > 0) {
          // User has past actions, go to dashboard
          setCurrentView('dashboard');
        } else {
          // No past actions, go to onboarding
          setCurrentView('onboarding');
        }
        } catch (error) {
          console.log('Error checking user history:', error);
          setCurrentView('landing');
        }
      };
      checkUserHistory();
    }
  }, [user, currentView]);

  const loadUserSessions = async () => {
    try {
      const response = await getUserSessions(user.id);
      const sessionsList = response.data || [];
      setUserSessions(sessionsList);
      
      if (sessionsList.length > 0) {
        // Select the most recent session
        const recentSession = sessionsList.sort((a, b) => new Date(b.startedAt) - new Date(a.startedAt))[0];
        setSelectedSession(recentSession.sessionToken);
      }
      
      setCurrentView('dashboard');
    } catch (error) {
      console.log('No user sessions found');
      setCurrentView('dashboard');
    }
  };

  const handleResumeUpload = async (file) => {
    setLoading(true);
    setError(null);
    try {
      const response = await uploadResume(file, selectedRole, user?.id);
      setUploadedResume({ file, data: response.data.resumeData });
      setResumeRecord(response.data.resumeData);
      setAssessmentSession(response.data.session);
      setShowVerify(true);
      console.log('Resume uploaded successfully:', response.data);
    } catch (err) {
      console.error('Resume upload failed:', err);
      const backendMessage = err?.response?.data?.message || err?.response?.data?.error || err?.message;
      setError(`Failed to upload resume.${backendMessage ? ' ' + backendMessage : ''}`);
    } finally {
      setLoading(false);
    }
  };

  const handleRoleSelect = (role) => {
    setSelectedRole(role);
    console.log('Role selected:', role);
  };

  const handleConfirmResume = async (edited) => {
    try {
      if (edited) {
        await updateResume(edited);
        setResumeRecord(edited);
      }
      setShowVerify(false);
      setCurrentView('assessment');
    } catch (err) {
      console.error('Resume update failed:', err);
      setError('Failed to update resume data');
    }
  };

  const handleStartAssessment = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await startAssessment(selectedRole);
      setAssessmentSession(response.data);
      setCurrentView('assessment');
    } catch (err) {
      console.error('Assessment start failed:', err);
      setError('Failed to start assessment');
    } finally {
      setLoading(false);
    }
  };

  const handleAssessmentComplete = () => {
    setCurrentView('dashboard');
    loadUserSessions(); // Refresh sessions
  };

  const handleBackToDashboard = () => {
    setCurrentView('dashboard');
  };

  const handleViewSession = (session) => {
    setSelectedSession(session.sessionToken);
    setCurrentView('dashboard');
  };

  const handleStartNewAssessment = async (targetRole) => {
    setAssessmentSession(null);
    setSelectedSession(null);
    if (targetRole) {
      setSelectedRole(targetRole);
      await handleStartAssessment();
    } else {
      // Check if user has existing sessions (indicating they've been through onboarding before)
      if (userSessions.length > 0) {
        setCurrentView('role-selection');
      } else {
        setCurrentView('onboarding');
      }
    }
  };

  const handleBackToLanding = () => {
    setCurrentView('landing');
    setOnboarding({ hasRegistered: false });
    setSelectedRole(null);
    setUploadedResume(null);
    setAssessmentSession(null);
    setSelectedSession(null);
  };

  const handleLoginSuccess = async () => {
    // Check if user has past sessions before redirecting
    try {
      const response = await getUserSessions(user.id);
      const sessionsList = response.data || [];
      if (sessionsList.length > 0) {
        // User has past actions, go to dashboard
        setCurrentView('dashboard');
        loadUserSessions();
      } else {
        // No past actions, go to onboarding
        setCurrentView('onboarding');
      }
    } catch (error) {
      console.log('Error checking user history on login:', error);
      setCurrentView('onboarding');
    }
  };

  const handleRegisterSuccess = async (resumeFile) => {
    if (resumeFile) {
      // Handle resume upload after registration
      await handleResumeUpload(resumeFile);
      setCurrentView('dashboard');
    } else {
      setCurrentView('onboarding');
    }
  };

  const handleLogout = () => {
    logout();
    setCurrentView('landing');
    setOnboarding({ hasRegistered: false });
    setSelectedRole(null);
    setUploadedResume(null);
    setAssessmentSession(null);
    setSelectedSession(null);
    setUserSessions([]);
  };

  if (userLoading) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-slate-50 via-blue-50 to-indigo-50 flex items-center justify-center">
        <div className="text-center">
          <Loader2 className="w-12 h-12 animate-spin text-indigo-600 mx-auto mb-4" />
          <h2 className="text-xl font-semibold text-gray-700 mb-2">Loading Application</h2>
          <p className="text-gray-500">Please wait while we initialize your session...</p>
        </div>
      </div>
    );
  }

  // Navigation Header
  const NavigationHeader = () => (
    <div className="bg-white shadow-sm border-b border-gray-200">
      <div className="max-w-7xl mx-auto px-6 py-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-4">
            <button
              onClick={handleBackToLanding}
              className="flex items-center space-x-2 text-gray-600 hover:text-gray-800 transition"
            >
              <Home className="w-5 h-5" />
              <span>Neuro-RAG</span>
            </button>
            {currentView !== 'landing' && (
              <button
                onClick={() => setCurrentView('dashboard')}
                className="flex items-center space-x-2 text-gray-600 hover:text-gray-800 transition"
              >
                <ArrowLeft className="w-4 h-4" />
                <span>Dashboard</span>
              </button>
            )}
          </div>
          {user && (
            <div className="flex items-center space-x-4">
              <span className="text-sm text-gray-600">
                Welcome, {user.email?.split('@')[0] || 'User'}
              </span>
              <div className="w-8 h-8 bg-indigo-100 rounded-full flex items-center justify-center">
                <span className="text-sm font-semibold text-indigo-600">
                  {user.email?.charAt(0).toUpperCase() || 'U'}
                </span>
              </div>
              <button
                onClick={handleLogout}
                className="text-sm text-gray-600 hover:text-gray-800 transition"
              >
                Logout
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 via-blue-50 to-indigo-50">
      {user && <NavigationHeader />}

      <main className="container mx-auto px-6 py-12">
        {error && (
          <div className="mb-6 bg-red-50 border border-red-200 rounded-lg p-4">
            <div className="flex">
              <div className="flex-shrink-0">
                <svg className="h-5 w-5 text-red-400" viewBox="0 0 20 20" fill="currentColor">
                  <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clipRule="evenodd" />
                </svg>
              </div>
              <div className="ml-3">
                <h3 className="text-sm font-medium text-red-800">Error</h3>
                <div className="mt-2 text-sm text-red-700">
                  <p>{error}</p>
                </div>
              </div>
            </div>
          </div>
        )}

        {currentView === 'landing' && (
          <div>
            <Hero />
            <section id="get-started" className="text-center max-w-4xl mx-auto mt-12">
              <h2 className="text-4xl font-bold mb-4 bg-gradient-to-r from-indigo-600 to-purple-600 bg-clip-text text-transparent">
                Get Started with Your Skill Assessment
              </h2>
              <p className="text-gray-600 mb-12 text-lg">
                Login or Register first. After registering, upload your resume and select your target role to begin your personalized assessment.
              </p>

              <div className="space-y-8">
                <div className="flex flex-col items-center gap-4">
                  <button
                    onClick={() => setShowRegisterModal(true)}
                    className="bg-indigo-600 text-white px-6 py-3 rounded-lg hover:bg-indigo-700 transition font-semibold"
                  >
                    I'm New — Register
                  </button>
                  <button
                    onClick={() => setShowLoginModal(true)}
                    className="bg-white border px-6 py-3 rounded-lg hover:bg-slate-50 transition font-semibold"
                  >
                    I Have an Account — Login
                  </button>
                </div>
                <ResumeVerificationModal
                  isOpen={showVerify}
                  onClose={() => setShowVerify(false)}
                  onConfirm={handleConfirmResume}
                  resumeData={resumeRecord}
                />
              </div>
            </section>
          </div>
        )}

        {currentView === 'onboarding' && (
          <div className="max-w-2xl mx-auto">
            <div className="bg-white rounded-xl shadow-lg p-8">
              <h2 className="text-2xl font-bold text-gray-800 mb-6">Complete Your Profile</h2>
              <div className="space-y-6">
                <ResumeUploader onUpload={handleResumeUpload} loading={loading} />
                <RoleSelector onSelectRole={handleRoleSelect} />
                <div className="flex justify-end">
                  <button
                    onClick={handleStartAssessment}
                    disabled={!selectedRole || loading}
                    className="bg-indigo-600 text-white px-6 py-3 rounded-lg hover:bg-indigo-700 transition font-semibold disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    Start Assessment
                  </button>
                </div>
              </div>
            </div>
          </div>
        )}

        {currentView === 'role-selection' && (
          <div className="max-w-2xl mx-auto">
            <div className="bg-white rounded-xl shadow-lg p-8">
              <h2 className="text-2xl font-bold text-gray-800 mb-6">Select Your Target Role</h2>
              <div className="space-y-6">
                <RoleSelector onSelectRole={handleRoleSelect} />
                <div className="flex justify-end">
                  <button
                    onClick={handleStartAssessment}
                    disabled={!selectedRole || loading}
                    className="bg-indigo-600 text-white px-6 py-3 rounded-lg hover:bg-indigo-700 transition font-semibold disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    Start Assessment
                  </button>
                </div>
              </div>
            </div>
          </div>
        )}

        {currentView === 'assessment' && assessmentSession && (
          <EnhancedAssessmentFlow
            sessionToken={assessmentSession.sessionToken}
            onComplete={handleAssessmentComplete}
            onBack={handleBackToDashboard}
          />
        )}

        {currentView === 'dashboard' && (
          <EnhancedDashboard
            userId={user?.id}
            sessionId={selectedSession || assessmentSession?.sessionToken}
            skillDependencies={[]}
            loading={loading}
            skillData={[]}
            onStartNewAssessment={handleStartNewAssessment}
            onLogout={handleLogout}
          />
        )}

        {currentView === 'roadmap' && assessmentSession && (
          <RoadmapPage
            sessionId={assessmentSession.id}
            onBackToDashboard={handleBackToDashboard}
          />
        )}

        <LoginModal
          isOpen={showLoginModal}
          onClose={() => setShowLoginModal(false)}
          onLoginSuccess={handleLoginSuccess}
        />
        <RegisterModal
          isOpen={showRegisterModal}
          onClose={() => setShowRegisterModal(false)}
          onRegisterSuccess={handleRegisterSuccess}
        />
      </main>
    </div>
  );
};

function App() {
  return (
    <UserProvider>
      <AppContent />
    </UserProvider>
  );
}

export default App;










