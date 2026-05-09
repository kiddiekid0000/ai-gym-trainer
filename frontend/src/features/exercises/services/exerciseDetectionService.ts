// Exercise detection service using MediaPipe Pose for side-view rep counting
// @ts-ignore - MediaPipe loaded from CDN
const { Pose } = window;

export interface PoseKeypoint {
  x: number;
  y: number;
  z: number;
  visibility: number;
}

export interface ExerciseSet {
  reps: number;
  duration: number; // in seconds
  startTime: number;
  endTime: number;
}

export type ExerciseType = 'PUSH_UP' | 'SQUAT' | 'JUMPING_JACK' | 'PLANK' | 'CRUNCH';

export class ExerciseDetector {
  private pose: any;
  private lastAngle: number = 0;
  private isGoingDown: boolean = false;
  private isGoingUp: boolean = false;
  private reps: number = 0;
  private setsData: ExerciseSet[] = [];
  private currentSetStart: number = 0;
  private exerciseType: ExerciseType;

  // Plank-specific
  private plankStartTime: number = 0;
  private isInPlankPosition: boolean = false;
  private plankDurations: number[] = [];

  constructor(exerciseType: ExerciseType) {
    this.exerciseType = exerciseType;
    if (Pose) {
      this.pose = new Pose({
        locateFile: (file: string) =>
          `https://cdn.jsdelivr.net/npm/@mediapipe/pose@0.4.1633559618/${file}`,
      });
    }
    this.currentSetStart = Date.now();
  }

  /**
   * Calculate angle between 3 joints
   */
  private calculateAngle(
    pointA: PoseKeypoint,
    pointB: PoseKeypoint,
    pointC: PoseKeypoint
  ): number {
    const radians =
      Math.atan2(pointC.y - pointB.y, pointC.x - pointB.x) -
      Math.atan2(pointA.y - pointB.y, pointA.x - pointB.x);
    let angle = Math.abs((radians * 180.0) / Math.PI);

    if (angle > 180.0) {
      angle = 360.0 - angle;
    }

    return angle;
  }

  /**
   * Calculate distance between 2 joints
   */
  private calculateDistance(pointA: PoseKeypoint, pointB: PoseKeypoint): number {
    return Math.sqrt(
      Math.pow(pointA.x - pointB.x, 2) + Math.pow(pointA.y - pointB.y, 2)
    );
  }

  /**
   * Check if user is in side view position (Y position of hips and shoulders)
   * Side view = x distance between left and right side > y distance
   */
  private isSideView(landmarks: PoseKeypoint[]): boolean {
    const leftShoulder = landmarks[11]; // Left shoulder
    const rightShoulder = landmarks[12]; // Right shoulder
    const leftHip = landmarks[23]; // Left hip
    const rightHip = landmarks[24]; // Right hip

    if (!leftShoulder || !rightShoulder || !leftHip || !rightHip) {
      return false;
    }

    // Side view check: shoulder and hip should be close in x
    const shoulderXDist = Math.abs(leftShoulder.x - rightShoulder.x);
    const hipXDist = Math.abs(leftHip.x - rightHip.x);

    // If both x distances are small, it's a side view
    return shoulderXDist < 0.15 && hipXDist < 0.15;
  }

  /**
   * Detect Push-ups
   * - Down: elbow angle < 90 degrees
   * - Up: elbow angle > 160 degrees
   */
  private detectPushUp(landmarks: PoseKeypoint[]): number {
    const leftShoulder = landmarks[11];
    const leftElbow = landmarks[13];
    const leftWrist = landmarks[15];
    const rightShoulder = landmarks[12];
    const rightElbow = landmarks[14];
    const rightWrist = landmarks[16];

    if (
      !leftShoulder ||
      !leftElbow ||
      !leftWrist ||
      !rightShoulder ||
      !rightElbow ||
      !rightWrist
    ) {
      return 0;
    }

    // Use the more visible arm
    const leftVisibility =
      leftShoulder.visibility +
      leftElbow.visibility +
      leftWrist.visibility;
    const rightVisibility =
      rightShoulder.visibility +
      rightElbow.visibility +
      rightWrist.visibility;

    const angle =
      leftVisibility > rightVisibility
        ? this.calculateAngle(leftShoulder, leftElbow, leftWrist)
        : this.calculateAngle(rightShoulder, rightElbow, rightWrist);

    // Detect rep: down (angle < 90) -> up (angle > 160)
    if (angle < 90 && !this.isGoingDown) {
      this.isGoingDown = true;
      this.isGoingUp = false;
    } else if (angle > 160 && this.isGoingDown && !this.isGoingUp) {
      this.isGoingUp = true;
      this.isGoingDown = false;
      this.reps++;
    }

    return this.reps;
  }

