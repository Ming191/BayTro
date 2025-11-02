import json
import logging
import re
from typing import Dict, Any, Optional
from langchain_openai import ChatOpenAI
from langchain.prompts import ChatPromptTemplate
from config import OPENAI_API_KEY, OPENAI_MODEL

logger = logging.getLogger(__name__)


class RoleValidatorService:
    def __init__(self):
        self.llm = ChatOpenAI(
            model=OPENAI_MODEL,
            temperature=0.1,
            api_key=OPENAI_API_KEY
        )

        # Define the validation prompt
        self.validation_prompt = ChatPromptTemplate.from_messages([
            ("system", """Bạn là chuyên gia phân tích ngữ nghĩa cho hệ thống tư vấn luật nhà ở.

NHIỆM VỤ: Phân tích câu hỏi và xác định xem câu hỏi có phù hợp với vai trò của người dùng không.

VAI TRÒ:
- **CHỦ NHÀ (landlord)**: Người cho thuê nhà, có quyền quản lý tài sản, thu tiền thuê
- **NGƯỜI THUÊ (tenant)**: Người thuê nhà, trả tiền thuê, có quyền lợi được bảo vệ

QUY TẮC PHÂN TÍCH - ĐỌC KỸ:

1. **Xác định chủ thể hành động trong câu hỏi**:
   - "Tôi có thể..." → Người hỏi muốn TỰ MÌNH làm
   - "Tôi có quyền yêu cầu [ai đó]..." → Người hỏi muốn yêu cầu người khác
   - "[Ai đó] có thể..." → Hỏi về quyền của người khác

2. **Kiểm tra MÂU THUẪN LOGIC**:
   - CHỦ NHÀ không thể "yêu cầu chủ nhà" (họ chính là chủ nhà!)
   - CHỦ NHÀ không thể "trả tiền thuê" (họ là người nhận tiền)
   - NGƯỜI THUÊ không thể "yêu cầu người thuê" (họ chính là người thuê!)
   - NGƯỜI THUÊ không thể "tăng giá thuê" (không có quyền này)

3. **Các câu hỏi HỢP LỆ**:
   - Hỏi về QUYỀN/NGHĨA VỤ của chính mình
   - Hỏi về QUYỀN/NGHĨA VỤ của bên kia (để biết)
   - Hỏi về THỦ TỤC, QUY TRÌNH pháp lý

4. **Các câu hỏi KHÔNG HỢP LỆ**:
   - Muốn làm hành động thuộc quyền của vai trò khác
   - Có mâu thuẫn logic rõ ràng (yêu cầu chính mình)

VÍ DỤ CHO CHỦ NHÀ:
* HỢP LỆ:
- "Tôi có thể tăng giá thuê không?"
- "Người thuê có quyền gì?"
- "Tôi có thể thu hồi nhà khi nào?"
- "Người thuê có thể từ chối tăng giá không?"

* KHÔNG HỢP LỆ:
- "Tôi có quyền yêu cầu chủ nhà giảm tiền không?" → MÂU THUẪN: họ chính là chủ nhà!
- "Tôi có thể không trả tiền thuê không?" → Chủ nhà không thuê nhà
- "Tôi có thể yêu cầu chủ nhà sửa chữa không?" → Họ chính là chủ nhà

VÍ DỤ CHO NGƯỜI THUÊ:
* HỢP LỆ:
- "Chủ nhà có thể tăng giá điện không?"
- "Tôi có quyền từ chối tăng giá không?"
- "Tôi có thể yêu cầu chủ nhà sửa chữa không?"
- "Chủ nhà có được đuổi tôi ra không?"

* KHÔNG HỢP LỆ:
- "Tôi có thể tăng giá thuê không?" → Không có quyền này
- "Tôi có quyền yêu cầu người thuê làm gì không?" → MÂU THUẪN: họ chính là người thuê!

QUY TẮC QUAN TRỌNG:
1. Phát hiện MÂU THUẪN LOGIC trước tiên (ví dụ: chủ nhà yêu cầu chủ nhà)
2. Câu hỏi về QUYỀN/NGHĨA VỤ để BIẾT → Luôn hợp lệ
3. Câu hỏi muốn THỰC HIỆN hành động không thuộc quyền → Không hợp lệ
4. Nếu KHÔNG CHẮC → Coi là HỢP LỆ

Trả về JSON THUẦN TÚY (không có markdown):
{{
    "is_valid": true/false,
    "action_subject": "người hỏi" hoặc "bên thứ ba" hoặc "chung",
    "question_type": "quyền lợi" hoặc "nghĩa vụ" hoặc "thủ tục" hoặc "hành động" hoặc "mâu thuẫn logic",
    "reason": "Giải thích ngắn gọn",
    "suggested_response": "Nếu không hợp lệ, gợi ý câu trả lời phù hợp"
}}"""),
            ("user", """Vai trò người dùng: {role}
Câu hỏi: {question}

Phân tích và trả về JSON.""")
        ])

    async def validate_question(
        self,
        question: str,
        user_role: str
    ) -> Dict[str, Any]:
        """
        Validate if a question is appropriate for the user's role

        Args:
            question: The user's question
            user_role: 'landlord' or 'tenant'

        Returns:
            {
                "is_valid": bool,
                "action_subject": str,
                "question_type": str,
                "reason": str,
                "suggested_response": str (optional)
            }
        """
        try:
            # Normalize role
            role_vietnamese = "CHỦ NHÀ" if user_role.lower() == "landlord" else "NGƯỜI THUÊ NHÀ"

            logger.info(f"Validating question for role: {role_vietnamese}")
            logger.info(f"Question: {question}")

            # Call LLM for validation
            response = await self.llm.ainvoke(
                self.validation_prompt.format_messages(
                    role=role_vietnamese,
                    question=question
                )
            )

            # Log raw response for debugging
            logger.info(f"Raw LLM response: {response.content[:500]}")

            # Parse JSON response - handle markdown wrapper
            content = response.content.strip()
            
            # Remove markdown code blocks if present
            if content.startswith('```'):
                json_match = re.search(r'```(?:json)?\s*({.*?})\s*```', content, re.DOTALL)
                if json_match:
                    content = json_match.group(1)
                else:
                    json_match = re.search(r'{.*}', content, re.DOTALL)
                    if json_match:
                        content = json_match.group(0)
            
            result = json.loads(content)

            logger.info(f"Validation result: is_valid={result.get('is_valid')}, type={result.get('question_type')}")

            return {
                "is_valid": result.get("is_valid", True),  # Default to True if parsing fails
                "action_subject": result.get("action_subject", "chung"),
                "question_type": result.get("question_type", "chung"),
                "reason": result.get("reason", ""),
                "suggested_response": result.get("suggested_response", "")
            }

        except json.JSONDecodeError as e:
            logger.error(f"Failed to parse LLM response: {e}")
            # Default to valid to avoid blocking legitimate questions
            return {
                "is_valid": True,
                "action_subject": "chung",
                "question_type": "chung",
                "reason": "Không thể phân tích, cho phép câu hỏi",
                "suggested_response": ""
            }
        except Exception as e:
            logger.error(f"Error in role validation: {e}", exc_info=True)
            # Default to valid to avoid blocking
            return {
                "is_valid": True,
                "action_subject": "chung",
                "question_type": "chung",
                "reason": f"Lỗi hệ thống: {str(e)}",
                "suggested_response": ""
            }

    def get_role_mismatch_response(
        self,
        question: str,
        user_role: str,
        validation_result: Dict[str, Any]
    ) -> str:
        """
        Generate a helpful response when a question is not appropriate for the user's role

        Args:
            question: The original question
            user_role: The user's role
            validation_result: The validation result from validate_question

        Returns:
            A friendly response explaining why the question is not appropriate
        """
        role_name = "chủ nhà" if user_role.lower() == "landlord" else "người thuê nhà"
        opposite_role = "người thuê nhà" if user_role.lower() == "landlord" else "chủ nhà"

        # Use suggested response if available
        if validation_result.get("suggested_response"):
            return validation_result["suggested_response"]

        # Generate default response based on question type
        base_response = f"""Với vai trò là **{role_name}**, câu hỏi này có vẻ không phù hợp với quyền hạn của bạn.

**Lý do**: {validation_result.get('reason', 'Hành động này thuộc quyền hạn của bên kia')}

**Gợi ý**:
"""

        if user_role.lower() == "tenant":
            base_response += f"""- Nếu bạn muốn biết **quyền lợi của mình** khi {opposite_role} làm điều này, hãy hỏi: "Nếu {opposite_role} làm... thì tôi có quyền gì?"
- Nếu bạn muốn biết **{opposite_role} có được làm điều này không**, hãy hỏi: "{opposite_role.capitalize()} có thể... không?"

Tôi vẫn có thể giúp bạn hiểu về quyền lợi và nghĩa vụ của mình theo luật nhà ở! Hãy thử hỏi lại theo cách khác nhé. """
        else:
            base_response += f"""- Nếu bạn muốn biết **nghĩa vụ của {opposite_role}**, hãy hỏi: "{opposite_role.capitalize()} có nghĩa vụ gì?"
- Nếu bạn muốn biết **quyền hạn của mình với tư cách chủ nhà**, hãy hỏi rõ hơn về tình huống cụ thể.

Tôi sẵn sàng tư vấn về quyền và trách nhiệm của bạn theo luật! """

        return base_response

