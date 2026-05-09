// src/features/leaderboards/services/leaderboardService.ts
import axios from 'axios';
import { API_BASE_URL, LEADERBOARD_ENDPOINTS } from '../../../config/urlConfig';

export interface LeaderboardEntry {
  rank: number;
  userId: number;
  username: string;
  totalReps: number;
  accuracy: number;
  streakDays: number;
}

class LeaderboardService {
  async getLeaderboard(exerciseId: number): Promise<LeaderboardEntry[]> {
    const response = await axios.get(
      `${API_BASE_URL}${LEADERBOARD_ENDPOINTS.GET}/${exerciseId}`
    );
    return response.data;
  }
}

const leaderboardService = new LeaderboardService();
export default leaderboardService;