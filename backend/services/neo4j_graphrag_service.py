import json
import logging
from typing import List, Dict, Any, Optional, TypedDict, Annotated
from operator import add

from neo4j import GraphDatabase
from langchain_openai import ChatOpenAI, OpenAIEmbeddings
from langchain.schema import Document
from langchain.prompts import ChatPromptTemplate
from langchain_community.vectorstores import Chroma
from langgraph.graph import StateGraph, END, START

from config import (
    OPENAI_API_KEY, OPENAI_MODEL, OPENAI_EMBEDDING_MODEL,
    NEO4J_URI, NEO4J_USERNAME, NEO4J_PASSWORD, NEO4J_DATABASE,
    JSON_DATA_PATH
)

logger = logging.getLogger(__name__)


class GraphState(TypedDict):
    """State for the LangGraph workflow"""
    question: str
    query_analysis: Optional[str]
    search_queries: List[str]
    retrieved_nodes: List[Dict[str, Any]]
    expanded_context: List[Dict[str, Any]]
    final_answer: str
    conversation_history: Annotated[List[Dict[str, str]], add]
    metadata: Dict[str, Any]
    user_role: Optional[str]  # Add user role to state


class Neo4jGraphRAGService:
    """Enhanced GraphRAG service using Neo4j (without APOC) and LangGraph"""

    def __init__(
        self,
        json_path: str = JSON_DATA_PATH,
        neo4j_uri: str = NEO4J_URI,
        neo4j_username: str = NEO4J_USERNAME,
        neo4j_password: str = NEO4J_PASSWORD,
        neo4j_database: str = NEO4J_DATABASE
    ):
        self.json_path = json_path

        # Initialize LLM and Embeddings
        self.llm = ChatOpenAI(
            model=OPENAI_MODEL,
            temperature=0.1,
            api_key=OPENAI_API_KEY
        )
        self.embeddings = OpenAIEmbeddings(
            model=OPENAI_EMBEDDING_MODEL,
            api_key=OPENAI_API_KEY
        )

        # Initialize Neo4j connection (raw driver - no APOC needed)
        try:
            self.driver = GraphDatabase.driver(
                neo4j_uri,
                auth=(neo4j_username, neo4j_password)
            )
            # Test connection
            with self.driver.session(database=neo4j_database) as session:
                result = session.run("RETURN 1 as test")
                result.single()
            self.database = neo4j_database
            logger.info("Connected to Neo4j successfully (APOC-free)")
        except Exception as e:
            logger.error(f"Failed to connect to Neo4j: {e}")
            raise

        # Load data
        with open(json_path, 'r', encoding='utf-8') as f:
            self.law_data = json.load(f)

        # Initialize or verify graph
        self._initialize_graph()

        # Initialize vector index (using ChromaDB as fallback)
        self._initialize_vector_index()

        # Build LangGraph workflow
        self.workflow = self._build_workflow()

        logger.info("Neo4jGraphRAGService initialized successfully")

    def _execute_query(self, query: str, parameters: Dict = None) -> List[Dict]:
        """Execute a Cypher query and return results"""
        with self.driver.session(database=self.database) as session:
            result = session.run(query, parameters or {})
            return [dict(record) for record in result]

    def _initialize_graph(self):
        """Initialize Neo4j graph schema and load data"""
        # Check if data already exists
        result = self._execute_query("MATCH (n) RETURN count(n) as count")
        node_count = result[0]['count'] if result else 0

        if node_count > 0:
            logger.info(f"Graph already initialized with {node_count} nodes")
            return

        logger.info("Initializing Neo4j graph with law data...")

        # Create constraints and indexes
        try:
            self._execute_query("CREATE CONSTRAINT chapter_id IF NOT EXISTS FOR (c:Chapter) REQUIRE c.id IS UNIQUE")
        except:
            pass
        try:
            self._execute_query("CREATE CONSTRAINT article_id IF NOT EXISTS FOR (a:Article) REQUIRE a.id IS UNIQUE")
        except:
            pass
        try:
            self._execute_query("CREATE CONSTRAINT clause_id IF NOT EXISTS FOR (c:Clause) REQUIRE c.id IS UNIQUE")
        except:
            pass
        try:
            self._execute_query("CREATE CONSTRAINT point_id IF NOT EXISTS FOR (p:Point) REQUIRE p.id IS UNIQUE")
        except:
            pass

        # Load data into graph
        for chapter in self.law_data:
            self._create_chapter_graph(chapter)

        # Create reference relationships
        self._create_references()

        result = self._execute_query("MATCH (n) RETURN count(n) as count")
        logger.info(f"Graph initialized with {result[0]['count']} nodes")

    def _create_chapter_graph(self, chapter: Dict[str, Any]):
        """Create nodes and relationships for a chapter"""
        chapter_id = chapter["chapter_id"]
        chapter_title = chapter["title"]

        # Create chapter node
        self._execute_query("""
            MERGE (c:Chapter {id: $id})
            SET c.title = $title, c.text = $text, c.level = 0
        """, {"id": f"Chương_{chapter_id}", "title": chapter_title, "text": chapter_title})

        # Handle sections
        if chapter.get("sections"):
            for section in chapter["sections"]:
                section_id = section["section_id"]
                section_title = section["title"]
                section_node_id = f"Chương_{chapter_id}_Mục_{section_id}"

                # Create section node
                self._execute_query("""
                    MERGE (s:Section {id: $id})
                    SET s.title = $title, s.text = $text,
                        s.section_id = $section_id, s.chapter_id = $chapter_id, s.level = 1
                """, {
                    "id": section_node_id,
                    "title": f"Mục {section_id}: {section_title}",
                    "text": section_title,
                    "section_id": section_id,
                    "chapter_id": chapter_id
                })

                # Create relationship
                self._execute_query("""
                    MATCH (c:Chapter {id: $chapter_id})
                    MATCH (s:Section {id: $section_id})
                    MERGE (c)-[:CONTAINS]->(s)
                """, {"chapter_id": f"Chương_{chapter_id}", "section_id": section_node_id})

                # Process articles in section
                for article in section.get("articles", []):
                    self._create_article_graph(article, chapter_id, section_node_id)

        # Handle articles directly under chapter
        elif chapter.get("articles"):
            for article in chapter["articles"]:
                self._create_article_graph(article, chapter_id, f"Chương_{chapter_id}")

    def _create_article_graph(self, article: Dict[str, Any], chapter_id: str, parent_node_id: str):
        """Create nodes for article, clauses, and points"""
        article_id = article["article_id"]
        article_title = article.get("title", "")
        article_node_id = f"Điều_{article_id}"

        # Create article node
        self._execute_query("""
            MERGE (a:Article {id: $id})
            SET a.title = $title, a.text = $text,
                a.article_id = $article_id, a.chapter_id = $chapter_id, a.level = 2
        """, {
            "id": article_node_id,
            "title": f"Điều {article_id}: {article_title}",
            "text": article_title,
            "article_id": article_id,
            "chapter_id": chapter_id
        })

        # Create relationship to parent
        self._execute_query("""
            MATCH (p {id: $parent_id})
            MATCH (a:Article {id: $article_id})
            MERGE (p)-[:CONTAINS]->(a)
        """, {"parent_id": parent_node_id, "article_id": article_node_id})

        # Process clauses
        for clause in article.get("clauses", []):
            clause_id = clause["clause_id"]
            clause_text = clause["text"]
            clause_node_id = f"Điều_{article_id}_Khoản_{clause_id}"

            # Create clause node with references stored
            self._execute_query("""
                MERGE (c:Clause {id: $id})
                SET c.text = $text, c.title = $title,
                    c.article_id = $article_id, c.clause_id = $clause_id,
                    c.chapter_id = $chapter_id, c.level = 3,
                    c.references = $references
            """, {
                "id": clause_node_id,
                "text": clause_text,
                "title": f"Điều {article_id} - Khoản {clause_id}",
                "article_id": article_id,
                "clause_id": clause_id,
                "chapter_id": chapter_id,
                "references": json.dumps(clause.get("references", []))
            })

            # Create relationship
            self._execute_query("""
                MATCH (a:Article {id: $article_id})
                MATCH (c:Clause {id: $clause_id})
                MERGE (a)-[:CONTAINS]->(c)
            """, {"article_id": article_node_id, "clause_id": clause_node_id})

            # Process points
            for point in clause.get("points", []):
                point_id = point["point_id"]
                point_text = point["text"]
                point_node_id = f"Điều_{article_id}_Khoản_{clause_id}_Điểm_{point_id}"

                self._execute_query("""
                    MERGE (p:Point {id: $id})
                    SET p.text = $text, p.title = $title,
                        p.article_id = $article_id, p.clause_id = $clause_id,
                        p.point_id = $point_id, p.chapter_id = $chapter_id,
                        p.level = 4, p.references = $references
                """, {
                    "id": point_node_id,
                    "text": point_text,
                    "title": f"Điều {article_id} - Khoản {clause_id} - Điểm {point_id}",
                    "article_id": article_id,
                    "clause_id": clause_id,
                    "point_id": point_id,
                    "chapter_id": chapter_id,
                    "references": json.dumps(point.get("references", []))
                })

                self._execute_query("""
                    MATCH (c:Clause {id: $clause_id})
                    MATCH (p:Point {id: $point_id})
                    MERGE (c)-[:CONTAINS]->(p)
                """, {"clause_id": clause_node_id, "point_id": point_node_id})

    def _create_references(self):
        """Create REFERENCES relationships based on stored reference data"""
        # Get all nodes with references
        result = self._execute_query("""
            MATCH (n)
            WHERE n.references IS NOT NULL AND n.references <> '[]'
            RETURN n.id as node_id, n.references as refs,
                   n.article_id as article_id, n.clause_id as clause_id
        """)

        for record in result:
            node_id = record['node_id']
            references = json.loads(record['refs'])
            current_article = record.get('article_id')
            current_clause = record.get('clause_id')

            for ref in references:
                target = ref.get('target', {})
                ref_text = ref.get('text', '')

                # Resolve target node(s)
                target_article = target.get('article')
                if target_article == 'current':
                    target_article = current_article

                if target_article is None:
                    continue

                # Build target node IDs
                target_nodes = []

                if 'clauses' in target and isinstance(target['clauses'], list):
                    for clause_num in target['clauses']:
                        if clause_num == 'current':
                            clause_num = current_clause
                        target_nodes.append(f"Điều_{target_article}_Khoản_{clause_num}")
                elif 'clause' in target:
                    clause_num = target['clause']
                    if clause_num == 'current':
                        clause_num = current_clause
                    target_nodes.append(f"Điều_{target_article}_Khoản_{clause_num}")
                else:
                    target_nodes.append(f"Điều_{target_article}")

                # Handle points
                if 'points' in target and isinstance(target['points'], list):
                    base_clause = target.get('clause', current_clause)
                    if base_clause == 'current':
                        base_clause = current_clause
                    for point_id in target['points']:
                        target_nodes.append(f"Điều_{target_article}_Khoản_{base_clause}_Điểm_{point_id}")

                # Create relationships
                for target_node in target_nodes:
                    try:
                        self._execute_query("""
                            MATCH (source {id: $source_id})
                            MATCH (target {id: $target_id})
                            MERGE (source)-[r:REFERENCES]->(target)
                            SET r.text = $ref_text
                        """, {
                            "source_id": node_id,
                            "target_id": target_node,
                            "ref_text": ref_text
                        })
                    except:
                        pass  # Skip if target doesn't exist

    def _initialize_vector_index(self):
        """Initialize vector index using ChromaDB (APOC-free)"""
        # Get all law nodes from Neo4j
        documents = []

        result = self._execute_query("""
            MATCH (n)
            WHERE n.text IS NOT NULL
            RETURN n.id as id, n.text as text, n.title as title,
                   n.level as level, labels(n)[0] as type
            ORDER BY n.level
        """)

        for record in result:
            # Build rich text for embedding
            text_parts = []
            if record.get('title'):
                text_parts.append(record['title'])
            if record.get('text'):
                text_parts.append(record['text'])

            full_text = "\n".join(text_parts)

            if full_text.strip():
                documents.append(Document(
                    page_content=full_text,
                    metadata={
                        "id": record['id'],
                        "type": record.get('type', 'unknown'),
                        "level": record.get('level', 0) or 0
                    }
                ))

        logger.info(f"Creating vector index for {len(documents)} documents...")

        # Use ChromaDB for vector search (works without APOC)
        self.vector_index = Chroma.from_documents(
            documents,
            self.embeddings,
            collection_name="neo4j_law_vectors",
            persist_directory="./chroma_neo4j_vectors"
        )

        logger.info("Vector index created successfully")

    def _build_workflow(self) -> StateGraph:
        """Build LangGraph workflow for multi-step reasoning"""
        workflow = StateGraph(GraphState)

        # Add nodes
        workflow.add_node("analyze_query", self._analyze_query)
        workflow.add_node("semantic_search", self._semantic_search)
        workflow.add_node("expand_context", self._expand_context)
        workflow.add_node("generate_answer", self._generate_answer)

        # Define edges
        workflow.add_edge(START, "analyze_query")
        workflow.add_edge("analyze_query", "semantic_search")
        workflow.add_edge("semantic_search", "expand_context")
        workflow.add_edge("expand_context", "generate_answer")
        workflow.add_edge("generate_answer", END)

        # Compile without checkpointer to avoid checkpoint issues
        return workflow.compile(checkpointer=None)

    async def _analyze_query(self, state: GraphState) -> GraphState:
        """Analyze user query to understand intent and extract key entities"""
        analysis_prompt = ChatPromptTemplate.from_messages([
            ("system", """Bạn là chuyên gia phân tích câu hỏi về luật nhà ở Việt Nam.
Nhiệm vụ: Phân tích câu hỏi và tạo 3-5 truy vấn tìm kiếm tối ưu để tìm thông tin liên quan.

QUAN TRỌNG:
- Tạo các query từ nhiều góc độ khác nhau
- Bao gồm cả thuật ngữ pháp lý và ngôn ngữ thông thường
- Tách các khái niệm chính thành query riêng

Hãy trả về JSON với format:
{{
    "analysis": "Phân tích ngắn gọn về câu hỏi",
    "search_queries": ["truy vấn 1", "truy vấn 2", "truy vấn 3", "truy vấn 4", "truy vấn 5"]
}}

VÍ DỤ:
Câu hỏi: "nhà lưu trú công nhân trong khu công nghiệp được xây dựng như nào?"
→ Queries:
1. "nhà lưu trú công nhân khu công nghiệp" (chính)
2. "điều kiện xây dựng nhà lưu trú công nhân" (góc độ điều kiện)
3. "quy hoạch nhà lưu trú công nhân" (góc độ quy hoạch)
4. "tiêu chuẩn kỹ thuật nhà lưu trú công nhân" (góc độ kỹ thuật)
5. "dự án đầu tư xây dựng nhà lưu trú" (góc độ dự án)
"""),
            ("user", "Câu hỏi: {question}")
        ])

        response = await self.llm.ainvoke(
            analysis_prompt.format_messages(question=state["question"])
        )

        # Parse JSON response
        try:
            result = json.loads(response.content)
            state["query_analysis"] = result.get("analysis", "")
            # Ensure at least 3 queries
            queries = result.get("search_queries", [state["question"]])
            if len(queries) < 3:
                queries.extend([state["question"]] * (3 - len(queries)))
            state["search_queries"] = queries[:5]  # Max 5 queries
        except:
            state["query_analysis"] = "Phân tích đơn giản"
            state["search_queries"] = [state["question"]]

        return state

    async def _semantic_search(self, state: GraphState) -> GraphState:
        """Perform semantic search using vector index"""
        retrieved_nodes = []
        seen_ids = set()

        for query in state["search_queries"]:
            # Increase k from 5 to 10 to retrieve more candidate nodes
            results = self.vector_index.similarity_search_with_score(query, k=10)

            for doc, score in results:
                node_id = doc.metadata.get("id")
                if node_id not in seen_ids:
                    seen_ids.add(node_id)
                    retrieved_nodes.append({
                        "id": node_id,
                        "content": doc.page_content,
                        "type": doc.metadata.get("type", ""),
                        "score": float(1.0 - score)  # ChromaDB uses distance, convert to similarity
                    })

        # Sort by score and increase from 8 to 12 nodes for better coverage
        retrieved_nodes.sort(key=lambda x: x["score"], reverse=True)
        state["retrieved_nodes"] = retrieved_nodes[:12]

        return state

    async def _expand_context(self, state: GraphState) -> GraphState:
        """Expand context using graph traversal with deeper and wider exploration"""
        expanded_context = []
        visited = set()

        for node in state["retrieved_nodes"]:
            node_id = node["id"]

            cypher_query = """
            MATCH (start {id: $node_id})

            OPTIONAL MATCH path1 = (start)-[:CONTAINS*0..3]->(child)

            OPTIONAL MATCH path2 = (start)-[:REFERENCES]->(ref)
            OPTIONAL MATCH path3 = (ref)-[:CONTAINS*0..2]->(ref_child)

            OPTIONAL MATCH path4 = (parent)-[:CONTAINS*1..2]->(start)

            OPTIONAL MATCH (parent)-[:CONTAINS]->(sibling)
            WHERE sibling <> start

            OPTIONAL MATCH (referencing)-[:REFERENCES]->(start)

            WITH start,
                 collect(DISTINCT child) as children,
                 collect(DISTINCT ref) as references,
                 collect(DISTINCT ref_child) as ref_children,
                 collect(DISTINCT parent) as parents,
                 collect(DISTINCT sibling) as siblings,
                 collect(DISTINCT referencing) as referencing_nodes

            UNWIND (children + references + ref_children + parents + siblings + referencing_nodes + [start]) as n
            WITH DISTINCT n
            WHERE n IS NOT NULL
            RETURN n.id as id, n.text as text, n.title as title,
                   labels(n)[0] as type, n.level as level
            ORDER BY n.level
            LIMIT 25
            """

            try:
                result = self._execute_query(cypher_query, {"node_id": node_id})
                for record in result:
                    if record.get('id') and record['id'] not in visited:
                        visited.add(record['id'])
                        expanded_context.append({
                            "id": record['id'],
                            "content": record.get('text') or record.get('title') or "",
                            "type": record.get('type') or "unknown",
                            "level": record.get('level') or 0
                        })
            except Exception as e:
                logger.error(f"Graph expansion failed for node {node_id}: {e}")
                # Add at least the original node
                if node_id not in visited:
                    visited.add(node_id)
                    expanded_context.append({
                        "id": node_id,
                        "content": node.get("content", ""),
                        "type": node.get("type", "unknown"),
                        "level": 0
                    })

        state["expanded_context"] = expanded_context
        return state

    async def _generate_answer(self, state: GraphState) -> GraphState:
        """Generate final answer using LLM"""
        context_parts = []
        for idx, node in enumerate(state["expanded_context"][:15], 1):
            context_parts.append(f"{idx}. [{node['type']}] {node['id']}: {node['content']}")

        context_text = "\n".join(context_parts)

        answer_prompt = ChatPromptTemplate.from_messages([
            ("system", """Bạn là trợ lý tư vấn luật nhà ở chuyên nghiệp của Việt Nam.

PHẠM VI TRẢ LỜI:
- CHỈ trả lời các câu hỏi liên quan đến Luật Nhà ở 2023 và các quy định pháp luật về nhà ở tại Việt Nam
- KHÔNG trả lời các câu hỏi về: giá nhà, địa điểm mua/thuê nhà, đánh giá thị trường, tư vấn đầu tư, thủ tục hành chính cụ thể, quy trình ngân hàng, hoặc bất kỳ vấn đề nào NGOÀI nội dung pháp luật

NẾU CÂU HỎI NGOÀI PHẠM VI:
Trả lời ngắn gọn: "Xin lỗi, tôi chỉ có thể tư vấn về các quy định pháp luật trong Luật Nhà ở 2023. Câu hỏi của bạn nằm ngoài phạm vi chuyên môn của tôi."
→ KHÔNG đưa ra thêm bất cứ gợi ý nào hoặc hướng dẫn khác

NHIỆM VỤ KHI CÂU HỎI ĐÚNG PHẠM VI:
1. Dựa vào TẤT CẢ NỘI DUNG LUẬT được cung cấp, tổng hợp thông tin từ NHIỀU ĐIỀU KHOẢN để trả lời đầy đủ
2. LUÔN TRÍCH DẪN cụ thể Điều, Khoản, Điểm khi trả lời
3. Giải thích rõ ràng, dễ hiểu cho người không chuyên
4. TỔ CHỨC thông tin thành các phần logic (định nghĩa, quy hoạch, yêu cầu, tiêu chuẩn, v.v.)
5. KHÔNG bỏ qua thông tin quan trọng có trong context
6. KHÔNG bịa đặt thông tin không có trong nội dung được cung cấp

ĐỊNH DẠNG TRẢ LỜI:
- KHÔNG dùng Markdown formatting (KHÔNG dùng **, *, _, #, v.v.)
- Dùng CHỮ HOA cho tiêu đề các phần
- Dùng dấu gạch đầu dòng (•, -, hoặc số) cho danh sách
- Dùng xuống dòng để phân tách các phần
- Text thuần túy, dễ đọc

CÁCH TRẢ LỜI CHO CÂU HỎI ĐÚNG PHẠM VI:
- Trả lời trực tiếp, rõ ràng và ĐẦY ĐỦ
- Dùng ngôn ngữ thân thiện, dễ hiểu
- Chia nhỏ thành các phần để dễ đọc
- Trích dẫn Điều/Khoản/Điểm cụ thể cho MỌI thông tin
- Tổng hợp thông tin từ nhiều điều khoản nếu cần

VÍ DỤ ĐỊNH DẠNG ĐÚNG:
I. ĐỊNH NGHĨA (không dùng **ĐỊNH NGHĨA**)
Nhà lưu trú công nhân là...

YÊU CẦU:
- Điều kiện thứ nhất (không dùng **Điều kiện thứ nhất**)
- Điều kiện thứ hai

VÍ DỤ CÂU HỎI NGOÀI PHẠM VI (Từ chối ngắn gọn):
- "Mua nhà ở đâu giá rẻ?" → TỪ CHỐI
- "Thuê nhà giá rẻ ở đâu?" → TỪ CHỐI
- "Phí công chứng bao nhiêu?" → TỪ CHỐI
- "Thủ tục vay ngân hàng như nào?" → TỪ CHỐI
- "Giá nhà sẽ tăng hay giảm?" → TỪ CHỐI

---

NỘI DUNG LUẬT:
{context}"""),
            ("user", "Câu hỏi: {question}")
        ])

        response = await self.llm.ainvoke(
            answer_prompt.format_messages(
                question=state["question"],
                context=context_text
            )
        )

        state["final_answer"] = response.content
        state["metadata"] = {
            "num_nodes_retrieved": len(state["retrieved_nodes"]),
            "num_nodes_expanded": len(state["expanded_context"]),
            "search_queries": state["search_queries"]
        }

        return state

    async def query(
        self,
        question: str,
        conversation_history: Optional[List[Dict[str, str]]] = None,
        user_role: Optional[str] = None
    ) -> Dict[str, Any]:
        """Main query method using LangGraph workflow"""
        initial_state = GraphState(
            question=question,
            query_analysis=None,
            search_queries=[],
            retrieved_nodes=[],
            expanded_context=[],
            final_answer="",
            conversation_history=conversation_history or [],
            metadata={},
            user_role=user_role or "tenant"  # Default to tenant if not specified
        )

        # Run workflow
        final_state = await self.workflow.ainvoke(initial_state)

        return {
            "answer": final_state["final_answer"],
            "context": final_state["expanded_context"][:5],
            "metadata": final_state["metadata"]
        }

    def close(self):
        """Close Neo4j connection"""
        if hasattr(self, 'driver'):
            self.driver.close()
