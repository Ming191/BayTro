"""
Enhanced Chatbot Router with conversation memory and improved endpoints
"""
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field
from typing import List, Dict, Any, Optional
from services.neo4j_graphrag_service import Neo4jGraphRAGService
from services.role_validator_service import RoleValidatorService
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

try:
    role_validator = RoleValidatorService()
    logger.info("Role Validator service initialized successfully")
except Exception as e:
    logger.error(f"Failed to initialize Role Validator service: {e}")
    role_validator = None


# In-memory conversation store (in production, use Redis or database)
conversation_store: Dict[str, List[Dict[str, str]]] = {}


# Request/Response Models
class ChatRequest(BaseModel):
    question: str = Field(..., description="User's question in Vietnamese")
    session_id: Optional[str] = Field(None, description="Session ID for conversation history")
    use_history: bool = Field(default=False, description="Whether to use conversation history")
    user_role: str = Field(..., description="User role: 'landlord' or 'tenant'")


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
    role_validation: Optional[Dict[str, Any]] = Field(None, description="Role validation result")


class HealthResponse(BaseModel):
    status: str
    database: str
    node_count: Optional[int] = None
    vector_index: str


# Endpoints
@router.post("/query", response_model=ChatResponse)
async def query_chatbot(request: ChatRequest):
    """
    Query the housing law chatbot with advanced GraphRAG and role-based validation

    - **question**: User's question in Vietnamese
    - **session_id**: Optional session ID for conversation continuity
    - **use_history**: Whether to use conversation history for context
    - **user_role**: User role ('landlord' or 'tenant') for validation
    """
    if graphrag_service is None:
        logger.error("GraphRAG service unavailable - check Neo4j connection")
        raise HTTPException(
            status_code=503,
            detail={
                "error": "GraphRAG service is not available",
                "message": "Please ensure Neo4j is running and properly configured",
                "suggestion": "Check NEO4J_URI, NEO4J_USERNAME, and NEO4J_PASSWORD in .env file"
            }
        )

    try:
        logger.info(f"Chatbot query from {request.user_role}: {request.question[:100]}...")

        # STEP 1: Validate role appropriateness FIRST
        validation_result = None
        if role_validator:
            try:
                validation_result = await role_validator.validate_question(
                    question=request.question,
                    user_role=request.user_role
                )
                logger.info(f"Role validation: is_valid={validation_result['is_valid']}, "
                           f"type={validation_result.get('question_type')}")

                # If question is not appropriate for this role, return early with guidance
                if not validation_result.get("is_valid", True):
                    guidance_response = role_validator.get_role_mismatch_response(
                        question=request.question,
                        user_role=request.user_role,
                        validation_result=validation_result
                    )

                    logger.info(f"Question blocked due to role mismatch. Returning guidance.")

                    return ChatResponse(
                        answer=guidance_response,
                        context=[],
                        metadata={
                            "role_validation": validation_result,
                            "blocked": True
                        },
                        session_id=request.session_id,
                        role_validation=validation_result
                    )
            except Exception as e:
                logger.warning(f"Role validation error (continuing anyway): {e}")
                # Continue processing if validation fails

        # STEP 2: Get conversation history if requested
        conversation_history = None
        if request.use_history and request.session_id:
            conversation_history = conversation_store.get(request.session_id, [])
            logger.info(f"Using conversation history with {len(conversation_history)} messages")

        # STEP 3: Query the GraphRAG service
        result = await graphrag_service.query(
            question=request.question,
            conversation_history=conversation_history,
            user_role=request.user_role  # Pass role for context-aware answering
        )

        # STEP 4: Update conversation history
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
            session_id=request.session_id,
            role_validation=validation_result
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


@router.get("/stats")
async def get_stats():
    """
    Get chatbot service statistics for monitoring
    """
    if graphrag_service is None:
        raise HTTPException(status_code=503, detail="Service not available")
    
    try:
        stats = graphrag_service.get_stats()
        return {
            "status": "ok",
            "database": stats,
            "conversation_sessions": len(conversation_store)
        }
    except Exception as e:
        logger.error(f"Failed to get stats: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/admin/rebuild-index")
async def rebuild_index():
    """
    Admin endpoint to rebuild the graph and vector index
    WARNING: This will clear and rebuild all data
    """
    if graphrag_service is None:
        raise HTTPException(status_code=503, detail="Service not available")

    try:
        logger.warning("Starting index rebuild - this will delete all existing data")
        
        # Clear existing data using raw driver
        graphrag_service._execute_query("MATCH (n) DETACH DELETE n")
        logger.info("Existing data cleared")

        # Reinitialize
        graphrag_service._initialize_graph()
        graphrag_service._initialize_neo4j_vector_index()
        
        logger.info("Index rebuild completed successfully")

        return {
            "message": "Index rebuilt successfully",
            "status": "completed",
            "stats": graphrag_service.get_stats()
        }
    except Exception as e:
        logger.error(f"Failed to rebuild index: {e}")
        raise HTTPException(
            status_code=500,
            detail=f"Rebuild failed: {str(e)}"
        )
