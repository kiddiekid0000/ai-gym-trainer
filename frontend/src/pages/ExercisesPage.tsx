// src/pages/ExercisesPage.tsx
import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useWorkouts } from '../features/workouts/hooks/useWorkouts';
import MediaPipeCounter from '../features/exercises/components/MediaPipeCounter';
import type { ExerciseType } from '../features/exercises/services/exerciseDetectionService';
import './ExercisesPage.css';

interface Exercise {
  id: number;
  name: string;
  type: string;
  difficulty: string;
  caloriesPerRep: number;
}

// Map exercise names to MediaPipe detection types
const EXERCISE_TYPE_MAP: Record<string, ExerciseType> = {
  'Push-ups': 'PUSH_UP',
  'Push Up': 'PUSH_UP',
  'Squats': 'SQUAT',
  'Squat': 'SQUAT',
  'Jumping Jacks': 'JUMPING_JACK',
  'Jumping Jack': 'JUMPING_JACK',
  'Planks': 'PLANK',
  'Plank': 'PLANK',
  'Sit-ups': 'CRUNCH',
  'Crunches': 'CRUNCH',
  'Crunch': 'CRUNCH',
};

// Exercise type information for MediaPipe guidance
const EXERCISE_GUIDE: Record<string, {icon: string, description: string, mediaPipeGuide: string}> = {
  'STRENGTH': {
    icon: '💪',
    description: 'Build muscle and increase power with resistance exercises',
    mediaPipeGuide: 'Detects full body joints: shoulders, elbows, wrists, hips, knees, ankles. Tracks depth and form of pushups, squats, deadlifts.'
  },
  'CARDIO': {
    icon: '🏃',
    description: 'Improve endurance and heart health with dynamic movements',
    mediaPipeGuide: 'Tracks rapid body movements: detects jumping, running motion, arm swings. Counts speed and repetition of cardio movements.'
  },
  'CORE': {
    icon: '🎯',
    description: 'Strengthen abdominal and stabilizer muscles',
    mediaPipeGuide: 'Focuses on torso alignment: detects spine position, hip-shoulder alignment. Counts planks duration, crunches, leg raises.'
  }
};

