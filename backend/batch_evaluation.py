import requests
import json
import time
import pandas as pd
from datetime import datetime
from typing import List, Dict, Any
from pathlib import Path

BASE_URL = "http://localhost:8000/api/evaluation"
DATASET_PATH = "data/evaluation_dataset.json"


class BatchEvaluator:

    def __init__(self, base_url: str = BASE_URL):
        self.base_url = base_url
        self.results = []

    def load_dataset(self, file_path: str) -> List[Dict]:
        with open(file_path, 'r', encoding='utf-8') as f:
            data = json.load(f)
        return data

    def evaluate_batch(self, test_cases: List[Dict], user_role: str = "landlord"):
        print("="*80 + "\n")

        start_time = time.time()
        self.results = []

        for idx, test_case in enumerate(test_cases, 1):

            try:
                result = self._evaluate_single(test_case, user_role)
                self.results.append(result)

                summary = result.get('summary', {})
                if summary:
                    scores = []
                    if 'retrieval_score' in summary:
                        scores.append(f"R:{summary['retrieval_score']:.2f}")
                    if 'generation_score' in summary:
                        scores.append(f"G:{summary['generation_score']:.2f}")
                    if 'faithfulness_score' in summary:
                        scores.append(f"F:{summary['faithfulness_score']:.2f}")
                    print(f"✓ [{' '.join(scores)}]")
                else:

                if idx % 10 == 0:
                    print(f"   ... checkpoint at {idx}/{len(test_cases)}, elapsed: {time.time()-start_time:.1f}s")
                    time.sleep(1)
                else:
                    time.sleep(0.3)

            except Exception as e:
                print(f"✗ Error: {str(e)[:50]}")
                self.results.append({
                    'test_case_id': test_case.get('id', idx-1),
                    'question': test_case.get('question', ''),
                    'error': str(e),
                    'success': False
                })

        elapsed = time.time() - start_time
        successful = sum(1 for r in self.results if r.get('success', True))

        print("="*80 + "\n")

        return self.results

    def _evaluate_single(self, test_case: Dict, user_role: str) -> Dict:
        payload = {
            "question": test_case['question'],
            "ground_truth_node_ids": test_case.get('ground_truth_node_ids'),
            "reference_answer": test_case.get('reference_answer'),
            "user_role": user_role,
            "auto_generate": True,
            "question_type": test_case.get('question_type')  # Pass question type
        }

        response = requests.post(
            f"{self.base_url}/complete",
            json=payload,
            timeout=120
        )
        response.raise_for_status()

        result = response.json()
        result['success'] = True
        result['test_case_id'] = test_case.get('id')
        result['question_type'] = test_case.get('question_type')
        return result

    def export_to_excel(self, output_file: str = None):
        if not self.results:
            return

        if output_file is None:
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            output_file = f"evaluation_results_{timestamp}.xlsx"


        try:
            with pd.ExcelWriter(output_file, engine='openpyxl') as writer:
                summary_data = []
                for idx, r in enumerate(self.results, 1):
                    if not r.get('success', True):
                        continue

                    question_type = r.get('question_type', 'N/A')

                    if question_type == 'irrelevant':
                        llm_judge = r.get('llm_judge_metrics', {})
                        rejection_score = llm_judge.get('irrelevant_rejection', {}).get('score')

                        summary_data.append({
                            'ID': r.get('test_case_id', idx),
                            'Question': r.get('question', '')[:80],
                            'Question_Type': question_type,
                            'Rejection_Score': rejection_score,
                            'Precision': None,
                            'Recall': None,
                            'F1_Score': None,
                            'Hit_Rate': None,
                            'Faithfulness': None,
                            'Answer_Relevance': None,
                            'Cosine_Similarity': None,
                        })
                    else:
                        retrieval = r.get('retrieval_metrics', {})
                        generation = r.get('generation_metrics', {})
                        llm_judge = r.get('llm_judge_metrics', {})

                        summary_data.append({
                            'ID': r.get('test_case_id', idx),
                            'Question': r.get('question', '')[:80],
                            'Question_Type': question_type,
                            'Rejection_Score': None,
                            'Precision': retrieval.get('precision'),
                            'Recall': retrieval.get('recall'),
                            'F1_Score': retrieval.get('f1_score'),
                            'Hit_Rate': retrieval.get('hit_rate'),
                            'Faithfulness': llm_judge.get('faithfulness', {}).get('score'),
                            'Answer_Relevance': llm_judge.get('answer_relevance', {}).get('score'),
                            'Cosine_Similarity': generation.get('cosine_similarity'),
                        })

                df_summary = pd.DataFrame(summary_data)

                if not df_summary.empty:
                    relevant_df = df_summary[df_summary['Question_Type'] != 'irrelevant']
                    irrelevant_df = df_summary[df_summary['Question_Type'] == 'irrelevant']

                    avg_row = {'ID': 'AVERAGE', 'Question': 'Average of all scores', 'Question_Type': 'ALL'}

                    for col in ['Precision', 'Recall', 'F1_Score', 'Hit_Rate', 'Faithfulness', 'Answer_Relevance', 'Cosine_Similarity']:
                        if not relevant_df.empty and col in relevant_df.columns:
                            avg_row[col] = relevant_df[col].mean()
                        else:
                            avg_row[col] = None

                    if not irrelevant_df.empty:
                        avg_row['Rejection_Score'] = irrelevant_df['Rejection_Score'].mean()
                    else:
                        avg_row['Rejection_Score'] = None

                    df_summary = pd.concat([df_summary, pd.DataFrame([avg_row])], ignore_index=True)

                df_summary.to_excel(writer, sheet_name='Summary', index=False)

                if not df_summary.empty and 'Question_Type' in df_summary.columns:
                    df_for_qtype = df_summary[df_summary['ID'] != 'AVERAGE'].copy()

                    if len(df_for_qtype) > 0 and df_for_qtype['Question_Type'].nunique() > 1:
                        stats_dict = {}

                        relevant_types = df_for_qtype[df_for_qtype['Question_Type'] != 'irrelevant']
                        if not relevant_types.empty:
                            for qtype in relevant_types['Question_Type'].unique():
                                qtype_data = relevant_types[relevant_types['Question_Type'] == qtype]
                                stats_dict[qtype] = {
                                    'Count': len(qtype_data),
                                    'Avg_Precision': qtype_data['Precision'].mean(),
                                    'Avg_Recall': qtype_data['Recall'].mean(),
                                    'Avg_F1': qtype_data['F1_Score'].mean(),
                                    'Avg_Hit_Rate': qtype_data['Hit_Rate'].mean(),
                                    'Avg_Faithfulness': qtype_data['Faithfulness'].mean(),
                                    'Avg_Relevance': qtype_data['Answer_Relevance'].mean(),
                                    'Avg_Cosine_Sim': qtype_data['Cosine_Similarity'].mean(),
                                }

                        irrelevant_types = df_for_qtype[df_for_qtype['Question_Type'] == 'irrelevant']
                        if not irrelevant_types.empty:
                            stats_dict['irrelevant'] = {
                                'Count': len(irrelevant_types),
                                'Avg_Rejection_Score': irrelevant_types['Rejection_Score'].mean(),
                                'Rejection_Rate': (irrelevant_types['Rejection_Score'] == 1).sum() / len(irrelevant_types),
                            }

                        df_qtype_stats = pd.DataFrame.from_dict(stats_dict, orient='index')
                        df_qtype_stats.to_excel(writer, sheet_name='By_Question_Type')

                answers_data = []
                for idx, r in enumerate(self.results, 1):
                    if not r.get('success', True):
                        continue
                    answers_data.append({
                        'ID': r.get('test_case_id', idx),
                        'Question': r.get('question', ''),
                        'Generated_Answer': r.get('generated_answer', ''),
                        'Question_Type': r.get('question_type', 'N/A'),
                    })

                df_answers = pd.DataFrame(answers_data)
                df_answers.to_excel(writer, sheet_name='Generated_Answers', index=False)

                explanations_data = []
                for idx, r in enumerate(self.results, 1):
                    if not r.get('success', True):
                        continue

                    question_type = r.get('question_type', 'N/A')
                    llm_judge = r.get('llm_judge_metrics', {})

                    if question_type == 'irrelevant':
                        # For irrelevant questions, show rejection explanation
                        rejection_data = llm_judge.get('irrelevant_rejection', {})
                        explanations_data.append({
                            'ID': r.get('test_case_id', idx),
                            'Question': r.get('question', '')[:60],
                            'Question_Type': question_type,
                            'Rejection_Score': rejection_data.get('score'),
                            'Rejection_Explanation': rejection_data.get('explanation', ''),
                            'Rejection_Detected': rejection_data.get('rejection_detected', False),
                            'Rejection_Phrases': ', '.join(rejection_data.get('rejection_phrases', [])),
                        })
                    else:
                        faithfulness_exp = llm_judge.get('faithfulness', {}).get('explanation', '')
                        relevance_exp = llm_judge.get('answer_relevance', {}).get('explanation', '')

                        explanations_data.append({
                            'ID': r.get('test_case_id', idx),
                            'Question': r.get('question', '')[:60],
                            'Question_Type': question_type,
                            'Faithfulness_Score': llm_judge.get('faithfulness', {}).get('score'),
                            'Faithfulness_Explanation': faithfulness_exp,
                            'Relevance_Score': llm_judge.get('answer_relevance', {}).get('score'),
                            'Relevance_Explanation': relevance_exp,
                        })

                df_explanations = pd.DataFrame(explanations_data)
                df_explanations.to_excel(writer, sheet_name='LLM_Judge_Explanations', index=False)

            print(f"Exported successfully to: {output_file}")
            return output_file

        except Exception as e:
            print(f"Error exporting to Excel: {e}")

            csv_file = output_file.replace('.xlsx', '.csv')
            if 'df_summary' in locals():
                df_summary.to_csv(csv_file, index=False, encoding='utf-8-sig')
                print(f"Exported to CSV: {csv_file}")
            return csv_file

    def export_to_json(self, output_file: str = None):
        """Export raw results to JSON"""
        if output_file is None:
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            output_file = f"evaluation_results_{timestamp}.json"

        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump({
                'metadata': {
                    'total_cases': len(self.results),
                    'successful_cases': sum(1 for r in self.results if r.get('success', True)),
                    'timestamp': datetime.now().isoformat()
                },
                'results': self.results
            }, f, ensure_ascii=False, indent=2)

        print(f"Exported JSON to: {output_file}")
        return output_file

    def print_summary(self):
        """In tổng kết"""
        if not self.results:
            print("Chưa có kết quả!")
            return

        successful = [r for r in self.results if r.get('success', True)]

        print("\n" + "="*80)
        print("="*80)
        print(f"Total test cases: {len(self.results)}")
        print(f"Successful: {len(successful)}")
        print(f"Failed: {len(self.results) - len(successful)}")

        if successful:
            retrieval_scores = [r.get('summary', {}).get('retrieval_score') for r in successful if r.get('summary', {}).get('retrieval_score') is not None]
            generation_scores = [r.get('summary', {}).get('generation_score') for r in successful if r.get('summary', {}).get('generation_score') is not None]
            faithfulness_scores = [r.get('summary', {}).get('faithfulness_score') for r in successful if r.get('summary', {}).get('faithfulness_score') is not None]
            relevance_scores = [r.get('summary', {}).get('relevance_score') for r in successful if r.get('summary', {}).get('relevance_score') is not None]
            rejection_scores = [r.get('summary', {}).get('irrelevant_rejection_score') for r in successful if r.get('summary', {}).get('irrelevant_rejection_score') is not None]

            print(f"\nAverage Scores:")
            if retrieval_scores:
                print(f"   Retrieval F1: {sum(retrieval_scores)/len(retrieval_scores):.4f}")
            if generation_scores:
                print(f"   Cosine Similarity: {sum(generation_scores)/len(generation_scores):.4f}")
            if faithfulness_scores:
                print(f"   Faithfulness: {sum(faithfulness_scores)/len(faithfulness_scores):.2f}")
            if relevance_scores:
                print(f"   Relevance: {sum(relevance_scores)/len(relevance_scores):.2f}")
            if rejection_scores:
                print(f"   Rejection (Irrelevant): {sum(rejection_scores)/len(rejection_scores):.2f}")

        print("="*80 + "\n")


def main():
    """Main function"""
    print("\n" + "="*80)
    print("EVALUATION FOR DATASETS")
    print("="*80 + "\n")
    evaluator = BatchEvaluator()

    print("Checking server connection...")
    if not evaluator.check_health():
        print("\nCannot connect to server. Exiting.")
        return

    dataset_path = Path(DATASET_PATH)
    if not dataset_path.exists():
        print(f"Dataset not found: {DATASET_PATH}")
        return

    test_cases = evaluator.load_dataset(DATASET_PATH)

    if not test_cases:
        print("Dataset is empty!")
        return

    print(f"\n⚡ Ready to evaluate {len(test_cases)} test cases")
    print(f"   Estimated time: ~{len(test_cases) * 15 / 60:.1f} minutes")
    user_role = "tenant"

    print(f"\nStarting evaluation with role: {user_role}\n")
    evaluator.evaluate_batch(test_cases, user_role)

    evaluator.print_summary()

    excel_file = evaluator.export_to_excel()
    json_file = evaluator.export_to_json()


if __name__ == "__main__":
    main()
