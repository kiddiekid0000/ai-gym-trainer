// src/features/exercises/services/exerciseService.ts
import axios from 'axios';
import { API_BASE_URL, EXERCISE_ENDPOINTS } from '../../../config/urlConfig';

export interface Exercise {
  id: number;
  name: string;
  type: 'STRENGTH' | 'CARDIO' | 'CORE';
  caloriesPerRep: number;
  difficulty: 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED';
}

class ExerciseService {
  async getExerciseTypes(): Promise<string[]> {
    const response = await axios.get(
      `${API_BASE_URL}${EXERCISE_ENDPOINTS.TYPES}`,
      { withCredentials: true }
    );
    return response.data;
  }

  async getAllExercises(): Promise<Exercise[]> {
    const response = await axios.get(
      `${API_BASE_URL}${EXERCISE_ENDPOINTS.ALL}`,
      { withCredentials: true }
    );
    return response.data;
  }

  async getExercisesByType(type: string): Promise<Exercise[]> {
    const response = await axios.get(
      `${API_BASE_URL}${EXERCISE_ENDPOINTS.BY_TYPE}/${type}`,
      { withCredentials: true }
    );
    return response.data;
  }
}

const exerciseService = new ExerciseService();
export default exerciseService;