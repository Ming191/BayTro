"""
Configuration file for GraphRAG server
"""
import os

# OpenAI API Configuration
OPENAI_API_KEY = os.getenv('OPENAI_API_KEY', '')
OPENAI_MODEL = os.getenv('OPENAI_MODEL', 'gpt-4o-mini')
OPENAI_EMBEDDING_MODEL = os.getenv('OPENAI_EMBEDDING_MODEL', 'text-embedding-3-large')

# Server Configuration
HOST = '0.0.0.0'
PORT = 5000
DEBUG = True

# GraphRAG Configuration
JSON_DATA_PATH = 'data/luatnhao_structuredv33.converted.json'
CHROMADB_PATH = 'chromadb_law_improved'
COLLECTION_NAME = 'law_nodes_improved'

# API Configuration
DEFAULT_TOP_K = 5
DEFAULT_EXPAND_DEPTH = 2
MAX_TOKENS = 1500
TEMPERATURE = 0.3