  /**
   * Detect Squats
   * - Down: knee angle < 90 degrees
   * - Up: knee angle > 160 degrees
   */
  private detectSquat(landmarks: PoseKeypoint[]): number {
    const leftHip = landmarks[23];
    const leftKnee = landmarks[25];
    const leftAnkle = landmarks[27];
    const rightHip = landmarks[24];
    const rightKnee = landmarks[26];
    const rightAnkle = landmarks[28];

    if (
      !leftHip ||
      !leftKnee ||
      !leftAnkle ||
      !rightHip ||
      !rightKnee ||
      !rightAnkle
    ) {
      return 0;
    }

    const leftVisibility =
      leftHip.visibility + leftKnee.visibility + leftAnkle.visibility;
    const rightVisibility =
      rightHip.visibility + rightKnee.visibility + rightAnkle.visibility;

    const angle =
      leftVisibility > rightVisibility
        ? this.calculateAngle(leftHip, leftKnee, leftAnkle)
        : this.calculateAngle(rightHip, rightKnee, rightAnkle);

    if (angle < 90 && !this.isGoingDown) {
      this.isGoingDown = true;
      this.isGoingUp = false;
    } else if (angle > 160 && this.isGoingDown && !this.isGoingUp) {
      this.isGoingUp = true;
      this.isGoingDown = false;
      this.reps++;
    }

    return this.reps;
  }

  /**
   * Detect Jumping Jacks
   * - Jump: hip position goes up then down
   * - Arms: spread out then close
   */
  private detectJumpingJack(landmarks: PoseKeypoint[]): number {
    const leftHip = landmarks[23];
    const rightHip = landmarks[24];
    const leftShoulder = landmarks[11];
    const rightShoulder = landmarks[12];
    const leftWrist = landmarks[15];
    const rightWrist = landmarks[16];

    if (
      !leftHip ||
      !rightHip ||
      !leftShoulder ||
      !rightShoulder ||
      !leftWrist ||
      !rightWrist
    ) {
      return 0;
    }

    // Calculate hip height (average of left and right)
    const hipHeight = (leftHip.y + rightHip.y) / 2;

    // Calculate arm spread distance
    const armSpread = Math.abs(leftWrist.x - rightWrist.x);

    // Store current state
    if (!this.lastAngle) {
      this.lastAngle = hipHeight;
    }

    // Detect jump: hip goes down then up, arms spread out
    const hipMovement = this.lastAngle - hipHeight;
    if (hipMovement > 0.05 && armSpread > 0.3 && !this.isGoingDown) {
      this.isGoingDown = true;
    } else if (hipMovement < -0.05 && this.isGoingDown && armSpread < 0.15) {
      this.isGoingDown = false;
      this.reps++;
    }

    this.lastAngle = hipHeight;
    return this.reps;
  }

  /**
   * Detect Plank
   * - Body should be straight (angle between shoulder-hip-ankle ~180)
   * - Count duration while in correct position
   */
  private detectPlank(landmarks: PoseKeypoint[]): number {
    const leftShoulder = landmarks[11];
    const leftHip = landmarks[23];
    const leftAnkle = landmarks[27];
    const rightShoulder = landmarks[12];
    const rightHip = landmarks[24];
    const rightAnkle = landmarks[28];

    if (
      !leftShoulder ||
      !leftHip ||
      !leftAnkle ||
      !rightShoulder ||
      !rightHip ||
      !rightAnkle
    ) {
      return 0;
    }

    // Check if in straight position (angle should be close to 180)
    const leftAngle = this.calculateAngle(leftShoulder, leftHip, leftAnkle);
    const rightAngle = this.calculateAngle(rightShoulder, rightHip, rightAnkle);
    const avgAngle = (leftAngle + rightAngle) / 2;

    // Correct plank position: angle between 160-180 (straight body)
    const isCorrectPosition = avgAngle > 160 && avgAngle < 200;

    if (isCorrectPosition && !this.isInPlankPosition) {
      this.plankStartTime = Date.now();
      this.isInPlankPosition = true;
    } else if (!isCorrectPosition && this.isInPlankPosition) {
      const plankDuration = (Date.now() - this.plankStartTime) / 1000; // in seconds
      this.plankDurations.push(plankDuration);
      this.isInPlankPosition = false;
      this.reps++; // Count as 1 plank hold
    }

    return this.reps;
  }

