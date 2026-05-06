// src/pages/WorkoutsPage.tsx
import { useState, useEffect } from 'react';
import { useWorkouts } from '../features/workouts/hooks/useWorkouts';
import websocketService from '../features/workouts/services/websocketService';
import './WorkoutsPage.css';

const WorkoutsPage = () => {
  const {
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
  } = useWorkouts();

  const [repCount, setRepCount] = useState(0);
  const [accuracy, setAccuracy] = useState(0);
  const [restDuration, setRestDuration] = useState(0);

  useEffect(() => {
    fetchWorkoutHistory();
    fetchPersonalRecords();

    // Connect to WebSocket for real-time updates
    websocketService.connect(
      () => {
        console.log('WebSocket connected');
        // Subscribe to workout updates
        websocketService.subscribe('/topic/workouts', (message) => {
          console.log('Workout update:', message);
          // Handle real-time updates
        });
      },
      (error) => {
        console.error('WebSocket error:', error);
      }
    );

    return () => {
      websocketService.disconnect();
    };
  }, []);

  const handleStartWorkout = async () => {
    await startWorkout();
  };

  const handleAddSet = async () => {
    if (currentWorkout) {
      await addSet(currentWorkout.id, { repCount, accuracy, restDuration });
      setRepCount(0);
      setAccuracy(0);
      setRestDuration(0);
    }
  };

  const handleEndWorkout = async () => {
    if (currentWorkout) {
      await endWorkout(currentWorkout.id);
    }
  };

  if (loading) return <div>Loading...</div>;
  if (error) return <div>Error: {error}</div>;

  return (
    <div className="workouts-page">
      <h1>Workouts</h1>

      {!currentWorkout ? (
        <button onClick={handleStartWorkout}>Start New Workout</button>
      ) : (
        <div className="current-workout">
          <h2>Current Workout</h2>
          <p>Started: {new Date(currentWorkout.startTime).toLocaleString()}</p>
          <p>Sets: {currentWorkout.sets.length}</p>

          <div className="add-set">
            <input
              type="number"
              placeholder="Rep Count"
              value={repCount}
              onChange={(e) => setRepCount(Number(e.target.value))}
            />
            <input
              type="number"
              step="0.01"
              placeholder="Accuracy"
              value={accuracy}
              onChange={(e) => setAccuracy(Number(e.target.value))}
            />
            <input
              type="number"
              placeholder="Rest Duration (seconds)"
              value={restDuration}
              onChange={(e) => setRestDuration(Number(e.target.value))}
            />
            <button onClick={handleAddSet}>Add Set</button>
          </div>

          <button onClick={handleEndWorkout}>End Workout</button>
        </div>
      )}

      <div className="workout-history">
        <h2>Workout History</h2>
        {workoutHistory.map((workout) => (
          <div key={workout.id} className="workout-card">
            <p>Start: {new Date(workout.startTime).toLocaleString()}</p>
            {workout.endTime && <p>End: {new Date(workout.endTime).toLocaleString()}</p>}
            <p>Status: {workout.status}</p>
            <p>Total Reps: {workout.totalReps}</p>
            <p>Duration: {workout.duration} seconds</p>
            <p>Average Accuracy: {workout.averageAccuracy}</p>
          </div>
        ))}
      </div>

      <div className="personal-records">
        <h2>Personal Records</h2>
        {personalRecords.map((record) => (
          <div key={record.exerciseId} className="record-card">
            <h3>{record.exerciseName}</h3>
            <p>Max Reps: {record.maxReps}</p>
            <p>Best Accuracy: {record.bestAccuracy}</p>
            <p>Total Workouts: {record.totalWorkouts}</p>
          </div>
        ))}
      </div>
    </div>
  );
};

export default WorkoutsPage;