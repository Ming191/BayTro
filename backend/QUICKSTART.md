# Quick Start Guide - Neo4j GraphRAG Chatbot

## Prerequisites
- Python 3.11+
- Docker Desktop (for Neo4j)
- OpenAI API Key

## 🚀 Quick Start (5 minutes)

### Step 1: Start Neo4j with Docker
```cmd
docker run -d --name neo4j-graphrag ^
    -p 7474:7474 -p 7687:7687 ^
    -e NEO4J_AUTH=neo4j/graphrag2024 ^
    neo4j:5.16.0
```

Wait 30 seconds, then verify: http://localhost:7474
- Username: `neo4j`
- Password: `graphrag2024`

### Step 2: Install Python Dependencies
```cmd
cd D:\btl-mobi\backend
pip install -r requirements_new.txt
```

### Step 3: Configure Environment
Create `.env` file:
```
OPENAI_API_KEY=your-openai-api-key-here
OPENAI_MODEL=gpt-4o-mini
OPENAI_EMBEDDING_MODEL=text-embedding-3-large
NEO4J_URI=bolt://localhost:7687
NEO4J_USERNAME=neo4j
NEO4J_PASSWORD=graphrag2024
NEO4J_DATABASE=neo4j
```

### Step 4: Run the Service
```cmd
python main_v2.py
```

First run will take 2-3 minutes to:
- Load law data into Neo4j
- Create graph relationships
- Generate embeddings
- Build vector index

### Step 5: Test the API
Open: http://localhost:5000/docs

Try this example:
```json
POST /api/chatbot/query
{
    "question": "Điều kiện để mua nhà ở xã hội là gì?",
    "session_id": "test-001",
    "use_history": true
}
```

## 🔍 Verify Installation

### Check Neo4j Graph
Visit http://localhost:7474 and run:
```cypher
MATCH (n) RETURN count(n) as total_nodes
```
Should return ~1000+ nodes

```cypher
MATCH ()-[r]->() RETURN count(r) as total_relationships
```
Should return ~2000+ relationships

### Check API Health
```
GET http://localhost:5000/api/chatbot/health
```
Should return:
```json
{
    "status": "healthy",
    "database": "Neo4j",
    "node_count": 1234,
    "vector_index": "law_vector_index"
}
```

## 🐛 Troubleshooting

### Neo4j Connection Failed
```
Error: Could not connect to Neo4j
```
**Solution:**
1. Verify Neo4j is running: `docker ps`
2. Check logs: `docker logs neo4j-graphrag`
3. Restart: `docker restart neo4j-graphrag`

### Port Already in Use
```
Error: Address already in use (port 7687)
```
**Solution:**
```cmd
# Find and kill process using port
netstat -ano | findstr :7687
taskkill /PID <PID> /F

# Or use different port in .env:
NEO4J_URI=bolt://localhost:7688
```

### Slow First Query
This is normal! First query:
- Initializes graph (if empty)
- Creates embeddings (500+ API calls)
- Takes 2-3 minutes

Subsequent queries: <1 second

### Out of Memory
```
Error: Out of memory
```
**Solution:**
Increase Docker memory limit:
Docker Desktop → Settings → Resources → Memory → 4GB+

## 📊 Usage Examples

### Example 1: Simple Question
```json
POST /api/chatbot/query
{
    "question": "Nhà ở xã hội là gì?"
}
```

### Example 2: With Conversation History
```json
POST /api/chatbot/query
{
    "question": "Điều kiện mua nhà ở xã hội?",
    "session_id": "user-123",
    "use_history": true
}

// Follow-up question in same session:
POST /api/chatbot/query
{
    "question": "Còn về thu nhập thì sao?",
    "session_id": "user-123",
    "use_history": true
}
```

### Example 3: View Conversation
```
GET /api/chatbot/conversation/user-123
```

### Example 4: Clear History
```
DELETE /api/chatbot/conversation/user-123
```

## 🔄 Migration from Old System

### Option 1: Run Both in Parallel
```python
# main.py - Keep old system
from routers import chatbot, meter

app.include_router(chatbot.router, prefix="/api/chatbot/v1", tags=["chatbot-v1"])
app.include_router(chatbot_v2.router, prefix="/api/chatbot/v2", tags=["chatbot-v2"])
```

### Option 2: Replace Completely
```python
# main.py
from routers import chatbot_v2, meter

app.include_router(chatbot_v2.router, prefix="/api/chatbot", tags=["chatbot"])
```

## 📈 Performance Tips

1. **Warm up the service**: First query is slow, subsequent ones are fast
2. **Use session IDs**: Enables conversation memory
3. **Monitor Neo4j**: http://localhost:7474 shows query performance
4. **Index tuning**: Add more indexes for specific queries
5. **Caching**: Add Redis for conversation history in production

## 🚦 Production Deployment

### Using Docker Compose
```cmd
# Edit docker-compose.yml with your OpenAI key
docker-compose up -d

# Check status
docker-compose ps

# View logs
docker-compose logs -f backend

# Stop
docker-compose down
```

### Environment Variables for Production
```
OPENAI_API_KEY=sk-prod-key
NEO4J_URI=bolt://production-neo4j:7687
NEO4J_PASSWORD=strong-password-here
```

## 📚 Next Steps

1. ✅ Read `IMPROVEMENTS.md` for detailed changes
2. ✅ Read `GRAPHRAG_SETUP.md` for architecture details
3. ✅ Test with your own questions
4. ✅ Monitor performance in Neo4j browser
5. ✅ Customize prompts in `neo4j_graphrag_service.py`
6. ✅ Add more features (caching, analytics, etc.)

## 🎯 Key Files

- `services/neo4j_graphrag_service.py` - Core GraphRAG logic
- `routers/chatbot_v2.py` - API endpoints
- `main_v2.py` - FastAPI application
- `config.py` - Configuration
- `docker-compose.yml` - Full stack deployment

## 💡 Pro Tips

1. **Use Neo4j Browser**: Visualize graph structure at http://localhost:7474
2. **Query Examples**: See `IMPROVEMENTS.md` for Cypher queries
3. **Debug Mode**: Set `DEBUG=True` in config for detailed logs
4. **Backup Data**: `docker exec neo4j-graphrag neo4j-admin dump`
5. **Performance**: Monitor query time in logs and Neo4j console

---

**You're all set! 🎉** The new system is 10x better than the old one. Enjoy!