  /**
   * Detect Crunches
   * - Crunch: torso bends forward (distance shoulder to hip decreases)
   */
  private detectCrunch(landmarks: PoseKeypoint[]): number {
    const leftShoulder = landmarks[11];
    const leftHip = landmarks[23];
    const rightShoulder = landmarks[12];
    const rightHip = landmarks[24];

    if (!leftShoulder || !leftHip || !rightShoulder || !rightHip) {
      return 0;
    }

    const leftDistance = this.calculateDistance(leftShoulder, leftHip);
    const rightDistance = this.calculateDistance(rightShoulder, rightHip);
    const avgDistance = (leftDistance + rightDistance) / 2;

    if (!this.lastAngle) {
      this.lastAngle = avgDistance;
    }

    // Detect crunch: distance decreases then increases
    const distanceChange = this.lastAngle - avgDistance;

    if (distanceChange > 0.05 && !this.isGoingDown) {
      this.isGoingDown = true;
      this.isGoingUp = false;
    } else if (distanceChange < -0.05 && this.isGoingDown && !this.isGoingUp) {
      this.isGoingUp = true;
      this.isGoingDown = false;
      this.reps++;
    }

    this.lastAngle = avgDistance;
    return this.reps;
  }

  /**
   * Process pose results and detect exercise reps
   */
  processFrame(landmarks: PoseKeypoint[]): number {
    if (!this.isSideView(landmarks)) {
      return 0; // Not in correct position
    }

    switch (this.exerciseType) {
      case 'PUSH_UP':
        return this.detectPushUp(landmarks);
      case 'SQUAT':
        return this.detectSquat(landmarks);
      case 'JUMPING_JACK':
        return this.detectJumpingJack(landmarks);
      case 'PLANK':
        return this.detectPlank(landmarks);
      case 'CRUNCH':
        return this.detectCrunch(landmarks);
      default:
        return 0;
    }
  }

  /**
   * Get current metrics
   */
  getMetrics() {
    const currentTime = Date.now();
    const totalDuration = (currentTime - this.currentSetStart) / 1000; // in seconds
    const speed = this.reps > 0 ? (this.reps / totalDuration).toFixed(2) : '0';

    return {
      reps: this.reps,
      duration: totalDuration.toFixed(1),
      speed: `${speed} reps/sec`,
      plankDurations: this.exerciseType === 'PLANK' ? this.plankDurations : [],
      totalPlankTime:
        this.exerciseType === 'PLANK'
          ? this.plankDurations.reduce((a, b) => a + b, 0).toFixed(1)
          : '0',
    };
  }

  /**
   * End current set and save metrics
   */
  endSet(): ExerciseSet {
    if (this.exerciseType === 'PLANK' && this.isInPlankPosition) {
      const plankDuration = (Date.now() - this.plankStartTime) / 1000;
      this.plankDurations.push(plankDuration);
      this.isInPlankPosition = false;
    }

    const set: ExerciseSet = {
      reps: this.reps,
      duration: (Date.now() - this.currentSetStart) / 1000,
      startTime: this.currentSetStart,
      endTime: Date.now(),
    };

    this.setsData.push(set);
    this.resetSet();
    return set;
  }

  /**
   * Reset for next set
   */
  private resetSet() {
    this.reps = 0;
    this.isGoingDown = false;
    this.isGoingUp = false;
    this.lastAngle = 0;
    this.currentSetStart = Date.now();
    this.plankStartTime = 0;
    this.isInPlankPosition = false;
  }

  /**
   * Get all sets data
   */
  getAllSets(): ExerciseSet[] {
    return this.setsData;
  }

  /**
   * Cleanup
   */
  async close() {
    await this.pose.close();
  }
}


