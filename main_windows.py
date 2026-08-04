import cv2
import time
import sys
import os
import numpy as np

import mediapipe as mp
from mediapipe.tasks import python
from mediapipe.tasks.python import vision

from hand_distance_calculator import HandDistanceCalculator
from trajectory_tracker import TrajectoryTracker, TrackingTarget
from ui_renderer import UIRenderer

def get_model_path():
    possible_paths = [
        os.path.join("app", "src", "main", "assets", "hand_landmarker.task"),
        "hand_landmarker.task"
    ]
    for path in possible_paths:
        if os.path.exists(path):
            return path
    return None

def open_webcam():
    # Try different camera indices (0..3) and backends (DirectShow, MSMF, Default)
    backends = []
    if sys.platform.startswith('win'):
        backends = [("DirectShow", cv2.CAP_DSHOW), ("Media Foundation", cv2.CAP_MSMF), ("Default", cv2.CAP_ANY)]
    else:
        backends = [("Default", cv2.CAP_ANY)]

    for index in range(4):
        for backend_name, backend_api in backends:
            cap = cv2.VideoCapture(index, backend_api)
            if cap.isOpened():
                # Read a frame to verify real stream access
                ret, frame = cap.read()
                if ret and frame is not None and frame.size > 0:
                    print(f"[+] Successfully opened webcam (Index: {index}, Backend: {backend_name})")
                    return cap
                cap.release()
    return None

