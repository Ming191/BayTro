from dotenv import load_dotenv
from pathlib import Path
import os
load_dotenv(dotenv_path=Path(__file__).parent / ".env", override=True)

import logging

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    force=True
)

logger = logging.getLogger(__name__)

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from routers import meter, chatbot, evaluation

# Create FastAPI app
app = FastAPI(
    title="BayTro Backend API - Enhanced",
    description="Unified backend with Neo4j-powered GraphRAG chatbot",
    version="2.0.0"
)

# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=[
        "http://localhost:8080",
        "http://10.0.2.2:5000",
        "http://127.0.0.1:8080",
        "*"  # For development - restrict in production
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

app.include_router(
    evaluation.router,
    prefix="/api/evaluation",
    tags=["evaluation"]
)

# Root endpoint
@app.get("/")
async def root():
    return {
        "message": "BayTro Backend API - Enhanced",
        "version": "2.0.0",
        "services": {
            "meter": "YOLO-based meter reading",
            "chatbot": "Neo4j + LangGraph GraphRAG",
            "evaluation": "Chatbot quality evaluation"
        }
    }

# Health check
@app.get("/health")
async def health():
    return {
        "status": "ok",
        "version": "2.0.0",
        "services": {
            "meter": "available",
            "chatbot": "available",
            "evaluation": "available"
        }
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "main:app",
        host="0.0.0.0",
        port=8000,
        reload=True
    )
