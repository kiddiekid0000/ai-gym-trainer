// src/features/workouts/services/workoutService.ts
import axios from 'axios';
import { API_BASE_URL, WORKOUT_ENDPOINTS } from '../../../config/urlConfig';

export interface WorkoutSession {
  id: number;
  startTime: string;
  endTime?: string;
  status: 'ACTIVE' | 'COMPLETED';
  totalReps?: number;
  duration?: number;
  averageAccuracy?: number;
  sets: WorkoutSet[];
}

export interface WorkoutSet {
  id: number;
  sessionId: number;
  setNumber: number;
  repCount: number;
  accuracy: number;
  restDuration: number;
  completedAt: string;
}

export interface PersonalRecord {
  exerciseId: number;
  exerciseName: string;
  maxReps: number;
  bestAccuracy: number;
  totalWorkouts: number;
}

class WorkoutService {
  async startWorkout(): Promise<WorkoutSession> {
    const response = await axios.post(
      `${API_BASE_URL}${WORKOUT_ENDPOINTS.START}`,
      {},
      { withCredentials: true }
    );
    return response.data;
  }

  async addSet(sessionId: number, setData: {
    repCount: number;
    accuracy: number;
    restDuration: number;
  }): Promise<WorkoutSet> {
    const response = await axios.post(
      `${API_BASE_URL}${WORKOUT_ENDPOINTS.ADD_SET}/${sessionId}/sets`,
      setData,
      { withCredentials: true }
    );
    return response.data;
  }

  async endWorkout(sessionId: number): Promise<WorkoutSession> {
    const response = await axios.put(
      `${API_BASE_URL}${WORKOUT_ENDPOINTS.END}/${sessionId}/end`,
      {},
      { withCredentials: true }
    );
    return response.data;
  }

  async getWorkoutHistory(startDate?: string, endDate?: string): Promise<WorkoutSession[]> {
    const params = new URLSearchParams();
    if (startDate) params.append('startDate', startDate);
    if (endDate) params.append('endDate', endDate);
    const response = await axios.get(
      `${API_BASE_URL}${WORKOUT_ENDPOINTS.HISTORY}?${params.toString()}`,
      { withCredentials: true }
    );
    return response.data;
  }

  async getPersonalRecords(period?: 'week' | 'month'): Promise<PersonalRecord[]> {
    const params = period ? `?period=${period}` : '';
    const response = await axios.get(
      `${API_BASE_URL}${WORKOUT_ENDPOINTS.RECORDS}${params}`,
      { withCredentials: true }
    );
    return response.data;
  }
}

const workoutService = new WorkoutService();
export default workoutService;