// src/pages/LeaderboardPage.tsx
import { useState, useEffect } from 'react';
import { useExercises } from '../features/exercises/hooks/useExercises';
import { useLeaderboards } from '../features/leaderboards/hooks/useLeaderboards';
import { useNavigate } from 'react-router-dom';
import './LeaderboardPage.css';

const LeaderboardPage = () => {
  const navigate = useNavigate();
  const { exercises, loading: exercisesLoading, error: exercisesError, fetchAllExercises } = useExercises();
  const { leaderboard, loading: leaderboardLoading, error: leaderboardError, fetchLeaderboard } = useLeaderboards();
  const [selectedExerciseId, setSelectedExerciseId] = useState<number | null>(null);
  const [selectedExerciseName, setSelectedExerciseName] = useState<string>('');

  useEffect(() => {
    fetchAllExercises();
  }, []);

  const handleExerciseSelect = (exerciseId: number, exerciseName: string) => {
    setSelectedExerciseId(exerciseId);
    setSelectedExerciseName(exerciseName);
    fetchLeaderboard(exerciseId);
  };

  return (
    <div className="leaderboard-page">
      <header className="page-header">
        <h1>🏆 Best Records (Leaderboard)</h1>
        <button className="back-btn" onClick={() => navigate('/dashboard')}>
          ← Back to Dashboard
        </button>
      </header>

      <div className="content">
        <div className="exercise-selector">
          <h2>Select Exercise to View Rankings</h2>
          {exercisesLoading && <p>Loading exercises...</p>}
          {exercisesError && <p className="error">Error: {exercisesError}</p>}
          {!exercisesLoading && (
            <div className="exercises-grid">
              {exercises && exercises.length > 0 ? (
                exercises.map((exercise) => (
                  <div
                    key={exercise.id}
                    className={`exercise-item ${selectedExerciseId === exercise.id ? 'selected' : ''}`}
                    onClick={() => handleExerciseSelect(exercise.id, exercise.name)}
                  >
                    <h3>{exercise.name}</h3>
                    <p className="exercise-type">{exercise.type}</p>
                    <p className="exercise-difficulty">Difficulty: {exercise.difficulty}</p>
                  </div>
                ))
              ) : (
                <p>No exercises available</p>
              )}
            </div>
          )}
        </div>

        {selectedExerciseId && (
          <div className="leaderboard-section">
            <h2>Top Performers - {selectedExerciseName}</h2>
            {leaderboardLoading && <p>Loading leaderboard...</p>}
            {leaderboardError && <p className="error">Error: {leaderboardError}</p>}
            {!leaderboardLoading && !leaderboardError && leaderboard && leaderboard.length > 0 ? (
              <table className="leaderboard-table">
                <thead>
                  <tr>
                    <th className="rank-col">🥇 Rank</th>
                    <th className="user-col">👤 User</th>
                    <th className="reps-col">💪 Total Reps</th>
                    <th className="accuracy-col">🎯 Accuracy</th>
                    <th className="streak-col">🔥 Streak (Days)</th>
                  </tr>
                </thead>
                <tbody>
                  {leaderboard.map((entry) => (
                    <tr key={`${entry.rank}-${entry.userId}`} className={`rank-${entry.rank}`}>
                      <td className="rank-col">
                        <strong>#{entry.rank}</strong>
                      </td>
                      <td className="user-col">{entry.username || `User ${entry.userId}`}</td>
                      <td className="reps-col"><strong>{entry.totalReps}</strong></td>
                      <td className="accuracy-col">{(entry.accuracy * 100).toFixed(1)}%</td>
                      <td className="streak-col">{entry.streakDays} days</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            ) : (
              <p className="no-data">No rankings available yet for this exercise</p>
            )}
          </div>
        )}
      </div>
    </div>
  );
};

export default LeaderboardPage;