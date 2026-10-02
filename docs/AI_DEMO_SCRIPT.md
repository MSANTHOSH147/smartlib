# SmartLib AI — 5–8 Minute Production Demonstration Script

This script provides an interactive demonstration walkthrough showcasing the multi-model architecture, hybrid retrieval, user memory, web grounding, and MCP integration.

---

### Step 1: Member Authentication (30 seconds)
1. Open the browser to `http://localhost:5173` (or production URL `https://smartlib-frontend-nmml.onrender.com`).
2. Log in with a member account (e.g. `member@smartlib.com` / `password123`).
3. Point out the floating AI Assistant button on the bottom right and the dedicated `/ai` navigation link.

---

### Step 2: Live Catalog Availability & Tool Execution (45 seconds)
1. Open the AI Assistant modal or page.
2. Prompt:
   > *"Do we have Clean Code available right now?"*
3. **Show**:
   - The indicator showing tool `checkBookAvailability` executing.
   - The exact response stating available copy count, total copies, and shelf location.
   - Point out that the AI did not hallucinate: it consulted authoritative MySQL database state.

---

### Step 3: Personalized Recommendations (45 seconds)
1. Prompt:
   > *"Recommend a good programming book for me to read next."*
2. **Show**:
   - Tool `getPersonalizedRecommendations` executing.
   - Recommendations tailored to previous borrowings and popular catalog titles with live availability badges.

---

### Step 4: Active User Memory & Transparency (60 seconds)
1. Prompt:
   > *"Remember that I prefer practical books with concrete Java code examples."*
2. **Show**:
   - Tool `rememberPreference` executing.
   - The assistant confirms the preference is saved.
3. Switch to the **Memory** tab in the UI:
   - Point out the active memory card with its timestamp and privacy badge.
   - Explain that users retain full ownership and can click **"Forget"** at any time.

---

### Step 5: Real-Time Web Grounding (45 seconds)
1. Prompt:
   > *"What is the latest LTS release of Java and what are its key features?"*
2. **Show**:
   - Web grounding activating via Google Search.
   - The accurate, current response.
   - Point out the interactive source citation tag (`Google Search / Oracle`) linking directly to the verified external source with `rel="noopener noreferrer"`.

---

### Step 6: Mixed Query & Domain Boundary Separation (60 seconds)
1. Prompt:
   > *"Do we have Clean Code in the library, and what is the latest book published by Robert C. Martin?"*
2. **Show**:
   - The assistant executing internal tools for physical library inventory and web grounding for external publishing history.
   - Highlight the clear demarcation: internal copies are sourced from the library database, while external author facts are sourced from the web.

---

### Step 7: Model Context Protocol (MCP) Integration (45 seconds)
1. Open terminal in `mcp-server/`.
2. Demonstrate calling the tool via MCP stdio:
   ```bash
   node dist/index.js
   ```
3. Explain that external AI clients (Claude Desktop, autonomous agents) can discover tools (`search_books`, `check_book_availability`) and catalog resources (`smartlib://books/1`) through standardized JSON-RPC protocols.

---

### Step 8: Observability, Request Tracing & Multi-Model Fallback (45 seconds)
1. Open DevTools Network tab and inspect the response headers of the last `/api/ai/chat` request:
   - Show `X-AI-Request-Id: <uuid>`.
2. Show backend logs:
   ```text
   AI_REQUEST_COMPLETED requestId=... provider=gemini model=gemini-2.5-flash fallback=false webGrounding=true memoryUsed=true reranking=true toolCount=1 totalLatencyMs=412 success=true
   ```
3. Highlight that **no prompts, no passwords, and no user memories are logged**.
4. Conclude with a summary of the circuit breaker: if Gemini experiences downtime, the system automatically routes to Groq (Llama 3.3 70B) within seconds.
