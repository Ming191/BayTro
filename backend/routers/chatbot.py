"""
Enhanced Chatbot Router with conversation memory and improved endpoints
"""
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field
from typing import List, Dict, Any, Optional
from services.neo4j_graphrag_service import Neo4jGraphRAGService
import logging

logger = logging.getLogger(__name__)

router = APIRouter()

# Initialize Neo4j GraphRAG service (singleton)
try:
    graphrag_service = Neo4jGraphRAGService()
    logger.info("Neo4j GraphRAG service initialized successfully")
except Exception as e:
    logger.error(f"Failed to initialize GraphRAG service: {e}")
    graphrag_service = None


# In-memory conversation store (in production, use Redis or database)
conversation_store: Dict[str, List[Dict[str, str]]] = {}


# Request/Response Models
class ChatRequest(BaseModel):
    question: str = Field(..., description="User's question in Vietnamese")
    session_id: Optional[str] = Field(None, description="Session ID for conversation history")
    use_history: bool = Field(default=False, description="Whether to use conversation history")


class ContextNode(BaseModel):
    id: str
    content: str
    type: str


class ChatResponse(BaseModel):
    answer: str = Field(..., description="Answer in plain text format")
    context: List[ContextNode]
    metadata: Optional[Dict[str, Any]] = None
    session_id: Optional[str] = None
    format: str = Field(default="text", description="Response format type")


class HealthResponse(BaseModel):
    status: str
    database: str
    node_count: Optional[int] = None
    vector_index: str


# Endpoints
@router.post("/query", response_model=ChatResponse)
async def query_chatbot(request: ChatRequest):
    """
    Query the housing law chatbot with advanced GraphRAG

    - **question**: User's question in Vietnamese
    - **session_id**: Optional session ID for conversation continuity
    - **use_history**: Whether to use conversation history for context
    """
    if graphrag_service is None:
        raise HTTPException(
            status_code=503,
            detail="GraphRAG service is not available. Please check Neo4j connection."
        )

    try:
        logger.info(f"Chatbot query: {request.question[:100]}...")

        # Get conversation history if requested
        conversation_history = None
        if request.use_history and request.session_id:
            conversation_history = conversation_store.get(request.session_id, [])
            logger.info(f"Using conversation history with {len(conversation_history)} messages")

        # Query the service
        result = await graphrag_service.query(
            question=request.question,
            conversation_history=conversation_history
        )

        # Update conversation history
        if request.session_id:
            if request.session_id not in conversation_store:
                conversation_store[request.session_id] = []

            conversation_store[request.session_id].append({
                "role": "user",
                "content": request.question
            })
            conversation_store[request.session_id].append({
                "role": "assistant",
                "content": result['answer']
            })

            # Keep only last 10 messages
            if len(conversation_store[request.session_id]) > 10:
                conversation_store[request.session_id] = conversation_store[request.session_id][-10:]

        logger.info(f"Query completed. Answer length: {len(result.get('answer', ''))}")
        logger.info(f"Context nodes: {len(result.get('context', []))}")

        return ChatResponse(
            answer=result['answer'],
            context=[ContextNode(**node) for node in result['context']],
            metadata=result.get('metadata'),
            session_id=request.session_id
        )

    except Exception as e:
        logger.error(f"Error in chatbot query: {str(e)}", exc_info=True)
        raise HTTPException(
            status_code=500,
            detail=f"Failed to process query: {str(e)}"
        )


@router.delete("/conversation/{session_id}")
async def clear_conversation(session_id: str):
    """Clear conversation history for a session"""
    if session_id in conversation_store:
        del conversation_store[session_id]
        return {"message": f"Conversation history cleared for session {session_id}"}
    return {"message": "Session not found"}


@router.get("/conversation/{session_id}")
async def get_conversation(session_id: str):
    """Get conversation history for a session"""
    history = conversation_store.get(session_id, [])
    return {
        "session_id": session_id,
        "message_count": len(history),
        "history": history
    }


@router.get("/health", response_model=HealthResponse)
async def chatbot_health():
    """Health check for chatbot service"""
    if graphrag_service is None:
        raise HTTPException(
            status_code=503,
            detail="GraphRAG service is not initialized"
        )

    try:
        # Query Neo4j for node count using the APOC-free method
        result = graphrag_service._execute_query("MATCH (n) RETURN count(n) as count")
        node_count = result[0]['count'] if result else 0

        return HealthResponse(
            status="healthy",
            database="Neo4j",
            node_count=node_count,
            vector_index="chroma_neo4j_vectors"
        )
    except Exception as e:
        logger.error(f"Health check failed: {str(e)}")
        raise HTTPException(
            status_code=503,
            detail=f"Service unhealthy: {str(e)}"
        )


@router.post("/admin/rebuild-index")
async def rebuild_index():
    """
    Admin endpoint to rebuild the graph and vector index
    WARNING: This will clear and rebuild all data
    """
    if graphrag_service is None:
        raise HTTPException(status_code=503, detail="Service not available")

    try:
        # Clear existing data using raw driver
        graphrag_service._execute_query("MATCH (n) DETACH DELETE n")

        # Reinitialize
        graphrag_service._initialize_graph()
        graphrag_service._initialize_vector_index()

        return {
            "message": "Index rebuilt successfully",
            "status": "completed"
        }
    except Exception as e:
        logger.error(f"Failed to rebuild index: {e}")
        raise HTTPException(
            status_code=500,
            detail=f"Rebuild failed: {str(e)}"
        )
