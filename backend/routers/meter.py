"""
Meter Reading Router
"""
from fastapi import APIRouter, UploadFile, File, HTTPException
from pydantic import BaseModel
from typing import List, Dict, Any
from services.meter_service import MeterReadingService
import logging

logger = logging.getLogger(__name__)

router = APIRouter()

# Initialize meter reading service (singleton)
meter_service = MeterReadingService()


# Response Models
class Detection(BaseModel):
    label: str
    x: float
    conf: float


class MeterResponse(BaseModel):
    text: str
    detections: List[Detection]


# Endpoints
@router.post("/predict", response_model=MeterResponse)
async def predict_meter(file: UploadFile = File(...)):
    """
    Predict meter reading from uploaded image
    
    - **file**: Image file (JPEG, PNG, etc.)
    
    Returns the predicted meter reading as text and detailed detections
    """
    try:
        # Validate file type
        if not file.content_type.startswith('image/'):
            raise HTTPException(
                status_code=400,
                detail="File must be an image"
            )
        
        logger.info(f"Processing meter image: {file.filename}")

        # Read image bytes
        image_bytes = await file.read()
        
        # Predict using service
        result = await meter_service.predict(image_bytes)
        
        return MeterResponse(
            text=result['text'],
            detections=[Detection(**d) for d in result['detections']]
        )
    
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error in meter prediction: {str(e)}", exc_info=True)
        raise HTTPException(
            status_code=500,
            detail=f"Failed to process image: {str(e)}"
        )


@router.get("/health")
async def meter_health():
    """Health check for meter reading service"""
    return {
        "status": "healthy",
        "model": "YOLO",
        "model_path": "best.pt"
    }
