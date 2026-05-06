// src/features/leaderboards/hooks/useLeaderboards.ts
import { useState } from 'react';
import leaderboardService, { type LeaderboardEntry } from '../services/leaderboardService';

export const useLeaderboards = () => {
  const [leaderboard, setLeaderboard] = useState<LeaderboardEntry[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const fetchLeaderboard = async (exerciseId: number) => {
    setLoading(true);
    setError('');
    try {
      const entries = await leaderboardService.getLeaderboard(exerciseId);
      setLeaderboard(entries);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to fetch leaderboard');
    } finally {
      setLoading(false);
    }
  };

  return {
    leaderboard,
    loading,
    error,
    fetchLeaderboard,
  };
};