import React, { useState, useEffect, useRef } from 'react';
import { Clock, HelpCircle, Lightbulb } from 'lucide-react';

const TextAnswerModule = ({ question, onSubmit, loading }) => {
  const [response, setResponse] = useState('');
  const [startTime, setStartTime] = useState(null);
  const [charCount, setCharCount] = useState(0);
  const [wordCount, setWordCount] = useState(0);
  const [editCount, setEditCount] = useState(0);
  const [pasteDetected, setPasteDetected] = useState(false);
  const textareaRef = useRef(null);

  // Reset state when question changes
  useEffect(() => {
    if (question) {
      setStartTime(Date.now());
      setResponse('');
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

  const handleSubmit = async () => {
    if (!response.trim()) return;

    const endTime = Date.now();
    const timeSpent = Math.floor((endTime - startTime) / 1000);

    const responseData = {
      questionId: question.id,
      responseText: response,
      totalTimeSeconds: timeSpent,
      thinkTimeSeconds: timeSpent,
      charCount,
      wordCount,
      editCount,
      pasteDetected,
    };

    try {
      await onSubmit(responseData);
      // Reset state after successful submission
      setResponse('');
      setCharCount(0);
      setWordCount(0);
      setEditCount(0);
      setPasteDetected(false);
      setStartTime(Date.now());
    } catch (error) {
      console.error('Error submitting response:', error);
      // Don't reset state on error so user can retry
    }
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
            <span className="text-sm font-medium text-gray-600">Text Question</span>
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
          style={{ display: 'block' }}
        />
        <div className="flex justify-between items-center mt-2 text-sm text-gray-500">
          <span>{charCount} characters</span>
          <span>{wordCount} words</span>
        </div>
      </div>

      {/* Submit */}
      <div className="flex justify-end">
        <button
          onClick={handleSubmit}
          disabled={loading || !response.trim()}
          className="px-6 py-3 bg-gradient-to-r from-indigo-600 to-purple-600 text-white font-semibold rounded-xl shadow-lg hover:from-indigo-700 hover:to-purple-700 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed transition-all transform hover:scale-[1.01]"
        >
          {loading ? 'Submitting...' : 'Submit Answer'}
        </button>
      </div>

      {/* <div className="mt-4 text-xs text-gray-400 text-center">
        Press Ctrl+Enter to submit quickly
      </div> */}
    </div>
  );
};

export default TextAnswerModule;
