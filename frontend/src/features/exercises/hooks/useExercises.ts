// src/features/exercises/hooks/useExercises.ts
import { useState, useEffect } from 'react';
import exerciseService, { type Exercise } from '../services/exerciseService';

export const useExercises = () => {
  const [exercises, setExercises] = useState<Exercise[]>([]);
  const [exerciseTypes, setExerciseTypes] = useState<string[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const fetchExerciseTypes = async () => {
    setLoading(true);
    setError('');
    try {
      const types = await exerciseService.getExerciseTypes();
      setExerciseTypes(types);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to fetch exercise types');
    } finally {
      setLoading(false);
    }
  };

  const fetchAllExercises = async () => {
    setLoading(true);
    setError('');
    try {
      const exs = await exerciseService.getAllExercises();
      setExercises(exs);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to fetch exercises');
    } finally {
      setLoading(false);
    }
  };

  const fetchExercisesByType = async (type: string) => {
    setLoading(true);
    setError('');
    try {
      const exs = await exerciseService.getExercisesByType(type);
      setExercises(exs);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to fetch exercises by type');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchExerciseTypes();
    fetchAllExercises(); // Fetch all exercises initially
  }, []);

  return {
    exercises,
    exerciseTypes,
    loading,
    error,
    fetchAllExercises,
    fetchExercisesByType,
  };
};