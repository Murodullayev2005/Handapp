import time
from enum import Enum

class TrackingTarget(Enum):
    INDEX_FINGER_TIP = "INDEX_FINGER_TIP"
    PALM_CENTER = "PALM_CENTER"
    WRIST = "WRIST"

class TrajectoryPoint:
    def __init__(self, x: float, y: float, timestamp_sec: float = None):
        self.x = x  # Normalized 0.0 .. 1.0
        self.y = y  # Normalized 0.0 .. 1.0
        self.timestamp_sec = timestamp_sec if timestamp_sec is not None else time.time()

class TrajectoryTracker:
    def __init__(self, tracking_target: TrackingTarget = TrackingTarget.INDEX_FINGER_TIP, fade_duration_sec: float = 1.5, smoothing_alpha: float = 0.45):
        self.tracking_target = tracking_target
        self.fade_duration_sec = fade_duration_sec
        self.smoothing_alpha = smoothing_alpha
        self.points_map = {}  # hand_index -> list of TrajectoryPoint
        self.last_smoothed_points = {}  # hand_index -> (x, y)

    def add_landmarks(self, hand_index: int, landmarks):
        if not landmarks or len(landmarks) < 21:
            return

        if self.tracking_target == TrackingTarget.INDEX_FINGER_TIP:
            tip = landmarks[8]
            raw_target = (tip.x, tip.y)
        elif self.tracking_target == TrackingTarget.WRIST:
            wrist = landmarks[0]
            raw_target = (wrist.x, wrist.y)
        elif self.tracking_target == TrackingTarget.PALM_CENTER:
            w = landmarks[0]
            index_mcp = landmarks[5]
            pinky_mcp = landmarks[17]
            cx = (w.x + index_mcp.x + pinky_mcp.x) / 3.0
            cy = (w.y + index_mcp.y + pinky_mcp.y) / 3.0
            raw_target = (cx, cy)
        else:
            tip = landmarks[8]
            raw_target = (tip.x, tip.y)

        # Exponential moving average filter
        prev_smoothed = self.last_smoothed_points.get(hand_index)
        if prev_smoothed:
            sx = prev_smoothed[0] + self.smoothing_alpha * (raw_target[0] - prev_smoothed[0])
            sy = prev_smoothed[1] + self.smoothing_alpha * (raw_target[1] - prev_smoothed[1])
            final_target = (sx, sy)
        else:
            final_target = raw_target

        self.last_smoothed_points[hand_index] = final_target

        if hand_index not in self.points_map:
            self.points_map[hand_index] = []
        self.points_map[hand_index].append(TrajectoryPoint(final_target[0], final_target[1]))

    def purge_old_points(self, current_time_sec: float = None):
        if current_time_sec is None:
            current_time_sec = time.time()

        empty_indices = []
        for hand_index, points in self.points_map.items():
            self.points_map[hand_index] = [
                pt for pt in points if (current_time_sec - pt.timestamp_sec) <= self.fade_duration_sec
            ]
            if not self.points_map[hand_index]:
                empty_indices.append(hand_index)

        for idx in empty_indices:
            del self.points_map[idx]
            if idx in self.last_smoothed_points:
                del self.last_smoothed_points[idx]

    def clear_all(self):
        self.points_map.clear()
        self.last_smoothed_points.clear()

    def get_active_trajectories(self):
        return self.points_map
