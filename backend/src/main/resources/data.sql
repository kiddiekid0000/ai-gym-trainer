-- Initial data for Exercise and Workout Tracking

-- Insert sample exercises
INSERT INTO exercises (name, type, calories_per_rep, difficulty) VALUES
('Push-ups', 'STRENGTH', 0.5, 'BEGINNER'),
('Squats', 'STRENGTH', 0.8, 'BEGINNER'),
('Planks', 'CORE', 0.3, 'INTERMEDIATE'),
('Burpees', 'CARDIO', 1.2, 'ADVANCED'),
('Jumping Jacks', 'CARDIO', 0.2, 'BEGINNER'),
('Lunges', 'STRENGTH', 0.7, 'INTERMEDIATE'),
('Mountain Climbers', 'CARDIO', 0.4, 'INTERMEDIATE'),
('Sit-ups', 'CORE', 0.4, 'BEGINNER'),
('Pull-ups', 'STRENGTH', 1.0, 'ADVANCED'),
('High Knees', 'CARDIO', 0.3, 'BEGINNER');

-- Best records table (leaderboard)
CREATE TABLE IF NOT EXISTS best_records (
    id BIGSERIAL PRIMARY KEY,
    exercise_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    total_reps BIGINT NOT NULL DEFAULT 0,
    accuracy DECIMAL(5,2) NOT NULL DEFAULT 0.0,
    streak_days INTEGER NOT NULL DEFAULT 0,
    rank INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY (exercise_id) REFERENCES exercises(id),
    UNIQUE(exercise_id, user_id)
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_workout_sessions_user_id ON workout_sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_workout_sessions_status ON workout_sessions(status);
CREATE INDEX IF NOT EXISTS idx_workout_sets_session_id ON workout_sets(session_id);
CREATE INDEX IF NOT EXISTS idx_exercise_records_user_id ON exercise_records(user_id);
CREATE INDEX IF NOT EXISTS idx_exercise_records_exercise_id ON exercise_records(exercise_id);
CREATE INDEX IF NOT EXISTS idx_exercise_records_date ON exercise_records(date);
CREATE INDEX IF NOT EXISTS idx_best_records_exercise_id ON best_records(exercise_id);
CREATE INDEX IF NOT EXISTS idx_best_records_total_reps ON best_records(total_reps DESC);

-- Insert initial exercise data
INSERT INTO exercises (name, type, calories_per_rep, difficulty) VALUES
('Push Up', 'PUSH_UP', 0.5, 'MEDIUM'),
('Squat', 'SQUAT', 0.8, 'MEDIUM'),
('Jumping Jack', 'JUMPING_JACK', 0.3, 'EASY')
ON CONFLICT (type) DO NOTHING;