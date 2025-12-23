import { useState, useEffect, useCallback } from 'react';
import * as assessmentService from '../services/assessmentService';

export function useBatchAssessment(sessionId) {
  const [questions, setQuestions] = useState([]);
  const [progress, setProgress] = useState(null);
  const [shouldStop, setShouldStop] = useState(false);
  const [stopReason, setStopReason] = useState(null);
  const [loading, setLoading] = useState(false);

  const loadBatchQuestions = useCallback(async () => {
    setLoading(true);
    try {
      const response = await assessmentService.fetchRecommendedQuestions(sessionId, 5);
      if (response.data.length === 0) {
        setShouldStop(true);
        setStopReason('NO_MORE_QUESTIONS');
        setQuestions([]);
      } else {
        setQuestions(response.data);
        setShouldStop(false);
        setStopReason(null);
      }
    } catch (error) {
      console.error('Error loading batch questions:', error);
    } finally {
      setLoading(false);
    }
  }, [sessionId]);

  const loadProgress = useCallback(async () => {
    try {
      const response = await assessmentService.fetchAssessmentProgress(sessionId);
      setProgress(response.data);
    } catch (error) {
      console.error('Error loading progress:', error);
    }
  }, [sessionId]);

  const submitBatchResponses = useCallback(async (batchResponses) => {
    setLoading(true);
    try {
      // Submit each response individually or implement batch submission if backend supports
      for (const responseData of batchResponses) {
        await assessmentService.submitResponse({
          ...responseData,
          sessionId,
        });
      }
      await loadProgress();
      await loadBatchQuestions();
    } catch (error) {
      console.error('Error submitting batch responses:', error);
      setLoading(false);
    } finally {
      setLoading(false);
    }
  }, [sessionId, loadProgress, loadBatchQuestions]);

  useEffect(() => {
    if (sessionId) {
      loadBatchQuestions();
      loadProgress();
    }
  }, [sessionId, loadBatchQuestions, loadProgress]);

  return {
    questions,
    progress,
    shouldStop,
    stopReason,
    loading,
    loadBatchQuestions,
    loadProgress,
    submitBatchResponses,
  };
}
