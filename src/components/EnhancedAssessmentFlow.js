import React, { useState, useEffect } from 'react';
import { useAssessment } from '../hooks/useAssessment';
import { Clock, CheckCircle, XCircle, ArrowRight, ArrowLeft, Brain, AlertCircle } from 'lucide-react';

const EnhancedAssessmentFlow = ({ sessionToken, onComplete, onBack }) => {
  const {
    currentQuestion,
    progress,
    shouldStop,
    stopReason,
    loading,
    submitResponse
  } = useAssessment(sessionToken);

  const [answer, setAnswer] = useState('');
  const [selectedOption, setSelectedOption] = useState('');
  const [confidence, setConfidence] = useState(0.5);
  const [timeSpent, setTimeSpent] = useState(0);
  const [startTime, setStartTime] = useState(Date.now());
  const [showExplanation, setShowExplanation] = useState(false);
  const [isCorrect, setIsCorrect] = useState(null);

  useEffect(() => {
    if (currentQuestion) {
      setAnswer('');
      setSelectedOption('');
      setConfidence(0.5);
      setTimeSpent(0);
      setStartTime(Date.now());
      setShowExplanation(false);
      setIsCorrect(null);
    }
  }, [currentQuestion]);

  useEffect(() => {
    const timer = setInterval(() => {
      setTimeSpent(Math.floor((Date.now() - startTime) / 1000));
    }, 1000);

    return () => clearInterval(timer);
  }, [startTime]);

  const handleSubmit = async () => {
    if (loading) return;

    const responseData = {
      sessionToken,
      questionId: currentQuestion.id,
      responseText: (currentQuestion.questionType === 'text' || currentQuestion.questionType === 'typing') ? answer : undefined,
      responseChoice: (currentQuestion.questionType === 'mcq' || currentQuestion.questionType === 'choice') ? selectedOption : undefined,
      confidenceLevel: confidence,
      thinkTimeSeconds: timeSpent,
      totalTimeSeconds: timeSpent,
      isCorrect: (currentQuestion.questionType === 'mcq' || currentQuestion.questionType === 'choice') ?
        (selectedOption === currentQuestion.correctAnswer) : null
    };

    if (currentQuestion.questionType === 'text' || currentQuestion.questionType === 'typing') {
      const words = (answer || '').trim() === '' ? 0 : (answer || '').trim().split(/\s+/).length;
      const minutes = Math.max(0.001, timeSpent / 60);
      responseData.wordCount = words;
      responseData.charCount = (answer || '').length;
      responseData.typingSpeedWpm = Math.round((words / minutes) * 100) / 100;
    }

    try {
      await submitResponse(responseData);

      // Reset state after successful submission
      setAnswer('');
      setSelectedOption('');
      setConfidence(0.5);
      setTimeSpent(0);
      setStartTime(Date.now());

      if (currentQuestion.questionType === 'mcq') {
        setIsCorrect(selectedOption === currentQuestion.correctAnswer);
        setShowExplanation(true);

        // Show explanation for 3 seconds before moving to next question
        setTimeout(() => {
          setShowExplanation(false);
        }, 3000);
      }
    } catch (error) {
      console.error('Error submitting response:', error);
      // Don't reset state on error so user can retry
    }
  };

  const isTextQuestion = currentQuestion?.questionType === 'text' || currentQuestion?.questionType === 'typing';
  const isMcqQuestion = currentQuestion?.questionType === 'mcq';
  const canSubmit = isTextQuestion ? answer.trim().length > 0 : selectedOption !== '';

  if (shouldStop) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-slate-50 via-blue-50 to-indigo-50 flex items-center justify-center">
        <div className="max-w-2xl mx-auto text-center p-8">
          <div className="bg-white rounded-2xl shadow-2xl p-8">
            <div className="mb-6">
              <div className="bg-green-100 w-20 h-20 rounded-full flex items-center justify-center mx-auto mb-4">
                <CheckCircle className="w-10 h-10 text-green-600" />
              </div>
              <h2 className="text-3xl font-bold text-gray-800 mb-2">Assessment Complete!</h2>
              <p className="text-gray-600 text-lg">
                {stopReason === 'NO_MORE_QUESTIONS' 
                  ? 'You have answered all available questions for your skill level.'
                  : 'The assessment has been completed based on your responses.'
                }
              </p>
            </div>
            
            <div className="bg-gray-50 rounded-lg p-6 mb-6">
              <h3 className="text-lg font-semibold text-gray-800 mb-4">Assessment Summary</h3>
              <div className="grid grid-cols-2 gap-4 text-sm">
                <div>
                  <span className="text-gray-600">Questions Answered:</span>
                  <span className="font-semibold ml-2">{progress?.questionsAnswered || 0}</span>
                </div>
                <div>
                  <span className="text-gray-600">Time Spent:</span>
                  <span className="font-semibold ml-2">{Math.floor(timeSpent / 60)}m {timeSpent % 60}s</span>
                </div>
              </div>
            </div>

            <div className="flex space-x-4">
              <button
                onClick={onBack}
                className="flex-1 bg-gray-100 hover:bg-gray-200 text-gray-700 px-6 py-3 rounded-lg transition font-semibold"
              >
                Back to Dashboard
              </button>
              <button
                onClick={onComplete}
                className="flex-1 bg-gradient-to-r from-indigo-600 to-purple-600 text-white px-6 py-3 rounded-lg hover:from-indigo-700 hover:to-purple-700 transition font-semibold"
              >
                View Results
              </button>
            </div>
          </div>
        </div>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-slate-50 via-blue-50 to-indigo-50 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-16 w-16 border-b-2 border-indigo-600 mx-auto mb-4"></div>
          <h2 className="text-xl font-semibold text-gray-700 mb-2">Loading Next Question</h2>
          <p className="text-gray-500">Preparing your personalized assessment...</p>
        </div>
      </div>
    );
  }

  if (!currentQuestion) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-slate-50 via-blue-50 to-indigo-50 flex items-center justify-center">
        <div className="text-center">
          <AlertCircle className="w-16 h-16 text-gray-400 mx-auto mb-4" />
          <h2 className="text-xl font-semibold text-gray-700 mb-2">No Questions Available</h2>
          <p className="text-gray-500">Unable to load questions at this time.</p>
        </div>
      </div>
    );
  }

  const options = isMcqQuestion ? JSON.parse(currentQuestion.options || '[]') : [];

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 via-blue-50 to-indigo-50">
      <div className="max-w-4xl mx-auto px-6 py-8">
        {/* Header */}
        {/* <div className="bg-white rounded-xl shadow-lg p-6 mb-8">
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center space-x-4">
              <div className="bg-indigo-100 p-2 rounded-lg">
                <Brain className="w-6 h-6 text-indigo-600" />
              </div>
              <div>
                <h1 className="text-xl font-semibold text-gray-800">Skill Assessment</h1>
                <p className="text-gray-600">
                  Question {progress?.questionsAnswered + 1 || 1} • {currentQuestion.difficulty}
                </p>
              </div>
            </div>
            <div className="flex items-center space-x-4">
              <div className="flex items-center space-x-2 text-sm text-gray-600">
                <Clock className="w-4 h-4" />
                <span>{Math.floor(timeSpent / 60)}:{(timeSpent % 60).toString().padStart(2, '0')}</span>
              </div>
              <div className="w-32 bg-gray-200 rounded-full h-2">
                <div
                  className="bg-indigo-600 h-2 rounded-full transition-all duration-300"
                  style={{ width: `${((progress?.questionsAnswered || 0) / (progress?.totalQuestions || 1)) * 100}%` }}
                ></div>
              </div>
            </div>
          </div>
        </div> */}

        {/* Question Card */}
        <div className="bg-white rounded-xl shadow-lg p-8 mb-8">
          <div className="mb-6">
            <div className="flex items-center mb-4">
              <span className="bg-indigo-100 text-indigo-800 px-3 py-1 rounded-full text-sm font-medium">
                {isMcqQuestion ? 'Multiple Choice' : 'Text Question'}
              </span>
              {currentQuestion.topic && (
                <span className="bg-gray-100 text-gray-700 px-3 py-1 rounded-full text-sm font-medium ml-2">
                  {currentQuestion.topic}
                </span>
              )}
            </div>
            <h2 className="text-2xl font-semibold text-gray-800 leading-relaxed">
              {currentQuestion.questionText}
            </h2>
          </div>

          {/* MCQ Options */}
          {isMcqQuestion && (
            <div className="space-y-3 mb-6">
              {options.map((option, index) => (
                <label
                  key={index}
                  className={`block p-4 rounded-lg border-2 cursor-pointer transition-all ${
                    selectedOption === option
                      ? 'border-indigo-500 bg-indigo-50'
                      : 'border-gray-200 hover:border-gray-300 hover:bg-gray-50'
                  }`}
                >
                  <input
                    type="radio"
                    name="answer"
                    value={option}
                    checked={selectedOption === option}
                    onChange={(e) => setSelectedOption(e.target.value)}
                    className="sr-only"
                  />
                  <div className="flex items-center">
                    <div className={`w-4 h-4 rounded-full border-2 mr-3 ${
                      selectedOption === option
                        ? 'border-indigo-500 bg-indigo-500'
                        : 'border-gray-300'
                    }`}>
                      {selectedOption === option && (
                        <div className="w-2 h-2 bg-white rounded-full mx-auto mt-0.5"></div>
                      )}
                    </div>
                    <span className="text-gray-800">{option}</span>
                  </div>
                </label>
              ))}
            </div>
          )}

          {/* Text Answer Input */}
          {isTextQuestion && (
            <div className="mb-6">
              <textarea
                value={answer}
                onChange={(e) => setAnswer(e.target.value)}
                placeholder="Type your answer here..."
                className="w-full h-32 p-4 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 resize-none"
                style={{ display: 'block' }}
              />
              <div className="flex justify-between items-center mt-2 text-sm text-gray-500">
                <span>{answer.length} characters</span>
                {currentQuestion.suggestedAnswerLength && (
                  <span>Suggested length: {currentQuestion.suggestedAnswerLength} words</span>
                )}
              </div>
            </div>
          )}

          {/* Confidence Slider */}
          <div className="mb-6">
            <label className="block text-sm font-medium text-gray-700 mb-2">
              How confident are you in your answer? ({Math.round(confidence * 100)}%)
            </label>
            <input
              type="range"
              min="0"
              max="1"
              step="0.1"
              value={confidence}
              onChange={(e) => setConfidence(parseFloat(e.target.value))}
              className="w-full h-2 bg-gray-200 rounded-lg appearance-none cursor-pointer slider"
            />
            <div className="flex justify-between text-xs text-gray-500 mt-1">
              <span>Not confident</span>
              <span>Very confident</span>
            </div>
          </div>

          {/* Explanation for MCQ */}
          {showExplanation && isMcqQuestion && (
            <div className={`mb-6 p-4 rounded-lg border-2 ${
              isCorrect 
                ? 'bg-green-50 border-green-200' 
                : 'bg-red-50 border-red-200'
            }`}>
              <div className="flex items-center mb-2">
                {isCorrect ? (
                  <CheckCircle className="w-5 h-5 text-green-600 mr-2" />
                ) : (
                  <XCircle className="w-5 h-5 text-red-600 mr-2" />
                )}
                <span className={`font-semibold ${
                  isCorrect ? 'text-green-800' : 'text-red-800'
                }`}>
                  {isCorrect ? 'Correct!' : 'Incorrect'}
                </span>
              </div>
              {currentQuestion.explanation && (
                <p className={`text-sm ${
                  isCorrect ? 'text-green-700' : 'text-red-700'
                }`}>
                  {currentQuestion.explanation}
                </p>
              )}
            </div>
          )}

          {/* Submit Button */}
          <div className="flex justify-between">
            <button
              onClick={onBack}
              className="flex items-center space-x-2 px-6 py-3 text-gray-600 hover:text-gray-800 transition"
            >
              <ArrowLeft className="w-4 h-4" />
              <span>Back</span>
            </button>
            
            <button
              onClick={handleSubmit}
              disabled={!canSubmit || loading}
              className={`flex items-center space-x-2 px-8 py-3 rounded-lg font-semibold transition ${
                canSubmit && !loading
                  ? 'bg-gradient-to-r from-indigo-600 to-purple-600 text-white hover:from-indigo-700 hover:to-purple-700'
                  : 'bg-gray-300 text-gray-500 cursor-not-allowed'
              }`}
            >
              {loading ? (
                <>
                  <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white"></div>
                  <span>Submitting...</span>
                </>
              ) : (
                <>
                  <span>Submit Answer</span>
                  <ArrowRight className="w-4 h-4" />
                </>
              )}
            </button>
          </div>
        </div>
      </div>

      <style jsx>{`
        .slider::-webkit-slider-thumb {
          appearance: none;
          height: 20px;
          width: 20px;
          border-radius: 50%;
          background: #6366f1;
          cursor: pointer;
        }
        .slider::-moz-range-thumb {
          height: 20px;
          width: 20px;
          border-radius: 50%;
          background: #6366f1;
          cursor: pointer;
          border: none;
        }
      `}</style>
    </div>
  );
};

export default EnhancedAssessmentFlow;







