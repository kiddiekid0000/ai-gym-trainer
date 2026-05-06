// src/features/workouts/hooks/useWorkouts.ts
import { useState } from 'react';
import workoutService, { type WorkoutSession, type PersonalRecord } from '../services/workoutService';

export const useWorkouts = () => {
  const [currentWorkout, setCurrentWorkout] = useState<WorkoutSession | null>(null);
  const [workoutHistory, setWorkoutHistory] = useState<WorkoutSession[]>([]);
  const [personalRecords, setPersonalRecords] = useState<PersonalRecord[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const startWorkout = async () => {
    setLoading(true);
    setError('');
    try {
      const session = await workoutService.startWorkout();
      setCurrentWorkout(session);
      return session;
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to start workout');
    } finally {
      setLoading(false);
    }
  };

  const addSet = async (sessionId: number, setData: {
    repCount: number;
    accuracy: number;
    restDuration: number;
  }) => {
    setLoading(true);
    setError('');
    try {
      const newSet = await workoutService.addSet(sessionId, setData);
      if (currentWorkout) {
        setCurrentWorkout({
          ...currentWorkout,
          sets: [...currentWorkout.sets, newSet],
        });
      }
      return newSet;
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to add set');
    } finally {
      setLoading(false);
    }
  };

  const endWorkout = async (sessionId: number) => {
    setLoading(true);
    setError('');
    try {
      const completedSession = await workoutService.endWorkout(sessionId);
      setCurrentWorkout(null);
      return completedSession;
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to end workout');
    } finally {
      setLoading(false);
    }
  };

  const fetchWorkoutHistory = async (startDate?: string, endDate?: string) => {
    setLoading(true);
    setError('');
    try {
      const history = await workoutService.getWorkoutHistory(startDate, endDate);
      setWorkoutHistory(history);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to fetch workout history');
    } finally {
      setLoading(false);
    }
  };

  const fetchPersonalRecords = async (period?: 'week' | 'month') => {
    setLoading(true);
    setError('');
    try {
      const records = await workoutService.getPersonalRecords(period);
      setPersonalRecords(records);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to fetch personal records');
    } finally {
      setLoading(false);
    }
  };

  return {
    currentWorkout,
    workoutHistory,
    personalRecords,
    loading,
    error,
    startWorkout,
    addSet,
    endWorkout,
    fetchWorkoutHistory,
    fetchPersonalRecords,
  };
};