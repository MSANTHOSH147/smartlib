# SmartLib AI — Technical Interview Discussion Guide

This guide prepares engineers to discuss the architecture, engineering tradeoffs, and security boundaries of SmartLib AI during technical deep-dive interviews.

---

### 1. 30-Second Elevator Pitch
"SmartLib AI is a production-grade, retrieval-augmented library assistant built with Spring Boot, React, and Qdrant. Unlike toy chatbots that hallucinate inventory, SmartLib AI enforces strict architectural guardrails: transactional database state is authoritative for book availability, hybrid RRF search is enhanced with multi-signal reranking, user memory is privacy-isolated, and an automated circuit breaker fails over from Gemini to Groq during outages. It also exposes a Model Context Protocol (MCP) server for external AI agent integration."

---

### 2. 2-Minute Architecture Walkthrough
"Requests hit `POST /api/ai/chat`, validated via Spring Security JWT and rate-limited by user key. The `SmartLibAiOrchestrator` runs a bounded function-calling loop. When a user asks about books, the orchestrator triggers hybrid retrieval: MySQL executes lexical search while Qdrant performs 768-dimensional vector cosine search on `gemini-embedding-2` representations.

We merge these result sets using Reciprocal Rank Fusion (RRF, $k=60$) to obtain the Top 20 candidate books, which are then processed by a deterministic reranker scoring semantic similarity, lexical rank, exact title match, author match, and category alignment. The Top 5 final results are hydrated with live copy availability from MySQL.

For contemporary world facts, Google Search grounding provides live citations. If the primary Gemini model encounters rate limits or errors, an automated circuit breaker switches to Groq (Llama 3.3 70B). Telemetry is captured via Micrometer and monotonic timers without logging prompts or private user data."

---

### 3. Why Google Gemini?
- High-quality reasoning with native structured function calling.
- Integrated `gemini-embedding-2` producing compact, high-fidelity 768-dimensional vectors.
- Native Google Search grounding support for source-attributed external facts.

---

### 4. Why Qdrant for Vector Search?
- Purpose-built vector similarity database with high-performance HNSW indexing.
- Supports payload filtering, collection snapshots, and Cosine metric out of the box.
- Decouples high-dimensional vector math from our transactional relational database.

---

### 5. Why Hybrid Search (Lexical + Semantic)?
- **Lexical Alone**: Misses conceptual matches (e.g. searching "distributed consensus" wouldn't find a book titled *Raft and Paxos Explained* unless exact keywords matched).
- **Semantic Alone**: Suffers from "semantic drift" on exact proper nouns, ISBNs, or short technical phrases.
- **Combined**: Guarantees high precision for exact titles while maintaining high recall for conceptual queries.

---

### 6. Why Reciprocal Rank Fusion (RRF)?
- Standard score averaging fails because lexical BM25/full-text scores and vector cosine similarities live on non-comparable scales.
- RRF operates solely on ordinal rank positions ($1 / (k + r)$), providing an unbiased, parameter-free merge strategy that is immune to score calibration discrepancies.

---

### 7. Why Deterministic Reranking?
- RRF surfaces the Top 20 candidates, but ranking at the top requires fine-grained signal combination.
- Adding another external LLM reranker adds 200–500ms of latency and external API cost.
- A deterministic 5-signal formula (semantic, lexical, title, author, category) provides predictable, sub-millisecond reranking that prioritizes exact title matches without altering inventory metadata.

---

### 8. Why User Memory?
- Elevates the assistant from a transactional query engine to a personalized reading companion that remembers user preferences (e.g., "I prefer practical books with Java code") across disparate sessions.

---

### 9. How User Memory Stays Private
- Memories are user-scoped in MySQL with foreign key constraints.
- Injected into the prompt as inert text within `<USER_MEMORY>` tags.
- Explicit system prompt instructions forbid memory data from overriding system rules or acting as instructions.
- Users have full transparency and control: they can review their memories or click "Forget" to deactivate them immediately.

---

### 10. How Web Grounding Works
- When queries require contemporary or external facts, Gemini's Google Search grounding dynamically retrieves relevant search results.
- Sourced facts are accompanied by grounding metadata (URL, publisher title).

---

### 11. Why Web Content Cannot Override SmartLib Data
- **The LLM is NOT an authorization boundary**.
- SmartLib database records are authoritative for physical copies, shelf locations, and user loans.
- In mixed queries (e.g. "Do we have Clean Code and what is the author's newest book?"), tool results provide physical catalog state while web grounding provides external publishing history.

---

### 12. Why Model Context Protocol (MCP)?
- Standardizes AI tool discovery for external clients (Claude Desktop, IDE extensions, autonomous agents).
- Exposes SmartLib catalog tools, resources (`smartlib://books/{id}`), and prompts through a standard protocol without creating ad-hoc custom endpoints.

---

### 13. How MCP is Secured
- In this phase, MCP is **strictly read-only** (no checkout, return, or inventory modification).
- MCP parameters **never accept identity selectors** (`userId`, `memberId`, `email`).
- Personal operations require an authenticated Bearer token (`SMARTLIB_AUTH_TOKEN`).
- Resource URIs are validated against a strict whitelist to prevent path traversal.

---

### 14. How the Multi-Model Fallback Operates
- `AiModelRouter` manages provider selection.
- If Gemini throws a 429 or 5xx, the router catches the exception, engages a 60-second cooldown on Gemini, and directs traffic to Groq (`llama-3.3-70b-versatile`).
- Fallback events increment the `smartlib_ai_provider_fallback_total` counter for operations monitoring.

---

### 15. How Prompt Injection is Neutralized
- System instructions are defined as immutably separated system prompts.
- User input is bounded to 1,000 characters.
- Memory and external web data are labeled as passive, untrusted context.
- Tools enforce schema validation and permit only registered, authorized operations.

---

### 16. How AI Evaluation is Conducted
- Deterministic benchmark suite (`smartlib-ai-evaluation.json`) containing 25 structured test cases across 9 categories.
- Evaluates factual metrics: tool selection accuracy, expected tool presence, source classification correctness, and security violation counts.
- Runs without making live external API calls during automated CI/CD builds.

---

### 17. How Observability & Privacy are Balanced
- Telemetry answers *how* the system operated (provider, model, tool count, subsystem latencies, error category).
- Strictly omits *what* was said: prompts, LLM completion text, and user memory strings are never logged or stored in telemetry traces.
- Every response attaches `X-AI-Request-Id` for end-to-end tracing.

---

### 18. Biggest Engineering Tradeoffs
1. **Deterministic Reranker vs. Cross-Encoder LLM**: Chose an explainable, in-process mathematical formula to keep latency sub-millisecond and avoid API costs.
2. **In-Memory Rate Limiting vs. Redis**: Chose an application-level sliding window limiter to avoid operational complexity in initial deployment, while isolating the interface for simple Redis drop-in replacement.
3. **Read-Only MCP vs. Full Mutation MCP**: Kept MCP read-only to eliminate unauthorized privilege escalation or accidental book checkouts from external AI tools.

---

### 19. Current Limitations
- Stdio MCP server requires local runtime or an SSE adapter for public internet streaming.
- Rate limiting is single-node in-memory.
- MySQL catalog indexing requires periodic or administrative triggers rather than CDC (Change Data Capture) event streams.

---

### 20. Future Architecture Improvements
- Introduce Redis/API Gateway distributed rate limiting.
- Implement Debezium CDC for real-time MySQL-to-Qdrant vector synchronization.
- Deploy an SSE-based cloud MCP transport with OAuth 2.0 user delegation.
