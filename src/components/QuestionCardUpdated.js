import React, { useState, useEffect, useRef } from 'react';
import { Clock, HelpCircle, Lightbulb } from 'lucide-react';

const QuestionCard = ({ question, onSubmit, loading }) => {
  const [response, setResponse] = useState('');
  const [selectedChoice, setSelectedChoice] = useState(null);
  const [sliderValue, setSliderValue] = useState(3);
  const [startTime, setStartTime] = useState(null);
  const [charCount, setCharCount] = useState(0);
  const [wordCount, setWordCount] = useState(0);
  const [confidenceLevel, setConfidenceLevel] = useState(0.5);
  const [editCount, setEditCount] = useState(0);
  const [pasteDetected, setPasteDetected] = useState(false);
  const textareaRef = useRef(null);

  // Reset state when question changes
  useEffect(() => {
    if (question) {
      setStartTime(Date.now());
      setResponse('');
      setSelectedChoice(null);
      setSliderValue(3);
      setCharCount(0);
      setWordCount(0);
      setEditCount(0);
      setPasteDetected(false);
    }
  }, [question?.id]);

  // Track character + word count
  useEffect(() => {
    const chars = response.length;
    const words =
      response.trim() === '' ? 0 : response.trim().split(/\s+/).length;

    setCharCount(chars);
    setWordCount(words);
  }, [response]);

  // Track paste + edits
  useEffect(() => {
    const handlePaste = () => setPasteDetected(true);
    const handleInput = () => setEditCount((prev) => prev + 1);

    if (textareaRef.current) {
      textareaRef.current.addEventListener('paste', handlePaste);
      textareaRef.current.addEventListener('input', handleInput);
    }
    return () => {
      if (textareaRef.current) {
        textareaRef.current.removeEventListener('paste', handlePaste);
        textareaRef.current.removeEventListener('input', handleInput);
      }
    };
  }, [question?.id]);

  if (!question) return null;

  // Normalize type
  let type =
    question?.questionType?.trim().toLowerCase() ||
    question?.type?.trim().toLowerCase() ||
    'text';
  console.log('QuestionCard: derived type:', type);

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

  const handleSubmit = () => {
    if (type === 'text' && !response.trim()) return;
    if ((type === 'mcq' || type === 'choice') && selectedChoice == null) return;

    const endTime = Date.now();
    const timeSpent = Math.floor((endTime - startTime) / 1000);

    const responseData = {
      questionId: question.id,
      responseText: type === 'text' ? response : undefined,
      responseChoice:
        type === 'mcq' || type === 'choice' ? selectedChoice : undefined,
      responseScale: type === 'scale' ? sliderValue : undefined,
      totalTimeSeconds: timeSpent,
      charCount,
      wordCount,
      thinkTime: timeSpent,
      editCount,
      pasteDetected,
      confidenceLevel,
    };

    onSubmit(responseData);
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && e.ctrlKey) {
      e.preventDefault();
      handleSubmit();
    }
  };

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

        <h2 className="text-xl font-semibold text-gray-900 mb-4 leading-relaxed">
          {question.questionText}
        </h2>

        {question.contextHint && (
          <div className="bg-blue-50 border-l-4 border-blue-400 p-4 mb-4">
            <div className="flex items-start">
              <Lightbulb className="w-5 h-5 text-blue-600 mt-0.5 mr-2 flex-shrink-0" />
              <div>
                <p className="text-sm font-medium text-blue-800">Context Hint</p>
                <p className="text-sm text-blue-700 mt-1">
                  {question.contextHint}
                </p>
              </div>
            </div>
          </div>
        )}

        {question.suggestedAnswerLength && (
          <div className="text-sm text-gray-600 mb-4">
            Suggested answer length: {question.suggestedAnswerLength} words
          </div>
        )}
      </div>

      {/* Response Input */}
      <div className="mb-6">
        {type === 'text' && (
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Your Answer
            </label>
            <textarea
              ref={textareaRef}
              value={response}
              onChange={(e) => setResponse(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="Type your response here... (Ctrl+Enter to submit)"
              className="w-full h-48 p-4 border border-gray-300 rounded-xl focus:ring-2 focus:ring-indigo-500 focus:border-transparent resize-none"
            />
            <div className="flex justify-between items-center mt-2 text-sm text-gray-500">
              <span>{charCount} characters</span>
              <span>{wordCount} words</span>
            </div>
          </div>
        )}

        {type === 'mcq' && (
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
                    value={option}
                    checked={selectedChoice === option}
                    onChange={(e) => setSelectedChoice(e.target.value)}
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
        )}

        {type === 'choice' && options.length > 0 && (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            {options.map((choice, idx) => (
              <button
                key={idx}
                type="button"
                onClick={() => setSelectedChoice(choice)}
                className={`text-left px-4 py-3 rounded-xl border transition-all duration-200 shadow-sm hover:shadow-md ${
                  selectedChoice === choice
                    ? 'bg-indigo-50 border-indigo-300 ring-2 ring-indigo-200'
                    : 'bg-white border-gray-200 hover:border-indigo-200'
                }`}
                disabled={loading}
              >
                {choice}
              </button>
            ))}
          </div>
        )}

        {type === 'scale' && (
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Select a value
            </label>
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
        )}
      </div>

      {/* Submit */}
      <div className="flex justify-end">
        <button
          onClick={handleSubmit}
          disabled={
            loading ||
            (type === 'text' && !response.trim()) ||
            (type === 'mcq' && selectedChoice == null) ||
            (type === 'choice' && selectedChoice == null)
          }
          className="px-6 py-3 bg-gradient-to-r from-indigo-600 to-purple-600 text-white font-semibold rounded-xl shadow-lg hover:from-indigo-700 hover:to-purple-700 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed transition-all transform hover:scale-[1.01]"
        >
          {loading ? 'Submitting...' : 'Submit Answer'}
        </button>
      </div>

      <div className="mt-4 text-xs text-gray-400 text-center">
        Press Ctrl+Enter to submit quickly
      </div>
    </div>
  );
};

export default QuestionCard;
