import cv2
import numpy as np
import time
from hand_distance_calculator import DistanceEvaluation, ProximityState

# Color themes in OpenCV BGR format: (B, G, R)
COLOR_THEMES = {
    "Neon Cyan": {"primary": (255, 240, 0), "glow": (255, 200, 0)},
    "Glowing Purple": {"primary": (255, 0, 208), "glow": (255, 0, 160)},
    "Electric Green": {"primary": (102, 255, 0), "glow": (50, 200, 0)},
    "Fire Red": {"primary": (51, 51, 255), "glow": (0, 0, 200)},
    "Solar Gold": {"primary": (0, 184, 255), "glow": (0, 140, 220)}
}

HAND_CONNECTIONS = [
    (0, 1), (1, 2), (2, 3), (3, 4),        # Thumb
    (0, 5), (5, 6), (6, 7), (7, 8),        # Index
    (5, 9), (9, 10), (10, 11), (11, 12),   # Middle
    (9, 13), (13, 14), (14, 15), (15, 16), # Ring
    (13, 17), (0, 17), (17, 18), (18, 19), (19, 20) # Pinky
]

class UIRenderer:
    def __init__(self):
        self.color_theme_names = list(COLOR_THEMES.keys())
        self.current_theme_index = 0

    def get_current_theme(self):
        theme_name = self.color_theme_names[self.current_theme_index]
        return theme_name, COLOR_THEMES[theme_name]

    def cycle_color_theme(self):
        self.current_theme_index = (self.current_theme_index + 1) % len(self.color_theme_names)
        return self.color_theme_names[self.current_theme_index]

    def draw_skeleton(self, frame: np.ndarray, landmarks_list):
        h, w, _ = frame.shape
        overlay = frame.copy()

        for landmarks in landmarks_list:
            if not landmarks or len(landmarks) < 21:
                continue

            # Draw bone connections
            for start_idx, end_idx in HAND_CONNECTIONS:
                p1 = (int(landmarks[start_idx].x * w), int(landmarks[start_idx].y * h))
                p2 = (int(landmarks[end_idx].x * w), int(landmarks[end_idx].y * h))
                cv2.line(overlay, p1, p2, (240, 240, 240), 2, cv2.LINE_AA)

            # Draw joint nodes
            for lm in landmarks:
                cx, cy = int(lm.x * w), int(lm.y * h)
                cv2.circle(overlay, (cx, cy), 5, (255, 240, 0), -1, cv2.LINE_AA)
                cv2.circle(overlay, (cx, cy), 2, (255, 255, 255), -1, cv2.LINE_AA)

        # Blend overlay with 0.65 weight for translucent skeleton look
        cv2.addWeighted(overlay, 0.65, frame, 0.35, 0, frame)

    def draw_trajectories(self, frame: np.ndarray, trajectories, fade_duration_sec: float, stroke_width: int):
        h, w, _ = frame.shape
        now = time.time()
        _, colors = self.get_current_theme()
        primary_bgr = colors["primary"]
        glow_bgr = colors["glow"]

        for hand_index, points in trajectories.items():
            if len(points) < 2:
                continue

            # Overlay layer for alpha-blended vector lines
            overlay = frame.copy()

            for i in range(len(points) - 1):
                pt1 = points[i]
                pt2 = points[i + 1]

                age1 = now - pt1.timestamp_sec
                age2 = now - pt2.timestamp_sec
                avg_age = (age1 + age2) / 2.0
                alpha = max(0.0, min(1.0, 1.0 - (avg_age / fade_duration_sec)))

                if alpha <= 0.02:
                    continue

                p1 = (int(pt1.x * w), int(pt1.y * h))
                p2 = (int(pt2.x * w), int(pt2.y * h))

                # Compute line color scaled by alpha
                line_color = (
                    int(primary_bgr[0] * alpha),
                    int(primary_bgr[1] * alpha),
                    int(primary_bgr[2] * alpha)
                )

                # Outer ambient glow line
                cv2.line(overlay, p1, p2, line_color, int(stroke_width * 2.2), cv2.LINE_AA)
                # Core line
                cv2.line(frame, p1, p2, line_color, stroke_width, cv2.LINE_AA)

            cv2.addWeighted(overlay, 0.4, frame, 0.6, 0, frame)

            # Draw glowing head tip on newest point
            latest = points[-1]
            age = now - latest.timestamp_sec
            head_alpha = max(0.0, min(1.0, 1.0 - (age / fade_duration_sec)))
            if head_alpha > 0.05:
                head_pt = (int(latest.x * w), int(latest.y * h))
                cv2.circle(frame, head_pt, int(stroke_width * 1.3), primary_bgr, -1, cv2.LINE_AA)
                cv2.circle(frame, head_pt, int(stroke_width * 0.6), (255, 255, 255), -1, cv2.LINE_AA)

    def draw_warning_banner(self, frame: np.ndarray, evaluation: DistanceEvaluation):
        if evaluation.proximity_state == ProximityState.SAFE:
            return

        h, w, _ = frame.shape
        is_too_close = (evaluation.proximity_state == ProximityState.TOO_CLOSE)

        # Pulse effect using sine time wave
        pulse_alpha = 0.8 + 0.2 * np.sin(time.time() * 8.0)

        banner_h = 75
        margin_x = int(w * 0.05)
        top_y = 65

        overlay = frame.copy()
        box_bg = (20, 20, 180) if is_too_close else (20, 140, 220)  # Red vs Amber (BGR)
        border_color = (80, 80, 255) if is_too_close else (80, 200, 255)

        cv2.rectangle(overlay, (margin_x, top_y), (w - margin_x, top_y + banner_h), box_bg, -1)
        cv2.rectangle(overlay, (margin_x, top_y), (w - margin_x, top_y + banner_h), border_color, 2, cv2.LINE_AA)

        cv2.addWeighted(overlay, pulse_alpha, frame, 1.0 - pulse_alpha, 0, frame)

        # Text labels
        title = "TOO CLOSE TO SCREEN!" if is_too_close else "SAFE DISTANCE WARNING"
        desc = "Please move your hand further away from the device." if is_too_close else "Hand is approaching safe proximity boundary."

        cv2.putText(frame, f"[!] {title}", (margin_x + 20, top_y + 30), cv2.FONT_HERSHEY_SIMPLEX, 0.75, (255, 255, 255), 2, cv2.LINE_AA)
        cv2.putText(frame, desc, (margin_x + 20, top_y + 55), cv2.FONT_HERSHEY_SIMPLEX, 0.48, (230, 230, 230), 1, cv2.LINE_AA)

        # Distance Pill Badge
        badge_text = f"{evaluation.estimated_distance_cm} cm"
        badge_x = w - margin_x - 110
        cv2.rectangle(frame, (badge_x, top_y + 18), (badge_x + 90, top_y + 54), (0, 0, 0), -1)
        cv2.putText(frame, badge_text, (badge_x + 12, top_y + 42), cv2.FONT_HERSHEY_SIMPLEX, 0.65, (255, 255, 255), 2, cv2.LINE_AA)

    def draw_hud(self, frame: np.ndarray, fps: int, inference_ms: float, hands_count: int, evaluation: DistanceEvaluation, tracking_target_name: str, show_skeleton: bool, show_controls: bool):
        h, w, _ = frame.shape
        overlay = frame.copy()

        # Top Header Bar Background
        cv2.rectangle(overlay, (10, 10), (w - 10, 52), (20, 24, 32), -1)
        cv2.rectangle(overlay, (10, 10), (w - 10, 52), (70, 75, 85), 1)

        cv2.addWeighted(overlay, 0.8, frame, 0.2, 0, frame)

        # Hands count status badge
        status_color = (0, 255, 100) if hands_count > 0 else (100, 100, 255)
        cv2.circle(frame, (30, 31), 6, status_color, -1, cv2.LINE_AA)
        status_text = f"{hands_count} Hand{'s' if hands_count > 1 else ''}" if hands_count > 0 else "Searching..."
        cv2.putText(frame, status_text, (44, 36), cv2.FONT_HERSHEY_SIMPLEX, 0.52, (255, 255, 255), 1, cv2.LINE_AA)

        # Performance Stats (FPS | Latency)
        perf_text = f"{fps} FPS | {inference_ms:.1f}ms"
        cv2.putText(frame, perf_text, (int(w * 0.38), 36), cv2.FONT_HERSHEY_SIMPLEX, 0.52, (255, 240, 0), 2, cv2.LINE_AA)

        # Proximity Badge
        state_label = evaluation.proximity_state.value
        state_color = (0, 255, 100) if state_label == "SAFE" else ((0, 200, 255) if state_label == "WARNING" else (50, 50, 255))
        cv2.putText(frame, f"Proximity: {state_label}", (int(w * 0.70), 36), cv2.FONT_HERSHEY_SIMPLEX, 0.52, state_color, 2, cv2.LINE_AA)

        # Draw Controls / Help Panel if enabled
        if show_controls:
            ctrl_y = h - 110
            ctrl_overlay = frame.copy()
            cv2.rectangle(ctrl_overlay, (10, ctrl_y), (420, h - 10), (15, 18, 24), -1)
            cv2.rectangle(ctrl_overlay, (10, ctrl_y), (420, h - 10), (60, 65, 75), 1)
            cv2.addWeighted(ctrl_overlay, 0.85, frame, 0.15, 0, frame)

            theme_name, _ = self.get_current_theme()
            cv2.putText(frame, f"Target: {tracking_target_name} | Theme: {theme_name}", (20, ctrl_y + 24), cv2.FONT_HERSHEY_SIMPLEX, 0.45, (0, 240, 255), 1, cv2.LINE_AA)
            cv2.putText(frame, "[C] Theme | [T] Target | [K] Skeleton | [R] Clear", (20, ctrl_y + 50), cv2.FONT_HERSHEY_SIMPLEX, 0.43, (220, 220, 220), 1, cv2.LINE_AA)
            cv2.putText(frame, "[W] Adjust Threshold | [S] Hide Help | [Q] Quit", (20, ctrl_y + 76), cv2.FONT_HERSHEY_SIMPLEX, 0.43, (220, 220, 220), 1, cv2.LINE_AA)
