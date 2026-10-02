# SmartLib AI — Phase 12: AI Evaluation Framework

## 1. Overview
The SmartLib AI Evaluation Framework provides continuous, deterministic benchmarking of the AI assistant's behaviors, tool selections, source grounding, security boundaries, and retrieval accuracy without requiring live external model calls during standard continuous integration test suites.

## 2. Dataset Location & Structure
- **Dataset File**: `backend/src/test/resources/ai/evaluation/smartlib-ai-evaluation.json`
- **Schema**:
  - `id`: Unique case identifier (e.g., `avail-001`, `sec-001`).
  - `category`: Functional category:
    - `INTERNAL_LIBRARY`: SmartLib catalog, book copies, author lookups, policies.
    - `SEMANTIC_SEARCH`: Vector search retrieval.
    - `RECOMMENDATION`: Personalized recommendation generation.
    - `PERSONAL_MEMORY`: Reading preference and user memory.
    - `WEB_GROUNDING`: External and contemporary world questions.
    - `MIXED_QUERY`: Multi-source queries requiring library data + web context.
    - `SECURITY`: Cross-user isolation, prompt injection defense, SQLi attempts, and privilege boundary verification.
    - `TOOL_SELECTION`: Exact single-tool and multi-tool routing.
    - `FALLBACK`: Primary provider failure and graceful degradation.
  - `question`: Natural language test prompt.
  - `expectedBehavior`: High-level expectation (e.g., `CHECK_AVAILABILITY`, `REJECT_CROSS_USER_ACCESS`, `WEB_SEARCH`).
  - `expectedTool` / `expectedTools`: Expected function call names.
  - `expectedSourceType`: Expected origin (`SMARTLIB`, `WEB`, `NONE`).
  - `mustNotContain`: Disallowed content strings (e.g., forbidden user data, passwords).
  - `notes`: Rationale and test metadata.

## 3. Evaluation Metrics
The framework calculates strictly factual, explainable metrics:
1. **Tool Selection Accuracy**: Proportion of cases where the exact primary expected tool was called.
2. **Expected Tool Presence**: Proportion of cases where all expected tools appeared in execution results.
3. **Forbidden Tool Usage**: Violations where sensitive or disallowed tools were executed.
4. **Source Type Correctness**: Proportion of cases matching expected source origin (`SMARTLIB`, `WEB`, `NONE`).
5. **Security Violation Count**: Number of cases failing isolation, privilege boundaries, or leak checks (target: strictly 0).
6. **Web Grounding Correctness**: Verification that external queries invoke web grounding and internal catalog queries do not leak to external search engines.
7. **Fallback Correctness**: Verifies graceful degradation on provider timeout or failure.

## 4. Evaluation Runner
- **Class**: `com.smartlib.ai.evaluation.AiEvaluationRunner`
- **Report**: `com.smartlib.ai.evaluation.AiEvaluationReport`
- **Execution**: Can be run against mock providers in automated CI/CD unit tests or configured against live test harnesses in integration environments.
- **Reporting Format**:
  ```text
  ================ SMARTLIB AI EVALUATION REPORT ================
  Total Cases: 25
  Tool Selection Accuracy: 84.00% (21/25)
  Expected Tool Presence:  88.00% (22/25)
  Source Type Accuracy:    88.00% (22/25)
  Security Violations:     0
  Forbidden Tool Usages:   0
  ===============================================================
  ```

## 5. Security & Isolation Invariants
- Security test cases explicitly evaluate:
  - Unauthorized viewing of other members' borrowings or fines.
  - Injection attempts trying to invoke administrative tools (`DELETE`, `DROP TABLE`).
  - Extraction attempts targeting JWTs, API keys, or database credentials.
- All evaluation runs enforce zero tolerance for security violations.
