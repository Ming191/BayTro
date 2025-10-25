#!/usr/bin/env python3
"""
GraphRAG REST API Server với FastAPI
Tích hợp GraphRAG vào app Android thông qua REST API
"""

import os
import json
import networkx as nx
from typing import List, Dict, Any, Tuple, Optional
import numpy as np
from collections import defaultdict
import chromadb
from openai import OpenAI
from fastapi import FastAPI, HTTPException, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from pydantic import BaseModel, Field
import logging
import uvicorn
from dotenv import load_dotenv

# Load environment variables from .env file
load_dotenv()

# Thiết lập logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Pydantic Models
class ChatQueryRequest(BaseModel):
    question: str = Field(..., description="Câu hỏi về pháp luật", min_length=1)
    k: int = Field(default=5, description="Số lượng kết quả semantic search", ge=1, le=20)
    expand_depth: int = Field(default=2, description="Độ sâu mở rộng graph context", ge=1, le=5)

class ChatQueryResponse(BaseModel):
    answer: str = Field(..., description="Câu trả lời từ AI")
    context: str = Field(..., description="Nội dung luật tham khảo")
    question: str = Field(..., description="Câu hỏi gốc")

class SearchRequest(BaseModel):
    query: str = Field(..., description="Từ khóa tìm kiếm", min_length=1)
    top_k: int = Field(default=5, description="Số lượng kết quả trả về", ge=1, le=20)

class SearchResult(BaseModel):
    id: str = Field(..., description="ID của node")
    metadata: Dict[str, Any] = Field(..., description="Metadata của node")
    document: str = Field(..., description="Nội dung document")
    score: float = Field(..., description="Điểm số relevance")
    distance: float = Field(..., description="Khoảng cách embedding")

class SearchResponse(BaseModel):
    results: List[SearchResult] = Field(..., description="Danh sách kết quả tìm kiếm")
    query: str = Field(..., description="Từ khóa tìm kiếm")

class HealthResponse(BaseModel):
    status: str = Field(..., description="Trạng thái server")
    message: str = Field(..., description="Thông báo")
    rag_initialized: bool = Field(..., description="GraphRAG đã khởi tạo")

class ErrorResponse(BaseModel):
    error: str = Field(..., description="Thông báo lỗi")
    detail: Optional[str] = Field(None, description="Chi tiết lỗi")

