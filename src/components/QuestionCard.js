import React, { useState, useEffect, useRef } from 'react';
import { Clock, HelpCircle, Lightbulb } from 'lucide-react';

const QuestionCard = ({ question, onSubmit, loading }) => {
  const [response, setResponse] = useState('');
  const [selectedChoice, setSelectedChoice] = useState(null);
  const [sliderValue, setSliderValue] = useState(3);
  const [startTime, setStartTime] = useState(null);
  const [charCount, setCharCount] = useState(0);
  const [wordCount, setWordCount] = useState(0);
  const [keyStrokes, setKeyStrokes] = useState(0);
  const textareaRef = useRef(null);
  const autoSubmitTimeoutRef = useRef(null);

  useEffect(() => {
    setStartTime(Date.now());
    // Reset form state when question changes
    setResponse('');
    setSelectedChoice(null);
    setSliderValue(3);
    setCharCount(0);
    setWordCount(0);
    setKeyStrokes(0);

    // Clear any existing auto-submit timeout
    if (autoSubmitTimeoutRef.current) {
      clearTimeout(autoSubmitTimeoutRef.current);
      autoSubmitTimeoutRef.current = null;
    }

    // Auto-focus textarea for behavioral questions
    const tRaw = question?.questionType || question?.type || 'text';
    const t = typeof tRaw === 'string' ? tRaw.toLowerCase() : 'text';
    if ((t === 'text' || t === 'typing') && textareaRef.current) {
      setTimeout(() => {
        textareaRef.current.focus();
      }, 100);
    }
  }, [question]);

  useEffect(() => {
    const chars = response?.length || 0;
    const words = response?.trim() === '' ? 0 : response?.trim().split(/\s+/).length || 0;

    setCharCount(chars);
    setWordCount(words);
  }, [response]);

  // Removed auto-submit logic to ensure submit button is always enabled

  const handleSubmit = (autoSubmit = false) => {
    const normalizedTypeRaw = question?.questionType || question?.type || 'text';
    const type = typeof normalizedTypeRaw === 'string' ? normalizedTypeRaw.trim().toLowerCase() : 'text';
    if ((type === 'text' || type === 'typing') && !response.trim()) return;
    if ((type === 'choice' || type === 'mcq') && selectedChoice == null) return;

    // Clear auto-submit timeout if it exists
    if (autoSubmitTimeoutRef.current) {
      clearTimeout(autoSubmitTimeoutRef.current);
      autoSubmitTimeoutRef.current = null;
    }

    const endTime = Date.now();
    const timeSpent = Math.floor((endTime - startTime) / 1000); // seconds
    const minutes = Math.max(0.001, timeSpent / 60);
    const typingSpeedWpm = (type === 'text' || type === 'typing') ? Math.round((wordCount / minutes) * 100) / 100 : undefined;

    const responseData = {
      questionId: question.id,
      responseText: (type === 'text' || type === 'typing') ? response : undefined,
      responseChoice: (type === 'choice' || type === 'mcq') ? selectedChoice : undefined,
      responseScale: type === 'scale' ? sliderValue : undefined,
      totalTimeSeconds: timeSpent,
      charCount,
      wordCount,
      typingSpeedWpm
    };

    onSubmit(responseData);
  };

  const handleKeyPress = (e) => {
    if (e.key === 'Enter' && e.ctrlKey) {
      handleSubmit();
    }
  };

  if (!question) return null;

  return (
    <div className="max-w-4xl mx-auto bg-white rounded-2xl shadow-xl p-8 transition-transform duration-300 will-change-transform">
      {/* Question Header */}
      <div className="mb-6">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center space-x-2">
            <HelpCircle className="w-6 h-6 text-blue-600" />
            <span className="text-sm font-medium text-gray-600">Question</span>
          </div>
          <div className="flex items-center space-x-2 text-sm text-gray-500">
            <Clock className="w-4 h-4" />
            <span>Take your time to think deeply</span>
          </div>
        </div>

        {/* Question Text */}
        <h2 className="text-xl font-semibold text-gray-900 mb-4 leading-relaxed">
          {question.questionText}
        </h2>

        {/* Context Hint */}
        {question.contextHint && (
          <div className="bg-blue-50 border-l-4 border-blue-400 p-4 mb-4">
            <div className="flex items-start">
              <Lightbulb className="w-5 h-5 text-blue-600 mt-0.5 mr-2 flex-shrink-0" />
              <div>
                <p className="text-sm font-medium text-blue-800">Context Hint</p>
                <p className="text-sm text-blue-700 mt-1">{question.contextHint}</p>
              </div>
            </div>
          </div>
        )}

        {/* Expected Answer Length */}
        {question.suggestedAnswerLength && (
          <div className="text-sm text-gray-600 mb-4">
            Suggested answer length: {question.suggestedAnswerLength} words
          </div>
        )}
      </div>

      {/* Response Input */}
      <div className="mb-6">
        {(() => {
          const tRaw = question?.questionType || question?.type || 'text';
          const t = typeof tRaw === 'string' ? tRaw.toLowerCase() : 'text';

          // --- normalize options safely ---
          let options = [];
          try {
            if (Array.isArray(question.options) && question.options.length > 0) {
              options = question.options;
            } else if (Array.isArray(question.choices) && question.choices.length > 0) {
              options = question.choices;
            } else if (typeof question.options === 'string' && question.options.trim() !== '') {
              // Try JSON first
              try {
                options = JSON.parse(question.options || "[]");
              } catch {
                // Fallback: split by commas
                options = question.options.split(',').map((o) => o.trim());
              }
            } else if (typeof question.choices === 'string' && question.choices.trim() !== '') {
              // Try JSON first
              try {
                options = JSON.parse(question.choices || "[]");
              } catch {
                // Fallback: split by commas
                options = question.choices.split(',').map((o) => o.trim());
              }
            }
          } catch (e) {
            console.error('Error parsing options:', e);
          }
          console.log('QuestionCard: parsed options:', JSON.stringify(options));

          const getChoiceValue = (choice) => typeof choice === 'object' ? (choice?.value || choice?.text || choice?.label || choice?.option) : choice;

          if (t === 'text' || t === 'typing') {
            return (
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Your Answer
                </label>
                <textarea
                  ref={textareaRef}
                  value={response}
                  onChange={(e) => setResponse(e.target.value)}
                  onKeyPress={handleKeyPress}
                  placeholder="Type your response here..."
                  className="w-full h-48 p-4 border border-gray-300 rounded-xl focus:ring-2 focus:ring-indigo-500 focus:border-transparent resize-none"
                  disabled={loading}
                />
                <div className="flex justify-between items-center mt-2 text-sm text-gray-500">
                  <span>{charCount} characters</span>
                  <span>{wordCount} words</span>
                </div>
              </div>
            );
          }

          if (t === 'mcq') {
            return (
              <div className="space-y-3">
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Select your answer
                </label>
                {options.length > 0 ? (
                  options.map((option, idx) => (
                    <label
                      key={idx}
                      className="flex items-center space-x-3 p-3 border rounded-lg hover:bg-gray-50 cursor-pointer"
                    >
                      <input
                        type="radio"
                        name="mcq-answer"
                        value={getChoiceValue(option)}
                        checked={selectedChoice === getChoiceValue(option)}
                        onChange={(e) => setSelectedChoice(e.target.value)}
                        className="text-blue-600 focus:ring-blue-500"
                        disabled={loading}
                      />
                      <span className="text-gray-700">{getChoiceValue(option)}</span>
                    </label>
                  ))
                ) : (
                  <p className="text-sm text-gray-500 italic">
                    No options available
                  </p>
                )}
              </div>
            );
          }

          if (t === 'choice') {
            return (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                {options.map((choice, idx) => (
                  <button
                    key={idx}
                    type="button"
                    onClick={() => setSelectedChoice(getChoiceValue(choice))}
                    className={`text-left px-4 py-3 rounded-xl border transition-all duration-200 shadow-sm hover:shadow-md ${
                      selectedChoice === getChoiceValue(choice)
                        ? 'bg-indigo-50 border-indigo-300 ring-2 ring-indigo-200'
                        : 'bg-white border-gray-200 hover:border-indigo-200'
                    }`}
                    disabled={loading}
                  >
                    {getChoiceValue(choice)}
                  </button>
                ))}
              </div>
            );
          }

          if (t === 'scale') {
            return (
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">Select a value</label>
                <input
                  type="range"
                  min="1"
                  max="5"
                  step="1"
                  value={sliderValue}
                  onChange={(e) => setSliderValue(Number(e.target.value))}
                  className="w-full accent-indigo-600"
                  disabled={loading}
                />
                <div className="flex justify-between text-sm text-gray-500 mt-1">
                  <span>1</span>
                  <span>2</span>
                  <span>3</span>
                  <span>4</span>
                  <span>5</span>
                </div>
              </div>
            );
          }

          // Default to text input (textarea) for any unrecognized type
          return (
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Your Answer
              </label>
              <textarea
                ref={textareaRef}
                value={response}
                onChange={(e) => setResponse(e.target.value)}
                onKeyPress={handleKeyPress}
                placeholder="Type your response here..."
                className="w-full h-48 p-4 border border-gray-300 rounded-xl focus:ring-2 focus:ring-indigo-500 focus:border-transparent resize-none"
                disabled={loading}
              />
              <div className="flex justify-between items-center mt-2 text-sm text-gray-500">
                <span>{charCount} characters</span>
                <span>{wordCount} words</span>
              </div>
            </div>
          );
        })()}
      </div>

      {/* Submit */}
      <div className="flex justify-end">
        <button
          onClick={() => handleSubmit(false)}
          disabled={(() => {
            const tRaw = question?.questionType || question?.type || 'text';
            const t = typeof tRaw === 'string' ? tRaw.toLowerCase() : 'text';
            if (loading) return true;
            if (t === 'text' || t === 'typing') return !response.trim();
            if (t === 'choice' || t === 'mcq') return selectedChoice == null;
            return false;
          })()}
          className="px-6 py-3 bg-gradient-to-r from-indigo-600 to-purple-600 text-white font-semibold rounded-xl shadow-lg hover:from-indigo-700 hover:to-purple-700 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed transition-all transform hover:scale-[1.01]"
        >
          {loading ? 'Submitting...' : 'Submit Answer'}
        </button>
      </div>

      {/* Keyboard Shortcut Hint */}
      {/* <div className="mt-4 text-xs text-gray-400 text-center">
        Press Ctrl+Enter to submit quickly
      </div> */}
    </div>
  );
};

export default QuestionCard;
