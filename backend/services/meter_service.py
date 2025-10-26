"""
Meter Reading Service using YOLO
"""
import cv2
import numpy as np
from ultralytics import YOLO
from typing import List, Dict, Any
import logging

logger = logging.getLogger(__name__)


class MeterReadingService:
    """Service class for meter reading using YOLO model"""
    
    def __init__(self, model_path: str = "best.pt"):
        """Initialize YOLO model"""
        self.model = YOLO(model_path)
        logger.info(f"Loaded YOLO model from {model_path}")
    
    async def predict(self, image_bytes: bytes) -> Dict[str, Any]:
        """
        Predict meter reading from image
        
        Args:
            image_bytes: Image data as bytes
            
        Returns:
            Dict with 'text' (predicted reading) and 'detections' (detailed results)
        """
        try:
            # Decode image
            nparr = np.frombuffer(image_bytes, np.uint8)
            img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
            
            if img is None:
                raise ValueError("Failed to decode image")
            
            # Preprocess image
            gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
            processed = cv2.cvtColor(gray, cv2.COLOR_GRAY2RGB)
            
            # Run YOLO prediction
            results = self.model.predict(processed)
            
            # Extract detections
            detections = []
            for r in results:
                for box in r.boxes:
                    x1 = float(box.xyxy[0][0])  # X coordinate (left)
                    label = self.model.names[int(box.cls)]
                    conf = float(box.conf)
                    detections.append({
                        "label": label,
                        "x": x1,
                        "conf": conf
                    })
            
            # Sort by X coordinate (left to right)
            detections = sorted(detections, key=lambda d: d["x"])
            
            # Combine labels to form text result
            text_result = " ".join([d["label"] for d in detections])
            
            logger.info(f"Predicted meter reading: {text_result}")
            
            return {
                "text": text_result,
                "detections": detections
            }
            
        except Exception as e:
            logger.error(f"Error in meter prediction: {str(e)}", exc_info=True)
            raise
