import { useState, useEffect, useCallback } from 'react';
import * as roadmapService from '../services/roadmapService';

export function useRoadmap(sessionId) {
  const [roadmap, setRoadmap] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const fetchRoadmap = useCallback(async () => {
    if (!sessionId) return;

    setLoading(true);
    setError(null);
    try {
      const response = await roadmapService.getRoadmap(sessionId);
      const data = response.data;
      // Backend returns a list for GET /session/{id}
      const roadmapData = Array.isArray(data) ? (data[0] || null) : data;
      setRoadmap(roadmapData);
    } catch (err) {
      // If no roadmap exists yet, that's not necessarily an error
      if (err.response && err.response.status === 404) {
        setRoadmap(null);
        setError(null);
      } else {
        setError(err.message || 'Failed to fetch roadmap');
        console.error('Error fetching roadmap:', err);
      }
    } finally {
      setLoading(false);
    }
  }, [sessionId]);

  const generateRoadmap = useCallback(async (skillGaps) => {
    if (!sessionId) return;

    setLoading(true);
    setError(null);
    try {
      const response = await roadmapService.generateRoadmap(sessionId, skillGaps);
      setRoadmap(response.data);
    } catch (err) {
      setError(err.message || 'Failed to generate roadmap');
      console.error('Error generating roadmap:', err);
    } finally {
      setLoading(false);
    }
  }, [sessionId]);

  const updateMilestoneProgress = useCallback(async (milestoneId, completed) => {
    if (!sessionId || !roadmap) return;

    try {
      await roadmapService.updateMilestoneProgress(sessionId, milestoneId, completed);
      // Update local state
      setRoadmap(prev => {
        if (!prev) return prev;
        return {
          ...prev,
          phases: prev.phases.map(phase => ({
            ...phase,
            milestones: phase.milestones.map(milestone =>
              milestone.id === milestoneId
                ? { ...milestone, completed }
                : milestone
            )
          }))
        };
      });
    } catch (err) {
      setError(err.message || 'Failed to update milestone');
      console.error('Error updating milestone:', err);
    }
  }, [sessionId, roadmap]);

  useEffect(() => {
    fetchRoadmap();
  }, [fetchRoadmap]);

  return {
    roadmap,
    loading,
    error,
    fetchRoadmap,
    generateRoadmap,
    updateMilestoneProgress,
  };
}
