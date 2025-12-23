import { useState, useEffect, useRef } from 'react';

export function useTypingDetection() {
  const [typingStats, setTypingStats] = useState({
    wordsPerMinute: 0,
    totalWords: 0,
    totalCharacters: 0,
    typingStartTime: null,
    isTyping: false,
    edits: 0,
    lastEditTime: null,
  });

  const textareaRef = useRef(null);
  const lastValueRef = useRef('');

  const calculateWPM = (characters, startTime) => {
    if (!startTime) return 0;
    const timeElapsed = (Date.now() - startTime) / 1000 / 60; // minutes
    const words = characters / 5; // standard: 5 characters = 1 word
    return Math.round(words / timeElapsed);
  };

  const handleTextChange = (value) => {
    const now = Date.now();
    const previousValue = lastValueRef.current;

    // Detect if user is actively typing
    const isTyping = value.length > previousValue.length;

    setTypingStats(prev => {
      const newStats = {
        ...prev,
        totalCharacters: value.length,
        totalWords: value.trim().split(/\s+/).filter(word => word.length > 0).length,
        isTyping,
      };

      // Start timing when user begins typing
      if (isTyping && !prev.typingStartTime) {
        newStats.typingStartTime = now;
      }

      // Calculate WPM if actively typing
      if (prev.typingStartTime) {
        newStats.wordsPerMinute = calculateWPM(newStats.totalCharacters, prev.typingStartTime);
      }

      // Count edits (backspaces/deletions)
      if (value.length < previousValue.length) {
        newStats.edits = prev.edits + 1;
        newStats.lastEditTime = now;
      }

      return newStats;
    });

    lastValueRef.current = value;
  };

  const resetStats = () => {
    setTypingStats({
      wordsPerMinute: 0,
      totalWords: 0,
      totalCharacters: 0,
      typingStartTime: null,
      isTyping: false,
      edits: 0,
      lastEditTime: null,
    });
    lastValueRef.current = '';
  };

  return {
    typingStats,
    handleTextChange,
    resetStats,
    textareaRef,
  };
}
