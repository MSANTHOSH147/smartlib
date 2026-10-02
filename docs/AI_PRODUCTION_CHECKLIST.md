# SmartLib AI — Production Deployment Checklist

## 1. Infrastructure Readiness
- [x] **Aiven MySQL Reachability**: Production MySQL connection parameters (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`) verified. SSL enabled for cloud database connections.
- [x] **Qdrant Vector Cluster**: Qdrant Cloud cluster reachable at `QDRANT_URL` with valid `QDRANT_API_KEY`.
- [x] **Collection Verified**: `smartlib_books` collection exists with 768-dimensional Cosine vector configuration.
- [x] **Gemini API Configuration**: Valid `GEMINI_API_KEY` provisioned. Primary model set to `gemini-3.8-flash` or `gemini-2.5-flash`.
- [x] **Groq Fallback Configuration**: Valid `GROQ_API_KEY` provisioned with model `llama-3.3-70b-versatile`.
- [x] **JWT Secret Strength**: `JWT_SECRET` generated using at least 256 bits of cryptographically random entropy (min 32 characters).
- [x] **Mail Service**: SMTP parameters configured for account notifications.
- [x] **CORS Allowlist**: Allowed origins explicitly include `https://smartlib-frontend-nmml.onrender.com` and optional `FRONTEND_URL`. Wildcard (`*`) is prohibited.
- [x] **Frontend Configuration**: `VITE_API_URL` points to `https://smartlib-backend-04o3.onrender.com/api`.

---

## 2. AI Capabilities & Pipelines
- [x] **Multi-Model Provider Routing**: Primary Gemini provider configured with automatic circuit breaker failover to Groq.
- [x] **Embedding Pipeline**: `gemini-embedding-2` configured with SHA-256 change detection for incremental indexing.
- [x] **Hybrid Retrieval**: MySQL lexical search + Qdrant semantic search merged using Reciprocal Rank Fusion (RRF, $k=60$).
- [x] **Deterministic Reranker**: Post-RRF candidate pool (Top 20) scored and filtered to Top 5 final results using transparent, explainable weights.
- [x] **Personalized Recommendations**: User borrowing history and memory preferences incorporated into book recommendations.
- [x] **User Memory Isolation**: Memories stored strictly per-user with confidence filtering ($\ge 0.75$) and passive context formatting.
- [x] **Web Grounding Guardrails**: Real-time external facts sourced with attribution; web content strictly treated as untrusted data.
- [x] **Model Context Protocol (MCP)**: Read-only TypeScript stdio gateway exposing safe tools and catalog resources.

---

## 3. Security & Privacy Controls
- [x] **Per-User Rate Limiting**: Sliding window limiter (10 requests/minute default, configurable via `AI_RATE_LIMIT_PER_MINUTE`).
- [x] **Strict Authentication**: All personal tools, memory operations, and chat endpoints require valid JWT authentication.
- [x] **Role-Based Authorization**: Administrative capabilities restricted to `ROLE_ADMIN`.
- [x] **No Secret Leakage**: Passwords, JWTs, API keys, and raw prompts are excluded from logs, error payloads, and metric dimensions.
- [x] **Production Startup Validation**: `ProductionStartupValidator` enforces non-default 256-bit JWT secret on startup in production profiles.
- [x] **Safe Error Classification**: HTTP errors (400, 401, 403, 404, 429, 500) sanitize internal stack traces.

---

## 4. Operational Telemetry & Monitoring
- [x] **Request ID Tracking**: Every AI request generates a unique UUID returned in the `X-AI-Request-Id` response header.
- [x] **High-Precision Monotonic Latency**: `System.nanoTime()` measures subsystem durations (MySQL, Qdrant, provider, reranking, total).
- [x] **Micrometer Metrics**: Actuator metrics registered for request counts, failures, provider fallbacks, and tool executions.
- [x] **Actuator Security**: `/actuator/health` and `/actuator/info` are public for orchestration; `/actuator/metrics` is restricted to admins; sensitive endpoints (`env`, `beans`, `configprops`) are excluded.
- [x] **Disaster Recovery**: Database snapshots and Qdrant collection snapshots scheduled before major deployments.