const ExercisesPage = () => {
  const navigate = useNavigate();
  const { currentWorkout, startWorkout, addSet } = useWorkouts();
  
  const [step, setStep] = useState<'type-selection' | 'exercise-selection' | 'ready-to-start' | 'exercising'>('type-selection');
  const [selectedType, setSelectedType] = useState<string | null>(null);
  const [selectedExercise, setSelectedExercise] = useState<Exercise | null>(null);
  const [isInWorkout, setIsInWorkout] = useState(false);
  const [showCounter, setShowCounter] = useState(false);
  
  // Fallback exercises for testing
  const fallbackExercises: Record<string, Exercise[]> = {
    'STRENGTH': [
      { id: 1, name: 'Push-ups', type: 'STRENGTH', difficulty: 'BEGINNER', caloriesPerRep: 0.5 },
      { id: 2, name: 'Squats', type: 'STRENGTH', difficulty: 'BEGINNER', caloriesPerRep: 0.8 },
      { id: 6, name: 'Lunges', type: 'STRENGTH', difficulty: 'INTERMEDIATE', caloriesPerRep: 0.7 },
    ],
    'CARDIO': [
      { id: 4, name: 'Jumping Jacks', type: 'CARDIO', difficulty: 'BEGINNER', caloriesPerRep: 0.4 },
      { id: 7, name: 'Mountain Climbers', type: 'CARDIO', difficulty: 'INTERMEDIATE', caloriesPerRep: 0.5 },
    ],
    'CORE': [
      { id: 3, name: 'Planks', type: 'CORE', difficulty: 'INTERMEDIATE', caloriesPerRep: 0.3 },
      { id: 5, name: 'Sit-ups', type: 'CORE', difficulty: 'BEGINNER', caloriesPerRep: 0.6 },
    ],
  };

  useEffect(() => {
    setIsInWorkout(!!currentWorkout);
  }, [currentWorkout]);

  const handleSelectType = async (type: string) => {
    setSelectedType(type);
    setSelectedExercise(null);
    setStep('exercise-selection');
    // Always use fallback exercises for now since API is failing
  };

  const handleSelectExercise = (exercise: Exercise) => {
    setSelectedExercise(exercise);
    setStep('ready-to-start');
  };

  const handleStartExercise = async () => {
    if (!currentWorkout) {
      await startWorkout();
    }
    // Show camera with MediaPipe counter
    setStep('exercising');
    setShowCounter(true);
  };

  const handleCounterComplete = async (data: {
    reps: number;
    duration: number;
    speed: string;
    sets: Array<{ reps: number; duration: number }>;
  }) => {
    // Save the workout set
    if (selectedExercise && currentWorkout) {
      await addSet(currentWorkout.id, {
        repCount: data.reps,
        accuracy: 100, // TODO: Calculate accuracy from form detection
        restDuration: 0,
      });
    }

    // Show completion message
    alert(
      `✅ Workout Complete!\n\nTotal Reps: ${data.reps}\nTotal Time: ${data.duration.toFixed(1)}s\nSpeed: ${data.speed}\n\nGreat job! 💪`
    );

    setShowCounter(false);
    setStep('type-selection');
    setSelectedExercise(null);
    setSelectedType(null);
  };

  const handleCounterCancel = () => {
    setShowCounter(false);
    setStep('ready-to-start');
  };

  const handleBackStep = () => {
    if (step === 'exercise-selection') {
      setStep('type-selection');
      setSelectedType(null);
    } else if (step === 'ready-to-start') {
      setStep('exercise-selection');
    }
  };

  const typeGuide = selectedType ? EXERCISE_GUIDE[selectedType] : null;

  if (showCounter && selectedExercise) {
    const exerciseType = EXERCISE_TYPE_MAP[selectedExercise.name] || 'PUSH_UP';
    return (
      <MediaPipeCounter
        exerciseName={selectedExercise.name}
        exerciseType={exerciseType}
        onComplete={handleCounterComplete}
        onCancel={handleCounterCancel}
      />
    );
  }

  return (
    <div className="exercises-page">
      <header className="page-header">
        <h1>🏋️ Start Exercising</h1>
        <button className="back-btn" onClick={() => navigate('/dashboard')}>
          ← Dashboard
        </button>
      </header>

      {isInWorkout && (
        <div className="workout-banner">
          <p>✅ Active workout session in progress. Your reps will be added to this session.</p>
        </div>
      )}

      <div className="content">
        {/* STEP 1: Select Exercise Type */}
        {step === 'type-selection' && (
          <div className="step-container">
            <h2>Step 1: Choose Your Exercise Type</h2>
            <p className="step-description">Select the type of exercise you want to do today</p>
            
            <div className="exercise-types-grid">
              {['STRENGTH', 'CARDIO', 'CORE'].map((type) => {
                const guide = EXERCISE_GUIDE[type] || {icon: '🏋️', description: type, mediaPipeGuide: ''};
                return (
                  <div
                    key={type}
                    className="type-card"
                    onClick={() => handleSelectType(type)}
                  >
                    <div className="type-icon">{guide.icon}</div>
                    <h3>{type}</h3>
                    <p className="type-description">{guide.description}</p>
                    <div className="mediapipe-info">
                      <p className="label">📱 MediaPipe Tracking:</p>
                      <p className="guide">{guide.mediaPipeGuide}</p>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        )}

        {/* STEP 2: Select Specific Exercise */}
        {step === 'exercise-selection' && selectedType && (
          <div className="step-container">
            <div className="step-header">
              <button className="back-step-btn" onClick={handleBackStep}>
                ← Change Type
              </button>
              <h2>Step 2: Choose Your Exercise</h2>
              <span className="step-info">{typeGuide?.icon} {selectedType} Exercises</span>
            </div>
            <p className="step-description">Select a specific exercise to perform</p>
            
            <div className="exercises-grid">
              {/* Use fallback exercises since API is failing */}
              {(fallbackExercises[selectedType] || []).map((exercise) => (
                <div
                  key={exercise.id}
                  className="exercise-card"
                  onClick={() => handleSelectExercise(exercise)}
                >
                  <div className="exercise-header">
                    <h3>{exercise.name}</h3>
                    <span className={`difficulty-badge difficulty-${exercise.difficulty.toLowerCase()}`}>
                      {exercise.difficulty}
                    </span>
                  </div>
                  <div className="exercise-details">
                    <p className="detail-item">
                      <strong>Calories:</strong> {exercise.caloriesPerRep} per rep
                    </p>
                    <p className="detail-item">
                      <strong>Type:</strong> {exercise.type}
                    </p>
                  </div>
                  <button className="select-btn">Select ▶️</button>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* STEP 3: Ready to Start */}
        {step === 'ready-to-start' && selectedExercise && typeGuide && (
          <div className="step-container ready-to-start">
            <div className="step-header">
              <button className="back-step-btn" onClick={handleBackStep}>
                ← Choose Different Exercise
              </button>
              <h2>Step 3: Ready to Start!</h2>
            </div>
            
            <div className="exercise-summary">
              <div className="summary-header">
                <span className="type-icon">{typeGuide.icon}</span>
                <h3>{selectedExercise.name}</h3>
              </div>
              
              <div className="summary-details">
                <div className="detail-row">
                  <span className="label">Exercise Type:</span>
                  <span className="value">{selectedType}</span>
                </div>
                <div className="detail-row">
                  <span className="label">Difficulty:</span>
                  <span className="value">{selectedExercise.difficulty}</span>
                </div>
                <div className="detail-row">
                  <span className="label">Calories per Rep:</span>
                  <span className="value">{selectedExercise.caloriesPerRep} kcal</span>
                </div>
              </div>

              <div className="mediapipe-preparation">
                <h4>📱 MediaPipe Setup:</h4>
                <div className="setup-steps">
                  <p>✓ Position your camera to see your full body</p>
                  <p>✓ Ensure good lighting</p>
                  <p>✓ Stand 2-3 meters from your camera</p>
                  <p>✓ MediaPipe will track your form and count reps automatically</p>
                </div>
              </div>

              <div className="action-buttons">
                <button className="btn-cancel" onClick={handleBackStep}>
                  ← Back
                </button>
                <button className="btn-start" onClick={handleStartExercise}>
                  🎥 Start Exercise with Camera
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default ExercisesPage;