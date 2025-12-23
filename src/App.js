import React, { useState, useEffect } from 'react';
import { useUser } from './contexts/UserContext';
import { useNavigate, Link } from 'react-router-dom';
import Hero from './components/Hero';
import ResumeUploader from './components/ResumeUploader';
import RoleSelector from './components/RoleSelector';
import AssessmentFlow from './components/AssessmentFlow';
import EnhancedDashboard from './components/EnhancedDashboard';
import RoadmapPage from './components/RoadmapPage';
import TopBar from './components/TopBar';
import { uploadResume, updateResume } from './services/resumeService';
import ResumeVerificationModal from './components/ResumeVerificationModal';
import { startAssessment } from './services/assessmentService';
import './App.css';

function App() {
  const { user, token, loading: authLoading, logout } = useUser();
  const navigate = useNavigate();

  const [uploadedResume, setUploadedResume] = useState(null);
  const [selectedRole, setSelectedRole] = useState(null);
  const [assessmentSession, setAssessmentSession] = useState(null);
  const [currentView, setCurrentView] = useState(() => {
    // Restore currentView from localStorage if user exists
    const storedView = localStorage.getItem('currentView');
    return (storedView && user) ? storedView : 'landing';
  }); // landing, onboarding, assessment, dashboard, roadmap
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [onboarding, setOnboarding] = useState({ hasRegistered: false });
  const [showVerify, setShowVerify] = useState(false);
  const [resumeRecord, setResumeRecord] = useState(null);

  // Handle logout
  const handleLogout = () => {
    logout();
    localStorage.removeItem('currentView');
    setCurrentView('landing');
  };

  // Check authentication status on app load
  useEffect(() => {
    if (!authLoading) {
      if (!user || !token) {
        // No valid authentication, show login/register options
        setCurrentView('landing');
      } else {
        // User is authenticated, allow access to app
        const params = new URLSearchParams(window.location.search);
        if (params.get('onboarding') === '1') {
          setOnboarding({ hasRegistered: true });
          const el = document.getElementById('get-started');
          if (el) el.scrollIntoView({ behavior: 'smooth' });
        }
      }
    }
  }, [user, token, authLoading]);

  // Update currentView in localStorage when it changes
  useEffect(() => {
    if (user && currentView !== 'landing') {
      localStorage.setItem('currentView', currentView);
    }
  }, [currentView, user]);

  // Reset to landing if user becomes null (logout)
  useEffect(() => {
    if (!user) {
      setCurrentView('landing');
      localStorage.removeItem('currentView');
    }
  }, [user]);

  const handleResumeUpload = async (file) => {
    if (!user) {
      navigate('/login');
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const response = await uploadResume(file, selectedRole, user.id);
      setUploadedResume({ file, data: response.data.resumeData });
      setResumeRecord(response.data.resumeData);
      // The session is now created during resume upload
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
      if (!resumeRecord?.id) return setShowVerify(false);
      const payload = {
        extractedEducation: JSON.stringify(edited.education || []),
        extractedSkills: JSON.stringify((edited.skills || []).map(s => ({ name: s }))),
        extractedExperience: resumeRecord.extractedExperience, // Keep existing if not editing
        totalYearsExperience: edited.experienceYears || resumeRecord.totalYearsExperience,
      };
      const res = await updateResume(resumeRecord.id, payload);
      setUploadedResume({ ...uploadedResume, data: res.data });
    } catch (e) {
      // Silently continue; user can proceed anyway
    } finally {
      setShowVerify(false);
    }
  };



  const handleAssessmentComplete = (progress) => {
    setCurrentView('dashboard');
  };

  const handleViewRoadmap = () => {
    setCurrentView('roadmap');
  };

  const handleBackToLanding = () => {
    setCurrentView('landing');
    setUploadedResume(null);
    setSelectedRole(null);
    setAssessmentSession(null);
    setError(null);
  };

  if (currentView === 'assessment' && assessmentSession) {
    return (
      <AssessmentFlow
        sessionToken={assessmentSession.sessionToken}
        sessionId={assessmentSession.id}
        onComplete={handleAssessmentComplete}
      />
    );
  }

  if (currentView === 'dashboard') {
    return (
      <EnhancedDashboard
        sessionId={assessmentSession?.sessionToken}
        onViewRoadmap={handleViewRoadmap}
        onBackToLanding={handleBackToLanding}
        onLogout={handleLogout}
      />
    );
  }

  if (currentView === 'roadmap') {
    return <RoadmapPage sessionId={assessmentSession?.sessionToken} onBackToDashboard={() => setCurrentView('dashboard')} />;
  }

  // Landing shows CTA to login/register and then reveals onboarding (resume upload) post-register
  return (
    <div className="App min-h-screen bg-gradient-to-br from-blue-50 via-indigo-50 to-purple-50">
      <TopBar />
      <Hero />
      <main className="container mx-auto px-6 py-12">
        <section id="get-started" className="text-center max-w-4xl mx-auto">
          <h2 className="text-4xl font-bold mb-4 bg-gradient-to-r from-indigo-600 to-purple-600 bg-clip-text text-transparent">
            Get Started with Your Skill Assessment
          </h2>
          <p className="text-gray-600 mb-12 text-lg">
            Login or Register first. After registering, upload your resume and select your target role to begin your personalized assessment.
          </p>

          <div className="space-y-8">
            {!user ? (
              <div className="flex flex-col items-center gap-4">
                <Link
                  to="/register"
                  className="bg-indigo-600 text-white px-6 py-3 rounded-lg hover:bg-indigo-700 transition font-semibold inline-block text-center"
                >
                  I'm New — Register
                </Link>
                <Link
                  to="/login"
                  className="bg-white border px-6 py-3 rounded-lg hover:bg-slate-50 transition font-semibold inline-block text-center"
                >
                  I Have an Account — Login
                </Link>
              </div>
            ) : (
              <>
                <ResumeUploader onUpload={handleResumeUpload} loading={loading} />
                <RoleSelector onSelectRole={handleRoleSelect} />
              </>
            )}
            <ResumeVerificationModal
              open={showVerify}
              parsed={{
                name: '',
                role: selectedRole || '',
                education: [],
                skills: [],
                experienceYears: uploadedResume?.data?.totalYearsExperience || 0,
              }}
              onConfirm={handleConfirmResume}
              onClose={() => setShowVerify(false)}
            />

            {error && (
              <div className="mt-4 p-4 bg-red-100 border border-red-300 rounded-lg text-red-800">
                {error}
              </div>
            )}

            {uploadedResume && selectedRole && assessmentSession && (
              <div className="mt-8">
                <div className="bg-green-50 border border-green-200 rounded-xl p-6 mb-6">
                  <div className="flex items-center justify-center mb-4">
                    <div className="bg-green-100 rounded-full p-3">
                      <svg className="w-6 h-6 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                      </svg>
                    </div>
                  </div>
                  <h3 className="text-lg font-semibold text-green-800 mb-2">Ready to Start Assessment!</h3>
                  <p className="text-green-700 mb-4">
                    Resume: <span className="font-medium">{uploadedResume.file.name}</span><br />
                    Target Role: <span className="font-medium">{selectedRole}</span>
                  </p>
                  <button
                    onClick={() => setCurrentView('assessment')}
                    disabled={loading}
                    className="bg-gradient-to-r from-indigo-600 to-purple-600 text-white font-semibold py-3 px-8 rounded-lg hover:from-indigo-700 hover:to-purple-700 transform hover:scale-105 transition-all duration-200 shadow-lg disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    Start Assessment
                  </button>
                </div>
              </div>
            )}
          </div>
        </section>
      </main>


    </div>
  );
}

export default App;
