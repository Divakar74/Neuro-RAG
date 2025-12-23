import { useEffect, useRef, useCallback } from 'react';

export function useAutoSave(data, saveFunction, delay = 2000) {
  const timeoutRef = useRef(null);
  const lastSavedDataRef = useRef(null);

  const autoSave = useCallback(async () => {
    if (!data || JSON.stringify(data) === JSON.stringify(lastSavedDataRef.current)) {
      return; // No changes to save
    }

    try {
      await saveFunction(data);
      lastSavedDataRef.current = JSON.parse(JSON.stringify(data)); // Deep copy
      console.log('Auto-saved successfully');
    } catch (error) {
      console.error('Auto-save failed:', error);
    }
  }, [data, saveFunction]);

  useEffect(() => {
    // Clear existing timeout
    if (timeoutRef.current) {
      clearTimeout(timeoutRef.current);
    }

    // Set new timeout for auto-save
    timeoutRef.current = setTimeout(autoSave, delay);

    // Cleanup function
    return () => {
      if (timeoutRef.current) {
        clearTimeout(timeoutRef.current);
      }
    };
  }, [data, autoSave, delay]);

  // Manual save function
  const manualSave = useCallback(async () => {
    if (timeoutRef.current) {
      clearTimeout(timeoutRef.current);
    }
    await autoSave();
  }, [autoSave]);

  // Save on page unload
  useEffect(() => {
    const handleBeforeUnload = () => {
      if (timeoutRef.current) {
        clearTimeout(timeoutRef.current);
      }
      // Note: This is a best-effort save, may not always work
      autoSave();
    };

    window.addEventListener('beforeunload', handleBeforeUnload);
    return () => {
      window.removeEventListener('beforeunload', handleBeforeUnload);
    };
  }, [autoSave]);

  return { manualSave };
}
