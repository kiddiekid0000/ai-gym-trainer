// MediaPipe-based exercise counter component with side-view camera
import React, { useEffect, useRef, useState } from 'react';
// @ts-ignore - MediaPipe loaded from CDN
const { Pose, Camera } = window;
import { ExerciseDetector, type PoseKeypoint, type ExerciseType } from '../services/exerciseDetectionService';
import './MediaPipeCounter.css';

interface MediaPipeCounterProps {
  exerciseName: string;
  exerciseType: ExerciseType;
  onComplete: (data: {
    reps: number;
    duration: number;
    speed: string;
    sets: Array<{ reps: number; duration: number }>;
  }) => void;
  onCancel: () => void;
}

interface ExerciseMetrics {
  reps: number;
  duration: string;
  speed: string;
  plankDurations: number[];
  totalPlankTime: string;
}

const MediaPipeCounter: React.FC<MediaPipeCounterProps> = ({
  exerciseName,
  exerciseType,
  onComplete,
  onCancel,
}) => {
  const videoRef = useRef<HTMLVideoElement>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const [isInitialized, setIsInitialized] = useState(false);
  const [metrics, setMetrics] = useState<ExerciseMetrics>({
    reps: 0,
    duration: '0.0',
    speed: '0 reps/sec',
    plankDurations: [],
    totalPlankTime: '0',
  });
  const [isExercising, setIsExercising] = useState(false);
  const [feedbackMessage, setFeedbackMessage] = useState('Position camera to see your side view');
  const [sessionData, setSessionData] = useState<Array<{ reps: number; duration: number }>>([]);

  const detectorRef = useRef<ExerciseDetector | null>(null);
  const cameraRef = useRef<any>(null);
  const frameCounterRef = useRef(0);

  useEffect(() => {
    initializeMediaPipe();
    return () => {
      cleanup();
    };
  }, []);

  const initializeMediaPipe = async () => {
    try {
      const pose = new Pose({
        locateFile: (file: string) =>
          `https://cdn.jsdelivr.net/npm/@mediapipe/pose@0.4.1633559618/${file}`,
      });

      pose.setOptions({
        modelComplexity: 1,
        smoothLandmarks: true,
        minDetectionConfidence: 0.5,
        minTrackingConfidence: 0.5,
      });

      pose.onResults(onResults);

      if (videoRef.current) {
        cameraRef.current = new Camera(videoRef.current, {
          onFrame: async () => {
            await pose.send({ image: videoRef.current! });
          },
          width: 640,
          height: 480,
        });

        cameraRef.current.start();
        detectorRef.current = new ExerciseDetector(exerciseType);
        setIsInitialized(true);
        setFeedbackMessage('Position your body on the side. Click Start when ready!');
      }
    } catch (error) {
      console.error('Error initializing MediaPipe:', error);
      setFeedbackMessage('Error loading MediaPipe. Please refresh the page.');
    }
  };

  const onResults = (results: any) => {
    const canvasElement = canvasRef.current;
    if (!canvasElement || !videoRef.current) return;

    const canvasCtx = canvasElement.getContext('2d');
    if (!canvasCtx) return;

    // Clear canvas
    canvasCtx.save();
    canvasCtx.clearRect(0, 0, canvasElement.width, canvasElement.height);
    canvasCtx.drawImage(results.image, 0, 0, canvasElement.width, canvasElement.height);

    if (isExercising && detectorRef.current && results.poseLandmarks) {
      // Process frame
      const reps = detectorRef.current.processFrame(
        results.poseLandmarks as PoseKeypoint[]
      );

      // Update metrics every 10 frames
      frameCounterRef.current++;
      if (frameCounterRef.current % 10 === 0) {
        const currentMetrics = detectorRef.current.getMetrics();
        setMetrics(currentMetrics);
      }

      // Draw skeleton
      drawPoseSkeleton(canvasCtx, results.poseLandmarks);
      drawRepCounter(canvasCtx, reps);
    } else if (!isExercising && results.poseLandmarks) {
      // Draw skeleton for preview
      drawPoseSkeleton(canvasCtx, results.poseLandmarks, 0.5);
      drawStandby(canvasCtx);
    }

    canvasCtx.restore();
  };

  const drawPoseSkeleton = (
    ctx: CanvasRenderingContext2D,
    landmarks: any[],
    opacity: number = 1
  ) => {
    const connections = [
      [11, 12], // shoulders
      [11, 13],
      [13, 15], // left arm
      [12, 14],
      [14, 16], // right arm
      [11, 23],
      [12, 24], // torso
      [23, 24], // hips
      [23, 25],
      [25, 27], // left leg
      [24, 26],
      [26, 28], // right leg
    ];

    // Draw connections
    ctx.strokeStyle = `rgba(0, 255, 0, ${opacity})`;
    ctx.lineWidth = 2;

    connections.forEach(([from, to]) => {
      const p1 = landmarks[from];
      const p2 = landmarks[to];

      if (p1?.visibility > 0.5 && p2?.visibility > 0.5) {
        ctx.beginPath();
        ctx.moveTo(p1.x * canvasRef.current!.width, p1.y * canvasRef.current!.height);
        ctx.lineTo(p2.x * canvasRef.current!.width, p2.y * canvasRef.current!.height);
        ctx.stroke();
      }
    });

    // Draw joints
    ctx.fillStyle = `rgba(0, 255, 0, ${opacity})`;
    landmarks.forEach((landmark) => {
      if (landmark.visibility > 0.5) {
        ctx.beginPath();
        ctx.arc(
          landmark.x * canvasRef.current!.width,
          landmark.y * canvasRef.current!.height,
          5,
          0,
          2 * Math.PI
        );
        ctx.fill();
      }
    });
  };

  const drawRepCounter = (ctx: CanvasRenderingContext2D, reps: number) => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    // Draw counter background
    ctx.fillStyle = 'rgba(0, 0, 0, 0.7)';
    ctx.fillRect(10, 10, 150, 80);

    // Draw reps
    ctx.fillStyle = '#00FF00';
    ctx.font = 'bold 48px Arial';
    ctx.fillText(reps.toString(), 50, 60);

    ctx.font = '16px Arial';
    ctx.fillText('REPS', 45, 75);
  };

  const drawStandby = (ctx: CanvasRenderingContext2D) => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    ctx.fillStyle = 'rgba(255, 200, 0, 0.7)';
    ctx.fillRect(10, 10, 300, 60);

    ctx.fillStyle = '#000';
    ctx.font = '18px Arial';
    ctx.fillText('📱 Position on side view • Click START', 20, 40);
  };

  const handleStart = () => {
    setIsExercising(true);
    setFeedbackMessage('🔴 Recording... Perform your exercise!');
  };

  const handleStop = () => {
    if (detectorRef.current) {
      const set = detectorRef.current.endSet();
      setSessionData([...sessionData, { reps: set.reps, duration: set.duration }]);
      setIsExercising(false);

      setFeedbackMessage(`✅ Set complete! ${set.reps} reps in ${set.duration.toFixed(1)}s`);
    }
  };

  const handleFinish = () => {
    if (detectorRef.current) {
      const allSets = detectorRef.current.getAllSets();
      const totalReps = allSets.reduce((sum, set) => sum + set.reps, 0);
      const totalDuration = allSets.reduce((sum, set) => sum + set.duration, 0);
      const avgSpeed = totalReps > 0 ? (totalReps / totalDuration).toFixed(2) : '0';

      onComplete({
        reps: totalReps,
        duration: totalDuration,
        speed: `${avgSpeed} reps/sec`,
        sets: allSets.map((set) => ({ reps: set.reps, duration: set.duration })),
      });
    }
  };

  const cleanup = async () => {
    if (cameraRef.current) {
      cameraRef.current.stop();
    }
    if (detectorRef.current) {
      await detectorRef.current.close();
    }
  };

  if (!isInitialized) {
    return <div className="mediapipe-loading">Loading camera and pose detection...</div>;
  }

  return (
    <div className="mediapipe-counter">
      <div className="counter-header">
        <h2>{exerciseName}</h2>
        <button className="close-btn" onClick={onCancel}>
          ✕
        </button>
      </div>

      <div className="counter-container">
        {/* Hidden video element for pose detection */}
        <video ref={videoRef} style={{ display: 'none' }} />

        {/* Canvas for display */}
        <canvas ref={canvasRef} width={640} height={480} className="pose-canvas" />

        <div className="metrics-display">
          <div className="metric">
            <div className="metric-label">Reps</div>
            <div className="metric-value">{metrics.reps}</div>
          </div>
          <div className="metric">
            <div className="metric-label">Time</div>
            <div className="metric-value">{metrics.duration}s</div>
          </div>
          <div className="metric">
            <div className="metric-label">Speed</div>
            <div className="metric-value">{metrics.speed}</div>
          </div>
          {exerciseType === 'PLANK' && (
            <div className="metric">
              <div className="metric-label">Plank Time</div>
              <div className="metric-value">{metrics.totalPlankTime}s</div>
            </div>
          )}
        </div>
      </div>

      <div className="feedback-message">{feedbackMessage}</div>

      <div className="session-history">
        <h3>Sets</h3>
        {sessionData.length === 0 ? (
          <p>No sets recorded yet</p>
        ) : (
          <ul>
            {sessionData.map((set, idx) => (
              <li key={idx}>
                Set {idx + 1}: {set.reps} reps in {set.duration.toFixed(1)}s
              </li>
            ))}
          </ul>
        )}
      </div>

      <div className="counter-controls">
        {!isExercising ? (
          <button className="btn-start" onClick={handleStart}>
            🔴 START
          </button>
        ) : (
          <button className="btn-stop" onClick={handleStop}>
            ⏹ STOP
          </button>
        )}
        <button
          className="btn-finish"
          onClick={handleFinish}
          disabled={sessionData.length === 0}
        >
          ✅ FINISH
        </button>
        <button className="btn-cancel" onClick={onCancel}>
          ✕ CANCEL
        </button>
      </div>
    </div>
  );
};

export default MediaPipeCounter;
