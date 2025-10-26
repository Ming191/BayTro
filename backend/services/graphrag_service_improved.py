"""
Improved GraphRAG Service for Vietnamese Housing Law Chatbot
Based on the original graphrag_server.py logic
"""
import os
import json
import logging
from typing import List, Dict, Any, Tuple, Optional
import networkx as nx
import chromadb
from openai import OpenAI
from config import OPENAI_API_KEY, OPENAI_MODEL, OPENAI_EMBEDDING_MODEL

logger = logging.getLogger(__name__)


class ImprovedGraphRAGService:
    """Enhanced GraphRAG service with full graph relationships and context expansion"""
    
    def __init__(
        self,
        json_path: str = "data/luatnhao_structuredv33.converted.json",
        chromadb_path: str = "chromadb_law_v2",
        collection_name: str = "law_nodes_improved_v2"
    ):
        self.json_path = json_path
        self.chromadb_path = chromadb_path
        self.collection_name = collection_name
        
        # Initialize OpenAI client
        self.client = OpenAI(api_key=OPENAI_API_KEY)
        
        # Initialize ChromaDB
        self.chroma_client = chromadb.PersistentClient(path=chromadb_path)
        
        # Load JSON data
        with open(json_path, 'r', encoding='utf-8') as f:
            self.law_data = json.load(f)
        
        # Build improved graph with relationships
        self.G = self.build_improved_graph()
        logger.info(f"Graph built with {len(self.G.nodes())} nodes and {len(self.G.edges())} edges.")
        
        # Load or create collection with enhanced embeddings
        try:
            self.collection = self.chroma_client.get_collection(name=collection_name)
            logger.info(f"Loaded existing collection: {collection_name}")
            # Check if collection has data
            if self.collection.count() == 0:
                logger.info("Collection exists but is empty, creating embeddings...")
                self._create_enhanced_embeddings()
        except Exception:
            logger.info(f"Collection not found, creating new one: {collection_name}")
            self._create_enhanced_embeddings()

    def build_improved_graph(self) -> nx.DiGraph:
        """Build graph with full references and hierarchy like original"""
        G = nx.DiGraph()
        node_mapping = {}

        # Step 1: Create all nodes with full metadata
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

            # Handle sections within chapter
            if chapter.get("sections"):
                for section in chapter["sections"]:
                    section_id = section["section_id"]
                    section_title = section["title"]

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
                        self._add_article_nodes(G, article, chapter_id, section_node_id, node_mapping)

            # Handle articles directly under chapter
            elif chapter.get("articles"):
                for article in chapter["articles"]:
                    self._add_article_nodes(G, article, chapter_id, chapter_node_id, node_mapping)

        # Step 2: Resolve references and create relationship edges
        for chapter in self.law_data:
            for article in chapter.get("articles", []):
                article_id = article["article_id"]
                for clause in article.get("clauses", []):
                    clause_id = clause["clause_id"]
                    # Process references in clause
                    for ref in clause.get("references", []):
                        self._resolve_reference(G, ref, article_id, clause_id, node_mapping)
                    # Process references in points
                    for point in clause.get("points", []):
                        for ref in point.get("references", []):
                            self._resolve_reference(G, ref, article_id, clause_id, node_mapping)

            # Also process articles within sections for references
            if chapter.get("sections"):
                for section in chapter["sections"]:
                    for article in section.get("articles", []):
                        article_id = article["article_id"]
                        for clause in article.get("clauses", []):
                            clause_id = clause["clause_id"]
                            for ref in clause.get("references", []):
                                self._resolve_reference(G, ref, article_id, clause_id, node_mapping)
                            for point in clause.get("points", []):
                                for ref in point.get("references", []):
                                    self._resolve_reference(G, ref, article_id, clause_id, node_mapping)

        return G

    def _add_article_nodes(self, G, article, chapter_id, parent_node_id, node_mapping):
        """Add article, clause, and point nodes with hierarchy"""
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

    def _resolve_reference(self, G, ref, current_article_id, current_clause_id, node_mapping):
        """Resolve reference and create relationship edge"""
        target = ref.get("target", {})
        ref_text = ref.get("text", "")

        # Find source node
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

        # Create reference edges
        for target_node in target_nodes:
            G.add_edge(source_node_id, target_node,
                      relation="references",
                      ref_text=ref_text)

    def _create_enhanced_embeddings(self):
        """Create embeddings with rich context like original"""
        # Clear existing collection
        try:
            self.chroma_client.delete_collection(self.collection_name)
        except:
            pass
        
        self.collection = self.chroma_client.create_collection(self.collection_name)

        node_records = []

        for node_id in self.G.nodes():
            data = self.G.nodes[node_id]

            # Create rich context
            context_parts = []

            # Add title
            if data.get("title"):
                context_parts.append(f"Tiêu đề: {data['title']}")

            # Add text content
            if data.get("text"):
                context_parts.append(f"Nội dung: {data['text']}")

            # Add hierarchy information
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

            # Add reference information
            references = []
            for neighbor in self.G.successors(node_id):
                if self.G[node_id][neighbor].get("relation") == "references":
                    ref_text = self.G[node_id][neighbor].get("ref_text", "")
                    if ref_text:
                        references.append(ref_text)

            if references:
                context_parts.append(f"Tham chiếu: {', '.join(references)}")

            # Add containment information
            contained_by = []
            for neighbor in self.G.predecessors(node_id):
                if self.G[neighbor][node_id].get("relation") == "contains":
                    contained_by.append(neighbor)

            if contained_by:
                context_parts.append(f"Được chứa trong: {', '.join(contained_by)}")

            # Combine all context
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
                # Filter out None values
                filtered_metadata = {k: v for k, v in metadata.items() if v is not None}

                node_records.append({
                    "id": node_id,
                    "text": full_text,
                    "metadata": filtered_metadata
                })

        logger.info(f"Creating embeddings for {len(node_records)} nodes")

        # Create embeddings in batches
        batch_size = 100
        for i in range(0, len(node_records), batch_size):
            batch = node_records[i:i + batch_size]
            ids = [record["id"] for record in batch]
            documents = [record["text"] for record in batch]
            metadatas = [record["metadata"] for record in batch]

            try:
                emb_response = self.client.embeddings.create(
                    model=OPENAI_EMBEDDING_MODEL,
                    input=documents
                )
                embeddings = [item.embedding for item in emb_response.data]

                self.collection.add(
                    ids=ids,
                    documents=documents,
                    embeddings=embeddings,
                    metadatas=metadatas
                )
            except Exception as e:
                logger.error(f"Error creating embeddings for batch {i}: {e}")
                continue

        logger.info("Enhanced embeddings created successfully")

    def _get_embedding(self, text: str) -> List[float]:
        """Get embedding for text"""
        response = self.client.embeddings.create(
            model=OPENAI_EMBEDDING_MODEL,
            input=[text]
        )
        return response.data[0].embedding

    def semantic_search(self, query: str, top_k: int = 5) -> List[Dict[str, Any]]:
        """Enhanced semantic search with re-ranking"""
        logger.info(f"Semantic search for: '{query}' with top_k={top_k}")
        logger.info(f"Collection count: {self.collection.count()}")
        
        q_emb = self._get_embedding(query)
        results = self.collection.query(
            query_embeddings=[q_emb],
            n_results=min(top_k * 5, 50),
            include=['metadatas', 'documents', 'distances']
        )

        logger.info(f"Raw search results: {len(results.get('ids', [[]])[0]) if results else 0} items")
        
        if not results or not results["ids"] or not results["ids"][0]:
            logger.warning("No search results found!")
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
                "content": doc,
                "type": node_type,
                "distance": distance,
                "score": final_score
            })

        # Sort by final score
        scored_results.sort(key=lambda x: x["score"], reverse=True)
        return scored_results[:top_k]

    def _get_graph_context(self, start_node_id: str, depth: int = 2, visited: set = None) -> List[Dict]:
        """Get context from graph with intelligent expansion"""
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

                # Add neighbors to queue
                neighbors_to_visit = []

                # Prioritize 'references' targets first
                ref_neighbors = [(nb, current_depth + 1) for nb in self.G.successors(current_node_id)
                                if self.G[current_node_id][nb].get("relation") == "references"]
                neighbors_to_visit.extend(ref_neighbors)

                # Add 'contains' children
                contains_children = [(nb, current_depth + 1) for nb in self.G.successors(current_node_id)
                                    if self.G[current_node_id][nb].get("relation") == "contains"]
                neighbors_to_visit.extend(contains_children)

                # Add 'contained_by' parents
                contained_by_parents = [(nb, current_depth + 1) for nb in self.G.predecessors(current_node_id)
                                       if self.G[nb][current_node_id].get("relation") == "contains"]
                neighbors_to_visit.extend(contained_by_parents)

                # Add valid neighbors to queue
                queue.extend([n for n in neighbors_to_visit if n[0] in self.G.nodes()])

        # Sort context nodes by depth and level
        context_nodes.sort(key=lambda x: (x["depth_from_start"], x["level"]))
        return context_nodes

    def _format_context_nodes(self, context_nodes: List[Dict]) -> str:
        """Format context nodes into readable string"""
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

    async def query(
        self,
        question: str,
        expand_depth: int = 1,
        top_k: int = 5
    ) -> Dict[str, Any]:
        """Enhanced query with full GraphRAG like original"""
        # Step 1: Semantic search
        semantic_results = self.semantic_search(question, top_k)

        if not semantic_results:
            return {
                "answer": "Không tìm thấy thông tin liên quan trong luật nhà ở.",
                "context": []
            }

        # Step 2: Expand context from graph
        all_context_nodes = []
        visited_nodes = set()

        for result in semantic_results:
            start_node_id = result["id"]
            context_from_node = self._get_graph_context(start_node_id, expand_depth, visited_nodes)
            all_context_nodes.extend(context_from_node)

        # Remove duplicates
        unique_context_nodes = list({node['id']: node for node in all_context_nodes}.values())
        unique_context_nodes.sort(key=lambda x: (x["depth_from_start"], x["level"]))

        # Step 3: Format context and create prompt
        context_text = self._format_context_nodes(unique_context_nodes)

        # Professional legal assistant prompt like original
        system_prompt = """Bạn là một trợ lý tư vấn về luật nhà ở chuyên nghiệp. Nhiệm vụ của bạn là:

1. Phân tích câu hỏi của người dùng một cách cẩn thận
2. Dựa vào NỘI DUNG LUẬT LIÊN QUAN được cung cấp, tìm kiếm và trích dẫn chính xác các điều luật, khoản, điểm liên quan.
3. Đưa ra câu trả lời chi tiết, chính xác và dễ hiểu.
4. Luôn TRÍCH DẪN SỐ ĐIỀU, KHOẢN, ĐIỂM CỤ THỂ khi trả lời (ví dụ: Theo Điều 10 Khoản 2 Điểm a...).
5. Nếu NỘI DUNG LUẬT LIÊN QUAN không đủ để trả lời câu hỏi, hãy nói rõ điều đó và gợi ý người dùng cung cấp thêm thông tin hoặc tìm kiếm các luật khác. KHÔNG ĐƯỢC bịa đặt thông tin.
6. Trả lời bằng tiếng Việt, rõ ràng và chuyên nghiệp.
7. Chú trọng vào các nội dung chi tiết ở cấp độ Khoản, Điểm.

NỘI DUNG LUẬT LIÊN QUAN:"""

        user_prompt = f"""Câu hỏi: {question}

{context_text}

Hãy trả lời câu hỏi dựa trên NỘI DUNG LUẬT LIÊN QUAN đã cho."""

        # Step 4: Call LLM
        try:
            response = self.client.chat.completions.create(
                model=OPENAI_MODEL,
                messages=[
                    {"role": "system", "content": system_prompt},
                    {"role": "user", "content": user_prompt}
                ],
                temperature=0.3,
                max_tokens=1500
            )
            answer = response.choices[0].message.content
        except Exception as e:
            logger.error(f"Error calling LLM: {e}")
            answer = f"Đã xảy ra lỗi khi gọi mô hình ngôn ngữ: {e}"

        return {
            "answer": answer,
            "context": [
                {
                    "id": node.get('id'),
                    "content": node.get('text') or node.get('title'),
                    "type": node.get('type')
                }
                for node in unique_context_nodes[:top_k]
            ]
        }
