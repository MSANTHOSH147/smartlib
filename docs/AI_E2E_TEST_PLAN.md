# SmartLib AI — End-to-End (E2E) Test Plan

This test plan defines the 20 manual end-to-end verification cases for validating the full SmartLib AI release in staging and production environments.

---

### Case 1: Member Authentication
- **Step**: Log in with valid member credentials.
- **Expected**: Successful JWT generation, navigation to `/dashboard`, floating AI assistant widget becomes active.

### Case 2: General Knowledge Question
- **Prompt**: "What is the difference between synchronous and asynchronous programming?"
- **Expected**: Accurate direct response from LLM without invoking library tools. Sources list is empty.

### Case 3: Exact Book Title Search
- **Prompt**: "Do you have Clean Code in the library?"
- **Expected**: Tool `searchBooks` or `checkBookAvailability` executes. Catalog record with title and author is returned.

### Case 4: Semantic Conceptual Search
- **Prompt**: "Find books about writing clean software and refactoring code"
- **Expected**: Tool `semanticSearchBooks` executes against Qdrant. Returns relevant titles (e.g. *Clean Code*, *Refactoring*) even if keywords differ.

### Case 5: Real-Time Availability Check
- **Prompt**: "Is Clean Code available right now and which shelf is it on?"
- **Expected**: Tool `checkBookAvailability` executes. Displays exact available copy count and shelf location from live MySQL records.

### Case 6: Personalized Recommendation
- **Prompt**: "Recommend a good book for me to read next"
- **Expected**: Tool `getPersonalizedRecommendations` executes. Tailors recommendations to user's borrowing history and preferences.

### Case 7: Explicit Preference Memory Creation
- **Prompt**: "Remember that I prefer practical books with concrete Java code examples"
- **Expected**: Tool `rememberPreference` executes. System stores structured memory record. Confirmation message returned to user.

### Case 8: Active Memory Retrieval
- **Step**: Open Memory tab in `/ai` or floating assistant widget.
- **Expected**: Newly added preference appears in the active preferences list with a timestamp.

### Case 9: Explicit Memory Deletion
- **Step**: Click "Forget" on the saved preference in the UI.
- **Expected**: `DELETE /api/ai/memory/{id}` executes. Memory is deactivated and removed from active context.

### Case 10: Real-Time Contemporary World Question
- **Prompt**: "What is the latest LTS release of Java and when was it released?"
- **Expected**: Web grounding activates via Google Search. Model response provides current release details accompanied by web citation source tags.

### Case 11: Mixed SmartLib + External Web Question
- **Prompt**: "Do we have Clean Code available, and what is the latest book published by Robert C. Martin?"
- **Expected**: Model executes SmartLib availability tool for physical copies, engages web grounding for contemporary publications, and presents clearly demarcated answers.

### Case 12: Source Citation Link Integrity
- **Step**: Click on a web source tag attached to an AI response.
- **Expected**: Opens the cited external URL in a new browser tab with `target="_blank"` and `rel="noopener noreferrer"`.

### Case 13: Provider Circuit Breaker Failover
- **Step**: Simulate primary Gemini outage (e.g., inject simulated 429 / HTTP 503).
- **Expected**: Orchestrator catches failure, engages 60-second cooldown on Gemini, and routes request seamlessly to Groq fallback provider (`llama-3.3-70b-versatile`). Response succeeds.

### Case 14: MCP Read-Only Catalog Tool Execution
- **Step**: Run MCP client tool `search_books` with `query="Clean Architecture"`.
- **Expected**: Returns JSON payload of matching books from backend without requiring authentication token.

### Case 15: MCP Personal Tool Execution with Authentication
- **Step**: Run MCP client tool `get_my_borrowings` with valid `SMARTLIB_AUTH_TOKEN`.
- **Expected**: Returns JSON payload of active borrowings for the authenticated user.

### Case 16: MCP Unauthorized Personal Access Rejection
- **Step**: Run MCP client tool `get_my_borrowings` without `SMARTLIB_AUTH_TOKEN` or attempt to supply `userId=5`.
- **Expected**: MCP server rejects request with authorization or invalid parameter error. No backend call made.

### Case 17: Per-User Sliding Window Rate Limiting
- **Step**: Send 11 rapid chat requests within 60 seconds from the same authenticated user.
- **Expected**: The 11th request is rejected with HTTP 429 and error message: *"Rate limit exceeded: Maximum 10 AI chat requests per minute."*

### Case 18: Role-Based Admin Access
- **Step**: Log in as `ROLE_ADMIN` and navigate to `/admin/ai`.
- **Expected**: Dedicated administrative AI assistant loads. Admin can inspect catalog stats and invoke management utilities.

### Case 19: Mobile Responsive UI Inspection
- **Step**: Open SmartLib in a mobile viewport (375px width).
- **Expected**: Floating widget collapses appropriately, conversation history wraps cleanly, and input area remains accessible without horizontal scrolling.

### Case 20: AI Request ID Header & Monotonic Latency Telemetry
- **Step**: Inspect Network panel for `POST /api/ai/chat`.
- **Expected**: HTTP response headers contain `X-AI-Request-Id: <uuid>`. Backend logs single-line safe telemetry event `AI_REQUEST_COMPLETED` with latency breakdown.
