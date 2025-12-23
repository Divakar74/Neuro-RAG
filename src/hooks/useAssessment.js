import { useState, useEffect, useCallback } from 'react';
import * as assessmentService from '../services/assessmentService';

export function useAssessment(sessionToken) {
  const [currentQuestion, setCurrentQuestion] = useState(null);
  const [progress, setProgress] = useState(null);
  const [shouldStop, setShouldStop] = useState(false);
  const [stopReason, setStopReason] = useState(null);
  const [loading, setLoading] = useState(false);

  const loadNextQuestion = useCallback(async () => {
    setLoading(true);
    try {
      const response = await assessmentService.fetchNextQuestion(sessionToken);
      if (response.data.shouldStop) {
        setShouldStop(true);
        setStopReason(response.data.reason);
        setCurrentQuestion(null);
      } else {
        setCurrentQuestion(response.data);
        setShouldStop(false);
        setStopReason(null);
      }
    } catch (error) {
      console.error('Error loading next question:', error);
    } finally {
      setLoading(false);
    }
  }, [sessionToken]);

  const loadProgress = useCallback(async () => {
    try {
      const response = await assessmentService.fetchAssessmentProgress(sessionToken);
      setProgress(response.data);
    } catch (error) {
      console.error('Error loading progress:', error);
    }
  }, [sessionToken]);

  const submitResponse = useCallback(async (responseData) => {
    setLoading(true);
    try {
      // Submit response first, then load next question and progress concurrently
      await assessmentService.submitResponse(responseData, sessionToken);
      // Load next question and progress concurrently without delay
      await Promise.all([loadNextQuestion(), loadProgress()]);
      setLoading(false); // Reset loading after successful submission and loading
    } catch (error) {
      console.error('Error submitting response:', error);
      setLoading(false); // Ensure loading is false on error
    }
  }, [sessionToken, loadProgress, loadNextQuestion]);

  useEffect(() => {
    if (sessionToken) {
      loadNextQuestion();
      loadProgress();
    }
  }, [sessionToken, loadNextQuestion, loadProgress]);

  return {
    currentQuestion,
    progress,
    shouldStop,
    stopReason,
    loading,
    loadNextQuestion,
    loadProgress,
    submitResponse
  };
}
