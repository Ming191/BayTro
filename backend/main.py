"""
BayTro Backend API - Monorepo
Unified backend for meter reading and GraphRAG chatbot
"""
from dotenv import load_dotenv
from pathlib import Path
import os
load_dotenv(dotenv_path=Path(__file__).parent / ".env", override=True)

import logging

# Configure logging FIRST, before any imports
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    force=True  # Override any existing logging configuration
)

logger = logging.getLogger(__name__)

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from routers import meter, chatbot

# Create FastAPI app
app = FastAPI(
    title="BayTro Backend API",
    description="Unified backend for BayTro app - Meter reading and AI chatbot",
    version="1.0.0"
)

# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=[
        "http://localhost:8080",
        "http://10.0.2.2:5000",
        "http://127.0.0.1:8080",
    ],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Include routers
app.include_router(
    meter.router,
    prefix="/api/meter",
    tags=["meter"]
)

app.include_router(
    chatbot.router,
    prefix="/api/chatbot",
    tags=["chatbot"]
)

# Root endpoint
@app.get("/")
async def root():
    return {
        "message": "BayTro Backend API",
        "version": "1.0.0",
        "services": ["meter", "chatbot"]
    }

# Health check
@app.get("/health")
async def health():
    return {
        "status": "ok",
        "services": {
            "meter": "available",
            "chatbot": "available"
        }
    }

# Legacy endpoint for backward compatibility
@app.post("/predict_meter")
async def predict_meter_legacy(file):
    """Legacy endpoint - redirects to /api/meter/predict"""
    from routers.meter import predict_meter
    logger.warning("Using legacy endpoint /predict_meter, please use /api/meter/predict")
    return await predict_meter(file)

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "main:app",
        host="0.0.0.0",
        port=5000,
        reload=True
    )
