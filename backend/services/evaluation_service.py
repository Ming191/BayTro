import logging
from typing import List, Dict, Any, Optional
import numpy as np
from langchain_openai import ChatOpenAI, OpenAIEmbeddings
from langchain.prompts import ChatPromptTemplate
from config import OPENAI_API_KEY, OPENAI_MODEL, OPENAI_EMBEDDING_MODEL

logger = logging.getLogger(__name__)


class EvaluationService:
    """Service for evaluating chatbot responses with multiple metrics"""

    def __init__(self):
        self.llm = ChatOpenAI(
            model=OPENAI_MODEL,
            temperature=0.0,
            api_key=OPENAI_API_KEY
        )
        self.embeddings = OpenAIEmbeddings(
            model=OPENAI_EMBEDDING_MODEL,
            api_key=OPENAI_API_KEY
        )
        logger.info("EvaluationService initialized")

    def evaluate_retrieval(
        self,
        retrieved_node_ids: List[str],
        ground_truth_node_ids: List[str],
        k: int = None
    ) -> Dict[str, float]:
        """
        Evaluate retrieval quality using Precision@k, Recall@k, F1-score, and Hit Rate

        Args:
            retrieved_node_ids: List of retrieved node IDs
            ground_truth_node_ids: List of ground truth relevant node IDs
            k: Number of top results to consider (if None, use all retrieved)

        Returns:
            Dict with precision@k, recall@k, f1_score, hit_rate
        """
        if not retrieved_node_ids:
            return {
                "precision": 0.0,
                "recall": 0.0,
                "f1_score": 0.0,
                "hit_rate": 0.0,
                "retrieved_count": 0,
                "ground_truth_count": len(ground_truth_node_ids),
                "relevant_retrieved": 0,
                "k": k
            }

        if not ground_truth_node_ids:
            logger.warning("No ground truth provided for retrieval evaluation")
            return {
                "precision": 0.0,
                "recall": 0.0,
                "f1_score": 0.0,
                "hit_rate": 0.0,
                "retrieved_count": len(retrieved_node_ids),
                "ground_truth_count": 0,
                "relevant_retrieved": 0,
                "k": k
            }

        # Use top-k results if k is specified
        if k is not None and k > 0:
            retrieved_node_ids = retrieved_node_ids[:k]

        # Convert to sets for comparison
        retrieved_set = set(retrieved_node_ids)
        ground_truth_set = set(ground_truth_node_ids)

        # Calculate intersection
        relevant_retrieved = retrieved_set.intersection(ground_truth_set)
        num_relevant_retrieved = len(relevant_retrieved)

        # Precision@k: TP / (TP + FP) = relevant_retrieved / total_retrieved
        precision = num_relevant_retrieved / len(retrieved_set) if len(retrieved_set) > 0 else 0.0

        # Recall@k: TP / (TP + FN) = relevant_retrieved / total_ground_truth
        recall = num_relevant_retrieved / len(ground_truth_set) if len(ground_truth_set) > 0 else 0.0

        # F1-score: harmonic mean of precision and recall
        f1_score = (2 * precision * recall) / (precision + recall) if (precision + recall) > 0 else 0.0

        # Hit Rate@k: whether at least one relevant document was retrieved in top-k
        hit_rate = 1.0 if num_relevant_retrieved > 0 else 0.0

        return {
            "precision": round(precision, 4),
            "recall": round(recall, 4),
            "f1_score": round(f1_score, 4),
            "hit_rate": round(hit_rate, 4),
            "retrieved_count": len(retrieved_set),
            "ground_truth_count": len(ground_truth_set),
            "relevant_retrieved": num_relevant_retrieved,
            "k": k if k else len(retrieved_node_ids)
        }


    async def evaluate_generation_cosine_similarity(
        self,
        generated_answer: str,
        reference_answer: str
    ) -> Dict[str, float]:
        try:
            # Get embeddings for both texts
            embeddings = await self.embeddings.aembed_documents([generated_answer, reference_answer])

            gen_embedding = np.array(embeddings[0])
            ref_embedding = np.array(embeddings[1])

            # Calculate cosine similarity
            cosine_sim = np.dot(gen_embedding, ref_embedding) / (
                np.linalg.norm(gen_embedding) * np.linalg.norm(ref_embedding)
            )

            return {
                "cosine_similarity": round(float(cosine_sim), 4),
                "embedding_dimension": len(gen_embedding)
            }
        except Exception as e:
            logger.error(f"Error calculating cosine similarity: {e}")
            return {
                "cosine_similarity": 0.0,
                "error": str(e)
            }
    async def evaluate_faithfulness(
        self,
        question: str,
        answer: str,
        context: List[str]
    ) -> Dict[str, Any]:
        """
        Evaluate faithfulness: whether the answer is grounded in the context
        Score: 0.0 to 1.0 (with 2 decimal precision, e.g., 0.75)

        Args:
            question: The user's question
            answer: The generated answer
            context: List of context texts used for generation

        Returns:
            Dict with faithfulness score (0.0-1.0) and explanation
        """
        context_text = "\n\n".join(context) if context else "Không có context"

        prompt = ChatPromptTemplate.from_messages([
            ("system", """Bạn là một chuyên gia đánh giá chất lượng câu trả lời.
Nhiệm vụ của bạn là đánh giá tính TRUNG THỰC (faithfulness) của câu trả lời dựa trên context được cung cấp.

Hãy cho điểm từ 0.00 đến 1.00 (với 2 chữ số thập phân, ví dụ: 0.75, 0.85, 0.92)

Tiêu chí đánh giá:
- 0.90-1.00: Hoàn toàn trung thực - Tất cả thông tin đều được hỗ trợ bởi context
- 0.70-0.89: Khá trung thực - Hầu hết thông tin có trong context, có thể có vài suy luận hợp lý
- 0.50-0.69: Một phần trung thực - Một số thông tin được hỗ trợ, nhưng có phần không căn cứ
- 0.30-0.49: Ít trung thực - Nhiều thông tin không có trong context
- 0.00-0.29: Không trung thực - Phần lớn thông tin sai hoặc mâu thuẫn với context

Trả lời theo định dạng JSON:
{{
    "score": 0.XX (số thập phân từ 0.00 đến 1.00),
    "explanation": "Giải thích ngắn gọn tại sao",
    "faithful_parts": ["danh sách các phần trung thực"],
    "unfaithful_parts": ["danh sách các phần không trung thực"]
}}"""),
            ("user", """Context:
{context}

Câu hỏi: {question}

Câu trả lời cần đánh giá:
{answer}

Hãy đánh giá tính trung thực của câu trả lời dựa trên context. Cho điểm từ 0.00 đến 1.00.""")
        ])

        try:
            response = await self.llm.ainvoke(
                prompt.format_messages(
                    context=context_text,
                    question=question,
                    answer=answer
                )
            )

            # Parse JSON response
            import json
            result = json.loads(response.content)

            # Get score and ensure it's in 0-1 range with 2 decimals
            score = float(result.get("score", 0))
            score = max(0.0, min(1.0, score))  # Clamp to 0-1
            score = round(score, 2)  # Round to 2 decimals

            return {
                "metric": "faithfulness",
                "score": score,
                "max_score": 1.0,
                "explanation": result.get("explanation", ""),
                "faithful_parts": result.get("faithful_parts", []),
                "unfaithful_parts": result.get("unfaithful_parts", [])
            }
        except Exception as e:
            logger.error(f"Error evaluating faithfulness: {e}")
            return {
                "metric": "faithfulness",
                "score": 0.0,
                "max_score": 1.0,
                "error": str(e)
            }

    async def evaluate_answer_relevance(
        self,
        question: str,
        answer: str
    ) -> Dict[str, Any]:
        """
        Evaluate answer relevance: whether the answer addresses the question
        Score: 0.0 to 1.0 (with 2 decimal precision, e.g., 0.75)

        Args:
            question: The user's question
            answer: The generated answer

        Returns:
            Dict with relevance score (0.0-1.0) and explanation
        """
        prompt = ChatPromptTemplate.from_messages([
            ("system", """Bạn là một chuyên gia đánh giá chất lượng câu trả lời.
Nhiệm vụ của bạn là đánh giá tính LIÊN QUAN (relevance) của câu trả lời với câu hỏi.

Hãy cho điểm từ 0.00 đến 1.00 (với 2 chữ số thập phân, ví dụ: 0.75, 0.85, 0.92)

Tiêu chí đánh giá:
- 0.90-1.00: Rất liên quan - Trả lời trực tiếp, đầy đủ, không thừa thông tin
- 0.70-0.89: Khá liên quan - Trả lời đúng trọng tâm nhưng có thể thiếu chi tiết nhỏ
- 0.50-0.69: Một phần liên quan - Trả lời được một số khía cạnh nhưng thiếu hoặc lạc đề
- 0.30-0.49: Ít liên quan - Trả lời chung chung, thiếu nhiều thông tin quan trọng
- 0.00-0.29: Không liên quan - Không trả lời câu hỏi hoặc hoàn toàn lạc đề

Trả lời theo định dạng JSON:
{{
    "score": 0.XX (số thập phân từ 0.00 đến 1.00),
    "explanation": "Giải thích ngắn gọn tại sao",
    "relevant_aspects": ["các khía cạnh được trả lời đúng"],
    "missing_aspects": ["các khía cạnh còn thiếu"],
    "irrelevant_parts": ["các phần không liên quan"]
}}"""),
            ("user", """Câu hỏi: {question}

Câu trả lời cần đánh giá:
{answer}

Hãy đánh giá tính liên quan của câu trả lời với câu hỏi. Cho điểm từ 0.00 đến 1.00.""")
        ])

        try:
            response = await self.llm.ainvoke(
                prompt.format_messages(
                    question=question,
                    answer=answer
                )
            )
            import json
            result = json.loads(response.content)

            score = float(result.get("score", 0))
            score = max(0.0, min(1.0, score))
            score = round(score, 2)

            return {
                "metric": "answer_relevance",
                "score": score,
                "max_score": 1.0,
                "explanation": result.get("explanation", ""),
                "relevant_aspects": result.get("relevant_aspects", []),
                "missing_aspects": result.get("missing_aspects", []),
                "irrelevant_parts": result.get("irrelevant_parts", [])
            }
        except Exception as e:
            logger.error(f"Error evaluating answer relevance: {e}")
            return {
                "metric": "answer_relevance",
                "score": 0.0,
                "max_score": 1.0,
                "error": str(e)
            }

    async def evaluate_overclaiming(
        self,
        answer: str,
        context: List[str]
    ) -> Dict[str, Any]:
        """
        Evaluate overclaiming: whether the answer makes claims beyond the context
        Score: 0 (has overclaiming), 1 (no overclaiming)

        Args:
            answer: The generated answer
            context: List of context texts used for generation

        Returns:
            Dict with overclaiming score (0/1) and explanation
        """
        context_text = "\n\n".join(context) if context else "Không có context"

        prompt = ChatPromptTemplate.from_messages([
            ("system", """Bạn là một chuyên gia đánh giá chất lượng câu trả lời.
Nhiệm vụ của bạn là kiểm tra xem câu trả lời có NÓI QUÁ (overclaiming) - đưa ra thông tin vượt quá những gì có trong context hay không.

Tiêu chí đánh giá:
- Score 1 (Không nói quá): Câu trả lời chỉ đưa ra thông tin có trong context hoặc suy luận hợp lý từ context. Có thể nói "không rõ" hoặc "không đủ thông tin" khi context không đủ.
- Score 0 (Có nói quá): Câu trả lời đưa ra các khẳng định, số liệu, hoặc chi tiết cụ thể không có trong context. Đưa ra thông tin như thể chắc chắn khi context không hỗ trợ.

Trả lời theo định dạng JSON:
{{
    "score": 0/1,
    "explanation": "Giải thích ngắn gọn",
    "overclaimed_statements": ["danh sách các phát biểu nói quá (nếu có)"],
    "severity": "none/minor/major"
}}"""),
            ("user", """Context:
{context}

Câu trả lời cần đánh giá:
{answer}

Hãy kiểm tra xem câu trả lời có nói quá so với context không.""")
        ])

        try:
            response = await self.llm.ainvoke(
                prompt.format_messages(
                    context=context_text,
                    answer=answer
                )
            )

            # Parse JSON response
            import json
            result = json.loads(response.content)

            return {
                "metric": "overclaiming",
                "score": result.get("score", 0),
                "max_score": 1,
                "explanation": result.get("explanation", ""),
                "overclaimed_statements": result.get("overclaimed_statements", []),
                "severity": result.get("severity", "unknown")
            }
        except Exception as e:
            logger.error(f"Error evaluating overclaiming: {e}")
            return {
                "metric": "overclaiming",
                "score": 0,
                "max_score": 1,
                "error": str(e)
            }

    async def evaluate_irrelevant_rejection(
        self,
        question: str,
        answer: str
    ) -> Dict[str, Any]:
        """
        Evaluate whether chatbot properly rejects irrelevant questions
        Score: 1 (proper rejection - states out of domain), 0 (still tries to answer)

        Args:
            question: The user's question
            answer: The generated answer

        Returns:
            Dict with rejection score (0/1) and explanation
        """
        prompt = ChatPromptTemplate.from_messages([
            ("system", """Bạn là một chuyên gia đánh giá chatbot.
Nhiệm vụ của bạn là kiểm tra xem chatbot có TỪ CHỐI trả lời đúng cách với câu hỏi KHÔNG LIÊN QUAN đến domain của nó (luật nhà ở) hay không.

Tiêu chí đánh giá:
- Score 1 (Từ chối đúng): Chatbot NÓI RÕ rằng câu hỏi không thuộc phạm vi/domain của mình (ví dụ: "không liên quan đến luật nhà ở", "chỉ trả lời về luật nhà ở", "không thuộc chuyên môn").
- Score 0 (Trả lời lung tung): Chatbot vẫn cố gắng trả lời câu hỏi hoặc đưa ra thông tin không liên quan.

Lưu ý:
- Từ chối đúng ≠ nói "không biết" mà phải nói rõ "không thuộc domain"
- Hướng dẫn tìm nguồn khác SAU KHI đã từ chối vẫn được tính là từ chối đúng

Trả lời theo định dạng JSON:
{{
    "score": 0/1,
    "explanation": "Giải thích ngắn gọn",
    "rejection_detected": true/false,
    "rejection_phrases": ["các cụm từ thể hiện từ chối"]
}}"""),
            ("user", """Câu hỏi: {question}

Câu trả lời của chatbot:
{answer}

Hãy đánh giá xem chatbot có từ chối trả lời đúng cách không.""")
        ])

        try:
            response = await self.llm.ainvoke(
                prompt.format_messages(
                    question=question,
                    answer=answer
                )
            )

            import json
            result = json.loads(response.content)

            return {
                "metric": "irrelevant_rejection",
                "score": result.get("score", 0),
                "max_score": 1,
                "explanation": result.get("explanation", ""),
                "rejection_detected": result.get("rejection_detected", False),
                "rejection_phrases": result.get("rejection_phrases", [])
            }
        except Exception as e:
            logger.error(f"Error evaluating irrelevant rejection: {e}")
            return {
                "metric": "irrelevant_rejection",
                "score": 0,
                "max_score": 1,
                "error": str(e)
            }


    async def evaluate_complete(
        self,
        question: str,
        generated_answer: str,
        retrieved_node_ids: List[str],
        retrieved_contexts: List[str],
        ground_truth_node_ids: Optional[List[str]] = None,
        reference_answer: Optional[str] = None,
        question_type: Optional[str] = None
    ) -> Dict[str, Any]:
        """
        Run complete evaluation suite

        Args:
            question: User's question
            generated_answer: Chatbot's generated answer
            retrieved_node_ids: List of retrieved node IDs
            retrieved_contexts: List of context texts
            ground_truth_node_ids: Ground truth relevant node IDs (optional)
            reference_answer: Reference answer for comparison (optional)
            question_type: Type of question (irrelevant, direct, paraphrased, multiple_points)

        Returns:
            Complete evaluation results
        """
        results = {
            "question": question,
            "generated_answer": generated_answer,
            "retrieval_metrics": {},
            "generation_metrics": {},
            "llm_judge_metrics": {}
        }

        if question_type == "irrelevant":
            rejection_eval = await self.evaluate_irrelevant_rejection(
                question=question,
                answer=generated_answer
            )
            results["llm_judge_metrics"]["irrelevant_rejection"] = rejection_eval

            results["summary"] = {
                "irrelevant_rejection_score": rejection_eval.get("score", 0)
            }
            return results


        if ground_truth_node_ids:
            results["retrieval_metrics"] = self.evaluate_retrieval(
                retrieved_node_ids=retrieved_node_ids,
                ground_truth_node_ids=ground_truth_node_ids
            )

        if reference_answer:
            results["generation_metrics"] = await self.evaluate_generation_cosine_similarity(
                generated_answer=generated_answer,
                reference_answer=reference_answer
            )

        if retrieved_contexts:
            faithfulness = await self.evaluate_faithfulness(
                question=question,
                answer=generated_answer,
                context=retrieved_contexts
            )

            overclaiming = await self.evaluate_overclaiming(
                answer=generated_answer,
                context=retrieved_contexts
            )

            results["llm_judge_metrics"]["faithfulness"] = faithfulness
            results["llm_judge_metrics"]["overclaiming"] = overclaiming

        answer_relevance = await self.evaluate_answer_relevance(
            question=question,
            answer=generated_answer
        )
        results["llm_judge_metrics"]["answer_relevance"] = answer_relevance

        # Calculate summary scores
        results["summary"] = self._calculate_summary(results)

        return results

    def _calculate_summary(self, results: Dict[str, Any]) -> Dict[str, Any]:
        """Calculate summary statistics from evaluation results"""
        summary = {}

        # Retrieval summary
        if results["retrieval_metrics"]:
            summary["retrieval_score"] = results["retrieval_metrics"].get("f1_score", 0.0)

        # Generation summary
        if results["generation_metrics"]:
            summary["generation_score"] = results["generation_metrics"].get("cosine_similarity", 0.0)

        # LLM judge summary (now already 0-1 scale, no normalization needed)
        llm_metrics = results["llm_judge_metrics"]
        if llm_metrics:
            faithfulness_score = llm_metrics.get("faithfulness", {}).get("score", 0.0)  # Already 0-1
            relevance_score = llm_metrics.get("answer_relevance", {}).get("score", 0.0)  # Already 0-1

            summary["faithfulness_score"] = round(faithfulness_score, 2)
            summary["relevance_score"] = round(relevance_score, 2)

            summary["llm_judge_average"] = round(
                (faithfulness_score + relevance_score) / 2.0, 2
            )

        return summary
