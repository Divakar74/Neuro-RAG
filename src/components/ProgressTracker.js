import React from 'react';
import { CheckCircle, Circle, TrendingUp } from 'lucide-react';

const CircularProgress = ({ percent = 0 }) => {
  const size = 84;
  const strokeWidth = 10;
  const radius = (size - strokeWidth) / 2;
  const circumference = 2 * Math.PI * radius;
  const offset = circumference - (percent / 100) * circumference;

  return (
    <svg width={size} height={size} className="transform -rotate-90">
      <circle
        cx={size / 2}
        cy={size / 2}
        r={radius}
        stroke="#E5E7EB"
        strokeWidth={strokeWidth}
        fill="transparent"
      />
      <circle
        cx={size / 2}
        cy={size / 2}
        r={radius}
        stroke="#6366F1"
        strokeWidth={strokeWidth}
        strokeLinecap="round"
        strokeDasharray={circumference}
        strokeDashoffset={offset}
        fill="transparent"
        className="transition-all duration-300 ease-out"
      />
    </svg>
  );
};

const ProgressTracker = ({ progress, mode = 'circular' }) => {
  if (!progress) return null;

  const { questionsAnswered, totalQuestions, overallProgress, skillConfidenceLevels } = progress;

  // Calculate progress percentage
  const progressPercentage = totalQuestions > 0 ? (questionsAnswered / totalQuestions) * 100 : 0;

  // Get top 5 skills by confidence
  const topSkills = Object.entries(skillConfidenceLevels || {})
    .sort(([,a], [,b]) => b - a)
    .slice(0, 5);

  return (
    <div className="bg-white rounded-xl shadow-lg p-6 mb-6">
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-lg font-semibold text-gray-900">Assessment Progress</h3>
        {mode === 'circular' ? (
          <div className="flex items-center space-x-3">
            <div className="relative">
              <CircularProgress percent={Math.min(progressPercentage, 100)} />
              <div className="absolute inset-0 flex items-center justify-center rotate-90">
                <span className="text-sm font-semibold text-indigo-600">
                  {Math.round(progressPercentage)}%
                </span>
              </div>
            </div>
          </div>
        ) : (
          <div className="flex items-center space-x-2">
            <TrendingUp className="w-5 h-5 text-green-600" />
            <span className="text-sm font-medium text-green-600">
              {Math.round(overallProgress * 100)}% Complete
            </span>
          </div>
        )}
      </div>

      {/* Progress Bar (shown also in circular mode for clarity) */}
      <div className="mb-6">
        <div className="flex justify-between text-sm text-gray-600 mb-2">
          <span>Questions Answered</span>
          <span>{questionsAnswered} / 15</span>
        </div>
        <div className="w-full bg-gray-200 rounded-full h-3 overflow-hidden">
          <div
            className="bg-gradient-to-r from-indigo-500 to-purple-500 h-3 rounded-full transition-all duration-500 ease-out"
            style={{ width: `${Math.min(progressPercentage, 100)}%` }}
          ></div>
        </div>
      </div>

      {/* Skill Confidence Overview - Hidden */}
      {/* {topSkills.length > 0 && (
        <div>
          <h4 className="text-sm font-medium text-gray-700 mb-3">Top Skills Assessed</h4>
          <div className="space-y-2">
            {topSkills.map(([skillCode, confidence]) => (
              <div key={skillCode} className="flex items-center justify-between">
                <div className="flex items-center space-x-2">
                  {confidence > 0.7 ? (
                    <CheckCircle className="w-4 h-4 text-green-600" />
                  ) : (
                    <Circle className="w-4 h-4 text-gray-400" />
                  )}
                  <span className="text-sm text-gray-700 capitalize">
                    {skillCode.replace('_', ' ')}
                  </span>
                </div>
                <div className="flex items-center space-x-2">
                  <div className="w-16 bg-gray-200 rounded-full h-2">
                    <div
                      className={`h-2 rounded-full ${
                        confidence > 0.7 ? 'bg-green-600' :
                        confidence > 0.4 ? 'bg-yellow-500' : 'bg-red-500'
                      }`}
                      style={{ width: `${confidence * 100}%` }}
                    ></div>
                  </div>
                  <span className="text-xs text-gray-500 w-8">
                    {Math.round(confidence * 100)}%
                  </span>
                </div>
              </div>
            ))}
          </div>
        </div>
      )} */}

      {/* Assessment Tips */}
      <div className="mt-6 p-4 bg-blue-50 rounded-lg">
        <p className="text-sm text-blue-800">
          💡 <strong>Tip:</strong> Take your time to provide thoughtful answers.
          The system adapts based on your responses to give you the most relevant questions.
        </p>
      </div>
    </div>
  );
};

export default ProgressTracker;
