# Neo4j GraphRAG Chatbot - Setup Guide

## Architecture Improvements

### What Changed:

1. **Neo4j Database** (replaced NetworkX + ChromaDB)
   - Native graph database for efficient relationship traversal
   - Built-in vector search with indexes
   - Cypher query language for complex graph queries
   - Better scalability and performance

2. **LangGraph Workflow** (replaced simple async function)
   - Multi-step reasoning pipeline:
     - Query Analysis: Understands intent and generates optimal search queries
     - Semantic Search: Vector-based retrieval from Neo4j
     - Context Expansion: Graph traversal to get related nodes
     - Answer Generation: LLM synthesis with citations
   - State management for conversation flow
   - Extensible for adding more agents

3. **Conversation Memory**
   - Session-based conversation history
   - Context-aware responses
   - Easy to extend with Redis or database storage

4. **Better Code Organization**
   - Async/await throughout for better performance
   - Proper error handling and logging
   - Type hints with TypedDict for state management
   - Separation of concerns

## Installation

### 1. Install Neo4j

**Windows:**
```cmd
# Download from https://neo4j.com/download/
# Or use Docker:
docker pull neo4j:latest
docker run -d --name neo4j ^
    -p 7474:7474 -p 7687:7687 ^
    -e NEO4J_AUTH=neo4j/minh2012 ^
    neo4j:latest
```

**Verify Neo4j is running:**
- Open http://localhost:7474 in browser
- Login with neo4j/your-password

### 2. Install Python Dependencies

```cmd
cd D:\btl-mobi\backend
pip install -r requirements_new.txt
```

### 3. Configure Environment Variables

Create `.env` file:
```cmd
copy .env.example .env
```

Edit `.env` with your credentials:
```
OPENAI_API_KEY=sk-your-actual-key
NEO4J_PASSWORD=your-neo4j-password
```

### 4. Run the Service

```cmd
python main_v2.py
```

Or update existing main.py to use new router:
```python
from routers import chatbot_v2

app.include_router(
    chatbot_v2.router,
    prefix="/api/chatbot",
    tags=["chatbot"]
)
```

## API Endpoints

### 1. Query Chatbot (with conversation memory)
```
POST /api/chatbot/query
{
    "question": "Điều kiện để được mua nhà ở xã hội là gì?",
    "session_id": "user-123",
    "use_history": true
}
```

### 2. Get Conversation History
```
GET /api/chatbot/conversation/{session_id}
```

### 3. Clear Conversation
```
DELETE /api/chatbot/conversation/{session_id}
```

### 4. Health Check
```
GET /api/chatbot/health
```

### 5. Rebuild Index (Admin)
```
POST /api/chatbot/admin/rebuild-index
```

## Key Improvements Explained

### 1. Query Analysis
Instead of directly searching, the system now:
- Analyzes the question to understand intent
- Generates multiple optimized search queries
- Handles synonyms and related concepts

### 2. Graph Traversal
Neo4j enables efficient traversal:
```cypher
MATCH (start {id: 'Điều_10'})
CALL apoc.path.subgraphAll(start, {
    relationshipFilter: "CONTAINS>|<REFERENCES",
    maxLevel: 2
})
YIELD nodes
```

### 3. Vector + Graph Hybrid
- Vector search finds semantically similar content
- Graph traversal expands to related legal provisions
- Best of both worlds for legal RAG

### 4. Structured State Management
```python
class GraphState(TypedDict):
    question: str
    query_analysis: Optional[str]
    search_queries: List[str]
    retrieved_nodes: List[Dict]
    expanded_context: List[Dict]
    final_answer: str
    conversation_history: List[Dict]
    metadata: Dict
```

## Performance Comparison

| Feature | Old (ChromaDB + NetworkX) | New (Neo4j + LangGraph) |
|---------|---------------------------|-------------------------|
| Graph Queries | O(n) traversal | O(log n) indexed |
| Vector Search | Separate DB | Integrated |
| Conversation | None | Built-in |
| Scalability | Limited | High |
| Query Planning | None | Multi-step |

## Next Steps

1. **Add APOC Procedures to Neo4j** (for advanced graph algorithms):
```cmd
# In Neo4j Docker:
docker exec -it neo4j bash
# Add to neo4j.conf: dbms.security.procedures.unrestricted=apoc.*
```

2. **Add Caching** (Redis for conversation history)

3. **Add Monitoring** (track query performance, user satisfaction)

4. **Fine-tune Prompts** (based on user feedback)

5. **Add More Agents** (fact-checker, citation validator, etc.)

## Troubleshooting

**Neo4j Connection Error:**
- Verify Neo4j is running: http://localhost:7474
- Check credentials in .env
- Ensure port 7687 is not blocked

**Vector Index Issues:**
- Rebuild index: POST /api/chatbot/admin/rebuild-index
- Check Neo4j logs for errors

**Slow Queries:**
- Check Neo4j query performance in browser
- Add more indexes if needed
- Reduce graph traversal depth

## Migration from Old Service

To migrate gradually:
1. Keep old service running
2. Add new router with different prefix (e.g., /api/chatbot/v2)
3. Test thoroughly
4. Switch frontend to new endpoints
5. Deprecate old service

