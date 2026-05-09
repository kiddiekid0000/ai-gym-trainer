// src/pages/WorkoutsPage.tsx
import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useWorkouts } from '../features/workouts/hooks/useWorkouts';
import './WorkoutsPage.css';

const WorkoutsPage = () => {
  const navigate = useNavigate();
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
  const [viewMode, setViewMode] = useState<'history' | 'records'>('history');

  useEffect(() => {
    fetchWorkoutHistory();
    fetchPersonalRecords();
  }, []);

  const handleStartWorkout = async () => {
    await startWorkout();
  };

  const handleAddSet = async () => {
    if (currentWorkout && (repCount > 0 || accuracy > 0)) {
      await addSet(currentWorkout.id, { repCount, accuracy, restDuration });
      setRepCount(0);
      setAccuracy(0);
      setRestDuration(0);
    }
  };

  const handleEndWorkout = async () => {
    if (currentWorkout) {
      await endWorkout(currentWorkout.id);
      fetchWorkoutHistory();
      fetchPersonalRecords();
    }
  };

  if (loading && !workoutHistory.length && !personalRecords.length) {
    return <div className="loading">Loading...</div>;
  }

  return (
    <div className="workouts-page">
      <header className="page-header">
        <h1>📊 Your Records</h1>
        <button className="back-btn" onClick={() => navigate('/dashboard')}>
          ← Back to Dashboard
        </button>
      </header>

      {error && <div className="error-banner">Error: {error}</div>}

      <div className="content">
        {/* Current Workout Section */}
        {currentWorkout && (
          <div className="current-workout-section">
            <h2>⏱️ Current Workout Session</h2>
            <div className="workout-info">
              <p><strong>Started:</strong> {new Date(currentWorkout.startTime).toLocaleString()}</p>
              <p><strong>Sets Completed:</strong> {currentWorkout.sets.length}</p>
              <p><strong>Total Reps:</strong> {currentWorkout.sets.reduce((sum, set) => sum + set.repCount, 0)}</p>
            </div>

            <div className="add-set-form">
              <h3>Add a Set</h3>
              <div className="form-group">
                <label>Rep Count</label>
                <input
                  type="number"
                  min="0"
                  placeholder="Number of reps completed"
                  value={repCount}
                  onChange={(e) => setRepCount(Number(e.target.value))}
                />
              </div>
              <div className="form-group">
                <label>Accuracy (%)</label>
                <input
                  type="number"
                  min="0"
                  max="100"
                  step="0.1"
                  placeholder="0-100%"
                  value={accuracy}
                  onChange={(e) => setAccuracy(Number(e.target.value))}
                />
              </div>
              <div className="form-group">
                <label>Rest Duration (seconds)</label>
                <input
                  type="number"
                  min="0"
                  placeholder="Rest time in seconds"
                  value={restDuration}
                  onChange={(e) => setRestDuration(Number(e.target.value))}
                />
              </div>
              <div className="button-group">
                <button className="btn-secondary" onClick={handleAddSet}>
                  ➕ Add Set
                </button>
                <button className="btn-danger" onClick={handleEndWorkout}>
                  🏁 End Workout
                </button>
              </div>
            </div>

            {currentWorkout.sets.length > 0 && (
              <div className="sets-list">
                <h3>Sets in This Session</h3>
                <div className="sets-grid">
                  {currentWorkout.sets.map((set, index) => (
                    <div key={index} className="set-card">
                      <p><strong>Set {set.setNumber}</strong></p>
                      <p>Reps: {set.repCount}</p>
                      <p>Accuracy: {set.accuracy}%</p>
                      <p>Rest: {set.restDuration}s</p>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        )}

        {!currentWorkout && (
          <div className="start-workout-section">
            <button className="btn-primary btn-large" onClick={handleStartWorkout}>
              ▶️ Start New Workout Session
            </button>
          </div>
        )}

        {/* View Toggle */}
        <div className="view-toggle">
          <button
            className={`toggle-btn ${viewMode === 'history' ? 'active' : ''}`}
            onClick={() => setViewMode('history')}
          >
            📅 Workout History
          </button>
          <button
            className={`toggle-btn ${viewMode === 'records' ? 'active' : ''}`}
            onClick={() => setViewMode('records')}
          >
            🏅 Personal Records
          </button>
        </div>

        {/* Workout History */}
        {viewMode === 'history' && (
          <div className="history-section">
            <h2>Workout History (Last 30 Days)</h2>
            {workoutHistory && workoutHistory.length > 0 ? (
              <div className="workouts-list">
                {workoutHistory.map((workout) => (
                  <div key={workout.id} className="workout-card">
                    <div className="workout-header">
                      <h3>Session #{workout.id}</h3>
                      <span className={`status-badge ${workout.status.toLowerCase()}`}>
                        {workout.status}
                      </span>
                    </div>
                    <div className="workout-details">
                      <p>
                        <strong>Date:</strong>{' '}
                        {new Date(workout.startTime).toLocaleString()}
                      </p>
                      {workout.endTime && (
                        <p>
                          <strong>Ended:</strong>{' '}
                          {new Date(workout.endTime).toLocaleString()}
                        </p>
                      )}
                      <p><strong>Total Reps:</strong> {workout.totalReps || 0}</p>
                      <p>
                        <strong>Duration:</strong>{' '}
                        {workout.duration ? `${workout.duration}s` : 'N/A'}
                      </p>
                      <p>
                        <strong>Average Accuracy:</strong>{' '}
                        {workout.averageAccuracy
                          ? `${(workout.averageAccuracy * 100).toFixed(1)}%`
                          : 'N/A'}
                      </p>
                      <p><strong>Sets:</strong> {workout.sets?.length || 0}</p>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <p className="no-data">No workout history yet. Start your first session!</p>
            )}
          </div>
        )}

        {/* Personal Records */}
        {viewMode === 'records' && (
          <div className="records-section">
            <h2>Your Personal Records</h2>
            {personalRecords && personalRecords.length > 0 ? (
              <div className="records-grid">
                {personalRecords.map((record) => (
                  <div key={record.exerciseId} className="record-card">
                    <h3>{record.exerciseName}</h3>
                    <div className="record-stat">
                      <span className="label">Max Reps:</span>
                      <span className="value">{record.maxReps}</span>
                    </div>
                    <div className="record-stat">
                      <span className="label">Best Accuracy:</span>
                      <span className="value">
                        {(record.bestAccuracy * 100).toFixed(1)}%
                      </span>
                    </div>
                    <div className="record-stat">
                      <span className="label">Total Workouts:</span>
                      <span className="value">{record.totalWorkouts}</span>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <p className="no-data">
                No personal records yet. Complete a workout to create records!
              </p>
            )}
          </div>
        )}
      </div>
    </div>
  );
};

export default WorkoutsPage;