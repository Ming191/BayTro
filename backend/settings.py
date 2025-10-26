"""
Production configuration cho GraphRAG FastAPI server
"""

import os
from typing import List

class Settings:
    # Server Configuration
    HOST: str = os.getenv("HOST", "0.0.0.0")
    PORT: int = int(os.getenv("PORT", "5000"))
    DEBUG: bool = os.getenv("DEBUG", "false").lower() == "true"
    
    # API Configuration
    API_TITLE: str = "GraphRAG API"
    API_DESCRIPTION: str = "API cho trợ lý AI tư vấn pháp luật nhà ở Việt Nam"
    API_VERSION: str = "1.0.0"
    
    # CORS Configuration
    CORS_ORIGINS: List[str] = [
        "http://localhost:3000",  # React dev server
        "http://localhost:8080",  # Android emulator
        "http://10.0.2.2:5000",   # Android emulator localhost
    ]
    
    # OpenAI Configuration
    OPENAI_API_KEY: str = os.getenv("OPENAI_API_KEY", "")
    OPENAI_MODEL: str = os.getenv("OPENAI_MODEL", "gpt-4o-mini")
    OPENAI_EMBEDDING_MODEL: str = os.getenv("OPENAI_EMBEDDING_MODEL", "text-embedding-3-large")
    
    # GraphRAG Configuration
    JSON_DATA_PATH: str = os.getenv("JSON_DATA_PATH", "data/luatnhao_structuredv33.converted.json")
    CHROMADB_PATH: str = os.getenv("CHROMADB_PATH", "chromadb_law_v2")
    COLLECTION_NAME: str = os.getenv("COLLECTION_NAME", "law_nodes_improved")
    
    # API Limits
    DEFAULT_TOP_K: int = int(os.getenv("DEFAULT_TOP_K", "5"))
    MAX_TOP_K: int = int(os.getenv("MAX_TOP_K", "20"))
    DEFAULT_EXPAND_DEPTH: int = int(os.getenv("DEFAULT_EXPAND_DEPTH", "1"))
    MAX_EXPAND_DEPTH: int = int(os.getenv("MAX_EXPAND_DEPTH", "5"))
    MAX_TOKENS: int = int(os.getenv("MAX_TOKENS", "1500"))
    TEMPERATURE: float = float(os.getenv("TEMPERATURE", "0.3"))
    
    # Logging Configuration
    LOG_LEVEL: str = os.getenv("LOG_LEVEL", "INFO")
    
    # Security
    SECRET_KEY: str = os.getenv("SECRET_KEY", "your-secret-key-here")
    
    @classmethod
    def validate(cls):
        """Validate required settings"""
        if not cls.OPENAI_API_KEY:
            raise ValueError("OPENAI_API_KEY is required")
        
        if not os.path.exists(cls.JSON_DATA_PATH):
            raise FileNotFoundError(f"Data file not found: {cls.JSON_DATA_PATH}")
        
        return True

# Global settings instance
settings = Settings()
