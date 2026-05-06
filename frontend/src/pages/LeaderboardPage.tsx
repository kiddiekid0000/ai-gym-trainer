// src/pages/LeaderboardPage.tsx
import { useState } from 'react';
import { useExercises } from '../features/exercises/hooks/useExercises';
import { useLeaderboards } from '../features/leaderboards/hooks/useLeaderboards';
import './LeaderboardPage.css';

const LeaderboardPage = () => {
  const { exercises, loading: exercisesLoading, error: exercisesError } = useExercises();
  const { leaderboard, loading: leaderboardLoading, error: leaderboardError, fetchLeaderboard } = useLeaderboards();
  const [selectedExerciseId, setSelectedExerciseId] = useState<number | null>(null);

  const handleExerciseSelect = (exerciseId: number) => {
    setSelectedExerciseId(exerciseId);
    fetchLeaderboard(exerciseId);
  };

  if (exercisesLoading) return <div>Loading exercises...</div>;
  if (exercisesError) return <div>Error: {exercisesError}</div>;

  return (
    <div className="leaderboard-page">
      <h1>Leaderboards</h1>

      <div className="exercise-selector">
        <h2>Select Exercise</h2>
        <div className="exercises-grid">
          {exercises.map((exercise) => (
            <div
              key={exercise.id}
              className={`exercise-item ${selectedExerciseId === exercise.id ? 'selected' : ''}`}
              onClick={() => handleExerciseSelect(exercise.id)}
            >
              <h3>{exercise.name}</h3>
              <p>{exercise.type}</p>
            </div>
          ))}
        </div>
      </div>

      {selectedExerciseId && (
        <div className="leaderboard">
          <h2>Leaderboard</h2>
          {leaderboardLoading && <div>Loading leaderboard...</div>}
          {leaderboardError && <div>Error: {leaderboardError}</div>}
          {!leaderboardLoading && !leaderboardError && (
            <table className="leaderboard-table">
              <thead>
                <tr>
                  <th>Rank</th>
                  <th>Username</th>
                  <th>Total Reps</th>
                  <th>Accuracy</th>
                  <th>Streak Days</th>
                </tr>
              </thead>
              <tbody>
                {leaderboard.map((entry) => (
                  <tr key={entry.rank}>
                    <td>{entry.rank}</td>
                    <td>{entry.username}</td>
                    <td>{entry.totalReps}</td>
                    <td>{(entry.accuracy * 100).toFixed(1)}%</td>
                    <td>{entry.streakDays}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}
    </div>
  );
};

export default LeaderboardPage;