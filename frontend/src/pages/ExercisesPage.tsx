// src/pages/ExercisesPage.tsx
import { useState } from 'react';
import { useExercises } from '../features/exercises/hooks/useExercises';
import './ExercisesPage.css';

const ExercisesPage = () => {
  const { exercises, exerciseTypes, loading, error, fetchAllExercises, fetchExercisesByType } = useExercises();
  const [selectedType, setSelectedType] = useState<string>('ALL');

  const handleTypeChange = (type: string) => {
    setSelectedType(type);
    if (type === 'ALL') {
      fetchAllExercises();
    } else {
      fetchExercisesByType(type);
    }
  };

  if (loading) return <div>Loading...</div>;
  if (error) return <div>Error: {error}</div>;

  return (
    <div className="exercises-page">
      <h1>Exercises</h1>
      <div className="filters">
        <button
          className={selectedType === 'ALL' ? 'active' : ''}
          onClick={() => handleTypeChange('ALL')}
        >
          All Exercises
        </button>
        {exerciseTypes.map((type) => (
          <button
            key={type}
            className={selectedType === type ? 'active' : ''}
            onClick={() => handleTypeChange(type)}
          >
            {type}
          </button>
        ))}
      </div>
      <div className="exercises-list">
        {exercises.map((exercise) => (
          <div key={exercise.id} className="exercise-card">
            <h3>{exercise.name}</h3>
            <p>Type: {exercise.type}</p>
            <p>Difficulty: {exercise.difficulty}</p>
            <p>Calories per Rep: {exercise.caloriesPerRep}</p>
          </div>
        ))}
      </div>
    </div>
  );
};

export default ExercisesPage;