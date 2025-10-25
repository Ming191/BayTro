"""
Chatbot Router for GraphRAG API
"""
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field
from typing import List, Dict, Any
from services.graphrag_service_improved import ImprovedGraphRAGService
import logging

logger = logging.getLogger(__name__)

router = APIRouter()

# Initialize improved GraphRAG service (singleton)
graphrag_service = ImprovedGraphRAGService()


# Request/Response Models
class ChatRequest(BaseModel):
    question: str = Field(..., description="User's question in Vietnamese")
    expand_depth: int = Field(default=1, ge=0, le=5, description="Graph expansion depth")
    top_k: int = Field(default=5, ge=1, le=20, description="Number of nodes to retrieve")


class ContextNode(BaseModel):
    id: str
    content: str
    type: str


class ChatResponse(BaseModel):
    answer: str
    context: List[ContextNode]


class SearchRequest(BaseModel):
    query: str = Field(..., description="Search query")
    top_k: int = Field(default=5, ge=1, le=20, description="Number of results")


class SearchResult(BaseModel):
    id: str
    content: str
    type: str
    distance: float


class SearchResponse(BaseModel):
    results: List[SearchResult]


# Endpoints
@router.post("/query", response_model=ChatResponse)
async def query_chatbot(request: ChatRequest):
    """
    Query the housing law chatbot
    
    - **question**: User's question in Vietnamese
    - **expand_depth**: How many hops to expand in the knowledge graph (0-5)
    - **top_k**: Number of initial nodes to retrieve (1-20)
    """
    try:
        logger.info(f"Chatbot query: {request.question[:50]}...")
        logger.info(f"Request params: expand_depth={request.expand_depth}, top_k={request.top_k}")
        
        result = await graphrag_service.query(
            question=request.question,
            expand_depth=request.expand_depth,
            top_k=request.top_k
        )
        
        logger.info(f"Query result keys: {list(result.keys())}")
        logger.info(f"Answer length: {len(result.get('answer', ''))}")
        logger.info(f"Context items: {len(result.get('context', []))}")
        
        return ChatResponse(
            answer=result['answer'],
            context=[ContextNode(**node) for node in result['context']]
        )
    
    except Exception as e:
        logger.error(f"Error in chatbot query: {str(e)}", exc_info=True)
        logger.error(f"Request was: {request}")
        raise HTTPException(
            status_code=500,
            detail=f"Failed to process query: {str(e)}"
        )


@router.post("/search", response_model=SearchResponse)
async def search_knowledge(request: SearchRequest):
    """
    Semantic search in the housing law knowledge base
    
    - **query**: Search query in Vietnamese
    - **top_k**: Number of results to return (1-20)
    """
    try:
        logger.info(f"Search query: {request.query[:50]}...")
        
        results = graphrag_service.semantic_search(
            query=request.query,
            top_k=request.top_k
        )
        
        return SearchResponse(
            results=[SearchResult(**result) for result in results]
        )
    
    except Exception as e:
        logger.error(f"Error in search: {str(e)}", exc_info=True)
        raise HTTPException(
            status_code=500,
            detail=f"Search failed: {str(e)}"
        )


@router.get("/health")
async def chatbot_health():
    """Health check for chatbot service"""
    try:
        count = graphrag_service.collection.count()
        return {
            "status": "healthy",
            "collection": graphrag_service.collection_name,
            "node_count": count
        }
    except Exception as e:
        logger.error(f"Health check failed: {str(e)}")
        raise HTTPException(
            status_code=503,
            detail=f"Service unhealthy: {str(e)}"
        )