def main():
    print("=" * 65)
    print("  Starting Windows Desktop Hand Trajectory & Distance Warning App")
    print("=" * 65)

    model_path = get_model_path()
    if not model_path:
        print("ERROR: hand_landmarker.task model file not found!")
        sys.exit(1)

    print(f"[+] Loaded MediaPipe Hand Model: {model_path}")

    # Initialize MediaPipe HandLandmarker in VIDEO mode
    base_options = python.BaseOptions(model_asset_path=model_path)
    options = vision.HandLandmarkerOptions(
        base_options=base_options,
        running_mode=vision.RunningMode.VIDEO,
        num_hands=2,
        min_hand_detection_confidence=0.5,
        min_hand_presence_confidence=0.5,
        min_tracking_confidence=0.5
    )
    landmarker = vision.HandLandmarker.create_from_options(options)

    # Initialize Helpers
    distance_calculator = HandDistanceCalculator(warning_threshold_ratio=0.38, too_close_threshold_ratio=0.52)
    trajectory_tracker = TrajectoryTracker(tracking_target=TrackingTarget.INDEX_FINGER_TIP, fade_duration_sec=1.5)
    ui_renderer = UIRenderer()

    # Open PC/Laptop Webcam
    cap = open_webcam()
    if not cap:
        print("\n" + "=" * 65)
        print("ERROR: Could not open any webcam (tried camera indices 0-3).")
        print("=" * 65)
        print("Troubleshooting Checklist:")
        print(" 1. Close any other application using your camera (Zoom, Teams, Skype, Discord, Chrome, OBS).")
        print(" 2. Check Windows Camera Privacy Settings:")
        print("    Settings -> Privacy & security -> Camera -> Ensure 'Let desktop apps access your camera' is ON.")
        print(" 3. Check if your laptop has a physical webcam privacy slider or Fn toggle key (e.g. Fn+F6 / Fn+F10).")
        print(" 4. Open Windows 'Camera' app to verify your webcam functions outside Python.")
        print("=" * 65 + "\n")
        sys.exit(1)

    # Set camera resolution (1280x720 preferred)
    cap.set(cv2.CAP_PROP_FRAME_WIDTH, 1280)
    cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 720)

    window_name = "Hand Motion Trajectory & Distance Warning - Windows Desktop"
    cv2.namedWindow(window_name, cv2.WINDOW_NORMAL)
    cv2.resizeWindow(window_name, 1280, 720)

    show_skeleton = True
    show_controls = True
    stroke_width = 8

    # FPS Calculation
    frame_count = 0
    fps = 30
    last_fps_time = time.time()
    start_time_ms = int(time.time() * 1000)

    print("\n[+] Windows Camera Stream Active!")
    print("    Press [C] to cycle trajectory color theme")
    print("    Press [T] to toggle tracking target (Fingertip vs Palm)")
    print("    Press [K] to toggle hand skeleton mesh")
    print("    Press [R] to reset current trajectory line")
    print("    Press [W] to adjust warning distance sensitivity")
    print("    Press [S] to toggle help HUD")
    print("    Press [Q] or [Esc] to quit\n")

    try:
        while cap.isOpened():
            ret, frame = cap.read()
            if not ret:
                print("Failed to read webcam frame.")
                break

            # Mirror horizontally for selfie view match on laptops
            frame = cv2.flip(frame, 1)
            h, w, _ = frame.shape

            # Convert BGR to RGB for MediaPipe
            rgb_frame = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
            mp_image = mp.Image(image_format=mp.ImageFormat.SRGB, data=rgb_frame)

            timestamp_ms = int(time.time() * 1000) - start_time_ms

            # Inference
            t0 = time.time()
            result = landmarker.detect_for_video(mp_image, timestamp_ms)
            t1 = time.time()
            inference_ms = (t1 - t0) * 1000.0

            # Calculate FPS
            frame_count += 1
            now_sec = time.time()
            if now_sec - last_fps_time >= 1.0:
                fps = frame_count
                frame_count = 0
                last_fps_time = now_sec

            landmarks_list = result.hand_landmarks if result else []
            hands_count = len(landmarks_list)

            # Update Trajectory Tracker
            for idx, landmarks in enumerate(landmarks_list):
                trajectory_tracker.add_landmarks(idx, landmarks)
            trajectory_tracker.purge_old_points(now_sec)

            # Evaluate Distance Proximity using primary hand
            primary_hand = landmarks_list[0] if landmarks_list else []
            evaluation = distance_calculator.evaluate_hand_proximity(primary_hand)

            # --- Render Layers ---
            # 1. Hand Skeleton Mesh (Optional)
            if show_skeleton and landmarks_list:
                ui_renderer.draw_skeleton(frame, landmarks_list)

            # 2. Continuous Trajectory Lines
            active_trajectories = trajectory_tracker.get_active_trajectories()
            ui_renderer.draw_trajectories(frame, active_trajectories, trajectory_tracker.fade_duration_sec, stroke_width)

            # 3. Distance Warning Banner
            ui_renderer.draw_warning_banner(frame, evaluation)

            # 4. Status HUD & Shortcuts Legend
            target_name = "Fingertip" if trajectory_tracker.tracking_target == TrackingTarget.INDEX_FINGER_TIP else ("Palm" if trajectory_tracker.tracking_target == TrackingTarget.PALM_CENTER else "Wrist")
            ui_renderer.draw_hud(
                frame,
                fps=fps,
                inference_ms=inference_ms,
                hands_count=hands_count,
                evaluation=evaluation,
                tracking_target_name=target_name,
                show_skeleton=show_skeleton,
                show_controls=show_controls
            )

            cv2.imshow(window_name, frame)

            # Keyboard Input Handling
            key = cv2.waitKey(1) & 0xFF
            if key == ord('q') or key == 27:  # 'q' or Esc
                print("Exiting application...")
                break
            elif key == ord('c') or key == ord('C'):
                new_theme = ui_renderer.cycle_color_theme()
                print(f"[+] Switched Trajectory Theme: {new_theme}")
            elif key == ord('t') or key == ord('T'):
                if trajectory_tracker.tracking_target == TrackingTarget.INDEX_FINGER_TIP:
                    trajectory_tracker.tracking_target = TrackingTarget.PALM_CENTER
                elif trajectory_tracker.tracking_target == TrackingTarget.PALM_CENTER:
                    trajectory_tracker.tracking_target = TrackingTarget.WRIST
                else:
                    trajectory_tracker.tracking_target = TrackingTarget.INDEX_FINGER_TIP
                trajectory_tracker.clear_all()
                print(f"[+] Switched Tracking Target: {trajectory_tracker.tracking_target.value}")
            elif key == ord('k') or key == ord('K'):
                show_skeleton = not show_skeleton
                print(f"[+] Skeleton Mesh: {'ON' if show_skeleton else 'OFF'}")
            elif key == ord('r') or key == ord('R'):
                trajectory_tracker.clear_all()
                print("[+] Reset Trajectory Trail")
            elif key == ord('s') or key == ord('S'):
                show_controls = not show_controls
            elif key == ord('w') or key == ord('W'):
                # Cycle distance warning sensitivity
                distance_calculator.warning_threshold_ratio = 0.28 if distance_calculator.warning_threshold_ratio >= 0.48 else distance_calculator.warning_threshold_ratio + 0.10
                print(f"[+] Warning Threshold: {distance_calculator.warning_threshold_ratio:.2f}")

    finally:
        cap.release()
        landmarker.close()
        cv2.destroyAllWindows()
        print("[+] Cleanup complete. Windows App closed.")

if __name__ == "__main__":
    main()