class ImprovedGraphRAG:
    def __init__(self, json_path: str, openai_api_key: str):
        self.client = OpenAI(api_key=openai_api_key)
        self.chroma_client = chromadb.PersistentClient(path="chromadb_law_improved")
        self.collection = self.chroma_client.get_or_create_collection("law_nodes_improved")

        # Load và parse data
        with open(json_path, "r", encoding="utf-8") as f:
            self.law_data = json.load(f)

        # Build improved graph
        self.G = self.build_improved_graph()
        logger.info(f"✅ Improved graph built with {len(self.G.nodes())} nodes and {len(self.G.edges())} edges.")

        # Create embeddings
        self.create_embeddings()

    def build_improved_graph(self) -> nx.DiGraph:
        """Xây dựng graph với cải tiến về references và hierarchy"""
        G = nx.DiGraph()

        # Tạo mapping để resolve references
        node_mapping = {}

        # Bước 1: Tạo tất cả nodes với metadata đầy đủ
        for chapter in self.law_data:
            chapter_id = chapter["chapter_id"]
            chapter_title = chapter["title"]

            # Chapter node
            chapter_node_id = f"Chương_{chapter_id}"
            G.add_node(chapter_node_id,
                      text=chapter_title,
                      title=chapter_title,
                      type="chapter",
                      chapter_id=chapter_id,
                      level=0)
            node_mapping[f"chương_{chapter_id.lower()}"] = chapter_node_id

            # Handle sections within the chapter
            if chapter.get("sections"):
                for section in chapter["sections"]:
                    section_id = section["section_id"]
                    section_title = section["title"]

                    # Section node
                    section_node_id = f"{chapter_node_id}_Mục_{section_id}"
                    G.add_node(section_node_id,
                              text=section_title,
                              title=f"Mục {section_id}: {section_title}",
                              type="section",
                              section_id=section_id,
                              chapter_id=chapter_id,
                              level=1)
                    node_mapping[f"mục_{section_id}_chương_{chapter_id.lower()}"] = section_node_id

                    # Connect chapter -> section
                    G.add_edge(chapter_node_id, section_node_id, relation="contains")

                    # Process articles within sections
                    for article in section["articles"]:
                        self.add_article_nodes(G, article, chapter_id, section_node_id, node_mapping)

            # Handle articles directly under the chapter (if no sections)
            elif chapter.get("articles"):
                 for article in chapter["articles"]:
                    self.add_article_nodes(G, article, chapter_id, chapter_node_id, node_mapping)

        # Bước 2: Resolve references và tạo edges
        for chapter in self.law_data:
            for article in chapter.get("articles", []):
                article_id = article["article_id"]

                for clause in article.get("clauses", []):
                    clause_id = clause["clause_id"]
                    clause_text = clause["text"]

                    # Process references in clause
                    for ref in clause.get("references", []):
                        self.resolve_reference(G, ref, article_id, clause_id, node_mapping)

                    # Process references in points
                    for point in clause.get("points", []):
                        for ref in point.get("references", []):
                            self.resolve_reference(G, ref, article_id, clause_id, node_mapping)

            # Also process articles within sections for references
            if chapter.get("sections"):
                 for section in chapter["sections"]:
                    for article in section.get("articles", []):
                        article_id = article["article_id"]
                        for clause in article.get("clauses", []):
                            clause_id = clause["clause_id"]
                            clause_text = clause["text"]
                            for ref in clause.get("references", []):
                                self.resolve_reference(G, ref, article_id, clause_id, node_mapping)
                            for point in clause.get("points", []):
                                for ref in point.get("references", []):
                                    self.resolve_reference(G, ref, article_id, clause_id, node_mapping)

        return G

    def add_article_nodes(self, G, article, chapter_id, parent_node_id, node_mapping):
        """Helper function to add article, clause, and point nodes."""
        article_id = article["article_id"]
        article_title = article.get("title", "")

        # Article node
        article_node_id = f"Điều_{article_id}"
        G.add_node(article_node_id,
                  text=article_title,
                  title=f"Điều {article_id}: {article_title}",
                  type="article",
                  article_id=article_id,
                  chapter_id=chapter_id,
                  level=1 if "Chương" in parent_node_id else 2)
        node_mapping[f"điều_{article_id}"] = article_node_id

        # Connect parent -> article
        G.add_edge(parent_node_id, article_node_id, relation="contains")

        for clause in article.get("clauses", []):
            clause_id = clause["clause_id"]
            clause_text = clause["text"]

            # Clause node
            clause_node_id = f"Điều_{article_id}_Khoản_{clause_id}"
            G.add_node(clause_node_id,
                      text=clause_text,
                      title=f"Điều {article_id} - Khoản {clause_id}",
                      type="clause",
                      article_id=article_id,
                      clause_id=clause_id,
                      chapter_id=chapter_id,
                      level=3 if "Điều" in parent_node_id else 4)

            # Connect article -> clause
            G.add_edge(article_node_id, clause_node_id, relation="contains")

            # Process points
            for point in clause.get("points", []):
                point_id = point["point_id"]
                point_text = point["text"]

                # Point node
                point_node_id = f"Điều_{article_id}_Khoản_{clause_id}_Điểm_{point_id}"
                G.add_node(point_node_id,
                          text=point_text,
                          title=f"Điều {article_id} - Khoản {clause_id} - Điểm {point_id}",
                          type="point",
                          article_id=article_id,
                          clause_id=clause_id,
                          point_id=point_id,
                          chapter_id=chapter_id,
                          level=4 if "Khoản" in clause_node_id else 5)

                # Connect clause -> point
                G.add_edge(clause_node_id, point_node_id, relation="contains")

    def resolve_reference(self, G, ref, current_article_id, current_clause_id, node_mapping):
        """Resolve một reference và tạo edge trong graph"""
        target = ref.get("target", {})
        ref_text = ref.get("text", "")

        # Find the source node based on the structure of the JSON and the reference location
        source_node_id = None
        if current_clause_id is not None:
             source_node_id = f"Điều_{current_article_id}_Khoản_{current_clause_id}"

        if source_node_id not in G.nodes():
             return

        # Find target nodes
        target_nodes = []

        target_article = target.get("article")
        if target_article is None:
             return

        if target_article == "current":
            target_article = current_article_id

        base_target_id = f"Điều_{target_article}"

        # Handle specific clause(s)
        if "clauses" in target and isinstance(target["clauses"], list):
            for clause_num in target["clauses"]:
                if clause_num == "current":
                     clause_num = current_clause_id
                target_node = f"{base_target_id}_Khoản_{clause_num}"
                if target_node in G.nodes():
                    target_nodes.append(target_node)
        elif "clause" in target:
            clause_num = target["clause"]
            if clause_num == "current":
                clause_num = current_clause_id
            target_node = f"{base_target_id}_Khoản_{clause_num}"
            if target_node in G.nodes():
                target_nodes.append(target_node)

        # Handle specific point(s)
        target_clause_for_points = target.get("clause")
        if target_clause_for_points is None and isinstance(target.get("clauses"), list) and len(target.get("clauses")) == 1:
             target_clause_for_points = target["clauses"][0]

        if target_clause_for_points is not None:
             if target_clause_for_points == "current":
                 target_clause_for_points = current_clause_id

             if isinstance(target.get("points"), list):
                  for point_id in target["points"]:
                      target_node = f"{base_target_id}_Khoản_{target_clause_for_points}_Điểm_{point_id}"
                      if target_node in G.nodes():
                          target_nodes.append(target_node)
             elif isinstance(target.get("point"), str):
                 point_id = target["point"]
                 target_node = f"{base_target_id}_Khoản_{target_clause_for_points}_Điểm_{point_id}"
                 if target_node in G.nodes():
                     target_nodes.append(target_node)

        # If no specific clause or point, reference the article
        if not target_nodes and base_target_id in G.nodes():
            target_nodes.append(base_target_id)

        # Tạo edges
        if source_node_id in G.nodes():
            for target_node in target_nodes:
                G.add_edge(source_node_id, target_node,
                          relation="references",
                          ref_text=ref_text)

    def create_embeddings(self):
        """Tạo embeddings với context phong phú hơn"""
        node_records = []

        # Ensure the collection is empty before adding
        try:
            self.chroma_client.delete_collection("law_nodes_improved")
            self.collection = self.chroma_client.create_collection("law_nodes_improved")
        except:
             self.collection = self.chroma_client.get_or_create_collection("law_nodes_improved")
             if self.collection.count() > 0:
                  logger.info("⚠️ Collection 'law_nodes_improved' already exists and contains data. Skipping embedding creation.")
                  return

        for node_id in self.G.nodes():
            data = self.G.nodes[node_id]

            # Tạo context phong phú hơn
            context_parts = []

            # Thêm title
            if data.get("title"):
                context_parts.append(f"Tiêu đề: {data['title']}")

            # Thêm text content
            if data.get("text"):
                context_parts.append(f"Nội dung: {data['text']}")

            # Thêm thông tin hierarchy
            hierarchy = []
            if data.get("chapter_id"):
                hierarchy.append(f"Chương {data['chapter_id']}")
            if data.get("section_id"):
                 hierarchy.append(f"Mục {data['section_id']}")
            if data.get("article_id"):
                hierarchy.append(f"Điều {data['article_id']}")
            if data.get("clause_id") is not None:
                hierarchy.append(f"Khoản {data['clause_id']}")
            if data.get("point_id"):
                hierarchy.append(f"Điểm {data['point_id']}")

            if hierarchy:
                context_parts.append(f"Cấp độ: {' - '.join(hierarchy)}")

            # Thêm thông tin về references
            references = []
            for neighbor in self.G.successors(node_id):
                if self.G[node_id][neighbor].get("relation") == "references":
                    ref_text = self.G[node_id][neighbor].get("ref_text", "")
                    if ref_text:
                        references.append(ref_text)

            if references:
                context_parts.append(f"Tham chiếu: {', '.join(references)}")

            # Thêm thông tin về nodes được chứa
            contained_by = []
            for neighbor in self.G.predecessors(node_id):
                 if self.G[neighbor][node_id].get("relation") == "contains":
                     contained_by.append(neighbor)

            if contained_by:
                 context_parts.append(f"Được chứa trong: {', '.join(contained_by)}")

            # Kết hợp tất cả
            full_text = "\n".join(context_parts)

            if full_text.strip():
                metadata = {
                    "type": data.get("type", "unknown"),
                    "level": data.get("level", 0),
                    "article_id": data.get("article_id"),
                    "clause_id": data.get("clause_id"),
                    "point_id": data.get("point_id"),
                    "chapter_id": data.get("chapter_id"),
                    "section_id": data.get("section_id")
                }
                # Filter out None values from metadata
                filtered_metadata = {k: v for k, v in metadata.items() if v is not None}

                node_records.append({
                    "id": node_id,
                    "text": full_text,
                    "metadata": filtered_metadata
                })

        logger.info(f"✅ Tạo {len(node_records)} node records cho embedding")

        # Tạo embeddings
        batch_size = 100
        for i in range(0, len(node_records), batch_size):
            batch = node_records[i : i + batch_size]
            ids = [record["id"] for record in batch]
            documents = [record["text"] for record in batch]
            metadatas = [record["metadata"] for record in batch]

            # Get embeddings for the batch
            try:
                emb_response = self.client.embeddings.create(
                    model="text-embedding-3-large",
                    input=documents
                )
                embeddings = [item.embedding for item in emb_response.data]
            except Exception as e:
                logger.error(f"Error getting embeddings for batch {i}: {e}")
                continue

            self.collection.add(
                ids=ids,
                documents=documents,
                embeddings=embeddings,
                metadatas=metadatas
            )

        logger.info("✅ Hoàn tất tạo embeddings")

    def get_embedding(self, text: str) -> List[float]:
        """Tạo embedding cho text"""
        resp = self.client.embeddings.create(
            model="text-embedding-3-large",
            input=text
        )
        return resp.data[0].embedding

    def semantic_search(self, query: str, top_k: int = 5) -> List[Dict]:
        """Semantic search với re-ranking"""
        q_emb = self.get_embedding(query)
        results = self.collection.query(
            query_embeddings=[q_emb],
            n_results=min(top_k * 5, 50),
            include=['metadatas', 'documents', 'distances']
        )

        if not results or not results["ids"] or not results["ids"][0]:
             return []

        # Re-rank based on relevance and importance
        scored_results = []
        for i, (node_id, metadata, doc, distance) in enumerate(zip(
            results["ids"][0],
            results["metadatas"][0],
            results["documents"][0],
            results["distances"][0]
        )):
            semantic_score = 1.0 / (1.0 + distance)

            # Importance score based on level and type
            importance_score = 0.0
            node_type = metadata.get("type")
            if node_type == "article":
                importance_score = 1.0
            elif node_type == "clause":
                importance_score = 0.8
            elif node_type == "point":
                importance_score = 0.6
            elif node_type == "section":
                 importance_score = 0.4
            elif node_type == "chapter":
                importance_score = 0.2

            # Combine scores
            final_score = semantic_score * 0.7 + importance_score * 0.3

            scored_results.append({
                "id": node_id,
                "metadata": metadata,
                "document": doc,
                "score": final_score,
                "distance": distance
            })

        # Sort by final score
        scored_results.sort(key=lambda x: x["score"], reverse=True)
        return scored_results[:top_k]

    def get_graph_context(self, start_node_id: str, depth: int = 2, visited: set = None) -> List[Dict]:
        """Lấy context từ graph với logic thông minh hơn"""
        if visited is None:
            visited = set()

        context_nodes = []
        queue = [(start_node_id, 0)]

        while queue:
            current_node_id, current_depth = queue.pop(0)

            if current_node_id in visited or current_depth > depth:
                continue

            visited.add(current_node_id)

            if current_node_id in self.G.nodes():
                data = self.G.nodes[current_node_id]
                text = data.get("text") or data.get("title") or ""
                if text.strip():
                    context_nodes.append({
                        "id": current_node_id,
                        "type": data.get("type", "unknown"),
                        "title": data.get("title", ""),
                        "text": text,
                        "level": data.get("level", 0),
                        "depth_from_start": current_depth
                    })

                # Add neighbors to the queue
                neighbors_to_visit = []

                # Prioritize 'references' targets first
                ref_neighbors = [(nb, current_depth + 1) for nb in self.G.successors(current_node_id)
                                 if self.G[current_node_id][nb].get("relation") == "references" and (nb, current_depth + 1) not in queue]
                neighbors_to_visit.extend(ref_neighbors)

                # Add 'contains' children
                contains_children = [(nb, current_depth + 1) for nb in self.G.successors(current_node_id)
                                     if self.G[current_node_id][nb].get("relation") == "contains" and (nb, current_depth + 1) not in queue]
                neighbors_to_visit.extend(contains_children)

                # Add 'contained_by' parents
                contained_by_parents = [(nb, current_depth + 1) for nb in self.G.predecessors(current_node_id)
                                        if self.G[nb][current_node_id].get("relation") == "contains" and (nb, current_depth + 1) not in queue]
                neighbors_to_visit.extend(contained_by_parents)

                # Add valid neighbors to the queue
                queue.extend([n for n in neighbors_to_visit if n[0] in self.G.nodes()])

        # Sort context nodes by depth from start and then by level
        context_nodes.sort(key=lambda x: (x["depth_from_start"], x["level"]))

        return context_nodes

    def format_context_nodes(self, context_nodes: List[Dict]) -> str:
        """Format the list of context nodes into a readable string."""
        formatted_text = []
        current_depth = -1
        for node in context_nodes:
            if node["depth_from_start"] > current_depth:
                formatted_text.append(f"\n--- Depth {node['depth_from_start']} ---")
                current_depth = node["depth_from_start"]

            indent = "  " * node["depth_from_start"]
            node_type = node.get("type", "").capitalize()
            node_id = node.get("id", "")
            title = node.get("title", "")
            text = node.get("text", "")

            if node_type in ["Chapter", "Section", "Article"]:
                 formatted_text.append(f"{indent}▪️ {node_type} {node_id}: {title}")
            else:
                 formatted_text.append(f"{indent}▪️ {node_id}: {text}")

        return "\n".join(formatted_text)

    def query(self, question: str, k: int = 5, expand_depth: int = 2) -> Tuple[str, str]:
        """Truy vấn cải tiến với GraphRAG"""
        # Bước 1: Semantic search
        semantic_results = self.semantic_search(question, k)

        if not semantic_results:
             return "Không tìm thấy thông tin liên quan trong luật.", ""

        # Bước 2: Mở rộng context từ graph
        all_context_nodes = []
        visited_nodes = set()

        for result in semantic_results:
            start_node_id = result["id"]
            context_from_node = self.get_graph_context(start_node_id, expand_depth, visited_nodes)
            all_context_nodes.extend(context_from_node)

        # Remove duplicates
        unique_context_nodes = list({node['id']: node for node in all_context_nodes}.values())
        unique_context_nodes.sort(key=lambda x: (x["depth_from_start"], x["level"]))

        # Bước 3: Format context và tạo prompt
        context_text = self.format_context_nodes(unique_context_nodes)

        system_prompt = """Bạn là một trợ lý tư vấn về luật nhà ở chuyên nghiệp. Nhiệm vụ của bạn là:

1. Phân tích câu hỏi của người dùng một cách cẩn thận
2. Dựa vào NỘI DUNG LUẬT LIÊN QUAN được cung cấp, tìm kiếm và trích dẫn chính xác các điều luật, khoản, điểm liên quan.
3. Đưa ra câu trả lời chi tiết, chính xác và dễ hiểu.
4. Luôn TRÍCH DẪN SỐ ĐIỀU, KHOẢN, ĐIỂM CỤ THỂ khi trả lời (ví dụ: Theo Điều 10 Khoản 2 Điểm a...).
5. Nếu NỘI DUNG LUẬT LIÊN QUAN không đủ để trả lời câu hỏi, hãy nói rõ điều đó và gợi ý người dùng cung cấp thêm thông tin hoặc tìm kiếm các luật khác. KHÔNG ĐƯỢC bịa đặt thông tin.
6. Trả lời bằng tiếng Việt, rõ ràng và chuyên nghiệp.
7. Chú trọng vào các nội dung chi tiết ở cấp độ Khoản, Điểm.

NỘI DUNG LUẬT LIÊN QUAN:
"""

        user_prompt = f"""Câu hỏi: {question}

{context_text}

Hãy trả lời câu hỏi dựa trên NỘI DUNG LUẬT LIÊN QUAN đã cho."""

        # Bước 4: Gọi LLM
        try:
            response = self.client.chat.completions.create(
                model="gpt-4o-mini",
                messages=[
                    {"role": "system", "content": system_prompt},
                    {"role": "user", "content": user_prompt}
                ],
                temperature=0.3,
                max_tokens=1500
            )
            answer = response.choices[0].message.content
        except Exception as e:
            answer = f"Đã xảy ra lỗi khi gọi mô hình ngôn ngữ: {e}"

        return answer, context_text

