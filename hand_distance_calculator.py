import math
from enum import Enum

class ProximityState(Enum):
    SAFE = "SAFE"
    WARNING = "WARNING"
    TOO_CLOSE = "TOO_CLOSE"

class DistanceEvaluation:
    def __init__(self, proximity_state: ProximityState, closeness_ratio: float, estimated_distance_cm: int, bbox_area_ratio: float):
        self.proximity_state = proximity_state
        self.closeness_ratio = closeness_ratio
        self.estimated_distance_cm = estimated_distance_cm
        self.bbox_area_ratio = bbox_area_ratio

class HandDistanceCalculator:
    def __init__(self, warning_threshold_ratio: float = 0.38, too_close_threshold_ratio: float = 0.52):
        self.warning_threshold_ratio = warning_threshold_ratio
        self.too_close_threshold_ratio = too_close_threshold_ratio

    def evaluate_hand_proximity(self, landmarks) -> DistanceEvaluation:
        if not landmarks:
            return DistanceEvaluation(
                proximity_state=ProximityState.SAFE,
                closeness_ratio=0.0,
                estimated_distance_cm=65,
                bbox_area_ratio=0.0
            )

        min_x = min(lm.x for lm in landmarks)
        max_x = max(lm.x for lm in landmarks)
        min_y = min(lm.y for lm in landmarks)
        max_y = max(lm.y for lm in landmarks)

        box_width = max_x - min_x
        box_height = max_y - min_y
        bbox_area_ratio = box_width * box_height

        # Hand span length (Wrist landmark 0 to Middle Finger MCP landmark 9)
        wrist = landmarks[0]
        middle_mcp = landmarks[9]
        dx = middle_mcp.x - wrist.x
        dy = middle_mcp.y - wrist.y
        dz = getattr(middle_mcp, 'z', 0.0) - getattr(wrist, 'z', 0.0)
        hand_span_3d = math.sqrt(dx * dx + dy * dy + dz * dz)

        # Combined scale indicator
        combined_scale = (box_width * 0.4) + (box_height * 0.4) + (hand_span_3d * 0.6)

        # Map combined scale to a normalized 0.0 .. 1.0 closeness ratio
        min_expected_scale = 0.12
        max_expected_scale = 0.65
        closeness_ratio = max(0.0, min(1.0, (combined_scale - min_expected_scale) / (max_expected_scale - min_expected_scale)))

        # Estimated distance in cm (camera perspective mapping)
        estimated_distance_cm = int(max(12, min(80, 75 - (closeness_ratio * 60))))

        if closeness_ratio >= self.too_close_threshold_ratio:
            state = ProximityState.TOO_CLOSE
        elif closeness_ratio >= self.warning_threshold_ratio:
            state = ProximityState.WARNING
        else:
            state = ProximityState.SAFE

        return DistanceEvaluation(
            proximity_state=state,
            closeness_ratio=closeness_ratio,
            estimated_distance_cm=estimated_distance_cm,
            bbox_area_ratio=bbox_area_ratio
        )
