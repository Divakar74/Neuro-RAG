import React, { useState, useEffect } from 'react';
import { Clock, HelpCircle, Lightbulb, ChevronLeft, ChevronRight } from 'lucide-react';

const MCQBatchModule = ({ questions, onSubmitBatch, loading }) => {
  const [currentIndex, setCurrentIndex] = useState(0);
  const [selectedChoices, setSelectedChoices] = useState({});

  useEffect(() => {
    // Reset selections when questions change
    setSelectedChoices({});
    setCurrentIndex(0);
  }, [questions]);

  if (!questions || questions.length === 0) return null;

  const currentQuestion = questions[currentIndex];

  // Normalize options safely
  let options = [];
  try {
    if (Array.isArray(currentQuestion.options) && currentQuestion.options.length > 0) {
      options = currentQuestion.options;
    } else if (Array.isArray(currentQuestion.choices) && currentQuestion.choices.length > 0) {
      options = currentQuestion.choices;
    } else if (typeof currentQuestion.options === 'string' && currentQuestion.options.trim() !== '') {
      try {
        options = JSON.parse(currentQuestion.options || "[]");
      } catch {
        options = currentQuestion.options.split(',').map((o) => o.trim());
      }
    } else if (typeof currentQuestion.choices === 'string' && currentQuestion.choices.trim() !== '') {
      try {
        options = JSON.parse(currentQuestion.choices || "[]");
      } catch {
        options = currentQuestion.choices.split(',').map((o) => o.trim());
      }
    }
  } catch (e) {
    console.error('Error parsing options:', e);
  }

  const handleChoiceSelect = (choice) => {
    setSelectedChoices({
      ...selectedChoices,
      [currentQuestion.id]: choice,
    });
  };

  const handlePrev = () => {
    setCurrentIndex((prev) => Math.max(prev - 1, 0));
  };

  const handleNext = () => {
    setCurrentIndex((prev) => Math.min(prev + 1, questions.length - 1));
  };

  const handleSubmit = () => {
    // Validate all questions answered
    const unanswered = questions.find(q => !selectedChoices[q.id]);
    if (unanswered) {
      alert('Please answer all questions before submitting.');
      return;
    }

    // Prepare batch response data
    const batchResponses = questions.map(q => ({
      questionId: q.id,
      responseChoice: selectedChoices[q.id],
      // Additional metrics can be added here if needed
    }));

    onSubmitBatch(batchResponses);
  };

  return (
    <div className="max-w-4xl mx-auto bg-white rounded-2xl shadow-xl p-8 transition-transform duration-300 will-change-transform">
      {/* Question Header */}
      <div className="mb-6">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center space-x-2">
            <HelpCircle className="w-6 h-6 text-blue-600" />
            <span className="text-sm font-medium text-gray-600">MCQ Batch Question {currentIndex + 1} of {questions.length}</span>
          </div>
          <div className="flex items-center space-x-2 text-sm text-gray-500">
            <Clock className="w-4 h-4" />
            <span>Take your time to think deeply</span>
          </div>
        </div>

        <h2 className="text-xl font-semibold text-gray-900 mb-4 leading-relaxed">
          {currentQuestion.questionText}
        </h2>

        {currentQuestion.contextHint && (
          <div className="bg-blue-50 border-l-4 border-blue-400 p-4 mb-4">
            <div className="flex items-start">
              <Lightbulb className="w-5 h-5 text-blue-600 mt-0.5 mr-2 flex-shrink-0" />
              <div>
                <p className="text-sm font-medium text-blue-800">Context Hint</p>
                <p className="text-sm text-blue-700 mt-1">
                  {currentQuestion.contextHint}
                </p>
              </div>
            </div>
          </div>
        )}

        {currentQuestion.suggestedAnswerLength && (
          <div className="text-sm text-gray-600 mb-4">
            Suggested answer length: {currentQuestion.suggestedAnswerLength} words
          </div>
        )}
      </div>

      {/* Options */}
      <div className="mb-6 space-y-3">
        {options.length > 0 ? (
          options.map((option, idx) => (
            <label
              key={idx}
              className="flex items-center space-x-3 p-3 border rounded-lg hover:bg-gray-50 cursor-pointer"
            >
              <input
                type="radio"
                name={`mcq-answer-${currentQuestion.id}`}
                value={option}
                checked={selectedChoices[currentQuestion.id] === option}
                onChange={() => handleChoiceSelect(option)}
                className="text-blue-600 focus:ring-blue-500"
                disabled={loading}
              />
              <span className="text-gray-700">{option}</span>
            </label>
          ))
        ) : (
          <p className="text-sm text-gray-500 italic">
            No options available
          </p>
        )}
      </div>

      {/* Navigation and Submit */}
      <div className="flex justify-between items-center">
        <button
          onClick={handlePrev}
          disabled={currentIndex === 0 || loading}
          className="px-4 py-2 bg-gray-200 rounded-lg hover:bg-gray-300 disabled:opacity-50 disabled:cursor-not-allowed flex items-center space-x-1"
        >
          <ChevronLeft className="w-4 h-4" />
          <span>Previous</span>
        </button>

        {currentIndex < questions.length - 1 ? (
          <button
            onClick={handleNext}
            disabled={loading}
            className="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 flex items-center space-x-1"
          >
            <span>Next</span>
            <ChevronRight className="w-4 h-4" />
          </button>
        ) : (
          <button
            onClick={handleSubmit}
            disabled={loading || Object.keys(selectedChoices).length !== questions.length}
            className="px-6 py-3 bg-gradient-to-r from-indigo-600 to-purple-600 text-white font-semibold rounded-xl shadow-lg hover:from-indigo-700 hover:to-purple-700 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed transition-all transform hover:scale-[1.01]"
          >
            {loading ? 'Submitting...' : 'Submit Batch'}
          </button>
        )}
      </div>
    </div>
  );
};

export default MCQBatchModule;