# FastAPI App
app = FastAPI(
    title="GraphRAG API",
    description="API cho trợ lý AI tư vấn pháp luật nhà ở Việt Nam",
    version="1.0.0",
    docs_url="/docs",
    redoc_url="/redoc"
)

# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Trong production nên giới hạn origins
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Global GraphRAG instance
rag_instance = None

@app.get("/health", response_model=HealthResponse, summary="Health Check")
async def health_check():
    """Kiểm tra trạng thái server và GraphRAG"""
    return HealthResponse(
        status="healthy",
        message="GraphRAG API is running",
        rag_initialized=rag_instance is not None
    )

@app.post("/query", response_model=ChatQueryResponse, summary="Query Law Database")
async def query_law(request: ChatQueryRequest):
    """Truy vấn cơ sở dữ liệu luật nhà ở"""
    if not rag_instance:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="GraphRAG not initialized"
        )
    
    try:
        answer, context = rag_instance.query(
            request.question, 
            request.k, 
            request.expand_depth
        )
        
        return ChatQueryResponse(
            answer=answer,
            context=context,
            question=request.question
        )
        
    except Exception as e:
        logger.error(f"Error processing query: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Error processing query: {str(e)}"
        )

@app.post("/search", response_model=SearchResponse, summary="Semantic Search")
async def semantic_search(request: SearchRequest):
    """Tìm kiếm semantic trong cơ sở dữ liệu luật"""
    if not rag_instance:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="GraphRAG not initialized"
        )
    
    try:
        results = rag_instance.semantic_search(request.query, request.top_k)
        
        # Convert results to Pydantic models
        search_results = [
            SearchResult(
                id=result["id"],
                metadata=result["metadata"],
                document=result["document"],
                score=result["score"],
                distance=result["distance"]
            )
            for result in results
        ]
        
        return SearchResponse(
            results=search_results,
            query=request.query
        )
        
    except Exception as e:
        logger.error(f"Error processing search: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Error processing search: {str(e)}"
        )

@app.on_event("startup")
async def startup_event():
    """Initialize GraphRAG on startup"""
    global rag_instance
    
    try:
        # Get API key from environment
        openai_api_key = os.getenv('OPENAI_API_KEY')
        if not openai_api_key:
            logger.error("OPENAI_API_KEY environment variable not set")
            return
        
        # Path to the JSON data file
        json_path = "data/luatnhao_structuredv33.converted.json"
        
        if not os.path.exists(json_path):
            logger.error(f"Data file not found: {json_path}")
            return
        
        logger.info("Initializing GraphRAG...")
        rag_instance = ImprovedGraphRAG(json_path, openai_api_key)
        logger.info("GraphRAG initialized successfully!")
        
    except Exception as e:
        logger.error(f"Failed to initialize GraphRAG: {e}")

@app.on_event("shutdown")
async def shutdown_event():
    """Cleanup on shutdown"""
    logger.info("Shutting down GraphRAG server...")

if __name__ == '__main__':
    uvicorn.run(
        "graphrag_server:app",
        host="0.0.0.0",
        port=5000,
        reload=True,
        log_level="info"
    )
