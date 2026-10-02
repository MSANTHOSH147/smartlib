# SmartLib AI — Phase 13: Observability & Telemetry

## 1. Overview
SmartLib AI incorporates comprehensive, privacy-preserving observability and metrics instrumentation. Telemetry answers operational questions (which provider answered, whether fallbacks or web grounding occurred, tool invocations, and monotonic subsystem latencies) without ever logging or exposing sensitive user data.

## 2. Request Trace (`AiRequestTrace`)
Every request flowing through the AI layer generates an `AiRequestTrace`:
- `requestId`: Unique correlation identifier (propagated as `X-AI-Request-Id`).
- `provider`: Primary or fallback model provider (e.g., `gemini`, `groq`).
- `model`: Model identifier (e.g., `gemini-2.5-flash`, `llama-3.3-70b-versatile`).
- `fallbackUsed`: Boolean indicating whether provider fallback was engaged.
- `webGroundingUsed`: Boolean indicating external web retrieval.
- `memoryUsed`: Boolean indicating persistent memory context incorporation.
- `rerankingUsed`: Boolean indicating whether deterministic candidate reranking was executed.
- `toolCount` & `toolNames`: Executed SmartLib tool functions.
- `qdrantLatencyMs`, `mysqlLatencyMs`, `providerLatencyMs`, `totalLatencyMs`: Component-level durations.
- `success`: Boolean indicating response status.
- `errorCategory`: Categorical classification (`NONE`, `VALIDATION`, `AUTHENTICATION`, `AUTHORIZATION`, `RATE_LIMIT`, `PROVIDER`, `TOOL`, `DATABASE`, `VECTOR_DB`, `WEB_GROUNDING`, `INTERNAL`).
- `timestamp`: Monotonic event creation timestamp.

## 3. Privacy Guarantees
The observability subsystem enforces strict privacy firewalls:
- **No Prompt Logging**: User query strings and conversation messages are never written to trace objects or log outputs.
- **No Output Logging**: LLM raw text replies and generated content are excluded from traces.
- **No Memory Content Leakage**: Memory content strings are omitted.
- **No Credentials or Secrets**: Passwords, JWTs, API keys, and database connection strings are prohibited.
- **No User IDs in Metric Dimensions**: Metric labels only use categorical identifiers (`provider`, `model`, `success`, `error_category`, `tool`). No user IDs or email addresses are attached to metric dimensions.

## 4. Structured Logging
Completed requests produce a single structured event:
```text
AI_REQUEST_COMPLETED requestId=7df590d9-b024-4f01-8ee2-78dcb1f4d924 provider=gemini model=gemini-2.5-flash fallback=false webGrounding=false memoryUsed=true reranking=true toolCount=1 totalLatencyMs=342 success=true errorCategory=NONE
```

## 5. Micrometer Metrics
Registered metrics include:
- `smartlib_ai_requests_total`: Counter tracking total AI requests by provider, model, success, and error category.
- `smartlib_ai_request_failures_total`: Counter tracking failures by provider and error category.
- `smartlib_ai_provider_requests_total`: Counter tracking provider-level calls.
- `smartlib_ai_provider_fallback_total`: Counter tracking provider fallback triggers.
- `smartlib_ai_tool_calls_total`: Counter tracking tool function executions.
- `smartlib_ai_web_grounding_total`: Counter tracking grounding queries.
- `smartlib_ai_memory_usage_total`: Counter tracking user memory access.
- `smartlib_ai_reranking_total`: Counter tracking search reranker runs.
- `smartlib_ai_request_duration`: Timer measuring total chat request duration.
- `smartlib_ai_provider_duration`: Timer measuring LLM provider response latency.
- `smartlib_ai_qdrant_duration`: Timer measuring Qdrant vector search latency.
- `smartlib_ai_mysql_duration`: Timer measuring MySQL search latency.

## 6. Monotonic Timing & Response Headers
All durations use high-precision monotonic timing (`System.nanoTime()`). Successful and handled responses attach the `X-AI-Request-Id` HTTP header to facilitate end-to-end tracing and client issue correlation without leaking server internals.
