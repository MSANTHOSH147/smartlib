# SmartLib AI — Production Environment Variables Matrix

## 1. Overview
This matrix documents all environment variables used across SmartLib backend, frontend, vector database, and Model Context Protocol (MCP) integrations.

> **CRITICAL SECURITY RULE**: Never store actual secrets in documentation, repository files, or client-side assets. All credentials must be provisioned through secure runtime environment variables in Render, AWS, or local `.env` files (which must remain in `.gitignore`).

---

## 2. Environment Variables Matrix

| Category | Variable Name | Required | Development Default | Production Requirement | Purpose |
| :--- | :--- | :---: | :--- | :--- | :--- |
| **Server** | `PORT` | Optional | `10000` | Provided by Render (`10000`) | Web server listening port |
| | `ENVIRONMENT` | Optional | `development` | `production` | Enforces strict startup validations |
| | `FRONTEND_URL` | Optional | `http://localhost:5173` | `https://smartlib-frontend-nmml.onrender.com` | Allowed CORS origin for browser requests |
| **Database** | `DB_URL` | Required (Prod) | `jdbc:mysql://localhost:3306/smartlib?...` | `jdbc:mysql://<aiven-host>:<port>/defaultdb?...` | MySQL connection JDBC URL |
| | `DB_USERNAME` | Required (Prod) | `root` | Aiven MySQL username | Authoritative database user |
| | `DB_PASSWORD` | Required (Prod) | *(empty)* | Secure Aiven password | Database authentication secret |
| | `JPA_SHOW_SQL` | Optional | `false` | `false` | Controls whether SQL statements are logged |
| **Security / JWT** | `JWT_SECRET` | **Required** | Local 256-bit test key | Random 256-bit+ HMAC-SHA key (min 32 chars) | Signs & verifies user session JWTs |
| | `JWT_EXPIRATION` | Optional | `86400000` (24h) | `86400000` (24h) | Token lifespan in milliseconds |
| **Primary AI (Gemini)** | `GEMINI_API_KEY` | **Required** | *(empty)* | Valid Google AI Studio API Key | Authoritative primary LLM & embeddings |
| | `GEMINI_MODEL` | Optional | `gemini-3.8-flash` | `gemini-3.8-flash` or `gemini-2.5-flash` | Primary chat LLM model identifier |
| | `GEMINI_TIMEOUT_SECONDS` | Optional | `30` | `30` | HTTP client timeout for Gemini calls |
| | `GEMINI_ENABLED` | Optional | `true` | `true` | Master switch for Gemini provider |
| **Fallback AI (Groq)** | `GROQ_API_KEY` | Recommended | *(empty)* | Valid Groq Cloud API Key | Secondary fallback LLM provider |
| | `GROQ_BASE_URL` | Optional | `https://api.groq.com/openai/v1` | `https://api.groq.com/openai/v1` | Groq OpenAI-compatible API base URL |
| | `GROQ_MODEL` | Optional | `llama-3.3-70b-versatile` | `llama-3.3-70b-versatile` | Fallback chat model identifier |
| | `GROQ_ENABLED` | Optional | `true` | `true` | Master switch for Groq fallback provider |
| | `GROQ_TIMEOUT` | Optional | `30` | `30` | Timeout in seconds for Groq calls |
| **AI Routing** | `AI_PRIMARY_PROVIDER` | Optional | `gemini` | `gemini` | Target primary provider name |
| | `AI_FALLBACK_PROVIDER`| Optional | `groq` | `groq` | Target fallback provider name |
| | `AI_ROUTING_COOLDOWN_SECONDS` | Optional | `60` | `60` | Duration provider stays in cooldown on error |
| **Vector DB (Qdrant)** | `QDRANT_URL` / `QDRANT_HOST` | **Required** | `http://localhost:6333` | Cloud Qdrant cluster endpoint | REST API endpoint for vector operations |
| | `QDRANT_API_KEY` | Optional (Local) / Required (Cloud) | *(empty)* | Cloud Qdrant cluster API key | Vector database authorization key |
| | `QDRANT_COLLECTION` | Optional | `smartlib_books` | `smartlib_books` | Collection name for book embeddings |
| **Embedding Engine** | `GEMINI_EMBEDDING_MODEL` | Optional | `gemini-embedding-2` | `gemini-embedding-2` | Text embedding model identifier |
| | `GEMINI_EMBEDDING_DIMENSION` | Optional | `768` | `768` | Fixed vector dimension (Cosine metric) |
| **Web Grounding** | `AI_WEB_GROUNDING_ENABLED` | Optional | `true` | `true` | Toggles Google Search grounding in Gemini |
| | `AI_WEB_GROUNDING_MAX_RESULTS` | Optional | `5` | `5` | Max external citations to evaluate |
| **User Memory** | `AI_MEMORY_ENABLED` | Optional | `true` | `true` | Enables persistent preference retention |
| | `AI_MEMORY_MIN_CONFIDENCE` | Optional | `0.75` | `0.75` | Minimum confidence score to store memory |
| | `AI_MEMORY_MAX_CONTEXT_MEMORIES` | Optional | `10` | `10` | Max active user memories in chat context |
| **Reranking** | `AI_RERANKING_ENABLED` | Optional | `true` | `true` | Activates post-RRF candidate reranking |
| | `AI_RERANKING_CANDIDATE_LIMIT` | Optional | `20` | `20` | RRF candidates passed to reranker |
| | `AI_RERANKING_FINAL_LIMIT` | Optional | `5` | `5` | Final top books returned to user |
| **Rate Limiting** | `AI_RATE_LIMIT_PER_MINUTE` | Optional | `10` | `10` | Max chat requests per minute per user |
| **Observability** | `MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE` | Optional | `health,info,metrics` | `health,info,metrics` | Exposes safe actuator endpoints |
| **Mail (SMTP)** | `MAIL_HOST` | Optional | `smtp.gmail.com` | `smtp.gmail.com` | Outbound notification mail server |
| | `MAIL_PORT` | Optional | `587` | `587` | SMTP port |
| | `MAIL_USERNAME` | Optional | *(empty)* | Verified email account | Notification sender address |
| | `MAIL_PASSWORD` | Optional | *(empty)* | Google App Password | Notification SMTP credentials |
| **Frontend** | `VITE_API_URL` | Required | `http://localhost:8080/api` | `https://smartlib-backend-04o3.onrender.com/api` | Base URL for REST API communication |
| **MCP Server** | `SMARTLIB_API_BASE_URL` | Optional | `http://localhost:8080` | `https://smartlib-backend-04o3.onrender.com` | Target backend REST service for MCP gateway |
| | `SMARTLIB_AUTH_TOKEN` | Optional | *(empty)* | User JWT Bearer Token | User session token for personal MCP tools |
