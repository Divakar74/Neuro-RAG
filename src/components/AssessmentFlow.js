import React, { useState } from 'react';
import { useAssessment } from '../hooks/useAssessment';
import QuestionCard from './QuestionCard';
import ProgressTracker from './ProgressTracker';
import { CheckCircle, AlertCircle, Loader2 } from 'lucide-react';
import Confetti from 'react-confetti';

const AssessmentFlow = ({ sessionToken, sessionId, onComplete }) => {
  const {
    currentQuestion,
    progress,
    shouldStop,
    stopReason,
    loading,
    submitResponse
  } = useAssessment(sessionToken);

  const [showCompletion, setShowCompletion] = useState(false);
  const [showConfetti, setShowConfetti] = useState(false);

  const handleResponseSubmit = async (responseData) => {
    try {
      await submitResponse({
        ...responseData,
        sessionId: sessionId
      });
      // No need to manually set loading here - useAssessment hook handles it
    } catch (error) {
      console.error('Error submitting response:', error);
      // Handle error (show toast, etc.)
    }
  };

  const handleAssessmentComplete = () => {
    setShowConfetti(true);
    setShowCompletion(true);
    setTimeout(() => {
      setShowConfetti(false);
      onComplete && onComplete(progress);
    }, 5000); // Show confetti for 5 seconds
  };

  if (loading && !currentQuestion && !shouldStop) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100 flex items-center justify-center">
        <div className="text-center">
          <Loader2 className="w-12 h-12 animate-spin text-blue-600 mx-auto mb-4" />
          <h2 className="text-xl font-semibold text-gray-700 mb-2">Loading Assessment</h2>
          <p className="text-gray-500">Preparing your personalized questions...</p>
        </div>
      </div>
    );
  }

  if (shouldStop && !showCompletion) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-green-50 to-emerald-100 flex items-center justify-center">
        <div className="text-center">
          <CheckCircle className="w-16 h-16 text-green-600 mx-auto mb-4" />
          <h2 className="text-2xl font-bold text-gray-900 mb-2">Assessment Complete!</h2>
          <p className="text-gray-600 mb-6">
            You've successfully completed your skill assessment.
            {stopReason && ` Reason: ${stopReason}`}
          </p>
          <button
            onClick={handleAssessmentComplete}
            className="px-6 py-3 bg-green-600 text-white font-medium rounded-lg hover:bg-green-700 transition-all duration-300 transform hover:scale-105"
          >
            View Results
          </button>
        </div>
      </div>
    );
  }

  if (showCompletion) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-green-50 to-emerald-100 flex items-center justify-center relative">
        {showConfetti && <Confetti recycle={false} numberOfPieces={200} />}
        <div className="text-center z-10">
          <div className="animate-bounce">
            <CheckCircle className="w-16 h-16 text-green-600 mx-auto mb-4" />
          </div>
          <h2 className="text-2xl font-bold text-gray-900 mb-2">Congratulations!</h2>
          <p className="text-gray-600">
           Redirecting to your dashboard...          </p>
        </div>
      </div>
    );
  }

  if (!currentQuestion) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100 flex items-center justify-center">
        <div className="text-center">
          <AlertCircle className="w-12 h-12 text-yellow-600 mx-auto mb-4" />
          <h2 className="text-xl font-semibold text-gray-700 mb-2">No Questions Available</h2>
          <p className="text-gray-500">Please try starting a new assessment.</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100 py-8 px-4">
      <div className="max-w-6xl mx-auto">
        {/* Progress Tracker - Show question tracker only */}
        <div className="mb-8 animate-fade-in">
          <ProgressTracker progress={progress} mode="circular" />
        </div>

        {/* Question Card */}
        <div className="transition-transform duration-300 ease-out will-change-transform animate-slide-up">
          <QuestionCard
            question={currentQuestion}
            onSubmit={handleResponseSubmit}
            loading={loading}
          />
        </div>
        
        {/* Footer with actions */}
        <div className="mt-8 flex items-center justify-end animate-fade-in">
          <div className="flex gap-3">
            <button
              type="button"
              className="px-4 py-2 border rounded-lg text-gray-700 hover:bg-gray-50"
              onClick={() => {
                // Light client-side save notion; server auto-saves per answer
                alert('Progress saved. You can resume later.');
              }}
            >
              Save Progress
            </button>
            <button
              type="button"
              className="px-4 py-2 bg-emerald-600 text-white rounded-lg hover:bg-emerald-700"
              onClick={() => {
                // Let backend decide stopping; we show results view
                window.scrollTo({ top: 0, behavior: 'smooth' });
                setShowConfetti(true);
                setShowCompletion(true);
                setTimeout(() => {
                  setShowConfetti(false);
                  onComplete && onComplete(progress);
                }, 1500);
              }}
            >
              Submit Assessment
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default AssessmentFlow;
